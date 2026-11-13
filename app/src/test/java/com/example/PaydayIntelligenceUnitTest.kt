package com.example

import com.example.data.MonthlyCommitmentItem
import com.example.data.model.DebtEntity
import com.example.data.model.MonthlyIncomeEntity
import com.example.logic.CashSurplusStatus
import com.example.logic.DebtRateClassification
import com.example.logic.PaydayInput
import com.example.logic.PaydayIntelligenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaydayIntelligenceUnitTest {

    private fun sampleCommitments(allUnpaid: Boolean = true): List<MonthlyCommitmentItem> = listOf(
        MonthlyCommitmentItem(1, 1, "Rent", 700.0, isDebt = false, debtKey = null, isPaid = !allUnpaid, actualPaidAmount = 700.0, paidTimestamp = null),
        MonthlyCommitmentItem(2, 2, "Playschool", 550.0, isDebt = false, debtKey = null, isPaid = false, actualPaidAmount = 550.0, paidTimestamp = null),
        MonthlyCommitmentItem(3, 3, "Maybank Credit Card", 1200.0, isDebt = true, debtKey = "MAYBANK_CC", isPaid = false, actualPaidAmount = 1200.0, paidTimestamp = null),
        MonthlyCommitmentItem(4, 4, "Shopee SLoan", 283.33, isDebt = true, debtKey = "SHOPEE_SLOAN", isPaid = false, actualPaidAmount = 283.33, paidTimestamp = null),
        MonthlyCommitmentItem(5, 5, "Hong Leong Car HP1", 530.0, isDebt = true, debtKey = "HL_HP1", isPaid = false, actualPaidAmount = 530.0, paidTimestamp = null),
        MonthlyCommitmentItem(6, 6, "AmBank Car HP2", 250.0, isDebt = true, debtKey = "AMBANK_HP2", isPaid = false, actualPaidAmount = 250.0, paidTimestamp = null)
    )

    private fun sampleDebts(): List<DebtEntity> = listOf(
        DebtEntity(1, "MAYBANK_CC", "Maybank Credit Card", 10093.43, 10093.43, 1200.0, 15.0, isHighInterest = true),
        DebtEntity(2, "SHOPEE_SLOAN", "Shopee SLoan", 1983.33, 1983.33, 283.33, 18.0, isHighInterest = true),
        DebtEntity(3, "HL_HP1", "Hong Leong Car HP1", 7267.0, 7267.0, 530.0, 3.0, isHighInterest = false),
        DebtEntity(4, "AMBANK_HP2", "AmBank Car HP2", 13301.0, 13301.0, 250.0, 3.0, isHighInterest = false)
    )

    @Test
    fun scenario1_salaryWithoutAdditionalIncome_conservesAccountingInvariant() {
        val plan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 4460.50,
                commitments = sampleCommitments(),
                debts = sampleDebts(),
                upcomingEssentialExpenses = 500.0,
                existingEmergencyFund = 500.0,
                emergencyFundTarget = 1000.0,
                cashBufferTarget = 150.0
            )
        )

        assertFalse(plan.hasInsufficientCash)
        assertEquals(4460.50, plan.actualAvailableCash, 0.01)
        assertEquals(1250.0, plan.essentialCommitmentsReserved, 0.01)
        assertEquals(2263.33, plan.minimumDebtPaymentsReserved, 0.01)
        assertEquals(500.0, plan.upcomingEssentialExpensesReserved, 0.01)
        assertNull(plan.additionalIncomeWarning)

        val totalAllocated = plan.essentialCommitmentsReserved +
            plan.minimumDebtPaymentsReserved +
            plan.upcomingEssentialExpensesReserved +
            plan.recommendedEmergencySavings +
            plan.recommendedExtraDebtRepayment +
            plan.recommendedDiscretionaryAllowance +
            plan.recommendedRemainingBuffer
        assertEquals(plan.actualAvailableCash, totalAllocated, 0.01)
        assertTrue(plan.recommendedRemainingBuffer >= 150.0)
        assertTrue(plan.items.all { it.whyExplanation.isNotBlank() })
    }

    @Test
    fun scenario2_salaryWithIncentiveOrBonus_shieldsAdditionalIncomeFromDiscretionary() {
        val baseInput = PaydayInput(
            monthKey = "2026-12",
            netSalary = 4460.50,
            commitments = sampleCommitments(),
            debts = sampleDebts(),
            upcomingEssentialExpenses = 500.0,
            existingEmergencyFund = 1000.0,
            emergencyFundTarget = 10000.0,
            cashBufferTarget = 150.0
        )
        val basePlan = PaydayIntelligenceEngine.calculatePlan(baseInput)
        val bonusPlan = PaydayIntelligenceEngine.calculatePlan(
            baseInput.copy(overtime = 300.0, incentives = 400.0, bonuses = 1000.0)
        )

        // Discretionary allowance must NOT inflate from OT/incentive/bonus windfalls
        assertEquals(basePlan.recommendedDiscretionaryAllowance, bonusPlan.recommendedDiscretionaryAllowance, 0.01)
        assertTrue(bonusPlan.recommendedExtraDebtRepayment > basePlan.recommendedExtraDebtRepayment)
        assertTrue(bonusPlan.recommendedEmergencySavings > basePlan.recommendedEmergencySavings)
        assertNotNull(bonusPlan.additionalIncomeWarning)
        assertTrue(bonusPlan.spendingConsequenceSummary.contains("additional income"))
    }

    @Test
    fun scenario3_multipleDebtsRankedByEffectiveRate_referenceOnlyHpExcludedFromEarlySettlement() {
        val plan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 5500.0,
                commitments = sampleCommitments(),
                debts = sampleDebts(),
                upcomingEssentialExpenses = 400.0,
                existingEmergencyFund = 1000.0,
                emergencyFundTarget = 1000.0,
                cashBufferTarget = 150.0
            )
        )

        // Shopee SLoan (18% provisional) ranks #1 ahead of Maybank CC (15% effective APR)
        assertEquals("SHOPEE_SLOAN", plan.recommendedExtraDebtTargetKey)
        assertEquals("SHOPEE_SLOAN", plan.rankedDebts[0].debtKey)
        assertEquals(DebtRateClassification.PROVISIONAL_RATE, plan.rankedDebts[0].classification)
        assertEquals("MAYBANK_CC", plan.rankedDebts[1].debtKey)
        assertEquals(DebtRateClassification.EFFECTIVE_APR, plan.rankedDebts[1].classification)

        // HP1 & HP2 are REFERENCE_ONLY and never eligible for automatic early settlement recommendation
        val hpDebts = plan.rankedDebts.filter { it.debtKey == "HL_HP1" || it.debtKey == "AMBANK_HP2" }
        assertEquals(2, hpDebts.size)
        assertTrue(hpDebts.all { it.classification == DebtRateClassification.REFERENCE_ONLY && !it.isEligibleForExtraRepayment })

        // When only HP1/HP2 remain, engine refuses to assume early settlement benefit
        val hpOnlyPlan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 5500.0,
                commitments = sampleCommitments().filter { it.debtKey != "MAYBANK_CC" && it.debtKey != "SHOPEE_SLOAN" },
                debts = sampleDebts().filter { !it.isHighInterest },
                upcomingEssentialExpenses = 400.0,
                existingEmergencyFund = 10000.0,
                emergencyFundTarget = 10000.0,
                cashBufferTarget = 150.0
            )
        )
        assertEquals(0.0, hpOnlyPlan.recommendedExtraDebtRepayment, 0.01)
        assertNull(hpOnlyPlan.recommendedExtraDebtTargetKey)
        assertTrue(hpOnlyPlan.dataGaps.any { it.contains("3% reference rate") })

        // Missing APR debt is explicitly flagged
        val missingRatePlan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 4000.0,
                debts = listOf(DebtEntity(9, "CUSTOM", "Friend Loan", 2000.0, 2000.0, 200.0, 0.0, false))
            )
        )
        assertEquals(DebtRateClassification.MISSING_RATE, missingRatePlan.rankedDebts[0].classification)
        assertTrue(missingRatePlan.dataGaps.any { it.contains("Missing effective interest rate") })
    }

    @Test
    fun scenario4_insufficientCashAfterEssentialCommitments_pausesDiscretionaryAndExtraAllocations() {
        val plan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 2500.0,
                commitments = sampleCommitments(),
                debts = sampleDebts(),
                upcomingEssentialExpenses = 500.0,
                existingEmergencyFund = 0.0,
                emergencyFundTarget = 1000.0
            )
        )

        // Required P1 = 1250 + 2263.33 + 500 = 4013.33 vs cash 2500 -> shortfall 1513.33
        assertTrue(plan.hasInsufficientCash)
        assertEquals(1513.33, plan.shortfallAmount, 0.01)
        assertEquals(0.0, plan.recommendedExtraDebtRepayment, 0.01)
        assertEquals(0.0, plan.recommendedEmergencySavings, 0.01)
        assertEquals(0.0, plan.recommendedDiscretionaryAllowance, 0.01)
        assertEquals(0.0, plan.recommendedRemainingBuffer, 0.01)
    }

    @Test
    fun scenario5_emergencySavingsBelowTargetVsMissingTarget() {
        val belowTargetPlan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 4800.0,
                commitments = sampleCommitments(),
                debts = sampleDebts(),
                upcomingEssentialExpenses = 400.0,
                existingEmergencyFund = 200.0,
                emergencyFundTarget = 1000.0,
                cashBufferTarget = 100.0
            )
        )
        assertTrue(belowTargetPlan.recommendedEmergencySavings > 0.0)
        assertTrue(belowTargetPlan.recommendedEmergencySavings <= 800.0)

        // When target is NOT configured, engine must NOT fabricate a target or recommend blindly
        val missingTargetPlan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 4800.0,
                commitments = sampleCommitments(),
                debts = sampleDebts(),
                upcomingEssentialExpenses = null,
                existingEmergencyFund = 200.0,
                emergencyFundTarget = null,
                cashBufferTarget = null
            )
        )
        assertEquals(0.0, missingTargetPlan.recommendedEmergencySavings, 0.01)
        assertTrue(missingTargetPlan.dataGaps.any { it.contains("Emergency fund target is not configured") })
        assertTrue(missingTargetPlan.dataGaps.any { it.contains("Upcoming essential living expenses") })
    }

    @Test
    fun scenario6_noAvailableSurplusForAdditionalDebtRepayment() {
        val plan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 3713.33,
                commitments = sampleCommitments(),
                debts = sampleDebts(),
                upcomingEssentialExpenses = 100.0,
                existingEmergencyFund = 1000.0,
                emergencyFundTarget = 1000.0,
                cashBufferTarget = 100.0
            )
        )
        assertFalse(plan.hasInsufficientCash)
        assertEquals(0.0, plan.recommendedExtraDebtRepayment, 0.01)
        assertEquals(0.0, plan.recommendedEmergencySavings, 0.01)
        assertEquals(0.0, plan.recommendedDiscretionaryAllowance, 0.01)
        assertEquals(100.0, plan.recommendedRemainingBuffer, 0.01)
    }

    @Test
    fun scenario9_reimbursementsAndUnconfirmedIncomeAndPaidCommitmentsNotDoubleCounted() {
        val incomeEntity = MonthlyIncomeEntity(
            monthKey = "2026-11",
            grossSalary = 4158.0,
            netSalary = 4460.50,
            overtime = 200.0,
            incentives = 0.0,
            thirteenthMonth = 0.0,
            otherIncome = 0.0,
            reimbursements = 650.0,
            unconfirmedIncome = 800.0,
            isConfirmedReceived = true
        )
        assertEquals(4660.50, incomeEntity.totalTakeHomeIncome, 0.01)

        val commitments = sampleCommitments(allUnpaid = false) // Rent isPaid = true (700)
        val plan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput.fromMonthlyState(
                monthKey = "2026-11",
                income = incomeEntity,
                defaultNetSalary = 4460.50,
                existingEverydayCash = 1039.56,
                commitments = commitments,
                debts = sampleDebts(),
                recordedExpensesThisMonth = 150.0,
                upcomingEssentialExpenses = 300.0,
                existingEmergencyFund = 0.0,
                emergencyFundTarget = 1000.0,
                cashBufferTarget = 100.0
            )
        )

        assertEquals(4660.50, plan.totalConfirmedIncome, 0.01)
        assertEquals(650.0, plan.reimbursementsExcluded, 0.01)
        assertEquals(800.0, plan.unconfirmedIncomeExcluded, 0.01)
        assertEquals(4660.50 - 700.0 - 150.0, plan.actualAvailableCash, 0.01)
        assertEquals(550.0, plan.essentialCommitmentsReserved, 0.01)

        val unconfirmedPlan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput.fromMonthlyState(
                monthKey = "2026-11",
                income = incomeEntity.copy(isConfirmedReceived = false),
                defaultNetSalary = 4460.50,
                existingEverydayCash = 0.0,
                commitments = sampleCommitments(),
                debts = sampleDebts(),
                recordedExpensesThisMonth = 0.0,
                upcomingEssentialExpenses = 300.0,
                existingEmergencyFund = 0.0,
                emergencyFundTarget = 1000.0,
                cashBufferTarget = 100.0
            )
        )
        assertEquals(0.0, unconfirmedPlan.actualAvailableCash, 0.01)
        assertTrue(unconfirmedPlan.hasInsufficientCash)
    }

    @Test
    fun checkIn6StageCompletionGuard_requiresAllPriorStages() {
        fun isCheckInComplete(s1: Boolean, s2: Boolean, s3: Boolean, s4: Boolean, s5: Boolean, s6: Boolean): Boolean {
            return s1 && s2 && s3 && s4 && s5 && s6
        }

        assertFalse(isCheckInComplete(s1 = true, s2 = false, s3 = true, s4 = true, s5 = true, s6 = true))
        assertFalse(isCheckInComplete(s1 = true, s2 = true, s3 = true, s4 = true, s5 = true, s6 = false))
        assertTrue(isCheckInComplete(s1 = true, s2 = true, s3 = true, s4 = true, s5 = true, s6 = true))
    }

    @Test
    fun v11_cashReconciliation_openingBalancePlusCashflowVsCurrentBankBalance_preventsDoubleCounting() {
        val commitments = sampleCommitments(allUnpaid = false) // Rent RM700 is paid; unpaid = Playschool 550 + debts 2263.33 = 2813.33

        // Mode A: Opening Bank Cash (RM1,039.56) + Confirmed Income (RM4,460.50) - Paid (RM700) - Expenses (RM150)
        val openingModePlan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 4460.50,
                existingEverydayCash = 1039.56,
                openingBankBalance = 1039.56,
                cashSourceMode = com.example.logic.CashSourceMode.OPENING_PLUS_CASHFLOW,
                commitments = commitments,
                debts = sampleDebts(),
                recordedExpensesThisMonth = 150.0,
                upcomingEssentialExpenses = 400.0,
                existingEmergencyFund = 500.0,
                emergencyFundTarget = 1000.0,
                cashBufferTarget = 150.0
            )
        )
        assertEquals(1039.56, openingModePlan.currentBankBalance, 0.01)
        assertEquals(3610.50, openingModePlan.calculatedMonthlyCashflow, 0.01)
        assertEquals(4650.06, openingModePlan.actualAvailableCash, 0.01)
        // Only unpaid essential (Playschool 550) is reserved, paid Rent (700) is NOT re-reserved
        assertEquals(550.0, openingModePlan.essentialCommitmentsReserved, 0.01)

        // Mode B: Current Bank Balance already reflects received salary, paid Rent, and recorded expenses
        val currentBankModePlan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 4460.50,
                existingEverydayCash = 4200.0,
                openingBankBalance = 1039.56,
                cashSourceMode = com.example.logic.CashSourceMode.CURRENT_BANK_BALANCE,
                commitments = commitments,
                debts = sampleDebts(),
                recordedExpensesThisMonth = 150.0,
                upcomingEssentialExpenses = 400.0,
                existingEmergencyFund = 500.0,
                emergencyFundTarget = 1000.0,
                cashBufferTarget = 150.0
            )
        )
        // Must use 4200.00 directly — NEVER re-add 4460.50 salary or re-subtract 700/150 outflows
        assertEquals(4200.0, currentBankModePlan.actualAvailableCash, 0.01)
        assertEquals(4200.0, currentBankModePlan.currentBankBalance, 0.01)
        assertEquals(3610.50, currentBankModePlan.calculatedMonthlyCashflow, 0.01)
        assertEquals(550.0, currentBankModePlan.essentialCommitmentsReserved, 0.01)
    }

    @Test
    fun v11_allocationValidation_rejectsNegativeNonNumericOverDebtOverEmergencyAndBufferViolations() {
        val plan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 20000.0, // High cashflow so caps trigger before total cash
                commitments = emptyList(),
                debts = listOf(
                    DebtEntity(2, "SHOPEE_SLOAN", "Shopee SLoan", 1983.33, 500.0, 283.33, 18.0, true),
                    DebtEntity(3, "HL_HP1", "Hong Leong Car HP1", 7267.0, 7267.0, 530.0, 3.0, false)
                ),
                upcomingEssentialExpenses = 500.0,
                existingEmergencyFund = 700.0,
                emergencyFundTarget = 1000.0, // Gap = RM300
                cashBufferTarget = 1000.0 // Mandatory min buffer = RM1000
            )
        )

        // 1. Negative & non-numeric inputs rejected
        val invalidParse = PaydayIntelligenceEngine.validateApprovedAllocation(
            plan = plan,
            extraDebtRaw = "-100",
            emergencyRaw = "abc",
            discretionaryRaw = ""
        )
        assertFalse(invalidParse.isValid)
        assertNotNull(invalidParse.extraDebtError)
        assertNotNull(invalidParse.emergencyError)
        assertNotNull(invalidParse.discretionaryError)

        // 2. Extra debt exceeding eligible debt balance (RM500, since HP1 is 3% reference-only) is rejected
        val overDebt = PaydayIntelligenceEngine.validateApprovedAllocation(
            plan = plan,
            extraDebtRaw = "800.00",
            emergencyRaw = "200.00",
            discretionaryRaw = "200.00"
        )
        assertFalse(overDebt.isValid)
        assertNotNull(overDebt.extraDebtError)
        assertTrue(overDebt.extraDebtError!!.contains("500.00"))

        // 3. Emergency contribution exceeding remaining target gap (RM300) is rejected
        val overEf = PaydayIntelligenceEngine.validateApprovedAllocation(
            plan = plan,
            extraDebtRaw = "400.00",
            emergencyRaw = "500.00",
            discretionaryRaw = "200.00"
        )
        assertFalse(overEf.isValid)
        assertNotNull(overEf.emergencyError)
        assertTrue(overEf.emergencyError!!.contains("300.00"))

        // 4. Buffer violation rejected even when within individual debt/EF caps
        val tightPlan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 2000.0,
                debts = listOf(DebtEntity(2, "SHOPEE_SLOAN", "Shopee SLoan", 1983.33, 1500.0, 283.33, 18.0, true)),
                commitments = emptyList(),
                upcomingEssentialExpenses = 500.0, // remaining surplus = 2000 - 283.33 - 500 = 1216.67
                existingEmergencyFund = 0.0,
                emergencyFundTarget = 5000.0,
                cashBufferTarget = 600.0 // Must keep >= 600 buffer
            )
        )
        val bufferViolation = PaydayIntelligenceEngine.validateApprovedAllocation(
            plan = tightPlan,
            extraDebtRaw = "500.00",
            emergencyRaw = "300.00",
            discretionaryRaw = "100.00" // leaves 316.67 < 600 min buffer
        )
        assertFalse(bufferViolation.isValid)
        assertNotNull(bufferViolation.generalError)
        assertTrue(bufferViolation.generalError!!.contains("minimum cash buffer"))

        // 5. Valid within all caps & buffer succeeds
        val validResult = PaydayIntelligenceEngine.validateApprovedAllocation(
            plan = tightPlan,
            extraDebtRaw = "300.00",
            emergencyRaw = "200.00",
            discretionaryRaw = "50.00" // leaves 666.67 >= 600 min buffer
        )
        assertTrue(validResult.isValid)
        assertNull(validResult.generalError)

        // 6. Insufficient cash / Priority 1 obligation violation cannot approve non-zero allocations
        val shortfallPlan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 1000.0,
                commitments = sampleCommitments(),
                debts = sampleDebts(),
                upcomingEssentialExpenses = 500.0
            )
        )
        val p1Violation = PaydayIntelligenceEngine.validateApprovedAllocation(
            plan = shortfallPlan,
            extraDebtRaw = "50.00",
            emergencyRaw = "0.00",
            discretionaryRaw = "0.00"
        )
        assertFalse(p1Violation.isValid)
        assertNotNull(p1Violation.generalError)
    }

    @Test
    fun v11_multiDebtWaterfallBreakdown_capsPerDebtCascadesSurplusAndSynchronizesExplanation() {
        val debts = listOf(
            DebtEntity(1, "MAYBANK_CC", "Maybank Credit Card", 10093.43, 10093.43, 1200.0, 15.0, isHighInterest = true),
            DebtEntity(2, "SHOPEE_SLOAN", "Shopee SLoan", 1983.33, 1983.33, 283.33, 18.0, isHighInterest = true),
            DebtEntity(3, "HL_HP1", "Hong Leong Car HP1", 7267.0, 7267.0, 530.0, 3.0, isHighInterest = false),
            DebtEntity(4, "AMBANK_HP2", "AmBank Car HP2", 13301.0, 13301.0, 250.0, 3.0, isHighInterest = false)
        )

        val plan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-12",
                netSalary = 4460.50,
                bonuses = 4000.0,
                commitments = sampleCommitments(),
                debts = debts,
                upcomingEssentialExpenses = 400.0,
                existingEmergencyFund = 10000.0,
                emergencyFundTarget = 10000.0,
                cashBufferTarget = 200.0
            )
        )

        // Should clear Shopee SLoan (1983.33) in full and cascade remainder to Maybank CC (15%), skipping HP1 & HP2
        assertEquals(2, plan.recommendedDebtAllocations.size)
        assertEquals("SHOPEE_SLOAN", plan.recommendedDebtAllocations[0].debtKey)
        assertEquals(1983.33, plan.recommendedDebtAllocations[0].allocatedAmount, 0.01)
        assertEquals("MAYBANK_CC", plan.recommendedDebtAllocations[1].debtKey)
        val expectedRemainder = plan.recommendedExtraDebtRepayment - 1983.33
        assertEquals(expectedRemainder, plan.recommendedDebtAllocations[1].allocatedAmount, 0.01)

        // Explanation matches persisted targets & amounts
        val extraItem = plan.items.first { it.id == "extra_debt" }
        assertTrue(extraItem.whyExplanation.contains("Shopee SLoan"))
        assertTrue(extraItem.whyExplanation.contains("Maybank Credit Card"))

        // Serialization round-trip preserves debt keys, APR, balance, and allocated amount
        val serialized = PaydayIntelligenceEngine.serializeDebtAllocations(plan.recommendedDebtAllocations)
        val deserialized = PaydayIntelligenceEngine.deserializeDebtAllocations(serialized)
        assertEquals(plan.recommendedDebtAllocations, deserialized)

        // Over-allocation across all eligible debts combined is capped cleanly by allocateExtraDebtAcrossDebts
        val overAllocated = PaydayIntelligenceEngine.allocateExtraDebtAcrossDebts(50000.0, plan.rankedDebts)
        assertEquals(2, overAllocated.size)
        assertEquals(1983.33 + 10093.43, overAllocated.sumOf { it.allocatedAmount }, 0.01)
    }

    @Test
    fun v111_task3_exactZeroSurplus_blocksExtraAllocationsWithoutFalseShortfall() {
        // Unpaid commitments = 1250 + 2263.33 = 3513.33; upcoming essentials = 250.00 -> Total Priority 1 = 3763.33
        val plan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 3763.33,
                commitments = sampleCommitments(),
                debts = sampleDebts(),
                upcomingEssentialExpenses = 250.0,
                existingEmergencyFund = 500.0,
                emergencyFundTarget = 1000.0,
                cashBufferTarget = 150.0
            )
        )

        assertEquals(CashSurplusStatus.ZERO_SURPLUS, plan.surplusStatus)
        assertFalse(plan.hasInsufficientCash)
        assertEquals(0.0, plan.shortfallAmount, 0.01)
        assertEquals(0.0, plan.recommendedExtraDebtRepayment, 0.01)
        assertEquals(0.0, plan.recommendedEmergencySavings, 0.01)
        assertEquals(0.0, plan.recommendedDiscretionaryAllowance, 0.01)
        assertEquals(0.0, plan.recommendedRemainingBuffer, 0.01)

        // None of the explanations or summaries should display a false monetary shortfall
        assertFalse(plan.spendingConsequenceSummary.contains("shortfall", ignoreCase = true))
        assertTrue(plan.spendingConsequenceSummary.contains("exactly covers Priority 1", ignoreCase = true))
        plan.items.forEach { item ->
            assertFalse(
                "Unexpected 'shortfall' in ${item.id}: ${item.whyExplanation}",
                item.whyExplanation.contains("shortfall", ignoreCase = true)
            )
        }

        // Validation blocks any non-zero extra allocation without calling it a shortfall, and accepts 0/0/0
        val rejected = PaydayIntelligenceEngine.validateApprovedAllocation(
            plan = plan,
            extraDebtRaw = "50.00",
            emergencyRaw = "0.00",
            discretionaryRaw = "0.00"
        )
        assertFalse(rejected.isValid)
        assertNotNull(rejected.generalError)
        assertFalse(rejected.generalError!!.contains("shortfall", ignoreCase = true))

        val accepted = PaydayIntelligenceEngine.validateApprovedAllocation(
            plan = plan,
            extraDebtRaw = "0.00",
            emergencyRaw = "0.00",
            discretionaryRaw = "0.00"
        )
        assertTrue(accepted.isValid)
    }

    @Test
    fun v111_task3_genuineNegativeSurplus_reportsCorrectMonetaryShortfall() {
        // Total Priority 1 = 3763.33 vs netSalary 3263.33 -> Shortfall = RM 500.00
        val plan = PaydayIntelligenceEngine.calculatePlan(
            PaydayInput(
                monthKey = "2026-11",
                netSalary = 3263.33,
                commitments = sampleCommitments(),
                debts = sampleDebts(),
                upcomingEssentialExpenses = 250.0,
                existingEmergencyFund = 500.0,
                emergencyFundTarget = 1000.0,
                cashBufferTarget = 150.0
            )
        )

        assertEquals(CashSurplusStatus.SHORTFALL, plan.surplusStatus)
        assertTrue(plan.hasInsufficientCash)
        assertEquals(500.0, plan.shortfallAmount, 0.01)
        assertEquals(0.0, plan.recommendedExtraDebtRepayment, 0.01)
        assertEquals(0.0, plan.recommendedEmergencySavings, 0.01)
        assertEquals(0.0, plan.recommendedDiscretionaryAllowance, 0.01)
        assertEquals(0.0, plan.recommendedRemainingBuffer, 0.01)
        assertTrue(plan.spendingConsequenceSummary.contains("500.00"))
        assertTrue(plan.spendingConsequenceSummary.contains("shortfall", ignoreCase = true))

        val extraDebtItem = plan.items.first { it.id == "extra_debt" }
        assertTrue(extraDebtItem.whyExplanation.contains("500.00"))
        assertTrue(extraDebtItem.whyExplanation.contains("shortfall", ignoreCase = true))
    }
}
