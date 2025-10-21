package com.example.conti

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.Conti.R
import com.example.Conti.databinding.ActivityMainBinding
import com.example.conti.ui.ViewModelFactory
import com.example.conti.ui.home.HomeViewModel

/**
 * MainActivity - Activity principale dell'applicazione.
 *
 * Responsabilità:
 * - Inizializzare il ViewBinding
 * - Creare i ViewModel
 * - Gestire la navigazione tra fragment (verrà implementato dopo)
 *
 * Per ora, mostriamo una schermata di test con le informazioni del database.
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding per accedere alle view in modo type-safe
    private lateinit var binding: ActivityMainBinding

    // ViewModel per la home
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inizializza il ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ottieni il repository dall'Application
        val application = application as ContiApplication
        val repository = application.repository

        // Crea la ViewModelFactory
        val viewModelFactory = ViewModelFactory(repository)

        // Inizializza il ViewModel
        homeViewModel = ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java]

        // Setup dell'UI
        setupUI()
        setupObservers()
    }

    /**
     * Configura l'interfaccia utente.
     */
    private fun setupUI() {
        // Per ora, mostriamo solo un messaggio di benvenuto
        // Implementeremo i fragment nelle prossime versioni

        // Test: inserisci un conto di prova se il database è vuoto
        homeViewModel.hasConti.observe(this) { hasConti ->
            if (!hasConti) {
                inserisciContoDemo()
            }
        }
    }

    /**
     * Configura gli observer per i LiveData del ViewModel.
     */
    private fun setupObservers() {
        // Osserva i conti
        homeViewModel.conti.observe(this) { conti ->
            if (conti.isNotEmpty()) {
                val messaggi = conti.map { conto ->
                    "${conto.nome} (${conto.istituto})"
                }

                // Aggiorna il TextView con la lista dei conti
                binding.textView.text = """
                    Benvenuto in Conti! 👋
                    
                    Conti nel database: ${conti.size}
                    
                    ${messaggi.joinToString("\n")}
                    
                    L'app è pronta per essere utilizzata!
                """.trimIndent()
            }
        }

        // Osserva il costo degli abbonamenti
        homeViewModel.costoAbbonamentiMensile.observe(this) { costo ->
            // Per debug: mostra il costo degli abbonamenti
            // Questo verrà mostrato in modo più elegante nell'UI finale
        }
    }

    /**
     * Inserisce un conto demo per testare il database.
     * Questo metodo è solo per testing iniziale.
     */
    private fun inserisciContoDemo() {
        val contoDemo = com.example.conti.data.database.entities.Conto(
            nome = "Conto Demo",
            istituto = "Banca Demo",
            saldoIniziale = 1000.0,
            colore = "#4CAF50",
            isFromExcel = false
        )

        homeViewModel.inserisciConto(
            conto = contoDemo,
            onSuccess = { id ->
                // Log per debug
                android.util.Log.d("MainActivity", "Conto demo inserito con ID: $id")
            },
            onError = { errore ->
                android.util.Log.e("MainActivity", "Errore inserimento conto: $errore")
            }
        )
    }
}