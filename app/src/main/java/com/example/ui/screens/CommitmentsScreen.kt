package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MonthlyCommitmentItem
import com.example.data.MonthlySummaryData
import com.example.data.model.DebtEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.MonthlyIncomeEntity
import com.example.data.model.SavingsAccountEntity
import com.example.logic.FlowchartJourneyState
import com.example.logic.KwspCalculator
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningGold
import com.example.util.DateUtils

@Composable
fun CommitmentsScreen(
    monthKey: String,
    items: List<MonthlyCommitmentItem>,
    currentIncome: MonthlyIncomeEntity?,
    onSaveIncome: (MonthlyIncomeEntity) -> Unit,
    onConfirmBatchPayments: (List<Pair<MonthlyCommitmentItem, Double>>) -> Unit,
    onUnmarkCommitment: (MonthlyCommitmentItem) -> Unit,
    onUpdateRecurringDefault: (MonthlyCommitmentItem, Double) -> Unit,
    profile: FinancialProfileEntity? = null,
    summary: MonthlySummaryData? = null,
    savingsAccounts: List<SavingsAccountEntity> = emptyList(),
    totalEmergencyFund: Double = 0.0,
    totalDedicatedSavings: Double = 0.0,
    journeyState: FlowchartJourneyState? = null,
    debts: List<DebtEntity> = emptyList(),
    onDepositToSavings: ((accountId: Long, amount: Double, fromEverydayCash: Boolean, note: String) -> Unit)? = null,
    onWithdrawFromSavings: ((accountId: Long, amount: Double, toEverydayCash: Boolean, note: String) -> Unit)? = null,
    onCreditInterestToSavings: ((accountId: Long, amount: Double, note: String) -> Unit)? = null,
    onNavigateToDashboard: (() -> Unit)? = null
) {
    // Current Active Stage (1 to 5)
    var currentStage by remember { mutableStateOf(1) }

    // Explicit completion flags — opening a stage NEVER automatically marks it complete
    var isStage1Complete by remember(currentIncome) { mutableStateOf(currentIncome != null) }
    var isStage2Complete by remember { mutableStateOf(false) }
    var isStage3Complete by remember { mutableStateOf(false) }
    var isStage4Complete by remember { mutableStateOf(false) }
    var isStage5Complete by remember { mutableStateOf(false) }

    var updateDefaultItem by remember { mutableStateOf<MonthlyCommitmentItem?>(null) }
    var selectedAccountForDeposit by remember { mutableStateOf<SavingsAccountEntity?>(null) }
    var selectedAccountForWithdraw by remember { mutableStateOf<SavingsAccountEntity?>(null) }
    var selectedAccountForInterest by remember { mutableStateOf<SavingsAccountEntity?>(null) }

    // Poka-Yoke in-memory selection state (NOT saved until dialog confirmation)
    val selectedPayments = remember { mutableStateMapOf<Long, Double>() }
    var showBatchConfirmDialog by remember { mutableStateOf(false) }
    var isConfirmingPayments by remember { mutableStateOf(false) }

    val isCheckInActive = DateUtils.isCheckInPeriod()
    val formattedMonth = DateUtils.formatMonthKey(monthKey)

    val totalPlanned = items.sumOf { it.defaultAmount }
    val totalAlreadyPaid = items.filter { it.isPaid }.sumOf { it.actualPaidAmount }
    val remaining = maxOf(0.0, totalPlanned - totalAlreadyPaid)
    val selectedSum = selectedPayments.values.sum()

    // Initialize Income inputs: Actual amounts received
    var grossText by remember(currentIncome) { mutableStateOf((currentIncome?.grossSalary ?: 4158.00).toString()) }
    var netText by remember(currentIncome) { mutableStateOf((currentIncome?.netSalary ?: 4460.50).toString()) }
    var overtimeText by remember(currentIncome) { mutableStateOf((currentIncome?.overtime ?: 0.0).toString()) }
    var incentivesText by remember(currentIncome) { mutableStateOf((currentIncome?.incentives ?: 0.0).toString()) }
    var thirteenthMonthText by remember(currentIncome) { mutableStateOf((currentIncome?.thirteenthMonth ?: 0.0).toString()) }
    var actualEmployeeKwspText by remember(currentIncome) { mutableStateOf(currentIncome?.actualEmployeeKwsp?.toString() ?: "") }
    var actualEmployerKwspText by remember(currentIncome) { mutableStateOf(currentIncome?.actualEmployerKwsp?.toString() ?: "") }

    val gross = grossText.toDoubleOrNull() ?: 4158.00
    val netSalaryVal = netText.toDoubleOrNull() ?: 4460.50
    val overtimeVal = overtimeText.toDoubleOrNull() ?: 0.0
    val incentivesVal = incentivesText.toDoubleOrNull() ?: 0.0
    val thirteenthMonthVal = thirteenthMonthText.toDoubleOrNull() ?: 0.0

    // Actual take-home cash flow: Net salary + actual additional amounts received
    val actualTakeHomeCashflow = netSalaryVal + overtimeVal + incentivesVal + thirteenthMonthVal

    // KWSP calculation strictly excludes overtime from contributable wages
    val kwspBreakdown = KwspCalculator.calculateKwsp(
        grossSalary = gross,
        incentives = incentivesVal,
        thirteenthMonth = thirteenthMonthVal,
        overtime = overtimeVal,
        actualEmployeeAmount = actualEmployeeKwspText.toDoubleOrNull(),
        actualEmployerAmount = actualEmployerKwspText.toDoubleOrNull()
    )

    val completedStagesMap = mapOf(
        1 to isStage1Complete,
        2 to isStage2Complete,
        3 to isStage3Complete,
        4 to isStage4Complete,
        5 to isStage5Complete
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("commitments_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Check-in status badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isCheckInActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isCheckInActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCheckInActive) {
                            "Monthly Check-In Active ($formattedMonth • 27th - End of Month)"
                        } else {
                            "Check-in period is active from 27th to month-end. You can review and update all stages anytime."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isCheckInActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCheckInActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // =========================================================================
        // VISIBLE FIVE-STAGE PROGRESS STEPPER INDICATOR
        // =========================================================================
        item {
            CheckInStepperBar(
                currentStage = currentStage,
                completedStages = completedStagesMap,
                onStageClick = { clickedStage ->
                    currentStage = clickedStage
                }
            )
        }

        // =========================================================================
        // STAGE 1 — MONTHLY INCOME
        // =========================================================================
        if (currentStage == 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                        text = "1",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "STAGE 1 — MONTHLY INCOME",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Record actual amounts received. Gross salary is NOT added to cash flow.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        // A. Basic Salary & E. Net Take-Home
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = grossText,
                                onValueChange = { grossText = it },
                                label = { Text("Basic Salary (for KWSP)") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = netText,
                                onValueChange = { netText = it },
                                label = { Text("Net Take-Home (Actual)") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // B. Overtime (EXCLUDED from KWSP) & C. Incentives
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = overtimeText,
                                onValueChange = { overtimeText = it },
                                label = { Text("Overtime (Actual)") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = incentivesText,
                                onValueChange = { incentivesText = it },
                                label = { Text("Incentives (Actual)") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // D. 13th-Month Bonus
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = thirteenthMonthText,
                            onValueChange = { thirteenthMonthText = it },
                            label = { Text("13th-Month Bonus (Actual Received)") },
                            prefix = { Text("RM ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actual Take-Home Cashflow Summary
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Monthly Cash Flow Received:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Net (${DateUtils.formatCurrency(netSalaryVal)}) + OT (${DateUtils.formatCurrency(overtimeVal)}) + Inc (${DateUtils.formatCurrency(incentivesVal)}) + Bonus (${DateUtils.formatCurrency(thirteenthMonthVal)})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Text(
                                    text = DateUtils.formatCurrency(actualTakeHomeCashflow),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SuccessGreen)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // KWSP Statutory Contribution Summary & Overtime Exclusion Badge
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (kwspBreakdown.isEstimated) "KWSP STATUTORY ESTIMATE" else "KWSP CONFIRMED FROM PAYSLIP",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = if (kwspBreakdown.isEstimated) WarningGold else SuccessGreen
                                        )
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "OT Excluded",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Eligible Contributable Wages: ${DateUtils.formatCurrency(kwspBreakdown.contributableWages)} (Overtime ${DateUtils.formatCurrency(overtimeVal)} strictly excluded)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Employee (11%): ${DateUtils.formatCurrency(kwspBreakdown.employeeAmount)} • Employer (${kwspBreakdown.employerRatePercent.toInt()}%): ${DateUtils.formatCurrency(kwspBreakdown.employerAmount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Total Monthly KWSP: ${DateUtils.formatCurrency(kwspBreakdown.totalContribution)} (Acc 1 [75%]: ${DateUtils.formatCurrency(kwspBreakdown.account1Allocation)}, Acc 2 [15%]: ${DateUtils.formatCurrency(kwspBreakdown.account2Allocation)}, Acc 3 [10%]: ${DateUtils.formatCurrency(kwspBreakdown.account3Allocation)})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Optional: Enter actual figures from your payslip to override statutory estimates:",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = actualEmployeeKwspText,
                                onValueChange = { actualEmployeeKwspText = it },
                                label = { Text("Payslip Employee EPF") },
                                placeholder = { Text("Optional") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = actualEmployerKwspText,
                                onValueChange = { actualEmployerKwspText = it },
                                label = { Text("Payslip Employer EPF") },
                                placeholder = { Text("Optional") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stage 1 Action: Explicit Save & Continue
                        Button(
                            onClick = {
                                val income = MonthlyIncomeEntity(
                                    monthKey = monthKey,
                                    grossSalary = gross,
                                    netSalary = netSalaryVal,
                                    overtime = overtimeVal,
                                    incentives = incentivesVal,
                                    thirteenthMonth = thirteenthMonthVal,
                                    actualEmployeeKwsp = actualEmployeeKwspText.toDoubleOrNull(),
                                    actualEmployerKwsp = actualEmployerKwspText.toDoubleOrNull()
                                )
                                onSaveIncome(income)
                                isStage1Complete = true
                                currentStage = 2
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_income_continue_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Continue to Stage 2: Commitments", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // STAGE 2 — MONTHLY COMMITMENTS
        // =========================================================================
        if (currentStage == 2) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("commitments_summary_card"),
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
                            Column {
                                Text(
                                    text = "STAGE 2 — MONTHLY COMMITMENTS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Select payments to record. Checkboxes do NOT commit payments yet.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "PLANNED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = DateUtils.formatCurrency(totalPlanned),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "RECORDED PAID",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = SuccessGreen,
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = DateUtils.formatCurrency(totalAlreadyPaid),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "SELECTED (IN-MEMORY)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = DateUtils.formatCurrency(selectedSum),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // List of commitment items with checkboxes and amount adjustment
            items(items, key = { it.commitmentId }) { item ->
                val isSelectedInBatch = selectedPayments.containsKey(item.commitmentId)
                val currentPayAmount = selectedPayments[item.commitmentId] ?: item.defaultAmount

                PokaYokeCommitmentRowItem(
                    item = item,
                    isSelectedInBatch = isSelectedInBatch,
                    batchAmount = currentPayAmount,
                    onToggleSelection = {
                        if (item.isPaid) {
                            onUnmarkCommitment(item)
                        } else {
                            if (isSelectedInBatch) {
                                selectedPayments.remove(item.commitmentId)
                            } else {
                                selectedPayments[item.commitmentId] = item.defaultAmount
                            }
                        }
                    },
                    onAmountChange = { newAmt ->
                        selectedPayments[item.commitmentId] = newAmt
                    },
                    onUpdateDefaultClick = { updateDefaultItem = item }
                )
            }

            // Stage 2 Actions
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStage = 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stage 1")
                    }

                    Button(
                        onClick = {
                            isStage2Complete = true
                            currentStage = 3
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continue to Stage 3 (${selectedPayments.size} selected)")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // =========================================================================
        // STAGE 3 — PAYMENT CONFIRMATION (POKA-YOKE)
        // =========================================================================
        if (currentStage == 3) {
            val selectedItemsList = items.filter { selectedPayments.containsKey(it.commitmentId) }
                .map { it to (selectedPayments[it.commitmentId] ?: it.defaultAmount) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                        text = "3",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "STAGE 3 — PAYMENT CONFIRMATION",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Review payment batch. Payments will ONLY record after confirmation in dialog.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        if (selectedItemsList.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (items.all { it.isPaid }) "All monthly commitments are already recorded as paid!" else "No payments currently selected in batch.",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "If no further commitments are due this month, tap below to confirm and proceed to Savings Vault.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            isStage3Complete = true
                                            currentStage = 4
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Confirm No Payments Due & Continue")
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Selected Payments to Record for $formattedMonth:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            selectedItemsList.forEach { (item, amt) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                    Text(
                                        text = DateUtils.formatCurrency(amt),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Total Selected Amount:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = DateUtils.formatCurrency(selectedSum),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // POKA-YOKE RESTORATION: Opens confirmation dialog, DOES NOT commit directly!
                            Button(
                                onClick = {
                                    showBatchConfirmDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("confirm_monthly_payments_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CONFIRM MONTHLY PAYMENTS (${selectedItemsList.size} • ${DateUtils.formatCurrency(selectedSum)})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { currentStage = 2 },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back to Stage 2: Adjust Selections")
                        }
                    }
                }
            }
        }

        // =========================================================================
        // STAGE 4 — SAVINGS VAULT
        // =========================================================================
        if (currentStage == 4) {
            val emergencyAccounts = savingsAccounts.filter { it.isEmergencyFund }
            val otherSavingsAccounts = savingsAccounts.filter { !it.isEmergencyFund }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                        text = "4",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "STAGE 4 — SAVINGS VAULT",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Record savings deposits, withdrawals & verified returns.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Important: Internal transfers between Everyday Bank Cash and Savings Vault accounts do not count as new income or additional wealth.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category A: Emergency Fund Accounts
                        Text(
                            text = "EMERGENCY FUND (LIQUID RESERVES)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = WarningGold,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (emergencyAccounts.isEmpty()) {
                            Text(
                                text = "No dedicated emergency fund account registered. Total: ${DateUtils.formatCurrency(totalEmergencyFund)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        } else {
                            emergencyAccounts.forEach { acc ->
                                SavingsAccountCheckInRow(
                                    account = acc,
                                    onDepositClick = { selectedAccountForDeposit = acc },
                                    onWithdrawClick = { selectedAccountForWithdraw = acc },
                                    onInterestClick = { selectedAccountForInterest = acc }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category B: Other Savings Goals
                        Text(
                            text = "OTHER SAVINGS GOALS (DEDICATED ACCOUNTS)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (otherSavingsAccounts.isEmpty()) {
                            Text(
                                text = "No other savings goals recorded yet.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        } else {
                            otherSavingsAccounts.forEach { acc ->
                                SavingsAccountCheckInRow(
                                    account = acc,
                                    onDepositClick = { selectedAccountForDeposit = acc },
                                    onWithdrawClick = { selectedAccountForWithdraw = acc },
                                    onInterestClick = { selectedAccountForInterest = acc }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stage 4 Explicit Decision Actions: User must explicitly complete savings review
                        Button(
                            onClick = {
                                isStage4Complete = true
                                currentStage = 5
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Savings Review & Proceed to Stage 5")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                isStage4Complete = true
                                currentStage = 5
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm No Savings Transactions This Month")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { currentStage = 3 },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back to Stage 3: Confirmations")
                        }
                    }
                }
            }
        }

        // =========================================================================
        // STAGE 5 — MONTHLY REVIEW (COMPLETION GUARDED)
        // =========================================================================
        if (currentStage == 5) {
            val netTakeHome = currentIncome?.netSalary ?: profile?.netMonthlySalary ?: 4460.50
            val totalIncome = (currentIncome?.totalTakeHomeIncome) ?: summary?.totalIncome ?: actualTakeHomeCashflow
            val commitmentsPaid = summary?.paidCommitments ?: totalAlreadyPaid
            val expensesVal = summary?.quickExpensesTotal ?: 0.0
            val cashflowSurplus = totalIncome - commitmentsPaid - expensesVal
            val currentDebt = summary?.totalDebtRemaining ?: debts.sumOf { it.currentBalance }
            val kwspBalance = summary?.totalKwsp ?: profile?.totalKwsp ?: 0.0

            val allPriorStagesComplete = isStage1Complete && isStage2Complete && isStage3Complete && isStage4Complete

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                        text = "5",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "STAGE 5 — MONTHLY REVIEW",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Automated financial breakdown & journey progression.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Review Metrics Grid
                        ReviewMetricRow(label = "Monthly Take-Home Income:", value = DateUtils.formatCurrency(totalIncome), isPositive = true)
                        ReviewMetricRow(label = "Total Commitments Paid:", value = DateUtils.formatCurrency(commitmentsPaid), isPositive = null)
                        ReviewMetricRow(label = "Quick Expenses Recorded:", value = DateUtils.formatCurrency(expensesVal), isPositive = null)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        ReviewMetricRow(
                            label = "Cash-Flow Surplus:",
                            value = DateUtils.formatCurrency(cashflowSurplus),
                            isPositive = cashflowSurplus >= 0.0
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Balance Positions
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "CURRENT ASSET & DEBT POSITION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Dedicated Savings Vault:", style = MaterialTheme.typography.bodySmall)
                                    Text(DateUtils.formatCurrency(totalDedicatedSavings), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Current Total Debt Remaining:", style = MaterialTheme.typography.bodySmall)
                                    Text(DateUtils.formatCurrency(currentDebt), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = if (currentDebt > 0) DangerRed else SuccessGreen))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total KWSP Retirement Balance:", style = MaterialTheme.typography.bodySmall)
                                    Text(DateUtils.formatCurrency(kwspBalance), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Financial Journey Progress & Next Target
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "FINANCIAL JOURNEY PROGRESS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = journeyState?.currentStepTitle ?: "Step 1: Starter Emergency Fund",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                val activeStep = journeyState?.steps?.find { it.status == com.example.logic.StepStatus.ACTIVE }
                                Text(
                                    text = activeStep?.description ?: "Build starter emergency liquidity to buffer against unexpected essential costs.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Next Target: ${journeyState?.nextTargetTitle ?: "Step 1: Starter Emergency Buffer"}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // COMPLETION LOGIC CORRECTION:
                        // Only display "Monthly Check-In Complete!" when all required stages have genuinely been completed
                        if (!allPriorStagesComplete) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = WarningGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = WarningGold, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Check-In Incomplete",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall.copy(color = WarningGold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Complete the following pending stages before finalizing check-in:",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (!isStage1Complete) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("• Stage 1: Income not saved", style = MaterialTheme.typography.bodySmall)
                                            TextButton(onClick = { currentStage = 1 }) { Text("Go to Stage 1") }
                                        }
                                    }
                                    if (!isStage2Complete) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("• Stage 2: Commitments not reviewed", style = MaterialTheme.typography.bodySmall)
                                            TextButton(onClick = { currentStage = 2 }) { Text("Go to Stage 2") }
                                        }
                                    }
                                    if (!isStage3Complete) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("• Stage 3: Payment confirmation required", style = MaterialTheme.typography.bodySmall)
                                            TextButton(onClick = { currentStage = 3 }) { Text("Go to Stage 3") }
                                        }
                                    }
                                    if (!isStage4Complete) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("• Stage 4: Savings review decision required", style = MaterialTheme.typography.bodySmall)
                                            TextButton(onClick = { currentStage = 4 }) { Text("Go to Stage 4") }
                                        }
                                    }
                                }
                            }
                        } else if (!isStage5Complete) {
                            // All prior stages completed; user can now explicitly complete Stage 5
                            Button(
                                onClick = {
                                    isStage5Complete = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("COMPLETE MONTHLY CHECK-IN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else {
                            // All 5 stages genuinely completed!
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SuccessGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Monthly Check-In Complete!",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall.copy(color = SuccessGreen)
                                        )
                                        Text(
                                            text = "All 5 stages for $formattedMonth have been validated and saved.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentStage = 1 },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Review Stages")
                                }

                                Button(
                                    onClick = {
                                        onNavigateToDashboard?.invoke()
                                    },
                                    modifier = Modifier.weight(2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Dashboard", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // POKA-YOKE CONFIRMATION DIALOG FOR PAYMENTS (RESTORED)
    // =========================================================================
    if (showBatchConfirmDialog) {
        val selectedItemsList = items.filter { selectedPayments.containsKey(it.commitmentId) }
            .map { it to (selectedPayments[it.commitmentId] ?: it.defaultAmount) }

        AlertDialog(
            onDismissRequest = {
                if (!isConfirmingPayments) {
                    showBatchConfirmDialog = false
                }
            },
            title = {
                Text(
                    text = "Confirm Monthly Payments",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Review and confirm the payments to record for $formattedMonth:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    selectedItemsList.forEach { (item, amt) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${item.name}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = DateUtils.formatCurrency(amt),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total to Record:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = DateUtils.formatCurrency(selectedSum),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Note: Payments are only committed upon pressing Confirm below. Cancelling will record nothing.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isConfirmingPayments) {
                            isConfirmingPayments = true
                            onConfirmBatchPayments(selectedItemsList)
                            selectedPayments.clear()
                            isStage3Complete = true
                            showBatchConfirmDialog = false
                            isConfirmingPayments = false
                            currentStage = 4
                        }
                    },
                    enabled = !isConfirmingPayments,
                    modifier = Modifier.testTag("dialog_confirm_payments_btn")
                ) {
                    Text(if (isConfirmingPayments) "Recording..." else "Confirm Payments")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatchConfirmDialog = false },
                    enabled = !isConfirmingPayments
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Permanent Monthly Default Update
    updateDefaultItem?.let { item ->
        UpdateDefaultDialog(
            item = item,
            onDismiss = { updateDefaultItem = null },
            onConfirm = { newDefault ->
                onUpdateRecurringDefault(item, newDefault)
                updateDefaultItem = null
            }
        )
    }

    // Savings Vault Dialogs
    selectedAccountForDeposit?.let { acc ->
        DepositDialog(
            account = acc,
            everydayCash = profile?.bankBalance ?: 2500.0,
            onDismiss = { selectedAccountForDeposit = null },
            onConfirm = { amount, fromCash, note ->
                onDepositToSavings?.invoke(acc.id, amount, fromCash, note)
                selectedAccountForDeposit = null
            }
        )
    }

    selectedAccountForWithdraw?.let { acc ->
        WithdrawDialog(
            account = acc,
            onDismiss = { selectedAccountForWithdraw = null },
            onConfirm = { amount, toCash, note ->
                onWithdrawFromSavings?.invoke(acc.id, amount, toCash, note)
                selectedAccountForWithdraw = null
            }
        )
    }

    selectedAccountForInterest?.let { acc ->
        CreditInterestDialog(
            account = acc,
            onDismiss = { selectedAccountForInterest = null },
            onConfirm = { amount, note ->
                onCreditInterestToSavings?.invoke(acc.id, amount, note)
                selectedAccountForInterest = null
            }
        )
    }
}

@Composable
fun CheckInStepperBar(
    currentStage: Int,
    completedStages: Map<Int, Boolean>,
    onStageClick: (Int) -> Unit
) {
    val stageTitles = listOf(
        1 to "1. Income",
        2 to "2. Commitments",
        3 to "3. Confirm",
        4 to "4. Savings",
        5 to "5. Review"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                stageTitles.forEach { (num, title) ->
                    val isActive = currentStage == num
                    val isDone = completedStages[num] == true

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onStageClick(num) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isActive -> MaterialTheme.colorScheme.primary
                                isDone -> SuccessGreen
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isDone && !isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Text(
                                        text = num.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            fontSize = 9.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isActive -> MaterialTheme.colorScheme.primary
                                isDone -> SuccessGreen
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { currentStage / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun ReviewMetricRow(
    label: String,
    value: String,
    isPositive: Boolean? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = when (isPositive) {
                    true -> SuccessGreen
                    false -> DangerRed
                    null -> MaterialTheme.colorScheme.onSurface
                }
            )
        )
    }
}

@Composable
fun SavingsAccountCheckInRow(
    account: SavingsAccountEntity,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onInterestClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = account.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = "${account.purpose} • Return: ${account.expectedAnnualReturnPercent ?: 0.0}% p.a.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Text(
                    text = DateUtils.formatCurrency(account.currentBalance),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = SuccessGreen)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable(onClick = onDepositClick)
                ) {
                    Text(
                        text = "+ Deposit",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable(onClick = onWithdrawClick)
                ) {
                    Text(
                        text = "- Withdraw",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = SuccessGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable(onClick = onInterestClick)
                ) {
                    Text(
                        text = "+ Credit Return",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreen),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PokaYokeCommitmentRowItem(
    item: MonthlyCommitmentItem,
    isSelectedInBatch: Boolean,
    batchAmount: Double,
    onToggleSelection: () -> Unit,
    onAmountChange: (Double) -> Unit,
    onUpdateDefaultClick: () -> Unit
) {
    var isEditingAmount by remember { mutableStateOf(false) }
    var editAmountText by remember(batchAmount) { mutableStateOf(batchAmount.toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("commitment_item_${item.commitmentId}"),
        colors = CardDefaults.cardColors(
            containerColor = when {
                item.isPaid -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                isSelectedInBatch -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isPaid) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isPaid || isSelectedInBatch,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (item.isPaid) SuccessGreen else MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("commitment_checkbox_${item.commitmentId}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (item.isDebt) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "DEBT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (item.isPaid) {
                        Text(
                            text = "Recorded Paid: ${DateUtils.formatCurrency(item.actualPaidAmount)} (Tap checkbox to unmark)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    } else if (isSelectedInBatch) {
                        Text(
                            text = "Ready to confirm: ${DateUtils.formatCurrency(batchAmount)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Text(
                            text = "Default: ${DateUtils.formatCurrency(item.defaultAmount)}/month",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Text(
                    text = if (item.isPaid) DateUtils.formatCurrency(item.actualPaidAmount) else DateUtils.formatCurrency(batchAmount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (item.isPaid) SuccessGreen else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Options: Edit payment amount & Edit recurring default
            if (!item.isPaid) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditingAmount) {
                        OutlinedTextField(
                            value = editAmountText,
                            onValueChange = {
                                editAmountText = it
                                val parsed = it.toDoubleOrNull()
                                if (parsed != null && parsed >= 0.0) {
                                    onAmountChange(parsed)
                                }
                            },
                            label = { Text("Amount") },
                            prefix = { Text("RM ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(130.dp)
                        )
                        TextButton(onClick = { isEditingAmount = false }) {
                            Text("Done")
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                isEditingAmount = true
                                if (!isSelectedInBatch) onToggleSelection()
                            }
                        ) {
                            Text(
                                text = "Adjust Amount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(onClick = onUpdateDefaultClick)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Change Default",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateDefaultDialog(
    item: MonthlyCommitmentItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var newAmountText by remember { mutableStateOf(item.defaultAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Recurring Default",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Update the standard monthly commitment amount for ${item.name}:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newAmountText,
                    onValueChange = { newAmountText = it },
                    label = { Text("Default Monthly Amount") },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = newAmountText.toDoubleOrNull()
                    if (parsed != null && parsed >= 0.0) {
                        onConfirm(parsed)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
