package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.model.ExpenseEntity
import com.example.data.model.FlowchartVerificationEntity
import com.example.data.model.MonthlyIncomeEntity
import com.example.data.model.SavingsAccountEntity
import com.example.data.model.SavingsTransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
}
