package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.example.data.model.MonthlyCheckInProgressEntity
import com.example.data.model.MonthlyIncomeEntity
import com.example.data.model.PaydayAllocationEntity
import com.example.ui.screens.CommitmentsScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun uiIntegration_checkInStage1ToPaydayPlanValidationAndApproval() {
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

        // V1.1 Validation check: Entering excessive extra debt (exceeds eligible debt cap) disables Approve button
        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("payday_override_card"))
        composeTestRule.onNodeWithTag("payday_override_extra_debt").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("payday_override_extra_debt").performTextInput("999999")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("approve_payday_plan_btn").performScrollTo().assertIsNotEnabled()

        // Valid custom extra debt enables Approve and persists approvedDebtAllocationsJson
        composeTestRule.onNodeWithTag("payday_override_extra_debt").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("payday_override_extra_debt").performTextInput("300")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("approve_payday_plan_btn").performScrollTo().assertIsEnabled().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(savedAllocation)
        assertTrue(savedAllocation!!.isApproved)
        assertTrue(savedAllocation!!.hasOverrides)
        assertEquals(300.0, savedAllocation!!.approvedExtraDebtRepayment, 0.01)
        assertTrue(savedAllocation!!.approvedDebtAllocationsJson.contains("SHOPEE_SLOAN"))
        composeTestRule.onNodeWithText("STAGE 3 — MONTHLY COMMITMENTS").assertIsDisplayed()

        // Jump straight to Stage 6 Review without completing Stages 3-5 -> must NOT say Complete!
        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("checkin_stage_tab_6"))
        composeTestRule.onNodeWithTag("checkin_stage_tab_6").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Check-In Incomplete").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun v11_scenario7_reopeningCheckInAfterScreenRecreationPreservesPersistedStageCompletion() {
        var persistedIncome by mutableStateOf<MonthlyIncomeEntity?>(null)
        var persistedAllocation by mutableStateOf<PaydayAllocationEntity?>(null)
        var persistedProgress by mutableStateOf<MonthlyCheckInProgressEntity?>(null)
        var screenGeneration by mutableStateOf(1)

        composeTestRule.setContent {
            MyApplicationTheme {
                // Force full subtree teardown & recreation whenever screenGeneration increments
                androidx.compose.runtime.key(screenGeneration) {
                    CommitmentsScreen(
                        monthKey = "2026-11",
                        items = sampleCommitments(),
                        currentIncome = persistedIncome,
                        onSaveIncome = { persistedIncome = it },
                        onConfirmBatchPayments = {},
                        onUnmarkCommitment = {},
                        onUpdateRecurringDefault = { _, _ -> },
                        debts = sampleDebts(),
                        currentPaydayAllocation = persistedAllocation,
                        onSavePaydayAllocation = { persistedAllocation = it },
                        checkInProgress = persistedProgress,
                        onSaveCheckInProgress = { persistedProgress = it }
                    )
                }
            }
        }

        // Complete Stages 1 -> 2 -> 3 -> 4 (no payments due), leaving Stages 5 & 6 incomplete
        composeTestRule.onNodeWithTag("save_income_continue_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("approve_payday_plan_btn"))
        composeTestRule.onNodeWithTag("approve_payday_plan_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("stage3_continue_btn"))
        composeTestRule.onNodeWithTag("stage3_continue_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("stage4_confirm_no_payments_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(persistedProgress)
        assertTrue(persistedProgress!!.stage3Reviewed)
        assertTrue(persistedProgress!!.stage4Confirmed)
        assertFalse(persistedProgress!!.stage5Reviewed)
        assertFalse(persistedProgress!!.stage6Completed)

        // Simulate leaving and reopening Monthly Check-In / Activity recreation
        screenGeneration = 2
        composeTestRule.waitForIdle()

        // Opening Stage 6 after recreation must STILL recognize Stages 1-4 complete & Stage 5 incomplete (must NOT say Complete!)
        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("checkin_stage_tab_6"))
        composeTestRule.onNodeWithTag("checkin_stage_tab_6").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Check-In Incomplete").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("• Stage 5: Savings review decision required").performScrollTo().assertIsDisplayed()

        // Complete Stage 5 & Stage 6 explicitly, recreate again, and verify Complete persists!
        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("checkin_stage_tab_5"))
        composeTestRule.onNodeWithTag("checkin_stage_tab_5").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("stage5_confirm_savings_btn"))
        composeTestRule.onNodeWithTag("stage5_confirm_savings_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("stage6_complete_btn"))
        composeTestRule.onNodeWithTag("stage6_complete_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Monthly Check-In Complete!").performScrollTo().assertIsDisplayed()

        // Simulate reopening Monthly Check-In again after full completion
        screenGeneration = 3
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("commitments_screen")
            .performScrollToNode(hasTestTag("checkin_stage_tab_6"))
        composeTestRule.onNodeWithTag("checkin_stage_tab_6").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Monthly Check-In Complete!").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun v111_zeroSurplusVsShortfall_rendersExactBreakEvenWithoutFalseShortfall() {
        // Commitments total = 700 + 1200 + 283.33 = 2183.33. Setting netSalary = 2183.33 yields exact zero surplus.
        var testIncome by mutableStateOf(
            MonthlyIncomeEntity(
                monthKey = "2026-11",
                grossSalary = 2183.33,
                netSalary = 2183.33,
                updatedTimestamp = 0L
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                CommitmentsScreen(
                    monthKey = "2026-11",
                    items = sampleCommitments(),
                    currentIncome = testIncome,
                    onSaveIncome = { testIncome = it },
                    onConfirmBatchPayments = {},
                    onUnmarkCommitment = {},
                    onUpdateRecurringDefault = { _, _ -> },
                    debts = sampleDebts(),
                    currentPaydayAllocation = null,
                    onSavePaydayAllocation = {}
                )
            }
        }

        // Go to Stage 2 directly to inspect exact zero-surplus state
        composeTestRule.onNodeWithTag("checkin_stage_tab_2").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("payday_zero_surplus_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Exact Break-Even After Obligations (RM 0.00 Surplus)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payday_shortfall_banner").assertDoesNotExist()

        // Lower netSalary by RM 500 -> genuine negative surplus must display shortfall banner with RM 500.00
        testIncome = testIncome.copy(netSalary = 1683.33)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("payday_shortfall_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Insufficient Cash After Obligations (Shortfall: RM 500.00)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payday_zero_surplus_banner").assertDoesNotExist()
    }
}
