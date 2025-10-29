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
import kotlinx.coroutines.launch

/**
 * Activity di login / registrazione
 *
 * ✅ Supporta login con email/password
 * ✅ Supporta registrazione
 * ✅ Reset password
 * ✅ Validazione input
 * ✅ Gestione errori user-friendly
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authManager = AuthManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            Log.d("LoginActivity", "✅ Utente già autenticato, reindirizzo a MainActivity")
            navigateToMain()
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
     * Valida input per login
     */
    private fun validateLoginInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Inserisci l'email"
            return false
        }
        binding.emailInputLayout.error = null

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Email non valida"
            return false
        }
        binding.emailInputLayout.error = null

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Inserisci la password"
            return false
        }
        binding.passwordInputLayout.error = null

        return true
    }

    /**
     * Valida input per registrazione
     */
    private fun validateRegisterInput(email: String, password: String, name: String): Boolean {
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Inserisci il tuo nome"
            return false
        }
        binding.nameInputLayout.error = null

        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Inserisci l'email"
            return false
        }
        binding.emailInputLayout.error = null

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Email non valida"
            return false
        }
        binding.emailInputLayout.error = null

        if (password.length < 6) {
            binding.passwordInputLayout.error = "La password deve essere di almeno 6 caratteri"
            return false
        }
        binding.passwordInputLayout.error = null

        return true
    }

    /**
     * Effettua il login
     */
    private fun signIn(email: String, password: String) {
        showLoading(true)

        lifecycleScope.launch {
            authManager.signInWithEmail(email, password)
                .onSuccess { user ->
                    Log.d("LoginActivity", "✅ Login riuscito: ${user.uid}")
                    Toast.makeText(
                        this@LoginActivity,
                        "✅ Benvenuto!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                }
                .onFailure { e ->
                    Log.e("LoginActivity", "❌ Login fallito", e)
                    showError(e)
                }

            showLoading(false)
        }
    }

    /**
     * Effettua la registrazione
     */
    private fun signUp(email: String, password: String, name: String) {
        showLoading(true)

        lifecycleScope.launch {
            authManager.signUpWithEmail(email, password, name)
                .onSuccess { user ->
                    Log.d("LoginActivity", "✅ Registrazione riuscita: ${user.uid}")
                    Toast.makeText(
                        this@LoginActivity,
                        "✅ Account creato con successo!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                }
                .onFailure { e ->
                    Log.e("LoginActivity", "❌ Registrazione fallita", e)
                    showError(e)
                }

            showLoading(false)
        }
    }

    /**
     * Mostra dialog per reset password
     */
    private fun showResetPasswordDialog() {
        val email = binding.emailInput.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Inserisci la tua email", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setMessage("Inviare email di reset password a:\n$email?")
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
        showLoading(true)

        lifecycleScope.launch {
            authManager.sendPasswordResetEmail(email)
                .onSuccess {
                    Toast.makeText(
                        this@LoginActivity,
                        "✅ Email di reset inviata a $email",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onFailure { e ->
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
        } else {
            // Passa a modalità registrazione
            binding.nameInputLayout.visibility = View.VISIBLE
            binding.loginButton.visibility = View.GONE
            binding.registerButton.visibility = View.VISIBLE
            binding.toggleModeText.text = "Hai già un account? Accedi"
            binding.titleText.text = "Crea un Account"
        }
    }

    /**
     * Mostra/nascondi loading
     */
    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !loading
        binding.registerButton.isEnabled = !loading
    }

    /**
     * Mostra errore user-friendly
     */
    private fun showError(exception: Throwable) {
        val message = when {
            exception.message?.contains("password") == true ->
                "Password errata. Riprova."
            exception.message?.contains("email") == true ->
                "Email non trovata o non valida."
            exception.message?.contains("network") == true ->
                "Errore di connessione. Controlla la rete."
            exception.message?.contains("user-not-found") == true ->
                "Utente non trovato. Verifica l'email."
            exception.message?.contains("wrong-password") == true ->
                "Password errata."
            exception.message?.contains("email-already-in-use") == true ->
                "Questa email è già registrata."
            exception.message?.contains("weak-password") == true ->
                "Password troppo debole. Usa almeno 6 caratteri."
            else ->
                "Errore: ${exception.message}"
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Naviga alla MainActivity
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}