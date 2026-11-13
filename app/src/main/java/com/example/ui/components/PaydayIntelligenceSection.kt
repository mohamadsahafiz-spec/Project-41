package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MonthlyCommitmentItem
import com.example.data.model.DebtEntity
import com.example.data.model.MonthlyIncomeEntity
import com.example.data.model.PaydayAllocationEntity
import com.example.logic.DebtRateClassification
import com.example.logic.PaydayAllocationPlan
import com.example.logic.PaydayInput
import com.example.logic.PaydayIntelligenceEngine
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningGold
import com.example.util.DateUtils
import java.util.Locale
import kotlin.math.abs

class PaydayPlanFormState(
    initialUpcomingEssentials: String,
    initialEmergencyTarget: String,
    initialCashBuffer: String,
    initialExtraDebt: String,
    initialEmergencyOverride: String,
    initialDiscretionaryOverride: String
) {
    var upcomingEssentialsText by mutableStateOf(initialUpcomingEssentials)
    var emergencyTargetText by mutableStateOf(initialEmergencyTarget)
    var cashBufferText by mutableStateOf(initialCashBuffer)
    var extraDebtOverrideText by mutableStateOf(initialExtraDebt)
    var emergencyOverrideText by mutableStateOf(initialEmergencyOverride)
    var discretionaryOverrideText by mutableStateOf(initialDiscretionaryOverride)
}

@Composable
fun rememberPaydayPlanState(
    monthKey: String,
    income: MonthlyIncomeEntity?,
    defaultNetSalary: Double,
    existingEverydayCash: Double,
    commitments: List<MonthlyCommitmentItem>,
    debts: List<DebtEntity>,
    recordedExpensesThisMonth: Double,
    existingEmergencyFund: Double,
    savedAllocation: PaydayAllocationEntity?
): Pair<PaydayPlanFormState, PaydayAllocationPlan> {
    val formState = remember(savedAllocation, monthKey) {
        PaydayPlanFormState(
            initialUpcomingEssentials = savedAllocation?.upcomingEssentialExpensesInput?.toString() ?: "",
            initialEmergencyTarget = savedAllocation?.emergencyFundTargetInput?.toString()
                ?: if (existingEmergencyFund < 1000.0) "1000.0" else "10000.0",
            initialCashBuffer = savedAllocation?.cashBufferTargetInput?.toString() ?: "",
            initialExtraDebt = "",
            initialEmergencyOverride = "",
            initialDiscretionaryOverride = ""
        )
    }

    val upcomingEssentialsVal = formState.upcomingEssentialsText.toDoubleOrNull()?.takeIf { it >= 0.0 }
    val emergencyTargetVal = formState.emergencyTargetText.toDoubleOrNull()?.takeIf { it >= 0.0 }
    val cashBufferVal = formState.cashBufferText.toDoubleOrNull()?.takeIf { it >= 0.0 }

    val paydayInput = remember(
        monthKey,
        income,
        defaultNetSalary,
        existingEverydayCash,
        commitments,
        debts,
        recordedExpensesThisMonth,
        upcomingEssentialsVal,
        existingEmergencyFund,
        emergencyTargetVal,
        cashBufferVal
    ) {
        PaydayInput.fromMonthlyState(
            monthKey = monthKey,
            income = income,
            defaultNetSalary = defaultNetSalary,
            existingEverydayCash = existingEverydayCash,
            commitments = commitments,
            debts = debts,
            recordedExpensesThisMonth = recordedExpensesThisMonth,
            upcomingEssentialExpenses = upcomingEssentialsVal,
            existingEmergencyFund = existingEmergencyFund,
            emergencyFundTarget = emergencyTargetVal,
            cashBufferTarget = cashBufferVal
        )
    }

    val plan = remember(paydayInput) {
        PaydayIntelligenceEngine.calculatePlan(paydayInput)
    }

    // Initialize override fields once per savedAllocation/plan change if empty
    remember(savedAllocation, plan.recommendedExtraDebtRepayment, plan.recommendedEmergencySavings, plan.recommendedDiscretionaryAllowance) {
        val initExtra = savedAllocation?.approvedExtraDebtRepayment ?: plan.recommendedExtraDebtRepayment
        val initEf = savedAllocation?.approvedEmergencySavings ?: plan.recommendedEmergencySavings
        val initDisc = savedAllocation?.approvedDiscretionaryAllowance ?: plan.recommendedDiscretionaryAllowance
        if (formState.extraDebtOverrideText.isEmpty()) {
            formState.extraDebtOverrideText = String.format(Locale.US, "%.2f", initExtra)
        }
        if (formState.emergencyOverrideText.isEmpty()) {
            formState.emergencyOverrideText = String.format(Locale.US, "%.2f", initEf)
        }
        if (formState.discretionaryOverrideText.isEmpty()) {
            formState.discretionaryOverrideText = String.format(Locale.US, "%.2f", initDisc)
        }
        true
    }

    return formState to plan
}

