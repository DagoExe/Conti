package com.example.conti.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conti.data.repository.AccountRepository
import com.example.conti.models.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

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
            accountRepository.saveAccount(account).fold(
                onSuccess = { /* Account salvato con successo */ },
                onFailure = { exception ->
                    _uiState.value = AccountsUiState.Error(exception.message ?: "Save failed")
                }
            )
        }
    }

    private fun calculateTotalBalance(accounts: List<Account>): Double {
        return accounts.sumOf { it.balance }
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