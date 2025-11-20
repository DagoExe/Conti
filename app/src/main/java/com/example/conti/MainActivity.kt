package com.example.conti

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.conti.auth.AuthManager
import com.example.conti.auth.LoginActivity
import com.example.conti.auth.PremiumDialogHelper
import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.databinding.ActivityMainBinding
import com.example.conti.utils.FirebaseDiagnostic
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MainActivity - Activity principale dell'app.
 *
 * ✅ VERSIONE MIGLIORATA con:
 * - Controllo autenticazione ritardato per permettere init Firebase
 * - Migliore gestione degli stati di caricamento
 * - Diagnostica Firebase integrata
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager
    private val firestoreRepository = FirestoreRepository()

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("MainActivity", "═══════════════════════════════════════")
        android.util.Log.d("MainActivity", "   APP STARTUP")
        android.util.Log.d("MainActivity", "═══════════════════════════════════════")

        try {
            // 1. Assicurati che Firebase sia inizializzato
            initializeFirebase()

            // 2. Inizializza AuthManager
            authManager = AuthManager.getInstance()
            android.util.Log.d("MainActivity", "✅ AuthManager inizializzato")

            // 3. ⚠️ IMPORTANTE: Dai tempo a Firebase di completare l'inizializzazione
            //    prima di controllare lo stato di autenticazione
            lifecycleScope.launch {
                checkAuthenticationAndProceed()
            }

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌❌❌ ERRORE CRITICO in onCreate ❌❌❌", e)
            e.printStackTrace()
            showCriticalError(e)
        }
    }

    /**
     * ✅ NUOVO: Assicura che Firebase sia inizializzato
     */
    private fun initializeFirebase() {
        try {
            // Verifica se Firebase è già inizializzato
            FirebaseApp.getInstance()
            android.util.Log.d("MainActivity", "✅ Firebase già inizializzato")
        } catch (e: IllegalStateException) {
            // Se non è inizializzato, inizializzalo
            FirebaseApp.initializeApp(this)
            android.util.Log.d("MainActivity", "✅ Firebase inizializzato ora")
        }

        // Esegui diagnostica Firebase (solo in debug)
        // ✅ FIX: Usa applicationInfo invece di BuildConfig
        if (0 != applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) {
            FirebaseDiagnostic.runDiagnostic(applicationContext)
        }
    }

    /**
     * ✅ NUOVO: Controlla autenticazione con un piccolo delay per permettere init
     */
    private suspend fun checkAuthenticationAndProceed() {
        // Piccolo delay per assicurarsi che Firebase sia pronto
        delay(100)

        // Controlla autenticazione
        if (!authManager.isAuthenticated) {
            android.util.Log.w("MainActivity", "⚠️ Utente non autenticato - Reindirizzo a LoginActivity")
            navigateToLogin()
            return
        }

        // ✅ NUOVO: Controlla verifica email
        val user = authManager.currentUser
        if (user != null && !user.isEmailVerified && !user.isAnonymous) {
            android.util.Log.w("MainActivity", "⚠️ Email non verificata - Reindirizzo a LoginActivity")

            runOnUiThread {
                MaterialAlertDialogBuilder(this)
                    .setTitle("📧 Verifica Email Richiesta")
                    .setMessage("Devi verificare la tua email prima di accedere all'app.")
                    .setPositiveButton("OK") { _, _ ->
                        authManager.signOut()
                        navigateToLogin()
                    }
                    .setCancelable(false)
                    .show()
            }
            return
        }

        android.util.Log.d("MainActivity", "✅ Utente autenticato: ${authManager.currentUser?.uid}")

        // Procedi con l'inizializzazione dell'UI
        initializeUI()
    }

    /**
     * ✅ NUOVO: Inizializza UI solo dopo verifica autenticazione
     */
    private fun initializeUI() {
        // Inflate layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        android.util.Log.d("MainActivity", "✅ Layout inflated")

        // Setup UI
        setupNavigation()
        setupToolbarMenu()
        android.util.Log.d("MainActivity", "✅ Navigation setup")

        // Setup profilo utente
        setupUserProfile()

        // Osserva stato autenticazione (per logout)
        observeAuthState()

        android.util.Log.d("MainActivity", "═══════════════════════════════════════")
        android.util.Log.d("MainActivity", "   SETUP COMPLETED")
        android.util.Log.d("MainActivity", "═══════════════════════════════════════")
    }

    /**
     * Setup menu toolbar con pulsante logout
     */
    private fun setupToolbarMenu() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    showLogoutDialog()
                    true
                }
                R.id.action_profile -> {
                    showProfileInfo()
                    true
                }
                else -> false
            }
        }

        // Infla il menu
        binding.toolbar.inflateMenu(R.menu.toolbar_menu)
    }

    /**
     * Mostra dialog di conferma logout premium
     */
    private fun showLogoutDialog() {
        PremiumDialogHelper.showLogoutConfirmDialog(
            context = this,
            onConfirm = {
                performLogout()
            },
            onCancel = {
                // Dialog annullato
                android.util.Log.d("MainActivity", "Logout annullato")
            }
        )
    }

    /**
     * Esegue il logout e reindirizza a LoginActivity
     */
    private fun performLogout() {
        android.util.Log.d("MainActivity", "🚪 Logout in corso...")

        authManager.signOut()

        android.util.Log.d("MainActivity", "✅ Logout completato")

        navigateToLogin()
    }

    /**
     * Mostra informazioni profilo con dialog premium
     */
    private fun showProfileInfo() {
        val user = authManager.currentUser
        val email = user?.email ?: "Utente anonimo"
        val uid = user?.uid ?: "N/A"

        // ✅ USA IL DIALOG PREMIUM
        PremiumDialogHelper.showProfileInfoDialog(
            context = this,
            email = email,
            uid = uid,
            onOkClick = {
                // Dialog chiuso
                android.util.Log.d("MainActivity", "Dialog profilo chiuso")
            }
        )
    }

    /**
     * Naviga a LoginActivity
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Setup della navigazione con gestione corretta del back stack per ogni tab.
     */
    private fun setupNavigation() {
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

            navController = navHostFragment.navController

            // ✅ Listener personalizzato per gestire il "re-click" sulla stessa tab
            binding.bottomNavigation.setOnItemSelectedListener { item ->

                // Controlla se siamo già nella tab selezionata
                val currentId = navController.currentDestination?.id
                val targetId = item.itemId

                // Se clicchiamo sulla tab Conti e siamo in un fragment figlio (es. Movimenti),
                // torniamo indietro fino a ContiFragment
                if (targetId == R.id.navigation_conti && currentId == R.id.navigation_movimenti) {
                    navController.popBackStack(R.id.navigation_conti, false)
                    return@setOnItemSelectedListener true
                }

                // Per gli altri casi, comportamento standard
                if (item.itemId != binding.bottomNavigation.selectedItemId) {
                    when (item.itemId) {
                        R.id.navigation_rate -> {
                            navController.navigate(R.id.navigation_rate)
                            true
                        }
                        R.id.navigation_conti -> {
                            // Quando si passa a Conti, puliamo lo stack fino a Conti
                            // per assicurare di vedere la lista conti e non i movimenti
                            navController.popBackStack(R.id.navigation_conti, true)
                            navController.navigate(R.id.navigation_conti)
                            true
                        }
                        R.id.navigation_home -> {
                            navController.navigate(R.id.navigation_home)
                            true
                        }
                        R.id.navigation_abbonamenti -> {
                            navController.navigate(R.id.navigation_abbonamenti)
                            true
                        }
                        R.id.navigation_debiti -> {
                            navController.navigate(R.id.navigation_debiti)
                            true
                        }
                        else -> false
                    }
                } else {
                    // Re-click sulla stessa tab: resetta lo stack alla root di quella tab
                    // Utile per tornare alla lista principale se si è dentro un dettaglio
                    navController.popBackStack(item.itemId, false)
                    true
                }
            }

            // Sincronizza lo stato iniziale del menu
            binding.bottomNavigation.setOnItemReselectedListener { item ->
                // Se l'utente clicca di nuovo sulla tab attiva, torna alla root di quella sezione
                navController.popBackStack(item.itemId, false)
            }

            // ✅ Listener per aggiornare il titolo della toolbar
            navController.addOnDestinationChangedListener { _, destination, _ ->
                binding.toolbar.title = when (destination.id) {
                    R.id.navigation_rate -> "Rate"
                    R.id.navigation_conti -> "I Tuoi Conti"
                    R.id.navigation_home -> "Home"
                    R.id.navigation_abbonamenti -> "Abbonamenti"
                    R.id.navigation_debiti -> "Debiti"
                    R.id.navigation_movimenti -> "Movimenti"
                    else -> "Conti"
                }

                // Aggiorna selezione bottom navigation se necessario
                // (utile quando si naviga programmaticamente o tramite back button)
                when (destination.id) {
                    R.id.navigation_rate -> binding.bottomNavigation.menu.findItem(R.id.navigation_rate).isChecked = true
                    R.id.navigation_conti, R.id.navigation_movimenti -> binding.bottomNavigation.menu.findItem(R.id.navigation_conti).isChecked = true
                    R.id.navigation_home -> binding.bottomNavigation.menu.findItem(R.id.navigation_home).isChecked = true
                    R.id.navigation_abbonamenti -> binding.bottomNavigation.menu.findItem(R.id.navigation_abbonamenti).isChecked = true
                    R.id.navigation_debiti -> binding.bottomNavigation.menu.findItem(R.id.navigation_debiti).isChecked = true
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
                        android.util.Log.w("MainActivity", "⚠️ Utente disconnesso - Reindirizzo a Login")
                        navigateToLogin()
                    } else {
                        android.util.Log.d("MainActivity", "👤 Utente connesso: ${user.uid}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Errore observeAuthState", e)
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

    /**
     * ✅ Gestisce il pulsante "back" di Android
     */
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * ✅ Gestisce il pulsante back hardware
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (navController.currentDestination?.id == R.id.navigation_home) {
            super.onBackPressed()
        } else {
            if (!navController.popBackStack()) {
                super.onBackPressed()
            }
        }
    }
}