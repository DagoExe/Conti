package com.example.conti.auth

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
 * ✅ VERSIONE CORRETTA con Web Client ID hardcoded
 */
class AuthManager private constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isAuthenticated: Boolean
        get() = currentUser != null

    val isEmailVerified: Boolean
        get() = currentUser?.isEmailVerified ?: false

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // ========================================
    // GOOGLE SIGN-IN - VERSIONE CORRETTA
    // ========================================

    /**
     * ✅ VERSIONE CORRETTA: Usa Web Client ID hardcoded
     *
     * IMPORTANTE: Dopo aver aggiunto la SHA-1 in Firebase Console,
     * questo ID dovrebbe funzionare correttamente.
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        // ✅ FIX: Usa direttamente il Web Client ID dal tuo google-services.json
        // Questo è il client_id di tipo 3 (Web client)
        val webClientId = "404921205660-eljajs4jgjjdl91ebis8on4mghgqib85.apps.googleusercontent.com"

        android.util.Log.d("AuthManager", "📱 Configurazione Google Sign-In")
        android.util.Log.d("AuthManager", "   Web Client ID: $webClientId")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Autentica con Google usando l'account selezionato.
     */
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthManager", "🔐 Autenticazione Google in corso...")
            android.util.Log.d("AuthManager", "   Account email: ${account.email}")
            android.util.Log.d("AuthManager", "   ID Token presente: ${account.idToken != null}")
            android.util.Log.d("AuthManager", "   ID Token length: ${account.idToken?.length ?: 0}")

            if (account.idToken == null) {
                android.util.Log.e("AuthManager", "❌ ID Token mancante!")
                android.util.Log.e("AuthManager", "   Possibili cause:")
                android.util.Log.e("AuthManager", "   1. SHA-1 non configurata in Firebase")
                android.util.Log.e("AuthManager", "   2. Web Client ID errato")
                android.util.Log.e("AuthManager", "   3. Package name non corretto")
                return Result.failure(Exception(
                    "Google Sign-In fallito: ID Token mancante.\n\n" +
                            "Verifica che:\n" +
                            "1. La SHA-1 sia stata aggiunta in Firebase Console\n" +
                            "2. Il file google-services.json sia aggiornato\n" +
                            "3. L'app sia stata ricompilata dopo le modifiche"
                ))
            }

            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            android.util.Log.d("AuthManager", "🔑 Credential creata, autenticazione con Firebase...")

            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google Sign-In fallito: utente null")

            android.util.Log.d("AuthManager", "✅ Google Sign-In riuscito!")
            android.util.Log.d("AuthManager", "   User ID: ${user.uid}")
            android.util.Log.d("AuthManager", "   Email: ${user.email}")
            android.util.Log.d("AuthManager", "   Display Name: ${user.displayName}")

            // Crea o aggiorna profilo utente
            createUserProfile(user, displayName = account.displayName)

            // Aggiorna ultimo login
            updateLastLogin(user.uid)

            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "❌ Errore Google Sign-In", e)
            android.util.Log.e("AuthManager", "   Messaggio: ${e.message}")
            android.util.Log.e("AuthManager", "   Tipo: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    // ========================================
    // VERIFICA EMAIL
    // ========================================

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

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("Login anonimo fallito")

            createUserProfile(user, isAnonymous = true)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // LOGIN CON EMAIL/PASSWORD
    // ========================================

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<FirebaseUser> {
        return try {
            if (!isValidEmail(email)) {
                return Result.failure(Exception("Email non valida"))
            }
            if (!isValidPassword(password)) {
                return Result.failure(Exception("Password troppo corta (minimo 6 caratteri)"))
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Registrazione fallita")

            createUserProfile(user, displayName = displayName)

            // Invia email di verifica
            user.sendEmailVerification().await()
            android.util.Log.d("AuthManager", "✅ Email di verifica inviata a: $email")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Login fallito")

            updateLastLogin(user.uid)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // LOGOUT
    // ========================================

    fun signOut() {
        auth.signOut()
    }

    // ========================================
    // GESTIONE PROFILO UTENTE
    // ========================================

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

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    // ========================================
    // RESET PASSWORD
    // ========================================

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

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = currentUser ?: throw Exception("Nessun utente autenticato")

            deleteUserData(user.uid)
            user.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deleteUserData(userId: String) {
        try {
            val userDocRef = firestore.collection("users").document(userId)

            // Elimina sottocollezioni
            val subcollections = listOf("accounts", "transactions", "subscriptions")

            for (subcollection in subcollections) {
                val snapshot = userDocRef.collection(subcollection).get().await()
                snapshot.documents.forEach { it.reference.delete().await() }
            }

            // Elimina documento principale
            userDocRef.delete().await()

            android.util.Log.d("AuthManager", "✅ Dati utente eliminati")
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "❌ Errore eliminazione dati", e)
        }
    }

    companion object {
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