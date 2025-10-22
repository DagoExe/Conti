package com.example.conti.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

// Account bancario
data class Account(
    @DocumentId
    val id: String = "",
    val accountName: String = "",
    val accountType: String = "", // "buddybank" o "hype"
    val balance: Double = 0.0,
    val currency: String = "EUR",
    val iban: String? = null,
    @ServerTimestamp
    val lastUpdated: Timestamp? = null
)

// Transazione
data class Transaction(
    @DocumentId
    val id: String = "",
    val accountId: String = "", // "buddybank" o "hype"
    val amount: Double = 0.0, // Positivo = entrata, Negativo = uscita
    val category: String = "",
    val description: String = "",
    val date: Timestamp = Timestamp.now(),
    val type: String = "", // "income" o "expense"
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

// Profilo utente
data class UserProfile(
    val email: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastLogin: Timestamp? = null
)