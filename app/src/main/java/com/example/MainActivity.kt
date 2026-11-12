package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.UiEvent
import com.example.ui.components.QuickExpenseDialog
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.CommitmentsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.FinancialJourneyScreen
import com.example.ui.screens.MonthlyReviewScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.SavingsVaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.ExpenseWidgetProvider

enum class ScreenTab(val title: String) {
    DASHBOARD("Project 41"),
    COMMITMENTS("Monthly Check-In"),
    VAULT("My Savings Vault"),
    JOURNEY("Flowchart Mission"),
    EXPENSES("Quick Expenses"),
    REVIEW("Monthly Review"),
    ACHIEVEMENTS("Milestones"),
    PROFILE("Profile & Settings")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleWidgetIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent?.action == ExpenseWidgetProvider.ACTION_ADD_EXPENSE ||
            intent?.getBooleanExtra("OPEN_EXPENSE_DIALOG", false) == true
        ) {
            viewModel.openQuickExpenseDialog()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect UI state
    val journeyState by viewModel.journeyState.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val latestAchievement by viewModel.latestAchievement.collectAsStateWithLifecycle()
    val currentMonthExpenses by viewModel.currentMonthExpenses.collectAsStateWithLifecycle()
    val commitmentItems by viewModel.commitmentItems.collectAsStateWithLifecycle()
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val showQuickExpenseDialog by viewModel.showQuickExpenseDialog.collectAsStateWithLifecycle()

    // Savings Vault states
    val savingsAccounts by viewModel.savingsAccounts.collectAsStateWithLifecycle()
    val totalDedicatedSavings by viewModel.totalDedicatedSavings.collectAsStateWithLifecycle()
    val totalEmergencyFund by viewModel.totalEmergencyFund.collectAsStateWithLifecycle()
    val totalInterestCredited by viewModel.totalInterestCredited.collectAsStateWithLifecycle()
    val currentMonthIncome by viewModel.currentMonthIncome.collectAsStateWithLifecycle()

    val totalSpendingThisMonth = remember(currentMonthExpenses) {
        currentMonthExpenses.sumOf { it.amount }
    }

    // Handle back button on sub-screens to return to Dashboard
    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        currentTab = ScreenTab.DASHBOARD
    }

    // Event listener for snackbars
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTab.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        if (currentTab == ScreenTab.DASHBOARD) {
                            Text(
                                text = "Your Journey. Your Rules.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { currentTab = ScreenTab.EXPENSES },
                        modifier = Modifier.testTag("nav_to_expenses_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Expenses",
                            tint = if (currentTab == ScreenTab.EXPENSES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { currentTab = ScreenTab.ACHIEVEMENTS },
                        modifier = Modifier.testTag("nav_to_achievements_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Milestones",
                            tint = if (currentTab == ScreenTab.ACHIEVEMENTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { currentTab = ScreenTab.PROFILE },
                        modifier = Modifier.testTag("nav_to_profile_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Profile & Settings",
                            tint = if (currentTab == ScreenTab.PROFILE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == ScreenTab.DASHBOARD,
                    onClick = { currentTab = ScreenTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_home")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.JOURNEY,
                    onClick = { currentTab = ScreenTab.JOURNEY },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = "Mission") },
                    label = { Text("Mission", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_journey")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.COMMITMENTS,
                    onClick = { currentTab = ScreenTab.COMMITMENTS },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Check-In") },
                    label = { Text("Check-In", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_commitments")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.VAULT,
                    onClick = { currentTab = ScreenTab.VAULT },
                    icon = { Icon(Icons.Default.Savings, contentDescription = "Vault") },
                    label = { Text("Vault", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_vault")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.REVIEW,
                    onClick = { currentTab = ScreenTab.REVIEW },
                    icon = { Icon(Icons.Default.Insights, contentDescription = "Review") },
                    label = { Text("Review", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_review")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.DASHBOARD -> {
                    DashboardScreen(
                        journeyState = journeyState,
                        profile = profile,
                        monthlySpending = totalSpendingThisMonth,
                        latestAchievement = latestAchievement,
                        onAddExpenseClick = { viewModel.openQuickExpenseDialog() },
                        onNavigateToCheckIn = { currentTab = ScreenTab.COMMITMENTS },
                        onNavigateToVault = { currentTab = ScreenTab.VAULT },
                        onNavigateToReview = { currentTab = ScreenTab.REVIEW },
                        onNavigateToJourney = { currentTab = ScreenTab.JOURNEY },
                        onNavigateToAchievements = { currentTab = ScreenTab.ACHIEVEMENTS }
                    )
                }
                ScreenTab.JOURNEY -> {
                    FinancialJourneyScreen(
                        journeyState = journeyState,
                        onConfirmStepVerification = { stepNum, isConfirmed, notes ->
                            viewModel.confirmStepVerification(stepNum, isConfirmed, notes)
                        }
                    )
                }
                ScreenTab.COMMITMENTS -> {
                    CommitmentsScreen(
                        monthKey = selectedMonthKey,
                        items = commitmentItems,
                        currentIncome = currentMonthIncome,
                        onSaveIncome = { income -> viewModel.saveMonthlyIncome(income) },
                        onConfirmBatchPayments = { payments -> viewModel.confirmBatchPayments(payments) },
                        onUnmarkCommitment = { item -> viewModel.unmarkCommitment(item) },
                        onUpdateRecurringDefault = { item, def -> viewModel.updateRecurringDefault(item, def) }
                    )
                }
                ScreenTab.VAULT -> {
                    SavingsVaultScreen(
                        profile = profile,
                        accounts = savingsAccounts,
                        totalEmergencyFund = totalEmergencyFund,
                        totalDedicatedSavings = totalDedicatedSavings,
                        totalInterestCredited = totalInterestCredited,
                        onAddAccount = { name, purpose, isEmergency, initialBal, ret ->
                            viewModel.addSavingsAccount(name, purpose, isEmergency, initialBal, ret)
                        },
                        onDeposit = { accountId, amount, fromEveryday, note ->
                            viewModel.depositToSavings(accountId, amount, fromEveryday, note)
                        },
                        onWithdraw = { accountId, amount, toEveryday, note ->
                            viewModel.withdrawFromSavings(accountId, amount, toEveryday, note)
                        },
                        onCreditInterest = { accountId, amount, note ->
                            viewModel.creditInterestToSavings(accountId, amount, note)
                        }
                    )
                }
                ScreenTab.EXPENSES -> {
                    ExpensesScreen(
                        currentMonthKey = viewModel.currentMonthKey,
                        selectedMonthKey = selectedMonthKey,
                        expenses = currentMonthExpenses,
                        onSelectMonth = { key -> viewModel.selectMonth(key) },
                        onAddExpenseClick = { viewModel.openQuickExpenseDialog() },
                        onDeleteExpense = { exp -> viewModel.deleteExpense(exp) }
                    )
                }
                ScreenTab.REVIEW -> {
                    MonthlyReviewScreen(
                        summary = monthlySummary,
                        profile = profile,
                        selectedMonthKey = selectedMonthKey,
                        onSelectMonth = { key -> viewModel.selectMonth(key) },
                        onUpdateProfile = { updated -> viewModel.updateProfile(updated) }
                    )
                }
                ScreenTab.ACHIEVEMENTS -> {
                    AchievementsScreen(achievements = achievements)
                }
                ScreenTab.PROFILE -> {
                    ProfileSettingsScreen(
                        profile = profile,
                        debts = debts,
                        onUpdateProfile = { updated -> viewModel.updateProfile(updated) },
                        onUpdateDebt = { key, bal -> viewModel.updateDebtBalance(key, bal) },
                        onExportBackup = { callback -> viewModel.exportBackup(callback) },
                        onImportBackup = { json, callback -> viewModel.importBackup(json, callback) }
                    )
                }
            }
        }
    }

    // Quick Expense entry dialog
    if (showQuickExpenseDialog) {
        QuickExpenseDialog(
            onDismiss = { viewModel.closeQuickExpenseDialog() },
            onSave = { amount, category, desc, timestamp ->
                viewModel.addExpense(amount, category, desc, timestamp)
            }
        )
    }
}
