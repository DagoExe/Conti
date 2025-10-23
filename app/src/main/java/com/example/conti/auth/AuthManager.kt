package com.example.conti.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.example.conti.models.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manager per gestire l'autenticazione Firebase.
 *
 * Fornisce metodi per:
 * - Login anonimo (per testing/sviluppo)
 * - Login con email/password
 * - Registrazione
 * - Logout
 * - Osservazione stato autenticazione
 */
class AuthManager {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Utente corrente (può essere null se non autenticato).
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Verifica se l'utente è autenticato.
     */
    val isAuthenticated: Boolean
        get() = currentUser != null

    /**
     * Flow che emette lo stato di autenticazione.
     */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // ========================================
    // LOGIN ANONIMO
    // ========================================

    /**
     * Effettua login anonimo.
     * Utile per testing o per permettere l'uso dell'app senza registrazione.
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("Login anonimo fallito")

            // Crea profilo utente in Firestore
            createUserProfile(user, isAnonymous = true)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // LOGIN CON EMAIL/PASSWORD
    // ========================================

    /**
     * Registra un nuovo utente con email e password.
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<FirebaseUser> {
        return try {
            // Valida email e password
            if (!isValidEmail(email)) {
                return Result.failure(Exception("Email non valida"))
            }
            if (!isValidPassword(password)) {
                return Result.failure(Exception("Password troppo corta (minimo 6 caratteri)"))
            }

            // Crea utente
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Registrazione fallita")

            // Crea profilo utente
            createUserProfile(user, displayName = displayName)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Effettua login con email e password.
     */
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Login fallito")

            // Aggiorna ultimo login
            updateLastLogin(user.uid)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // LOGOUT
    // ========================================

    /**
     * Effettua logout.
     */
    fun signOut() {
        auth.signOut()
    }

    // ========================================
    // GESTIONE PROFILO UTENTE
    // ========================================

    /**
     * Crea il profilo utente in Firestore.
     */
    private suspend fun createUserProfile(
        user: FirebaseUser,
        displayName: String? = null,
        isAnonymous: Boolean = false
    ) {
        try {
            val profile = UserProfile(
                id = user.uid,
                email = if (isAnonymous) "anonymous@local" else (user.email ?: ""),
                displayName = displayName ?: user.displayName,
                photoUrl = user.photoUrl?.toString()
            )

            firestore.collection("users")
                .document(user.uid)
                .set(profile)
                .await()

            android.util.Log.d("AuthManager", "✅ Profilo utente creato")
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "❌ Errore creazione profilo", e)
        }
    }

    /**
     * Aggiorna la data dell'ultimo login.
     */
    private suspend fun updateLastLogin(userId: String) {
        try {
            firestore.collection("users")
                .document(userId)
                .update("lastLogin", com.google.firebase.Timestamp.now())
                .await()
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "❌ Errore aggiornamento lastLogin", e)
        }
    }

    // ========================================
    // VALIDAZIONE
    // ========================================

    /**
     * Valida un indirizzo email.
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Valida una password (minimo 6 caratteri).
     */
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    // ========================================
    // RESET PASSWORD
    // ========================================

    /**
     * Invia email per reset password.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // ELIMINA ACCOUNT
    // ========================================

    /**
     * Elimina l'account corrente.
     * ATTENZIONE: Elimina anche tutti i dati Firestore.
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = currentUser ?: throw Exception("Nessun utente autenticato")

            // Elimina dati Firestore
            deleteUserData(user.uid)

            // Elimina account Firebase Auth
            user.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina tutti i dati dell'utente da Firestore.
     */
    private suspend fun deleteUserData(userId: String) {
        try {
            // Elimina tutti gli account
            val accountsSnapshot = firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .get()
                .await()

            accountsSnapshot.documents.forEach { it.reference.delete().await() }

            // Elimina tutte le transazioni
            val transactionsSnapshot = firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .get()
                .await()

            transactionsSnapshot.documents.forEach { it.reference.delete().await() }

            // Elimina tutti gli abbonamenti
            val subscriptionsSnapshot = firestore.collection("users")
                .document(userId)
                .collection("subscriptions")
                .get()
                .await()

            subscriptionsSnapshot.documents.forEach { it.reference.delete().await() }

            // Elimina profilo utente
            firestore.collection("users")
                .document(userId)
                .delete()
                .await()

            android.util.Log.d("AuthManager", "✅ Dati utente eliminati")
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "❌ Errore eliminazione dati", e)
        }
    }

    companion object {
        /**
         * Istanza singleton di AuthManager.
         */
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(): AuthManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthManager()
                INSTANCE = instance
                instance
            }
        }
    }
}