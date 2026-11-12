package com.example.logic

import com.example.data.model.DebtEntity
import com.example.data.model.FinancialProfileEntity
import kotlin.math.max
import kotlin.math.min

enum class StepStatus {
    COMPLETED,
    ACTIVE,
    LOCKED
}

data class FlowchartStepInfo(
    val stepNumber: Int,
    val title: String,
    val shortName: String,
    val description: String,
    val status: StepStatus,
    val progressPercent: Float,
    val currentAmount: Double,
    val targetAmount: Double,
    val malaysianAdaptationNote: String,
    val actionTip: String
)

data class FlowchartJourneyState(
    val currentStepNumber: Int,
    val currentStepTitle: String,
    val currentStepProgressPercent: Float,
    val overallJourneyProgressPercent: Float,
    val nextTargetTitle: String,
    val nextTargetAmount: Double,
    val nextTargetRemaining: Double,
    val steps: List<FlowchartStepInfo>,
    val totalDebt: Double,
    val highInterestDebt: Double,
    val moderateInterestDebt: Double,
    val accessibleSavings: Double,
    val totalKwsp: Double,
    val estimatedMonthlyLivingExpenses: Double
)

object FlowchartCalculator {
    const val SOURCE_URL = "https://ukpersonal.finance/flowchart/"
    const val FLOWCHART_VERSION = "UK Personal Finance Flowchart v3.0.10 (Adapted for Malaysia)"

