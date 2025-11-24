package com.example.conti.ui.abbonamenti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conti.data.repository.AccountRepository
import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.models.Account
import com.example.conti.models.Subscription
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel per la gestione degli abbonamenti.
 *
 * ✅ VERSIONE CORRETTA - Sostituisce la versione con encoding issues
 *
 * Features:
 * - Caricamento abbonamenti attivi/inattivi
 * - Calcolo costi mensili e annuali totali
 * - Gestione CRUD abbonamenti
 * - Calcolo date di rinnovo
 */
class AbbonamentiViewModel : ViewModel() {

    private val firestoreRepository = FirestoreRepository()
    private val accountRepository = AccountRepository.getInstance()

    // State per la UI
    private val _uiState = MutableStateFlow<AbbonamentiUiState>(AbbonamentiUiState.Loading)
    val uiState: StateFlow<AbbonamentiUiState> = _uiState.asStateFlow()

    // Filtro attivo/inattivo
    private val _showActiveOnly = MutableStateFlow(true)
    val showActiveOnly: StateFlow<Boolean> = _showActiveOnly.asStateFlow()

    init {
        android.util.Log.d(TAG, "===========================================")
        android.util.Log.d(TAG, "  ABBONAMENTI VIEWMODEL INIZIALIZZATO")
        android.util.Log.d(TAG, "===========================================")
        loadSubscriptions()
    }

    /**
     * Carica gli abbonamenti con filtro attivo/inattivo
     */
    private fun loadSubscriptions() {
        android.util.Log.d(TAG, "🔄 loadSubscriptions() chiamato")

        viewModelScope.launch {
            _showActiveOnly
                .flatMapLatest { showActive ->
                    android.util.Log.d(TAG, "🔍 Filtro cambiato: showActive = $showActive")

                    combine(
                        firestoreRepository.getAllSubscriptions(showActive),
                        accountRepository.getAccountsFlow()
                    ) { subscriptions, accounts ->
                        android.util.Log.d(TAG, "📊 Dati ricevuti:")
                        android.util.Log.d(TAG, "   - Abbonamenti: ${subscriptions.size}")
                        android.util.Log.d(TAG, "   - Conti: ${accounts.size}")
                        Pair(subscriptions, accounts)
                    }
                }
                .catch { exception ->
                    android.util.Log.e(TAG, "❌ ERRORE caricamento abbonamenti", exception)
                    _uiState.value = AbbonamentiUiState.Error(exception.message ?: "Errore sconosciuto")
                }
                .collect { (subscriptions, accounts) ->
                    android.util.Log.d(TAG, "📦 Elaborazione dati...")

                    if (subscriptions.isEmpty()) {
                        val message = if (_showActiveOnly.value) {
                            "Nessun abbonamento attivo"
                        } else {
                            "Nessun abbonamento presente"
                        }
                        android.util.Log.d(TAG, "📭 EMPTY: $message")
                        _uiState.value = AbbonamentiUiState.Empty(message)
                    } else {
                        // Crea mappa accountId -> Account per lookup veloce
                        val accountMap = accounts.associateBy { it.accountId }

                        // Calcola statistiche
                        val stats = calculateStats(subscriptions)

                        android.util.Log.d(TAG, "✅ SUCCESS:")
                        android.util.Log.d(TAG, "   Totale: ${subscriptions.size}")
                        android.util.Log.d(TAG, "   Attivi: ${stats.activeCount}")
                        android.util.Log.d(TAG, "   Costo Mensile: EUR ${stats.monthlyTotal}")
                        android.util.Log.d(TAG, "   Costo Annuale: EUR ${stats.annualTotal}")

                        _uiState.value = AbbonamentiUiState.Success(
                            subscriptions = subscriptions,
                            accounts = accountMap,
                            monthlyTotal = stats.monthlyTotal,
                            annualTotal = stats.annualTotal,
                            activeCount = stats.activeCount
                        )
                    }
                }
        }
    }

    /**
     * Calcola statistiche sugli abbonamenti
     */
    private fun calculateStats(subscriptions: List<Subscription>): SubscriptionStats {
        var monthlyTotal = 0.0
        var annualTotal = 0.0
        var activeCount = 0

        subscriptions.forEach { subscription ->
            if (subscription.isActive) {
                activeCount++
                monthlyTotal += subscription.getMonthlyCost()
                annualTotal += subscription.getAnnualCost()
            }
        }

        return SubscriptionStats(
            monthlyTotal = monthlyTotal,
            annualTotal = annualTotal,
            activeCount = activeCount
        )
    }

    /**
     * Crea un nuovo abbonamento
     */
    fun createSubscription(subscription: Subscription) {
        android.util.Log.d(TAG, "===========================================")
        android.util.Log.d(TAG, "💾 CREATE SUBSCRIPTION")
        android.util.Log.d(TAG, "===========================================")
        android.util.Log.d(TAG, "Nome: ${subscription.name}")
        android.util.Log.d(TAG, "Importo: EUR ${subscription.amount}")
        android.util.Log.d(TAG, "Frequenza: ${subscription.frequency}")
        android.util.Log.d(TAG, "Conto: ${subscription.accountId}")

        viewModelScope.launch {
            // Non impostare Loading qui - il Flow si aggiornerà automaticamente

            firestoreRepository.createSubscription(subscription)
                .onSuccess { id ->
                    android.util.Log.d(TAG, "✅ Abbonamento creato con successo: $id")
                    android.util.Log.d(TAG, "   Il Flow si aggiornerà automaticamente...")
                    // Il listener in getAllSubscriptions() riceverà il nuovo abbonamento
                }
                .onFailure { exception ->
                    android.util.Log.e(TAG, "❌ ERRORE creazione abbonamento", exception)
                    _uiState.value = AbbonamentiUiState.Error(
                        exception.message ?: "Errore durante la creazione"
                    )
                }
        }
    }

