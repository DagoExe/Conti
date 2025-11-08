package com.example.conti.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.example.conti.utils.MessageHelper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.conti.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import kotlinx.coroutines.launch

/**
 * ✨ RegisterActivity - Schermata di registrazione premium (Figma "MONIO")
 *
 * AGGIORNATO con dialog custom premium che seguono il design system MONIO
 *
 * Features:
 * - Registrazione tramite email/password
 * - Verifica email con dialog custom premium
 * - Navigazione a LoginActivity
 *
 * Design Reference: Figma "MONIO" - RegisterScreen + AccountCreatedDialog
 */
class RegisterActivity : AppCompatActivity() {

    private val authManager = AuthManager.getInstance()

    private lateinit var fullNameField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var confirmPasswordField: EditText
    private lateinit var createAccountButton: MaterialButton
    private lateinit var signInLink: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_premium)

        Log.d(TAG, "╔════════════════════════════════════════╗")
        Log.d(TAG, "   REGISTER ACTIVITY STARTED (PREMIUM UI)")
        Log.d(TAG, "╚════════════════════════════════════════╝")

        setupUI()
    }

    private fun setupUI() {
        fullNameField = findViewById(R.id.etFullName)
        emailField = findViewById(R.id.etEmail)
        passwordField = findViewById(R.id.etPassword)
        confirmPasswordField = findViewById(R.id.etConfirmPassword)
        createAccountButton = findViewById(R.id.btnCreateAccount)
        signInLink = findViewById(R.id.tvSignInLink)
        progressBar = findViewById(R.id.progressBar)

        createAccountButton.setOnClickListener {
            val name = fullNameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            if (validateInput(name, email, password, confirmPassword)) {
                signUp(email, password, name)
            }
        }

        signInLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // ====================================================
    // 🔐 REGISTRAZIONE
    // ====================================================

    private fun signUp(email: String, password: String, name: String) {
        Log.d(TAG, "🔐 Tentativo di registrazione per: $email")
        showLoading(true)

        lifecycleScope.launch {
            authManager.signUpWithEmail(email, password, name)
                .onSuccess { user ->
                    Log.d(TAG, "✅ Registrazione riuscita: ${user.email}")

                    // 🎨 USA IL NUOVO DIALOG PREMIUM
                    showEmailVerificationSentDialog(email)
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Registrazione fallita: ${e.message}", e)
                    showError(e)
                }
            showLoading(false)
        }
    }

    // ====================================================
    // ✉️ VERIFICA EMAIL - DIALOG PREMIUM
    // ====================================================

    /**
     * 🎨 Mostra dialog premium di conferma account creato
     *
     * Sostituisce il vecchio MaterialAlertDialogBuilder con un dialog custom
     * che segue il design system MONIO premium.
     */
    private fun showEmailVerificationSentDialog(email: String) {
        PremiumDialogHelper.showAccountCreatedDialog(
            context = this,
            email = email,
            onOkClick = {
                // Torna al login
                Log.d(TAG, "➡️ Utente ha confermato, reindirizzo a LoginActivity")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        )
    }

    // ====================================================
    // ⚙️ VALIDAZIONE INPUT
    // ====================================================

    private fun validateInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            MessageHelper.showError(this, "Inserisci il tuo nome completo")
            return false
        }
        if (email.isEmpty()) {
            MessageHelper.showError(this, "Inserisci la tua email")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            MessageHelper.showError(this, "Email non valida")
            return false
        }
        if (password.length < 6) {
            MessageHelper.showError(this, "La password deve avere almeno 6 caratteri")
            return false
        }
        if (password != confirmPassword) {
            MessageHelper.showError(this, "Le password non coincidono")
            return false
        }
        return true
    }

    // ====================================================
    // ⚠️ GESTIONE ERRORI
    // ====================================================

    private fun showError(exception: Throwable) {
        val message = when (exception) {
            is FirebaseAuthUserCollisionException -> "Email già registrata. Accedi invece."
            is FirebaseAuthWeakPasswordException -> "Password troppo debole."
            is FirebaseNetworkException -> "Problema di rete. Controlla la connessione."
            is FirebaseAuthInvalidCredentialsException -> "Email non valida."
            else -> "Errore: ${exception.message ?: "Sconosciuto"}"
        }

        MessageHelper.showError(this, message)
        Log.e(TAG, "Errore autenticazione: ${exception.message}", exception)
    }

    // ====================================================
    // 🔄 UI UTILITY
    // ====================================================

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        createAccountButton.isEnabled = !loading
        signInLink.isEnabled = !loading
    }



    companion object {
        private const val TAG = "RegisterActivity"
    }
}