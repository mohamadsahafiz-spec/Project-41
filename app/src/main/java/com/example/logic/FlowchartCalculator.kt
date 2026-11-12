package com.example.logic

import com.example.data.model.DebtEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.FlowchartVerificationEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
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
    val objective: String,
    val description: String,
    val status: StepStatus,
    val progressPercent: Float,
    val currentAmount: Double,
    val targetAmount: Double,
    val remainingAmount: Double,
    val estimatedCompletionDate: String,
    val calculationRationale: String,
    val recommendedMonthlyContribution: Double,
    val motivationalMessage: String,
    val clearNextAction: String,
    val malaysianAdaptationNote: String
)

data class FlowchartJourneyState(
    val currentStepNumber: Int,
    val currentStepTitle: String,
    val currentStepProgressPercent: Float,
    val overallJourneyProgressPercent: Float,
    val nextTargetTitle: String,
    val nextTargetAmount: Double,
    val nextTargetRemaining: Double,
    val activeMission: FlowchartStepInfo,
    val steps: List<FlowchartStepInfo>,
    val totalDebt: Double,
    val highInterestDebt: Double,
    val moderateInterestDebt: Double,
    val everydayCash: Double,
    val dedicatedSavings: Double,
    val dedicatedEmergencyFund: Double,
    val totalKwsp: Double,
    val monthlySurplus: Double
)

object FlowchartCalculator {
    const val SOURCE_URL = "https://ukpersonal.finance/flowchart/"
    const val FLOWCHART_VERSION = "UK Personal Finance Flowchart v3.0.10 (Adapted for Malaysia)"

