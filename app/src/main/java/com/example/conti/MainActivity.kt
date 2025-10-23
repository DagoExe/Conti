package com.example.conti

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.conti.auth.AuthManager
import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager.getInstance()

        if (!authManager.isAuthenticated) {
            performAnonymousLogin()
        } else {
            setupUserProfile()
            setupUI()
        }
    }

    private fun performAnonymousLogin() {
        lifecycleScope.launch {
            authManager.signInAnonymously()
                .onSuccess { user ->
                    android.util.Log.d("MainActivity", "✅ Login anonimo riuscito: ${user.uid}")
                    setupUserProfile()
                    setupUI()
                }
                .onFailure { error ->
                    android.util.Log.e("MainActivity", "❌ Errore login anonimo", error)
                    showLoginErrorDialog()
                }
        }
    }

    /**
     * Crea o aggiorna il profilo utente su Firestore
     */
    private fun setupUserProfile() {
        lifecycleScope.launch {
            try {
                val email = authManager.currentUser?.email ?: "anonymous@local"
                firestoreRepository.updateUserProfile(email)
                android.util.Log.d("MainActivity", "👤 Profilo aggiornato per $email")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Errore aggiornamento profilo", e)
            }
        }
    }

    private fun setupUI() {
        setupNavigation()
        observeAuthState()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbar.title = when (destination.id) {
                R.id.navigation_home -> "Home"
                R.id.navigation_movimenti -> "Movimenti"
                R.id.navigation_abbonamenti -> "Abbonamenti"
                R.id.navigation_conti -> "I Tuoi Conti"
                else -> "Conti"
            }
        }
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            authManager.authState.collect { user ->
                if (user == null) {
                    android.util.Log.w("MainActivity", "⚠️ Utente disconnesso")
                    performAnonymousLogin()
                } else {
                    setupUserProfile()
                }
            }
        }
    }

    private fun showLoginErrorDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Errore Autenticazione")
            .setMessage("Impossibile accedere all'app. Controlla la connessione e riprova.")
            .setPositiveButton("Riprova") { _, _ -> performAnonymousLogin() }
            .setNegativeButton("Esci") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}
