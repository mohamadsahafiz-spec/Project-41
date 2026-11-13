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
import androidx.compose.material3.FilterChip
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
import com.example.logic.CashSourceMode
import com.example.logic.CashSurplusStatus
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

class PaydayPlanFormState(
    initialCashSourceMode: CashSourceMode,
    initialOpeningCash: String,
    initialUpcomingEssentials: String,
    initialEmergencyTarget: String,
    initialCashBuffer: String,
    initialExtraDebt: String,
    initialEmergencyOverride: String,
    initialDiscretionaryOverride: String
) {
    var cashSourceMode by mutableStateOf(initialCashSourceMode)
    var openingCashText by mutableStateOf(initialOpeningCash)
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
    val initialMode = remember(savedAllocation) {
        runCatching {
            savedAllocation?.cashSourceMode?.let { CashSourceMode.valueOf(it) }
        }.getOrNull() ?: CashSourceMode.OPENING_PLUS_CASHFLOW
    }

    val formState = remember(savedAllocation, monthKey) {
        PaydayPlanFormState(
            initialCashSourceMode = initialMode,
            initialOpeningCash = savedAllocation?.openingCashInput?.let {
                String.format(Locale.US, "%.2f", it)
            } ?: String.format(Locale.US, "%.2f", existingEverydayCash),
            initialUpcomingEssentials = savedAllocation?.upcomingEssentialExpensesInput?.toString() ?: "",
            initialEmergencyTarget = savedAllocation?.emergencyFundTargetInput?.toString()
                ?: if (existingEmergencyFund < 1000.0) "1000.0" else "10000.0",
            initialCashBuffer = savedAllocation?.cashBufferTargetInput?.toString() ?: "",
            initialExtraDebt = "",
            initialEmergencyOverride = "",
            initialDiscretionaryOverride = ""
        )
    }

    val openingCashVal = maxOf(0.0, formState.openingCashText.toDoubleOrNull() ?: 0.0)
    val upcomingEssentialsVal = formState.upcomingEssentialsText.toDoubleOrNull()?.takeIf { it >= 0.0 }
    val emergencyTargetVal = formState.emergencyTargetText.toDoubleOrNull()?.takeIf { it >= 0.0 }
    val cashBufferVal = formState.cashBufferText.toDoubleOrNull()?.takeIf { it >= 0.0 }

    val paydayInput = remember(
        monthKey,
        income,
        defaultNetSalary,
        existingEverydayCash,
        openingCashVal,
        formState.cashSourceMode,
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
            openingBankBalance = openingCashVal,
            cashSourceMode = formState.cashSourceMode,
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

    var overrideDefaultsInitializedFor by remember(savedAllocation, monthKey) { mutableStateOf<Triple<Double, Double, Double>?>(null) }
    val targetDefaults = Triple(
        savedAllocation?.approvedExtraDebtRepayment ?: plan.recommendedExtraDebtRepayment,
        savedAllocation?.approvedEmergencySavings ?: plan.recommendedEmergencySavings,
        savedAllocation?.approvedDiscretionaryAllowance ?: plan.recommendedDiscretionaryAllowance
    )
    if (overrideDefaultsInitializedFor != targetDefaults) {
        if (overrideDefaultsInitializedFor == null) {
            formState.extraDebtOverrideText = String.format(Locale.US, "%.2f", targetDefaults.first)
            formState.emergencyOverrideText = String.format(Locale.US, "%.2f", targetDefaults.second)
            formState.discretionaryOverrideText = String.format(Locale.US, "%.2f", targetDefaults.third)
        }
        overrideDefaultsInitializedFor = targetDefaults
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
    // Item 1: Header, Cash Reconciliation, Planning Assumptions, Available Cash, Warnings & Data Gaps
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

                // Cash Reconciliation Mode (Single Source of Truth — Zero Double-Counting)
                Text(
                    text = "EVERYDAY CASH SOURCE OF TRUTH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = formState.cashSourceMode == CashSourceMode.OPENING_PLUS_CASHFLOW,
                        onClick = { formState.cashSourceMode = CashSourceMode.OPENING_PLUS_CASHFLOW },
                        label = { Text("Opening + Cashflow", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cash_mode_opening_plus_cashflow")
                    )
                    FilterChip(
                        selected = formState.cashSourceMode == CashSourceMode.CURRENT_BANK_BALANCE,
                        onClick = { formState.cashSourceMode = CashSourceMode.CURRENT_BANK_BALANCE },
                        label = { Text("Current Bank Bal", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cash_mode_current_bank")
                    )
                }

                if (formState.cashSourceMode == CashSourceMode.OPENING_PLUS_CASHFLOW) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = formState.openingCashText,
                        onValueChange = { formState.openingCashText = it },
                        label = { Text("Pre-Payday Opening Bank Cash") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payday_opening_cash_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        label = { Text("Min Cash Buffer") },
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

                // Cash Reconciliation Summary Box (Account Cash vs Monthly Cashflow)
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
                                    color = when (plan.surplusStatus) {
                                        CashSurplusStatus.SHORTFALL -> DangerRed
                                        CashSurplusStatus.ZERO_SURPLUS -> WarningGold
                                        CashSurplusStatus.POSITIVE_SURPLUS -> SuccessGreen
                                    }
                                ),
                                modifier = Modifier.testTag("payday_available_cash_text")
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plan.cashReconciliationExplanation,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current Account Cash (Bank): ${DateUtils.formatCurrency(plan.currentBankBalance)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = "Monthly Cashflow: ${DateUtils.formatCurrency(plan.calculatedMonthlyCashflow)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        if (plan.reimbursementsExcluded > 0.0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Reimbursements Excluded: ${DateUtils.formatCurrency(plan.reimbursementsExcluded)} (not counted as income)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = InfoBlue, fontWeight = FontWeight.Medium)
                            )
                        }
                        if (plan.unconfirmedIncomeExcluded > 0.0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Unconfirmed Income Excluded: ${DateUtils.formatCurrency(plan.unconfirmedIncomeExcluded)} (pending receipt)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = WarningGold, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                if (plan.surplusStatus == CashSurplusStatus.SHORTFALL) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payday_shortfall_banner"),
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
                } else if (plan.surplusStatus == CashSurplusStatus.ZERO_SURPLUS) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payday_zero_surplus_banner"),
                        color = WarningGold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = WarningGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Exact Break-Even After Obligations (RM 0.00 Surplus)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = WarningGold)
                                )
                                Text(
                                    text = "Available cash exactly covers Priority 1 obligations with zero extra surplus left for savings top-ups, extra debt payoff, or discretionary spending.",
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

    // Item 3: Debt Ranking & Debt-by-Debt Waterfall Breakdown + Consequence
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

                if (plan.recommendedDebtAllocations.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payday_debt_breakdown_box"),
                        color = InfoBlue.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "RECOMMENDED DEBT-BY-DEBT EXTRA REPAYMENT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = InfoBlue,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            plan.recommendedDebtAllocations.forEach { alloc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${alloc.debtName} (Bal ${DateUtils.formatCurrency(alloc.outstandingBalance)})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                    Text(
                                        text = DateUtils.formatCurrency(alloc.allocatedAmount),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = InfoBlue)
                                    )
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

    // Item 4: Strict Validated Override Controls & Approval Actions
    item(key = "payday_override_approval") {
        val validation = PaydayIntelligenceEngine.validateApprovedAllocation(
            plan = plan,
            extraDebtRaw = formState.extraDebtOverrideText,
            emergencyRaw = formState.emergencyOverrideText,
            discretionaryRaw = formState.discretionaryOverrideText
        )

        val upcomingEssentialsVal = formState.upcomingEssentialsText.toDoubleOrNull()?.takeIf { it >= 0.0 }
        val emergencyTargetVal = formState.emergencyTargetText.toDoubleOrNull()?.takeIf { it >= 0.0 }
        val cashBufferVal = formState.cashBufferText.toDoubleOrNull()?.takeIf { it >= 0.0 }

        val eligibleDebts = plan.rankedDebts.filter { it.isEligibleForExtraRepayment }
        val referenceOnlyDebts = plan.rankedDebts.filter { it.classification == DebtRateClassification.REFERENCE_ONLY }

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
                    if (validation.hasOverrides || !validation.isValid) {
                        TextButton(
                            onClick = {
                                formState.extraDebtOverrideText = String.format(Locale.US, "%.2f", plan.recommendedExtraDebtRepayment)
                                formState.emergencyOverrideText = String.format(Locale.US, "%.2f", plan.recommendedEmergencySavings)
                                formState.discretionaryOverrideText = String.format(Locale.US, "%.2f", plan.recommendedDiscretionaryAllowance)
                            },
                            modifier = Modifier.testTag("reset_payday_overrides_btn")
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
                            "Approved Extra Debt (Max ${DateUtils.formatCurrency(plan.maxEligibleDebtBalance)} • Rec ${DateUtils.formatCurrency(plan.recommendedExtraDebtRepayment)})"
                        )
                    },
                    isError = validation.extraDebtError != null,
                    supportingText = validation.extraDebtError?.let { err ->
                        { Text(text = err, color = DangerRed, fontSize = 11.sp) }
                    },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payday_override_extra_debt")
                )

                // Live Approved Debt-by-Debt Distribution matching persisted keys/amounts
                if (validation.extraDebtError == null && validation.approvedDebtAllocations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payday_approved_debt_breakdown"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "APPROVED DEBT-BY-DEBT DISTRIBUTION",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            validation.approvedDebtAllocations.forEach { alloc ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${alloc.debtName} (${alloc.debtKey})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                    )
                                    Text(
                                        text = DateUtils.formatCurrency(alloc.allocatedAmount),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.emergencyOverrideText,
                    onValueChange = { formState.emergencyOverrideText = it },
                    label = {
                        Text(
                            "Approved Emergency Savings (Gap ${DateUtils.formatCurrency(plan.emergencyTargetGap)} • Rec ${DateUtils.formatCurrency(plan.recommendedEmergencySavings)})"
                        )
                    },
                    isError = validation.emergencyError != null,
                    supportingText = validation.emergencyError?.let { err ->
                        { Text(text = err, color = DangerRed, fontSize = 11.sp) }
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
                    isError = validation.discretionaryError != null,
                    supportingText = validation.discretionaryError?.let { err ->
                        { Text(text = err, color = DangerRed, fontSize = 11.sp) }
                    },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payday_override_discretionary")
                )

                if (validation.generalError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payday_validation_error_banner"),
                        color = DangerRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validation.generalError,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (!validation.isValid) DangerRed.copy(alpha = 0.14f) else SuccessGreen.copy(alpha = 0.12f),
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
                                text = if (validation.remainingBuffer < -0.01) "Over-Allocated Buffer Deficit:" else "Approved Remaining Cash Buffer:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = buildString {
                                    if (plan.configuredCashBufferTarget != null) {
                                        append("Min Required: ${DateUtils.formatCurrency(plan.requiredMinCashBuffer)} • ")
                                    }
                                    append(
                                        if (validation.hasOverrides) {
                                            "Rec Buffer: ${DateUtils.formatCurrency(plan.recommendedRemainingBuffer)}"
                                        } else {
                                            "Matches engine recommendation"
                                        }
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Text(
                            text = DateUtils.formatCurrency(validation.remainingBuffer),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (!validation.isValid) DangerRed else SuccessGreen
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
                            if (!validation.isValid) return@Button
                            val now = System.currentTimeMillis()
                            val recDebtJson = PaydayIntelligenceEngine.serializeDebtAllocations(plan.recommendedDebtAllocations)
                            val approvedDebtJson = PaydayIntelligenceEngine.serializeDebtAllocations(validation.approvedDebtAllocations)
                            val synchronizedDebtExplanation = PaydayIntelligenceEngine.formatDebtBreakdownExplanation(
                                allocations = validation.approvedDebtAllocations,
                                eligibleDebts = eligibleDebts,
                                referenceOnlyDebts = referenceOnlyDebts,
                                hasInsufficientCash = plan.hasInsufficientCash,
                                shortfallAmount = plan.shortfallAmount,
                                surplusStatus = plan.surplusStatus
                            )
                            val rationale = if (validation.hasOverrides) {
                                "Approved custom plan: $synchronizedDebtExplanation ${plan.spendingConsequenceSummary}"
                            } else {
                                plan.spendingConsequenceSummary
                            }

                            val entity = PaydayAllocationEntity(
                                id = savedAllocation?.id ?: 0L,
                                monthKey = monthKey,
                                cashSourceMode = plan.cashSourceMode.name,
                                openingCashInput = plan.openingBankBalanceUsed,
                                confirmedAvailableCash = plan.actualAvailableCash,
                                calculatedMonthlyCashflow = plan.calculatedMonthlyCashflow,
                                reimbursementsExcluded = plan.reimbursementsExcluded,
                                unconfirmedIncomeExcluded = plan.unconfirmedIncomeExcluded,
                                upcomingEssentialExpensesInput = upcomingEssentialsVal,
                                emergencyFundTargetInput = emergencyTargetVal,
                                cashBufferTargetInput = cashBufferVal,
                                minCashBufferEnforced = plan.requiredMinCashBuffer,
                                recEssentialCommitments = plan.essentialCommitmentsReserved,
                                recMinDebtPayments = plan.minimumDebtPaymentsReserved,
                                recUpcomingEssentials = plan.upcomingEssentialExpensesReserved,
                                recExtraDebtRepayment = plan.recommendedExtraDebtRepayment,
                                recExtraDebtTargetKey = plan.recommendedExtraDebtTargetKey,
                                recDebtAllocationsJson = recDebtJson,
                                recEmergencySavings = plan.recommendedEmergencySavings,
                                recDiscretionaryAllowance = plan.recommendedDiscretionaryAllowance,
                                recRemainingBuffer = plan.recommendedRemainingBuffer,
                                approvedExtraDebtRepayment = validation.parsedExtraDebt,
                                approvedDebtAllocationsJson = approvedDebtJson,
                                approvedEmergencySavings = validation.parsedEmergency,
                                approvedDiscretionaryAllowance = validation.parsedDiscretionary,
                                approvedRemainingBuffer = validation.remainingBuffer,
                                hasOverrides = validation.hasOverrides,
                                isApproved = true,
                                approvedTimestamp = now,
                                rationaleSummary = rationale,
                                updatedTimestamp = now
                            )
                            onSaveAndApprovePlan(entity)
                        },
                        enabled = validation.isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                            .testTag("approve_payday_plan_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (validation.hasOverrides) "Approve Custom Plan" else "Approve & Continue",
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
