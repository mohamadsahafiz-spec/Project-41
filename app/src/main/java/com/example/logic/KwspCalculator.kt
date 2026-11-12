package com.example.logic

import kotlin.math.roundToLong

data class KwspContributionBreakdown(
    val grossSalary: Double,
    val employeeRatePercent: Double,
    val employerRatePercent: Double,
    val employeeAmount: Double,
    val employerAmount: Double,
    val totalContribution: Double,
    val account1Allocation: Double, // 75%
    val account2Allocation: Double, // 15%
    val account3Allocation: Double, // 10%
    val isEstimated: Boolean
)

object KwspCalculator {

    /**
     * Calculates KWSP statutory contributions using current official schedules:
     * - Employee rate: 11%
     * - Employer rate: 13% for wages <= RM5,000; 12% for wages > RM5,000
     * - 2024+ restructuring allocation:
     *   Akaun Persaraan (Account 1): 75%
     *   Akaun Sejahtera (Account 2): 15%
     *   Akaun Fleksibel (Account 3): 10%
     */
    fun calculateKwsp(
        grossSalary: Double,
        actualEmployeeAmount: Double? = null,
        actualEmployerAmount: Double? = null
    ): KwspContributionBreakdown {
        val hasActual = actualEmployeeAmount != null && actualEmployerAmount != null

        val employeeRate = 11.0
        val employerRate = if (grossSalary <= 5000.0) 13.0 else 12.0

        val employeeContribution = if (actualEmployeeAmount != null) {
            actualEmployeeAmount
        } else {
            // Statutory estimate with standard rounding
            (grossSalary * (employeeRate / 100.0) * 100).roundToLong() / 100.0
        }

        val employerContribution = if (actualEmployerAmount != null) {
            actualEmployerAmount
        } else {
            (grossSalary * (employerRate / 100.0) * 100).roundToLong() / 100.0
        }

        val total = employeeContribution + employerContribution
        val acc1 = (total * 0.75 * 100).roundToLong() / 100.0
        val acc2 = (total * 0.15 * 100).roundToLong() / 100.0
        val acc3 = (total * 0.10 * 100).roundToLong() / 100.0

        return KwspContributionBreakdown(
            grossSalary = grossSalary,
            employeeRatePercent = employeeRate,
            employerRatePercent = employerRate,
            employeeAmount = employeeContribution,
            employerAmount = employerContribution,
            totalContribution = total,
            account1Allocation = acc1,
            account2Allocation = acc2,
            account3Allocation = acc3,
            isEstimated = !hasActual
        )
    }
}
