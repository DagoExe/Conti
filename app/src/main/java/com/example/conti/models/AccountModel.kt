package com.example.conti.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Modello dominio per un Account bancario.
 * Rappresenta un documento nella sottocollezione users/{userId}/accounts/{accountId}
 *
 * STRUTTURA FIRESTORE:
 * users/{userId}/accounts/{accountId}
 *   - accountName: string
 *   - accountType: string ("buddybank" | "hype" | "other")
 *   - balance: number
 *   - currency: string
 *   - iban: string (opzionale)
 *   - lastUpdated: timestamp
 */
data class Account(
    @DocumentId
    val accountId: String = "", // "buddybank", "hype", ecc. - ID del documento Firestore

    @PropertyName("accountName")
    val accountName: String = "",

    @PropertyName("accountType")
    val accountType: AccountType = AccountType.OTHER,

    @PropertyName("balance")
    val balance: Double = 0.0,

    @PropertyName("currency")
    val currency: String = "EUR",

    @PropertyName("iban")
    val iban: String? = null, // Opzionale per conti come Hype

    @PropertyName("lastUpdated")
    @ServerTimestamp
    val lastUpdated: Timestamp? = null
) {
    /**
     * Costruttore vuoto richiesto da Firestore per la deserializzazione
     */
    constructor() : this(
        accountId = "",
        accountName = "",
        accountType = AccountType.OTHER,
        balance = 0.0,
        currency = "EUR",
        iban = null,
        lastUpdated = null
    )

    /**
     * Valida i dati dell'account
     */
    fun isValid(): Boolean {
        return accountId.isNotBlank() &&
                accountName.isNotBlank() &&
                currency.isNotBlank() &&
                (iban == null || iban.isValidIBAN())
    }

    /**
     * Formatta il balance con il simbolo della valuta
     */
    fun getFormattedBalance(): String {
        return when (currency) {
            "EUR" -> String.format("%.2f €", balance)
            "USD" -> String.format("$ %.2f", balance)
            else -> String.format("%.2f %s", balance, currency)
        }
    }
}

/**
 * Enum per i tipi di account supportati
 */
enum class AccountType(val displayName: String) {
    @PropertyName("buddybank")
    BUDDYBANK("Conto BuddyBank"),

    @PropertyName("hype")
    HYPE("Hype Card"),

    @PropertyName("other")
    OTHER("Altro");

    companion object {
        fun fromString(value: String): AccountType {
            return values().find {
                it.name.lowercase() == value.lowercase()
            } ?: OTHER
        }
    }
}

/**
 * Extension function per validare IBAN
 */
private fun String.isValidIBAN(): Boolean {
    // Rimuovi spazi e converti in maiuscolo
    val cleanIban = this.replace(" ", "").uppercase()

    // Verifica lunghezza base (15-34 caratteri)
    if (cleanIban.length < 15 || cleanIban.length > 34) return false

    // Verifica formato base: IT + 2 cifre + 23 caratteri alfanumerici
    if (cleanIban.startsWith("IT")) {
        return cleanIban.matches(Regex("^IT\\d{2}[A-Z]\\d{10}[A-Z0-9]{12}$"))
    }

    // Per altri paesi, verifica solo il formato base
    return cleanIban.matches(Regex("^[A-Z]{2}\\d{2}[A-Z0-9]+$"))
}