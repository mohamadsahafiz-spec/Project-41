package com.example.data

import android.content.Context
import com.example.data.model.AchievementEntity
import com.example.data.model.CommitmentEntity
import com.example.data.model.DebtEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.FlowchartVerificationEntity
import com.example.data.model.MonthlyCheckInEntity
import com.example.data.model.MonthlyIncomeEntity
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

class FinancialRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
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
        monthlyIncomeDao.insertOrUpdate(income)
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
        val netSalary = incomeRecord?.netSalary ?: profile.netMonthlySalary
        val variableIncome = (incomeRecord?.overtime ?: 0.0) + (incomeRecord?.incentives ?: 0.0) +
                (incomeRecord?.thirteenthMonth ?: 0.0) + (incomeRecord?.otherIncome ?: 0.0)
        val totalIncome = netSalary + variableIncome

        val kwspBreakdown = KwspCalculator.calculateKwsp(
            grossSalary = grossSalary,
            actualEmployeeAmount = incomeRecord?.actualEmployeeKwsp,
            actualEmployerAmount = incomeRecord?.actualEmployerKwsp
        )

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

        root.put("appName", "Project 41")
        root.put("version", "2.0")
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
                    val d = debtsArray.getJSONObject(i)
                    val key = d.getString("key")
                    val cur = d.optDouble("currentBalance", 0.0)
                    debtDao.setDebtBalance(key, cur)
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

            syncDedicatedSavingsToProfile()
            triggerAchievementEvaluation()
            ExpenseWidgetProvider.updateAllWidgets(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
