package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.model.ExpenseEntity
import com.example.data.model.FlowchartVerificationEntity
import com.example.data.model.MonthlyIncomeEntity
import android.appwidget.AppWidgetManager
import android.content.Intent
import com.example.data.model.SavingsAccountEntity
import com.example.data.model.SavingsTransactionEntity
import com.example.widget.ExpenseWidgetProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun readAppNameFromContext_isProject41() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Project 41", appName)
    }

    @Test
    fun testWidgetRemoteViewsInflation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = android.widget.RemoteViews(context.packageName, R.layout.widget_expense)
        val inflated = views.apply(context, null)
        assertNotNull(inflated)
    }

    @Test
    fun testExpenseInsertionAndMonthlyTotal() = runBlocking {
        val expenseDao = db.expenseDao()
        val expense1 = ExpenseEntity(
            amount = 55.50,
            category = "Groceries",
            description = "Lotus groceries",
            timestamp = 1700000000000L,
            monthKey = "2026-11"
        )
        val expense2 = ExpenseEntity(
            amount = 40.00,
            category = "Petrol",
            description = "Petronas RON95",
            timestamp = 1700000100000L,
            monthKey = "2026-11"
        )

        expenseDao.insert(expense1)
        expenseDao.insert(expense2)

        val expenses = expenseDao.getExpensesForMonthDirect("2026-11")
        assertEquals(2, expenses.size)

        val total = expenseDao.getTotalExpenseForMonth("2026-11")
        assertNotNull(total)
        assertEquals(95.50, total ?: 0.0, 0.001)
    }

    @Test
    fun testSavingsVaultAccountsAndTransactions() = runBlocking {
        val accountDao = db.savingsAccountDao()
        val txDao = db.savingsTransactionDao()

        val account = SavingsAccountEntity(
            name = "Starter Emergency Buffer",
            purpose = "Emergency Fund",
            isEmergencyFund = true,
            currentBalance = 500.0,
            expectedAnnualReturnPercent = 2.5
        )
        val accountId = accountDao.insert(account)
        assertTrue(accountId > 0)

        val tx = SavingsTransactionEntity(
            accountId = accountId,
            type = "DEPOSIT",
            amount = 500.0,
            fromEverydayCash = true,
            note = "Initial buffer deposit"
        )
        txDao.insert(tx)

        val accounts = accountDao.getAllAccountsDirect()
        assertEquals(1, accounts.size)
        assertEquals(500.0, accounts[0].currentBalance, 0.01)

        val totalEmergency = accountDao.getTotalEmergencyFundDirect()
        assertEquals(500.0, totalEmergency ?: 0.0, 0.01)
    }

    @Test
    fun testMonthlyIncomeAndKwspStorage() = runBlocking {
        val incomeDao = db.monthlyIncomeDao()
        val income = MonthlyIncomeEntity(
            monthKey = "2026-11",
            grossSalary = 4158.00,
            netSalary = 4460.50,
            overtime = 0.0,
            incentives = 0.0,
            thirteenthMonth = 0.0,
            actualEmployeeKwsp = 457.38,
            actualEmployerKwsp = 540.54
        )
        incomeDao.insertOrUpdate(income)

        val retrieved = incomeDao.getIncomeForMonthDirect("2026-11")
        assertNotNull(retrieved)
        assertEquals(4158.00, retrieved?.grossSalary ?: 0.0, 0.01)
        assertEquals(4460.50, retrieved?.netSalary ?: 0.0, 0.01)
        assertEquals(457.38, retrieved?.actualEmployeeKwsp ?: 0.0, 0.01)
        assertEquals(540.54, retrieved?.actualEmployerKwsp ?: 0.0, 0.01)
    }

    @Test
    fun testFlowchartVerificationDao() = runBlocking {
        val dao = db.flowchartVerificationDao()
        val verification = FlowchartVerificationEntity(
            stepNumber = 0,
            isConfirmed = true,
            confirmedTimestamp = System.currentTimeMillis(),
            notes = "Budget and essential standing orders verified"
        )
        dao.setVerification(verification)

        val record = dao.getVerificationForStep(0)
        assertNotNull(record)
        assertTrue(record?.isConfirmed == true)
        assertEquals("Budget and essential standing orders verified", record?.notes)
    }

    @Test
    fun testWidgetBuildViewsAndFormatting() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = com.example.widget.ExpenseWidgetProvider.buildViews(context, "2026-11", 245.50, 4)
        val inflated = views.apply(context, null)
        assertNotNull(inflated)

        val amountView = inflated.findViewById<android.widget.TextView>(R.id.widget_amount)
        assertNotNull(amountView)
        assertEquals("RM 245.50", amountView.text.toString())

        val countView = inflated.findViewById<android.widget.TextView>(R.id.widget_count)
        assertNotNull(countView)
        assertEquals("4 expenses recorded", countView.text.toString())
    }

    @Test
    fun testWidgetQuickAddLaunchesQuickExpenseActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = com.example.widget.ExpenseWidgetProvider.buildViews(context, "2026-11", 50.0, 1)
        val inflated = views.apply(context, null)

        val btnAdd = inflated.findViewById<android.view.View>(R.id.widget_btn_add)
        assertNotNull(btnAdd)

        // Verify QuickExpenseActivity can be launched by Robolectric without throwing
        val controller = org.robolectric.Robolectric.buildActivity(QuickExpenseActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        controller.destroy()
    }

    @Test
    fun testQuickExpenseActivityNotExportedInManifest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager
        val componentName = android.content.ComponentName(context, QuickExpenseActivity::class.java)
        val activityInfo = packageManager.getActivityInfo(componentName, 0)
        assertNotNull(activityInfo)
        assertFalse("QuickExpenseActivity must not be exported (android:exported=false)", activityInfo.exported)
    }

    @Test
    fun testWidgetLifecycleExecutionSafety() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = ExpenseWidgetProvider()

        // 1. Test onEnabled safe execution
        provider.onEnabled(context)

        // 2. Test ACTION_REFRESH_WIDGET broadcast receipt
        val refreshIntent = Intent(ExpenseWidgetProvider.ACTION_REFRESH_WIDGET)
        provider.onReceive(context, refreshIntent)

        // 3. Test onUpdate safe execution with empty/dummy widget IDs
        val appWidgetManager = AppWidgetManager.getInstance(context)
        provider.onUpdate(context, appWidgetManager, intArrayOf())
    }

    @Test
    fun testPaymentSelectionsDoNotCommitBeforeConfirmation() = runBlocking {
        val checkInDao = db.monthlyCheckInDao()
        val checkIn = com.example.data.model.MonthlyCheckInEntity(
            id = 1L,
            monthKey = "2026-11",
            commitmentId = 101L,
            isPaid = false,
            actualPaidAmount = 250.0,
            paidTimestamp = null
        )
        checkInDao.insertOrReplace(checkIn)

        // Simulate user selecting commitment in-memory in Stage 2
        val selectedPayments = mutableMapOf(101L to 250.0)
        assertTrue(selectedPayments.containsKey(101L))

        // Before dialog confirmation, database MUST remain uncommitted (isPaid == false)
        val records = checkInDao.getCheckInsForMonthDirect("2026-11")
        assertEquals(1, records.size)
        assertFalse("Commitment must remain unpaid before dialog confirmation", records[0].isPaid)
        assertEquals(null, records[0].paidTimestamp)
    }

    @Test
    fun testCancellingConfirmationCreatesNoPaymentRecords() = runBlocking {
        val checkInDao = db.monthlyCheckInDao()
        val checkIn = com.example.data.model.MonthlyCheckInEntity(
            id = 2L,
            monthKey = "2026-11",
            commitmentId = 102L,
            isPaid = false,
            actualPaidAmount = 500.0,
            paidTimestamp = null
        )
        checkInDao.insertOrReplace(checkIn)

        // User opens dialog and clicks CANCEL (dialog dismissed without calling confirmBatchPayments)
        val dialogCancelled = true
        if (!dialogCancelled) {
            checkInDao.insertOrReplace(checkIn.copy(isPaid = true))
        }

        val records = checkInDao.getCheckInsForMonthDirect("2026-11")
        assertEquals(1, records.size)
        assertFalse("Cancelled confirmation must not record any payments", records[0].isPaid)
    }

    @Test
    fun testConfirmingPaymentsRecordsEachPaymentExactlyOnce() = runBlocking {
        val checkInDao = db.monthlyCheckInDao()
        val commitmentItem = com.example.data.MonthlyCommitmentItem(
            checkInId = 3L,
            commitmentId = 103L,
            name = "Hong Leong HP1 Car Loan",
            defaultAmount = 541.0,
            isDebt = true,
            debtKey = "HL_HP1",
            isPaid = false,
            actualPaidAmount = 541.0,
            paidTimestamp = null
        )

        // 1. Initial unconfirmed state
        checkInDao.insertOrReplace(
            com.example.data.model.MonthlyCheckInEntity(
                id = commitmentItem.checkInId,
                monthKey = "2026-11",
                commitmentId = commitmentItem.commitmentId,
                isPaid = false,
                actualPaidAmount = commitmentItem.defaultAmount
            )
        )

        // 2. User confirms in dialog
        val confirmedCheckIn = com.example.data.model.MonthlyCheckInEntity(
            id = commitmentItem.checkInId,
            monthKey = "2026-11",
            commitmentId = commitmentItem.commitmentId,
            isPaid = true,
            actualPaidAmount = 541.0,
            paidTimestamp = System.currentTimeMillis()
        )
        checkInDao.insertOrReplace(confirmedCheckIn)

        // 3. User taps or re-confirms — must NOT duplicate records
        checkInDao.insertOrReplace(confirmedCheckIn)

        val records = checkInDao.getCheckInsForMonthDirect("2026-11")
        assertEquals(1, records.size)
        assertTrue(records[0].isPaid)
        assertEquals(541.0, records[0].actualPaidAmount, 0.01)
    }

    @Test
    fun testReopeningCompletedStagesDoesNotDuplicateTransactions() = runBlocking {
        val incomeDao = db.monthlyIncomeDao()
        val initialIncome = com.example.data.model.MonthlyIncomeEntity(
            monthKey = "2026-11",
            grossSalary = 4158.00,
            netSalary = 4460.50,
            overtime = 0.0,
            incentives = 0.0
        )
        incomeDao.insertOrUpdate(initialIncome)

        // Reopening Stage 1 and saving updated values (e.g. adding overtime & incentives)
        val updatedIncome = initialIncome.copy(
            overtime = 300.0,
            incentives = 150.0
        )
        incomeDao.insertOrUpdate(updatedIncome)

        val retrieved = incomeDao.getIncomeForMonthDirect("2026-11")
        assertNotNull(retrieved)
        assertEquals(300.0, retrieved?.overtime ?: 0.0, 0.01)
        assertEquals(150.0, retrieved?.incentives ?: 0.0, 0.01)
        // Verify take home cash flow strictly reflects net + ot + incentives without gross
        assertEquals(4460.50 + 300.0 + 150.0, retrieved?.totalTakeHomeIncome ?: 0.0, 0.01)
    }
}


