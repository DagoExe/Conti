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

/**
 * MainActivity - Activity principale dell'app.
 *
 * ✅ VERSIONE CORRETTA CON:
 * - Migliore gestione errori Firebase
 * - Logging dettagliato
 * - Fallback in caso di errori di autenticazione
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            android.util.Log.d("MainActivity", "=== onCreate START ===")

            // 1. Inflate layout
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            android.util.Log.d("MainActivity", "✅ Layout inflated")

            // 2. Setup UI PRIMA di Firebase
            setupNavigation()
            android.util.Log.d("MainActivity", "✅ Navigation setup")

            // 3. Inizializza AuthManager
            authManager = AuthManager.getInstance()
            android.util.Log.d("MainActivity", "✅ AuthManager inizializzato")

            // 4. Controlla autenticazione
            if (!authManager.isAuthenticated) {
                android.util.Log.d("MainActivity", "⚠️ Utente non autenticato, login anonimo...")
                performAnonymousLogin()
            } else {
                android.util.Log.d("MainActivity", "✅ Utente già autenticato: ${authManager.currentUser?.uid}")
                setupUserProfile()
                observeAuthState()
            }

            android.util.Log.d("MainActivity", "=== onCreate END ===")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ ERRORE CRITICO in onCreate", e)
            e.printStackTrace()
            showCriticalError(e)
        }
    }

    /**
     * Setup della navigazione (può funzionare anche senza Firebase).
     */
    private fun setupNavigation() {
        try {
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

            android.util.Log.d("MainActivity", "✅ Navigation configurata correttamente")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Errore setup navigation", e)
            throw e
        }
    }

    /**
     * Effettua login anonimo con gestione errori robusta.
     */
    private fun performAnonymousLogin() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("MainActivity", "🔐 Tentativo login anonimo...")

                authManager.signInAnonymously()
                    .onSuccess { user ->
                        android.util.Log.d("MainActivity", "✅ Login anonimo riuscito: ${user.uid}")
                        setupUserProfile()
                        observeAuthState()
                    }
                    .onFailure { error ->
                        android.util.Log.e("MainActivity", "❌ Errore login anonimo", error)

                        // IMPORTANTE: Non bloccare l'app, mostra errore ma continua
                        showLoginError(error)

                        // Continua comunque con UI limitata
                        observeAuthState()
                    }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Eccezione durante login", e)
                showLoginError(e)
                observeAuthState()
            }
        }
    }

    /**
     * Crea o aggiorna il profilo utente su Firestore.
     */
    private fun setupUserProfile() {
        lifecycleScope.launch {
            try {
                val email = authManager.currentUser?.email ?: "anonymous@local"
                firestoreRepository.updateUserProfile(email)
                android.util.Log.d("MainActivity", "👤 Profilo aggiornato per $email")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Errore aggiornamento profilo", e)
                // Non bloccare l'app per questo errore
            }
        }
    }

    /**
     * Osserva lo stato di autenticazione.
     */
    private fun observeAuthState() {
        try {
            lifecycleScope.launch {
                authManager.authState.collect { user ->
                    if (user == null) {
                        android.util.Log.w("MainActivity", "⚠️ Utente disconnesso")
                        // Non tentare nuovamente il login automaticamente
                        // per evitare loop infiniti
                    } else {
                        android.util.Log.d("MainActivity", "👤 Utente connesso: ${user.uid}")
                        setupUserProfile()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Errore observeAuthState", e)
        }
    }

    /**
     * Mostra un errore di login senza bloccare l'app.
     */
    private fun showLoginError(error: Throwable) {
        try {
            android.util.Log.w("MainActivity", "⚠️ Mostrando errore login all'utente")

            // Mostra un Toast invece di un dialog bloccante
            runOnUiThread {
                android.widget.Toast.makeText(
                    this,
                    "⚠️ Errore autenticazione: ${error.message}\nAlcune funzionalità potrebbero non essere disponibili.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Errore mostrando errore", e)
        }
    }

    /**
     * Mostra un errore critico che impedisce l'avvio dell'app.
     */
    private fun showCriticalError(error: Throwable) {
        try {
            runOnUiThread {
                MaterialAlertDialogBuilder(this)
                    .setTitle("❌ Errore Critico")
                    .setMessage("Impossibile avviare l'app:\n\n${error.message}\n\nVerifica la configurazione di Firebase.")
                    .setPositiveButton("Riprova") { _, _ ->
                        recreate() // Riavvia l'activity
                    }
                    .setNegativeButton("Esci") { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Impossibile mostrare dialog errore", e)
            finish() // Chiudi l'app
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}