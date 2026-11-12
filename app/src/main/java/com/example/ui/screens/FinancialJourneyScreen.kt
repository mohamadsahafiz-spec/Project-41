package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.FlowchartCalculator
import com.example.logic.FlowchartJourneyState
import com.example.logic.FlowchartStepInfo
import com.example.logic.StepStatus
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningGold
import com.example.util.DateUtils

@Composable
fun FinancialJourneyScreen(
    journeyState: FlowchartJourneyState,
    onConfirmStepVerification: (stepNumber: Int, isConfirmed: Boolean, notes: String) -> Unit
) {
    val context = LocalContext.current
    var verifyingStepNumber by remember { mutableStateOf<Int?>(null) }
    var showRationaleDialog by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("financial_journey_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Governing Strategy Attribution Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(FlowchartCalculator.SOURCE_URL))
                        context.startActivity(browserIntent)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
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
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PROJECT 41 • GOVERNING STRATEGY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View Source",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = FlowchartCalculator.FLOWCHART_VERSION,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Attribution: Based on the UK Personal Finance Flowchart (ukpersonal.finance/flowchart). Conditions adapt UK instruments to Malaysian equivalents (KWSP, ASB, Tabung Haji) while strictly preserving original decision branches. Steps cannot be unlocked without meeting verified conditions.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Active Mission Hero Banner
        item {
            val mission = journeyState.activeMission
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CURRENT ACTIVE MISSION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Text(
                            text = "${journeyState.overallJourneyProgressPercent.toInt()}% Total Progress",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = mission.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mission.objective,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target: ${DateUtils.formatCurrency(mission.targetAmount)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Remaining: ${DateUtils.formatCurrency(mission.remainingAmount)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { mission.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Estimated Completion: ${mission.estimatedCompletionDate}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { showRationaleDialog = mission.calculationRationale }
                            )
                        }
                    }
                }
            }
        }

        // Flowchart Steps
        items(journeyState.steps, key = { it.stepNumber }) { step ->
            FlowchartStepCard(
                step = step,
                onVerifyClick = { verifyingStepNumber = step.stepNumber }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Step Verification Dialog (Step 0 & Step 2)
    verifyingStepNumber?.let { stepNum ->
        StepVerificationDialog(
            stepNumber = stepNum,
            onDismiss = { verifyingStepNumber = null },
            onConfirm = { isConfirmed, notes ->
                onConfirmStepVerification(stepNum, isConfirmed, notes)
                verifyingStepNumber = null
            }
        )
    }

    // Calculation Rationale Dialog
    showRationaleDialog?.let { rationale ->
        AlertDialog(
            onDismissRequest = { showRationaleDialog = null },
            title = { Text("Estimation Calculation Rationale", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = rationale, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Project 41 calculates estimates strictly from your verified net cashflow surplus, without fabricating dates.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRationaleDialog = null }) { Text("Got it") }
            }
        )
    }
}

@Composable
fun FlowchartStepCard(
    step: FlowchartStepInfo,
    onVerifyClick: () -> Unit
) {
    val statusColor = when (step.status) {
        StepStatus.COMPLETED -> SuccessGreen
        StepStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        StepStatus.LOCKED -> MaterialTheme.colorScheme.outline
    }

    val statusText = when (step.status) {
        StepStatus.COMPLETED -> "COMPLETED"
        StepStatus.ACTIVE -> "CURRENT FOCUS"
        StepStatus.LOCKED -> "LOCKED"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flowchart_step_${step.stepNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = when (step.status) {
                StepStatus.ACTIVE -> MaterialTheme.colorScheme.surface
                StepStatus.COMPLETED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                StepStatus.LOCKED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (step.status == StepStatus.ACTIVE) 3.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (step.status) {
                            StepStatus.COMPLETED -> Icons.Default.CheckCircle
                            StepStatus.ACTIVE -> Icons.Default.RadioButtonChecked
                            StepStatus.LOCKED -> Icons.Default.Lock
                        },
                        contentDescription = statusText,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STEP ${step.stepNumber}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = statusColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Step Progress Bar
            if (step.status != StepStatus.LOCKED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Verified Progress: ${DateUtils.formatCurrency(step.currentAmount)} / ${DateUtils.formatCurrency(step.targetAmount)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "${step.progressPercent.toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { step.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Malaysian Adaptation Box
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MALAYSIAN ADAPTATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = step.malaysianAdaptationNote,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Action Tip or Verification Action
            if (step.stepNumber == 0 || step.stepNumber == 2) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onVerifyClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (step.status == StepStatus.COMPLETED) "Review Step ${step.stepNumber} Verification" else "Review & Confirm Step ${step.stepNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (step.status == StepStatus.ACTIVE) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = WarningGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "👉 Next Action: ${step.clearNextAction}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StepVerificationDialog(
    stepNumber: Int,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String) -> Unit
) {
    var check1 by remember { mutableStateOf(false) }
    var check2 by remember { mutableStateOf(false) }
    var check3 by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (stepNumber == 0) "Step 0 Verification: Budget & Essentials" else "Step 2 Verification: Workplace Match / KWSP",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (stepNumber == 0) {
                    Text(
                        "According to the UK Personal Finance Flowchart v3.0.10, Step 0 requires active budgeting and direct debits for essential commitments:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    VerificationCheckRow(
                        checked = check1,
                        text = "I have established a realistic monthly living budget",
                        onChecked = { check1 = it }
                    )
                    VerificationCheckRow(
                        checked = check2,
                        text = "All essential commitments (Rent, Childcare, Utilities, Family) are tracked",
                        onChecked = { check2 = it }
                    )
                    VerificationCheckRow(
                        checked = check3,
                        text = "I review and confirm recurring monthly commitments in Project 41",
                        onChecked = { check3 = it }
                    )
                } else {
                    Text(
                        "Step 2 verifies maximizing mandatory retirement contributions. In Malaysia, statutory employer EPF (13%) is deducted by your employer:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    VerificationCheckRow(
                        checked = check1,
                        text = "I have confirmed employer statutory EPF deduction on my payslip",
                        onChecked = { check1 = it }
                    )
                    VerificationCheckRow(
                        checked = check2,
                        text = "My EPF Account 1, 2, and 3 balances are tracked in Project 41",
                        onChecked = { check2 = it }
                    )
                    check3 = true
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isComplete = check1 && check2 && check3
                    onConfirm(isComplete, if (isComplete) "Confirmed by user" else "Pending requirements")
                }
            ) {
                Text("Confirm Status")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VerificationCheckRow(
    checked: Boolean,
    text: String,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = SuccessGreen)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
