package com.example.logic

import com.example.data.model.MonthlyIncomeEntity
import kotlin.math.roundToLong

data class KwspContributionBreakdown(
    val grossSalary: Double, // Contributable wages (Basic Salary + Incentives + 13th-Month Bonus)
    val basicSalary: Double = grossSalary,
    val contributableWages: Double = grossSalary,
    val overtimeExcluded: Double = 0.0,
    val incentivesIncluded: Double = 0.0,
    val bonusIncluded: Double = 0.0,
    val employeeRatePercent: Double,
    val employerRatePercent: Double,
    val employeeAmount: Double,
    val employerAmount: Double,
    val totalContribution: Double,
    val account1Allocation: Double, // 75% Akaun Persaraan
    val account2Allocation: Double, // 15% Akaun Sejahtera
    val account3Allocation: Double, // 10% Akaun Fleksibel
    val isEstimated: Boolean,
    val actualEmployeeProvided: Boolean = !isEstimated,
    val actualEmployerProvided: Boolean = !isEstimated
)

object KwspCalculator {

    /**
     * Calculates KWSP statutory contributions using current official schedules:
     * - Contributable wages = Basic Salary + Incentives + 13th-Month Bonus.
     * - OVERTIME IS EXPLICITLY EXCLUDED from KWSP-contributable wages.
     * - Employee rate: 11%
     * - Employer rate: 13% for contributable wages <= RM5,000; 12% for wages > RM5,000
     * - 2024+ restructuring allocation:
     *   Akaun Persaraan (Account 1): 75%
     *   Akaun Sejahtera (Account 2): 15%
     *   Akaun Fleksibel (Account 3): 10%
     * - Actual figures from payslip override estimates.
     */
    fun calculateKwsp(
        grossSalary: Double,
        incentives: Double = 0.0,
        thirteenthMonth: Double = 0.0,
        overtime: Double = 0.0,
        actualEmployeeAmount: Double? = null,
        actualEmployerAmount: Double? = null
    ): KwspContributionBreakdown {
        // OVERTIME is excluded from contributable wages.
        // Contributable wages = Basic Salary + Incentives + 13th-Month Bonus
        val contributableWages = grossSalary + incentives + thirteenthMonth
        val hasActual = (actualEmployeeAmount != null && actualEmployeeAmount > 0.0) &&
                (actualEmployerAmount != null && actualEmployerAmount > 0.0)

        val employeeRate = 11.0
        val employerRate = if (contributableWages <= 5000.0) 13.0 else 12.0

        val employeeContribution = if (actualEmployeeAmount != null && actualEmployeeAmount > 0.0) {
            actualEmployeeAmount
        } else {
            // Statutory estimate with standard rounding based ONLY on eligible contributable wages (EXCLUDING OVERTIME)
            (contributableWages * (employeeRate / 100.0) * 100).roundToLong() / 100.0
        }

        val employerContribution = if (actualEmployerAmount != null && actualEmployerAmount > 0.0) {
            actualEmployerAmount
        } else {
            (contributableWages * (employerRate / 100.0) * 100).roundToLong() / 100.0
        }

        val total = employeeContribution + employerContribution
        val acc1 = (total * 0.75 * 100).roundToLong() / 100.0
        val acc2 = (total * 0.15 * 100).roundToLong() / 100.0
        val acc3 = (total * 0.10 * 100).roundToLong() / 100.0

        return KwspContributionBreakdown(
            grossSalary = contributableWages,
            basicSalary = grossSalary,
            contributableWages = contributableWages,
            overtimeExcluded = overtime,
            incentivesIncluded = incentives,
            bonusIncluded = thirteenthMonth,
            employeeRatePercent = employeeRate,
            employerRatePercent = employerRate,
            employeeAmount = employeeContribution,
            employerAmount = employerContribution,
            totalContribution = total,
            account1Allocation = acc1,
            account2Allocation = acc2,
            account3Allocation = acc3,
            isEstimated = !hasActual,
            actualEmployeeProvided = actualEmployeeAmount != null && actualEmployeeAmount > 0.0,
            actualEmployerProvided = actualEmployerAmount != null && actualEmployerAmount > 0.0
        )
    }

    /**
     * Overload for backwards compatibility with single gross parameter and actual amounts
     */
    fun calculateKwsp(
        grossSalary: Double,
        actualEmployeeAmount: Double?,
        actualEmployerAmount: Double?
    ): KwspContributionBreakdown {
        return calculateKwsp(
            grossSalary = grossSalary,
            incentives = 0.0,
            thirteenthMonth = 0.0,
            overtime = 0.0,
            actualEmployeeAmount = actualEmployeeAmount,
            actualEmployerAmount = actualEmployerAmount
        )
    }

    /**
     * Calculate KWSP directly from MonthlyIncomeEntity.
     * Guarantees overtime is excluded from contributable wages.
     */
    fun calculateFromMonthlyIncome(income: MonthlyIncomeEntity): KwspContributionBreakdown {
        return calculateKwsp(
            grossSalary = income.grossSalary, // Basic salary
            incentives = income.incentives,
            thirteenthMonth = income.thirteenthMonth,
            overtime = income.overtime, // Overtime is excluded from contributable wages
            actualEmployeeAmount = income.actualEmployeeKwsp,
            actualEmployerAmount = income.actualEmployerKwsp
        )
    }
}

