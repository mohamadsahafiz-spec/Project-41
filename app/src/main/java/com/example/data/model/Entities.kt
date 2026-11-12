package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String, // Groceries, Petrol, Utilities, Family Dining, Other
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val monthKey: String // e.g. "2026-11"
)

@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultAmount: Double,
    val isDebt: Boolean = false,
    val debtKey: String? = null // e.g. "MAYBANK_CC", "HL_HP1", "AMBANK_HP2", "SHOPEE_SLOAN"
)

@Entity(
    tableName = "monthly_checkins",
    indices = [Index(value = ["monthKey", "commitmentId"], unique = true)]
)
data class MonthlyCheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String, // "2026-11"
    val commitmentId: Long,
    val isPaid: Boolean = false,
    val actualPaidAmount: Double = 0.0,
    val paidTimestamp: Long? = null
)

@Entity(tableName = "financial_profile")
data class FinancialProfileEntity(
    @PrimaryKey val id: Int = 1,
    val netMonthlySalary: Double = 4460.50,
    val basicSalary: Double = 4158.00,
    val bankBalance: Double = 1039.56,
    val dedicatedSavings: Double = 0.0,
    val asb: Double = 0.0,
    val tabungHaji: Double = 0.0,
    val fixedDeposits: Double = 0.0,
    val otherInvestments: Double = 0.0,
    val kwspAccount1: Double = 92783.88,
    val kwspAccount2: Double = 6231.14,
    val kwspAccount3: Double = 164.70,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val totalKwsp: Double
        get() = kwspAccount1 + kwspAccount2 + kwspAccount3

    val totalAccessibleSavings: Double
        get() = bankBalance + dedicatedSavings + asb + tabungHaji + fixedDeposits + otherInvestments
}

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String, // "MAYBANK_CC", "HL_HP1", "AMBANK_HP2", "SHOPEE_SLOAN"
    val name: String,
    val initialBalance: Double,
    val currentBalance: Double,
    val monthlyTarget: Double,
    val aprPercent: Double,
    val isHighInterest: Boolean // CC (>15%) and SLoan (~18%) are true; HP1 & HP2 (~5-7%) are false
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val key: String,
    val title: String,
    val description: String,
    val category: String,
    val isUnlocked: Boolean = false,
    val unlockedTimestamp: Long? = null
)
