package com.example.conti.debug

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.conti.R
import com.example.conti.auth.AuthManager
import com.example.conti.utils.FirebaseDiagnostic
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 🔧 TEST ACTIVITY - Activity di debug per testare Firebase
 *
 * COME USARLA:
 * 1. Aggiungi questa activity nel Manifest:
 * ```xml
 * <activity
 *     android:name="com.example.conti.debug.FirebaseTestActivity"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.MAIN" />
 *         <category android:name="android.intent.category.LAUNCHER" />
 *     </intent-filter>
 * </activity>
 * ```
 *
 * 2. Commenta temporaneamente LoginActivity come LAUNCHER
 * 3. Lancia l'app e usa i pulsanti per testare
 */
class FirebaseTestActivity : AppCompatActivity() {

    private lateinit var tvResults: TextView
    private lateinit var scrollView: android.widget.ScrollView // ✅ Aggiungi riferimento
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout programmatico per semplicità
        setContentView(createLayout())

        tvResults = findViewById(R.id.tvResults)
        authManager = AuthManager.getInstance()

        setupButtons()

        log("🔥 Firebase Test Activity Pronta")
        log("Premi i pulsanti per testare Firebase")
    }

    private fun createLayout(): android.view.View {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Titolo
        layout.addView(TextView(this).apply {
            text = "🔧 Firebase Test"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        })

        // Pulsante Diagnostica
        layout.addView(Button(this).apply {
            id = android.view.View.generateViewId()
            text = "1️⃣ Esegui Diagnostica Firebase"
            setOnClickListener { runDiagnostic() }
        })

        // Pulsante Test Connessione
        layout.addView(Button(this).apply {
            id = android.view.View.generateViewId()
            text = "2️⃣ Test Connessione Firestore"
            setOnClickListener { testFirestoreConnection() }
        })

        // Pulsante Test Login Anonimo
        layout.addView(Button(this).apply {
            id = android.view.View.generateViewId()
            text = "3️⃣ Test Login Anonimo"
            setOnClickListener { testAnonymousLogin() }
        })

        // Pulsante Test Registrazione
        layout.addView(Button(this).apply {
            id = android.view.View.generateViewId()
            text = "4️⃣ Test Registrazione"
            setOnClickListener { testSignUp() }
        })

        // Pulsante Test Login
        layout.addView(Button(this).apply {
            id = android.view.View.generateViewId()
            text = "5️⃣ Test Login"
            setOnClickListener { testSignIn() }
        })

        // Pulsante Clear
        layout.addView(Button(this).apply {
            id = android.view.View.generateViewId()
            text = "🗑️ Pulisci Log"
            setOnClickListener { clearLog() }
        })

        // ScrollView con TextView per risultati
        scrollView = android.widget.ScrollView(this).apply { // ✅ Salva riferimento
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        tvResults = TextView(this).apply {
            id = R.id.tvResults
            textSize = 12f
            setPadding(16, 16, 16, 16)
            setTextIsSelectable(true)
            typeface = android.graphics.Typeface.MONOSPACE
        }

        scrollView.addView(tvResults)
        layout.addView(scrollView)

        return layout
    }

    private fun setupButtons() {
        // I listener sono già impostati nel createLayout()
    }

    // ========================================
    // TEST 1: Diagnostica Firebase
    // ========================================

    private fun runDiagnostic() {
        log("═══════════════════════════════════════")
        log("TEST 1: DIAGNOSTICA FIREBASE")
        log("═══════════════════════════════════════")

        FirebaseDiagnostic.runDiagnostic(applicationContext)

        log("\n✅ Diagnostica completata - Controlla Logcat per dettagli")
    }

    // ========================================
    // TEST 2: Test Connessione Firestore
    // ========================================

    private fun testFirestoreConnection() {
        log("═══════════════════════════════════════")
        log("TEST 2: CONNESSIONE FIRESTORE")
        log("═══════════════════════════════════════")

        lifecycleScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                log("✅ Istanza Firestore ottenuta")

                // Prova a leggere da una collezione di test
                log("🔄 Tentativo di lettura da Firestore...")

                val testDoc = firestore.collection("test").document("connection").get().await()

                if (testDoc.exists()) {
                    log("✅ Documento test TROVATO")
                    log("   Dati: ${testDoc.data}")
                } else {
                    log("ℹ️ Documento test NON ESISTE (normale)")
                }

                log("✅ Connessione Firestore OK!")

            } catch (e: Exception) {
                log("❌ ERRORE Connessione Firestore:")
                log("   ${e.javaClass.simpleName}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ========================================
    // TEST 3: Login Anonimo
    // ========================================

    private fun testAnonymousLogin() {
        log("═══════════════════════════════════════")
        log("TEST 3: LOGIN ANONIMO")
        log("═══════════════════════════════════════")

        lifecycleScope.launch {
            authManager.signInAnonymously()
                .onSuccess { user ->
                    log("✅ Login anonimo RIUSCITO!")
                    log("   User ID: ${user.uid}")
                    log("   Is Anonymous: ${user.isAnonymous}")
                }
                .onFailure { e ->
                    log("❌ Login anonimo FALLITO:")
                    log("   ${e.javaClass.simpleName}: ${e.message}")
                }
        }
    }

    // ========================================
    // TEST 4: Registrazione
    // ========================================

    private fun testSignUp() {
        log("═══════════════════════════════════════")
        log("TEST 4: REGISTRAZIONE")
        log("═══════════════════════════════════════")

        val testEmail = "test_${System.currentTimeMillis()}@example.com"
        val testPassword = "password123"
        val testName = "Test User"

        log("📧 Email: $testEmail")
        log("🔒 Password: $testPassword")
        log("👤 Nome: $testName")
        log("")

        lifecycleScope.launch {
            authManager.signUpWithEmail(testEmail, testPassword, testName)
                .onSuccess { user ->
                    log("✅ Registrazione RIUSCITA!")
                    log("   User ID: ${user.uid}")
                    log("   Email: ${user.email}")
                    log("   Is Email Verified: ${user.isEmailVerified}")
                }
                .onFailure { e ->
                    log("❌ Registrazione FALLITA:")
                    log("   ${e.javaClass.simpleName}: ${e.message}")
                }
        }
    }

    // ========================================
    // TEST 5: Login con Email
    // ========================================

    private fun testSignIn() {
        log("═══════════════════════════════════════")
        log("TEST 5: LOGIN CON EMAIL")
        log("═══════════════════════════════════════")

        // Usa le credenziali del test di registrazione precedente
        // NOTA: Devi prima eseguire il Test 4 per creare l'utente

        log("⚠️ ATTENZIONE: Devi prima eseguire il Test 4 (Registrazione)")
        log("   e poi inserire qui le credenziali create.")
        log("")

        val testEmail = "test@example.com" // Modifica con email valida
        val testPassword = "password123"

        log("📧 Email: $testEmail")
        log("🔒 Password: $testPassword")
        log("")

        lifecycleScope.launch {
            authManager.signInWithEmail(testEmail, testPassword)
                .onSuccess { user ->
                    log("✅ Login RIUSCITO!")
                    log("   User ID: ${user.uid}")
                    log("   Email: ${user.email}")
                }
                .onFailure { e ->
                    log("❌ Login FALLITO:")
                    log("   ${e.javaClass.simpleName}: ${e.message}")
                    log("")
                    log("💡 Assicurati di aver prima creato l'utente con il Test 4")
                }
        }
    }

    // ========================================
    // UTILITY
    // ========================================

    private fun log(message: String) {
        android.util.Log.d("FirebaseTest", message)
        runOnUiThread {
            tvResults.append("$message\n")

            // Auto-scroll in fondo usando il riferimento salvato
            scrollView.fullScroll(android.view.View.FOCUS_DOWN) // ✅ Usa riferimento diretto
        }
    }

    private fun clearLog() {
        tvResults.text = ""
        log("🗑️ Log pulito")
    }
}