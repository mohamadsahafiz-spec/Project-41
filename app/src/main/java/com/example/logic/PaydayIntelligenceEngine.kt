package com.example.logic

import com.example.data.MonthlyCommitmentItem
import com.example.data.model.DebtEntity
import com.example.data.model.MonthlyIncomeEntity
import com.example.util.DateUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

enum class DebtRateClassification {
    EFFECTIVE_APR,
    PROVISIONAL_RATE,
    REFERENCE_ONLY,
    MISSING_RATE
}

data class RankedDebtCandidate(
    val debtKey: String,
    val debtName: String,
    val currentBalance: Double,
    val monthlyTarget: Double,
    val effectiveAprPercent: Double,
    val classification: DebtRateClassification,
    val isEligibleForExtraRepayment: Boolean,
    val note: String
)

data class PaydayInput(
    val monthKey: String,
    val netSalary: Double,
    val overtime: Double = 0.0,
    val incentives: Double = 0.0,
    val bonuses: Double = 0.0,
    val otherIncome: Double = 0.0,
    val reimbursements: Double = 0.0,
    val unconfirmedIncome: Double = 0.0,
    val isIncomeConfirmed: Boolean = true,
    val existingEverydayCash: Double = 0.0,
    val commitments: List<MonthlyCommitmentItem> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val recordedExpensesThisMonth: Double = 0.0,
    val upcomingEssentialExpenses: Double? = null,
    val existingEmergencyFund: Double = 0.0,
    val emergencyFundTarget: Double? = null,
    val cashBufferTarget: Double? = null,
    val debtRateOverrides: Map<String, DebtRateClassification> = emptyMap()
) {
    companion object {
        fun fromMonthlyState(
            monthKey: String,
            income: MonthlyIncomeEntity?,
            defaultNetSalary: Double,
            existingEverydayCash: Double,
            commitments: List<MonthlyCommitmentItem>,
            debts: List<DebtEntity>,
            recordedExpensesThisMonth: Double,
            upcomingEssentialExpenses: Double?,
            existingEmergencyFund: Double,
            emergencyFundTarget: Double?,
            cashBufferTarget: Double?
        ): PaydayInput {
            return PaydayInput(
                monthKey = monthKey,
                netSalary = income?.netSalary ?: defaultNetSalary,
                overtime = income?.overtime ?: 0.0,
                incentives = income?.incentives ?: 0.0,
                bonuses = income?.thirteenthMonth ?: 0.0,
                otherIncome = income?.otherIncome ?: 0.0,
                reimbursements = income?.reimbursements ?: 0.0,
                unconfirmedIncome = income?.unconfirmedIncome ?: 0.0,
                isIncomeConfirmed = income?.isConfirmedReceived ?: true,
                existingEverydayCash = existingEverydayCash,
                commitments = commitments,
                debts = debts,
                recordedExpensesThisMonth = recordedExpensesThisMonth,
                upcomingEssentialExpenses = upcomingEssentialExpenses,
                existingEmergencyFund = existingEmergencyFund,
                emergencyFundTarget = emergencyFundTarget,
                cashBufferTarget = cashBufferTarget
            )
        }
    }
}

data class PaydayRecommendationItem(
    val id: String,
    val priorityRank: Int,
    val title: String,
    val recommendedAmount: Double,
    val whyExplanation: String,
    val isEditable: Boolean = false
)

data class PaydayAllocationPlan(
    val monthKey: String,
    val confirmedRegularIncome: Double,
    val confirmedAdditionalIncome: Double,
    val totalConfirmedIncome: Double,
    val reimbursementsExcluded: Double,
    val unconfirmedIncomeExcluded: Double,
    val alreadyPaidCommitments: Double,
    val recordedExpensesThisMonth: Double,
    val actualAvailableCash: Double,
    val essentialCommitmentsReserved: Double,
    val minimumDebtPaymentsReserved: Double,
    val upcomingEssentialExpensesReserved: Double,
    val recommendedExtraDebtRepayment: Double,
    val recommendedExtraDebtTargetKey: String?,
    val recommendedExtraDebtTargetName: String?,
    val recommendedEmergencySavings: Double,
    val recommendedDiscretionaryAllowance: Double,
    val recommendedRemainingBuffer: Double,
    val hasInsufficientCash: Boolean,
    val shortfallAmount: Double,
    val rankedDebts: List<RankedDebtCandidate>,
    val items: List<PaydayRecommendationItem>,
    val additionalIncomeWarning: String?,
    val spendingConsequenceSummary: String,
    val dataGaps: List<String>
)

