package com.example

import com.example.data.model.DebtEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.FlowchartVerificationEntity
import com.example.logic.FlowchartCalculator
import com.example.logic.KwspCalculator
import com.example.logic.StepStatus
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private fun createInitialProfile(): FinancialProfileEntity {
        return FinancialProfileEntity(
            netMonthlySalary = 4460.50,
            bankBalance = 1039.56, // Everyday Cash (Tier A) - NOT emergency savings
            dedicatedSavings = 0.0, // Dedicated Savings (Tier B)
            kwspAccount1 = 92783.88,
            kwspAccount2 = 6231.14,
            kwspAccount3 = 164.70
        )
    }

    private fun createInitialDebts(): List<DebtEntity> {
        return listOf(
            DebtEntity(
                key = "MAYBANK_CC",
                name = "Maybank Credit Card",
                initialBalance = 10093.43,
                currentBalance = 10093.43,
                monthlyTarget = 1200.0,
                aprPercent = 15.0,
                isHighInterest = true
            ),
            DebtEntity(
                key = "SHOPEE_SLOAN",
                name = "Shopee SLoan",
                initialBalance = 1983.33,
                currentBalance = 1983.33,
                monthlyTarget = 283.33,
                aprPercent = 18.0,
                isHighInterest = true
            ),
            DebtEntity(
                key = "HL_HP1",
                name = "Hong Leong Car HP1",
                initialBalance = 7267.0,
                currentBalance = 7267.0,
                monthlyTarget = 530.0,
                aprPercent = 3.0,
                isHighInterest = false
            ),
            DebtEntity(
                key = "AMBANK_HP2",
                name = "AmBank Car HP2",
                initialBalance = 13301.0,
                currentBalance = 13301.0,
                monthlyTarget = 250.0,
                aprPercent = 3.0,
                isHighInterest = false
            )
        )
    }

    @Test
    fun testInitialFlowchartState_step1RequiresDedicatedSavings() {
        val profile = createInitialProfile()
        val debts = createInitialDebts()

        // Dedicated emergency savings = 0.0, ordinary bank balance = 1039.56
        val journey = FlowchartCalculator.calculateJourney(
            profile = profile,
            debts = debts,
            dedicatedEmergencySavings = 0.0,
            dedicatedTotalSavings = 0.0,
            verifications = emptyList()
        )

        // Step 0 requires explicit verification (not completed automatically)
        assertEquals(StepStatus.ACTIVE, journey.steps[0].status)

        // Step 1 requires dedicated emergency savings of RM1,000.
        // Ordinary bank balance RM1,039.56 does NOT count!
        assertEquals(StepStatus.ACTIVE, journey.steps[1].status)
        assertEquals(0.0, journey.steps[1].currentAmount, 0.01)
        assertEquals(1000.0, journey.steps[1].targetAmount, 0.01)
        assertEquals(1000.0, journey.steps[1].remainingAmount, 0.01)
        assertEquals(0f, journey.steps[1].progressPercent, 0.01f)

        // Step 2 and subsequent steps must be LOCKED until Step 1 completes
        assertEquals(StepStatus.LOCKED, journey.steps[2].status)
        assertEquals(StepStatus.LOCKED, journey.steps[3].status)

        // Verify debts separation
        assertEquals(12076.76, journey.highInterestDebt, 0.01)
        assertEquals(20568.0, journey.moderateInterestDebt, 0.01)
        assertEquals(32644.76, journey.totalDebt, 0.01)

        // Everyday Cash is separate from dedicated savings
        assertEquals(1039.56, journey.everydayCash, 0.01)
        assertEquals(0.0, journey.dedicatedSavings, 0.01)
        assertEquals(0.0, journey.dedicatedEmergencyFund, 0.01)
    }

    @Test
    fun testStep0VerificationFlow() {
        val profile = createInitialProfile()
        val debts = createInitialDebts()

        // Unconfirmed
        val unconfirmedJourney = FlowchartCalculator.calculateJourney(
            profile = profile,
            debts = debts,
            dedicatedEmergencySavings = 0.0,
            dedicatedTotalSavings = 0.0,
            verifications = emptyList()
        )
        assertEquals(StepStatus.ACTIVE, unconfirmedJourney.steps[0].status)

        // Confirmed via FlowchartVerificationEntity
        val confirmedJourney = FlowchartCalculator.calculateJourney(
            profile = profile,
            debts = debts,
            dedicatedEmergencySavings = 0.0,
            dedicatedTotalSavings = 0.0,
            verifications = listOf(
                FlowchartVerificationEntity(stepNumber = 0, isConfirmed = true, notes = "Budget verified")
            )
        )
        assertEquals(StepStatus.COMPLETED, confirmedJourney.steps[0].status)
        assertEquals(0.0, confirmedJourney.steps[0].remainingAmount, 0.01)
    }

    @Test
    fun testStep1AdvancementToStep3WhenDedicatedEmergencyBufferSaved() {
        val profile = createInitialProfile()
        val debts = createInitialDebts()

        // When user deposits RM1,000 into dedicated Emergency Fund
        val journey = FlowchartCalculator.calculateJourney(
            profile = profile,
            debts = debts,
            dedicatedEmergencySavings = 1000.0,
            dedicatedTotalSavings = 1000.0,
            verifications = listOf(
                FlowchartVerificationEntity(stepNumber = 0, isConfirmed = true),
                FlowchartVerificationEntity(stepNumber = 2, isConfirmed = true)
            )
        )

        // Step 1 is COMPLETED
        assertEquals(StepStatus.COMPLETED, journey.steps[1].status)
        assertEquals(1000.0, journey.steps[1].currentAmount, 0.01)
        assertEquals(0.0, journey.steps[1].remainingAmount, 0.01)
        assertEquals(100f, journey.steps[1].progressPercent, 0.01f)

        // Step 2 (KWSP) is COMPLETED
        assertEquals(StepStatus.COMPLETED, journey.steps[2].status)

        // Step 3 (High Interest Debt) is now the ACTIVE mission!
        assertEquals(StepStatus.ACTIVE, journey.steps[3].status)
        assertEquals(3, journey.currentStepNumber)

        // Step 4 is LOCKED until Step 3 clears
        assertEquals(StepStatus.LOCKED, journey.steps[4].status)
    }

    @Test
    fun testStep3AdvancementToStep4WhenHighInterestDebtCleared() {
        val profile = createInitialProfile()
        // High interest debts paid off!
        val debts = listOf(
            DebtEntity(key = "MAYBANK_CC", name = "Maybank Credit Card", initialBalance = 10093.43, currentBalance = 0.0, monthlyTarget = 1200.0, aprPercent = 15.0, isHighInterest = true),
            DebtEntity(key = "SHOPEE_SLOAN", name = "Shopee SLoan", initialBalance = 1983.33, currentBalance = 0.0, monthlyTarget = 283.33, aprPercent = 18.0, isHighInterest = true),
            DebtEntity(key = "HL_HP1", name = "Hong Leong Car HP1", initialBalance = 7267.0, currentBalance = 7267.0, monthlyTarget = 530.0, aprPercent = 3.0, isHighInterest = false),
            DebtEntity(key = "AMBANK_HP2", name = "AmBank Car HP2", initialBalance = 13301.0, currentBalance = 13301.0, monthlyTarget = 250.0, aprPercent = 3.0, isHighInterest = false)
        )

        val journey = FlowchartCalculator.calculateJourney(
            profile = profile,
            debts = debts,
            dedicatedEmergencySavings = 1000.0,
            dedicatedTotalSavings = 1000.0,
            verifications = listOf(
                FlowchartVerificationEntity(stepNumber = 0, isConfirmed = true),
                FlowchartVerificationEntity(stepNumber = 2, isConfirmed = true)
            )
        )

        // Step 3 is now COMPLETED
        assertEquals(StepStatus.COMPLETED, journey.steps[3].status)
        // Step 4 (Full 3-Month Emergency Fund) is now ACTIVE!
        assertEquals(StepStatus.ACTIVE, journey.steps[4].status)
        assertEquals(4, journey.currentStepNumber)
    }

    @Test
    fun testKwspCalculator_statutorySchedulesAndAllocation() {
        // Engineer Basic salary: RM4,158.00 (<= RM5,000 threshold -> 13% employer rate)
        val gross = 4158.00
        val breakdown = KwspCalculator.calculateKwsp(gross)

        assertEquals(11.0, breakdown.employeeRatePercent, 0.01)
        assertEquals(13.0, breakdown.employerRatePercent, 0.01)
        assertEquals(457.38, breakdown.employeeAmount, 0.01) // 4158 * 0.11 = 457.38
        assertEquals(540.54, breakdown.employerAmount, 0.01) // 4158 * 0.13 = 540.54
        assertEquals(997.92, breakdown.totalContribution, 0.01)

        // 2024 Restructuring: 75% Acc 1, 15% Acc 2, 10% Acc 3
        assertEquals(748.44, breakdown.account1Allocation, 0.01) // 997.92 * 0.75 = 748.44
        assertEquals(149.69, breakdown.account2Allocation, 0.01) // 997.92 * 0.15 = 149.688 -> 149.69
        assertEquals(99.79, breakdown.account3Allocation, 0.01)  // 997.92 * 0.10 = 99.792 -> 99.79
        assertTrue(breakdown.isEstimated)
    }

    @Test
    fun testKwspCalculator_actualPayslipOverride() {
        val gross = 4158.00
        // User overrides from actual payslip deductions
        val breakdown = KwspCalculator.calculateKwsp(
            grossSalary = gross,
            actualEmployeeAmount = 457.00,
            actualEmployerAmount = 541.00
        )

        assertEquals(457.00, breakdown.employeeAmount, 0.01)
        assertEquals(541.00, breakdown.employerAmount, 0.01)
        assertEquals(998.00, breakdown.totalContribution, 0.01)
        assertFalse(breakdown.isEstimated)
    }

    @Test
    fun testKwspCalculator_higherIncomeEmployerRate() {
        // Gross > RM5,000 -> 12% employer rate
        val gross = 6000.00
        val breakdown = KwspCalculator.calculateKwsp(gross)

        assertEquals(11.0, breakdown.employeeRatePercent, 0.01)
        assertEquals(12.0, breakdown.employerRatePercent, 0.01)
        assertEquals(660.00, breakdown.employeeAmount, 0.01)
        assertEquals(720.00, breakdown.employerAmount, 0.01)
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = DateUtils.formatCurrency(4460.5)
        assertEquals("RM 4,460.50", formatted)
    }

    @Test
    fun testDecemberDetection() {
        assertTrue(DateUtils.isDecember("2026-12"))
        assertFalse(DateUtils.isDecember("2026-11"))
    }

    @Test
    fun testDebtConfigurationRatesAndHpBehavior() {
        val debts = createInitialDebts()
        val mbCc = debts.first { it.key == "MAYBANK_CC" }
        val sloan = debts.first { it.key == "SHOPEE_SLOAN" }
        val hp1 = debts.first { it.key == "HL_HP1" }
        val hp2 = debts.first { it.key == "AMBANK_HP2" }

        // 1. Maybank Credit Card: 15% p.a., high interest priority
        assertEquals(15.0, mbCc.aprPercent, 0.01)
        assertTrue(mbCc.isHighInterest)

        // 2. Shopee SLoan: 1.5% monthly provisional (18% p.a.), high interest priority
        assertEquals(18.0, sloan.aprPercent, 0.01)
        assertTrue(sloan.isHighInterest)

        // 3. Hong Leong HP1: 3% reference
        assertEquals(3.0, hp1.aprPercent, 0.01)
        assertFalse(hp1.isHighInterest)

        // 4. AmBank HP2: 3% reference, full legal loan balance tracked, user contribution RM250
        assertEquals(3.0, hp2.aprPercent, 0.01)
        assertFalse(hp2.isHighInterest)
        assertEquals(13301.0, hp2.currentBalance, 0.01)
        assertEquals(250.0, hp2.monthlyTarget, 0.01)
    }
}
