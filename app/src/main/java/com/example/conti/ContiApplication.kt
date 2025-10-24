package com.example.conti

import android.app.Application
import com.example.conti.data.repository.AccountRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Classe Application personalizzata per l'app Conti.
 */
class ContiApplication : Application() {

    /**
     * Repository per gli Account (singleton, lazy initialization).
     */
    val accountRepository: AccountRepository by lazy {
        AccountRepository.getInstance()
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

        // Configura Firestore
        firestore

        android.util.Log.d("ContiApplication", "✅ Firebase e Firestore inizializzati")
    }
}