object PaydayIntelligenceEngine {

    fun classifyDebtRate(
        debt: DebtEntity,
        overrideClassification: DebtRateClassification? = null
    ): DebtRateClassification {
        if (overrideClassification != null) return overrideClassification
        if (debt.aprPercent <= 0.0) return DebtRateClassification.MISSING_RATE
        if (debt.key == "SHOPEE_SLOAN") return DebtRateClassification.PROVISIONAL_RATE
        val isReferenceHp = debt.key == "HL_HP1" ||
            debt.key == "AMBANK_HP2" ||
            (!debt.isHighInterest &&
                (debt.key.contains("HP", ignoreCase = true) ||
                    debt.name.contains("Hire Purchase", ignoreCase = true) ||
                    debt.name.contains("HP1", ignoreCase = true) ||
                    debt.name.contains("HP2", ignoreCase = true)))
        if (isReferenceHp) return DebtRateClassification.REFERENCE_ONLY
        return DebtRateClassification.EFFECTIVE_APR
    }

    fun calculatePlan(input: PaydayInput): PaydayAllocationPlan {
        val dataGaps = mutableListOf<String>()

        val regularNet = max(0.0, input.netSalary)
        val overtime = max(0.0, input.overtime)
        val incentives = max(0.0, input.incentives)
        val bonuses = max(0.0, input.bonuses)
        val otherIncome = max(0.0, input.otherIncome)
        val rawAdditional = round2(overtime + incentives + bonuses + otherIncome)
        val reimbursementsExcluded = max(0.0, input.reimbursements)

        val confirmedRegularIncome: Double
        val confirmedAdditionalIncome: Double
        val unconfirmedIncomeExcluded: Double

        if (input.isIncomeConfirmed) {
            confirmedRegularIncome = round2(regularNet)
            confirmedAdditionalIncome = rawAdditional
            unconfirmedIncomeExcluded = round2(max(0.0, input.unconfirmedIncome))
        } else {
            confirmedRegularIncome = 0.0
            confirmedAdditionalIncome = 0.0
            unconfirmedIncomeExcluded = round2(regularNet + rawAdditional + max(0.0, input.unconfirmedIncome))
            dataGaps.add(
                "Income receipt is marked unconfirmed (${DateUtils.formatCurrency(unconfirmedIncomeExcluded)} excluded from available cash until received)."
            )
        }

        if (reimbursementsExcluded > 0.0) {
            // Reimbursements must never count as spendable income or inflate cashflow
        }

        val totalConfirmedIncome = round2(confirmedRegularIncome + confirmedAdditionalIncome)

        // Already recorded outflows in this month so we do not double-count paid commitments or spent expenses
        val alreadyPaidCommitments = round2(
            input.commitments.filter { it.isPaid }.sumOf { max(0.0, it.actualPaidAmount) }
        )
        val recordedExpenses = round2(max(0.0, input.recordedExpensesThisMonth))
        val actualAvailableCash = round2(
            max(0.0, totalConfirmedIncome - alreadyPaidCommitments - recordedExpenses)
        )

        // Priority 1A: Upcoming essential (non-debt) commitments
        val unpaidEssentialCommitments = input.commitments.filter { !it.isDebt && !it.isPaid }
        val essentialCommitmentsReserved = round2(
            unpaidEssentialCommitments.sumOf { max(0.0, it.defaultAmount) }
        )

        // Priority 1B: Minimum debt payments (unpaid debt commitments, plus any active debt without a commitment row)
        val unpaidDebtCommitments = input.commitments.filter { it.isDebt && !it.isPaid }
        val coveredDebtKeys = input.commitments.mapNotNull { it.debtKey }.toSet()
        val uncoveredActiveDebtsMin = input.debts
            .filter { it.currentBalance > 0.01 && it.key !in coveredDebtKeys }
            .sumOf { max(0.0, it.monthlyTarget) }
        val minimumDebtPaymentsReserved = round2(
            unpaidDebtCommitments.sumOf { max(0.0, it.defaultAmount) } + uncoveredActiveDebtsMin
        )

        // Priority 1C: Upcoming essential living expenses
        val upcomingEssentialExpensesReserved = if (input.upcomingEssentialExpenses != null) {
            round2(max(0.0, input.upcomingEssentialExpenses))
        } else {
            dataGaps.add(
                "Upcoming essential living expenses (groceries, petrol, utilities) are not provided. Enter an estimate to avoid over-allocating cash."
            )
            0.0
        }

        val totalPriority1Required = round2(
            essentialCommitmentsReserved + minimumDebtPaymentsReserved + upcomingEssentialExpensesReserved
        )

        // Rank debts by effective interest rate and eligibility
        val rankedDebts = input.debts
            .filter { it.currentBalance > 0.01 }
            .map { debt ->
                val classification = classifyDebtRate(debt, input.debtRateOverrides[debt.key])
                val eligible = classification == DebtRateClassification.EFFECTIVE_APR ||
                    classification == DebtRateClassification.PROVISIONAL_RATE
                val note = when (classification) {
                    DebtRateClassification.EFFECTIVE_APR ->
                        "Confirmed ${formatRate(debt.aprPercent)}% p.a. effective interest — eligible for extra debt repayment."
                    DebtRateClassification.PROVISIONAL_RATE ->
                        "Provisional ${formatRate(debt.aprPercent)}% p.a. (1.5% monthly) — eligible for high-interest priority."
                    DebtRateClassification.REFERENCE_ONLY ->
                        "Reference ${formatRate(debt.aprPercent)}% rate only (vehicle HP). Early settlement savings cannot be assumed without lender rebate quote."
                    DebtRateClassification.MISSING_RATE ->
                        "Interest rate missing or unverified. Cannot rank for extra repayment optimization."
                }
                RankedDebtCandidate(
                    debtKey = debt.key,
                    debtName = debt.name,
                    currentBalance = round2(debt.currentBalance),
                    monthlyTarget = round2(debt.monthlyTarget),
                    effectiveAprPercent = debt.aprPercent,
                    classification = classification,
                    isEligibleForExtraRepayment = eligible,
                    note = note
                )
            }
            .sortedWith(
                compareByDescending<RankedDebtCandidate> { it.isEligibleForExtraRepayment }
                    .thenByDescending { it.effectiveAprPercent }
                    .thenBy { it.currentBalance }
            )

        rankedDebts.filter { it.classification == DebtRateClassification.MISSING_RATE }.forEach { missing ->
            dataGaps.add("Missing effective interest rate for ${missing.debtName}; excluded from automatic debt ranking.")
        }
        val referenceOnlyDebts = rankedDebts.filter { it.classification == DebtRateClassification.REFERENCE_ONLY }
        if (referenceOnlyDebts.isNotEmpty() && rankedDebts.none { it.isEligibleForExtraRepayment }) {
            dataGaps.add(
                "Remaining debts (${referenceOnlyDebts.joinToString { it.debtName }}) use a 3% reference rate without confirmed early-settlement rebate info."
            )
        }

        val eligibleDebts = rankedDebts.filter { it.isEligibleForExtraRepayment }
        val totalEligibleDebtBalance = round2(eligibleDebts.sumOf { it.currentBalance })
        val topEligibleDebt = eligibleDebts.firstOrNull()

        // Evaluate emergency savings vs configured target
        val existingEmergencyFund = round2(max(0.0, input.existingEmergencyFund))
        val emergencyTarget = input.emergencyFundTarget?.let { round2(max(0.0, it)) }
        if (emergencyTarget == null) {
            dataGaps.add(
                "Emergency fund target is not configured. Set a target (e.g., Starter RM 1,000 or 3-Month RM 10,000) for personalized emergency allocation."
            )
        }
        val emergencyGap = if (emergencyTarget != null) {
            round2(max(0.0, emergencyTarget - existingEmergencyFund))
        } else {
            0.0
        }

        if (input.cashBufferTarget == null) {
            dataGaps.add(
                "Cash buffer target is not explicitly set; reserving a conservative liquidity buffer when surplus allows."
            )
        }

        val cashAfterPriority1 = round2(actualAvailableCash - totalPriority1Required)
        val hasInsufficientCash = cashAfterPriority1 <= 0.0
        val shortfallAmount = if (cashAfterPriority1 < 0.0) round2(-cashAfterPriority1) else 0.0

        val recommendedExtraDebtRepayment: Double
        val recommendedEmergencySavings: Double
        val recommendedDiscretionaryAllowance: Double
        val recommendedRemainingBuffer: Double

        if (hasInsufficientCash) {
            recommendedExtraDebtRepayment = 0.0
            recommendedEmergencySavings = 0.0
            recommendedDiscretionaryAllowance = 0.0
            recommendedRemainingBuffer = 0.0
        } else {
            val allocatableSurplus = cashAfterPriority1

            // Priority 2A: Preserve reasonable cash buffer
            val desiredBuffer = input.cashBufferTarget?.let { max(0.0, it) }
                ?: min(200.0, round2(allocatableSurplus * 0.10))
            val reservedBuffer = round2(min(allocatableSurplus, desiredBuffer))
            val poolAfterBuffer = round2(max(0.0, allocatableSurplus - reservedBuffer))

            // Priority 4A: Controlled discretionary allowance ONLY from regular income pool
            // Additional income (OT, incentives, bonuses, other) never inflates discretionary spending.
            val additionalPool = min(poolAfterBuffer, confirmedAdditionalIncome)
            val regularPool = round2(max(0.0, poolAfterBuffer - additionalPool))

            val starterThreshold = if (emergencyTarget != null) min(1000.0, emergencyTarget) else 0.0
            val needsStarterUrgently = emergencyTarget != null && existingEmergencyFund < starterThreshold

            recommendedDiscretionaryAllowance = when {
                regularPool <= 0.0 -> 0.0
                needsStarterUrgently && regularPool < 200.0 -> 0.0
                emergencyGap == 0.0 && totalEligibleDebtBalance == 0.0 ->
                    round2(min(500.0, regularPool * 0.25))
                else ->
                    round2(min(300.0, regularPool * 0.15))
            }

            val allocationPool = round2(max(0.0, poolAfterBuffer - recommendedDiscretionaryAllowance))

            // Priority 2B & 3: Emergency savings vs Debt optimisation
            var recEmergency = 0.0
            var recExtraDebt = 0.0

            if (emergencyGap > 0.0 && totalEligibleDebtBalance > 0.0) {
                var remainingPool = allocationPool

                // Protect starter buffer first (Flowchart Step 1)
                val starterGap = round2(max(0.0, starterThreshold - existingEmergencyFund))
                if (starterGap > 0.0) {
                    val starterAlloc = round2(min(remainingPool, starterGap))
                    recEmergency = round2(recEmergency + starterAlloc)
                    remainingPool = round2(remainingPool - starterAlloc)
                }

                if (remainingPool > 0.0) {
                    val remainingEfGap = round2(max(0.0, emergencyGap - recEmergency))
                    val highInterestPresent = eligibleDebts.any { it.effectiveAprPercent >= 10.0 }
                    val debtShare = if (highInterestPresent) 0.70 else 0.30

                    val initialDebtAlloc = round2(min(totalEligibleDebtBalance, remainingPool * debtShare))
                    val initialEfAlloc = round2(min(remainingEfGap, remainingPool - initialDebtAlloc))

                    recExtraDebt = initialDebtAlloc
                    recEmergency = round2(recEmergency + initialEfAlloc)

                    // Cascade any unabsorbed remainder between EF gap and eligible debt
                    var leftover = round2(remainingPool - initialDebtAlloc - initialEfAlloc)
                    if (leftover > 0.0 && totalEligibleDebtBalance > recExtraDebt) {
                        val extraToDebt = round2(min(leftover, totalEligibleDebtBalance - recExtraDebt))
                        recExtraDebt = round2(recExtraDebt + extraToDebt)
                        leftover = round2(leftover - extraToDebt)
                    }
                    if (leftover > 0.0 && emergencyGap > recEmergency) {
                        val extraToEf = round2(min(leftover, emergencyGap - recEmergency))
                        recEmergency = round2(recEmergency + extraToEf)
                    }
                }
            } else if (emergencyGap > 0.0) {
                recEmergency = round2(min(allocationPool, emergencyGap))
            } else if (totalEligibleDebtBalance > 0.0) {
                recExtraDebt = round2(min(allocationPool, totalEligibleDebtBalance))
            }

            recommendedEmergencySavings = recEmergency
            recommendedExtraDebtRepayment = recExtraDebt
            recommendedRemainingBuffer = round2(
                max(
                    0.0,
                    actualAvailableCash -
                        totalPriority1Required -
                        recommendedEmergencySavings -
                        recommendedExtraDebtRepayment -
                        recommendedDiscretionaryAllowance
                )
            )
        }

        val additionalIncomeWarning = if (confirmedAdditionalIncome > 0.0) {
            "Additional income of ${DateUtils.formatCurrency(confirmedAdditionalIncome)} (OT/incentives/bonus) is excluded from discretionary spending so windfalls accelerate protection and debt payoff."
        } else {
            null
        }

        val spendingConsequenceSummary = buildSpendingConsequence(
            hasInsufficientCash = hasInsufficientCash,
            shortfallAmount = shortfallAmount,
            recommendedExtraDebtRepayment = recommendedExtraDebtRepayment,
            topEligibleDebt = topEligibleDebt,
            recommendedEmergencySavings = recommendedEmergencySavings,
            emergencyFundTarget = emergencyTarget,
            existingEmergencyFund = existingEmergencyFund,
            confirmedAdditionalIncome = confirmedAdditionalIncome
        )

        val items = buildRecommendationItems(
            input = input,
            totalConfirmedIncome = totalConfirmedIncome,
            reimbursementsExcluded = reimbursementsExcluded,
            unconfirmedIncomeExcluded = unconfirmedIncomeExcluded,
            actualAvailableCash = actualAvailableCash,
            essentialCommitmentsReserved = essentialCommitmentsReserved,
            unpaidEssentialCount = unpaidEssentialCommitments.size,
            minimumDebtPaymentsReserved = minimumDebtPaymentsReserved,
            unpaidDebtCount = unpaidDebtCommitments.size,
            upcomingEssentialExpensesReserved = upcomingEssentialExpensesReserved,
            hasUpcomingExpenseInput = input.upcomingEssentialExpenses != null,
            recommendedExtraDebtRepayment = recommendedExtraDebtRepayment,
            topEligibleDebt = topEligibleDebt,
            eligibleDebts = eligibleDebts,
            referenceOnlyDebts = referenceOnlyDebts,
            recommendedEmergencySavings = recommendedEmergencySavings,
            existingEmergencyFund = existingEmergencyFund,
            emergencyFundTarget = emergencyTarget,
            recommendedDiscretionaryAllowance = recommendedDiscretionaryAllowance,
            confirmedAdditionalIncome = confirmedAdditionalIncome,
            recommendedRemainingBuffer = recommendedRemainingBuffer,
            hasInsufficientCash = hasInsufficientCash,
            shortfallAmount = shortfallAmount
        )

        return PaydayAllocationPlan(
            monthKey = input.monthKey,
            confirmedRegularIncome = confirmedRegularIncome,
            confirmedAdditionalIncome = confirmedAdditionalIncome,
            totalConfirmedIncome = totalConfirmedIncome,
            reimbursementsExcluded = reimbursementsExcluded,
            unconfirmedIncomeExcluded = unconfirmedIncomeExcluded,
            alreadyPaidCommitments = alreadyPaidCommitments,
            recordedExpensesThisMonth = recordedExpenses,
            actualAvailableCash = actualAvailableCash,
            essentialCommitmentsReserved = essentialCommitmentsReserved,
            minimumDebtPaymentsReserved = minimumDebtPaymentsReserved,
            upcomingEssentialExpensesReserved = upcomingEssentialExpensesReserved,
            recommendedExtraDebtRepayment = recommendedExtraDebtRepayment,
            recommendedExtraDebtTargetKey = if (recommendedExtraDebtRepayment > 0.0) topEligibleDebt?.debtKey else null,
            recommendedExtraDebtTargetName = if (recommendedExtraDebtRepayment > 0.0) topEligibleDebt?.debtName else null,
            recommendedEmergencySavings = recommendedEmergencySavings,
            recommendedDiscretionaryAllowance = recommendedDiscretionaryAllowance,
            recommendedRemainingBuffer = recommendedRemainingBuffer,
            hasInsufficientCash = hasInsufficientCash,
            shortfallAmount = shortfallAmount,
            rankedDebts = rankedDebts,
            items = items,
            additionalIncomeWarning = additionalIncomeWarning,
            spendingConsequenceSummary = spendingConsequenceSummary,
            dataGaps = dataGaps
        )
    }

