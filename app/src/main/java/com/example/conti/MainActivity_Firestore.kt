package com.example.conti

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.conti.auth.AuthManager
import com.example.conti.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * MainActivity - Activity principale con autenticazione Firebase.
 *
 * Responsabilità:
 * - Gestire l'autenticazione
 * - Configurare la navigazione
 * - Fornire il NavController ai fragment
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inizializza AuthManager
        authManager = AuthManager.getInstance()

        // Verifica autenticazione
        if (!authManager.isAuthenticated) {
            // Effettua login anonimo per permettere l'uso dell'app
            performAnonymousLogin()
        } else {
            // Utente già autenticato, configura UI
            setupUI()
        }
    }

    /**
     * Effettua login anonimo.
     */
    private fun performAnonymousLogin() {
        android.util.Log.d("MainActivity", "🔑 Login anonimo in corso...")

        lifecycleScope.launch {
            authManager.signInAnonymously()
                .onSuccess { user ->
                    android.util.Log.d("MainActivity", "✅ Login anonimo riuscito: ${user.uid}")
                    setupUI()
                }
                .onFailure { error ->
                    android.util.Log.e("MainActivity", "❌ Errore login anonimo", error)
                    showLoginErrorDialog()
                }
        }
    }

    /**
     * Configura l'interfaccia utente dopo il login.
     */
    private fun setupUI() {
        setupNavigation()

        // Osserva lo stato di autenticazione
        observeAuthState()
    }

    /**
     * Configura il Navigation Component.
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        // Collega la Bottom Navigation al NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Listener per cambiare il titolo della toolbar
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

    /**
     * Osserva lo stato di autenticazione.
     */
    private fun observeAuthState() {
        lifecycleScope.launch {
            authManager.authState.collect { user ->
                if (user == null) {
                    // Utente disconnesso, effettua logout
                    android.util.Log.w("MainActivity", "⚠️ Utente disconnesso")
                    // Qui potresti navigare a una schermata di login
                    performAnonymousLogin()
                } else {
                    android.util.Log.d("MainActivity", "✅ Utente autenticato: ${user.uid}")
                }
            }
        }
    }

    /**
     * Mostra dialog di errore login.
     */
    private fun showLoginErrorDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Errore Autenticazione")
            .setMessage("Impossibile accedere all'app. Verifica la connessione Internet e riprova.")
            .setPositiveButton("Riprova") { _, _ ->
                performAnonymousLogin()
            }
            .setNegativeButton("Esci") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Gestisce il tasto back.
     */
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Non fare logout automaticamente al destroy dell'activity
        // L'utente rimane autenticato tra sessioni
    }
}