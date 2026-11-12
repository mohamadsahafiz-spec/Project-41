package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.model.ExpenseEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun readAppNameFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Freedom Tracker", appName)
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
}