    private fun buildSpendingConsequence(
        hasInsufficientCash: Boolean,
        shortfallAmount: Double,
        recommendedExtraDebtRepayment: Double,
        topEligibleDebt: RankedDebtCandidate?,
        recommendedEmergencySavings: Double,
        emergencyFundTarget: Double?,
        existingEmergencyFund: Double,
        confirmedAdditionalIncome: Double
    ): String {
        if (hasInsufficientCash) {
            return "Spending any cash right now deepens your obligation shortfall of ${DateUtils.formatCurrency(shortfallAmount)} and risks late fees or credit card reliance."
        }
        val totalImpactfulAllocation = round2(recommendedExtraDebtRepayment + recommendedEmergencySavings)
        if (totalImpactfulAllocation <= 0.0) {
            return "No unallocated extra debt or emergency contribution is pending this month; keep discretionary spending within the recommended cap to preserve your liquidity buffer."
        }

        val parts = mutableListOf<String>()
        if (recommendedExtraDebtRepayment > 0.0 && topEligibleDebt != null) {
            val annualInterestSaved = round2(recommendedExtraDebtRepayment * (topEligibleDebt.effectiveAprPercent / 100.0))
            parts.add(
                "keeping ${DateUtils.formatCurrency(recommendedExtraDebtRepayment)} from going to ${topEligibleDebt.debtName} (${formatRate(topEligibleDebt.effectiveAprPercent)}% p.a.) leaves ~${DateUtils.formatCurrency(annualInterestSaved)}/year in avoidable interest and delays clearing that balance"
            )
        }
        if (recommendedEmergencySavings > 0.0 && emergencyFundTarget != null) {
            val newFund = round2(existingEmergencyFund + recommendedEmergencySavings)
            parts.add(
                "skipping ${DateUtils.formatCurrency(recommendedEmergencySavings)} in emergency savings keeps your reserve at ${DateUtils.formatCurrency(existingEmergencyFund)} instead of reaching ${DateUtils.formatCurrency(newFund)} toward your ${DateUtils.formatCurrency(emergencyFundTarget)} target"
            )
        }
        val prefix = if (confirmedAdditionalIncome > 0.0) {
            "If you treat your surplus/additional income as spending money instead of allocating it: "
        } else {
            "If you spend your surplus instead of allocating it: "
        }
        return prefix + parts.joinToString("; and ") + "."
    }