fun LazyListScope.paydayIntelligenceStageItems(
    monthKey: String,
    formState: PaydayPlanFormState,
    plan: PaydayAllocationPlan,
    savedAllocation: PaydayAllocationEntity?,
    onSaveAndApprovePlan: (PaydayAllocationEntity) -> Unit,
    onBackToIncome: () -> Unit
) {
    // Item 1: Header, Planning Assumptions, Available Cash, Warnings & Data Gaps
    item(key = "payday_header_assumptions") {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payday_intelligence_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "2",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "STAGE 2 — PAYDAY INTELLIGENCE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Explainable payday allocation plan before paying commitments. Never auto-executes transfers.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                if (savedAllocation?.isApproved == true) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SuccessGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (savedAllocation.hasOverrides) {
                                    "Saved Approved Plan (Custom Overrides Applied)"
                                } else {
                                    "Saved Approved Plan (Following Engine Recommendation)"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "PAYDAY PLANNING ASSUMPTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.upcomingEssentialsText,
                    onValueChange = { formState.upcomingEssentialsText = it },
                    label = { Text("Upcoming Essential Living Expenses") },
                    placeholder = { Text("e.g. groceries, petrol, utilities") },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payday_upcoming_essentials_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = formState.emergencyTargetText,
                        onValueChange = { formState.emergencyTargetText = it },
                        label = { Text("Emergency Target") },
                        placeholder = { Text("Unset") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payday_emergency_target_input")
                    )

                    OutlinedTextField(
                        value = formState.cashBufferText,
                        onValueChange = { formState.cashBufferText = it },
                        label = { Text("Cash Buffer Target") },
                        placeholder = { Text("Auto 10% (<=200)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payday_cash_buffer_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { formState.emergencyTargetText = "1000.0" },
                        label = { Text("Starter RM 1k", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { formState.emergencyTargetText = "10000.0" },
                        label = { Text("3-Mo RM 10k", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { formState.emergencyTargetText = "" },
                        label = { Text("Clear Target", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Actual Available Cash",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = DateUtils.formatCurrency(plan.actualAvailableCash),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (plan.hasInsufficientCash) DangerRed else SuccessGreen
                                ),
                                modifier = Modifier.testTag("payday_available_cash_text")
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Confirmed Income: ${DateUtils.formatCurrency(plan.totalConfirmedIncome)} • Already Paid/Spent: ${DateUtils.formatCurrency(plan.alreadyPaidCommitments + plan.recordedExpensesThisMonth)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        if (plan.reimbursementsExcluded > 0.0) {
                            Text(
                                text = "Reimbursements Excluded: ${DateUtils.formatCurrency(plan.reimbursementsExcluded)} (not counted as income)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = InfoBlue, fontWeight = FontWeight.Medium)
                            )
                        }
                        if (plan.unconfirmedIncomeExcluded > 0.0) {
                            Text(
                                text = "Unconfirmed Income Excluded: ${DateUtils.formatCurrency(plan.unconfirmedIncomeExcluded)} (pending receipt)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = WarningGold, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                if (plan.hasInsufficientCash) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DangerRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Insufficient Cash After Obligations (Shortfall: ${DateUtils.formatCurrency(plan.shortfallAmount)})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = DangerRed)
                                )
                                Text(
                                    text = "Pause extra debt repayments, savings top-ups, and discretionary spending until mandatory bills and minimum debt payments are covered.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }

                plan.additionalIncomeWarning?.let { warning ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = InfoBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }

                if (plan.dataGaps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payday_data_gaps_box"),
                        color = WarningGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = WarningGold, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Data Completeness & Assumptions",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = WarningGold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            plan.dataGaps.forEach { gap ->
                                Text(
                                    text = "• $gap",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Item 2: Explainable Recommendations Breakdown
    item(key = "payday_recommendations_breakdown") {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payday_recommendations_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "RECOMMENDED PAYDAY ALLOCATIONS (WITH WHY)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                plan.items.filter { it.id != "available_cash" }.forEach { item ->
                    PaydayRecommendationItemCard(
                        title = item.title,
                        priorityRank = item.priorityRank,
                        recommendedAmount = item.recommendedAmount,
                        whyExplanation = item.whyExplanation
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Item 3: Debt Ranking & Consequence of Spending Surplus
    item(key = "payday_debt_and_consequence") {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payday_consequence_box"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (plan.rankedDebts.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "DEBT PRIORITY RANKING (EFFECTIVE RATE ORDER)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            plan.rankedDebts.forEachIndexed { idx, debt ->
                                val badgeColor = when (debt.classification) {
                                    DebtRateClassification.EFFECTIVE_APR,
                                    DebtRateClassification.PROVISIONAL_RATE -> if (idx == 0 && debt.isEligibleForExtraRepayment) DangerRed else WarningGold
                                    DebtRateClassification.REFERENCE_ONLY,
                                    DebtRateClassification.MISSING_RATE -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(
                                    text = "${idx + 1}. ${debt.debtName} (${DateUtils.formatCurrency(debt.currentBalance)})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = badgeColor)
                                )
                                Text(
                                    text = debt.note,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                if (idx < plan.rankedDebts.lastIndex) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = WarningGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "CONSEQUENCE OF SPENDING SURPLUS INSTEAD OF ALLOCATING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = WarningGold,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plan.spendingConsequenceSummary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            }
        }
    }

    // Item 4: User Override Controls & Approval Actions
    item(key = "payday_override_approval") {
        val approvedExtraDebt = maxOf(0.0, formState.extraDebtOverrideText.toDoubleOrNull() ?: plan.recommendedExtraDebtRepayment)
        val approvedEmergency = maxOf(0.0, formState.emergencyOverrideText.toDoubleOrNull() ?: plan.recommendedEmergencySavings)
        val approvedDiscretionary = maxOf(0.0, formState.discretionaryOverrideText.toDoubleOrNull() ?: plan.recommendedDiscretionaryAllowance)

        val totalReservedObligations = plan.essentialCommitmentsReserved +
            plan.minimumDebtPaymentsReserved +
            plan.upcomingEssentialExpensesReserved
        val approvedRemainingBuffer = plan.actualAvailableCash -
            totalReservedObligations -
            approvedExtraDebt -
            approvedEmergency -
            approvedDiscretionary

        val hasOverrides = abs(approvedExtraDebt - plan.recommendedExtraDebtRepayment) > 0.01 ||
            abs(approvedEmergency - plan.recommendedEmergencySavings) > 0.01 ||
            abs(approvedDiscretionary - plan.recommendedDiscretionaryAllowance) > 0.01

        val upcomingEssentialsVal = formState.upcomingEssentialsText.toDoubleOrNull()?.takeIf { it >= 0.0 }
        val emergencyTargetVal = formState.emergencyTargetText.toDoubleOrNull()?.takeIf { it >= 0.0 }
        val cashBufferVal = formState.cashBufferText.toDoubleOrNull()?.takeIf { it >= 0.0 }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payday_override_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REVIEW & ADJUST YOUR APPROVED PLAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    )
                    if (hasOverrides) {
                        TextButton(
                            onClick = {
                                formState.extraDebtOverrideText = String.format(Locale.US, "%.2f", plan.recommendedExtraDebtRepayment)
                                formState.emergencyOverrideText = String.format(Locale.US, "%.2f", plan.recommendedEmergencySavings)
                                formState.discretionaryOverrideText = String.format(Locale.US, "%.2f", plan.recommendedDiscretionaryAllowance)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset to Recommended", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = formState.extraDebtOverrideText,
                    onValueChange = { formState.extraDebtOverrideText = it },
                    label = {
                        Text(
                            "Approved Extra Debt (${plan.recommendedExtraDebtTargetName ?: "None"}) • Rec: ${DateUtils.formatCurrency(plan.recommendedExtraDebtRepayment)}"
                        )
                    },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payday_override_extra_debt")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.emergencyOverrideText,
                    onValueChange = { formState.emergencyOverrideText = it },
                    label = {
                        Text("Approved Emergency Savings • Rec: ${DateUtils.formatCurrency(plan.recommendedEmergencySavings)}")
                    },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payday_override_emergency")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.discretionaryOverrideText,
                    onValueChange = { formState.discretionaryOverrideText = it },
                    label = {
                        Text("Approved Discretionary Allowance • Rec: ${DateUtils.formatCurrency(plan.recommendedDiscretionaryAllowance)}")
                    },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payday_override_discretionary")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (approvedRemainingBuffer < -0.01) DangerRed.copy(alpha = 0.14f) else SuccessGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (approvedRemainingBuffer < -0.01) "Over-Allocated Buffer Deficit:" else "Approved Remaining Cash Buffer:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (hasOverrides) {
                                    "Recommended Buffer: ${DateUtils.formatCurrency(plan.recommendedRemainingBuffer)} (Custom overrides active)"
                                } else {
                                    "Matches engine recommendation"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Text(
                            text = DateUtils.formatCurrency(approvedRemainingBuffer),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (approvedRemainingBuffer < -0.01) DangerRed else SuccessGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToIncome,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stage 1")
                    }

                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val entity = PaydayAllocationEntity(
                                id = savedAllocation?.id ?: 0L,
                                monthKey = monthKey,
                                confirmedAvailableCash = plan.actualAvailableCash,
                                reimbursementsExcluded = plan.reimbursementsExcluded,
                                unconfirmedIncomeExcluded = plan.unconfirmedIncomeExcluded,
                                upcomingEssentialExpensesInput = upcomingEssentialsVal,
                                emergencyFundTargetInput = emergencyTargetVal,
                                cashBufferTargetInput = cashBufferVal,
                                recEssentialCommitments = plan.essentialCommitmentsReserved,
                                recMinDebtPayments = plan.minimumDebtPaymentsReserved,
                                recUpcomingEssentials = plan.upcomingEssentialExpensesReserved,
                                recExtraDebtRepayment = plan.recommendedExtraDebtRepayment,
                                recExtraDebtTargetKey = plan.recommendedExtraDebtTargetKey,
                                recEmergencySavings = plan.recommendedEmergencySavings,
                                recDiscretionaryAllowance = plan.recommendedDiscretionaryAllowance,
                                recRemainingBuffer = plan.recommendedRemainingBuffer,
                                approvedExtraDebtRepayment = approvedExtraDebt,
                                approvedEmergencySavings = approvedEmergency,
                                approvedDiscretionaryAllowance = approvedDiscretionary,
                                approvedRemainingBuffer = approvedRemainingBuffer,
                                hasOverrides = hasOverrides,
                                isApproved = true,
                                approvedTimestamp = now,
                                rationaleSummary = plan.spendingConsequenceSummary,
                                updatedTimestamp = now
                            )
                            onSaveAndApprovePlan(entity)
                        },
                        enabled = approvedRemainingBuffer >= -0.01,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                            .testTag("approve_payday_plan_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (hasOverrides) "Approve Custom Plan" else "Approve & Continue",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PaydayRecommendationItemCard(
    title: String,
    priorityRank: Int,
    recommendedAmount: Double,
    whyExplanation: String
) {
    val priorityLabel = when (priorityRank) {
        1 -> "P1 OBLIGATION"
        2 -> "P2 PROTECTION"
        3 -> "P3 DEBT OPT"
        4 -> "P4 SPENDING"
        else -> "SUMMARY"
    }
    val priorityColor = when (priorityRank) {
        1 -> InfoBlue
        2 -> SuccessGreen
        3 -> WarningGold
        4 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = priorityColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = priorityLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = DateUtils.formatCurrency(recommendedAmount),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "WHY: $whyExplanation",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            )
        }
    }
}
