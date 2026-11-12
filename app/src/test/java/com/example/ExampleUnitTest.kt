package com.example

import com.example.data.model.DebtEntity
import com.example.data.model.FinancialProfileEntity
import com.example.logic.FlowchartCalculator
import com.example.logic.StepStatus
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testInitialFlowchartState_step3IsActive() {
        val profile = FinancialProfileEntity(
            netMonthlySalary = 4460.50,
            bankBalance = 1039.56,
            dedicatedSavings = 0.0,
            kwspAccount1 = 92783.88,
            kwspAccount2 = 6231.14,
            kwspAccount3 = 164.70
        )
        val debts = listOf(
            DebtEntity(key = "MAYBANK_CC", name = "Maybank Credit Card", initialBalance = 10093.43, currentBalance = 10093.43, monthlyTarget = 1200.0, aprPercent = 18.0, isHighInterest = true),
            DebtEntity(key = "SHOPEE_SLOAN", name = "Shopee SLoan", initialBalance = 1983.33, currentBalance = 1983.33, monthlyTarget = 283.33, aprPercent = 18.0, isHighInterest = true),
            DebtEntity(key = "HL_HP1", name = "Hong Leong Car HP1", initialBalance = 7267.0, currentBalance = 7267.0, monthlyTarget = 530.0, aprPercent = 6.0, isHighInterest = false),
            DebtEntity(key = "AMBANK_HP2", name = "AmBank Car HP2", initialBalance = 13301.0, currentBalance = 13301.0, monthlyTarget = 250.0, aprPercent = 6.0, isHighInterest = false)
        )

        val journey = FlowchartCalculator.calculateJourney(profile, debts)

        // Step 0 should be completed (budget/essentials tracked)
        assertEquals(StepStatus.COMPLETED, journey.steps[0].status)
        // Step 1 should be completed (liquid buffer >= RM1,000)
        assertEquals(StepStatus.COMPLETED, journey.steps[1].status)
        // Step 2 should be completed (KWSP statutory match active)
        assertEquals(StepStatus.COMPLETED, journey.steps[2].status)
        // Step 3 (High Interest Debt) should be the ACTIVE focus!
        assertEquals(StepStatus.ACTIVE, journey.steps[3].status)
        assertEquals(3, journey.currentStepNumber)
        // Step 4 (Full EF) should be LOCKED until step 3 clears
        assertEquals(StepStatus.LOCKED, journey.steps[4].status)
        // High interest debt total
        assertEquals(12076.76, journey.highInterestDebt, 0.01)
        // Total debt
        assertEquals(32644.76, journey.totalDebt, 0.01)
    }

    @Test
    fun testFlowchartAdvancement_clearingHighInterestDebtUnlocksStep4() {
        val profile = FinancialProfileEntity(
            netMonthlySalary = 4460.50,
            bankBalance = 1039.56,
            dedicatedSavings = 0.0,
            kwspAccount1 = 92783.88,
            kwspAccount2 = 6231.14,
            kwspAccount3 = 164.70
        )
        // High interest debts cleared to 0!
        val debts = listOf(
            DebtEntity(key = "MAYBANK_CC", name = "Maybank Credit Card", initialBalance = 10093.43, currentBalance = 0.0, monthlyTarget = 1200.0, aprPercent = 18.0, isHighInterest = true),
            DebtEntity(key = "SHOPEE_SLOAN", name = "Shopee SLoan", initialBalance = 1983.33, currentBalance = 0.0, monthlyTarget = 283.33, aprPercent = 18.0, isHighInterest = true),
            DebtEntity(key = "HL_HP1", name = "Hong Leong Car HP1", initialBalance = 7267.0, currentBalance = 7267.0, monthlyTarget = 530.0, aprPercent = 6.0, isHighInterest = false),
            DebtEntity(key = "AMBANK_HP2", name = "AmBank Car HP2", initialBalance = 13301.0, currentBalance = 13301.0, monthlyTarget = 250.0, aprPercent = 6.0, isHighInterest = false)
        )

        val journey = FlowchartCalculator.calculateJourney(profile, debts)

        // Step 3 should now be COMPLETED
        assertEquals(StepStatus.COMPLETED, journey.steps[3].status)
        // Step 4 (Full EF) should now be the ACTIVE focus!
        assertEquals(StepStatus.ACTIVE, journey.steps[4].status)
        assertEquals(4, journey.currentStepNumber)
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = DateUtils.formatCurrency(4460.5)
        assertEquals("RM 4,460.50", formatted)
    }

    @Test
    fun testDecemberDetection() {
        assertTrue(DateUtils.isDecember("2026-12"))
        assertTrue(!DateUtils.isDecember("2026-11"))
    }
}
