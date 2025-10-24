package com.example.conti.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.example.conti.data.dto.AccountDto
import com.example.conti.models.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository per gestire gli Account in Firestore.
 * Singleton pattern per evitare multiple istanze.
 */
class AccountRepository private constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserAccountsCollection() =
        firestore.collection("users")
            .document(auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated"))
            .collection("accounts")

    /**
     * Ottiene tutti gli account dell'utente come Flow (aggiornamenti in tempo reale)
     */
    fun getAccountsFlow(): Flow<List<Account>> {
        return getUserAccountsCollection()
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject<AccountDto>()?.toDomain(doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("AccountRepository", "Errore parsing account ${doc.id}", e)
                        null
                    }
                }
            }
    }

    /**
     * Ottiene tutti gli account (snapshot singolo, no real-time)
     */
    suspend fun getAccounts(): Result<List<Account>> {
        return try {
            val snapshot = getUserAccountsCollection().get().await()
            val accounts = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject<AccountDto>()?.toDomain(doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("AccountRepository", "Errore parsing account ${doc.id}", e)
                    null
                }
            }
            Result.success(accounts)
        } catch (e: Exception) {
            android.util.Log.e("AccountRepository", "Errore getAccounts", e)
            Result.failure(e)
        }
    }

    /**
     * Ottiene un singolo account per ID
     */
    suspend fun getAccount(accountId: String): Result<Account> {
        return try {
            val doc = getUserAccountsCollection()
                .document(accountId)
                .get()
                .await()

            if (doc.exists()) {
                val accountDto = doc.toObject<AccountDto>()
                val account = accountDto?.toDomain(doc.id)
                if (account != null) {
                    Result.success(account)
                } else {
                    Result.failure(Exception("Account data is invalid"))
                }
            } else {
                Result.failure(Exception("Account not found"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AccountRepository", "Errore getAccount", e)
            Result.failure(e)
        }
    }

    /**
     * Crea o aggiorna un account
     */
    suspend fun saveAccount(account: Account): Result<Unit> {
        return try {
            if (!account.isValid()) {
                return Result.failure(IllegalArgumentException("Account data is invalid"))
            }

            val accountDto = AccountDto.fromDomain(account)
            getUserAccountsCollection()
                .document(account.accountId)
                .set(accountDto)
                .await()

            android.util.Log.d("AccountRepository", "✅ Account salvato: ${account.accountId}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AccountRepository", "❌ Errore saveAccount", e)
            Result.failure(e)
        }
    }

    /**
     * Aggiorna il saldo di un account
     */
    suspend fun updateBalance(accountId: String, newBalance: Double): Result<Unit> {
        return try {
            getUserAccountsCollection()
                .document(accountId)
                .update(
                    mapOf(
                        "balance" to newBalance,
                        "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                .await()

            android.util.Log.d("AccountRepository", "✅ Saldo aggiornato per $accountId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AccountRepository", "❌ Errore updateBalance", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina un account
     */
    suspend fun deleteAccount(accountId: String): Result<Unit> {
        return try {
            getUserAccountsCollection()
                .document(accountId)
                .delete()
                .await()

            android.util.Log.d("AccountRepository", "✅ Account eliminato: $accountId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AccountRepository", "❌ Errore deleteAccount", e)
            Result.failure(e)
        }
    }

    /**
     * Calcola il saldo totale di tutti gli account
     */
    suspend fun getTotalBalance(): Result<Double> {
        return try {
            val snapshot = getUserAccountsCollection().get().await()
            val total = snapshot.documents.sumOf { doc ->
                doc.toObject<AccountDto>()?.balance ?: 0.0
            }
            Result.success(total)
        } catch (e: Exception) {
            android.util.Log.e("AccountRepository", "❌ Errore getTotalBalance", e)
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AccountRepository? = null

        fun getInstance(): AccountRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AccountRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}