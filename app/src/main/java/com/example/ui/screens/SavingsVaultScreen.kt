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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.SavingsAccountEntity
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningGold
import com.example.util.DateUtils
import kotlin.math.min

@Composable
fun SavingsVaultScreen(
    profile: FinancialProfileEntity,
    accounts: List<SavingsAccountEntity>,
    totalEmergencyFund: Double,
    totalDedicatedSavings: Double,
    totalInterestCredited: Double,
    onAddAccount: (name: String, purpose: String, isEmergencyFund: Boolean, initialBal: Double, expectedReturn: Double?) -> Unit,
    onDeposit: (accountId: Long, amount: Double, fromEverydayCash: Boolean, note: String) -> Unit,
    onWithdraw: (accountId: Long, amount: Double, toEverydayCash: Boolean, note: String) -> Unit,
    onCreditInterest: (accountId: Long, amount: Double, note: String) -> Unit
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var selectedAccountForDeposit by remember { mutableStateOf<SavingsAccountEntity?>(null) }
    var selectedAccountForWithdraw by remember { mutableStateOf<SavingsAccountEntity?>(null) }
    var selectedAccountForInterest by remember { mutableStateOf<SavingsAccountEntity?>(null) }

    val starterBufferTarget = 1000.0
    val starterProgress = min(100f, (totalEmergencyFund / starterBufferTarget * 100.0).toFloat())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("savings_vault_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MY SAVINGS VAULT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Deliberate reserves separated from everyday cash",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Button(
                    onClick = { showAddAccountDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("add_savings_account_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Three Distinct Financial Categories (Requirement 3)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "THREE DISTINCT CASH TIERS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Category A
                    TierRow(
                        categoryLetter = "A",
                        title = "EVERYDAY CASH",
                        subtitle = "Primary checking / operational bank cash",
                        amount = DateUtils.formatCurrency(profile.bankBalance),
                        note = "Transactional funds. Does NOT count as emergency fund.",
                        color = MaterialTheme.colorScheme.secondary
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                    // Category B
                    TierRow(
                        categoryLetter = "B",
                        title = "DEDICATED SAVINGS",
                        subtitle = "Savings Vault • Deliberate liquid reserves",
                        amount = DateUtils.formatCurrency(totalDedicatedSavings),
                        note = "Emergency fund: ${DateUtils.formatCurrency(totalEmergencyFund)}",
                        color = SuccessGreen
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                    // Category C
                    TierRow(
                        categoryLetter = "C",
                        title = "RETIREMENT ASSETS",
                        subtitle = "KWSP / EPF statutory retirement",
                        amount = DateUtils.formatCurrency(profile.totalKwsp),
                        note = "Statutory long-term pillar. Locked for retirement.",
                        color = WarningGold
                    )
                }
            }
        }

        // Emergency Fund Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Emergency Fund Progress",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        Surface(
                            color = if (totalEmergencyFund >= starterBufferTarget) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (totalEmergencyFund >= starterBufferTarget) "STARTER BUFFER UNLOCKED" else "IN PROGRESS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (totalEmergencyFund >= starterBufferTarget) SuccessGreen else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "DEDICATED EMERGENCY FUND",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            )
                            Text(
                                text = DateUtils.formatCurrency(totalEmergencyFund),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "STEP 1 TARGET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            )
                            Text(
                                text = DateUtils.formatCurrency(starterBufferTarget),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { starterProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = SuccessGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total returns credited to date: ${DateUtils.formatCurrency(totalInterestCredited)} (confirmed actual growth)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }

        // Savings Accounts List
        item {
            Text(
                text = "Dedicated Savings Accounts",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        if (accounts.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No dedicated savings accounts yet",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create an account above to start building your emergency fund.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }
                }
            }
        } else {
            items(accounts, key = { it.id }) { account ->
                SavingsAccountCard(
                    account = account,
                    onDepositClick = { selectedAccountForDeposit = account },
                    onWithdrawClick = { selectedAccountForWithdraw = account },
                    onInterestClick = { selectedAccountForInterest = account }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        AddSavingsAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, purpose, isEmergency, bal, ret ->
                onAddAccount(name, purpose, isEmergency, bal, ret)
                showAddAccountDialog = false
            }
        )
    }

    // Deposit Dialog
    selectedAccountForDeposit?.let { account ->
        DepositDialog(
            account = account,
            everydayCash = profile.bankBalance,
            onDismiss = { selectedAccountForDeposit = null },
            onConfirm = { amount, fromEveryday, note ->
                onDeposit(account.id, amount, fromEveryday, note)
                selectedAccountForDeposit = null
            }
        )
    }

    // Withdraw Dialog
    selectedAccountForWithdraw?.let { account ->
        WithdrawDialog(
            account = account,
            onDismiss = { selectedAccountForWithdraw = null },
            onConfirm = { amount, toEveryday, note ->
                onWithdraw(account.id, amount, toEveryday, note)
                selectedAccountForWithdraw = null
            }
        )
    }

    // Interest/Return Credited Dialog
    selectedAccountForInterest?.let { account ->
        CreditInterestDialog(
            account = account,
            onDismiss = { selectedAccountForInterest = null },
            onConfirm = { amount, note ->
                onCreditInterest(account.id, amount, note)
                selectedAccountForInterest = null
            }
        )
    }
}

@Composable
fun TierRow(
    categoryLetter: String,
    title: String,
    subtitle: String,
    amount: String,
    note: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = categoryLetter,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp
                )
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun SavingsAccountCard(
    account: SavingsAccountEntity,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onInterestClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("savings_account_${account.id}"),
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
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (account.isEmergencyFund) SuccessGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = account.purpose,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (account.isEmergencyFund) SuccessGreen else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (account.expectedAnnualReturnPercent != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Est: ${account.expectedAnnualReturnPercent}% p.a.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Text(
                    text = DateUtils.formatCurrency(account.currentBalance),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (account.totalInterestCredited > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Confirmed returns credited: +${DateUtils.formatCurrency(account.totalInterestCredited)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDepositClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deposit", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onWithdrawClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Withdraw", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onInterestClick,
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Return", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AddSavingsAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, purpose: String, isEmergency: Boolean, initialBal: Double, ret: Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("Emergency Fund") }
    var isEmergency by remember { mutableStateOf(true) }
    var initialBalText by remember { mutableStateOf("") }
    var returnRateText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Savings Vault Account", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account / Bank Name") },
                    placeholder = { Text("e.g. Maybank SavePlus, Versa Cash, ASB") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Purpose: Emergency Fund", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isEmergency,
                        onCheckedChange = {
                            isEmergency = it
                            purpose = if (it) "Emergency Fund" else "Other Savings Goals"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = initialBalText,
                    onValueChange = { initialBalText = it },
                    label = { Text("Initial Balance (MYR)") },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = returnRateText,
                    onValueChange = { returnRateText = it },
                    label = { Text("Expected Return % p.a. (Optional Estimate)") },
                    placeholder = { Text("e.g. 3.5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
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
                    if (name.isBlank()) {
                        error = "Account name is required"
                    } else {
                        val bal = initialBalText.toDoubleOrNull() ?: 0.0
                        val ret = returnRateText.toDoubleOrNull()
                        onConfirm(name.trim(), purpose, isEmergency, bal, ret)
                    }
                }
            ) {
                Text("Create Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DepositDialog(
    account: SavingsAccountEntity,
    everydayCash: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, fromEverydayCash: Boolean, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var fromEveryday by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deposit to ${account.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Deposit Amount (MYR)") },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transfer from Everyday Cash", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Available bank cash: ${DateUtils.formatCurrency(everydayCash)}. (Internal transfer; not an expense).",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(checked = fromEveryday, onCheckedChange = { fromEveryday = it })
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional Note") },
                    placeholder = { Text("e.g. November monthly savings allocation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                    if (amount == null || amount <= 0.0) {
                        error = "Enter a valid deposit amount"
                    } else if (fromEveryday && amount > everydayCash) {
                        error = "Insufficient Everyday Cash (${DateUtils.formatCurrency(everydayCash)})"
                    } else {
                        onConfirm(amount, fromEveryday, note)
                    }
                }
            ) {
                Text("Confirm Deposit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WithdrawDialog(
    account: SavingsAccountEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, toEverydayCash: Boolean, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var toEveryday by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw from ${account.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Current balance: ${DateUtils.formatCurrency(account.currentBalance)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Withdrawal Amount (MYR)") },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Return to Everyday Cash", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Adds to everyday bank balance.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(checked = toEveryday, onCheckedChange = { toEveryday = it })
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional Reason / Note") },
                    placeholder = { Text("e.g. Emergency car battery replacement") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                    if (amount == null || amount <= 0.0) {
                        error = "Enter a valid withdrawal amount"
                    } else if (amount > account.currentBalance) {
                        error = "Cannot withdraw more than balance (${DateUtils.formatCurrency(account.currentBalance)})"
                    } else {
                        onConfirm(amount, toEveryday, note)
                    }
                }
            ) {
                Text("Confirm Withdrawal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreditInterestDialog(
    account: SavingsAccountEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Credit Return / Dividend", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Record confirmed actual return or dividend paid into ${account.name}. This is verified growth, not an estimate.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Credited Amount (MYR)") },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Description / Note") },
                    placeholder = { Text("e.g. Monthly interest credited") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                    if (amount == null || amount <= 0.0) {
                        error = "Enter a valid return amount"
                    } else {
                        onConfirm(amount, note)
                    }
                }
            ) {
                Text("Record Growth")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
