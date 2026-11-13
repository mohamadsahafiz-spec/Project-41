package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.FinancialRepository
import com.example.data.model.CommitmentEntity
import com.example.data.model.DebtEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.MonthlyCheckInProgressEntity
import com.example.data.model.MonthlyIncomeEntity
import com.example.data.model.PaydayAllocationEntity
import com.example.logic.CashSurplusStatus
import com.example.logic.DebtExtraAllocationItem
import com.example.logic.DebtRateClassification
import com.example.logic.PaydayIntelligenceEngine
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun scenario7And8_userOverridesAndReopeningCheckInPreservesApprovedRecordsAndMigrations() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify MIGRATION_2_3 -> MIGRATION_3_4 upgrades v2 -> v4 without losing rows
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
        v2Db.execSQL(
            """
            INSERT INTO payday_allocations (
                monthKey, confirmedAvailableCash, reimbursementsExcluded, unconfirmedIncomeExcluded,
                recEssentialCommitments, recMinDebtPayments, recUpcomingEssentials,
                recExtraDebtRepayment, recExtraDebtTargetKey, recEmergencySavings, recDiscretionaryAllowance, recRemainingBuffer,
                approvedExtraDebtRepayment, approvedEmergencySavings, approvedDiscretionaryAllowance, approvedRemainingBuffer,
                hasOverrides, isApproved, approvedTimestamp, rationaleSummary, updatedTimestamp
            ) VALUES (
                '2026-10', 4500.0, 0.0, 0.0, 1000.0, 2000.0, 400.0,
                600.0, 'SHOPEE_SLOAN', 200.0, 150.0, 150.0,
                600.0, 200.0, 150.0, 150.0,
                0, 1, 1000, 'Migrated v3 plan', 1000
            )
            """.trimIndent()
        )
        AppDatabase.MIGRATION_3_4.migrate(v2Db)

        val incomeCursor = v2Db.query("SELECT monthKey, reimbursements, unconfirmedIncome, isConfirmedReceived FROM monthly_incomes WHERE monthKey = '2026-10'")
        assertTrue(incomeCursor.moveToFirst())
        assertEquals("2026-10", incomeCursor.getString(0))
        incomeCursor.close()

        val allocCursor = v2Db.query("SELECT monthKey, cashSourceMode, recDebtAllocationsJson, approvedExtraDebtRepayment FROM payday_allocations WHERE monthKey = '2026-10'")
        assertTrue(allocCursor.moveToFirst())
        assertEquals("2026-10", allocCursor.getString(0))
        assertEquals("OPENING_PLUS_CASHFLOW", allocCursor.getString(1))
        assertEquals("[]", allocCursor.getString(2))
        assertEquals(600.0, allocCursor.getDouble(3), 0.01)
        allocCursor.close()

        v2Db.execSQL("INSERT INTO monthly_checkin_progress VALUES ('2026-10', 1, 1, 0, 0, 1000)")
        val progCursor = v2Db.query("SELECT stage3Reviewed, stage4Confirmed, stage5Reviewed FROM monthly_checkin_progress WHERE monthKey = '2026-10'")
        assertTrue(progCursor.moveToFirst())
        assertEquals(1, progCursor.getInt(0))
        assertEquals(1, progCursor.getInt(1))
        assertEquals(0, progCursor.getInt(2))
        progCursor.close()

        v2Db.close()
        context.deleteDatabase(dbName)

        // 2. Verify in-memory Room DB preserves recommended vs user-approved allocations & stage progress
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

            // Check-In Progress DAO persists completed stages cleanly
            val progressDao = roomDb.monthlyCheckInProgressDao()
            progressDao.insertOrUpdate(
                MonthlyCheckInProgressEntity(
                    monthKey = "2026-11",
                    stage3Reviewed = true,
                    stage4Confirmed = true,
                    stage5Reviewed = false,
                    stage6Completed = false
                )
            )
            val savedProgress = progressDao.getProgressForMonthDirect("2026-11")
            assertNotNull(savedProgress)
            assertTrue(savedProgress!!.stage3Reviewed)
            assertTrue(savedProgress.stage4Confirmed)
            assertFalse(savedProgress.stage5Reviewed)
            assertFalse(savedProgress.stage6Completed)
        } finally {
            roomDb.close()
        }
    }

    @Test
    fun scenario8And9_jsonExportAndRestore_preservesPaydayAndIncomeHistory_andHandlesOldBackups() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val targetDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            // Seed baseline tables in sourceDb & targetDb (as prepopulated app DBs have)
            val initialProfile = FinancialProfileEntity(bankBalance = 2500.0, netMonthlySalary = 4460.50)
            val initialDebts = listOf(
                DebtEntity(1, "MAYBANK_CC", "Maybank Credit Card", 10093.43, 8500.0, 1200.0, 15.0, true),
                DebtEntity(2, "SHOPEE_SLOAN", "Shopee SLoan", 1983.33, 500.0, 283.33, 18.0, true)
            )
            val initialCommitments = listOf(CommitmentEntity(1, "Rent", 750.0, false, null))

            sourceDb.financialProfileDao().insertOrUpdate(initialProfile)
            sourceDb.debtDao().insertAll(initialDebts)
            sourceDb.commitmentDao().insertAll(initialCommitments)

            targetDb.financialProfileDao().insertOrUpdate(FinancialProfileEntity())
            targetDb.debtDao().insertAll(initialDebts.map { it.copy(currentBalance = it.initialBalance) })
            targetDb.commitmentDao().insertAll(listOf(CommitmentEntity(1, "Rent", 700.0, false, null)))

            val sourceRepo = FinancialRepository(context, sourceDb)
            val targetRepo = FinancialRepository(context, targetDb)

            // Save monthly income, multi-debt PaydayAllocation, and Check-In progress in sourceRepo
            sourceRepo.saveMonthlyIncome(
                MonthlyIncomeEntity(
                    monthKey = "2026-11",
                    grossSalary = 4158.0,
                    netSalary = 4460.50,
                    overtime = 250.0,
                    incentives = 400.0,
                    reimbursements = 180.0,
                    unconfirmedIncome = 300.0,
                    isConfirmedReceived = true
                )
            )
            val recDebtJson = PaydayIntelligenceEngine.serializeDebtAllocations(
                listOf(
                    DebtExtraAllocationItem("SHOPEE_SLOAN", "Shopee SLoan", 18.0, DebtRateClassification.PROVISIONAL_RATE, 500.0, 500.0),
                    DebtExtraAllocationItem("MAYBANK_CC", "Maybank Credit Card", 15.0, DebtRateClassification.EFFECTIVE_APR, 8500.0, 700.0)
                )
            )
            val approvedDebtJson = PaydayIntelligenceEngine.serializeDebtAllocations(
                listOf(
                    DebtExtraAllocationItem("SHOPEE_SLOAN", "Shopee SLoan", 18.0, DebtRateClassification.PROVISIONAL_RATE, 500.0, 500.0),
                    DebtExtraAllocationItem("MAYBANK_CC", "Maybank Credit Card", 15.0, DebtRateClassification.EFFECTIVE_APR, 8500.0, 400.0)
                )
            )
            sourceRepo.savePaydayAllocation(
                PaydayAllocationEntity(
                    monthKey = "2026-11",
                    cashSourceMode = "CURRENT_BANK_BALANCE",
                    openingCashInput = 1039.56,
                    confirmedAvailableCash = 5110.50,
                    calculatedMonthlyCashflow = 4000.0,
                    reimbursementsExcluded = 180.0,
                    unconfirmedIncomeExcluded = 300.0,
                    upcomingEssentialExpensesInput = 400.0,
                    emergencyFundTargetInput = 1000.0,
                    cashBufferTargetInput = 200.0,
                    minCashBufferEnforced = 200.0,
                    recEssentialCommitments = 750.0,
                    recMinDebtPayments = 1483.33,
                    recUpcomingEssentials = 400.0,
                    recExtraDebtRepayment = 1200.0,
                    recExtraDebtTargetKey = "SHOPEE_SLOAN",
                    recDebtAllocationsJson = recDebtJson,
                    recEmergencySavings = 300.0,
                    recDiscretionaryAllowance = 200.0,
                    recRemainingBuffer = 777.17,
                    approvedExtraDebtRepayment = 900.0,
                    approvedDebtAllocationsJson = approvedDebtJson,
                    approvedEmergencySavings = 450.0,
                    approvedDiscretionaryAllowance = 150.0,
                    approvedRemainingBuffer = 977.17,
                    hasOverrides = true,
                    isApproved = true,
                    approvedTimestamp = 1700001234000L,
                    rationaleSummary = "Approved custom cascade"
                )
            )
            sourceRepo.saveCheckInProgress(
                MonthlyCheckInProgressEntity(
                    monthKey = "2026-11",
                    stage3Reviewed = true,
                    stage4Confirmed = true,
                    stage5Reviewed = true,
                    stage6Completed = false
                )
            )

            // Scenario 8: Export from source and restore into target
            val exportedJson = sourceRepo.exportDataAsJson()
            val rootObj = JSONObject(exportedJson)
            assertEquals("3.0", rootObj.getString("version"))
            assertTrue(rootObj.has("monthly_incomes"))
            assertTrue(rootObj.has("payday_allocations"))
            assertTrue(rootObj.has("monthly_checkin_progress"))

            val restoreSuccess = targetRepo.restoreDataFromJson(exportedJson)
            assertTrue(restoreSuccess)

            val restoredIncome = targetDb.monthlyIncomeDao().getIncomeForMonthDirect("2026-11")
            assertNotNull(restoredIncome)
            assertEquals(250.0, restoredIncome!!.overtime, 0.01)
            assertEquals(180.0, restoredIncome.reimbursements, 0.01)

            val restoredAlloc = targetDb.paydayAllocationDao().getAllocationForMonthDirect("2026-11")
            assertNotNull(restoredAlloc)
            assertEquals("CURRENT_BANK_BALANCE", restoredAlloc!!.cashSourceMode)
            assertEquals(1200.0, restoredAlloc.recExtraDebtRepayment, 0.01)
            assertEquals(900.0, restoredAlloc.approvedExtraDebtRepayment, 0.01)
            assertEquals(300.0, restoredAlloc.recEmergencySavings, 0.01)
            assertEquals(450.0, restoredAlloc.approvedEmergencySavings, 0.01)
            val restoredApprovedTargets = PaydayIntelligenceEngine.deserializeDebtAllocations(restoredAlloc.approvedDebtAllocationsJson)
            assertEquals(2, restoredApprovedTargets.size)
            assertEquals("SHOPEE_SLOAN", restoredApprovedTargets[0].debtKey)
            assertEquals(500.0, restoredApprovedTargets[0].allocatedAmount, 0.01)
            assertEquals("MAYBANK_CC", restoredApprovedTargets[1].debtKey)
            assertEquals(400.0, restoredApprovedTargets[1].allocatedAmount, 0.01)

            val restoredProgress = targetDb.monthlyCheckInProgressDao().getProgressForMonthDirect("2026-11")
            assertNotNull(restoredProgress)
            assertTrue(restoredProgress!!.stage3Reviewed)
            assertTrue(restoredProgress.stage4Confirmed)
            assertTrue(restoredProgress.stage5Reviewed)
            assertFalse(restoredProgress.stage6Completed)

            // Scenario 9: Restoring older V2.0 backup without Payday Intelligence / Income / Progress records
            val legacyV2Json = JSONObject().apply {
                put("version", "2.0")
                put(
                    "profile",
                    JSONObject().apply {
                        put("netMonthlySalary", 4800.0)
                        put("bankBalance", 1800.0)
                    }
                )
                put(
                    "debts",
                    JSONArray().apply {
                        put(JSONObject().apply {
                            put("key", "SHOPEE_SLOAN")
                            put("currentBalance", 250.0)
                        })
                    }
                )
                put(
                    "commitments",
                    JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", 1)
                            put("defaultAmount", 820.0)
                        })
                    }
                )
            }.toString()

            val legacyRestoreSuccess = targetRepo.restoreDataFromJson(legacyV2Json)
            assertTrue(legacyRestoreSuccess)
            assertEquals(1800.0, targetDb.financialProfileDao().getProfileDirect()!!.bankBalance, 0.01)
            assertEquals(250.0, targetDb.debtDao().getDebtByKey("SHOPEE_SLOAN")!!.currentBalance, 0.01)
            assertEquals(820.0, targetDb.commitmentDao().getAllCommitmentsDirect().first().defaultAmount, 0.01)
        } finally {
            sourceDb.close()
            targetDb.close()
        }
    }

    @Test
    fun v111_task1_userUpdatedDebtAprSurvivesDatabaseReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "apr_persistence_test_${System.currentTimeMillis()}"

        try {
            val dbFirstOpen = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
                .addCallback(AppDatabase.prepopulateCallback())
                .allowMainThreadQueries()
                .build()

            // Fresh prepopulation sets initial reference rates
            val initialDebts = dbFirstOpen.debtDao().getAllDebtsDirect().associateBy { it.key }
            assertEquals(15.0, initialDebts.getValue("MAYBANK_CC").aprPercent, 0.01)
            assertEquals(18.0, initialDebts.getValue("SHOPEE_SLOAN").aprPercent, 0.01)
            assertEquals(3.0, initialDebts.getValue("HL_HP1").aprPercent, 0.01)
            assertEquals(3.0, initialDebts.getValue("AMBANK_HP2").aprPercent, 0.01)

            // User updates debt APRs manually
            val updatedCc = initialDebts.getValue("MAYBANK_CC").copy(aprPercent = 17.5)
            val updatedSloan = initialDebts.getValue("SHOPEE_SLOAN").copy(aprPercent = 21.25)
            dbFirstOpen.debtDao().update(updatedCc)
            dbFirstOpen.debtDao().update(updatedSloan)
            dbFirstOpen.close()

            // Reopen database with the exact same prepopulate/open callback
            val dbReopened = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
                .addCallback(AppDatabase.prepopulateCallback())
                .allowMainThreadQueries()
                .build()
            try {
                val reopenedDebts = dbReopened.debtDao().getAllDebtsDirect().associateBy { it.key }
                assertEquals(17.5, reopenedDebts.getValue("MAYBANK_CC").aprPercent, 0.01)
                assertEquals(21.25, reopenedDebts.getValue("SHOPEE_SLOAN").aprPercent, 0.01)
                assertEquals(3.0, reopenedDebts.getValue("HL_HP1").aprPercent, 0.01)
                assertEquals(3.0, reopenedDebts.getValue("AMBANK_HP2").aprPercent, 0.01)
            } finally {
                dbReopened.close()
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun v111_task2_debtAprAndMetadataSurviveJsonRoundTrip_andLegacyBackupDoesNotFabricateMissingRates() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val targetDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            // Deliberately modified debt rates, targets, names, and high-interest classification
            sourceDb.financialProfileDao().insertOrUpdate(FinancialProfileEntity())
            sourceDb.debtDao().insertAll(
                listOf(
                    DebtEntity(1, "MAYBANK_CC", "Maybank CC Gold", 12000.0, 9123.45, 1350.0, 22.5, isHighInterest = true),
                    DebtEntity(2, "SHOPEE_SLOAN", "Shopee SLoan Refinanced", 2500.0, 800.0, 220.0, 11.5, isHighInterest = false),
                    DebtEntity(3, "HL_HP1", "Hong Leong Car HP1", 7267.0, 6100.0, 530.0, 4.2, isHighInterest = false)
                )
            )

            // Target starts with factory baseline rates
            targetDb.financialProfileDao().insertOrUpdate(FinancialProfileEntity())
            targetDb.debtDao().insertAll(
                listOf(
                    DebtEntity(1, "MAYBANK_CC", "Maybank Credit Card", 10093.43, 10093.43, 1200.0, 15.0, isHighInterest = true),
                    DebtEntity(2, "SHOPEE_SLOAN", "Shopee SLoan", 1983.33, 1983.33, 283.33, 18.0, isHighInterest = true),
                    DebtEntity(3, "HL_HP1", "Hong Leong Car HP1", 7267.0, 7267.0, 530.0, 3.0, isHighInterest = false)
                )
            )

            val sourceRepo = FinancialRepository(context, sourceDb)
            val targetRepo = FinancialRepository(context, targetDb)

            // 1. Round-trip with deliberately changed APR and isHighInterest metadata
            val exported = sourceRepo.exportDataAsJson()
            assertTrue(targetRepo.restoreDataFromJson(exported))

            val restoredCc = targetDb.debtDao().getDebtByKey("MAYBANK_CC")!!
            assertEquals("Maybank CC Gold", restoredCc.name)
            assertEquals(12000.0, restoredCc.initialBalance, 0.01)
            assertEquals(9123.45, restoredCc.currentBalance, 0.01)
            assertEquals(1350.0, restoredCc.monthlyTarget, 0.01)
            assertEquals(22.5, restoredCc.aprPercent, 0.01)
            assertTrue(restoredCc.isHighInterest)

            val restoredSloan = targetDb.debtDao().getDebtByKey("SHOPEE_SLOAN")!!
            assertEquals("Shopee SLoan Refinanced", restoredSloan.name)
            assertEquals(800.0, restoredSloan.currentBalance, 0.01)
            assertEquals(11.5, restoredSloan.aprPercent, 0.01)
            assertFalse(restoredSloan.isHighInterest)

            val restoredHp1 = targetDb.debtDao().getDebtByKey("HL_HP1")!!
            assertEquals(4.2, restoredHp1.aprPercent, 0.01)
            assertEquals(6100.0, restoredHp1.currentBalance, 0.01)
            assertFalse(restoredHp1.isHighInterest)

            // 2. Legacy backup without aprPercent / isHighInterest preserves existing metadata and never fabricates missing rate for unknown debt
            val legacyNoAprJson = JSONObject().apply {
                put("version", "1.0")
                put(
                    "debts",
                    JSONArray().apply {
                        put(JSONObject().apply {
                            put("key", "MAYBANK_CC")
                            put("currentBalance", 7777.0)
                        })
                        put(JSONObject().apply {
                            put("key", "LEGACY_CUSTOM_DEBT")
                            put("name", "Legacy Unrated Loan")
                            put("currentBalance", 1500.0)
                        })
                    }
                )
            }.toString()

            assertTrue(targetRepo.restoreDataFromJson(legacyNoAprJson))

            // Existing debt kept its non-legacy APR & classification while updating balance
            val ccAfterLegacy = targetDb.debtDao().getDebtByKey("MAYBANK_CC")!!
            assertEquals(7777.0, ccAfterLegacy.currentBalance, 0.01)
            assertEquals(22.5, ccAfterLegacy.aprPercent, 0.01)
            assertTrue(ccAfterLegacy.isHighInterest)

            // New debt from legacy backup without APR metadata does NOT fabricate an interest rate
            val legacyDebt = targetDb.debtDao().getDebtByKey("LEGACY_CUSTOM_DEBT")!!
            assertEquals(1500.0, legacyDebt.currentBalance, 0.01)
            assertEquals(0.0, legacyDebt.aprPercent, 0.01)
            assertFalse(legacyDebt.isHighInterest)
            assertEquals(DebtRateClassification.MISSING_RATE, PaydayIntelligenceEngine.classifyDebtRate(legacyDebt))
        } finally {
            sourceDb.close()
            targetDb.close()
        }
    }
}
