package com.example.conti.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Classe di diagnostica per verificare lo stato di Firebase.
 *
 * USALA COSÌ:
 * FirebaseDiagnostic.runDiagnostic(applicationContext)
 */
object FirebaseDiagnostic {

    private const val TAG = "🔥 FirebaseDiagnostic"

    fun runDiagnostic(context: Context) {
        Log.d(TAG, "════════════════════════════════════════════")
        Log.d(TAG, "   FIREBASE DIAGNOSTIC STARTED")
        Log.d(TAG, "════════════════════════════════════════════")

        // 1. Verifica Firebase App
        checkFirebaseApp(context)

        // 2. Verifica Auth
        checkFirebaseAuth()

        // 3. Verifica Firestore
        checkFirestore()

        // 4. Verifica Network
        checkNetworkPermissions(context)

        Log.d(TAG, "════════════════════════════════════════════")
        Log.d(TAG, "   DIAGNOSTIC COMPLETED")
        Log.d(TAG, "════════════════════════════════════════════")
    }

    private fun checkFirebaseApp(context: Context) {
        try {
            val apps = FirebaseApp.getApps(context)
            Log.d(TAG, "✅ Firebase Apps inizializzate: ${apps.size}")

            if (apps.isEmpty()) {
                Log.e(TAG, "❌ ERRORE: Nessuna Firebase App trovata!")
                Log.e(TAG, "   Soluzione: Verifica google-services.json")
                return
            }

            val defaultApp = FirebaseApp.getInstance()
            Log.d(TAG, "✅ Default App: ${defaultApp.name}")
            Log.d(TAG, "   Project ID: ${defaultApp.options.projectId}")
            Log.d(TAG, "   Application ID: ${defaultApp.options.applicationId}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERRORE FirebaseApp: ${e.message}", e)
        }
    }

    private fun checkFirebaseAuth() {
        try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            if (currentUser == null) {
                Log.w(TAG, "⚠️ FirebaseAuth: Nessun utente autenticato")
                Log.d(TAG, "   Questo è normale al primo avvio")
            } else {
                Log.d(TAG, "✅ FirebaseAuth: Utente autenticato")
                Log.d(TAG, "   UID: ${currentUser.uid}")
                Log.d(TAG, "   Email: ${currentUser.email ?: "anonymous"}")
                Log.d(TAG, "   Is Anonymous: ${currentUser.isAnonymous}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERRORE FirebaseAuth: ${e.message}", e)
        }
    }

    private fun checkFirestore() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "✅ Firestore inizializzato")

            // Tenta una semplice lettura per verificare connessione
            firestore.collection("test")
                .document("connection")
                .get()
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Firestore: Connessione OK")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Firestore: Errore connessione - ${e.message}", e)
                }

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERRORE Firestore: ${e.message}", e)
        }
    }

    private fun checkNetworkPermissions(context: Context) {
        try {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager

            val activeNetwork = connectivity.activeNetworkInfo

            if (activeNetwork?.isConnected == true) {
                Log.d(TAG, "✅ Network: Connesso (${activeNetwork.typeName})")
            } else {
                Log.w(TAG, "⚠️ Network: Nessuna connessione attiva")
                Log.w(TAG, "   Firebase potrebbe non funzionare senza rete")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERRORE Network check: ${e.message}", e)
        }
    }

    /**
     * Effettua un test completo di login anonimo.
     */
    fun testAnonymousLogin(onResult: (Boolean, String) -> Unit) {
        Log.d(TAG, "🔐 Testing anonymous login...")

        val auth = FirebaseAuth.getInstance()

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val user = result.user
                Log.d(TAG, "✅ Login anonimo RIUSCITO")
                Log.d(TAG, "   UID: ${user?.uid}")
                onResult(true, "Login OK: ${user?.uid}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Login anonimo FALLITO: ${e.message}", e)
                onResult(false, "Login FAILED: ${e.message}")
            }
    }
}