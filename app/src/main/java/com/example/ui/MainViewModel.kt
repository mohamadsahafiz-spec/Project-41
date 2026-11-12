package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FinancialRepository
import com.example.data.MonthlyCommitmentItem
import com.example.data.MonthlySummaryData
import com.example.data.model.AchievementEntity
import com.example.data.model.DebtEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FinancialProfileEntity
import com.example.logic.FlowchartCalculator
import com.example.logic.FlowchartJourneyState
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = FinancialRepository(application)

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    val currentMonthKey = DateUtils.getCurrentMonthKey()
    private val _selectedMonthKey = MutableStateFlow(currentMonthKey)
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    // Quick Expense Dialog State
    private val _showQuickExpenseDialog = MutableStateFlow(false)
    val showQuickExpenseDialog: StateFlow<Boolean> = _showQuickExpenseDialog.asStateFlow()

    private val _monthlySummary = MutableStateFlow<MonthlySummaryData?>(null)
    val monthlySummary: StateFlow<MonthlySummaryData?> = _monthlySummary.asStateFlow()

    val profile: StateFlow<FinancialProfileEntity> = repository.profileFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FinancialProfileEntity()
        ).let { flow ->
            // Map null to default entity
            MutableStateFlow(FinancialProfileEntity()).apply {
                viewModelScope.launch {
                    flow.collect { value = it ?: FinancialProfileEntity() }
                }
            }
        }

    val debts: StateFlow<List<DebtEntity>> = repository.debtsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journeyState: StateFlow<FlowchartJourneyState> = repository.journeyStateFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FlowchartCalculator.calculateJourney(FinancialProfileEntity(), emptyList())
        )

    val achievements: StateFlow<List<AchievementEntity>> = repository.achievementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestAchievement: StateFlow<AchievementEntity?> = repository.latestUnlockedAchievementFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMonthExpenses: StateFlow<List<ExpenseEntity>> = _selectedMonthKey
        .flatMapLatest { key -> repository.getExpensesForMonth(key) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commitmentItems: StateFlow<List<MonthlyCommitmentItem>> = _selectedMonthKey
        .flatMapLatest { key -> repository.getCommitmentItemsForMonth(key) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureCheckInsForMonth(currentMonthKey)
            repository.triggerAchievementEvaluation()
            refreshMonthlySummary(currentMonthKey)
        }
    }

    fun openQuickExpenseDialog() {
        _showQuickExpenseDialog.value = true
    }

    fun closeQuickExpenseDialog() {
        _showQuickExpenseDialog.value = false
    }

    fun selectMonth(monthKey: String) {
        _selectedMonthKey.value = monthKey
        viewModelScope.launch {
            repository.ensureCheckInsForMonth(monthKey)
            refreshMonthlySummary(monthKey)
        }
    }

    fun refreshMonthlySummary(monthKey: String = _selectedMonthKey.value) {
        viewModelScope.launch {
            val summary = repository.getMonthlySummary(monthKey)
            _monthlySummary.value = summary
        }
    }

    fun addExpense(amount: Double, category: String, description: String, timestamp: Long) {
        viewModelScope.launch {
            repository.addExpense(amount, category, description, timestamp)
            _showQuickExpenseDialog.value = false
            refreshMonthlySummary(_selectedMonthKey.value)
            _eventFlow.emit(UiEvent.ShowSnackbar("Recorded ${DateUtils.formatCurrency(amount)} in $category"))
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            refreshMonthlySummary(_selectedMonthKey.value)
            _eventFlow.emit(UiEvent.ShowSnackbar("Deleted expense entry"))
        }
    }

    fun toggleCommitment(item: MonthlyCommitmentItem) {
        viewModelScope.launch {
            val month = _selectedMonthKey.value
            if (item.isPaid) {
                repository.unmarkCommitmentPaid(item, month)
                _eventFlow.emit(UiEvent.ShowSnackbar("Marked '${item.name}' unpaid"))
            } else {
                repository.markCommitmentPaid(item, month, item.defaultAmount)
                _eventFlow.emit(UiEvent.ShowSnackbar("Paid '${item.name}' (${DateUtils.formatCurrency(item.defaultAmount)})"))
            }
            refreshMonthlySummary(month)
        }
    }

    fun payCustomCommitment(item: MonthlyCommitmentItem, customAmount: Double) {
        viewModelScope.launch {
            val month = _selectedMonthKey.value
            repository.markCommitmentPaid(item, month, customAmount)
            refreshMonthlySummary(month)
            _eventFlow.emit(UiEvent.ShowSnackbar("Recorded ${DateUtils.formatCurrency(customAmount)} paid for '${item.name}'"))
        }
    }

    fun updateRecurringDefault(item: MonthlyCommitmentItem, newDefault: Double) {
        viewModelScope.launch {
            repository.updateRecurringDefault(item.commitmentId, newDefault)
            refreshMonthlySummary(_selectedMonthKey.value)
            _eventFlow.emit(UiEvent.ShowSnackbar("Updated monthly recurring default to ${DateUtils.formatCurrency(newDefault)}"))
        }
    }

    fun updateProfile(updated: FinancialProfileEntity) {
        viewModelScope.launch {
            repository.updateProfile(updated)
            refreshMonthlySummary(_selectedMonthKey.value)
            _eventFlow.emit(UiEvent.ShowSnackbar("Financial profile updated"))
        }
    }

    fun updateDebtBalance(key: String, newBalance: Double) {
        viewModelScope.launch {
            repository.updateDebtBalance(key, newBalance)
            refreshMonthlySummary(_selectedMonthKey.value)
            _eventFlow.emit(UiEvent.ShowSnackbar("Debt balance updated"))
        }
    }

    fun exportBackup(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataAsJson()
            onExported(json)
        }
    }

    fun importBackup(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreDataFromJson(json)
            if (success) {
                refreshMonthlySummary(_selectedMonthKey.value)
                _eventFlow.emit(UiEvent.ShowSnackbar("Data successfully restored!"))
            } else {
                _eventFlow.emit(UiEvent.ShowSnackbar("Failed to restore data. Invalid JSON format."))
            }
            onComplete(success)
        }
    }
}
