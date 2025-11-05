package com.example.conti.ui.movimenti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conti.data.repository.AccountRepository
import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.models.Transaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel per la schermata Movimenti.
 * Gestisce il caricamento e filtraggio delle transazioni.
 *
 * ✅ VERSIONE AGGIORNATA:
 * - Considera il saldo iniziale dell'account nel calcolo del bilancio
 * - Il bilancio mostrato è il saldo corrente dell'account (saldo_iniziale + movimenti)
 */
class MovimentiViewModel : ViewModel() {

    private val firestoreRepository = FirestoreRepository()
    private val accountRepository = AccountRepository.getInstance()

    // State per la UI
    private val _uiState = MutableStateFlow<MovimentiUiState>(MovimentiUiState.Loading)
    val uiState: StateFlow<MovimentiUiState> = _uiState.asStateFlow()

    /**
     * Carica tutti i movimenti (senza filtro)
     * ✅ AGGIORNATO: calcola il saldo totale di tutti i conti
     */
    fun loadAllMovimenti() {
        viewModelScope.launch {
            // Combina i flow di transazioni e accounts
            combine(
                firestoreRepository.getAllTransactions(),
                accountRepository.getAccountsFlow()
            ) { transactions, accounts ->
                Pair(transactions, accounts)
            }
                .catch { exception ->
                    android.util.Log.e("MovimentiViewModel", "Errore caricamento movimenti", exception)
                    _uiState.value = MovimentiUiState.Error(exception.message ?: "Unknown error")
                }
                .collect { (transactions, accounts) ->
                    if (transactions.isEmpty()) {
                        _uiState.value = MovimentiUiState.Empty("Nessun movimento presente")
                    } else {
                        val stats = calculateStats(transactions)

                        // ✅ Calcola il saldo totale di tutti gli account
                        val saldoTotale = accounts.sumOf { it.balance }

                        _uiState.value = MovimentiUiState.Success(
                            transactions = transactions,
                            totalIncome = stats.first,
                            totalExpenses = stats.second,
                            currentBalance = saldoTotale,
                            accountName = "Tutti i conti"
                        )
                    }
                }
        }
    }

    /**
     * Carica i movimenti filtrati per un conto specifico
     * ✅ AGGIORNATO: usa il saldo corrente dell'account (che include già il saldo iniziale)
     */
    fun loadMovimentiByAccount(accountId: String) {
        viewModelScope.launch {
            // Combina i flow di transazioni e account specifico
            combine(
                firestoreRepository.getAllTransactions(accountId),
                accountRepository.getAccountsFlow()
            ) { transactions, accounts ->
                val account = accounts.find { it.accountId == accountId }
                Triple(transactions, account, accounts)
            }
                .catch { exception ->
                    android.util.Log.e("MovimentiViewModel", "Errore caricamento movimenti per account $accountId", exception)
                    _uiState.value = MovimentiUiState.Error(exception.message ?: "Unknown error")
                }
                .collect { (transactions, account, _) ->
                    if (account == null) {
                        _uiState.value = MovimentiUiState.Error("Account non trovato")
                        return@collect
                    }

                    if (transactions.isEmpty()) {
                        _uiState.value = MovimentiUiState.Empty("Nessun movimento per questo conto")
                    } else {
                        val stats = calculateStats(transactions)

                        // ✅ IMPORTANTE: Usa il balance corrente dell'account
                        // Questo valore include già: saldo_iniziale + tutte_le_transazioni
                        // Il balance viene aggiornato automaticamente in FirestoreRepository.updateBalanceAfterTransaction
                        _uiState.value = MovimentiUiState.Success(
                            transactions = transactions,
                            totalIncome = stats.first,
                            totalExpenses = stats.second,
                            currentBalance = account.balance,  // ✅ Saldo attuale (iniziale + movimenti)
                            accountName = account.accountName
                        )

                        android.util.Log.d("MovimentiViewModel", """
                            📊 Riepilogo per ${account.accountName}:
                               Entrate: €${stats.first}
                               Uscite: €${stats.second}
                               Saldo Corrente: €${account.balance}
                        """.trimIndent())
                    }
                }
        }
    }

    /**
     * Calcola statistiche: (entrate totali, uscite totali)
     * Nota: queste sono solo le somme delle transazioni, non includono il saldo iniziale
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
 * ✅ AGGIORNATO: include currentBalance (saldo corrente del conto)
 */
sealed class MovimentiUiState {
    object Loading : MovimentiUiState()
    data class Empty(val message: String) : MovimentiUiState()
    data class Success(
        val transactions: List<Transaction>,
        val totalIncome: Double,       // Somma delle entrate
        val totalExpenses: Double,     // Somma delle uscite
        val currentBalance: Double,    // ✅ NUOVO: Saldo attuale (iniziale + movimenti)
        val accountName: String        // ✅ NUOVO: Nome del conto
    ) : MovimentiUiState()
    data class Error(val message: String) : MovimentiUiState()
}