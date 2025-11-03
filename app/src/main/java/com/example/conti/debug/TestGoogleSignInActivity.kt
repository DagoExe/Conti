package com.example.conti.debug

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.conti.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import java.security.MessageDigest

/**
 * Activity di test per verificare la configurazione Google Sign-In
 */
class TestGoogleSignInActivity : AppCompatActivity() {

    private lateinit var tvResults: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout semplice
        setContentView(android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)

            addView(TextView(context).apply {
                text = "🔍 Google Sign-In Configuration Test"
                textSize = 20f
                setPadding(0, 0, 0, 32)
            })

            addView(Button(context).apply {
                text = "Run Tests"
                setOnClickListener { runTests() }
            })

            tvResults = TextView(context).apply {
                textSize = 12f
                setPadding(16, 16, 16, 16)
                setTextIsSelectable(true)
            }
            addView(tvResults)
        })

        // Esegui test automaticamente
        runTests()
    }

    private fun runTests() {
        val results = StringBuilder()

        results.append("═══════════════════════════════════\n")
        results.append("GOOGLE SIGN-IN CONFIGURATION TEST\n")
        results.append("═══════════════════════════════════\n\n")

        // Test 1: Firebase App
        try {
            val app = FirebaseApp.getInstance()
            results.append("✅ Firebase App: ${app.name}\n")
            results.append("   Project ID: ${app.options.projectId}\n")
            results.append("   App ID: ${app.options.applicationId}\n")
            results.append("   API Key: ${app.options.apiKey?.substring(0, 10)}...\n\n")
        } catch (e: Exception) {
            results.append("❌ Firebase App Error: ${e.message}\n\n")
        }

        // Test 2: Package Name
        results.append("📦 Package Name: ${packageName}\n\n")

        // Test 3: SHA-1 Fingerprint
        try {
            val signatures = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES).signatures
            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val sha1 = md.digest().joinToString(":") { "%02X".format(it) }
                results.append("🔐 SHA-1: $sha1\n")
                results.append("\n⚠️ IMPORTANTE:\n")
                results.append("Questa SHA-1 DEVE essere aggiunta in:\n")
                results.append("Firebase Console → Project Settings → \n")
                results.append("Your apps → Android app → Add fingerprint\n\n")
            }
        } catch (e: Exception) {
            results.append("❌ SHA-1 Error: ${e.message}\n\n")
        }

        // Test 4: Web Client ID
        val webClientId = "404921205660-eljajs4jgjjdl91ebis8on4mghgqib85.apps.googleusercontent.com"
        results.append("🌐 Web Client ID: \n${webClientId}\n\n")

        // Test 5: Google Sign-In Configuration
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            val client = GoogleSignIn.getClient(this, gso)
            results.append("✅ GoogleSignInClient created successfully\n\n")
        } catch (e: Exception) {
            results.append("❌ GoogleSignInClient Error: ${e.message}\n\n")
        }

        // Test 6: Check if already signed in
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (lastSignedInAccount != null) {
            results.append("📱 Last Signed In Account:\n")
            results.append("   Email: ${lastSignedInAccount.email}\n")
            results.append("   ID Token: ${lastSignedInAccount.idToken != null}\n\n")
        } else {
            results.append("ℹ️ No previous sign-in found\n\n")
        }

        results.append("═══════════════════════════════════\n")
        results.append("NEXT STEPS:\n")
        results.append("═══════════════════════════════════\n")
        results.append("1. Copy the SHA-1 above\n")
        results.append("2. Go to Firebase Console\n")
        results.append("3. Add the SHA-1 to your Android app\n")
        results.append("4. Download new google-services.json\n")
        results.append("5. Replace the file in app/\n")
        results.append("6. Clean and rebuild the app\n")

        tvResults.text = results.toString()

        // Log anche in Logcat
        android.util.Log.d("GoogleSignInTest", results.toString())
    }
}