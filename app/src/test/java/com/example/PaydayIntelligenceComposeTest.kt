package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.data.MonthlyCommitmentItem
import com.example.data.model.DebtEntity
import com.example.data.model.MonthlyIncomeEntity
import com.example.data.model.PaydayAllocationEntity
import com.example.ui.screens.CommitmentsScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaydayIntelligenceComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleCommitments(): List<MonthlyCommitmentItem> = listOf(
        MonthlyCommitmentItem(1, 1, "Rent", 700.0, isDebt = false, debtKey = null, isPaid = false, actualPaidAmount = 700.0, paidTimestamp = null),
        MonthlyCommitmentItem(2, 2, "Maybank Credit Card", 1200.0, isDebt = true, debtKey = "MAYBANK_CC", isPaid = false, actualPaidAmount = 1200.0, paidTimestamp = null),
        MonthlyCommitmentItem(3, 3, "Shopee SLoan", 283.33, isDebt = true, debtKey = "SHOPEE_SLOAN", isPaid = false, actualPaidAmount = 283.33, paidTimestamp = null)
    )

    private fun sampleDebts(): List<DebtEntity> = listOf(
        DebtEntity(1, "MAYBANK_CC", "Maybank Credit Card", 10093.43, 10093.43, 1200.0, 15.0, isHighInterest = true),
        DebtEntity(2, "SHOPEE_SLOAN", "Shopee SLoan", 1983.33, 1983.33, 283.33, 18.0, isHighInterest = true)
    )

    @Test
    fun uiIntegration_checkInStage1ToPaydayPlanToCommitmentsAndCompletionGuard() {
        var savedIncome: MonthlyIncomeEntity? = null
        var savedAllocation: PaydayAllocationEntity? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                CommitmentsScreen(
                    monthKey = "2026-11",
                    items = sampleCommitments(),
                    currentIncome = savedIncome,
                    onSaveIncome = { savedIncome = it },
                    onConfirmBatchPayments = {},
                    onUnmarkCommitment = {},
                    onUpdateRecurringDefault = { _, _ -> },
                    debts = sampleDebts(),
                    currentPaydayAllocation = savedAllocation,
                    onSavePaydayAllocation = { savedAllocation = it }
                )
            }
        }

        // Stage 1: Enter reimbursements (excluded) and advance to Stage 2 Payday Plan
        composeTestRule.onNodeWithTag("reimbursements_input").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("reimbursements_input").performTextInput("350")
        composeTestRule.onNodeWithTag("save_income_continue_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(savedIncome)
        assertEquals(350.0, savedIncome!!.reimbursements, 0.01)

        // Stage 2: Payday Intelligence card renders WHY explanations & consequence box
        composeTestRule.onNodeWithTag("payday_intelligence_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("STAGE 2 — PAYDAY INTELLIGENCE").assertIsDisplayed()

        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("payday_recommendations_card"))
        composeTestRule.onNodeWithTag("payday_recommendations_card").assertIsDisplayed()

        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("payday_consequence_box"))
        composeTestRule.onNodeWithTag("payday_consequence_box").assertIsDisplayed()

        // Scroll to override/approve card, adjust extra debt override, and approve Payday Plan
        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("payday_override_card"))
        composeTestRule.onNodeWithTag("payday_override_extra_debt").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("payday_override_extra_debt").performTextInput("300")
        composeTestRule.onNodeWithTag("approve_payday_plan_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(savedAllocation)
        assertTrue(savedAllocation!!.isApproved)
        assertTrue(savedAllocation!!.hasOverrides)
        assertEquals(300.0, savedAllocation!!.approvedExtraDebtRepayment, 0.01)
        composeTestRule.onNodeWithText("STAGE 3 — MONTHLY COMMITMENTS").assertIsDisplayed()

        // Jump straight to Stage 6 Review without completing Stages 3-5 -> must NOT say Complete!
        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("checkin_stage_tab_6"))
        composeTestRule.onNodeWithTag("checkin_stage_tab_6").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Check-In Incomplete").performScrollTo().assertIsDisplayed()
    }
}
