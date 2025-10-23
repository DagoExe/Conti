package com.example.conti.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.example.conti.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Repository centrale per tutte le operazioni Firestore.
 *
 * Gestisce:
 * - Account bancari
 * - Transazioni/Movimenti
 * - Abbonamenti
 * - Statistiche e calcoli
 *
 * Struttura Firestore:
 * /users/{userId}/
 *   ├── accounts/{accountId}
 *   ├── transactions/{transactionId}
 *   └── subscriptions/{subscriptionId}
 */
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ========================================
    // UTILITY
    // ========================================

    /**
     * Ottiene l'ID dell'utente corrente.
     * @throws IllegalStateException se l'utente non è autenticato
     */
    private fun getUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Utente non autenticato")
    }

    /**
     * Ottiene il riferimento alla collection principale dell'utente.
     */
    private fun getUserCollection(collection: String) =
        db.collection("users").document(getUserId()).collection(collection)

    // ========================================
    // ACCOUNT / CONTI
    // ========================================

    /**
     * Crea un nuovo account.
     */
    suspend fun createAccount(account: Account): Result<String> {
        return try {
            val userId = getUserId()
            val docRef = db.collection("users")
                .document(userId)
                .collection("accounts")
                .document()

            val accountWithId = account.copy(
                id = docRef.id,
                createdAt = Timestamp.now(),
                lastUpdated = Timestamp.now()
            )

            docRef.set(accountWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene tutti gli account come Flow reattivo.
     */
    fun getAllAccounts(): Flow<List<Account>> = callbackFlow {
        val userId = getUserId()
        val listener = db.collection("users")
            .document(userId)
            .collection("accounts")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val accounts = snapshot?.documents?.mapNotNull {
                    it.toObject(Account::class.java)
                } ?: emptyList()

                trySend(accounts)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Ottiene un account specifico per ID.
     */
    fun getAccountById(accountId: String): Flow<Account?> = callbackFlow {
        val userId = getUserId()
        val listener = db.collection("users")
            .document(userId)
            .collection("accounts")
            .document(accountId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val account = snapshot?.toObject(Account::class.java)
                trySend(account)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Aggiorna un account esistente.
     */
    suspend fun updateAccount(account: Account): Result<Unit> {
        return try {
            val userId = getUserId()
            db.collection("users")
                .document(userId)
                .collection("accounts")
                .document(account.id)
                .set(account.copy(lastUpdated = Timestamp.now()), SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aggiorna il saldo di un account.
     */
    suspend fun updateAccountBalance(accountId: String, newBalance: Double): Result<Unit> {
        return try {
            val userId = getUserId()
            db.collection("users")
                .document(userId)
                .collection("accounts")
                .document(accountId)
                .update(
                    mapOf(
                        "balance" to newBalance,
                        "lastUpdated" to Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un account.
     * ATTENZIONE: Non elimina le transazioni associate (da gestire manualmente).
     */
    suspend fun deleteAccount(accountId: String): Result<Unit> {
        return try {
            val userId = getUserId()
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

    /**
     * Aggiunge una transazione e aggiorna il saldo dell'account.
     */
    suspend fun addTransaction(transaction: Transaction): Result<String> {
        return try {
            val userId = getUserId()

            // Crea la transazione
            val docRef = db.collection("users")
                .document(userId)
                .collection("transactions")
                .document()

            val transactionWithId = transaction.copy(
                id = docRef.id,
                createdAt = Timestamp.now()
            )

            docRef.set(transactionWithId).await()

            // Aggiorna il saldo dell'account
            updateBalanceAfterTransaction(transaction)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aggiunge multiple transazioni in batch (es. da Excel).
     */
    suspend fun addTransactionsBatch(transactions: List<Transaction>): Result<Int> {
        return try {
            val userId = getUserId()
            val batch = db.batch()

            transactions.forEach { transaction ->
                val docRef = db.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .document()

                batch.set(docRef, transaction.copy(
                    id = docRef.id,
                    createdAt = Timestamp.now()
                ))
            }

            batch.commit().await()

            // Ricalcola il saldo dell'account
            if (transactions.isNotEmpty()) {
                recalculateAccountBalance(transactions.first().accountId)
            }

            Result.success(transactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aggiorna il saldo di un account dopo una transazione.
     */
    private suspend fun updateBalanceAfterTransaction(transaction: Transaction) {
        val userId = getUserId()
        val accountRef = db.collection("users")
            .document(userId)
            .collection("accounts")
            .document(transaction.accountId)

        db.runTransaction { tx ->
            val snapshot = tx.get(accountRef)
            val currentBalance = snapshot.getDouble("balance") ?: 0.0
            val newBalance = currentBalance + transaction.amount

            tx.update(accountRef, "balance", newBalance)
            tx.update(accountRef, "lastUpdated", Timestamp.now())
        }.await()
    }

    /**
     * Ricalcola il saldo di un account da zero.
     * Utile dopo import massivo o correzioni.
     */
    private suspend fun recalculateAccountBalance(accountId: String) {
        val userId = getUserId()

        // Ottieni saldo iniziale
        val accountSnapshot = db.collection("users")
            .document(userId)
            .collection("accounts")
            .document(accountId)
            .get()
            .await()

        val initialBalance = accountSnapshot.getDouble("initialBalance") ?: 0.0

        // Calcola somma di tutte le transazioni
        val transactionsSnapshot = db.collection("users")
            .document(userId)
            .collection("transactions")
            .whereEqualTo("accountId", accountId)
            .get()
            .await()

        val totalMovements = transactionsSnapshot.documents.sumOf {
            it.getDouble("amount") ?: 0.0
        }

        val newBalance = initialBalance + totalMovements

        // Aggiorna saldo
        updateAccountBalance(accountId, newBalance)
    }

    /**
     * Ottiene tutte le transazioni come Flow.
     */
    fun getAllTransactions(
        accountId: String? = null,
        limit: Int = 100
    ): Flow<List<Transaction>> = callbackFlow {
        val userId = getUserId()
        var query: Query = db.collection("users")
            .document(userId)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)

        // Filtra per account se specificato
        if (accountId != null) {
            query = query.whereEqualTo("accountId", accountId)
        }

        val listener = query.limit(limit.toLong())
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

    /**
     * Ottiene transazioni in un intervallo di date.
     */
    fun getTransactionsByDateRange(
        startDate: Timestamp,
        endDate: Timestamp,
        accountId: String? = null
    ): Flow<List<Transaction>> = callbackFlow {
        val userId = getUserId()
        var query: Query = db.collection("users")
            .document(userId)
            .collection("transactions")
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.DESCENDING)

        if (accountId != null) {
            query = query.whereEqualTo("accountId", accountId)
        }

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

    /**
     * Elimina una transazione e aggiorna il saldo.
     */
    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            val userId = getUserId()

            // Leggi la transazione prima di eliminarla
            val transactionRef = db.collection("users")
                .document(userId)
                .collection("transactions")
                .document(transactionId)

            val transaction = transactionRef.get().await()
                .toObject(Transaction::class.java)
                ?: throw Exception("Transazione non trovata")

            // Elimina la transazione
            transactionRef.delete().await()

            // Inverti l'importo per ripristinare il saldo
            updateBalanceAfterTransaction(transaction.copy(amount = -transaction.amount))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // ABBONAMENTI / SUBSCRIPTIONS
    // ========================================

    /**
     * Crea un nuovo abbonamento.
     */
    suspend fun createSubscription(subscription: Subscription): Result<String> {
        return try {
            val userId = getUserId()
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
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene tutti gli abbonamenti.
     */
    fun getAllSubscriptions(activeOnly: Boolean = false): Flow<List<Subscription>> = callbackFlow {
        val userId = getUserId()
        var query: Query = db.collection("users")
            .document(userId)
            .collection("subscriptions")
            .orderBy("name", Query.Direction.ASCENDING)

        if (activeOnly) {
            query = query.whereEqualTo("isActive", true)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val subscriptions = snapshot?.documents?.mapNotNull {
                it.toObject(Subscription::class.java)
            } ?: emptyList()

            trySend(subscriptions)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Aggiorna un abbonamento.
     */
    suspend fun updateSubscription(subscription: Subscription): Result<Unit> {
        return try {
            val userId = getUserId()
            db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .document(subscription.id)
                .set(subscription.copy(lastUpdated = Timestamp.now()), SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disattiva un abbonamento.
     */
    suspend fun deactivateSubscription(
        subscriptionId: String,
        endDate: Timestamp = Timestamp.now()
    ): Result<Unit> {
        return try {
            val userId = getUserId()
            db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .document(subscriptionId)
                .update(
                    mapOf(
                        "isActive" to false,
                        "endDate" to endDate,
                        "lastUpdated" to Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un abbonamento.
     */
    suspend fun deleteSubscription(subscriptionId: String): Result<Unit> {
        return try {
            val userId = getUserId()
            db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .document(subscriptionId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // STATISTICHE
    // ========================================

    /**
     * Calcola il totale per categoria in un periodo.
     */
    suspend fun getTotalByCategory(
        startDate: Timestamp,
        endDate: Timestamp
    ): Result<Map<String, Double>> {
        return try {
            val userId = getUserId()

            val snapshot = db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .whereEqualTo("type", "expense")
                .get()
                .await()

            val totals = mutableMapOf<String, Double>()

            snapshot.documents.forEach { doc ->
                val category = doc.getString("category") ?: "Altro"
                val amount = doc.getDouble("amount") ?: 0.0
                totals[category] = totals.getOrDefault(category, 0.0) + Math.abs(amount)
            }

            Result.success(totals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calcola il costo mensile totale degli abbonamenti attivi.
     */
    suspend fun getTotalMonthlySubscriptionCost(): Result<Double> {
        return try {
            val userId = getUserId()

            val snapshot = db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val total = snapshot.documents.sumOf { doc ->
                val amount = doc.getDouble("amount") ?: 0.0
                val frequency = doc.getString("frequency") ?: "MONTHLY"

                when (frequency) {
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
    // UTILITY - TIMESTAMP HELPERS
    // ========================================

    companion object {
        /**
         * Ottiene il timestamp per l'inizio del mese corrente.
         */
        fun getStartOfCurrentMonth(): Timestamp {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return Timestamp(calendar.time)
        }

        /**
         * Ottiene il timestamp per la fine del mese corrente.
         */
        fun getEndOfCurrentMonth(): Timestamp {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            return Timestamp(calendar.time)
        }
    }
}