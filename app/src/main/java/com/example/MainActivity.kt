package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.ExpenseWidgetProvider

enum class ScreenTab(val title: String) {
    DASHBOARD("Freedom Tracker"),
    COMMITMENTS("Monthly Commitments"),
    EXPENSES("Quick Expenses"),
    JOURNEY("Financial Journey"),
    REVIEW("Monthly Review"),
    ACHIEVEMENTS("Milestones"),
    PROFILE("Profile & Backup")
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
        if (intent?.action == ExpenseWidgetProvider.ACTION_ADD_EXPENSE) {
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
                    Text(
                        text = currentTab.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                actions = {
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
                    label = { Text("Home", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_home")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.COMMITMENTS,
                    onClick = { currentTab = ScreenTab.COMMITMENTS },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Check-In") },
                    label = { Text("Check-In", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_commitments")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.EXPENSES,
                    onClick = { currentTab = ScreenTab.EXPENSES },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Expenses") },
                    label = { Text("Expenses", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_expenses")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.JOURNEY,
                    onClick = { currentTab = ScreenTab.JOURNEY },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = "Flowchart") },
                    label = { Text("Flowchart", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_journey")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.REVIEW,
                    onClick = { currentTab = ScreenTab.REVIEW },
                    icon = { Icon(Icons.Default.Insights, contentDescription = "Review") },
                    label = { Text("Review", fontSize = 11.sp) },
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
                        onNavigateToReview = { currentTab = ScreenTab.REVIEW },
                        onNavigateToJourney = { currentTab = ScreenTab.JOURNEY },
                        onNavigateToAchievements = { currentTab = ScreenTab.ACHIEVEMENTS }
                    )
                }
                ScreenTab.COMMITMENTS -> {
                    CommitmentsScreen(
                        monthKey = selectedMonthKey,
                        items = commitmentItems,
                        onToggleCommitment = { item -> viewModel.toggleCommitment(item) },
                        onPayCustomAmount = { item, amt -> viewModel.payCustomCommitment(item, amt) },
                        onUpdateRecurringDefault = { item, def -> viewModel.updateRecurringDefault(item, def) }
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
                ScreenTab.JOURNEY -> {
                    FinancialJourneyScreen(journeyState = journeyState)
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
