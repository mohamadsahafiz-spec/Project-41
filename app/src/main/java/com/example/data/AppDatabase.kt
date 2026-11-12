package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AchievementDao
import com.example.data.dao.CommitmentDao
import com.example.data.dao.DebtDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.FinancialProfileDao
import com.example.data.dao.FlowchartVerificationDao
import com.example.data.dao.MonthlyCheckInDao
import com.example.data.dao.MonthlyIncomeDao
import com.example.data.dao.SavingsAccountDao
import com.example.data.dao.SavingsTransactionDao
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

@Database(
    entities = [
        ExpenseEntity::class,
        CommitmentEntity::class,
        MonthlyCheckInEntity::class,
        FinancialProfileEntity::class,
        DebtEntity::class,
        AchievementEntity::class,
        SavingsAccountEntity::class,
        SavingsTransactionEntity::class,
        MonthlyIncomeEntity::class,
        FlowchartVerificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun commitmentDao(): CommitmentDao
    abstract fun monthlyCheckInDao(): MonthlyCheckInDao
    abstract fun financialProfileDao(): FinancialProfileDao
    abstract fun debtDao(): DebtDao
    abstract fun achievementDao(): AchievementDao
    abstract fun savingsAccountDao(): SavingsAccountDao
    abstract fun savingsTransactionDao(): SavingsTransactionDao
    abstract fun monthlyIncomeDao(): MonthlyIncomeDao
    abstract fun flowchartVerificationDao(): FlowchartVerificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "freedom_tracker_db" // Preserving database name to preserve existing data
                )
                    .addCallback(DatabasePrepopulateCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabasePrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.beginTransaction()
            try {
                // Initial Profile: Everyday Cash = RM1,039.56, Dedicated Savings = RM0.00
                db.execSQL(
                    """
                    INSERT INTO financial_profile (
                        id, netMonthlySalary, basicSalary, bankBalance, dedicatedSavings, 
                        asb, tabungHaji, fixedDeposits, otherInvestments, 
                        kwspAccount1, kwspAccount2, kwspAccount3, lastUpdated
                    ) VALUES (
                        1, 4460.50, 4158.00, 1039.56, 0.0, 
                        0.0, 0.0, 0.0, 0.0, 
                        92783.88, 6231.14, 164.70, ${System.currentTimeMillis()}
                    )
                    """.trimIndent()
                )

                // Initial Debts:
                db.execSQL(
                    """
                    INSERT INTO debts (`key`, name, initialBalance, currentBalance, monthlyTarget, aprPercent, isHighInterest)
                    VALUES 
                    ('MAYBANK_CC', 'Maybank Credit Card', 10093.43, 10093.43, 1200.0, 18.0, 1),
                    ('SHOPEE_SLOAN', 'Shopee SLoan', 1983.33, 1983.33, 283.33, 18.0, 1),
                    ('HL_HP1', 'Hong Leong Car HP1', 7267.0, 7267.0, 530.0, 6.0, 0),
                    ('AMBANK_HP2', 'AmBank Car HP2', 13301.0, 13301.0, 250.0, 6.0, 0)
                    """.trimIndent()
                )

                // Initial Commitments:
                db.execSQL(
                    """
                    INSERT INTO commitments (name, defaultAmount, isDebt, debtKey)
                    VALUES 
                    ('Rent', 700.0, 0, NULL),
                    ('Maybank Credit Card', 1200.0, 1, 'MAYBANK_CC'),
                    ('Hong Leong Car HP1', 530.0, 1, 'HL_HP1'),
                    ('AmBank Car HP2 (Contribution)', 250.0, 1, 'AMBANK_HP2'),
                    ('Parents', 200.0, 0, NULL),
                    ('Mother''s House Fibre', 95.0, 0, NULL),
                    ('Playschool', 550.0, 0, NULL),
                    ('Shopee SLoan', 283.33, 1, 'SHOPEE_SLOAN'),
                    ('Formula', 100.0, 0, NULL)
                    """.trimIndent()
                )

                // Initial Achievements:
                // STARTER_EMERGENCY must be LOCKED (0) because dedicated emergency savings is RM0!
                db.execSQL(
                    """
                    INSERT INTO achievements (`key`, title, description, category, isUnlocked, unlockedTimestamp)
                    VALUES 
                    ('FIRST_EXPENSE', 'First Step Taken', 'Record your first quick expense entry.', 'Habit', 0, NULL),
                    ('STARTER_EMERGENCY', 'Starter Buffer', 'Reach RM1,000 in dedicated emergency reserve (Flowchart Step 1).', 'Savings', 0, NULL),
                    ('SAVINGS_500', 'First RM500 Saved', 'Build RM500 in dedicated emergency savings reserve.', 'Savings', 0, NULL),
                    ('SLOAN_CLEARED', 'SLoan Liberated', 'Fully clear the Shopee SLoan balance to RM0.', 'Debt', 0, NULL),
                    ('CREDIT_CARD_CLEARED', 'Credit Card Zeroed', 'Maybank credit card debt completely paid off.', 'Debt', 0, NULL),
                    ('CONSUMER_DEBT_FREE', 'High-Interest Free', 'All high-interest debt eliminated! Flowchart Step 3 completed.', 'Debt', 0, NULL),
                    ('ONE_MONTH_EMERGENCY', '1-Month Safety Buffer', 'Accumulate 1 month of living expenses in dedicated savings (~RM3,300).', 'Savings', 0, NULL),
                    ('THREE_MONTH_EMERGENCY', '3-Month Solid Defense', 'Accumulate 3 months of emergency expenses in dedicated savings (~RM10,000).', 'Savings', 0, NULL),
                    ('HP1_CLEARED', 'Car HP1 Settled', 'Hong Leong vehicle hire purchase fully settled.', 'Debt', 0, NULL),
                    ('HP2_CLEARED', 'Car HP2 Settled', 'AmBank vehicle hire purchase fully settled.', 'Debt', 0, NULL),
                    ('TOTAL_DEBT_FREE', '100% Debt Free', 'Zero debt! Both credit cards and cars cleared. Flowchart Step 5 completed.', 'Debt', 0, NULL),
                    ('SIX_MONTH_EMERGENCY', 'Ironclad Emergency Reserve', 'Accumulate 6 months of living expenses in dedicated savings (~RM20,000).', 'Savings', 0, NULL),
                    ('KWSP_100K', 'KWSP Milestone', 'Total KWSP retirement balance exceeds RM100,000.', 'Retirement', 0, NULL),
                    ('FINANCIAL_FREEDOM_FOUNDATION', 'Freedom Horizon', 'Positive net worth advancing into Step 6 long-term investments.', 'Milestone', 0, NULL)
                    """.trimIndent()
                )

                // Initial Savings Vault: Default Emergency Fund account (balance RM0)
                db.execSQL(
                    """
                    INSERT INTO savings_accounts (name, purpose, isEmergencyFund, currentBalance, expectedAnnualReturnPercent, totalInterestCredited, createdTimestamp)
                    VALUES 
                    ('Emergency Fund (High-Yield / Liquid)', 'Emergency Fund', 1, 0.0, 3.5, 0.0, ${System.currentTimeMillis()})
                    """.trimIndent()
                )

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}