    fun calculateJourney(
        profile: FinancialProfileEntity,
        debts: List<DebtEntity>,
        hasRecordedAnyExpense: Boolean = true
    ): FlowchartJourneyState {
        val totalDebt = debts.sumOf { it.currentBalance }
        val highInterestDebts = debts.filter { it.isHighInterest }
        val moderateInterestDebts = debts.filter { !it.isHighInterest }

        val highInterestBalance = highInterestDebts.sumOf { it.currentBalance }
        val highInterestInitial = highInterestDebts.sumOf { it.initialBalance }.let { if (it <= 0) 12076.76 else it }

        val moderateInterestBalance = moderateInterestDebts.sumOf { it.currentBalance }
        val moderateInterestInitial = moderateInterestDebts.sumOf { it.initialBalance }.let { if (it <= 0) 20568.0 else it }

        val accessibleSavings = profile.totalAccessibleSavings
        val totalKwsp = profile.totalKwsp

        // Estimate essential monthly living expenses (rent + kids + utilities + food + parents)
        // Around RM3,300/month
        val monthlyEssentials = 3300.0

        // Step 0: Budgeting & Basics
        val step0Completed = hasRecordedAnyExpense
        val step0 = FlowchartStepInfo(
            stepNumber = 0,
            title = "Step 0: Budgeting & Essentials Tracking",
            shortName = "Budget & Essentials",
            description = "Track all income and essential monthly commitments to prevent overspending and late fees.",
            status = if (step0Completed) StepStatus.COMPLETED else StepStatus.ACTIVE,
            progressPercent = if (step0Completed) 100f else 50f,
            currentAmount = if (step0Completed) 1.0 else 0.0,
            targetAmount = 1.0,
            malaysianAdaptationNote = "Adapted with recurring Malaysian commitments: Rent (RM700), Parents (RM200), Mother's fibre (RM95), Playschool (RM550), and Formula (RM100).",
            actionTip = "Check in on your monthly commitments between the 27th and month-end."
        )

        // Step 1: Starter Emergency Fund (RM1,000 Buffer)
        val step1Target = 1000.0
        val step1Completed = accessibleSavings >= step1Target
        val step1Progress = min(100f, (accessibleSavings / step1Target * 100.0).toFloat())
        val step1 = FlowchartStepInfo(
            stepNumber = 1,
            title = "Step 1: Starter Emergency Fund (RM1,000 Buffer)",
            shortName = "Starter Buffer",
            description = "Hold a minimum liquid cash reserve of RM1,000 to handle minor shocks without borrowing.",
            status = if (step1Completed) StepStatus.COMPLETED else StepStatus.ACTIVE,
            progressPercent = step1Progress,
            currentAmount = accessibleSavings,
            targetAmount = step1Target,
            malaysianAdaptationNote = "UK £500-£1,000 starter buffer adapted to RM1,000 liquid bank balance.",
            actionTip = "Keep this buffer untouched in your primary bank account."
        )

        // Step 2: Mandatory Retirement Match / Maximise Free Money
        val step2Completed = totalKwsp > 0
        val step2 = FlowchartStepInfo(
            stepNumber = 2,
            title = "Step 2: Workplace Pension Match / Statutory KWSP",
            shortName = "Workplace EPF/KWSP",
            description = "Maximise mandatory statutory employer retirement contributions.",
            status = StepStatus.COMPLETED,
            progressPercent = 100f,
            currentAmount = totalKwsp,
            targetAmount = totalKwsp,
            malaysianAdaptationNote = "In Malaysia, statutory KWSP (EPF) 11% employee + 12-13% employer match is automatically deducted for salaried engineers. You have RM99,179.72 accumulated across Accounts 1, 2, and 3.",
            actionTip = "Statutory employer contributions are actively building your retirement pillar."
        )

        // Step 3: High-Interest Debt Elimination (> 10% APR)
        val step3Completed = highInterestBalance <= 0.01
        val step3Progress = if (step3Completed) 100f else {
            val paidOff = max(0.0, highInterestInitial - highInterestBalance)
            max(0f, min(100f, (paidOff / highInterestInitial * 100.0).toFloat()))
        }
        val step3Status = when {
            step3Completed -> StepStatus.COMPLETED
            step1Completed -> StepStatus.ACTIVE
            else -> StepStatus.LOCKED
        }
        val step3 = FlowchartStepInfo(
            stepNumber = 3,
            title = "Step 3: High-Interest Debt Elimination (>10% APR)",
            shortName = "High-Interest Debt",
            description = "Aggressively pay off Maybank Credit Card (~18% APR) and Shopee SLoan (~18% APR).",
            status = step3Status,
            progressPercent = step3Progress,
            currentAmount = highInterestBalance,
            targetAmount = 0.0,
            malaysianAdaptationNote = "Targets Malaysian credit card debt (18% p.a.) and Shopee SLoan. Credit card repayment is not treated as a new purchase expense.",
            actionTip = "Target: Clear Shopee SLoan (RM1,983.33) first, then accelerate Maybank Credit Card (RM10,093.43) with RM1,200/mo."
        )

        // Step 4: Full Emergency Fund (3 - 6 Months)
        val step4Target = monthlyEssentials * 3 // RM9,900 (~RM10,000)
        val step4Completed = accessibleSavings >= step4Target
        val step4Progress = min(100f, (accessibleSavings / step4Target * 100.0).toFloat())
        val step4Status = when {
            step4Completed -> StepStatus.COMPLETED
            step3Completed -> StepStatus.ACTIVE
            else -> StepStatus.LOCKED
        }
        val step4 = FlowchartStepInfo(
            stepNumber = 4,
            title = "Step 4: Full Emergency Fund (3 Months Living Expenses)",
            shortName = "3-Month Safety Net",
            description = "Build a solid liquid reserve of RM10,000 (3 months of essential family expenses) in high-yield or accessible savings.",
            status = step4Status,
            progressPercent = step4Progress,
            currentAmount = accessibleSavings,
            targetAmount = step4Target,
            malaysianAdaptationNote = "Liquid reserves in high-yield bank savings, ASB, or Tabung Haji (KWSP cannot be counted as liquid emergency cash).",
            actionTip = "Once high-interest debt is cleared, redirect the RM1,483/mo debt budget into liquid emergency savings."
        )

        // Step 5: Moderate-Interest Debt & Short-Term Goals (< 5 years)
        val step5Completed = moderateInterestBalance <= 0.01
        val step5Progress = if (step5Completed) 100f else {
            val paidOff = max(0.0, moderateInterestInitial - moderateInterestBalance)
            max(0f, min(100f, (paidOff / moderateInterestInitial * 100.0).toFloat()))
        }
        val step5Status = when {
            step5Completed -> StepStatus.COMPLETED
            step4Completed -> StepStatus.ACTIVE
            else -> StepStatus.LOCKED
        }
        val step5 = FlowchartStepInfo(
            stepNumber = 5,
            title = "Step 5: Moderate-Interest Debt (Vehicle Hire Purchase)",
            shortName = "Vehicle Hire Purchase",
            description = "Clear Hong Leong Car HP1 (RM7,267) and AmBank Car HP2 (RM13,301) to become 100% debt free.",
            status = step5Status,
            progressPercent = step5Progress,
            currentAmount = moderateInterestBalance,
            targetAmount = 0.0,
            malaysianAdaptationNote = "Addresses Malaysian Hire Purchase (Sewa Beli kereta) commitments: HP1 (RM530/mo) and HP2 contribution (RM250/mo).",
            actionTip = "Continue regular monthly HP installments or settle early once emergency fund is fully funded."
        )

        // Step 6: Long-Term Investing & Financial Freedom (Target Age 41–45)
        val step6Status = when {
            step5Completed -> StepStatus.ACTIVE
            else -> StepStatus.LOCKED
        }
        val step6 = FlowchartStepInfo(
            stepNumber = 6,
            title = "Step 6: Long-Term Investing & Financial Freedom",
            shortName = "Financial Freedom (Age 41-45)",
            description = "Invest surplus income towards financial independence between age 41 and 45 (5–9 years horizon).",
            status = step6Status,
            progressPercent = if (step5Completed) 50f else 10f,
            currentAmount = profile.asb + profile.tabungHaji + profile.otherInvestments,
            targetAmount = 100000.0,
            malaysianAdaptationNote = "UK S&S ISA & SIPP adapted to Malaysian instruments: ASB (Amanah Saham Bumiputera), Tabung Haji, KWSP Voluntary Self-Contribution (up to RM100k/year), and low-cost index funds.",
            actionTip = "Maximise ASB / Tabung Haji and KWSP voluntary top-ups with your freed-up cash flow."
        )

        val steps = listOf(step0, step1, step2, step3, step4, step5, step6)

        // Find current active step
        val currentStep = steps.firstOrNull { it.status == StepStatus.ACTIVE } ?: steps.last()

        // Calculate overall journey progress (each step contributes ~14.28% out of 7 steps)
        val completedStepsCount = steps.count { it.status == StepStatus.COMPLETED }
        val currentStepFraction = (currentStep.progressPercent / 100f)
        val overallProgress = min(100f, ((completedStepsCount + currentStepFraction) / steps.size.toFloat()) * 100f)

        // Determine next specific target
        val (targetTitle, targetAmt, targetRemaining) = when {
            !step1Completed -> Triple("Build RM1,000 Starter Buffer", 1000.0, max(0.0, 1000.0 - accessibleSavings))
            highInterestBalance > 0 -> {
                val sloan = debts.firstOrNull { it.key == "SHOPEE_SLOAN" }
                if (sloan != null && sloan.currentBalance > 0) {
                    Triple("Clear Shopee SLoan (${formatMyr(sloan.currentBalance)})", sloan.initialBalance, sloan.currentBalance)
                } else {
                    val cc = debts.firstOrNull { it.key == "MAYBANK_CC" }
                    val ccBal = cc?.currentBalance ?: highInterestBalance
                    Triple("Clear Maybank Credit Card (${formatMyr(ccBal)})", cc?.initialBalance ?: highInterestInitial, ccBal)
                }
            }
            !step4Completed -> Triple("Save 3-Month Emergency Fund (${formatMyr(step4Target)})", step4Target, max(0.0, step4Target - accessibleSavings))
            moderateInterestBalance > 0 -> Triple("Pay off Car Hire Purchase (${formatMyr(moderateInterestBalance)})", moderateInterestInitial, moderateInterestBalance)
            else -> Triple("Invest for Freedom (Age 41-45)", 100000.0, max(0.0, 100000.0 - (profile.asb + profile.tabungHaji)))
        }

        return FlowchartJourneyState(
            currentStepNumber = currentStep.stepNumber,
            currentStepTitle = currentStep.title,
            currentStepProgressPercent = currentStep.progressPercent,
            overallJourneyProgressPercent = overallProgress,
            nextTargetTitle = targetTitle,
            nextTargetAmount = targetAmt,
            nextTargetRemaining = targetRemaining,
            steps = steps,
            totalDebt = totalDebt,
            highInterestDebt = highInterestBalance,
            moderateInterestDebt = moderateInterestBalance,
            accessibleSavings = accessibleSavings,
            totalKwsp = totalKwsp,
            estimatedMonthlyLivingExpenses = monthlyEssentials
        )
    }

    private fun formatMyr(amount: Double): String {
        return "RM ${String.format("%,.2f", amount)}"
    }
}
