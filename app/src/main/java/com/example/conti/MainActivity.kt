package com.example.conti

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.conti.auth.AuthManager
import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.databinding.ActivityMainBinding
import com.example.conti.utils.FirebaseDiagnostic
import com.example.conti.utils.TestDataGenerator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * MainActivity - Activity principale dell'app.
 *
 * ✅ VERSIONE PULITA SENZA TEST AUTOMATICI
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            android.util.Log.d("MainActivity", "═══════════════════════════════════════")
            android.util.Log.d("MainActivity", "   APP STARTUP")
            android.util.Log.d("MainActivity", "═══════════════════════════════════════")

            // 🔥 ESEGUI DIAGNOSTICA FIREBASE
            FirebaseDiagnostic.runDiagnostic(applicationContext)

            // 1. Inflate layout
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            android.util.Log.d("MainActivity", "✅ Layout inflated")

            // 2. Setup UI PRIMA di Firebase (così l'app non crasha anche se Firebase fallisce)
            setupNavigation()
            android.util.Log.d("MainActivity", "✅ Navigation setup")

            // 3. Inizializza AuthManager
            authManager = AuthManager.getInstance()
            android.util.Log.d("MainActivity", "✅ AuthManager inizializzato")

            // 4. Controlla autenticazione
            if (!authManager.isAuthenticated) {
                android.util.Log.d("MainActivity", "⚠️ Utente non autenticato")

                // 🔥 LOGIN ANONIMO CON DIAGNOSTICA
                FirebaseDiagnostic.testAnonymousLogin { success, message ->
                    if (success) {
                        android.util.Log.d("MainActivity", "✅ Test login riuscito: $message")
                        setupUserProfile()
                        observeAuthState()

                        TestDataGenerator.createSampleTransactions()
                    } else {
                        android.util.Log.e("MainActivity", "❌ Test login fallito: $message")
                        showFirebaseError(message)
                    }
                }
            } else {
                android.util.Log.d("MainActivity", "✅ Utente già autenticato: ${authManager.currentUser?.uid}")
                setupUserProfile()
                observeAuthState()

                TestDataGenerator.createSampleTransactions()
            }

            android.util.Log.d("MainActivity", "═══════════════════════════════════════")
            android.util.Log.d("MainActivity", "   SETUP COMPLETED")
            android.util.Log.d("MainActivity", "═══════════════════════════════════════")

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌❌❌ ERRORE CRITICO in onCreate ❌❌❌", e)
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
     * Mostra un errore specifico di Firebase.
     */
    private fun showFirebaseError(message: String) {
        try {
            runOnUiThread {
                MaterialAlertDialogBuilder(this)
                    .setTitle("⚠️ Errore Firebase")
                    .setMessage("Impossibile autenticarsi:\n\n$message\n\nVerifica:\n• Connessione internet\n• Configurazione google-services.json\n• Regole Firestore")
                    .setPositiveButton("Riprova") { _, _ ->
                        recreate()
                    }
                    .setNegativeButton("Continua senza Firebase") { _, _ ->
                        // L'app continuerà a funzionare in modalità limitata
                    }
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Errore mostrando dialog Firebase", e)
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
                    .setMessage("Impossibile avviare l'app:\n\n${error.message}\n\n${error.stackTraceToString()}")
                    .setPositiveButton("Riprova") { _, _ ->
                        recreate()
                    }
                    .setNegativeButton("Esci") { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Impossibile mostrare dialog errore", e)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}