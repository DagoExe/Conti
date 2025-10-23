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
import kotlinx.coroutines.launch

/**
 * Activity di login / registrazione
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authManager = AuthManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            signIn(email, password)
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val name = binding.nameInput.text.toString().trim()
            signUp(email, password, name)
        }

        binding.resetPasswordText.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            resetPassword(email)
        }
    }

    private fun signIn(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            authManager.signInWithEmail(email, password)
                .onSuccess { user ->
                    Log.d("LoginActivity", "✅ Login riuscito: ${user.uid}")
                    navigateToMain()
                }
                .onFailure { e ->
                    Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_LONG).show()
                }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun signUp(email: String, password: String, name: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            authManager.signUpWithEmail(email, password, name)
                .onSuccess { user ->
                    Log.d("LoginActivity", "✅ Registrazione riuscita: ${user.uid}")
                    navigateToMain()
                }
                .onFailure { e ->
                    Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_LONG).show()
                }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun resetPassword(email: String) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Inserisci la tua email", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            authManager.sendPasswordResetEmail(email)
                .onSuccess {
                    Toast.makeText(
                        this@LoginActivity,
                        "Email di reset inviata a $email",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onFailure {
                    Toast.makeText(this@LoginActivity, "Errore: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
