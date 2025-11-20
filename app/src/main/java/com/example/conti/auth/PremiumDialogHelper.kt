package com.example.conti.auth

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import com.example.conti.R
import com.google.android.material.button.MaterialButton

/**
 * 🎨 PremiumDialogHelper - Helper per creare dialog custom in stile MONIO premium
 *
 * Questa classe gestisce la creazione di dialog personalizzati che seguono
 * il design system premium del progetto (sfondo scuro, gradiente gold, etc.)
 *
 * Design Reference: Figma "MONIO" - Dialog Components
 */
object PremiumDialogHelper {

    /**
     * 📧 Dialog di Verifica Email
     *
     * Mostra un dialog che richiede all'utente di verificare la propria email.
     * Include 3 azioni: verifica, ri-invio email, logout.
     *
     * @param context Context dell'activity
     * @param email Email dell'utente da mostrare
     * @param onVerifyClick Callback quando l'utente clicca "Ho verificato"
     * @param onResendClick Callback quando l'utente clicca "Ri-invia email"
     * @param onLogoutClick Callback quando l'utente clicca "Logout"
     * @return Dialog configurato (non ancora mostrato)
     */
    fun showEmailVerificationDialog(
        context: Context,
        email: String,
        onVerifyClick: () -> Unit,
        onResendClick: () -> Unit,
        onLogoutClick: () -> Unit
    ): Dialog {

        // Crea il dialog con layout custom
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        // Infla il layout custom
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_email_verification, null)
        dialog.setContentView(view)

        // Background trasparente per mostrare i corner radius del CardView
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Configura l'email badge
        val emailBadge = view.findViewById<TextView>(R.id.tvEmailBadge)
        emailBadge.text = email

        // Configura i pulsanti
        val btnPrimary = view.findViewById<MaterialButton>(R.id.btnPrimaryAction)
        val btnSecondary = view.findViewById<MaterialButton>(R.id.btnSecondaryAction)
        val btnTertiary = view.findViewById<MaterialButton>(R.id.btnTertiaryAction)

        btnPrimary.setOnClickListener {
            onVerifyClick()
            dialog.dismiss()
        }

        btnSecondary.setOnClickListener {
            onResendClick()
            // Non dismissiamo il dialog, l'utente può continuare
        }

        btnTertiary.setOnClickListener {
            onLogoutClick()
            dialog.dismiss()
        }

        // Mostra il dialog
        dialog.show()

        return dialog
    }

    /**
     * ✅ Dialog di Account Creato
     *
     * Mostra un dialog di conferma quando l'account è stato creato con successo.
     * Include istruzioni per la verifica email.
     *
     * @param context Context dell'activity
     * @param email Email dell'utente da mostrare
     * @param onOkClick Callback quando l'utente clicca "OK"
     * @return Dialog configurato (non ancora mostrato)
     */
    fun showAccountCreatedDialog(
        context: Context,
        email: String,
        onOkClick: () -> Unit
    ): Dialog {

        // Crea il dialog con layout custom
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        // Infla il layout custom
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_account_created, null)
        dialog.setContentView(view)

        // Background trasparente per mostrare i corner radius del CardView
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Configura l'email badge
        val emailBadge = view.findViewById<TextView>(R.id.tvEmailBadge)
        emailBadge.text = email

        // Configura il pulsante OK
        val btnOk = view.findViewById<MaterialButton>(R.id.btnOk)
        btnOk.setOnClickListener {
            onOkClick()
            dialog.dismiss()
        }

        // Mostra il dialog
        dialog.show()

        return dialog
    }

    /**
     * 🎯 Dialog Generico Premium
     *
     * Crea un dialog generico con lo stile premium.
     * Utile per altri casi d'uso futuri.
     *
     * @param context Context dell'activity
     * @param title Titolo del dialog
     * @param message Messaggio del dialog
     * @param positiveText Testo del pulsante positivo
     * @param onPositiveClick Callback del pulsante positivo
     * @param negativeText Testo del pulsante negativo (opzionale)
     * @param onNegativeClick Callback del pulsante negativo (opzionale)
     */
    fun showGenericDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String,
        onPositiveClick: () -> Unit,
        negativeText: String? = null,
        onNegativeClick: (() -> Unit)? = null
    ) {
        // TODO: Implementare se necessario
        // Per ora usiamo i dialog specifici sopra
    }

    /**
     * 👤 Dialog Informazioni Profilo
     *
     * Mostra un dialog con le informazioni del profilo utente
     *
     * @param context Context dell'activity
     * @param email Email dell'utente
     * @param uid User ID Firebase
     * @param onOkClick Callback quando l'utente clicca "OK"
     * @return Dialog configurato (già mostrato)
     */
    fun showProfileInfoDialog(
        context: Context,
        email: String,
        uid: String,
        onOkClick: () -> Unit
    ): Dialog {
        // Crea il dialog con layout custom
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        // Infla il layout custom
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_profile_info, null)
        dialog.setContentView(view)

        // Background trasparente per mostrare i corner radius del CardView
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Imposta i dati del profilo
        val tvEmail = view.findViewById<TextView>(R.id.tvProfileEmail)
        val tvUid = view.findViewById<TextView>(R.id.tvProfileUid)
        val btnOk = view.findViewById<MaterialButton>(R.id.btnProfileOk)

        tvEmail.text = email
        tvUid.text = uid

        // Configura il pulsante OK
        btnOk.setOnClickListener {
            onOkClick()
            dialog.dismiss()
        }

        // Mostra il dialog
        dialog.show()

        return dialog
    }

    /**
     * 🚪 Dialog Conferma Logout
     *
     * @param context Context dell'activity
     * @param onConfirm Callback quando l'utente conferma
     * @param onCancel Callback quando l'utente annulla
     */
    fun showLogoutConfirmDialog(
        context: Context,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {}
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        // Crea un layout inline per semplicità
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28.dp(context), 28.dp(context), 28.dp(context), 28.dp(context))
            setBackgroundResource(R.drawable.bg_card_dark)
        }

        // Titolo
        layout.addView(TextView(context).apply {
            text = "🚪 Logout"
            textSize = 20f
            setTextColor(context.getColor(R.color.text_primary))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 16.dp(context))
        })

        // Messaggio
        layout.addView(TextView(context).apply {
            text = "Sei sicuro di voler uscire?"
            textSize = 16f
            setTextColor(context.getColor(R.color.text_secondary))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24.dp(context))
        })

        // Pulsanti
        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Pulsante Annulla
        buttonsLayout.addView(com.google.android.material.button.MaterialButton(context).apply {
            text = "Annulla"
            layoutParams = LinearLayout.LayoutParams(
                0,
                56.dp(context),
                1f
            ).apply {
                marginEnd = 8.dp(context)
            }
            setTextColor(context.getColor(R.color.text_secondary))
            setBackgroundColor(context.getColor(R.color.zinc_800))
            setOnClickListener {
                onCancel()
                dialog.dismiss()
            }
        })

        // Pulsante Conferma
        buttonsLayout.addView(com.google.android.material.button.MaterialButton(context).apply {
            text = "Sì, esci"
            layoutParams = LinearLayout.LayoutParams(
                0,
                56.dp(context),
                1f
            ).apply {
                marginStart = 8.dp(context)
            }
            setBackgroundResource(R.drawable.bg_gold_gradient)
            setTextColor(context.getColor(R.color.zinc_950))
            setOnClickListener {
                onConfirm()
                dialog.dismiss()
            }
        })

        layout.addView(buttonsLayout)

        dialog.setContentView(layout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        return dialog
    }

    // Helper extension per convertire dp in px
    private fun Int.dp(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}