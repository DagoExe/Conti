package com.example.conti.ui.movimenti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.models.Transaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel per la schermata Movimenti.
 * Gestisce il caricamento e filtraggio delle transazioni.
 */
class MovimentiViewModel : ViewModel() {

    private val firestoreRepository = FirestoreRepository()

    // State per la UI
    private val _uiState = MutableStateFlow<MovimentiUiState>(MovimentiUiState.Loading)
    val uiState: StateFlow<MovimentiUiState> = _uiState.asStateFlow()

    /**
     * Carica tutti i movimenti (senza filtro)
     */
    fun loadAllMovimenti() {
        viewModelScope.launch {
            firestoreRepository.getAllTransactions()
                .catch { exception ->
                    android.util.Log.e("MovimentiViewModel", "Errore caricamento movimenti", exception)
                    _uiState.value = MovimentiUiState.Error(exception.message ?: "Unknown error")
                }
                .collect { transactions ->
                    if (transactions.isEmpty()) {
                        _uiState.value = MovimentiUiState.Empty("Nessun movimento presente")
                    } else {
                        val stats = calculateStats(transactions)
                        _uiState.value = MovimentiUiState.Success(
                            transactions = transactions,
                            totalIncome = stats.first,
                            totalExpenses = stats.second
                        )
                    }
                }
        }
    }

    /**
     * Carica i movimenti filtrati per un conto specifico
     */
    fun loadMovimentiByAccount(accountId: String) {
        viewModelScope.launch {
            firestoreRepository.getAllTransactions(accountId)
                .catch { exception ->
                    android.util.Log.e("MovimentiViewModel", "Errore caricamento movimenti per account $accountId", exception)
                    _uiState.value = MovimentiUiState.Error(exception.message ?: "Unknown error")
                }
                .collect { transactions ->
                    if (transactions.isEmpty()) {
                        _uiState.value = MovimentiUiState.Empty("Nessun movimento per questo conto")
                    } else {
                        val stats = calculateStats(transactions)
                        _uiState.value = MovimentiUiState.Success(
                            transactions = transactions,
                            totalIncome = stats.first,
                            totalExpenses = stats.second
                        )
                    }
                }
        }
    }

    /**
     * Calcola statistiche: (entrate totali, uscite totali)
     */
    private fun calculateStats(transactions: List<Transaction>): Pair<Double, Double> {
        var income = 0.0
        var expenses = 0.0

        transactions.forEach { transaction ->
            if (transaction.amount >= 0) {
                income += transaction.amount
            } else {
                expenses += kotlin.math.abs(transaction.amount)
            }
        }

        return Pair(income, expenses)
    }

    /**
     * Ricarica i dati
     */
    fun refresh(accountId: String? = null) {
        if (accountId != null) {
            loadMovimentiByAccount(accountId)
        } else {
            loadAllMovimenti()
        }
    }
}

/**
 * Sealed class per gli stati della UI
 */
sealed class MovimentiUiState {
    object Loading : MovimentiUiState()
    data class Empty(val message: String) : MovimentiUiState()
    data class Success(
        val transactions: List<Transaction>,
        val totalIncome: Double,
        val totalExpenses: Double
    ) : MovimentiUiState()
    data class Error(val message: String) : MovimentiUiState()
}