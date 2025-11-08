package com.example.conti.auth

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
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
}