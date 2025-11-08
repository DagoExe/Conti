package com.example.conti.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.conti.utils.MessageHelper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.conti.MainActivity
import com.example.conti.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import kotlinx.coroutines.launch

/**
 * ✨ LoginActivity - versione aggiornata con dialog premium Figma (activity_login.xml)
 *
 * AGGIORNATO con dialog custom premium che seguono il design system MONIO
 *
 * Features:
 * - Email/Password login
 * - Google Sign-In
 * - Reset Password
 * - Email verification con dialog custom premium
 *
 * Design Reference: Figma "MONIO" - LoginScreen + EmailVerificationDialog
 */
class LoginActivity : AppCompatActivity() {

    private val authManager = AuthManager.getInstance()

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var googleButton: MaterialButton
    private lateinit var signInButton: MaterialButton
    private lateinit var signUpLink: TextView
    private lateinit var progressBar: ProgressBar

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
                showLoading(false)
            }
        } else {
            showLoading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d(TAG, "╔════════════════════════════════════════╗")
        Log.d(TAG, "   LOGIN ACTIVITY STARTED (PREMIUM UI)")
        Log.d(TAG, "╚════════════════════════════════════════╝")

        setupUI()
        checkIfAlreadyLoggedIn()
    }

    private fun setupUI() {
        emailField = findViewById(R.id.etEmail)
        passwordField = findViewById(R.id.etPassword)
        googleButton = findViewById(R.id.btnGoogleSignIn)
        signInButton = findViewById(R.id.btnSignIn)
        signUpLink = findViewById(R.id.tvSignUpLink)
        progressBar = findViewById(R.id.progressBar)

        // Login con email/password
        signInButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (validateLoginInput(email, password)) {
                signIn(email, password)
            }
        }

        // Google Sign-In
        googleButton.setOnClickListener {
            signInWithGoogleClick()
        }

        // Link registrazione
        signUpLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // ========================================
    // LOGIN / REGISTRAZIONE
    // ========================================

    private fun signIn(email: String, password: String) {
        Log.d(TAG, "🔐 Tentativo di login per: $email")
        showLoading(true)

        lifecycleScope.launch {
            authManager.signInWithEmail(email, password)
                .onSuccess { user ->
                    Log.d(TAG, "✅ Login riuscito per ${user.email}")
                    if (user.isEmailVerified) {
                        MessageHelper.showSuccess(this@LoginActivity, "Benvenuto!")
                        navigateToMain()
                    } else {
                        // 🎨 USA IL NUOVO DIALOG PREMIUM
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

    // ========================================
    // GOOGLE SIGN-IN
    // ========================================

    private fun signInWithGoogleClick() {
        Log.d(TAG, "🔐 Avvio Google Sign-In...")
        showLoading(true)
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("404921205660-eljajs4jgjjdl91ebis8on4mghgqib85.apps.googleusercontent.com")
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore avvio Google Sign-In", e)
            showError(e)
            showLoading(false)
        }
    }

    private fun signInWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "🔐 Autenticazione Google per: ${account.email}")

        if (account.idToken == null) {
            showError(Exception("⚠️ ID Token mancante. Configura correttamente OAuth."))
            showLoading(false)
            return
        }

        lifecycleScope.launch {
            authManager.signInWithGoogle(account)
                .onSuccess { user ->
                    MessageHelper.showSuccess(
                        this@LoginActivity,
                        "Benvenuto ${user.displayName ?: ""}!"
                    )
                    navigateToMain()
                }
                .onFailure { e ->
                    showError(e)
                }
            showLoading(false)
        }
    }

    // ========================================
    // VERIFICA EMAIL - DIALOG PREMIUM
    // ========================================

    /**
     * 🎨 Mostra dialog premium di verifica email
     *
     * Sostituisce il vecchio MaterialAlertDialogBuilder con un dialog custom
     * che segue il design system MONIO premium.
     */
    private fun showEmailVerificationDialog() {
        val email = authManager.currentUser?.email ?: "la tua email"

        PremiumDialogHelper.showEmailVerificationDialog(
            context = this,
            email = email,
            onVerifyClick = {
                // L'utente dice di aver verificato
                checkEmailVerification()
            },
            onResendClick = {
                // Ri-invia email di verifica
                resendVerificationEmail()
            },
            onLogoutClick = {
                // Logout
                authManager.signOut()
                Toast.makeText(this, "👋 Disconnesso", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun checkEmailVerification() {
        showLoading(true)
        lifecycleScope.launch {
            authManager.reloadUser()
            if (authManager.isEmailVerified) {
                MessageHelper.showSuccess(this@LoginActivity, "Email verificata!")
                navigateToMain()
            } else {
                MessageHelper.showError(
                    this@LoginActivity,
                    "Email non ancora verificata. Controlla la tua casella."
                )
            }
            showLoading(false)
        }
    }

    private fun resendVerificationEmail() {
        showLoading(true)
        lifecycleScope.launch {
            authManager.sendEmailVerification()
                .onSuccess {
                    MessageHelper.showSuccess(
                        this@LoginActivity,
                        "Email di verifica inviata. Controlla la tua casella."
                    )
                }
                .onFailure { e -> showError(e) }
            showLoading(false)
        }
    }

    // ========================================
    // VALIDAZIONE
    // ========================================

    private fun validateLoginInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            MessageHelper.showError(this, "Inserisci la tua email")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            MessageHelper.showError(this, "Email non valida")
            return false
        }
        if (password.isEmpty()) {
            MessageHelper.showError(this, "Inserisci la password")
            return false
        }
        return true
    }

    // ========================================
    // ERROR HANDLING
    // ========================================

    private fun showError(exception: Throwable) {
        val message = when (exception) {
            is FirebaseAuthInvalidUserException -> "Account non trovato."
            is FirebaseAuthInvalidCredentialsException -> "Email o password errati."
            is FirebaseAuthUserCollisionException -> "Email già in uso."
            is FirebaseAuthWeakPasswordException -> "Password troppo debole."
            is FirebaseNetworkException -> "Errore di rete. Controlla la connessione."
            else -> "Errore: ${exception.message ?: "Sconosciuto"}"
        }

        MessageHelper.showError(this, message)
        Log.e(TAG, "Errore autenticazione: ${exception.message}", exception)
    }

    // ========================================
    // ALTRO
    // ========================================

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        googleButton.isEnabled = !loading
        signInButton.isEnabled = !loading
        signUpLink.isEnabled = !loading
    }

    private fun checkIfAlreadyLoggedIn() {
        if (authManager.isAuthenticated) {
            if (authManager.isEmailVerified || authManager.currentUser?.isAnonymous == true) {
                navigateToMain()
            } else {
                showEmailVerificationDialog()
            }
        }
    }

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