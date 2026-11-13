package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.model.MonthlyIncomeEntity
import com.example.data.model.PaydayAllocationEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaydayIntelligenceDbTest {

    @Test
    fun scenario7And8_userOverridesAndReopeningCheckInPreservesApprovedRecordsAndMigration2To3() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify MIGRATION_2_3 upgrades v2 database without losing existing monthly_incomes rows
        val dbName = "migration_test_db_${System.currentTimeMillis()}"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `monthly_incomes` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `monthKey` TEXT NOT NULL,
                                `grossSalary` REAL NOT NULL,
                                `netSalary` REAL NOT NULL,
                                `overtime` REAL NOT NULL,
                                `incentives` REAL NOT NULL,
                                `thirteenthMonth` REAL NOT NULL,
                                `otherIncome` REAL NOT NULL,
                                `actualEmployeeKwsp` REAL,
                                `actualEmployerKwsp` REAL,
                                `updatedTimestamp` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_monthly_incomes_monthKey` ON `monthly_incomes` (`monthKey`)")
                        db.execSQL(
                            """
                            INSERT INTO monthly_incomes (monthKey, grossSalary, netSalary, overtime, incentives, thirteenthMonth, otherIncome, updatedTimestamp)
                            VALUES ('2026-10', 4158.0, 4460.5, 100.0, 0.0, 0.0, 0.0, 1000)
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val v2Db = helper.writableDatabase
        AppDatabase.MIGRATION_2_3.migrate(v2Db)
        val cursor = v2Db.query("SELECT monthKey, reimbursements, unconfirmedIncome, isConfirmedReceived FROM monthly_incomes WHERE monthKey = '2026-10'")
        assertTrue(cursor.moveToFirst())
        assertEquals("2026-10", cursor.getString(0))
        assertEquals(0.0, cursor.getDouble(1), 0.01)
        assertEquals(0.0, cursor.getDouble(2), 0.01)
        assertEquals(1, cursor.getInt(3))
        cursor.close()
        v2Db.close()
        context.deleteDatabase(dbName)

        // 2. Verify in-memory Room DB preserves recommended vs user-approved allocations on reopen
        val roomDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val incomeDao = roomDb.monthlyIncomeDao()
            incomeDao.insertOrUpdate(
                MonthlyIncomeEntity(
                    monthKey = "2026-11",
                    netSalary = 4460.50,
                    reimbursements = 320.0,
                    unconfirmedIncome = 500.0,
                    isConfirmedReceived = true
                )
            )
            val savedIncome = incomeDao.getIncomeForMonthDirect("2026-11")
            assertNotNull(savedIncome)
            assertEquals(320.0, savedIncome!!.reimbursements, 0.01)
            assertEquals(500.0, savedIncome.unconfirmedIncome, 0.01)
            assertEquals(4460.50, savedIncome.totalTakeHomeIncome, 0.01)

            val dao = roomDb.paydayAllocationDao()
            val initialApproved = PaydayAllocationEntity(
                monthKey = "2026-11",
                confirmedAvailableCash = 5000.0,
                recEssentialCommitments = 1250.0,
                recMinDebtPayments = 2263.33,
                recUpcomingEssentials = 400.0,
                recExtraDebtRepayment = 600.0,
                recExtraDebtTargetKey = "SHOPEE_SLOAN",
                recEmergencySavings = 200.0,
                recDiscretionaryAllowance = 136.67,
                recRemainingBuffer = 150.0,
                approvedExtraDebtRepayment = 450.0,
                approvedEmergencySavings = 350.0,
                approvedDiscretionaryAllowance = 136.67,
                approvedRemainingBuffer = 150.0,
                hasOverrides = true,
                isApproved = true,
                approvedTimestamp = 1700000000000L,
                rationaleSummary = "Custom split"
            )
            dao.insertOrUpdate(initialApproved)

            val reopened = dao.getAllocationForMonthDirect("2026-11")
            assertNotNull(reopened)
            assertEquals(600.0, reopened!!.recExtraDebtRepayment, 0.01)
            assertEquals(450.0, reopened.approvedExtraDebtRepayment, 0.01)
            assertEquals(200.0, reopened.recEmergencySavings, 0.01)
            assertEquals(350.0, reopened.approvedEmergencySavings, 0.01)
            assertTrue(reopened.hasOverrides)
            assertTrue(reopened.isApproved)

            // Re-saving same month updates existing record cleanly without duplication
            dao.insertOrUpdate(reopened.copy(approvedExtraDebtRepayment = 500.0))
            val updated = dao.getAllocationForMonthDirect("2026-11")
            assertEquals(500.0, updated!!.approvedExtraDebtRepayment, 0.01)
        } finally {
            roomDb.close()
        }
    }
}
