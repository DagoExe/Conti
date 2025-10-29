package com.example.conti.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.conti.MainActivity
import com.example.conti.databinding.ActivityLoginBinding
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
 * ✅ VERSIONE MIGLIORATA con:
 * - Migliore gestione errori Firebase
 * - Logging dettagliato per debug
 * - Validazione input migliorata
 * - Messaggi utente più chiari
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authManager = AuthManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            Log.d(TAG, "✅ Utente già autenticato, reindirizzo a MainActivity")
            navigateToMain()
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

        // Reset Password
        binding.resetPasswordText.setOnClickListener {
            showResetPasswordDialog()
        }

        // Link "Non hai un account? Registrati"
        binding.toggleModeText.setOnClickListener {
            toggleLoginRegisterMode()
        }
    }

    /**
     * ✅ MIGLIORATO: Valida input per login con messaggi chiari
     */
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

    /**
     * ✅ MIGLIORATO: Valida input per registrazione
     */
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

    /**
     * ✅ MIGLIORATO: Effettua il login con logging dettagliato
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

                    Toast.makeText(
                        this@LoginActivity,
                        "✅ Benvenuto!",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToMain()
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Login fallito: ${e.message}", e)
                    showError(e)
                }

            showLoading(false)
        }
    }

    /**
     * ✅ MIGLIORATO: Effettua la registrazione con logging dettagliato
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

                    Toast.makeText(
                        this@LoginActivity,
                        "✅ Account creato con successo! Benvenuto $name!",
                        Toast.LENGTH_LONG
                    ).show()

                    navigateToMain()
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Registrazione fallita: ${e.message}", e)
                    showError(e)
                }

            showLoading(false)
        }
    }

    /**
     * ✅ MIGLIORATO: Mostra errore user-friendly basato sul tipo di eccezione Firebase
     */
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
        // ✅ FIX: Usa applicationInfo invece di BuildConfig
        if (0 != applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) {
            MaterialAlertDialogBuilder(this)
                .setTitle("🐛 Debug Info")
                .setMessage("Tipo: ${exception.javaClass.simpleName}\n\nMessaggio: ${exception.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    /**
     * Mostra dialog per reset password
     */
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

    /**
     * Invia email di reset password
     */
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

    /**
     * Toggle tra modalità login e registrazione
     */
    private fun toggleLoginRegisterMode() {
        if (binding.nameInputLayout.visibility == View.VISIBLE) {
            // Passa a modalità login
            binding.nameInputLayout.visibility = View.GONE
            binding.loginButton.visibility = View.VISIBLE
            binding.registerButton.visibility = View.GONE
            binding.toggleModeText.text = "Non hai un account? Registrati"
            binding.titleText.text = "Accedi"
            Log.d(TAG, "🔄 Modalità: LOGIN")
        } else {
            // Passa a modalità registrazione
            binding.nameInputLayout.visibility = View.VISIBLE
            binding.loginButton.visibility = View.GONE
            binding.registerButton.visibility = View.VISIBLE
            binding.toggleModeText.text = "Hai già un account? Accedi"
            binding.titleText.text = "Crea un Account"
            Log.d(TAG, "🔄 Modalità: REGISTRAZIONE")
        }
    }

    /**
     * Mostra/nascondi loading
     */
    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !loading
        binding.registerButton.isEnabled = !loading
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