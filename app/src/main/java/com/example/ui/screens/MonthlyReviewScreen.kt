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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.MonthlySummaryData
import com.example.data.model.FinancialProfileEntity
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningGold
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun MonthlyReviewScreen(
    summary: MonthlySummaryData?,
    profile: FinancialProfileEntity,
    selectedMonthKey: String,
    onSelectMonth: (String) -> Unit,
    onUpdateProfile: (FinancialProfileEntity) -> Unit
) {
    var showCalibrationDialog by remember { mutableStateOf(false) }
    val formattedMonth = DateUtils.formatMonthKey(selectedMonthKey)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("monthly_review_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Month Switcher
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { onSelectMonth(shiftMonth(selectedMonthKey, -1)) }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$formattedMonth Review",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Automatic Financial Calculation",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    IconButton(
                        onClick = { onSelectMonth(shiftMonth(selectedMonthKey, 1)) }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }

        if (summary != null) {
            // Cashflow Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (summary.remainingCashflow >= 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "NET MONTHLY SURPLUS / DEFICIT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (summary.remainingCashflow >= 0) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DateUtils.formatCurrency(summary.remainingCashflow),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (summary.remainingCashflow >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (summary.remainingCashflow >= 0) {
                                "Unallocated surplus available for accelerated debt payoff or savings buffer."
                            } else {
                                "Spending & commitments exceeded monthly income this month."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (summary.remainingCashflow >= 0) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            }

            // Income breakdown
            item {
                ReviewSectionCard(title = "Income") {
                    ReviewRow(label = "Net Monthly Salary", value = DateUtils.formatCurrency(summary.netSalary))
                    if (summary.isDecemberBonusMonth) {
                        ReviewRow(
                            label = "13th-Month Guaranteed Bonus",
                            value = "+ ${DateUtils.formatCurrency(summary.thirteenthMonthBonus)}",
                            valueColor = SuccessGreen
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ReviewRow(
                        label = "Total Income",
                        value = DateUtils.formatCurrency(summary.totalIncome),
                        isBold = true
                    )
                }
            }

            // Outflow breakdown
            item {
                ReviewSectionCard(title = "Outflows & Spending") {
                    ReviewRow(label = "Commitments Paid", value = DateUtils.formatCurrency(summary.paidCommitments))
                    ReviewRow(label = "Quick Daily Expenses", value = DateUtils.formatCurrency(summary.quickExpensesTotal))

                    if (summary.expensesByCategory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Daily Spending Categories:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        summary.expensesByCategory.forEach { (cat, amt) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "• $cat", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(text = DateUtils.formatCurrency(amt), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ReviewRow(
                        label = "Total Outflows",
                        value = DateUtils.formatCurrency(summary.totalOutflow),
                        isBold = true,
                        valueColor = if (summary.remainingCashflow >= 0) MaterialTheme.colorScheme.onSurface else DangerRed
                    )
                }
            }

            // Debt & Asset Balances
            item {
                ReviewSectionCard(title = "Debt & Net Worth Position") {
                    ReviewRow(label = "Remaining Debt Balance", value = DateUtils.formatCurrency(summary.totalDebtRemaining))
                    ReviewRow(
                        label = "Debt Paid Off Since Start",
                        value = DateUtils.formatCurrency(summary.totalDebtReduction),
                        valueColor = SuccessGreen
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ReviewRow(label = "Liquid Accessible Savings", value = DateUtils.formatCurrency(summary.accessibleSavings))
                    ReviewRow(label = "KWSP Retirement Balance", value = DateUtils.formatCurrency(summary.totalKwsp))
                }
            }

            // Month-end Calibrate Balances Button
            item {
                OutlinedButton(
                    onClick = { showCalibrationDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("calibrate_balances_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calibrate Financial Balances (Month-End)")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCalibrationDialog) {
        CalibrateBalancesDialog(
            profile = profile,
            onDismiss = { showCalibrationDialog = false },
            onSave = { updated ->
                onUpdateProfile(updated)
                showCalibrationDialog = false
            }
        )
    }
}

@Composable
fun ReviewSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun ReviewRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
                color = valueColor
            )
        )
    }
}

@Composable
fun CalibrateBalancesDialog(
    profile: FinancialProfileEntity,
    onDismiss: () -> Unit,
    onSave: (FinancialProfileEntity) -> Unit
) {
    var bankBal by remember { mutableStateOf(profile.bankBalance.toString()) }
    var dedicatedSav by remember { mutableStateOf(profile.dedicatedSavings.toString()) }
    var kwsp1 by remember { mutableStateOf(profile.kwspAccount1.toString()) }
    var kwsp2 by remember { mutableStateOf(profile.kwspAccount2.toString()) }
    var kwsp3 by remember { mutableStateOf(profile.kwspAccount3.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calibrate Month-End Balances", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = "Update your real snapshot figures. Historical defaults are preserved.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bankBal,
                        onValueChange = { bankBal = it },
                        label = { Text("Bank Balance (MYR)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dedicatedSav,
                        onValueChange = { dedicatedSav = it },
                        label = { Text("Dedicated Savings (MYR)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = kwsp1,
                        onValueChange = { kwsp1 = it },
                        label = { Text("KWSP Account 1 (MYR)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = kwsp2,
                        onValueChange = { kwsp2 = it },
                        label = { Text("KWSP Account 2 (MYR)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = kwsp3,
                        onValueChange = { kwsp3 = it },
                        label = { Text("KWSP Account 3 (MYR)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = profile.copy(
                        bankBalance = bankBal.toDoubleOrNull() ?: profile.bankBalance,
                        dedicatedSavings = dedicatedSav.toDoubleOrNull() ?: profile.dedicatedSavings,
                        kwspAccount1 = kwsp1.toDoubleOrNull() ?: profile.kwspAccount1,
                        kwspAccount2 = kwsp2.toDoubleOrNull() ?: profile.kwspAccount2,
                        kwspAccount3 = kwsp3.toDoubleOrNull() ?: profile.kwspAccount3
                    )
                    onSave(updated)
                }
            ) {
                Text("Save Balances")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun shiftMonth(monthKey: String, delta: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        }
        val date = sdf.parse(monthKey) ?: return monthKey
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur")).apply {
            time = date
            add(Calendar.MONTH, delta)
        }
        sdf.format(cal.time)
    } catch (e: Exception) {
        monthKey
    }
}
