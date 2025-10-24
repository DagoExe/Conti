package com.example.conti.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Modello per un movimento bancario in Firestore.
 *
 * Struttura Firestore: /users/{userId}/transactions/{transactionId}
 */
data class Transaction(
    @DocumentId
    val id: String = "",
    val accountId: String = "",
    val amount: Double = 0.0, // Positivo = entrata, Negativo = uscita
    val description: String = "",
    val category: String = "",
    val notes: String? = null,
    val date: Timestamp = Timestamp.now(),
    val type: String = "", // "income" o "expense"
    val isRecurring: Boolean = false,
    val subscriptionId: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    constructor() : this(
        id = "",
        accountId = "",
        amount = 0.0,
        description = "",
        category = "",
        date = Timestamp.now(),
        type = ""
    )
}

/**
 * Modello per un abbonamento/spesa ricorrente in Firestore.
 *
 * Struttura Firestore: /users/{userId}/subscriptions/{subscriptionId}
 */
data class Subscription(
    @DocumentId
    val id: String = "",
    val accountId: String = "",
    val name: String = "",
    val description: String? = null,
    val amount: Double = 0.0,
    val frequency: String = "MONTHLY", // MONTHLY, QUARTERLY, SEMIANNUAL, ANNUAL
    val category: String = "Abbonamento",
    val startDate: Timestamp = Timestamp.now(),
    val nextRenewalDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp? = null,
    val isActive: Boolean = true,
    val notes: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastUpdated: Timestamp? = null
) {
    constructor() : this(
        id = "",
        accountId = "",
        name = "",
        amount = 0.0,
        frequency = "MONTHLY"
    )

    /**
     * Calcola il costo mensile dell'abbonamento.
     */
    fun getMonthlyCost(): Double {
        return when (frequency) {
            "MONTHLY" -> amount
            "QUARTERLY" -> amount / 3.0
            "SEMIANNUAL" -> amount / 6.0
            "ANNUAL" -> amount / 12.0
            else -> 0.0
        }
    }

    /**
     * Calcola il costo annuale dell'abbonamento.
     */
    fun getAnnualCost(): Double {
        return when (frequency) {
            "MONTHLY" -> amount * 12
            "QUARTERLY" -> amount * 4
            "SEMIANNUAL" -> amount * 2
            "ANNUAL" -> amount
            else -> 0.0
        }
    }
}

/**
 * Enum per le frequenze di pagamento.
 */
enum class PaymentFrequency(val displayName: String, val months: Int) {
    MONTHLY("Mensile", 1),
    QUARTERLY("Trimestrale", 3),
    SEMIANNUAL("Semestrale", 6),
    ANNUAL("Annuale", 12);

    companion object {
        fun fromString(value: String): PaymentFrequency {
            return entries.find { it.name == value } ?: MONTHLY
        }
    }
}

/**
 * Profilo utente Firebase.
 *
 * Struttura Firestore: /users/{userId}
 */
data class UserProfile(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastLogin: Timestamp? = null
) {
    constructor() : this(id = "", email = "")
}