    private fun buildRecommendationItems(
        input: PaydayInput,
        totalConfirmedIncome: Double,
        reimbursementsExcluded: Double,
        unconfirmedIncomeExcluded: Double,
        actualAvailableCash: Double,
        essentialCommitmentsReserved: Double,
        unpaidEssentialCount: Int,
        minimumDebtPaymentsReserved: Double,
        unpaidDebtCount: Int,
        upcomingEssentialExpensesReserved: Double,
        hasUpcomingExpenseInput: Boolean,
        recommendedExtraDebtRepayment: Double,
        topEligibleDebt: RankedDebtCandidate?,
        eligibleDebts: List<RankedDebtCandidate>,
        referenceOnlyDebts: List<RankedDebtCandidate>,
        recommendedEmergencySavings: Double,
        existingEmergencyFund: Double,
        emergencyFundTarget: Double?,
        recommendedDiscretionaryAllowance: Double,
        confirmedAdditionalIncome: Double,
        recommendedRemainingBuffer: Double,
        hasInsufficientCash: Boolean,
        shortfallAmount: Double
    ): List<PaydayRecommendationItem> {
        val exclusionNotes = buildList {
            if (reimbursementsExcluded > 0.0) {
                add("excludes ${DateUtils.formatCurrency(reimbursementsExcluded)} reimbursements")
            }
            if (unconfirmedIncomeExcluded > 0.0) {
                add("excludes ${DateUtils.formatCurrency(unconfirmedIncomeExcluded)} unconfirmed income")
            }
        }
        val availableWhy = buildString {
            append("Confirmed income is ${DateUtils.formatCurrency(totalConfirmedIncome)}")
            if (exclusionNotes.isNotEmpty()) {
                append(" (${exclusionNotes.joinToString(", ")})")
            }
            append(". After ${DateUtils.formatCurrency(totalConfirmedIncome - actualAvailableCash)} already paid/spent this month, ${DateUtils.formatCurrency(actualAvailableCash)} is available to allocate.")
        }

        val essentialWhy = if (essentialCommitmentsReserved > 0.0) {
            "Priority 1: Protects $unpaidEssentialCount unpaid essential living commitment(s) (e.g., rent, family, utilities) before any extra allocation."
        } else {
            "Priority 1: All recurring non-debt commitments are already marked paid for ${DateUtils.formatMonthKey(input.monthKey)}."
        }

        val minDebtWhy = if (minimumDebtPaymentsReserved > 0.0) {
            "Priority 1: Reserves scheduled instalments across $unpaidDebtCount unpaid debt commitment(s) to prevent arrears or late charges."
        } else {
            "Priority 1: All scheduled debt instalments are already marked paid for this month."
        }

        val upcomingEssentialsWhy = if (hasUpcomingExpenseInput) {
            "Priority 1: Reserves your budgeted ${DateUtils.formatCurrency(upcomingEssentialExpensesReserved)} for upcoming groceries, fuel, and essentials."
        } else {
            "Priority 1: Not yet entered — enter your upcoming essential living expenses above so surplus isn't overstated."
        }

        val emergencyWhy = when {
            hasInsufficientCash ->
                "Paused due to ${DateUtils.formatCurrency(shortfallAmount)} obligation shortfall; restore positive cashflow first."
            emergencyFundTarget == null ->
                "Emergency target is not set. Current dedicated emergency fund is ${DateUtils.formatCurrency(existingEmergencyFund)}."
            existingEmergencyFund >= emergencyFundTarget ->
                "Your emergency fund (${DateUtils.formatCurrency(existingEmergencyFund)}) already meets your ${DateUtils.formatCurrency(emergencyFundTarget)} target."
            recommendedEmergencySavings > 0.0 ->
                "Priority 2: Emergency fund is ${DateUtils.formatCurrency(existingEmergencyFund)} of ${DateUtils.formatCurrency(emergencyFundTarget)}. Adds ${DateUtils.formatCurrency(recommendedEmergencySavings)} while preserving your cash buffer."
            else ->
                "No remaining surplus left after Priority 1 obligations and cash buffer."
        }

        val extraDebtWhy = when {
            hasInsufficientCash ->
                "Paused due to ${DateUtils.formatCurrency(shortfallAmount)} obligation shortfall; protect minimum repayments first."
            eligibleDebts.isEmpty() && referenceOnlyDebts.isNotEmpty() ->
                "Only 3% reference hire-purchase loans remain (HP1/HP2); early settlement benefit cannot be assumed without lender rebate confirmation."
            eligibleDebts.isEmpty() ->
                "No active eligible debts require extra repayment."
            recommendedExtraDebtRepayment <= 0.0 ->
                "No remaining surplus after Priority 1 obligations, cash buffer, and starter emergency protection."
            topEligibleDebt != null -> {
                val cascadeNote = if (recommendedExtraDebtRepayment > topEligibleDebt.currentBalance && eligibleDebts.size > 1) {
                    val second = eligibleDebts[1]
                    " Clears ${topEligibleDebt.debtName} (${DateUtils.formatCurrency(topEligibleDebt.currentBalance)}) and rolls remainder into ${second.debtName} (${formatRate(second.effectiveAprPercent)}%)."
                } else {
                    ""
                }
                "Priority 3: Targets ${topEligibleDebt.debtName} first (${formatRate(topEligibleDebt.effectiveAprPercent)}% p.a. — highest eligible rate).$cascadeNote"
            }
            else -> "No eligible extra debt recommendation."
        }

        val discretionaryWhy = when {
            hasInsufficientCash ->
                "Paused to close your ${DateUtils.formatCurrency(shortfallAmount)} essential shortfall."
            recommendedDiscretionaryAllowance <= 0.0 && confirmedAdditionalIncome > 0.0 ->
                "Priority 4: Regular salary surplus is tight; ${DateUtils.formatCurrency(confirmedAdditionalIncome)} in OT/incentives/bonus is held out of spending money."
            recommendedDiscretionaryAllowance <= 0.0 ->
                "Priority 4: No regular surplus available for discretionary spending after protection and debt priorities."
            confirmedAdditionalIncome > 0.0 ->
                "Priority 4: Sized conservatively from regular salary surplus only; ${DateUtils.formatCurrency(confirmedAdditionalIncome)} additional income is protected from lifestyle creep."
            else ->
                "Priority 4: Controlled guilt-free spending allowance leaving surplus for protection and debt payoff."
        }

        val bufferWhy = if (hasInsufficientCash) {
            "Buffer depleted by ${DateUtils.formatCurrency(shortfallAmount)} obligation shortfall; adjust expenses or confirm pending income."
        } else {
            "Priority 2: Keeps ${DateUtils.formatCurrency(recommendedRemainingBuffer)} liquid in Everyday Cash to absorb timing mismatches."
        }

        return listOf(
            PaydayRecommendationItem(
                id = "available_cash",
                priorityRank = 0,
                title = "Actual Available Cash",
                recommendedAmount = actualAvailableCash,
                whyExplanation = availableWhy,
                isEditable = false
            ),
            PaydayRecommendationItem(
                id = "essential_commitments",
                priorityRank = 1,
                title = "Essential Commitments Reserved",
                recommendedAmount = essentialCommitmentsReserved,
                whyExplanation = essentialWhy,
                isEditable = false
            ),
            PaydayRecommendationItem(
                id = "minimum_debt",
                priorityRank = 1,
                title = "Minimum Debt Payments Reserved",
                recommendedAmount = minimumDebtPaymentsReserved,
                whyExplanation = minDebtWhy,
                isEditable = false
            ),
            PaydayRecommendationItem(
                id = "upcoming_essentials",
                priorityRank = 1,
                title = "Upcoming Essential Living Expenses",
                recommendedAmount = upcomingEssentialExpensesReserved,
                whyExplanation = upcomingEssentialsWhy,
                isEditable = false
            ),
            PaydayRecommendationItem(
                id = "emergency_savings",
                priorityRank = 2,
                title = "Recommended Emergency Savings",
                recommendedAmount = recommendedEmergencySavings,
                whyExplanation = emergencyWhy,
                isEditable = true
            ),
            PaydayRecommendationItem(
                id = "extra_debt",
                priorityRank = 3,
                title = "Recommended Extra Debt Repayment",
                recommendedAmount = recommendedExtraDebtRepayment,
                whyExplanation = extraDebtWhy,
                isEditable = true
            ),
            PaydayRecommendationItem(
                id = "discretionary",
                priorityRank = 4,
                title = "Discretionary Spending Allowance",
                recommendedAmount = recommendedDiscretionaryAllowance,
                whyExplanation = discretionaryWhy,
                isEditable = true
            ),
            PaydayRecommendationItem(
                id = "remaining_buffer",
                priorityRank = 2,
                title = "Remaining Cash Buffer",
                recommendedAmount = recommendedRemainingBuffer,
                whyExplanation = bufferWhy,
                isEditable = false
            )
        )
    }

    private fun formatRate(rate: Double): String {
        return if (rate % 1.0 == 0.0) rate.toInt().toString() else String.format(java.util.Locale.US, "%.1f", rate)
    }

    private fun round2(value: Double): Double {
        return (value * 100.0).roundToLong() / 100.0
    }
}
