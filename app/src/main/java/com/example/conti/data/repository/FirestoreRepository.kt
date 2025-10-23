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
 * FirestoreRepository
 *
 * Gestisce l'accesso sicuro ai dati Firestore per:
 * - Profili utente
 * - Conti bancari
 * - Transazioni
 * - Statistiche
 *
 * Struttura Firestore:
 * users/{userId}/
 *   ├── profile (documento)
 *   ├── accounts/{accountId}
 *   └── transactions/{transactionId}
 */
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ========================================
    // UTILITY / ACCESS CONTROL
    // ========================================

    /**
     * Verifica che l'utente sia autenticato.
     * Se non lo è, lancia un'eccezione.
     */
    private fun requireUser(): String {
        val user = auth.currentUser
        if (user == null) {
            Log.e("FirestoreRepository", "❌ Accesso negato: utente non autenticato.")
            throw IllegalStateException("Utente non autenticato. Esegui login prima di accedere ai dati.")
        }
        return user.uid
    }

    /**
     * Riferimento alla collezione interna dell’utente autenticato.
     */
    private fun userCollection(path: String) =
        db.collection("users").document(requireUser()).collection(path)

    /**
     * Riferimento al documento del profilo dell’utente autenticato.
     */
    private fun userProfileRef() =
        db.collection("users").document(requireUser()).collection("profile")

    // ========================================
    // PROFILO UTENTE
    // ========================================

    /**
     * Crea o aggiorna il documento del profilo utente.
     */
    suspend fun updateUserProfile(email: String) {
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
                id = docRef.id,
                createdAt = Timestamp.now(),
                lastUpdated = Timestamp.now()
            )

            docRef.set(newAccount).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Errore creazione account", e)
            Result.failure(e)
        }
    }

    fun getAllAccounts(): Flow<List<Account>> = callbackFlow {
        val userId = requireUser()
        val listener = db.collection("users")
            .document(userId)
            .collection("accounts")
            .orderBy("accountName", Query.Direction.ASCENDING)
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

    suspend fun updateAccount(account: Account): Result<Unit> {
        return try {
            val userId = requireUser()
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

    fun getAllTransactions(accountId: String? = null): Flow<List<Transaction>> = callbackFlow {
        val userId = requireUser()
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

    // ========================================
// TRANSAZIONI / MOVIMENTI (PUBBLICI)
// ========================================

    /**
     * Ottiene le transazioni in un intervallo di date.
     */
    fun getTransactionsByDateRange(
        startDate: Timestamp,
        endDate: Timestamp
    ): Flow<List<Transaction>> = callbackFlow {
        val userId = requireUser()

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

    /**
     * Aggiunge più transazioni in batch.
     */
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

    fun getAllSubscriptions(activeOnly: Boolean = false): Flow<List<Subscription>> = callbackFlow {
        val userId = requireUser()
        var query: Query = db.collection("users")
            .document(userId)
            .collection("subscriptions") // CollectionReference è sottotipo di Query

        if (activeOnly) query = query.whereEqualTo("isActive", true)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val subs = snapshot?.documents?.mapNotNull {
                it.toObject(Subscription::class.java)
            } ?: emptyList()

            trySend(subs)
        }

        awaitClose { listener.remove() }
    }


    /**
     * Crea un nuovo abbonamento.
     */
    suspend fun createSubscription(subscription: Subscription): Result<String> {
        return try {
            val userId = requireUser()
            val docRef = db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .document()
            val s = subscription.copy(
                id = docRef.id,
                createdAt = Timestamp.now(),
                lastUpdated = Timestamp.now()
            )
            docRef.set(s).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calcola il costo totale mensile degli abbonamenti attivi.
     */
    suspend fun getTotalMonthlySubscriptionCost(): Result<Double> {
        return try {
            val userId = requireUser()
            val snapshot = db.collection("users")
                .document(userId)
                .collection("subscriptions")
                .whereEqualTo("isActive", true)
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
}
