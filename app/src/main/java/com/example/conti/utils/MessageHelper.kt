package com.example.conti.utils

import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.conti.R

/**
 * ✨ MessageHelper - Toast Personalizzati Premium
 *
 * Utility class per mostrare messaggi di successo ed errore con design premium.
 *
 * Features:
 * - Toast personalizzati con layout custom
 * - Animazioni smooth
 * - Design coerente con il tema MONIO
 * - Icone appropriate per successo/errore
 * - Colori: verde emerald (successo), rosso (errore)
 *
 * Usage:
 * ```kotlin
 * MessageHelper.showSuccess(activity, "Login effettuato con successo!")
 * MessageHelper.showError(activity, "Email o password errati.")
 * ```
 *
 * @since 1.0
 * @author MONIO Dev Team
 */
object MessageHelper {

    /**
     * Mostra un messaggio di SUCCESSO con toast personalizzato
     *
     * @param activity Activity corrente per il context
     * @param message Messaggio da visualizzare
     * @param duration Durata del toast (default: Toast.LENGTH_LONG)
     */
    fun showSuccess(
        activity: Activity,
        message: String,
        duration: Int = Toast.LENGTH_LONG
    ) {
        activity.runOnUiThread {
            try {
                val inflater = LayoutInflater.from(activity)
                val layout: View = inflater.inflate(R.layout.layout_toast_success, null)

                // Imposta il testo del messaggio
                val textView = layout.findViewById<TextView>(R.id.tvToastMessage)
                textView.text = message

                // Crea e configura il Toast
                val toast = Toast(activity.applicationContext)
                toast.duration = duration
                toast.view = layout
                toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 120)
                toast.show()
            } catch (e: Exception) {
                // Fallback: usa un toast normale se c'è un errore
                Toast.makeText(activity, message, duration).show()
            }
        }
    }

    /**
     * Mostra un messaggio di ERRORE con toast personalizzato
     *
     * @param activity Activity corrente per il context
     * @param message Messaggio di errore da visualizzare
     * @param duration Durata del toast (default: Toast.LENGTH_LONG)
     */
    fun showError(
        activity: Activity,
        message: String,
        duration: Int = Toast.LENGTH_LONG
    ) {
        activity.runOnUiThread {
            try {
                val inflater = LayoutInflater.from(activity)
                val layout: View = inflater.inflate(R.layout.layout_toast_error, null)

                // Imposta il testo del messaggio
                val textView = layout.findViewById<TextView>(R.id.tvToastMessage)
                textView.text = message

                // Crea e configura il Toast
                val toast = Toast(activity.applicationContext)
                toast.duration = duration
                toast.view = layout
                toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 120)
                toast.show()
            } catch (e: Exception) {
                // Fallback: usa un toast normale se c'è un errore
                Toast.makeText(activity, message, duration).show()
            }
        }
    }

    /**
     * Mostra un messaggio INFORMATIVO generico
     * (usa lo stile di successo ma senza pretese di positività)
     *
     * @param activity Activity corrente per il context
     * @param message Messaggio informativo da visualizzare
     * @param duration Durata del toast (default: Toast.LENGTH_SHORT)
     */
    fun showInfo(
        activity: Activity,
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        // Per messaggi info generici, usa lo stesso stile del successo
        // ma con durata più breve
        showSuccess(activity, message, duration)
    }
}
