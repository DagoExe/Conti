package com.example.conti.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conti.data.repository.AccountRepository
import com.example.conti.models.Account
import com.example.conti.models.AccountType
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel per operazioni bancarie generiche.
 * Usa AccountRepository per accedere ai dati.
 */
class BankingViewModel : ViewModel() {

    private val accountRepository = AccountRepository.getInstance()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAccounts()
    }

    /**
     * Carica gli account (snapshot singolo)
     */
    fun loadAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            accountRepository.getAccounts()
                .onSuccess { accountsList ->
                    _accounts.value = accountsList
                    android.util.Log.d("BankingViewModel", "✅ Caricati ${accountsList.size} account")
                }
                .onFailure { e ->
                    android.util.Log.e("BankingViewModel", "❌ Errore caricamento account: ${e.message}")
                }
            _isLoading.value = false
        }
    }

    /**
     * Crea gli account iniziali (BuddyBank e Hype)
     */
    fun createInitialAccounts() {
        viewModelScope.launch {
            // Crea BuddyBank
            val buddybank = Account(
                accountId = "buddybank",
                accountName = "Conto BuddyBank",
                accountType = AccountType.BUDDYBANK,
                balance = 0.0,
                currency = "EUR",
                iban = "IT60X0347901600000000000000", // Esempio IBAN
                lastUpdated = Timestamp.now()
            )

            accountRepository.saveAccount(buddybank)
                .onSuccess {
                    android.util.Log.d("BankingViewModel", "✅ BuddyBank creato")
                }
                .onFailure { e ->
                    android.util.Log.e("BankingViewModel", "❌ Errore creazione BuddyBank: ${e.message}")
                }

            // Crea Hype
            val hype = Account(
                accountId = "hype",
                accountName = "Hype Card",
                accountType = AccountType.HYPE,
                balance = 0.0,
                currency = "EUR",
                iban = null, // Hype non ha IBAN tradizionale
                lastUpdated = Timestamp.now()
            )

            accountRepository.saveAccount(hype)
                .onSuccess {
                    android.util.Log.d("BankingViewModel", "✅ Hype creato")
                }
                .onFailure { e ->
                    android.util.Log.e("BankingViewModel", "❌ Errore creazione Hype: ${e.message}")
                }

            // Ricarica la lista
            loadAccounts()
        }
    }

    /**
     * Aggiorna il saldo di un account
     */
    fun updateAccountBalance(accountId: String, newBalance: Double) {
        viewModelScope.launch {
            accountRepository.updateBalance(accountId, newBalance)
                .onSuccess {
                    android.util.Log.d("BankingViewModel", "✅ Saldo aggiornato per $accountId")
                    loadAccounts() // Ricarica la lista
                }
                .onFailure { e ->
                    android.util.Log.e("BankingViewModel", "❌ Errore aggiornamento saldo: ${e.message}")
                }
        }
    }
}