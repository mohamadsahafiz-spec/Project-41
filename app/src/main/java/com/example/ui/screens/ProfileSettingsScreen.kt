package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.DebtEntity
import com.example.data.model.FinancialProfileEntity
import com.example.ui.theme.SuccessGreen
import com.example.logic.FlowchartCalculator
import com.example.util.DateUtils

@Composable
fun ProfileSettingsScreen(
    profile: FinancialProfileEntity,
    debts: List<DebtEntity>,
    onUpdateProfile: (FinancialProfileEntity) -> Unit,
    onUpdateDebt: (String, Double) -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onImportBackup: (String, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }

    var netSalary by remember(profile) { mutableStateOf(profile.netMonthlySalary.toString()) }
    var basicSalary by remember(profile) { mutableStateOf(profile.basicSalary.toString()) }
    var bankBal by remember(profile) { mutableStateOf(profile.bankBalance.toString()) }
    var dedicatedSav by remember(profile) { mutableStateOf(profile.dedicatedSavings.toString()) }
    var asb by remember(profile) { mutableStateOf(profile.asb.toString()) }
    var tabungHaji by remember(profile) { mutableStateOf(profile.tabungHaji.toString()) }
    var kwsp1 by remember(profile) { mutableStateOf(profile.kwspAccount1.toString()) }
    var kwsp2 by remember(profile) { mutableStateOf(profile.kwspAccount2.toString()) }
    var kwsp3 by remember(profile) { mutableStateOf(profile.kwspAccount3.toString()) }

    var isSaved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("profile_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Profile Info Header Card
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Salaried Field Service Engineer (Age 36)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Target Financial Freedom: Age 41–45 (Malaysia)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Salaried Field Service Engineer (stable primary income)\n• Guaranteed 13th-month bonus paid every December\n• Variable incentives & overtime are excluded from guaranteed budget\n• Local-first private database, no cloud accounts or banking credentials",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        // Income & Liquid Balances
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
                    Text(
                        text = "Income & Accessible Savings",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = netSalary,
                        onValueChange = { netSalary = it; isSaved = false },
                        label = { Text("Net Monthly Salary") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = basicSalary,
                        onValueChange = { basicSalary = it; isSaved = false },
                        label = { Text("Basic Salary (13th-Month Bonus Base)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = bankBal,
                        onValueChange = { bankBal = it; isSaved = false },
                        label = { Text("Primary Bank Balance (Liquid)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dedicatedSav,
                        onValueChange = { dedicatedSav = it; isSaved = false },
                        label = { Text("Dedicated Savings Reserve") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = asb,
                            onValueChange = { asb = it; isSaved = false },
                            label = { Text("ASB") },
                            prefix = { Text("RM ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tabungHaji,
                            onValueChange = { tabungHaji = it; isSaved = false },
                            label = { Text("Tabung Haji") },
                            prefix = { Text("RM ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // KWSP Retirement
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
                    Text(
                        text = "KWSP / EPF Retirement Accounts",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total KWSP: ${DateUtils.formatCurrency(profile.totalKwsp)} (Not liquid emergency cash)",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = kwsp1,
                        onValueChange = { kwsp1 = it; isSaved = false },
                        label = { Text("Account 1 (Retirement)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = kwsp2,
                        onValueChange = { kwsp2 = it; isSaved = false },
                        label = { Text("Account 2 (Housing/Health)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = kwsp3,
                        onValueChange = { kwsp3 = it; isSaved = false },
                        label = { Text("Account 3 (Flexible)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val updated = profile.copy(
                                netMonthlySalary = netSalary.toDoubleOrNull() ?: profile.netMonthlySalary,
                                basicSalary = basicSalary.toDoubleOrNull() ?: profile.basicSalary,
                                bankBalance = bankBal.toDoubleOrNull() ?: profile.bankBalance,
                                dedicatedSavings = dedicatedSav.toDoubleOrNull() ?: profile.dedicatedSavings,
                                asb = asb.toDoubleOrNull() ?: profile.asb,
                                tabungHaji = tabungHaji.toDoubleOrNull() ?: profile.tabungHaji,
                                kwspAccount1 = kwsp1.toDoubleOrNull() ?: profile.kwspAccount1,
                                kwspAccount2 = kwsp2.toDoubleOrNull() ?: profile.kwspAccount2,
                                kwspAccount3 = kwsp3.toDoubleOrNull() ?: profile.kwspAccount3
                            )
                            onUpdateProfile(updated)
                            isSaved = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSaved) "Profile Saved!" else "Save Profile Changes")
                    }
                }
            }
        }

        // Debt Balances Adjustment
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
                    Text(
                        text = "Current Debts",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    debts.forEach { debt ->
                        var editBal by remember(debt.currentBalance) { mutableStateOf(debt.currentBalance.toString()) }
                        val rateLabel = when (debt.key) {
                            "MAYBANK_CC" -> "15% p.a."
                            "SHOPEE_SLOAN" -> "1.5% monthly (provisional)"
                            "HL_HP1" -> "3% (reference only)"
                            "AMBANK_HP2" -> "3% (reference only)"
                            else -> "${debt.aprPercent}% p.a."
                        }
                        val isHp = debt.key == "HL_HP1" || debt.key == "AMBANK_HP2"
                        val isSettled = debt.currentBalance <= 0.01

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = debt.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (isSettled) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = SuccessGreen.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "SETTLED",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = SuccessGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Rate: $rateLabel • Monthly Target: ${DateUtils.formatCurrency(debt.monthlyTarget)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    if (debt.key == "AMBANK_HP2") {
                                        Text(
                                            text = "Note: RM250 is user personal contribution; legal loan balance is tracked in full.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    } else if (isHp) {
                                        Text(
                                            text = "Note: 3% is reference only. No automatic principal deduction. Update from lender statement.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = editBal,
                                    onValueChange = {
                                        editBal = it
                                        val newBal = it.toDoubleOrNull()
                                        if (newBal != null && newBal >= 0.0) {
                                            onUpdateDebt(debt.key, newBal)
                                        }
                                    },
                                    label = { Text(if (isHp) "Lender Balance" else "Balance") },
                                    prefix = { Text("RM ") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        // JSON Backup & Restore (Local-First Offline)
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
                    Text(
                        text = "Local-First Backup & Restore",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Export your financial snapshot as plain JSON. You can back it up or restore anytime without any external server or account.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onExportBackup { json ->
                                    showExportDialog = json
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("export_backup_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("import_backup_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                    }
                }
            }
        }

        // App Version & Changelog
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_version_changelog_card"),
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Project 41 • v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "Build ${BuildConfig.VERSION_CODE} • Payday Intelligence V1",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        OutlinedButton(
                            onClick = { showChangelogDialog = true },
                            modifier = Modifier.testTag("open_changelog_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Changelog")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = { Text("Project 41 Changelog", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "v1.1.0 — Payday Intelligence V1",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Added Stage 2 Payday Intelligence inside Monthly Check-In before payment confirmation.\n" +
                                "• Prioritizes essential bills, minimum debts, emergency fund protection, and highest-APR debt payoff.\n" +
                                "• Excludes reimbursements and unconfirmed income from spendable payday cash.\n" +
                                "• Supports user allocation overrides stored separately from engine recommendations.\n" +
                                "• Displays WHY explanations, missing data alerts, and spending consequence warnings.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp)
                        )
                    }
                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "v1.0.1 — Safety & Compact Widget",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Restored Poka-Yoke confirmation dialog and stage completion guards.\n" +
                                "• Compact dark home widget with floating QuickExpenseActivity popup.\n" +
                                "• Corrected HP1/HP2 3% reference handling and KWSP overtime exclusion.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp)
                        )
                    }
                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "v1.0.0 — Initial Project 41 Release",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• UK Personal Finance Flowchart v3.0.10 (Malaysia), Savings Vault, Milestones & JSON Backup.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelogDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Export Dialog
    showExportDialog?.let { json ->
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            title = { Text("Exported Financial JSON", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Copy this JSON text to securely store your backup offline:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    text = json,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("FreedomTrackerBackup", json))
                        showExportDialog = null
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = null }) { Text("Close") }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var importText by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restore From JSON Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste previously exported JSON content below:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it; importError = null },
                        placeholder = { Text("Paste JSON here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        singleLine = false
                    )
                    if (importError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = importError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isBlank()) {
                            importError = "Please paste valid JSON"
                        } else {
                            onImportBackup(importText) { success ->
                                if (success) {
                                    showImportDialog = false
                                } else {
                                    importError = "Invalid JSON structure. Please check backup format."
                                }
                            }
                        }
                    }
                ) {
                    Text("Restore Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }
}
