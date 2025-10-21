package com.example.conti

import android.app.Application
import com.example.conti.data.database.AppDatabase
import com.example.conti.data.repository.MovimentiRepository

/**
 * Classe Application personalizzata per l'app Conti.
 *
 * Viene creata una sola volta all'avvio dell'app e rimane in vita per tutto il ciclo di vita.
 *
 * Responsabilità:
 * - Inizializzare il database Room
 * - Creare il repository
 * - Fornire istanze singleton accessibili da tutta l'app
 *
 * IMPORTANTE: Questa classe deve essere registrata nell'AndroidManifest.xml
 */
class ContiApplication : Application() {

    /**
     * Database Room (singleton, lazy initialization).
     * Viene creato solo quando necessario.
     */
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    /**
     * Repository centrale (singleton, lazy initialization).
     * Dipende dal database, quindi viene creato dopo.
     */
    val repository: MovimentiRepository by lazy {
        MovimentiRepository(
            contoDao = database.contoDao(),
            movimentoDao = database.movimentoDao(),
            abbonamentoDao = database.abbonamentoDao()
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Log per debug (opzionale)
        // Log.d("ContiApplication", "Application initialized")

        // Qui puoi inizializzare altre librerie se necessario
        // Es: Crash reporting, Analytics, ecc.
    }
}