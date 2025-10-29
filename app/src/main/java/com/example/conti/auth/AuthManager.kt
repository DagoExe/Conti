package com.example.conti.auth

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.conti.models.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manager per gestire l'autenticazione Firebase.
 *
 * ✅ VERSIONE AGGIORNATA con:
 * - Google Sign-In
 * - Verifica email obbligatoria
 * - Ri-invio email di verifica
 */
class AuthManager private constructor() {

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
     * ✅ NUOVO: Verifica se l'email dell'utente è verificata.
     */
    val isEmailVerified: Boolean
        get() = currentUser?.isEmailVerified ?: false

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
    // GOOGLE SIGN-IN
    // ========================================

    /**
     * ✅ NUOVO: Crea GoogleSignInClient per Google Sign-In.
     *
     * IMPORTANTE: Devi avere configurato Google Sign-In nella Firebase Console
     * e aggiunto la SHA-1 del tuo keystore.
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getWebClientId(context))
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Ottiene il Web Client ID da Firebase (necessario per Google Sign-In).
     *
     * NOTA: Il Web Client ID si trova in:
     * Firebase Console > Project Settings > General > Web API Key
     * oppure nel file google-services.json sotto "oauth_client" con type 3
     */
    private fun getWebClientId(context: Context): String {
        // ⚠️ IMPORTANTE: Sostituisci questo con il TUO Web Client ID
        // Lo trovi in google-services.json sotto "client" > "oauth_client" > "client_id" (type 3)

        // Per ora, proviamo a leggerlo dal google-services.json
        try {
            val resources = context.resources
            val packageName = context.packageName
            val resId = resources.getIdentifier(
                "default_web_client_id",
                "string",
                packageName
            )
            return context.getString(resId)
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "❌ Impossibile ottenere Web Client ID", e)
            throw IllegalStateException(
                "Web Client ID non trovato. Assicurati di aver configurato Google Sign-In " +
                        "nella Firebase Console e di aver aggiunto google-services.json al progetto."
            )
        }
    }

    /**
     * ✅ NUOVO: Autentica con Google usando l'account selezionato.
     *
     * @param account Account Google selezionato dall'utente
     * @return Result con l'utente Firebase autenticato
     */
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthManager", "🔐 Autenticazione Google in corso...")

            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google Sign-In fallito")

            android.util.Log.d("AuthManager", "✅ Google Sign-In riuscito: ${user.uid}")

            // Crea profilo utente
            createUserProfile(user, displayName = account.displayName)

            // Aggiorna ultimo login
            updateLastLogin(user.uid)

            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "❌ Errore Google Sign-In", e)
            Result.failure(e)
        }
    }

    // ========================================
    // VERIFICA EMAIL
    // ========================================

    /**
     * ✅ NUOVO: Invia email di verifica all'utente corrente.
     */
    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = currentUser ?: throw Exception("Nessun utente autenticato")

            if (user.isEmailVerified) {
                return Result.failure(Exception("Email già verificata"))
            }

            user.sendEmailVerification().await()

            android.util.Log.d("AuthManager", "✅ Email di verifica inviata a: ${user.email}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "❌ Errore invio email verifica", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ NUOVO: Ricarica i dati dell'utente (per aggiornare isEmailVerified).
     */
    suspend fun reloadUser(): Result<Unit> {
        return try {
            val user = currentUser ?: throw Exception("Nessun utente autenticato")
            user.reload().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

            // Crea profilo utente
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
     *
     * ✅ AGGIORNATO: Invia automaticamente email di verifica dopo registrazione.
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

            // ✅ NUOVO: Invia email di verifica
            user.sendEmailVerification().await()
            android.util.Log.d("AuthManager", "✅ Email di verifica inviata a: $email")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Effettua login con email e password.
     *
     * ✅ NOTA: Dopo il login, controllare isEmailVerified prima di permettere accesso.
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