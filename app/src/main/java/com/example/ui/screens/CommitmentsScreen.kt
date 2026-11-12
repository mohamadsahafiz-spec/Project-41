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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.util.DateUtils

@Composable
fun CommitmentsScreen(
    monthKey: String,
    items: List<MonthlyCommitmentItem>,
    onToggleCommitment: (MonthlyCommitmentItem) -> Unit,
    onPayCustomAmount: (MonthlyCommitmentItem, Double) -> Unit,
    onUpdateRecurringDefault: (MonthlyCommitmentItem, Double) -> Unit
) {
    var customPayItem by remember { mutableStateOf<MonthlyCommitmentItem?>(null) }
    var updateDefaultItem by remember { mutableStateOf<MonthlyCommitmentItem?>(null) }

    val isCheckInActive = DateUtils.isCheckInPeriod()
    val formattedMonth = DateUtils.formatMonthKey(monthKey)

    val totalPlanned = items.sumOf { it.defaultAmount }
    val totalPaid = items.filter { it.isPaid }.sumOf { it.actualPaidAmount }
    val remaining = maxOf(0.0, totalPlanned - totalPaid)
    val progress = if (totalPlanned > 0) (totalPaid / totalPlanned).toFloat() else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("commitments_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Check-in window info badge
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
                            "Monthly Check-In Window Active (27th - End of Month)"
                        } else {
                            "Check-in period is active from 27th to end of month. Defaults restore each month."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isCheckInActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCheckInActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Summary Card: Total planned, paid, remaining
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("commitments_summary_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
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
                                text = "TOTAL PAID",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = SuccessGreen,
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = DateUtils.formatCurrency(totalPaid),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = SuccessGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // List of commitments
        items(items, key = { it.commitmentId }) { item ->
            CommitmentRowItem(
                item = item,
                onToggle = { onToggleCommitment(item) },
                onCustomPayClick = { customPayItem = item },
                onUpdateDefaultClick = { updateDefaultItem = item }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialog: Custom Payment Amount (Temporary for this month only)
    customPayItem?.let { item ->
        CustomPayDialog(
            item = item,
            onDismiss = { customPayItem = null },
            onConfirm = { amount ->
                onPayCustomAmount(item, amount)
                customPayItem = null
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
fun CommitmentRowItem(
    item: MonthlyCommitmentItem,
    onToggle: () -> Unit,
    onCustomPayClick: () -> Unit,
    onUpdateDefaultClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("commitment_item_${item.commitmentId}"),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPaid) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
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
                // One tap checkbox
                Checkbox(
                    checked = item.isPaid,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = SuccessGreen,
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
                            text = if (item.actualPaidAmount != item.defaultAmount) {
                                "Paid: ${DateUtils.formatCurrency(item.actualPaidAmount)} (Default was ${DateUtils.formatCurrency(item.defaultAmount)})"
                            } else {
                                "Paid: ${DateUtils.formatCurrency(item.actualPaidAmount)}"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold
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
                    text = if (item.isPaid) DateUtils.formatCurrency(item.actualPaidAmount) else DateUtils.formatCurrency(item.defaultAmount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (item.isPaid) SuccessGreen else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Action row: Custom payment & Edit default
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable(onClick = onCustomPayClick)
                ) {
                    Text(
                        text = if (item.isPaid) "Edit Paid Amount" else "Pay Different Amount",
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

@Composable
fun CustomPayDialog(
    item: MonthlyCommitmentItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(if (item.isPaid) item.actualPaidAmount.toString() else item.defaultAmount.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pay Different Amount",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Record an actual payment for '${item.name}' that differs from the default (${DateUtils.formatCurrency(item.defaultAmount)}).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: This is temporary for this month and will NOT overwrite your recurring default.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                            amountText = it
                            error = null
                        }
                    },
                    label = { Text("Actual Paid Amount (MYR)") },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_pay_input")
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount < 0.0) {
                        error = "Enter a valid amount"
                    } else {
                        onConfirm(amount)
                    }
                },
                modifier = Modifier.testTag("confirm_custom_pay_btn")
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UpdateDefaultDialog(
    item: MonthlyCommitmentItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var defaultText by remember { mutableStateOf(item.defaultAmount.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

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
                    text = "Permanently update the monthly recurring default for '${item.name}'.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Current default: ${DateUtils.formatCurrency(item.defaultAmount)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = defaultText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                            defaultText = it
                            error = null
                        }
                    },
                    label = { Text("New Monthly Default (MYR)") },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_default_input")
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = defaultText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        error = "Enter a valid default amount"
                    } else {
                        onConfirm(amount)
                    }
                },
                modifier = Modifier.testTag("confirm_update_default_btn")
            ) {
                Text("Update Default")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