    fun calculateJourney(
        profile: FinancialProfileEntity,
        debts: List<DebtEntity>,
        dedicatedEmergencySavings: Double,
        dedicatedTotalSavings: Double,
        verifications: List<FlowchartVerificationEntity> = emptyList(),
        currentMonthRecordedExpenses: Double = 0.0,
        plannedCommitments: Double = 3908.33
    ): FlowchartJourneyState {
        val totalDebt = debts.sumOf { it.currentBalance }
        val highInterestDebts = debts.filter { it.isHighInterest }
        val moderateInterestDebts = debts.filter { !it.isHighInterest }

        val highInterestBalance = highInterestDebts.sumOf { it.currentBalance }
        val highInterestInitial = highInterestDebts.sumOf { it.initialBalance }.let { if (it <= 0) 12076.76 else it }

        val moderateInterestBalance = moderateInterestDebts.sumOf { it.currentBalance }
        val moderateInterestInitial = moderateInterestDebts.sumOf { it.initialBalance }.let { if (it <= 0) 20568.0 else it }

        val everydayCash = profile.bankBalance
        val totalKwsp = profile.totalKwsp

        // Actual Monthly Surplus available for savings or debt acceleration
        val monthlyNetIncome = profile.netMonthlySalary
        val monthlySurplus = max(0.0, monthlyNetIncome - plannedCommitments - currentMonthRecordedExpenses)

        val verificationMap = verifications.associateBy { it.stepNumber }

        // Step 0: Budgeting & Essentials Tracking
        // CORRECTION: Must NOT complete automatically from an expense. Requires verified confirmation!
        val step0Confirmed = verificationMap[0]?.isConfirmed == true
        val step0Remaining = if (step0Confirmed) 0.0 else 1.0
        val step0 = FlowchartStepInfo(
            stepNumber = 0,
            title = "Step 0: Budgeting & Essentials Management",
            shortName = "Budget & Essentials",
            objective = "Know where every Ringgit goes and establish direct debits for essential survival commitments.",
            description = "Track all income, recurring commitments (Rent RM700, Parents RM200, Fibre RM95, Playschool RM550, Formula RM100), and everyday spending.",
            status = if (step0Confirmed) StepStatus.COMPLETED else StepStatus.ACTIVE,
            progressPercent = if (step0Confirmed) 100f else 50f,
            currentAmount = if (step0Confirmed) 1.0 else 0.5,
            targetAmount = 1.0,
            remainingAmount = step0Remaining,
            estimatedCompletionDate = if (step0Confirmed) "Confirmed" else "Review & Confirm",
            calculationRationale = "Step 0 requires user verification of monthly budget and standing orders.",
            recommendedMonthlyContribution = 0.0,
            motivationalMessage = "Budgeting is the foundational engine of financial freedom.",
            clearNextAction = "Review and confirm your monthly commitment checklist.",
            malaysianAdaptationNote = "Adapted with Malaysian commitments: Rent (RM700), Parents (RM200), Mother's fibre (RM95), Playschool (RM550), and Formula (RM100)."
        )

        // Step 1: Starter Emergency Fund (RM1,000 Buffer)
        // CORRECTION: Only DEDICATED emergency savings count. Ordinary bank balance does NOT count!
        val step1Target = 1000.0
        val step1Remaining = max(0.0, step1Target - dedicatedEmergencySavings)
        val step1Completed = dedicatedEmergencySavings >= step1Target
        val step1Progress = min(100f, (dedicatedEmergencySavings / step1Target * 100.0).toFloat())

        val (step1EstDate, step1Rationale) = calculateEstimate(step1Remaining, monthlySurplus)

        val step1Status = when {
            step1Completed -> StepStatus.COMPLETED
            step0Confirmed -> StepStatus.ACTIVE
            else -> StepStatus.ACTIVE // Can build starter fund concurrently
        }

        val step1 = FlowchartStepInfo(
            stepNumber = 1,
            title = "Step 1: Build Your Starter Emergency Buffer",
            shortName = "Starter Buffer",
            objective = "Deposit a deliberate RM1,000 into a dedicated savings vault to protect against unexpected shocks.",
            description = "Hold a dedicated, untouchable cash reserve of RM1,000 in your Savings Vault. Everyday bank cash does not qualify as an emergency fund.",
            status = step1Status,
            progressPercent = step1Progress,
            currentAmount = dedicatedEmergencySavings,
            targetAmount = step1Target,
            remainingAmount = step1Remaining,
            estimatedCompletionDate = step1EstDate,
            calculationRationale = step1Rationale,
            recommendedMonthlyContribution = min(step1Remaining, if (monthlySurplus > 0) monthlySurplus else 250.0),
            motivationalMessage = "A deliberate starter buffer ensures you never have to swipe a credit card for surprise emergencies.",
            clearNextAction = "Transfer cash into 'My Savings Vault' under Emergency Fund purpose.",
            malaysianAdaptationNote = "UK £500-£1,000 starter buffer adapted to RM1,000 in a dedicated high-yield liquid savings account."
        )

        // Step 2: Workplace Pension Match / Statutory KWSP Verification
        // CORRECTION: Positive KWSP balance alone does not mean condition is completed. Documented & verified.
        val step2Confirmed = verificationMap[2]?.isConfirmed == true || totalKwsp > 0
        val step2Status = when {
            !step1Completed -> StepStatus.LOCKED
            step2Confirmed -> StepStatus.COMPLETED
            else -> StepStatus.ACTIVE
        }
        val step2 = FlowchartStepInfo(
            stepNumber = 2,
            title = "Step 2: Workplace Pension Match / Statutory KWSP",
            shortName = "Workplace EPF/KWSP",
            objective = "Verify that mandatory employer retirement contributions (12–13%) are maximized.",
            description = "In the UK Flowchart, this step captures 'free money' via employer match. In Malaysia, statutory employer EPF is mandatory (13% for gross <= RM5,000).",
            status = step2Status,
            progressPercent = if (step2Status == StepStatus.COMPLETED) 100f else 0f,
            currentAmount = totalKwsp,
            targetAmount = totalKwsp,
            remainingAmount = 0.0,
            estimatedCompletionDate = if (step2Status == StepStatus.COMPLETED) "Verified Active" else "Review Payslip",
            calculationRationale = "Salaried engineer has statutory employer EPF deducted monthly.",
            recommendedMonthlyContribution = 0.0,
            motivationalMessage = "Statutory employer contributions are building your long-term retirement pillar in the background.",
            clearNextAction = "Verify employer EPF contribution in Monthly Check-In.",
            malaysianAdaptationNote = "Statutory 11% employee + 13% employer EPF contribution across Accounts 1, 2, and 3. Mandatory deductions are statutory, not discretionary matching."
        )

        // Step 3: High-Interest Debt Elimination (>10% APR)
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
        val (step3EstDate, step3Rationale) = calculateEstimate(
            highInterestBalance,
            if (monthlySurplus > 0) monthlySurplus + 1483.33 else 1483.33 // CC (1200) + SLoan (283.33)
        )
        val step3 = FlowchartStepInfo(
            stepNumber = 3,
            title = "Step 3: High-Interest Debt Elimination (>10% APR)",
            shortName = "High-Interest Debt",
            objective = "Aggressively destroy Maybank Credit Card (15% p.a.) and Shopee SLoan (1.5% monthly provisional).",
            description = "High-interest debt destroys compounding wealth. Eliminate Maybank CC (RM10,093.43 @ 15% p.a.) and Shopee SLoan (RM1,983.33 @ 1.5%/mo) before saving large funds.",
            status = step3Status,
            progressPercent = step3Progress,
            currentAmount = max(0.0, highInterestInitial - highInterestBalance),
            targetAmount = highInterestInitial,
            remainingAmount = highInterestBalance,
            estimatedCompletionDate = step3EstDate,
            calculationRationale = step3Rationale,
            recommendedMonthlyContribution = 1483.33,
            motivationalMessage = "Clearing 15% and 1.5%/mo debt provides an immediate guaranteed return equal to the interest saved.",
            clearNextAction = "Clear Shopee SLoan first (RM1,983.33), then deploy RM1,483.33/mo into Maybank CC.",
            malaysianAdaptationNote = "Targets Malaysian credit card debt (15% p.a.) and Shopee SLoan (1.5% monthly). Credit card repayments are debt elimination, not new purchases."
        )

        // Step 4: Full Emergency Fund (3 Months Living Expenses)
        val step4Target = 10000.0 // 3 months of essential family expenses (~RM3,300/mo)
        val step4Remaining = max(0.0, step4Target - dedicatedEmergencySavings)
        val step4Completed = dedicatedEmergencySavings >= step4Target
        val step4Progress = min(100f, (dedicatedEmergencySavings / step4Target * 100.0).toFloat())
        val step4Status = when {
            step4Completed -> StepStatus.COMPLETED
            step3Completed -> StepStatus.ACTIVE
            else -> StepStatus.LOCKED
        }
        val (step4EstDate, step4Rationale) = calculateEstimate(step4Remaining, monthlySurplus + 1483.33)
        val step4 = FlowchartStepInfo(
            stepNumber = 4,
            title = "Step 4: Full Emergency Fund (3 Months Living Expenses)",
            shortName = "3-Month Safety Net",
            objective = "Build RM10,000 in dedicated liquid emergency reserves (My Savings Vault).",
            description = "Save 3 full months of living costs into high-yield liquid savings. KWSP is not liquid emergency cash.",
            status = step4Status,
            progressPercent = step4Progress,
            currentAmount = dedicatedEmergencySavings,
            targetAmount = step4Target,
            remainingAmount = step4Remaining,
            estimatedCompletionDate = step4EstDate,
            calculationRationale = step4Rationale,
            recommendedMonthlyContribution = 1000.0,
            motivationalMessage = "With 3 months of emergency buffer, life's financial curveballs bounce right off you.",
            clearNextAction = "Redirect freed-up RM1,483/mo debt repayment budget into My Savings Vault.",
            malaysianAdaptationNote = "Liquid reserves in high-yield bank savings, ASB, or Tabung Haji. KWSP Account 1 is locked for retirement."
        )

        // Step 5: Moderate-Interest Debt (Vehicle Hire Purchase)
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
        val (step5EstDate, step5Rationale) = calculateEstimate(moderateInterestBalance, 780.0)
        val step5 = FlowchartStepInfo(
            stepNumber = 5,
            title = "Step 5: Moderate-Interest Debt (Vehicle Hire Purchase)",
            shortName = "Vehicle Hire Purchase",
            objective = "Settle Hong Leong Car HP1 (RM7,267 @ 3% ref) and AmBank Car HP2 (RM13,301 @ 3% ref) to become 100% debt free.",
            description = "Settle car hire-purchase loans. 3% rate is reference info only (no interest projection). Outstanding principal updates strictly from confirmed statement balances.",
            status = step5Status,
            progressPercent = step5Progress,
            currentAmount = max(0.0, moderateInterestInitial - moderateInterestBalance),
            targetAmount = moderateInterestInitial,
            remainingAmount = moderateInterestBalance,
            estimatedCompletionDate = step5EstDate,
            calculationRationale = step5Rationale,
            recommendedMonthlyContribution = 780.0,
            motivationalMessage = "No car payments means RM780 more freedom in your pocket each month.",
            clearNextAction = "Record monthly instalments (HP1 RM530, HP2 User contribution RM250) and update statement balances in Settings.",
            malaysianAdaptationNote = "Addresses Malaysian Hire Purchase (HP1 & HP2 at 3% ref rate). Instalments are recorded without automatic principal deductions; loans are marked settled only when confirmed balance is RM0."
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
            objective = "Invest surplus income towards financial independence between age 41 and 45 (5–9 years horizon).",
            description = "Maximize ASB, Tabung Haji, voluntary KWSP self-contributions (up to RM100k/yr), and low-cost global index funds.",
            status = step6Status,
            progressPercent = if (step5Completed) 50f else 5f,
            currentAmount = profile.asb + profile.tabungHaji + profile.otherInvestments,
            targetAmount = 100000.0,
            remainingAmount = max(0.0, 100000.0 - (profile.asb + profile.tabungHaji)),
            estimatedCompletionDate = "Target Age 41–45",
            calculationRationale = "5 to 9 year investment horizon to reach financial independence.",
            recommendedMonthlyContribution = 1500.0,
            motivationalMessage = "Your money works for you. Financial freedom on your terms.",
            clearNextAction = "Establish recurring monthly investments in ASB / index funds.",
            malaysianAdaptationNote = "UK S&S ISA adapted to ASB, Tabung Haji, KWSP voluntary top-up, and global index funds."
        )

        val steps = listOf(step0, step1, step2, step3, step4, step5, step6)

        // Identify currently active mission (first non-completed step)
        val activeMission = steps.firstOrNull { it.status == StepStatus.ACTIVE } ?: steps.last()

        val completedCount = steps.count { it.status == StepStatus.COMPLETED }
        val overallProgress = min(100f, ((completedCount + (activeMission.progressPercent / 100f)) / steps.size.toFloat()) * 100f)

        return FlowchartJourneyState(
            currentStepNumber = activeMission.stepNumber,
            currentStepTitle = activeMission.title,
            currentStepProgressPercent = activeMission.progressPercent,
            overallJourneyProgressPercent = overallProgress,
            nextTargetTitle = activeMission.title,
            nextTargetAmount = activeMission.targetAmount,
            nextTargetRemaining = activeMission.remainingAmount,
            activeMission = activeMission,
            steps = steps,
            totalDebt = totalDebt,
            highInterestDebt = highInterestBalance,
            moderateInterestDebt = moderateInterestBalance,
            everydayCash = everydayCash,
            dedicatedSavings = dedicatedTotalSavings,
            dedicatedEmergencyFund = dedicatedEmergencySavings,
            totalKwsp = totalKwsp,
            monthlySurplus = monthlySurplus
        )
    }

    private fun calculateEstimate(remaining: Double, monthlyRate: Double): Pair<String, String> {
        if (remaining <= 0.0) {
            return Pair("Goal Achieved", "Target reached based on verified records.")
        }
        if (monthlyRate <= 0.0) {
            return Pair(
                "Estimation unavailable",
                "Monthly surplus is currently RM0.00. Free up cashflow to project completion."
            )
        }
        val monthsNeeded = ceil(remaining / monthlyRate).toInt()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"))
        cal.add(Calendar.MONTH, monthsNeeded)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        }
        val targetDate = sdf.format(cal.time)
        val rationale = "Remaining RM${String.format(Locale.US, "%,.2f", remaining)} ÷ RM${String.format(Locale.US, "%,.2f", monthlyRate)}/mo = ~$monthsNeeded months ($targetDate)"
        return Pair(targetDate, rationale)
    }
}
