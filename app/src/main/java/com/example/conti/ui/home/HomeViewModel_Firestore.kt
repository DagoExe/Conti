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
 * ViewModel per la schermata Home - Versione Firestore CORRETTA.
 *
 * ✅ FIXES:
 * - Type inference esplicito per la conversione da Movimento a Transaction
 * - Import corretto dell'entità Movimento
 */
class HomeViewModel(
    private val repository: FirestoreRepository
) : ViewModel() {

    // ========================================
    // DATI OSSERVABILI
    // ========================================

    /**
     * Lista di tutti gli account, osservabile dall'UI.
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

    data class MonthlyStats(
        val totalIncome: Double = 0.0,
        val totalExpenses: Double = 0.0,
        val balance: Double = 0.0,
        val transactionCount: Int = 0
    )

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

    val totalBalance: Flow<Double> = repository.getAllAccounts().map { accounts ->
        accounts.sumOf { it.balance }
    }

    // ========================================
    // OPERAZIONI ACCOUNT
    // ========================================

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
     * ✅ CORRETTO: Importa transazioni da Excel con type inference esplicito
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

                // ✅ FIX: Type inference esplicito per evitare errori di compilazione
                val transactions = result.movimenti.map { movimento: com.example.conti.data.database.entities.Movimento ->
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