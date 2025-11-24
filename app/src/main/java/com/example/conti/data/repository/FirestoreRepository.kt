package com.example.conti.data.repository

import android.util.Log
import com.example.conti.models.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * FirestoreRepository - VERSIONE CORRETTA
 *
 * ✅ NON CRASHA se l'utente non è autenticato
 * ✅ Restituisce Flow vuoti invece di lanciare eccezioni
 * ✅ Logging dettagliato per debug
 */
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ========================================
    // UTILITY / ACCESS CONTROL
    // ========================================

    /**
     * Verifica che l'utente sia autenticato.
     * ✅ NUOVO: Non lancia eccezione, restituisce null se non autenticato.
     */
    private fun getUserIdSafely(): String? {
        val user = auth.currentUser
        if (user == null) {
            Log.w("FirestoreRepository", "⚠️ Utente non ancora autenticato")
        }
        return user?.uid
    }

    /**
     * Verifica che l'utente sia autenticato.
     * ⚠️ Lancia eccezione solo per operazioni di SCRITTURA.
     */
    private fun requireUser(): String {
        val user = auth.currentUser
        if (user == null) {
            Log.e("FirestoreRepository", "❌ Accesso negato: utente non autenticato.")
            throw IllegalStateException("Utente non autenticato. Esegui login prima di accedere ai dati.")
        }
        return user.uid
    }

    // ========================================
    // PROFILO UTENTE
    // ========================================

    /**
     * Crea o aggiorna il documento del profilo utente.
     */
    suspend fun updateUserProfile(email: String) {
        try {
            val userId = requireUser()
            val profileData = mapOf(
                "email" to email,
                "lastLogin" to Timestamp.now()
            )

            db.collection("users")
                .document(userId)
                .collection("profile")
                .document("main")
                .set(profileData, SetOptions.merge())
                .await()

            Log.d("FirestoreRepository", "✅ Profilo aggiornato per $email")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "❌ Errore aggiornamento profilo", e)
        }
    }

    // ========================================
    // ACCOUNT / CONTI
    // ========================================

    suspend fun createAccount(account: Account): Result<String> {
        return try {
            val userId = requireUser()
            val docRef = db.collection("users")
                .document(userId)
                .collection("accounts")
                .document()

            val newAccount = account.copy(
                accountId = docRef.id,
                accountName = "",
                accountType = AccountType.OTHER,
                balance = 0.0,
                currency = "EUR",
                iban = null,
                lastUpdated = Timestamp.now()
            )

            docRef.set(newAccount).await()
            Log.d("FirestoreRepository", "✅ Account creato: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "❌ Errore creazione account", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ CORRETTO: Restituisce Flow vuoto se l'utente non è autenticato
     * invece di crashare.
     */
    fun getAllAccounts(): Flow<List<Account>> = callbackFlow {
        // Controlla se l'utente è autenticato
        val userId = getUserIdSafely()

        if (userId == null) {
            Log.w("FirestoreRepository", "⚠️ getAllAccounts: utente non autenticato, restituisco lista vuota")
            // Invia lista vuota e attendi che l'utente si autentichi
            trySend(emptyList())

            // Aspetta che l'utente si autentichi
            val authListener = FirebaseAuth.AuthStateListener { auth ->
                if (auth.currentUser != null) {
                    Log.d("FirestoreRepository", "✅ Utente autenticato, riprovo a caricare accounts")
                    // L'observer verrà ricreato quando il LiveData viene re-osservato
                }
            }
            auth.addAuthStateListener(authListener)
            awaitClose { auth.removeAuthStateListener(authListener) }
            return@callbackFlow
        }

        // Utente autenticato, procedi normalmente
        try {
            val listener = db.collection("users")
                .document(userId)
                .collection("accounts")
                .orderBy("accountName", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirestoreRepository", "❌ Errore caricamento accounts", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    val accounts = snapshot?.documents?.mapNotNull {
                        it.toObject(Account::class.java)
                    } ?: emptyList()

                    Log.d("FirestoreRepository", "📊 Caricati ${accounts.size} accounts")
                    trySend(accounts)
                }
            awaitClose {
                Log.d("FirestoreRepository", "🔌 Chiusura listener accounts")
                listener.remove()
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "❌ Eccezione getAllAccounts", e)
            trySend(emptyList())
            close(e)
        }
    }

    suspend fun updateAccount(account: Account): Result<Unit> {
        return try {
            val userId = requireUser()
            db.collection("users")
                .document(userId)
                .collection("accounts")
                .document(account.accountName)
                .set(account.copy(lastUpdated = Timestamp.now()), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(accountId: String): Result<Unit> {
        return try {
            val userId = requireUser()
            db.collection("users")
                .document(userId)
                .collection("accounts")
                .document(accountId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // TRANSAZIONI / MOVIMENTI
    // ========================================

    suspend fun addTransaction(transaction: Transaction): Result<String> {
        return try {
            val userId = requireUser()

            val docRef = db.collection("users")
                .document(userId)
                .collection("transactions")
                .document()

            val newTransaction = transaction.copy(
                id = docRef.id,
                createdAt = Timestamp.now()
            )

            docRef.set(newTransaction).await()
            updateBalanceAfterTransaction(newTransaction)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Errore creazione transazione", e)
            Result.failure(e)
        }
    }

    private suspend fun updateBalanceAfterTransaction(transaction: Transaction) {
        val userId = requireUser()
        val accountRef = db.collection("users")
            .document(userId)
            .collection("accounts")
            .document(transaction.accountId)

        db.runTransaction { tx ->
            val snapshot = tx.get(accountRef)
            val currentBalance = snapshot.getDouble("balance") ?: 0.0
            val newBalance = currentBalance + transaction.amount

            tx.update(accountRef, mapOf(
                "balance" to newBalance,
                "lastUpdated" to Timestamp.now()
            ))
        }.await()
    }

    /**
     * ✅ CORRETTO: Restituisce Flow vuoto se utente non autenticato
     */
    fun getAllTransactions(accountId: String? = null): Flow<List<Transaction>> = callbackFlow {
        val userId = getUserIdSafely()

        if (userId == null) {
            Log.w("FirestoreRepository", "⚠️ getAllTransactions: utente non autenticato")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var query = db.collection("users")
            .document(userId)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)

        if (accountId != null) query = query.whereEqualTo("accountId", accountId)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val transactions = snapshot?.documents?.mapNotNull {
                it.toObject(Transaction::class.java)
            } ?: emptyList()

            trySend(transactions)
        }

        awaitClose { listener.remove() }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            val userId = requireUser()
            val ref = db.collection("users")
                .document(userId)
                .collection("transactions")
                .document(transactionId)

            val transaction = ref.get().await().toObject(Transaction::class.java)
                ?: throw Exception("Transazione non trovata")

            ref.delete().await()
            updateBalanceAfterTransaction(transaction.copy(amount = -transaction.amount))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // STATISTICHE
    // ========================================

    suspend fun getTotalByCategory(startDate: Timestamp, endDate: Timestamp): Result<Map<String, Double>> {
        return try {
            val userId = requireUser()
            val snapshot = db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()

            val totals = mutableMapOf<String, Double>()
            snapshot.documents.forEach { doc ->
                val category = doc.getString("category") ?: "Altro"
                val amount = doc.getDouble("amount") ?: 0.0
                totals[category] = totals.getOrDefault(category, 0.0) + amount
            }

            Result.success(totals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ✅ CORRETTO: Restituisce Flow vuoto se utente non autenticato
     */
    fun getTransactionsByDateRange(
        startDate: Timestamp,
        endDate: Timestamp
    ): Flow<List<Transaction>> = callbackFlow {
        val userId = getUserIdSafely()

        if (userId == null) {
            Log.w("FirestoreRepository", "⚠️ getTransactionsByDateRange: utente non autenticato")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection("users")
            .document(userId)
            .collection("transactions")
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val transactions = snapshot?.documents?.mapNotNull {
                    it.toObject(Transaction::class.java)
                } ?: emptyList()

                trySend(transactions)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addTransactionsBatch(transactions: List<Transaction>): Result<Int> {
        return try {
            val userId = requireUser()
            val batch = db.batch()

            transactions.forEach { t ->
                val docRef = db.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .document()
                batch.set(docRef, t.copy(id = docRef.id, createdAt = Timestamp.now()))
            }

            batch.commit().await()
            Result.success(transactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // ABBONAMENTI
    // ========================================

    /**
     * ✅ CORRETTO: Restituisce Flow vuoto se utente non autenticato
     */
    fun getAllSubscriptions(activeOnly: Boolean = true): Flow<List<Subscription>> = callbackFlow {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            android.util.Log.e("FirestoreRepo", "❌ User non autenticato")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        android.util.Log.d("FirestoreRepo", "📡 Ascolto abbonamenti (activeOnly=$activeOnly) per user $userId")

        // ✅ FIX: Rimosso .orderBy("name") dalla query quando si usa whereEqualTo("active", true)
        // Questo evita l'errore di "Missing Index" di Firestore per query composite.
        // L'ordinamento viene ora effettuato in memoria sulla lista risultante.
        val query = if (activeOnly) {
            db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .whereEqualTo("active", true)
        } else {
            db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .orderBy("name", Query.Direction.ASCENDING)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreRepo", "❌ Errore listener abbonamenti", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                android.util.Log.d("FirestoreRepo", "📭 Nessun abbonamento trovato")
                trySend(emptyList())
                return@addSnapshotListener
            }

            val subscriptions = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Subscription::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreRepo", "❌ Errore parsing abbonamento ${doc.id}", e)
                    null
                }
            }.sortedBy { it.name.lowercase() } // Ordinamento in memoria

            android.util.Log.d("FirestoreRepo", "✅ Caricati ${subscriptions.size} abbonamenti")
            subscriptions.forEachIndexed { index, sub ->
                android.util.Log.d("FirestoreRepo", "   [$index] ${sub.name} - €${sub.amount} - ${if (sub.isActive) "ATTIVO" else "INATTIVO"}")
            }

            trySend(subscriptions)
        }

        awaitClose {
            android.util.Log.d("FirestoreRepo", "🔌 Chiuso listener abbonamenti")
            listener.remove()
        }
    }

    suspend fun createSubscription(subscription: Subscription): Result<String> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                android.util.Log.e("FirestoreRepo", "❌ User non autenticato")
                return Result.failure(Exception("Utente non autenticato"))
            }

            android.util.Log.d("FirestoreRepo", "💾 Creazione abbonamento: ${subscription.name}")
            
            val docRef = db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .document()

            val subscriptionWithId = subscription.copy(
                id = docRef.id,
                createdAt = Timestamp.now(),
                lastUpdated = Timestamp.now()
            )

            docRef.set(subscriptionWithId).await()

            android.util.Log.d("FirestoreRepo", "✅ Abbonamento creato con ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "❌ Errore creazione abbonamento", e)
            Result.failure(e)
        }
    }

    suspend fun updateSubscription(subscription: Subscription): Result<Unit> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("Utente non autenticato"))
            }

            android.util.Log.d("FirestoreRepo", "📝 Aggiornamento abbonamento: ${subscription.id}")

            val updatedSubscription = subscription.copy(
                lastUpdated = Timestamp.now()
            )

            db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .document(subscription.id)
                .set(updatedSubscription)
                .await()

            android.util.Log.d("FirestoreRepo", "✅ Abbonamento aggiornato")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "❌ Errore aggiornamento abbonamento", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSubscription(subscriptionId: String): Result<Unit> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("Utente non autenticato"))
            }

            android.util.Log.d("FirestoreRepo", "🗑️ Eliminazione abbonamento: $subscriptionId")

            db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .document(subscriptionId)
                .delete()
                .await()

            android.util.Log.d("FirestoreRepo", "✅ Abbonamento eliminato")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "❌ Errore eliminazione abbonamento", e)
            Result.failure(e)
        }
    }

    suspend fun deactivateSubscription(subscriptionId: String): Result<Unit> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("Utente non autenticato"))
            }

            android.util.Log.d("FirestoreRepo", "⏸️ Disattivazione abbonamento: $subscriptionId")

            db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .document(subscriptionId)
                .update(
                    mapOf(
                        "active" to false,
                        "lastUpdated" to Timestamp.now()
                    )
                )
                .await()

            android.util.Log.d("FirestoreRepo", "✅ Abbonamento disattivato")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "❌ Errore disattivazione abbonamento", e)
            Result.failure(e)
        }
    }

    suspend fun reactivateSubscription(subscriptionId: String): Result<Unit> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("Utente non autenticato"))
            }

            android.util.Log.d("FirestoreRepo", "▶️ Riattivazione abbonamento: $subscriptionId")

            db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .document(subscriptionId)
                .update(
                    mapOf(
                        "active" to true,
                        "lastUpdated" to Timestamp.now()
                    )
                )
                .await()

            android.util.Log.d("FirestoreRepo", "✅ Abbonamento riattivato")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "❌ Errore riattivazione abbonamento", e)
            Result.failure(e)
        }
    }

    suspend fun getSubscription(subscriptionId: String): Result<Subscription?> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("Utente non autenticato"))
            }

            android.util.Log.d("FirestoreRepo", "🔍 Recupero abbonamento: $subscriptionId")

            val doc = db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .document(subscriptionId)
                .get()
                .await()

            val subscription = doc.toObject(Subscription::class.java)?.copy(id = doc.id)

            if (subscription != null) {
                android.util.Log.d("FirestoreRepo", "✅ Abbonamento trovato: ${subscription.name}")
            } else {
                android.util.Log.d("FirestoreRepo", "📭 Abbonamento non trovato")
            }

            Result.success(subscription)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "❌ Errore recupero abbonamento", e)
            Result.failure(e)
        }
    }

    suspend fun getExpiringSubscriptions(daysThreshold: Int = 7): Result<List<Subscription>> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("Utente non autenticato"))
            }

            android.util.Log.d("FirestoreRepo", "⚠️ Recupero abbonamenti in scadenza (entro $daysThreshold giorni)")

            val now = java.util.Calendar.getInstance()
            val threshold = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, daysThreshold)
            }

            val snapshot = db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .whereEqualTo("active", true)
                .whereLessThanOrEqualTo("nextRenewalDate", Timestamp(threshold.time))
                .whereGreaterThanOrEqualTo("nextRenewalDate", Timestamp(now.time))
                .get()
                .await()

            val subscriptions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Subscription::class.java)?.copy(id = doc.id)
            }

            android.util.Log.d("FirestoreRepo", "✅ Trovati ${subscriptions.size} abbonamenti in scadenza")
            Result.success(subscriptions)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "❌ Errore recupero abbonamenti in scadenza", e)
            Result.failure(e)
        }
    }

    suspend fun getTotalMonthlySubscriptionCost(): Result<Double> {
        return try {
            val userId = requireUser()
            val snapshot = db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .whereEqualTo("active", true)
                .get()
                .await()

            val total = snapshot.documents.sumOf { doc ->
                val amount = doc.getDouble("amount") ?: 0.0
                val freq = doc.getString("frequency") ?: "MONTHLY"
                when (freq) {
                    "MONTHLY" -> amount
                    "QUARTERLY" -> amount / 3.0
                    "SEMIANNUAL" -> amount / 6.0
                    "ANNUAL" -> amount / 12.0
                    else -> 0.0
                }
            }

            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // TIMESTAMP HELPERS
    // ========================================

    companion object {
        fun getStartOfCurrentMonth(): Timestamp {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return Timestamp(c.time)
        }

        fun getEndOfCurrentMonth(): Timestamp {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            c.set(Calendar.MILLISECOND, 999)
            return Timestamp(c.time)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calcola costo mensile di un abbonamento
     */
    fun Subscription.getMonthlyCost(): Double {
        return when (frequency) {
            "MONTHLY" -> amount
            "QUARTERLY" -> amount / 3.0
            "SEMIANNUAL" -> amount / 6.0
            "ANNUAL" -> amount / 12.0
            else -> amount
        }
    }

    /**
     * Calcola costo annuale di un abbonamento
     */
    fun Subscription.getAnnualCost(): Double {
        return when (frequency) {
            "MONTHLY" -> amount * 12
            "QUARTERLY" -> amount * 4
            "SEMIANNUAL" -> amount * 2
            "ANNUAL" -> amount
            else -> amount * 12
        }
    }
}