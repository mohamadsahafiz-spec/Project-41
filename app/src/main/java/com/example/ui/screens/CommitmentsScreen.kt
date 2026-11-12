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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.MonthlyIncomeEntity
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
    onUpdateRecurringDefault: (MonthlyCommitmentItem, Double) -> Unit
) {
    var showIncomeSection by remember { mutableStateOf(false) }
    var updateDefaultItem by remember { mutableStateOf<MonthlyCommitmentItem?>(null) }
    var showBatchConfirmDialog by remember { mutableStateOf(false) }

    // Poka-Yoke in-memory selection state (NOT saved until CONFIRM button is pressed)
    val selectedPayments = remember { mutableStateMapOf<Long, Double>() }

    val isCheckInActive = DateUtils.isCheckInPeriod()
    val formattedMonth = DateUtils.formatMonthKey(monthKey)

    val totalPlanned = items.sumOf { it.defaultAmount }
    val totalAlreadyPaid = items.filter { it.isPaid }.sumOf { it.actualPaidAmount }
    val remaining = maxOf(0.0, totalPlanned - totalAlreadyPaid)

    val selectedSum = selectedPayments.values.sum()

    // Initialize or read Income inputs
    var grossText by remember(currentIncome) { mutableStateOf((currentIncome?.grossSalary ?: 4158.00).toString()) }
    var netText by remember(currentIncome) { mutableStateOf((currentIncome?.netSalary ?: 4460.50).toString()) }
    var overtimeText by remember(currentIncome) { mutableStateOf((currentIncome?.overtime ?: 0.0).toString()) }
    var incentivesText by remember(currentIncome) { mutableStateOf((currentIncome?.incentives ?: 0.0).toString()) }
    var thirteenthMonthText by remember(currentIncome) { mutableStateOf((currentIncome?.thirteenthMonth ?: 0.0).toString()) }
    var actualEmployeeKwspText by remember(currentIncome) { mutableStateOf(currentIncome?.actualEmployeeKwsp?.toString() ?: "") }
    var actualEmployerKwspText by remember(currentIncome) { mutableStateOf(currentIncome?.actualEmployerKwsp?.toString() ?: "") }

    val gross = grossText.toDoubleOrNull() ?: 4158.00
    val kwspBreakdown = KwspCalculator.calculateKwsp(
        grossSalary = gross,
        actualEmployeeAmount = actualEmployeeKwspText.toDoubleOrNull(),
        actualEmployerAmount = actualEmployerKwspText.toDoubleOrNull()
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
                            "Monthly Check-In Active (27th - End of Month)"
                        } else {
                            "Check-in period is active from 27th to end of month. Defaults restore automatically."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isCheckInActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCheckInActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // MONTHLY INCOME CHECK-IN (Requirement 6)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showIncomeSection = !showIncomeSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Monthly Income Check-In",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Gross: ${DateUtils.formatCurrency(gross)} • Net: ${DateUtils.formatCurrency(netText.toDoubleOrNull() ?: 4460.50)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        IconButton(onClick = { showIncomeSection = !showIncomeSection }) {
                            Icon(
                                imageVector = if (showIncomeSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle income section"
                            )
                        }
                    }

                    if (showIncomeSection) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Gross salary drives KWSP estimates. Net salary drives take-home cashflow.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = grossText,
                                onValueChange = { grossText = it },
                                label = { Text("Gross Salary") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = netText,
                                onValueChange = { netText = it },
                                label = { Text("Net Take-Home") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = overtimeText,
                                onValueChange = { overtimeText = it },
                                label = { Text("Overtime (Optional)") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = incentivesText,
                                onValueChange = { incentivesText = it },
                                label = { Text("Incentives") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (DateUtils.isDecember(monthKey)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = thirteenthMonthText,
                                onValueChange = { thirteenthMonthText = it },
                                label = { Text("13th-Month Guaranteed Bonus") },
                                prefix = { Text("RM ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // KWSP Statutory Contribution Summary
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (kwspBreakdown.isEstimated) "KWSP STATUTORY ESTIMATE (Third Schedule)" else "KWSP CONFIRMED (Payslip)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Employee (11%): ${DateUtils.formatCurrency(kwspBreakdown.employeeAmount)} • Employer (13%): ${DateUtils.formatCurrency(kwspBreakdown.employerAmount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Total KWSP: ${DateUtils.formatCurrency(kwspBreakdown.totalContribution)} (Acc 1 [75%]: ${DateUtils.formatCurrency(kwspBreakdown.account1Allocation)}, Acc 2 [15%]: ${DateUtils.formatCurrency(kwspBreakdown.account2Allocation)}, Acc 3 [10%]: ${DateUtils.formatCurrency(kwspBreakdown.account3Allocation)})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = actualEmployeeKwspText,
                                onValueChange = { actualEmployeeKwspText = it },
                                label = { Text("Payslip Employee EPF") },
                                placeholder = { Text("Override") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = actualEmployerKwspText,
                                onValueChange = { actualEmployerKwspText = it },
                                label = { Text("Payslip Employer EPF") },
                                placeholder = { Text("Override") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val income = MonthlyIncomeEntity(
                                    monthKey = monthKey,
                                    grossSalary = grossText.toDoubleOrNull() ?: 4158.0,
                                    netSalary = netText.toDoubleOrNull() ?: 4460.50,
                                    overtime = overtimeText.toDoubleOrNull() ?: 0.0,
                                    incentives = incentivesText.toDoubleOrNull() ?: 0.0,
                                    thirteenthMonth = thirteenthMonthText.toDoubleOrNull() ?: 0.0,
                                    actualEmployeeKwsp = actualEmployeeKwspText.toDoubleOrNull(),
                                    actualEmployerKwsp = actualEmployerKwspText.toDoubleOrNull()
                                )
                                onSaveIncome(income)
                                showIncomeSection = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Monthly Income")
                        }
                    }
                }
            }
        }

        // Summary Card: Total planned, paid, remaining, selected
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("commitments_summary_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$formattedMonth Commitments",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Surface(
                            color = if (remaining <= 0.01) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (remaining <= 0.01) "ALL COMPLETED" else "${items.count { it.isPaid }}/${items.size} PAID",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining <= 0.01) SuccessGreen else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                text = "REMAINING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = DangerRed,
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = DateUtils.formatCurrency(remaining),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed
                                )
                            )
                        }
                    }
                }
            }
        }

        // Poka-Yoke Selection Header & Instructions
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Poka-Yoke: Select items below to prepare payment. Payments are only committed when you tap 'CONFIRM MONTHLY PAYMENTS'.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // List of commitment items
        items(items, key = { it.commitmentId }) { item ->
            val isSelectedInBatch = selectedPayments.containsKey(item.commitmentId)
            val currentPayAmount = selectedPayments[item.commitmentId] ?: item.defaultAmount

            PokaYokeCommitmentRowItem(
                item = item,
                isSelectedInBatch = isSelectedInBatch,
                batchAmount = currentPayAmount,
                onToggleSelection = {
                    if (item.isPaid) {
                        // If already marked paid, tapping allows unmarking
                        onUnmarkCommitment(item)
                    } else {
                        // Toggle in-memory selection
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

        // POKA-YOKE CONFIRMATION BUTTON (Requirement 5)
        item {
            Spacer(modifier = Modifier.height(6.dp))
            if (selectedPayments.isNotEmpty()) {
                Button(
                    onClick = { showBatchConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_monthly_payments_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONFIRM MONTHLY PAYMENTS (${selectedPayments.size} • ${DateUtils.formatCurrency(selectedSum)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // FINAL CONFIRMATION DIALOG (Requirement 5)
    if (showBatchConfirmDialog) {
        val selectedItemsList = items.filter { selectedPayments.containsKey(it.commitmentId) }
            .map { it to (selectedPayments[it.commitmentId] ?: it.defaultAmount) }

        AlertDialog(
            onDismissRequest = { showBatchConfirmDialog = false },
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
                            text = "Note: Instalments for HP1 and HP2 include interest and contributions. Outstanding loan balances update via statement calibrations in Settings.",
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
                        onConfirmBatchPayments(selectedItemsList)
                        selectedPayments.clear()
                        showBatchConfirmDialog = false
                    },
                    modifier = Modifier.testTag("dialog_confirm_payments_btn")
                ) {
                    Text("Record Payments")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchConfirmDialog = false }) {
                    Text("Cancel (Do not record)")
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
                // Checkbox: when not paid, selects for batch confirmation
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
                                // Also select it for batch
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
