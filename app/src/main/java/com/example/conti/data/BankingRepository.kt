package com.example.conti.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.conti.models.Account
import com.example.conti.models.Transaction
import kotlinx.coroutines.tasks.await

class BankingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Ottieni ID utente corrente
    private fun getUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Utente non autenticato")
    }

    // === OPERAZIONI ACCOUNT ===

    // Crea nuovo account
    suspend fun createAccount(account: Account): Result<String> {
        return try {
            val userId = getUserId()
            val docRef = db.collection("users")
                .document(userId)
                .collection("accounts")
                .document(account.accountType.name) // "buddybank" o "hype"

            docRef.set(account).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Leggi tutti gli account
    suspend fun getAccounts(): Result<List<Account>> {
        return try {
            val userId = getUserId()
            val snapshot = db.collection("users")
                .document(userId)
                .collection("accounts")
                .get()
                .await()

            val accounts = snapshot.documents.mapNotNull {
                it.toObject(Account::class.java)
            }
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Aggiorna saldo account
    suspend fun updateAccountBalance(
        accountType: String,
        newBalance: Double
    ): Result<Unit> {
        return try {
            val userId = getUserId()
            db.collection("users")
                .document(userId)
                .collection("accounts")
                .document(accountType)
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

    // === OPERAZIONI TRANSAZIONI ===

    // Aggiungi transazione
    suspend fun addTransaction(transaction: Transaction): Result<String> {
        return try {
            val userId = getUserId()

            // Aggiungi transazione
            val docRef = db.collection("users")
                .document(userId)
                .collection("transactions")
                .document()

            docRef.set(transaction.copy(id = docRef.id)).await()

            // Aggiorna saldo account
            updateBalanceAfterTransaction(transaction)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Aggiorna saldo dopo transazione
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

    // Leggi transazioni con filtri
    suspend fun getTransactions(
        accountType: String? = null, // null = tutti i conti
        limit: Int = 50
    ): Result<List<Transaction>> {
        return try {
            val userId = getUserId()
            var query: Query = db.collection("users")
                .document(userId)
                .collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)

            // Filtra per conto se specificato
            if (accountType != null) {
                query = query.whereEqualTo("accountId", accountType)
            }

            val snapshot = query.limit(limit.toLong()).get().await()

            val transactions = snapshot.documents.mapNotNull {
                it.toObject(Transaction::class.java)
            }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Elimina transazione
    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            val userId = getUserId()

            // Prima leggi la transazione per aggiornare il saldo
            val transactionRef = db.collection("users")
                .document(userId)
                .collection("transactions")
                .document(transactionId)

            val transaction = transactionRef.get().await()
                .toObject(Transaction::class.java)
                ?: throw Exception("Transazione non trovata")

            // Inverti l'importo per ripristinare il saldo
            updateBalanceAfterTransaction(
                transaction.copy(amount = -transaction.amount)
            )

            // Elimina la transazione
            transactionRef.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === STATISTICHE ===

    // Calcola totale per categoria
    suspend fun getTotalByCategory(
        month: Int,
        year: Int
    ): Result<Map<String, Double>> {
        return try {
            val userId = getUserId()

            // Calcola timestamp inizio e fine mese
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month - 1, 1, 0, 0, 0)
            val startTimestamp = Timestamp(calendar.time)

            calendar.set(year, month, 1, 0, 0, 0)
            val endTimestamp = Timestamp(calendar.time)

            val snapshot = db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereGreaterThanOrEqualTo("date", startTimestamp)
                .whereLessThan("date", endTimestamp)
                .get()
                .await()

            val totals = mutableMapOf<String, Double>()

            snapshot.documents.forEach { doc ->
                val transaction = doc.toObject(Transaction::class.java)
                if (transaction != null && transaction.type == "expense") {
                    val category = transaction.category
                    totals[category] = totals.getOrDefault(category, 0.0) +
                            Math.abs(transaction.amount)
                }
            }

            Result.success(totals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}