package com.example.conti

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.conti.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * MainActivity - Activity principale dell'applicazione con Bottom Navigation.
 *
 * Responsabilità:
 * - Gestire la navigazione tra i fragment tramite Bottom Navigation Bar
 * - Configurare la Toolbar
 * - Fornire il NavController ai fragment
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inizializza Firebase
        FirebaseApp.initializeApp(this)

        // Configura Firestore
        db = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Supporto offline
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        }

        // Verifica connessione
        testFirestoreConnection()
    }

    private fun testFirestoreConnection() {
        db.collection("test")
            .document("connection")
            .set(mapOf("timestamp" to System.currentTimeMillis()))
            .addOnSuccessListener {
                println("✅ Firestore connesso correttamente!")
            }
            .addOnFailureListener { e ->
                println("❌ Errore connessione Firestore: ${e.message}")
            }
    }

    /**
     * Configura il Navigation Component collegando:
     * - NavHostFragment (container dei fragment)
     * - BottomNavigationView (barra di navigazione inferiore)
     */
    private fun setupNavigation() {
        // Ottieni il NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        // Collega la Bottom Navigation al NavController
        // Questo sincronizza automaticamente la selezione degli item con i fragment
        binding.bottomNavigation.setupWithNavController(navController)

        // Listener per cambiare il titolo della toolbar in base al fragment
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
     * Gestisce il tasto back.
     * Se siamo nella Home, esce dall'app.
     * Altrimenti, torna al fragment precedente.
     */
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}