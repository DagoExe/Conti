package com.example.conti.data.dto

import com.google.firebase.Timestamp
import com.example.conti.models.Account
import com.example.conti.models.AccountType

/**
 * Data Transfer Object per Account in Firestore
 * Mantiene la compatibilità diretta con la struttura del database
 */
data class AccountDto(
    val accountName: String = "",
    val accountType: String = "",
    val balance: Double = 0.0,
    val currency: String = "EUR",
    val iban: String? = null,
    val lastUpdated: Timestamp? = null
) {
    /**
     * Converte il DTO nel modello di dominio
     */
    fun toDomain(accountId: String): Account {
        return Account(
            accountId = accountId,
            accountName = accountName,
            accountType = AccountType.fromString(accountType),
            balance = balance,
            currency = currency,
            iban = iban,
            lastUpdated = lastUpdated
        )
    }

    companion object {
        /**
         * Crea un DTO dal modello di dominio
         */
        fun fromDomain(account: Account): AccountDto {
            return AccountDto(
                accountName = account.accountName,
                accountType = account.accountType.name.lowercase(),
                balance = account.balance,
                currency = account.currency,
                iban = account.iban,
                lastUpdated = account.lastUpdated
            )
        }
    }
}