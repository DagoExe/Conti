package com.example.conti.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conti.data.repository.AccountRepository
import com.example.conti.models.Account
import com.example.conti.utils.CurrencyUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel per la schermata Home.
 * Gestisce il riepilogo totale, statistiche mensili e lista account.
 */
class HomeViewModel : ViewModel() {

    private val accountRepository = AccountRepository.getInstance()

    // State per la UI
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    /**
     * Carica tutti i dati per la Home
     */
    private fun loadHomeData() {
        viewModelScope.launch {
            accountRepository.getAccountsFlow()
                .catch { exception ->
                    android.util.Log.e("HomeViewModel", "Errore loadHomeData", exception)
                    _uiState.value = HomeUiState.Error(exception.message ?: "Unknown error")
                }
                .collect { accounts ->
                    if (accounts.isEmpty()) {
                        _uiState.value = HomeUiState.Empty
                    } else {
                        val summary = calculateSummary(accounts)
                        _uiState.value = HomeUiState.Success(
                            accounts = accounts,
                            totalBalance = summary.totalBalance,
                            monthlyIncome = summary.monthlyIncome,
                            monthlyExpenses = summary.monthlyExpenses,
                            activeSubscriptions = summary.activeSubscriptions,
                            subscriptionsCost = summary.subscriptionsCost
                        )
                    }
                }
        }
    }

    /**
     * Calcola il riepilogo dei dati
     */
    private fun calculateSummary(accounts: List<Account>): HomeSummary {
        val totalBalance = accounts.sumOf { it.balance }

        // TODO: Implementare calcolo entrate/uscite mensili quando avremo TransactionRepository
        val monthlyIncome = 0.0
        val monthlyExpenses = 0.0

        // TODO: Implementare calcolo abbonamenti quando avremo SubscriptionRepository
        val activeSubscriptions = 0
        val subscriptionsCost = 0.0

        return HomeSummary(
            totalBalance = totalBalance,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            activeSubscriptions = activeSubscriptions,
            subscriptionsCost = subscriptionsCost
        )
    }

    /**
     * Ricarica i dati
     */
    fun refresh() {
        loadHomeData()
    }
}

/**
 * Data class per il riepilogo Home
 */
data class HomeSummary(
    val totalBalance: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val activeSubscriptions: Int,
    val subscriptionsCost: Double
)

/**
 * Sealed class per gli stati della UI
 */
sealed class HomeUiState {
    object Loading : HomeUiState()
    object Empty : HomeUiState()
    data class Success(
        val accounts: List<Account>,
        val totalBalance: Double,
        val monthlyIncome: Double,
        val monthlyExpenses: Double,
        val activeSubscriptions: Int,
        val subscriptionsCost: Double
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}