    /**
     * Aggiorna un abbonamento esistente
     */
    fun updateSubscription(subscription: Subscription) {
        android.util.Log.d(TAG, "📝 updateSubscription: ${subscription.id}")

        viewModelScope.launch {
            firestoreRepository.updateSubscription(subscription)
                .onSuccess {
                    android.util.Log.d(TAG, "✅ Abbonamento aggiornato")
                }
                .onFailure { exception ->
                    android.util.Log.e(TAG, "❌ Errore aggiornamento", exception)
                    _uiState.value = AbbonamentiUiState.Error(
                        exception.message ?: "Errore durante l'aggiornamento"
                    )
                }
        }
    }

    /**
     * Disattiva un abbonamento (non lo elimina, lo marca come inattivo)
     */
    fun deactivateSubscription(subscriptionId: String) {
        android.util.Log.d(TAG, "⏸️ deactivateSubscription: $subscriptionId")

        viewModelScope.launch {
            firestoreRepository.deactivateSubscription(subscriptionId)
                .onSuccess {
                    android.util.Log.d(TAG, "✅ Abbonamento disattivato")
                }
                .onFailure { exception ->
                    android.util.Log.e(TAG, "❌ Errore disattivazione", exception)
                    _uiState.value = AbbonamentiUiState.Error(
                        exception.message ?: "Errore durante la disattivazione"
                    )
                }
        }
    }

    /**
     * Riattiva un abbonamento
     */
    fun reactivateSubscription(subscriptionId: String) {
        android.util.Log.d(TAG, "▶️ reactivateSubscription: $subscriptionId")

        viewModelScope.launch {
            firestoreRepository.reactivateSubscription(subscriptionId)
                .onSuccess {
                    android.util.Log.d(TAG, "✅ Abbonamento riattivato")
                }
                .onFailure { exception ->
                    android.util.Log.e(TAG, "❌ Errore riattivazione", exception)
                    _uiState.value = AbbonamentiUiState.Error(
                        exception.message ?: "Errore durante la riattivazione"
                    )
                }
        }
    }

    /**
     * Elimina definitivamente un abbonamento
     */
    fun deleteSubscription(subscriptionId: String) {
        android.util.Log.d(TAG, "🗑️ deleteSubscription: $subscriptionId")

        viewModelScope.launch {
            firestoreRepository.deleteSubscription(subscriptionId)
                .onSuccess {
                    android.util.Log.d(TAG, "✅ Abbonamento eliminato")
                }
                .onFailure { exception ->
                    android.util.Log.e(TAG, "❌ Errore eliminazione", exception)
                    _uiState.value = AbbonamentiUiState.Error(
                        exception.message ?: "Errore durante l'eliminazione"
                    )
                }
        }
    }

    /**
     * Toggle filtro attivo/inattivo
     */
    fun toggleShowActiveOnly() {
        val newValue = !_showActiveOnly.value
        android.util.Log.d(TAG, "🔄 toggleShowActiveOnly: $newValue")
        _showActiveOnly.value = newValue
    }

    /**
     * Ricarica i dati
     */
    fun refresh() {
        android.util.Log.d(TAG, "🔄 refresh() chiamato (noop - Flow già attivo)")
        // Con i flow attivi, non serve ricaricare manualmente
    }

    /**
     * Calcola la prossima data di rinnovo dato un abbonamento
     */
    fun calculateNextRenewal(subscription: Subscription): com.google.firebase.Timestamp {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = subscription.nextRenewalDate.toDate()

        // Aggiungi il periodo in base alla frequenza
        when (subscription.frequency) {
            "MONTHLY" -> calendar.add(java.util.Calendar.MONTH, 1)
            "QUARTERLY" -> calendar.add(java.util.Calendar.MONTH, 3)
            "SEMIANNUAL" -> calendar.add(java.util.Calendar.MONTH, 6)
            "ANNUAL" -> calendar.add(java.util.Calendar.YEAR, 1)
        }

        return com.google.firebase.Timestamp(calendar.time)
    }

    /**
     * Verifica se un abbonamento è in scadenza nei prossimi N giorni
     */
    fun isExpiringSoon(subscription: Subscription, daysThreshold: Int = 7): Boolean {
        val now = java.util.Calendar.getInstance()
        val renewal = java.util.Calendar.getInstance()
        renewal.time = subscription.nextRenewalDate.toDate()

        val diffMillis = renewal.timeInMillis - now.timeInMillis
        val diffDays = diffMillis / (24 * 60 * 60 * 1000)

        return diffDays in 0..daysThreshold
    }

    companion object {
        private const val TAG = "AbbonamentiViewModel"
    }
}

/**
 * Data class per le statistiche degli abbonamenti
 */
data class SubscriptionStats(
    val monthlyTotal: Double,
    val annualTotal: Double,
    val activeCount: Int
)

/**
 * Sealed class per gli stati della UI
 */
sealed class AbbonamentiUiState {
    object Loading : AbbonamentiUiState()

    data class Empty(val message: String) : AbbonamentiUiState()

    data class Success(
        val subscriptions: List<Subscription>,
        val accounts: Map<String, Account>, // Map per lookup veloce
        val monthlyTotal: Double,
        val annualTotal: Double,
        val activeCount: Int
    ) : AbbonamentiUiState()

    data class Error(val message: String) : AbbonamentiUiState()
}