package com.example.conti.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conti.data.repository.AccountRepository
import com.example.conti.models.Account
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel per gestire la UI degli Account.
 */
class AccountsViewModel : ViewModel() {

    private val accountRepository = AccountRepository.getInstance()

    // State per la UI
    private val _uiState = MutableStateFlow<AccountsUiState>(AccountsUiState.Loading)
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAccountsFlow()
                .catch { exception ->
                    android.util.Log.e("AccountsViewModel", "Errore loadAccounts", exception)
                    _uiState.value = AccountsUiState.Error(exception.message ?: "Unknown error")
                }
                .collect { accounts ->
                    _uiState.value = if (accounts.isEmpty()) {
                        AccountsUiState.Empty
                    } else {
                        AccountsUiState.Success(accounts, calculateTotalBalance(accounts))
                    }
                }
        }
    }

    fun saveAccount(account: Account) {
        viewModelScope.launch {
            _uiState.value = AccountsUiState.Loading
            accountRepository.saveAccount(account).fold(
                onSuccess = {
                    android.util.Log.d("AccountsViewModel", "✅ Account salvato con successo")
                    // Il Flow si aggiornerà automaticamente
                },
                onFailure = { exception ->
                    android.util.Log.e("AccountsViewModel", "❌ Errore salvataggio", exception)
                    _uiState.value = AccountsUiState.Error(exception.message ?: "Save failed")
                }
            )
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            accountRepository.deleteAccount(accountId).fold(
                onSuccess = {
                    android.util.Log.d("AccountsViewModel", "✅ Account eliminato")
                },
                onFailure = { exception ->
                    android.util.Log.e("AccountsViewModel", "❌ Errore eliminazione", exception)
                    _uiState.value = AccountsUiState.Error(exception.message ?: "Delete failed")
                }
            )
        }
    }

    private fun calculateTotalBalance(accounts: List<Account>): Double {
        return accounts.sumOf { it.balance }
    }

    fun retry() {
        loadAccounts()
    }
}

/**
 * Sealed class per rappresentare gli stati della UI
 */
sealed class AccountsUiState {
    object Loading : AccountsUiState()
    object Empty : AccountsUiState()
    data class Success(val accounts: List<Account>, val totalBalance: Double) : AccountsUiState()
    data class Error(val message: String) : AccountsUiState()
}