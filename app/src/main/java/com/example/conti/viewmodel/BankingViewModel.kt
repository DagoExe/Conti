package com.example.conti.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.example.conti.data.BankingRepository
import com.example.conti.models.Account
import com.example.conti.models.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BankingViewModel : ViewModel() {

    private val repository = BankingRepository()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAccounts()
        loadTransactions()
    }

    // Carica account
    fun loadAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAccounts()
                .onSuccess { _accounts.value = it }
                .onFailure { e -> println("Errore: ${e.message}") }
            _isLoading.value = false
        }
    }

    // Carica transazioni
    fun loadTransactions(accountType: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getTransactions(accountType)
                .onSuccess { _transactions.value = it }
                .onFailure { e -> println("Errore: ${e.message}") }
            _isLoading.value = false
        }
    }

    // Aggiungi transazione manuale
    fun addManualTransaction(
        accountType: String,
        amount: Double,
        description: String,
        category: String,
        date: Timestamp = Timestamp.now()
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountType,
                amount = amount,
                description = description,
                category = category,
                date = date,
                type = if (amount >= 0) "income" else "expense"
            )

            repository.addTransaction(transaction)
                .onSuccess {
                    loadTransactions() // Ricarica lista
                    loadAccounts() // Aggiorna saldi
                }
                .onFailure { e -> println("Errore: ${e.message}") }
        }
    }

    // Crea account iniziali
    fun createInitialAccounts() {
        viewModelScope.launch {
            // BuddyBank
            repository.createAccount(
                Account(
                    accountName = "BuddyBank",
                    accountType = "buddybank",
                    balance = 0.0,
                    iban = "IT60X..." // Opzionale
                )
            )

            // Hype
            repository.createAccount(
                Account(
                    accountName = "Hype",
                    accountType = "hype",
                    balance = 0.0
                )
            )

            loadAccounts()
        }
    }
}