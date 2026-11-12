package com.example.data

import android.content.Context
import com.example.data.model.AchievementEntity
import com.example.data.model.CommitmentEntity
import com.example.data.model.DebtEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.MonthlyCheckInEntity
import com.example.logic.AchievementEngine
import com.example.logic.FlowchartCalculator
import com.example.logic.FlowchartJourneyState
import com.example.util.DateUtils
import com.example.widget.ExpenseWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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
    val netSalary: Double,
    val isDecemberBonusMonth: Boolean,
    val thirteenthMonthBonus: Double,
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
    val accessibleSavings: Double,
    val totalKwsp: Double
)

class FinancialRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val expenseDao = db.expenseDao()
    private val commitmentDao = db.commitmentDao()
    private val monthlyCheckInDao = db.monthlyCheckInDao()
    private val profileDao = db.financialProfileDao()
    private val debtDao = db.debtDao()
    private val achievementDao = db.achievementDao()

    val profileFlow: Flow<FinancialProfileEntity?> = profileDao.getProfile()
    val debtsFlow: Flow<List<DebtEntity>> = debtDao.getAllDebts()
    val commitmentsFlow: Flow<List<CommitmentEntity>> = commitmentDao.getAllCommitments()
    val achievementsFlow: Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()
    val latestUnlockedAchievementFlow: Flow<AchievementEntity?> = achievementDao.getLatestUnlockedAchievement()

    fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesForMonth(monthKey)
    }

    /**
     * Combines Commitments and Check-Ins for a given monthKey into complete items.
     */
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
        }
    }

    /**
     * Flowchart journey state combined reactively from Profile and Debts.
     */
    val journeyStateFlow: Flow<FlowchartJourneyState> = combine(
        profileDao.getProfile(),
        debtDao.getAllDebts()
    ) { profileOpt, debts ->
        val profile = profileOpt ?: FinancialProfileEntity()
        FlowchartCalculator.calculateJourney(profile, debts)
    }

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

        // Evaluate achievements
        triggerAchievementEvaluation()

        // Update home screen widget
        ExpenseWidgetProvider.updateAllWidgets(context)

        id
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.delete(expense)
        ExpenseWidgetProvider.updateAllWidgets(context)
    }

    suspend fun markCommitmentPaid(
        item: MonthlyCommitmentItem,
        monthKey: String,
        actualAmount: Double
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val checkIn = MonthlyCheckInEntity(
            id = item.checkInId,
            monthKey = monthKey,
            commitmentId = item.commitmentId,
            isPaid = true,
            actualPaidAmount = actualAmount,
            paidTimestamp = now
        )
        monthlyCheckInDao.insertOrReplace(checkIn)

        // If this commitment is paying off a debt, reduce the debt balance!
        if (item.isDebt && !item.debtKey.isNullOrBlank()) {
            debtDao.reduceDebtBalance(item.debtKey, actualAmount)
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

        // Restore debt balance if it was paid
        if (item.isDebt && !item.debtKey.isNullOrBlank() && item.isPaid) {
            val debt = debtDao.getDebtByKey(item.debtKey)
            if (debt != null) {
                debtDao.setDebtBalance(item.debtKey, debt.currentBalance + item.actualPaidAmount)
            }
        }

        triggerAchievementEvaluation()
    }

    suspend fun updateRecurringDefault(commitmentId: Long, newDefault: Double) = withContext(Dispatchers.IO) {
        commitmentDao.updateDefaultAmount(commitmentId, newDefault)
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
        AchievementEngine.checkAndUnlockAchievements(achievementDao, profile, debts, expensesCount)
    }

    /**
     * Compute comprehensive Monthly Review Summary for any selected month.
     */
    suspend fun getMonthlySummary(monthKey: String): MonthlySummaryData = withContext(Dispatchers.IO) {
        val profile = profileDao.getProfileDirect() ?: FinancialProfileEntity()
        val debts = debtDao.getAllDebtsDirect()
        val commitments = commitmentDao.getAllCommitmentsDirect()
        val checkIns = monthlyCheckInDao.getCheckInsForMonthDirect(monthKey)
        val checkInMap = checkIns.associateBy { it.commitmentId }

        val isDec = DateUtils.isDecember(monthKey)
        val bonus = if (isDec) profile.basicSalary else 0.0
        val totalIncome = profile.netMonthlySalary + bonus

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

        MonthlySummaryData(
            monthKey = monthKey,
            netSalary = profile.netMonthlySalary,
            isDecemberBonusMonth = isDec,
            thirteenthMonthBonus = bonus,
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
            accessibleSavings = profile.totalAccessibleSavings,
            totalKwsp = profile.totalKwsp
        )
    }

    /**
     * JSON Backup Export
     */
    suspend fun exportDataAsJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val profile = profileDao.getProfileDirect() ?: FinancialProfileEntity()
        val debts = debtDao.getAllDebtsDirect()
        val commitments = commitmentDao.getAllCommitmentsDirect()

        // Profile json
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

        // Debts json
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

        // Commitments json
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

        root.put("exportedAt", System.currentTimeMillis())
        root.put("version", "1.0")
        root.toString(2)
    }

    /**
     * JSON Backup Restore
     */
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

            triggerAchievementEvaluation()
            ExpenseWidgetProvider.updateAllWidgets(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
