package com.example.data

import android.content.Context
import com.example.data.model.AchievementEntity
import com.example.data.model.CommitmentEntity
import com.example.data.model.DebtEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.FlowchartVerificationEntity
import com.example.data.model.MonthlyCheckInEntity
import com.example.data.model.MonthlyCheckInProgressEntity
import com.example.data.model.MonthlyIncomeEntity
import com.example.data.model.PaydayAllocationEntity
import com.example.data.model.SavingsAccountEntity
import com.example.data.model.SavingsTransactionEntity
import com.example.logic.AchievementEngine
import com.example.logic.FlowchartCalculator
import com.example.logic.FlowchartJourneyState
import com.example.logic.KwspCalculator
import com.example.logic.KwspContributionBreakdown
import com.example.util.DateUtils
import com.example.widget.ExpenseWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class MonthlyCommitmentItem(
    val checkInId: Long,
    val commitmentId: Long,
    val name: String,
    val defaultAmount: Double,
    val isDebt: Boolean,
    val debtKey: String?,
    val isPaid: Boolean,
    val actualPaidAmount: Double,
    val paidTimestamp: Long?
)

data class MonthlySummaryData(
    val monthKey: String,
    val grossSalary: Double,
    val netSalary: Double,
    val variableIncome: Double,
    val totalIncome: Double,
    val plannedCommitments: Double,
    val paidCommitments: Double,
    val remainingCommitments: Double,
    val quickExpensesTotal: Double,
    val expensesByCategory: Map<String, Double>,
    val totalOutflow: Double,
    val remainingCashflow: Double,
    val totalDebtRemaining: Double,
    val totalDebtReduction: Double,
    val everydayCash: Double,
    val dedicatedSavings: Double,
    val dedicatedEmergencyFund: Double,
    val totalKwsp: Double,
    val kwspBreakdown: KwspContributionBreakdown
)

