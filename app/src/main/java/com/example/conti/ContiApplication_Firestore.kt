package com.example.conti

import android.app.Application
import com.example.conti.data.repository.FirestoreRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Classe Application personalizzata per l'app Conti.
 *
 * Viene creata una sola volta all'avvio dell'app e rimane in vita per tutto il ciclo di vita.
 *
 * Responsabilità:
 * - Inizializzare Firebase
 * - Configurare Firestore
 * - Creare il repository singleton
 *
 * IMPORTANTE: Questa classe deve essere registrata nell'AndroidManifest.xml
 */
class ContiApplication : Application() {

    /**
     * Repository Firestore (singleton, lazy initialization).
     */
    val repository: FirestoreRepository by lazy {
        FirestoreRepository()
    }

    /**
     * Istanza Firestore configurata.
     */
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Cache offline
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Inizializza Firebase
        FirebaseApp.initializeApp(this)

        // Configura Firestore (già fatto nel lazy initializer)
        // Questo lo esegue subito per evitare ritardi al primo accesso
        firestore

        // Log per debug (opzionale)
        android.util.Log.d("ContiApplication", "✅ Firebase e Firestore inizializzati")

        // Test connessione Firestore (opzionale, rimuovi in produzione)
        testFirestoreConnection()
    }

    /**
     * Verifica la connessione a Firestore.
     * RIMUOVI IN PRODUZIONE - Solo per debug.
     */
    private fun testFirestoreConnection() {
        firestore.collection("test")
            .document("connection")
            .set(mapOf("timestamp" to System.currentTimeMillis()))
            .addOnSuccessListener {
                android.util.Log.d("ContiApplication", "✅ Firestore connesso correttamente!")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ContiApplication", "❌ Errore connessione Firestore: ${e.message}")
            }
    }
}