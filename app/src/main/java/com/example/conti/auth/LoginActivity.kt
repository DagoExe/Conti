package com.example.conti.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.conti.MainActivity
import com.example.conti.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch

/**
 * Activity di login / registrazione
 *
 * ✅ VERSIONE AGGIORNATA con:
 * - Google Sign-In
 * - Verifica email obbligatoria
 * - Re-invio email di verifica
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authManager = AuthManager.getInstance()

    /**
     * ✅ NUOVO: Launcher per Google Sign-In
     */
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    signInWithGoogle(account)
                } else {
                    showError(Exception("Google Sign-In fallito"))
                }
            } catch (e: ApiException) {
                Log.e(TAG, "❌ Google Sign-In fallito: ${e.statusCode}", e)
                showError(Exception("Google Sign-In fallito: ${e.message}"))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("404921205660-eljajs4jgjjdl91ebis8on4mghgqib85.apps.googleusercontent.com")
                .requestEmail()
                .build()

            Log.d(TAG, "✅ GoogleSignInOptions configurato correttamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore configurazione GoogleSignInOptions", e)
        }

        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "   LOGIN ACTIVITY STARTED")
        Log.d(TAG, "═══════════════════════════════════════")

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkIfAlreadyLoggedIn()
    }

    /**
     * Controlla se l'utente è già autenticato
     */
    private fun checkIfAlreadyLoggedIn() {
        if (authManager.isAuthenticated) {
            // ✅ NUOVO: Controlla anche se l'email è verificata
            if (authManager.isEmailVerified || authManager.currentUser?.isAnonymous == true) {
                Log.d(TAG, "✅ Utente già autenticato e verificato, reindirizzo a MainActivity")
                navigateToMain()
            } else {
                Log.d(TAG, "⚠️ Utente autenticato ma email non verificata")
                showEmailVerificationDialog()
            }
        } else {
            Log.d(TAG, "ℹ️ Nessun utente autenticato, mostro form login")
        }
    }

    private fun setupUI() {
        // Pulsante Login
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (validateLoginInput(email, password)) {
                signIn(email, password)
            }
        }

        // Pulsante Registrazione
        binding.registerButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val name = binding.nameInput.text.toString().trim()

            if (validateRegisterInput(email, password, name)) {
                signUp(email, password, name)
            }
        }

        // ✅ NUOVO: Pulsante Google Sign-In
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogleClick()
        }

        // Reset Password
        binding.resetPasswordText.setOnClickListener {
            showResetPasswordDialog()
        }

        // Link "Non hai un account? Registrati"
        binding.toggleModeText.setOnClickListener {
            toggleLoginRegisterMode()
        }
    }

    // ========================================
    // ✅ NUOVO: GOOGLE SIGN-IN
    // ========================================

    /**
     * Avvia il flusso Google Sign-In
     */
    private fun signInWithGoogleClick() {
        Log.d(TAG, "🔐 Avvio Google Sign-In...")
        showLoading(true)

        try {
            val googleSignInClient = authManager.getGoogleSignInClient(this)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore avvio Google Sign-In", e)
            showError(e)
            showLoading(false)
        }
    }

    /**
     * Autentica con Google
     */
    private fun signInWithGoogle(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        Log.d(TAG, "🔐 Autenticazione Google per: ${account.email}")
        Log.d(TAG, "   ID Token presente: ${account.idToken != null}")

        // ✅ Verifica che l'ID Token sia presente
        if (account.idToken == null) {
            Log.e(TAG, "❌ ID Token mancante! Configurazione OAuth non corretta.")
            Toast.makeText(
                this,
                "❌ Errore configurazione Google Sign-In. Contatta lo sviluppatore.",
                Toast.LENGTH_LONG
            ).show()
            showLoading(false)
            return
        }

        lifecycleScope.launch {
            authManager.signInWithGoogle(account)
                .onSuccess { user ->
                    Log.d(TAG, "✅ Google Sign-In riuscito!")
                    Log.d(TAG, "   User ID: ${user.uid}")
                    Log.d(TAG, "   Email: ${user.email}")

                    Toast.makeText(
                        this@LoginActivity,
                        "✅ Benvenuto ${user.displayName}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToMain()
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Google Sign-In fallito: ${e.message}", e)
                    showError(e)
                }

            showLoading(false)
        }
    }

    // ========================================
    // VALIDAZIONE INPUT
    // ========================================

    private fun validateLoginInput(email: String, password: String): Boolean {
        // Reset errori precedenti
        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null

        // Valida email
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "📧 Inserisci la tua email"
            binding.emailInput.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "❌ Email non valida"
            binding.emailInput.requestFocus()
            return false
        }

        // Valida password
        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "🔒 Inserisci la password"
            binding.passwordInput.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.passwordInputLayout.error = "⚠️ La password deve avere almeno 6 caratteri"
            binding.passwordInput.requestFocus()
            return false
        }

        return true
    }

    private fun validateRegisterInput(email: String, password: String, name: String): Boolean {
        // Reset errori precedenti
        binding.nameInputLayout.error = null
        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null

        // Valida nome
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "👤 Inserisci il tuo nome"
            binding.nameInput.requestFocus()
            return false
        }

        if (name.length < 2) {
            binding.nameInputLayout.error = "⚠️ Il nome deve avere almeno 2 caratteri"
            binding.nameInput.requestFocus()
            return false
        }

        // Valida email
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "📧 Inserisci la tua email"
            binding.emailInput.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "❌ Email non valida"
            binding.emailInput.requestFocus()
            return false
        }

        // Valida password
        if (password.length < 6) {
            binding.passwordInputLayout.error = "🔒 La password deve avere almeno 6 caratteri"
            binding.passwordInput.requestFocus()
            return false
        }

        // Suggerisci password forte
        if (password.length < 8) {
            binding.passwordInputLayout.helperText = "💡 Suggerimento: usa almeno 8 caratteri per maggiore sicurezza"
        }

        return true
    }

    // ========================================
    // LOGIN / REGISTRAZIONE
    // ========================================

    /**
     * ✅ AGGIORNATO: Effettua il login e controlla verifica email
     */
    private fun signIn(email: String, password: String) {
        Log.d(TAG, "🔐 Tentativo di login per: $email")
        showLoading(true)

        lifecycleScope.launch {
            authManager.signInWithEmail(email, password)
                .onSuccess { user ->
                    Log.d(TAG, "✅ Login riuscito!")
                    Log.d(TAG, "   User ID: ${user.uid}")
                    Log.d(TAG, "   Email: ${user.email}")
                    Log.d(TAG, "   Email verificata: ${user.isEmailVerified}")

                    // ✅ NUOVO: Controlla se l'email è verificata
                    if (user.isEmailVerified) {
                        Toast.makeText(
                            this@LoginActivity,
                            "✅ Benvenuto!",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToMain()
                    } else {
                        Log.w(TAG, "⚠️ Email non verificata")
                        showEmailVerificationDialog()
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Login fallito: ${e.message}", e)
                    showError(e)
                }

            showLoading(false)
        }
    }

    /**
     * ✅ AGGIORNATO: Effettua la registrazione e invia email di verifica
     */
    private fun signUp(email: String, password: String, name: String) {
        Log.d(TAG, "📝 Tentativo di registrazione per: $email")
        showLoading(true)

        lifecycleScope.launch {
            authManager.signUpWithEmail(email, password, name)
                .onSuccess { user ->
                    Log.d(TAG, "✅ Registrazione riuscita!")
                    Log.d(TAG, "   User ID: ${user.uid}")
                    Log.d(TAG, "   Email: ${user.email}")
                    Log.d(TAG, "   Nome: $name")

                    // ✅ NUOVO: Mostra dialog per verifica email
                    showEmailVerificationSentDialog(email)
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Registrazione fallita: ${e.message}", e)
                    showError(e)
                }

            showLoading(false)
        }
    }

    // ========================================
    // ✅ NUOVO: VERIFICA EMAIL
    // ========================================

    /**
     * Mostra dialog che spiega che l'email non è verificata
     */
    private fun showEmailVerificationDialog() {
        val email = authManager.currentUser?.email ?: "la tua email"

        MaterialAlertDialogBuilder(this)
            .setTitle("📧 Verifica Email Richiesta")
            .setMessage(
                "Per accedere all'app devi verificare la tua email.\n\n" +
                        "Ti abbiamo inviato un'email di verifica a:\n$email\n\n" +
                        "Controlla la tua casella di posta (anche nello spam) e clicca sul link per verificare."
            )
            .setPositiveButton("Ho verificato") { _, _ ->
                checkEmailVerification()
            }
            .setNegativeButton("Ri-invia email") { _, _ ->
                resendVerificationEmail()
            }
            .setNeutralButton("Logout") { _, _ ->
                authManager.signOut()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Mostra dialog dopo registrazione per informare dell'email inviata
     */
    private fun showEmailVerificationSentDialog(email: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("✅ Account Creato!")
            .setMessage(
                "Ti abbiamo inviato un'email di verifica a:\n$email\n\n" +
                        "Prima di poter accedere, devi verificare la tua email.\n\n" +
                        "Controlla la tua casella di posta (anche nello spam) e clicca sul link per verificare."
            )
            .setPositiveButton("OK") { _, _ ->
                // Torna al form di login
                toggleToLoginMode()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Controlla se l'email è stata verificata
     */
    private fun checkEmailVerification() {
        showLoading(true)

        lifecycleScope.launch {
            // Ricarica i dati utente per aggiornare isEmailVerified
            authManager.reloadUser()

            if (authManager.isEmailVerified) {
                Log.d(TAG, "✅ Email verificata!")

                Toast.makeText(
                    this@LoginActivity,
                    "✅ Email verificata con successo!",
                    Toast.LENGTH_LONG
                ).show()

                navigateToMain()
            } else {
                Log.w(TAG, "⚠️ Email non ancora verificata")

                MaterialAlertDialogBuilder(this@LoginActivity)
                    .setTitle("⚠️ Email Non Verificata")
                    .setMessage(
                        "Non abbiamo ancora ricevuto la conferma della verifica.\n\n" +
                                "Controlla la tua email e clicca sul link, poi riprova."
                    )
                    .setPositiveButton("Riprova") { _, _ ->
                        checkEmailVerification()
                    }
                    .setNegativeButton("Ri-invia email") { _, _ ->
                        resendVerificationEmail()
                    }
                    .show()
            }

            showLoading(false)
        }
    }

    /**
     * Ri-invia email di verifica
     */
    private fun resendVerificationEmail() {
        showLoading(true)

        lifecycleScope.launch {
            authManager.sendEmailVerification()
                .onSuccess {
                    Log.d(TAG, "✅ Email di verifica ri-inviata")

                    Toast.makeText(
                        this@LoginActivity,
                        "✅ Email di verifica inviata! Controlla la tua casella di posta.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Errore ri-invio email", e)

                    Toast.makeText(
                        this@LoginActivity,
                        "❌ Errore: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            showLoading(false)
        }
    }

    // ========================================
    // GESTIONE ERRORI
    // ========================================

    private fun showError(exception: Throwable) {
        val message = when (exception) {
            // Errori di autenticazione Firebase
            is FirebaseAuthInvalidUserException -> {
                Log.e(TAG, "   Tipo errore: Utente non trovato")
                "❌ Account non trovato. Verifica l'email o registrati."
            }
            is FirebaseAuthInvalidCredentialsException -> {
                Log.e(TAG, "   Tipo errore: Credenziali non valide")
                "❌ Email o password errati. Riprova."
            }
            is FirebaseAuthUserCollisionException -> {
                Log.e(TAG, "   Tipo errore: Email già in uso")
                "⚠️ Questa email è già registrata. Prova ad accedere invece."
            }
            is FirebaseAuthWeakPasswordException -> {
                Log.e(TAG, "   Tipo errore: Password debole")
                "🔒 Password troppo debole. Usa almeno 6 caratteri."
            }
            is FirebaseNetworkException -> {
                Log.e(TAG, "   Tipo errore: Problema di rete")
                "🌐 Errore di connessione. Controlla la tua rete e riprova."
            }

            // Errori generici
            else -> {
                Log.e(TAG, "   Tipo errore: Generico - ${exception.javaClass.simpleName}")
                when {
                    exception.message?.contains("password", ignoreCase = true) == true ->
                        "❌ Password errata. Riprova o usa 'Password dimenticata'."
                    exception.message?.contains("email", ignoreCase = true) == true ->
                        "❌ Email non valida o non trovata."
                    exception.message?.contains("network", ignoreCase = true) == true ->
                        "🌐 Errore di connessione. Controlla la rete."
                    else ->
                        "❌ Errore: ${exception.message ?: "Sconosciuto"}"
                }
            }
        }

        // Mostra errore all'utente
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // In debug, mostra anche dialog con dettagli tecnici
        if (0 != applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) {
            MaterialAlertDialogBuilder(this)
                .setTitle("🐛 Debug Info")
                .setMessage("Tipo: ${exception.javaClass.simpleName}\n\nMessaggio: ${exception.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // ========================================
    // RESET PASSWORD
    // ========================================

    private fun showResetPasswordDialog() {
        val email = binding.emailInput.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "📧 Inserisci prima la tua email nel campo sopra", Toast.LENGTH_SHORT).show()
            binding.emailInput.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "❌ Email non valida", Toast.LENGTH_SHORT).show()
            binding.emailInput.requestFocus()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("🔑 Reset Password")
            .setMessage("Inviare email di reset password a:\n\n$email?")
            .setPositiveButton("Invia") { _, _ ->
                resetPassword(email)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun resetPassword(email: String) {
        Log.d(TAG, "📧 Invio email reset password a: $email")
        showLoading(true)

        lifecycleScope.launch {
            authManager.sendPasswordResetEmail(email)
                .onSuccess {
                    Log.d(TAG, "✅ Email di reset inviata")
                    Toast.makeText(
                        this@LoginActivity,
                        "✅ Email inviata! Controlla la tua casella di posta.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Errore invio email reset", e)
                    Toast.makeText(
                        this@LoginActivity,
                        "❌ Errore: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            showLoading(false)
        }
    }

    // ========================================
    // UI UTILITY
    // ========================================

    /**
     * Toggle tra modalità login e registrazione
     */
    private fun toggleLoginRegisterMode() {
        if (binding.nameInputLayout.visibility == View.VISIBLE) {
            // Passa a modalità login
            toggleToLoginMode()
        } else {
            // Passa a modalità registrazione
            toggleToRegisterMode()
        }
    }

    private fun toggleToLoginMode() {
        binding.nameInputLayout.visibility = View.GONE
        binding.loginButton.visibility = View.VISIBLE
        binding.registerButton.visibility = View.GONE
        binding.toggleModeText.text = "Non hai un account? Registrati"
        binding.titleText.text = "Accedi"
        Log.d(TAG, "🔄 Modalità: LOGIN")
    }

    private fun toggleToRegisterMode() {
        binding.nameInputLayout.visibility = View.VISIBLE
        binding.loginButton.visibility = View.GONE
        binding.registerButton.visibility = View.VISIBLE
        binding.toggleModeText.text = "Hai già un account? Accedi"
        binding.titleText.text = "Crea un Account"
        Log.d(TAG, "🔄 Modalità: REGISTRAZIONE")
    }

    /**
     * Mostra/nascondi loading
     */
    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !loading
        binding.registerButton.isEnabled = !loading
        binding.googleSignInButton.isEnabled = !loading
        binding.toggleModeText.isEnabled = !loading
        binding.resetPasswordText.isEnabled = !loading
    }

    /**
     * Naviga alla MainActivity
     */
    private fun navigateToMain() {
        Log.d(TAG, "➡️ Navigazione a MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}