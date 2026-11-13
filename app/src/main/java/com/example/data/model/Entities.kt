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
    val bankBalance: Double = 1039.56, // Category A: EVERYDAY CASH (current/transactional)
    val dedicatedSavings: Double = 0.0, // Category B: DEDICATED SAVINGS
    val asb: Double = 0.0,
    val tabungHaji: Double = 0.0,
    val fixedDeposits: Double = 0.0,
    val otherInvestments: Double = 0.0,
    val kwspAccount1: Double = 92783.88, // Category C: RETIREMENT ASSETS
    val kwspAccount2: Double = 6231.14,
    val kwspAccount3: Double = 164.70,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val totalKwsp: Double
        get() = kwspAccount1 + kwspAccount2 + kwspAccount3

    val totalEverydayCash: Double
        get() = bankBalance

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

/**
 * MY SAVINGS VAULT - Category B (Dedicated Savings accounts separated from Everyday Cash)
 */
@Entity(tableName = "savings_accounts")
data class SavingsAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "Maybank SavePlus", "Rize", "Versa Cash", "ASB"
    val purpose: String, // "Emergency Fund" or "Other Savings Goals"
    val isEmergencyFund: Boolean = true,
    val currentBalance: Double = 0.0,
    val expectedAnnualReturnPercent: Double? = null,
    val totalInterestCredited: Double = 0.0,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "savings_transactions")
data class SavingsTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val type: String, // "DEPOSIT", "WITHDRAWAL", "INTEREST"
    val amount: Double,
    val fromEverydayCash: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

/**
 * Monthly Income Check-In with Gross Salary for KWSP calculations and Net Salary for Cashflow
 */
@Entity(
    tableName = "monthly_incomes",
    indices = [Index(value = ["monthKey"], unique = true)]
)
data class MonthlyIncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String, // "yyyy-MM"
    val grossSalary: Double = 4158.00,
    val netSalary: Double = 4460.50,
    val overtime: Double = 0.0,
    val incentives: Double = 0.0,
    val thirteenthMonth: Double = 0.0,
    val otherIncome: Double = 0.0,
    val reimbursements: Double = 0.0,
    val unconfirmedIncome: Double = 0.0,
    val isConfirmedReceived: Boolean = true,
    val actualEmployeeKwsp: Double? = null,
    val actualEmployerKwsp: Double? = null,
    val updatedTimestamp: Long = System.currentTimeMillis()
) {
    val kwspContributableWages: Double
        get() = grossSalary + incentives + thirteenthMonth

    // Reimbursements and unconfirmed income must NEVER count as spendable take-home income
    val totalTakeHomeIncome: Double
        get() = if (isConfirmedReceived) {
            netSalary + overtime + incentives + thirteenthMonth + otherIncome
        } else {
            0.0
        }
}

/**
 * Payday Intelligence V1 allocation plan per month.
 * Persists engine-recommended figures separately from user-approved/overridden figures.
 */
@Entity(
    tableName = "payday_allocations",
    indices = [Index(value = ["monthKey"], unique = true)]
)
data class PaydayAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val confirmedAvailableCash: Double,
    val reimbursementsExcluded: Double = 0.0,
    val unconfirmedIncomeExcluded: Double = 0.0,
    val upcomingEssentialExpensesInput: Double? = null,
    val emergencyFundTargetInput: Double? = null,
    val cashBufferTargetInput: Double? = null,
    // Engine recommendations (immutable snapshot for this plan)
    val recEssentialCommitments: Double,
    val recMinDebtPayments: Double,
    val recUpcomingEssentials: Double,
    val recExtraDebtRepayment: Double,
    val recExtraDebtTargetKey: String? = null,
    val recEmergencySavings: Double,
    val recDiscretionaryAllowance: Double,
    val recRemainingBuffer: Double,
    // User-approved / overridden allocations
    val approvedExtraDebtRepayment: Double,
    val approvedEmergencySavings: Double,
    val approvedDiscretionaryAllowance: Double,
    val approvedRemainingBuffer: Double,
    val hasOverrides: Boolean = false,
    val isApproved: Boolean = false,
    val approvedTimestamp: Long? = null,
    val rationaleSummary: String = "",
    val updatedTimestamp: Long = System.currentTimeMillis()
)


/**
 * Flowchart Step Verification: Allows user review and confirmation for Step 0 (Budgeting) and Step 2 (Workplace match)
 */
@Entity(tableName = "flowchart_verifications")
data class FlowchartVerificationEntity(
    @PrimaryKey val stepNumber: Int,
    val isConfirmed: Boolean = false,
    val confirmedTimestamp: Long? = null,
    val notes: String = ""
)