class FinancialRepository(
    private val context: Context,
    dbOverride: AppDatabase? = null
) {
    private val db = dbOverride ?: AppDatabase.getDatabase(context)
    private val expenseDao = db.expenseDao()
    private val commitmentDao = db.commitmentDao()
    private val monthlyCheckInDao = db.monthlyCheckInDao()
    private val profileDao = db.financialProfileDao()
    private val debtDao = db.debtDao()
    private val achievementDao = db.achievementDao()
    private val savingsAccountDao = db.savingsAccountDao()
    private val savingsTransactionDao = db.savingsTransactionDao()
    private val monthlyIncomeDao = db.monthlyIncomeDao()
    private val flowchartVerificationDao = db.flowchartVerificationDao()
    private val paydayAllocationDao = db.paydayAllocationDao()
    private val monthlyCheckInProgressDao = db.monthlyCheckInProgressDao()

    val profileFlow: Flow<FinancialProfileEntity?> = profileDao.getProfile().distinctUntilChanged()
    val debtsFlow: Flow<List<DebtEntity>> = debtDao.getAllDebts().distinctUntilChanged()
    val commitmentsFlow: Flow<List<CommitmentEntity>> = commitmentDao.getAllCommitments().distinctUntilChanged()
    val achievementsFlow: Flow<List<AchievementEntity>> = achievementDao.getAllAchievements().distinctUntilChanged()
    val latestUnlockedAchievementFlow: Flow<AchievementEntity?> = achievementDao.getLatestUnlockedAchievement().distinctUntilChanged()

    // My Savings Vault Flows
    val savingsAccountsFlow: Flow<List<SavingsAccountEntity>> = savingsAccountDao.getAllAccounts().distinctUntilChanged()
    val totalDedicatedSavingsFlow: Flow<Double> = savingsAccountDao.getTotalDedicatedSavings().map { it ?: 0.0 }.distinctUntilChanged()
    val totalEmergencyFundFlow: Flow<Double> = savingsAccountDao.getTotalEmergencyFund().map { it ?: 0.0 }.distinctUntilChanged()
    val totalInterestCreditedFlow: Flow<Double> = savingsAccountDao.getTotalInterestCredited().map { it ?: 0.0 }.distinctUntilChanged()

    val flowchartVerificationsFlow: Flow<List<FlowchartVerificationEntity>> = flowchartVerificationDao.getAllVerifications().distinctUntilChanged()

    fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesForMonth(monthKey)
    }

    fun getIncomeForMonth(monthKey: String): Flow<MonthlyIncomeEntity?> {
        return monthlyIncomeDao.getIncomeForMonth(monthKey)
    }

    fun getPaydayAllocationForMonth(monthKey: String): Flow<PaydayAllocationEntity?> {
        return paydayAllocationDao.getAllocationForMonth(monthKey).distinctUntilChanged()
    }

    fun getCheckInProgressForMonth(monthKey: String): Flow<MonthlyCheckInProgressEntity?> {
        return monthlyCheckInProgressDao.getProgressForMonth(monthKey).distinctUntilChanged()
    }

    fun getCommitmentItemsForMonth(monthKey: String): Flow<List<MonthlyCommitmentItem>> {
        return combine(
            commitmentDao.getAllCommitments(),
            monthlyCheckInDao.getCheckInsForMonth(monthKey)
        ) { commitments, checkIns ->
            val checkInMap = checkIns.associateBy { it.commitmentId }
            commitments.map { commitment ->
                val checkIn = checkInMap[commitment.id]
                MonthlyCommitmentItem(
                    checkInId = checkIn?.id ?: 0L,
                    commitmentId = commitment.id,
                    name = commitment.name,
                    defaultAmount = commitment.defaultAmount,
                    isDebt = commitment.isDebt,
                    debtKey = commitment.debtKey,
                    isPaid = checkIn?.isPaid ?: false,
                    actualPaidAmount = if (checkIn != null && checkIn.isPaid) checkIn.actualPaidAmount else commitment.defaultAmount,
                    paidTimestamp = checkIn?.paidTimestamp
                )
            }
        }.distinctUntilChanged()
    }

    /**
     * Flowchart journey state combined reactively from Profile, Debts, Dedicated Emergency Savings, and Verifications.
     */
    val journeyStateFlow: Flow<FlowchartJourneyState> = combine(
        profileDao.getProfile(),
        debtDao.getAllDebts(),
        totalEmergencyFundFlow,
        totalDedicatedSavingsFlow,
        flowchartVerificationsFlow
    ) { profileOpt, debts, emergencySavings, dedicatedSavings, verifications ->
        val profile = profileOpt ?: FinancialProfileEntity()
        FlowchartCalculator.calculateJourney(
            profile = profile,
            debts = debts,
            dedicatedEmergencySavings = emergencySavings,
            dedicatedTotalSavings = dedicatedSavings,
            verifications = verifications
        )
    }.distinctUntilChanged()

    suspend fun ensureCheckInsForMonth(monthKey: String) = withContext(Dispatchers.IO) {
        val existingCheckIns = monthlyCheckInDao.getCheckInsForMonthDirect(monthKey)
        val existingCommitmentIds = existingCheckIns.map { it.commitmentId }.toSet()
        val allCommitments = commitmentDao.getAllCommitmentsDirect()

        val missingCheckIns = allCommitments.filter { it.id !in existingCommitmentIds }.map { commitment ->
            MonthlyCheckInEntity(
                monthKey = monthKey,
                commitmentId = commitment.id,
                isPaid = false,
                actualPaidAmount = commitment.defaultAmount,
                paidTimestamp = null
            )
        }
        if (missingCheckIns.isNotEmpty()) {
            monthlyCheckInDao.insertAll(missingCheckIns)
        }
    }

    suspend fun addExpense(
        amount: Double,
        category: String,
        description: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        }
        val monthKey = sdf.format(Date(timestamp))

        val expense = ExpenseEntity(
            amount = amount,
            category = category,
            description = description.trim(),
            timestamp = timestamp,
            monthKey = monthKey
        )
        val id = expenseDao.insert(expense)

        triggerAchievementEvaluation()
        ExpenseWidgetProvider.updateAllWidgets(context)
        id
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.delete(expense)
        triggerAchievementEvaluation()
        ExpenseWidgetProvider.updateAllWidgets(context)
    }

    /**
     * POKA-YOKE Payment Confirmation:
     * Commits selected payments in a batch.
     * For HP1 & HP2, does not blindly deduct instalment from legal loan principal.
     */
    suspend fun confirmBatchPayments(
        monthKey: String,
        payments: List<Pair<MonthlyCommitmentItem, Double>>
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        for ((item, actualAmount) in payments) {
            val checkIn = MonthlyCheckInEntity(
                id = item.checkInId,
                monthKey = monthKey,
                commitmentId = item.commitmentId,
                isPaid = true,
                actualPaidAmount = actualAmount,
                paidTimestamp = now
            )
            monthlyCheckInDao.insertOrReplace(checkIn)

            // For High-Interest Revolving/Personal loans (Maybank CC & SLoan), payment reduces balance
            if (item.isDebt && !item.debtKey.isNullOrBlank()) {
                val debt = debtDao.getDebtByKey(item.debtKey)
                if (debt != null && debt.isHighInterest) {
                    debtDao.reduceDebtBalance(item.debtKey, actualAmount)
                }
                // Note: For vehicle hire purchase (HP1 & HP2), principal is NOT reduced directly by payment instalment.
            }
        }
        val existingProgress = monthlyCheckInProgressDao.getProgressForMonthDirect(monthKey)
            ?: MonthlyCheckInProgressEntity(monthKey = monthKey)
        monthlyCheckInProgressDao.insertOrUpdate(
            existingProgress.copy(
                stage3Reviewed = true,
                stage4Confirmed = true,
                updatedTimestamp = now
            )
        )
        triggerAchievementEvaluation()
    }

    suspend fun unmarkCommitmentPaid(
        item: MonthlyCommitmentItem,
        monthKey: String
    ) = withContext(Dispatchers.IO) {
        val checkIn = MonthlyCheckInEntity(
            id = item.checkInId,
            monthKey = monthKey,
            commitmentId = item.commitmentId,
            isPaid = false,
            actualPaidAmount = item.defaultAmount,
            paidTimestamp = null
        )
        monthlyCheckInDao.insertOrReplace(checkIn)

        if (item.isDebt && !item.debtKey.isNullOrBlank() && item.isPaid) {
            val debt = debtDao.getDebtByKey(item.debtKey)
            if (debt != null && debt.isHighInterest) {
                debtDao.setDebtBalance(item.debtKey, debt.currentBalance + item.actualPaidAmount)
            }
        }
        triggerAchievementEvaluation()
    }

    suspend fun updateRecurringDefault(commitmentId: Long, newDefault: Double) = withContext(Dispatchers.IO) {
        commitmentDao.updateDefaultAmount(commitmentId, newDefault)
    }

    // --- MY SAVINGS VAULT OPERATIONS ---
    suspend fun addSavingsAccount(
        name: String,
        purpose: String,
        isEmergencyFund: Boolean,
        initialBalance: Double,
        expectedReturn: Double?
    ): Long = withContext(Dispatchers.IO) {
        val account = SavingsAccountEntity(
            name = name,
            purpose = purpose,
            isEmergencyFund = isEmergencyFund,
            currentBalance = initialBalance,
            expectedAnnualReturnPercent = expectedReturn,
            totalInterestCredited = 0.0,
            createdTimestamp = System.currentTimeMillis()
        )
        val id = savingsAccountDao.insert(account)
        if (initialBalance > 0) {
            savingsTransactionDao.insert(
                SavingsTransactionEntity(
                    accountId = id,
                    type = "DEPOSIT",
                    amount = initialBalance,
                    fromEverydayCash = false,
                    note = "Initial balance"
                )
            )
        }
        syncDedicatedSavingsToProfile()
        triggerAchievementEvaluation()
        id
    }

    suspend fun depositToSavings(
        accountId: Long,
        amount: Double,
        fromEverydayCash: Boolean,
        note: String
    ) = withContext(Dispatchers.IO) {
        val account = savingsAccountDao.getAccountById(accountId) ?: return@withContext
        val newBalance = account.currentBalance + amount
        savingsAccountDao.update(account.copy(currentBalance = newBalance))

        savingsTransactionDao.insert(
            SavingsTransactionEntity(
                accountId = accountId,
                type = "DEPOSIT",
                amount = amount,
                fromEverydayCash = fromEverydayCash,
                note = note
            )
        )

        // Internal transfer: deduct from everyday cash without double-counting as an expense
        if (fromEverydayCash) {
            val profile = profileDao.getProfileDirect() ?: FinancialProfileEntity()
            val newBankBalance = maxOf(0.0, profile.bankBalance - amount)
            profileDao.updateBankBalance(newBankBalance)
        }

        syncDedicatedSavingsToProfile()
        triggerAchievementEvaluation()
    }

    suspend fun withdrawFromSavings(
        accountId: Long,
        amount: Double,
        toEverydayCash: Boolean,
        note: String
    ) = withContext(Dispatchers.IO) {
        val account = savingsAccountDao.getAccountById(accountId) ?: return@withContext
        val newBalance = maxOf(0.0, account.currentBalance - amount)
        savingsAccountDao.update(account.copy(currentBalance = newBalance))

        savingsTransactionDao.insert(
            SavingsTransactionEntity(
                accountId = accountId,
                type = "WITHDRAWAL",
                amount = amount,
                fromEverydayCash = toEverydayCash,
                note = note
            )
        )

        // Internal transfer to everyday cash
        if (toEverydayCash) {
            val profile = profileDao.getProfileDirect() ?: FinancialProfileEntity()
            profileDao.updateBankBalance(profile.bankBalance + amount)
        }

        syncDedicatedSavingsToProfile()
        triggerAchievementEvaluation()
    }

    suspend fun creditInterestToSavings(
        accountId: Long,
        interestAmount: Double,
        note: String
    ) = withContext(Dispatchers.IO) {
        val account = savingsAccountDao.getAccountById(accountId) ?: return@withContext
        val newBalance = account.currentBalance + interestAmount
        val newTotalInterest = account.totalInterestCredited + interestAmount
        savingsAccountDao.update(account.copy(currentBalance = newBalance, totalInterestCredited = newTotalInterest))

        savingsTransactionDao.insert(
            SavingsTransactionEntity(
                accountId = accountId,
                type = "INTEREST",
                amount = interestAmount,
                fromEverydayCash = false,
                note = if (note.isBlank()) "Credited return / dividend" else note
            )
        )

        syncDedicatedSavingsToProfile()
        triggerAchievementEvaluation()
    }

    private suspend fun syncDedicatedSavingsToProfile() {
        val accounts = savingsAccountDao.getAllAccountsDirect()
        val totalDedicated = accounts.sumOf { it.currentBalance }
        profileDao.updateDedicatedSavings(totalDedicated)
    }

    // --- MONTHLY INCOME CHECK-IN ---
    suspend fun saveMonthlyIncome(income: MonthlyIncomeEntity) = withContext(Dispatchers.IO) {
        val existing = monthlyIncomeDao.getIncomeForMonthDirect(income.monthKey)
        monthlyIncomeDao.insertOrUpdate(
            income.copy(
                id = existing?.id ?: income.id,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    // --- PAYDAY INTELLIGENCE V1/V1.1 ALLOCATION PLAN ---
    suspend fun savePaydayAllocation(allocation: PaydayAllocationEntity) = withContext(Dispatchers.IO) {
        val existing = paydayAllocationDao.getAllocationForMonthDirect(allocation.monthKey)
        paydayAllocationDao.insertOrUpdate(
            allocation.copy(
                id = existing?.id ?: allocation.id,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    // --- PERSISTENT MONTHLY CHECK-IN STAGE PROGRESS ---
    suspend fun saveCheckInProgress(progress: MonthlyCheckInProgressEntity) = withContext(Dispatchers.IO) {
        monthlyCheckInProgressDao.insertOrUpdate(
            progress.copy(updatedTimestamp = System.currentTimeMillis())
        )
    }

    // --- FLOWCHART STEP VERIFICATION ---
    suspend fun setStepVerification(stepNumber: Int, isConfirmed: Boolean, notes: String = "") = withContext(Dispatchers.IO) {
        flowchartVerificationDao.setVerification(
            FlowchartVerificationEntity(
                stepNumber = stepNumber,
                isConfirmed = isConfirmed,
                confirmedTimestamp = if (isConfirmed) System.currentTimeMillis() else null,
                notes = notes
            )
        )
        triggerAchievementEvaluation()
    }

    suspend fun updateProfile(profile: FinancialProfileEntity) = withContext(Dispatchers.IO) {
        profileDao.insertOrUpdate(profile.copy(lastUpdated = System.currentTimeMillis()))
        triggerAchievementEvaluation()
    }

    suspend fun updateDebtBalance(key: String, newBalance: Double) = withContext(Dispatchers.IO) {
        debtDao.setDebtBalance(key, newBalance)
        triggerAchievementEvaluation()
    }

    suspend fun triggerAchievementEvaluation() = withContext(Dispatchers.IO) {
        val profile = profileDao.getProfileDirect() ?: FinancialProfileEntity()
        val debts = debtDao.getAllDebtsDirect()
        val expensesCount = expenseDao.getTotalExpenseCount()
        val emergencySavings = savingsAccountDao.getTotalEmergencyFundDirect() ?: 0.0
        val accounts = savingsAccountDao.getAllAccountsDirect()
        val totalDedicated = accounts.sumOf { it.currentBalance }

        AchievementEngine.checkAndUnlockAchievements(
            achievementDao = achievementDao,
            profile = profile,
            debts = debts,
            totalExpensesCount = expensesCount,
            dedicatedEmergencySavings = emergencySavings,
            dedicatedTotalSavings = totalDedicated
        )
    }

    suspend fun getMonthlySummary(monthKey: String): MonthlySummaryData = withContext(Dispatchers.IO) {
        val profile = profileDao.getProfileDirect() ?: FinancialProfileEntity()
        val debts = debtDao.getAllDebtsDirect()
        val commitments = commitmentDao.getAllCommitmentsDirect()
        val checkIns = monthlyCheckInDao.getCheckInsForMonthDirect(monthKey)
        val checkInMap = checkIns.associateBy { it.commitmentId }

        val incomeRecord = monthlyIncomeDao.getIncomeForMonthDirect(monthKey)
        val grossSalary = incomeRecord?.grossSalary ?: profile.basicSalary
        val isConfirmed = incomeRecord?.isConfirmedReceived ?: true
        val netSalary = if (isConfirmed) (incomeRecord?.netSalary ?: profile.netMonthlySalary) else 0.0
        val overtime = if (isConfirmed) (incomeRecord?.overtime ?: 0.0) else 0.0
        val incentives = if (isConfirmed) (incomeRecord?.incentives ?: 0.0) else 0.0
        val thirteenthMonth = if (isConfirmed) (incomeRecord?.thirteenthMonth ?: 0.0) else 0.0
        val otherIncome = if (isConfirmed) (incomeRecord?.otherIncome ?: 0.0) else 0.0
        val variableIncome = overtime + incentives + thirteenthMonth + otherIncome
        val totalIncome = netSalary + variableIncome

        val kwspBreakdown = if (incomeRecord != null) {
            KwspCalculator.calculateFromMonthlyIncome(incomeRecord)
        } else {
            KwspCalculator.calculateKwsp(
                grossSalary = grossSalary,
                actualEmployeeAmount = null,
                actualEmployerAmount = null
            )
        }


        val plannedCommitments = commitments.sumOf { it.defaultAmount }
        var paidCommitments = 0.0
        commitments.forEach { c ->
            val checkIn = checkInMap[c.id]
            if (checkIn != null && checkIn.isPaid) {
                paidCommitments += checkIn.actualPaidAmount
            }
        }
        val remainingCommitments = maxOf(0.0, plannedCommitments - paidCommitments)

        val expenses = expenseDao.getExpensesForMonthDirect(monthKey)
        val quickExpensesTotal = expenses.sumOf { it.amount }
        val categoryBreakdown = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val totalOutflow = paidCommitments + quickExpensesTotal
        val remainingCashflow = totalIncome - totalOutflow

        val initialDebt = debts.sumOf { it.initialBalance }
        val currentDebt = debts.sumOf { it.currentBalance }
        val debtReduction = maxOf(0.0, initialDebt - currentDebt)

        val emergencySavings = savingsAccountDao.getTotalEmergencyFundDirect() ?: 0.0
        val accounts = savingsAccountDao.getAllAccountsDirect()
        val totalDedicated = accounts.sumOf { it.currentBalance }

        MonthlySummaryData(
            monthKey = monthKey,
            grossSalary = grossSalary,
            netSalary = netSalary,
            variableIncome = variableIncome,
            totalIncome = totalIncome,
            plannedCommitments = plannedCommitments,
            paidCommitments = paidCommitments,
            remainingCommitments = remainingCommitments,
            quickExpensesTotal = quickExpensesTotal,
            expensesByCategory = categoryBreakdown,
            totalOutflow = totalOutflow,
            remainingCashflow = remainingCashflow,
            totalDebtRemaining = currentDebt,
            totalDebtReduction = debtReduction,
            everydayCash = profile.bankBalance,
            dedicatedSavings = totalDedicated,
            dedicatedEmergencyFund = emergencySavings,
            totalKwsp = profile.totalKwsp,
            kwspBreakdown = kwspBreakdown
        )
    }

    suspend fun exportDataAsJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val profile = profileDao.getProfileDirect() ?: FinancialProfileEntity()
        val debts = debtDao.getAllDebtsDirect()
        val commitments = commitmentDao.getAllCommitmentsDirect()
        val savings = savingsAccountDao.getAllAccountsDirect()
        val incomes = monthlyIncomeDao.getAllIncomesDirect()
        val paydayAllocations = paydayAllocationDao.getAllAllocationsDirect()
        val checkInProgressList = monthlyCheckInProgressDao.getAllProgressDirect()

        val profileObj = JSONObject().apply {
            put("netMonthlySalary", profile.netMonthlySalary)
            put("basicSalary", profile.basicSalary)
            put("bankBalance", profile.bankBalance)
            put("dedicatedSavings", profile.dedicatedSavings)
            put("asb", profile.asb)
            put("tabungHaji", profile.tabungHaji)
            put("fixedDeposits", profile.fixedDeposits)
            put("otherInvestments", profile.otherInvestments)
            put("kwspAccount1", profile.kwspAccount1)
            put("kwspAccount2", profile.kwspAccount2)
            put("kwspAccount3", profile.kwspAccount3)
        }
        root.put("profile", profileObj)

        val debtsArray = JSONArray()
        debts.forEach { d ->
            val obj = JSONObject().apply {
                put("key", d.key)
                put("name", d.name)
                put("initialBalance", d.initialBalance)
                put("currentBalance", d.currentBalance)
                put("monthlyTarget", d.monthlyTarget)
                put("aprPercent", d.aprPercent)
                put("isHighInterest", d.isHighInterest)
            }
            debtsArray.put(obj)
        }
        root.put("debts", debtsArray)

        val commitmentsArray = JSONArray()
        commitments.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("defaultAmount", c.defaultAmount)
                put("isDebt", c.isDebt)
                put("debtKey", c.debtKey ?: "")
            }
            commitmentsArray.put(obj)
        }
        root.put("commitments", commitmentsArray)

        val savingsArray = JSONArray()
        savings.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("purpose", s.purpose)
                put("isEmergencyFund", s.isEmergencyFund)
                put("currentBalance", s.currentBalance)
                put("totalInterestCredited", s.totalInterestCredited)
            }
            savingsArray.put(obj)
        }
        root.put("savings_accounts", savingsArray)

        val incomesArray = JSONArray()
        incomes.forEach { inc ->
            val obj = JSONObject().apply {
                put("monthKey", inc.monthKey)
                put("grossSalary", inc.grossSalary)
                put("netSalary", inc.netSalary)
                put("overtime", inc.overtime)
                put("incentives", inc.incentives)
                put("thirteenthMonth", inc.thirteenthMonth)
                put("otherIncome", inc.otherIncome)
                put("reimbursements", inc.reimbursements)
                put("unconfirmedIncome", inc.unconfirmedIncome)
                put("isConfirmedReceived", inc.isConfirmedReceived)
                put("actualEmployeeKwsp", inc.actualEmployeeKwsp ?: JSONObject.NULL)
                put("actualEmployerKwsp", inc.actualEmployerKwsp ?: JSONObject.NULL)
                put("updatedTimestamp", inc.updatedTimestamp)
            }
            incomesArray.put(obj)
        }
        root.put("monthly_incomes", incomesArray)

        val allocationsArray = JSONArray()
        paydayAllocations.forEach { alloc ->
            val obj = JSONObject().apply {
                put("monthKey", alloc.monthKey)
                put("cashSourceMode", alloc.cashSourceMode)
                put("openingCashInput", alloc.openingCashInput)
                put("confirmedAvailableCash", alloc.confirmedAvailableCash)
                put("calculatedMonthlyCashflow", alloc.calculatedMonthlyCashflow)
                put("reimbursementsExcluded", alloc.reimbursementsExcluded)
                put("unconfirmedIncomeExcluded", alloc.unconfirmedIncomeExcluded)
                put("upcomingEssentialExpensesInput", alloc.upcomingEssentialExpensesInput ?: JSONObject.NULL)
                put("emergencyFundTargetInput", alloc.emergencyFundTargetInput ?: JSONObject.NULL)
                put("cashBufferTargetInput", alloc.cashBufferTargetInput ?: JSONObject.NULL)
                put("minCashBufferEnforced", alloc.minCashBufferEnforced)
                put("recEssentialCommitments", alloc.recEssentialCommitments)
                put("recMinDebtPayments", alloc.recMinDebtPayments)
                put("recUpcomingEssentials", alloc.recUpcomingEssentials)
                put("recExtraDebtRepayment", alloc.recExtraDebtRepayment)
                put("recExtraDebtTargetKey", alloc.recExtraDebtTargetKey ?: JSONObject.NULL)
                put("recDebtAllocationsJson", alloc.recDebtAllocationsJson)
                put("recEmergencySavings", alloc.recEmergencySavings)
                put("recDiscretionaryAllowance", alloc.recDiscretionaryAllowance)
                put("recRemainingBuffer", alloc.recRemainingBuffer)
                put("approvedExtraDebtRepayment", alloc.approvedExtraDebtRepayment)
                put("approvedDebtAllocationsJson", alloc.approvedDebtAllocationsJson)
                put("approvedEmergencySavings", alloc.approvedEmergencySavings)
                put("approvedDiscretionaryAllowance", alloc.approvedDiscretionaryAllowance)
                put("approvedRemainingBuffer", alloc.approvedRemainingBuffer)
                put("hasOverrides", alloc.hasOverrides)
                put("isApproved", alloc.isApproved)
                put("approvedTimestamp", alloc.approvedTimestamp ?: JSONObject.NULL)
                put("rationaleSummary", alloc.rationaleSummary)
                put("updatedTimestamp", alloc.updatedTimestamp)
            }
            allocationsArray.put(obj)
        }
        root.put("payday_allocations", allocationsArray)

        val progressArray = JSONArray()
        checkInProgressList.forEach { prog ->
            val obj = JSONObject().apply {
                put("monthKey", prog.monthKey)
                put("stage3Reviewed", prog.stage3Reviewed)
                put("stage4Confirmed", prog.stage4Confirmed)
                put("stage5Reviewed", prog.stage5Reviewed)
                put("stage6Completed", prog.stage6Completed)
                put("updatedTimestamp", prog.updatedTimestamp)
            }
            progressArray.put(obj)
        }
        root.put("monthly_checkin_progress", progressArray)

        root.put("appName", "Project 41")
        root.put("version", "3.0")
        root.put("exportedAt", System.currentTimeMillis())
        root.toString(2)
    }

    suspend fun restoreDataFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                val restoredProfile = FinancialProfileEntity(
                    id = 1,
                    netMonthlySalary = p.optDouble("netMonthlySalary", 4460.50),
                    basicSalary = p.optDouble("basicSalary", 4158.00),
                    bankBalance = p.optDouble("bankBalance", 1039.56),
                    dedicatedSavings = p.optDouble("dedicatedSavings", 0.0),
                    asb = p.optDouble("asb", 0.0),
                    tabungHaji = p.optDouble("tabungHaji", 0.0),
                    fixedDeposits = p.optDouble("fixedDeposits", 0.0),
                    otherInvestments = p.optDouble("otherInvestments", 0.0),
                    kwspAccount1 = p.optDouble("kwspAccount1", 92783.88),
                    kwspAccount2 = p.optDouble("kwspAccount2", 6231.14),
                    kwspAccount3 = p.optDouble("kwspAccount3", 164.70),
                    lastUpdated = System.currentTimeMillis()
                )
                profileDao.insertOrUpdate(restoredProfile)
            }

            if (root.has("debts")) {
                val debtsArray = root.getJSONArray("debts")
                for (i in 0 until debtsArray.length()) {
                    val d = debtsArray.optJSONObject(i) ?: continue
                    val key = d.optString("key", "")
                    if (key.isBlank()) continue
                    val existing = debtDao.getDebtByKey(key)
                    val curBalance = optNullableDouble(d, "currentBalance")
                    val initBalance = optNullableDouble(d, "initialBalance")
                    val monthlyTarget = optNullableDouble(d, "monthlyTarget")
                    val name = optNullableString(d, "name")
                    val aprPercent = optNullableDouble(d, "aprPercent")
                    val isHighInterest = optNullableBoolean(d, "isHighInterest")

                    if (existing != null) {
                        debtDao.update(
                            existing.copy(
                                name = name ?: existing.name,
                                initialBalance = initBalance ?: existing.initialBalance,
                                currentBalance = curBalance ?: existing.currentBalance,
                                monthlyTarget = monthlyTarget ?: existing.monthlyTarget,
                                aprPercent = aprPercent ?: existing.aprPercent,
                                isHighInterest = isHighInterest ?: existing.isHighInterest
                            )
                        )
                    } else {
                        val resolvedCur = curBalance ?: 0.0
                        debtDao.insertAll(
                            listOf(
                                DebtEntity(
                                    key = key,
                                    name = name ?: key,
                                    initialBalance = initBalance ?: resolvedCur,
                                    currentBalance = resolvedCur,
                                    monthlyTarget = monthlyTarget ?: 0.0,
                                    aprPercent = aprPercent ?: 0.0,
                                    isHighInterest = isHighInterest ?: false
                                )
                            )
                        )
                    }
                }
            }

            if (root.has("commitments")) {
                val commArray = root.getJSONArray("commitments")
                for (i in 0 until commArray.length()) {
                    val c = commArray.getJSONObject(i)
                    val id = c.getLong("id")
                    val def = c.optDouble("defaultAmount", 0.0)
                    commitmentDao.updateDefaultAmount(id, def)
                }
            }

            // V1.1 additions (strictly optional for backward compatibility with older JSON backups)
            val incomesArray = root.optJSONArray("monthly_incomes")
            if (incomesArray != null) {
                for (i in 0 until incomesArray.length()) {
                    val obj = incomesArray.optJSONObject(i) ?: continue
                    val monthKey = obj.optString("monthKey", "")
                    if (monthKey.isBlank()) continue
                    val existing = monthlyIncomeDao.getIncomeForMonthDirect(monthKey)
                    monthlyIncomeDao.insertOrUpdate(
                        MonthlyIncomeEntity(
                            id = existing?.id ?: 0L,
                            monthKey = monthKey,
                            grossSalary = obj.optDouble("grossSalary", 4158.00),
                            netSalary = obj.optDouble("netSalary", 4460.50),
                            overtime = obj.optDouble("overtime", 0.0),
                            incentives = obj.optDouble("incentives", 0.0),
                            thirteenthMonth = obj.optDouble("thirteenthMonth", 0.0),
                            otherIncome = obj.optDouble("otherIncome", 0.0),
                            reimbursements = obj.optDouble("reimbursements", 0.0),
                            unconfirmedIncome = obj.optDouble("unconfirmedIncome", 0.0),
                            isConfirmedReceived = obj.optBoolean("isConfirmedReceived", true),
                            actualEmployeeKwsp = optNullableDouble(obj, "actualEmployeeKwsp"),
                            actualEmployerKwsp = optNullableDouble(obj, "actualEmployerKwsp"),
                            updatedTimestamp = obj.optLong("updatedTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val allocationsArray = root.optJSONArray("payday_allocations")
            if (allocationsArray != null) {
                for (i in 0 until allocationsArray.length()) {
                    val obj = allocationsArray.optJSONObject(i) ?: continue
                    val monthKey = obj.optString("monthKey", "")
                    if (monthKey.isBlank()) continue
                    val existing = paydayAllocationDao.getAllocationForMonthDirect(monthKey)
                    paydayAllocationDao.insertOrUpdate(
                        PaydayAllocationEntity(
                            id = existing?.id ?: 0L,
                            monthKey = monthKey,
                            cashSourceMode = obj.optString("cashSourceMode", "OPENING_PLUS_CASHFLOW"),
                            openingCashInput = obj.optDouble("openingCashInput", 0.0),
                            confirmedAvailableCash = obj.optDouble("confirmedAvailableCash", 0.0),
                            calculatedMonthlyCashflow = obj.optDouble("calculatedMonthlyCashflow", 0.0),
                            reimbursementsExcluded = obj.optDouble("reimbursementsExcluded", 0.0),
                            unconfirmedIncomeExcluded = obj.optDouble("unconfirmedIncomeExcluded", 0.0),
                            upcomingEssentialExpensesInput = optNullableDouble(obj, "upcomingEssentialExpensesInput"),
                            emergencyFundTargetInput = optNullableDouble(obj, "emergencyFundTargetInput"),
                            cashBufferTargetInput = optNullableDouble(obj, "cashBufferTargetInput"),
                            minCashBufferEnforced = obj.optDouble("minCashBufferEnforced", 0.0),
                            recEssentialCommitments = obj.optDouble("recEssentialCommitments", 0.0),
                            recMinDebtPayments = obj.optDouble("recMinDebtPayments", 0.0),
                            recUpcomingEssentials = obj.optDouble("recUpcomingEssentials", 0.0),
                            recExtraDebtRepayment = obj.optDouble("recExtraDebtRepayment", 0.0),
                            recExtraDebtTargetKey = optNullableString(obj, "recExtraDebtTargetKey"),
                            recDebtAllocationsJson = obj.optString("recDebtAllocationsJson", "[]"),
                            recEmergencySavings = obj.optDouble("recEmergencySavings", 0.0),
                            recDiscretionaryAllowance = obj.optDouble("recDiscretionaryAllowance", 0.0),
                            recRemainingBuffer = obj.optDouble("recRemainingBuffer", 0.0),
                            approvedExtraDebtRepayment = obj.optDouble("approvedExtraDebtRepayment", 0.0),
                            approvedDebtAllocationsJson = obj.optString("approvedDebtAllocationsJson", "[]"),
                            approvedEmergencySavings = obj.optDouble("approvedEmergencySavings", 0.0),
                            approvedDiscretionaryAllowance = obj.optDouble("approvedDiscretionaryAllowance", 0.0),
                            approvedRemainingBuffer = obj.optDouble("approvedRemainingBuffer", 0.0),
                            hasOverrides = obj.optBoolean("hasOverrides", false),
                            isApproved = obj.optBoolean("isApproved", false),
                            approvedTimestamp = optNullableLong(obj, "approvedTimestamp"),
                            rationaleSummary = obj.optString("rationaleSummary", ""),
                            updatedTimestamp = obj.optLong("updatedTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val progressArray = root.optJSONArray("monthly_checkin_progress")
            if (progressArray != null) {
                for (i in 0 until progressArray.length()) {
                    val obj = progressArray.optJSONObject(i) ?: continue
                    val monthKey = obj.optString("monthKey", "")
                    if (monthKey.isBlank()) continue
                    monthlyCheckInProgressDao.insertOrUpdate(
                        MonthlyCheckInProgressEntity(
                            monthKey = monthKey,
                            stage3Reviewed = obj.optBoolean("stage3Reviewed", false),
                            stage4Confirmed = obj.optBoolean("stage4Confirmed", false),
                            stage5Reviewed = obj.optBoolean("stage5Reviewed", false),
                            stage6Completed = obj.optBoolean("stage6Completed", false),
                            updatedTimestamp = obj.optLong("updatedTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            syncDedicatedSavingsToProfile()
            triggerAchievementEvaluation()
            try {
                ExpenseWidgetProvider.updateAllWidgets(context)
            } catch (_: Exception) {
                // Ignore widget update failures when running under headless test runners
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun optNullableDouble(obj: JSONObject, key: String): Double? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val value = obj.optDouble(key, Double.NaN)
        return if (value.isNaN()) null else value
    }

    private fun optNullableLong(obj: JSONObject, key: String): Long? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optLong(key)
    }

    private fun optNullableString(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optString(key).takeIf { it.isNotEmpty() }
    }

    private fun optNullableBoolean(obj: JSONObject, key: String): Boolean? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return when (val raw = obj.opt(key)) {
            is Boolean -> raw
            is String -> raw.toBooleanStrictOrNull()
            is Number -> raw.toInt() != 0
            else -> null
        }
    }
}
