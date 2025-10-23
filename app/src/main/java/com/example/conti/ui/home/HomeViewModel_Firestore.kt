package com.example.conti.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.models.Account
import com.example.conti.models.Transaction
import com.example.conti.models.Subscription
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel per la schermata Home - Versione Firestore.
 *
 * Responsabilità:
 * - Fornire i dati degli account dall'UI
 * - Gestire operazioni CRUD su Firestore
 * - Calcolare statistiche
 *
 * Il ViewModel sopravvive ai cambiamenti di configurazione (rotazione schermo, ecc.)
 * e mantiene i dati in modo reattivo tramite Flow.
 */
class HomeViewModel(
    private val repository: FirestoreRepository
) : ViewModel() {

    // ========================================
    // DATI OSSERVABILI
    // ========================================

    /**
     * Lista di tutti gli account, osservabile dall'UI.
     * Si aggiorna automaticamente quando i dati in Firestore cambiano.
     */
    val accounts = repository.getAllAccounts().asLiveData()

    /**
     * Transazioni del mese corrente.
     */
    val currentMonthTransactions = repository.getTransactionsByDateRange(
        startDate = FirestoreRepository.getStartOfCurrentMonth(),
        endDate = FirestoreRepository.getEndOfCurrentMonth()
    ).asLiveData()

    /**
     * Abbonamenti attivi.
     */
    val activeSubscriptions = repository.getAllSubscriptions(activeOnly = true).asLiveData()

    // ========================================
    // STATISTICHE CALCOLATE
    // ========================================

    /**
     * Statistiche del mese corrente (entrate, uscite, bilancio).
     */
    data class MonthlyStats(
        val totalIncome: Double = 0.0,
        val totalExpenses: Double = 0.0,
        val balance: Double = 0.0,
        val transactionCount: Int = 0
    )

    /**
     * Flow delle statistiche mensili.
     */
    val monthlyStats: Flow<MonthlyStats> = repository.getTransactionsByDateRange(
        startDate = FirestoreRepository.getStartOfCurrentMonth(),
        endDate = FirestoreRepository.getEndOfCurrentMonth()
    ).map { transactions ->
        val income = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val expenses = transactions.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }

        MonthlyStats(
            totalIncome = income,
            totalExpenses = expenses,
            balance = income - expenses,
            transactionCount = transactions.size
        )
    }

    /**
     * Saldo totale di tutti gli account.
     */
    val totalBalance: Flow<Double> = repository.getAllAccounts().map { accounts ->
        accounts.sumOf { it.balance }
    }

    // ========================================
    // OPERAZIONI ACCOUNT
    // ========================================

    /**
     * Crea un nuovo account.
     */
    fun createAccount(
        account: Account,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.createAccount(account)
                .onSuccess { accountId ->
                    android.util.Log.d("HomeViewModel", "✅ Account creato: $accountId")
                    onSuccess(accountId)
                }
                .onFailure { error ->
                    android.util.Log.e("HomeViewModel", "❌ Errore creazione account", error)
                    onError(error.message ?: "Errore sconosciuto")
                }
        }
    }

    /**
     * Aggiorna un account esistente.
     */
    fun updateAccount(
        account: Account,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateAccount(account)
                .onSuccess {
                    android.util.Log.d("HomeViewModel", "✅ Account aggiornato")
                    onSuccess()
                }
                .onFailure { error ->
                    android.util.Log.e("HomeViewModel", "❌ Errore aggiornamento account", error)
                    onError(error.message ?: "Errore sconosciuto")
                }
        }
    }

    /**
     * Elimina un account.
     */
    fun deleteAccount(
        accountId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
                .onSuccess {
                    android.util.Log.d("HomeViewModel", "✅ Account eliminato")
                    onSuccess()
                }
                .onFailure { error ->
                    android.util.Log.e("HomeViewModel", "❌ Errore eliminazione account", error)
                    onError(error.message ?: "Errore sconosciuto")
                }
        }
    }

    // ========================================
    // OPERAZIONI TRANSAZIONI
    // ========================================

    /**
     * Aggiunge una singola transazione.
     */
    fun addTransaction(
        transaction: Transaction,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
                .onSuccess { transactionId ->
                    android.util.Log.d("HomeViewModel", "✅ Transazione aggiunta: $transactionId")
                    onSuccess(transactionId)
                }
                .onFailure { error ->
                    android.util.Log.e("HomeViewModel", "❌ Errore aggiunta transazione", error)
                    onError(error.message ?: "Errore sconosciuto")
                }
        }
    }

    /**
     * Importa transazioni da Excel (batch).
     */
    fun importTransactionsFromExcel(
        accountId: String,
        filePath: String,
        onSuccess: (Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "📊 Importazione da Excel: $filePath")

                // Leggi file Excel
                val excelReader = com.example.conti.data.excel.ExcelReader()
                val result = excelReader.leggiExcel(filePath, accountId.toLongOrNull() ?: 0)

                if (result.errori.isNotEmpty()) {
                    val errorMessage = "Errori lettura Excel:\n${result.errori.joinToString("\n")}"
                    android.util.Log.e("HomeViewModel", errorMessage)
                    onError(errorMessage)
                    return@launch
                }

                if (result.movimenti.isEmpty()) {
                    android.util.Log.w("HomeViewModel", "⚠️ Nessun movimento trovato")
                    onError("Nessun movimento trovato nel file")
                    return@launch
                }

                // Converti i movimenti Room in Transaction Firestore
                val transactions = result.movimenti.map { movimento ->
                    Transaction(
                        accountId = accountId,
                        amount = movimento.importo,
                        description = movimento.descrizione,
                        category = movimento.categoria,
                        notes = movimento.note,
                        date = Timestamp(java.util.Date.from(
                            movimento.data.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                        )),
                        type = if (movimento.importo >= 0) "income" else "expense",
                        isRecurring = movimento.isRicorrente,
                        subscriptionId = movimento.idAbbonamento?.toString()
                    )
                }

                android.util.Log.d("HomeViewModel", "📤 Invio ${transactions.size} transazioni a Firestore")

                // Salva in batch
                repository.addTransactionsBatch(transactions)
                    .onSuccess { count ->
                        android.util.Log.d("HomeViewModel", "✅ Importate $count transazioni")
                        onSuccess(count)
                    }
                    .onFailure { error ->
                        android.util.Log.e("HomeViewModel", "❌ Errore import batch", error)
                        onError(error.message ?: "Errore import")
                    }

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ Errore import Excel", e)
                onError(e.message ?: "Errore sconosciuto")
            }
        }
    }

    /**
     * Elimina una transazione.
     */
    fun deleteTransaction(
        transactionId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
                .onSuccess {
                    android.util.Log.d("HomeViewModel", "✅ Transazione eliminata")
                    onSuccess()
                }
                .onFailure { error ->
                    android.util.Log.e("HomeViewModel", "❌ Errore eliminazione transazione", error)
                    onError(error.message ?: "Errore sconosciuto")
                }
        }
    }

    // ========================================
    // OPERAZIONI ABBONAMENTI
    // ========================================

    /**
     * Crea un nuovo abbonamento.
     */
    fun createSubscription(
        subscription: Subscription,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.createSubscription(subscription)
                .onSuccess { subscriptionId ->
                    android.util.Log.d("HomeViewModel", "✅ Abbonamento creato: $subscriptionId")
                    onSuccess(subscriptionId)
                }
                .onFailure { error ->
                    android.util.Log.e("HomeViewModel", "❌ Errore creazione abbonamento", error)
                    onError(error.message ?: "Errore sconosciuto")
                }
        }
    }

    /**
     * Ottiene il costo mensile totale degli abbonamenti.
     */
    fun getTotalMonthlyCost(
        onSuccess: (Double) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.getTotalMonthlySubscriptionCost()
                .onSuccess { total ->
                    onSuccess(total)
                }
                .onFailure { error ->
                    android.util.Log.e("HomeViewModel", "❌ Errore calcolo costo", error)
                    onError(error.message ?: "Errore sconosciuto")
                }
        }
    }
}