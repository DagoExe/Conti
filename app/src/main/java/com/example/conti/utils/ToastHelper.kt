package com.example.conti.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.conti.R

/**
 * ✨ TOAST HELPER - Posizionamento Personalizzato
 *
 * Questa classe gestisce la creazione e visualizzazione di toast personalizzati
 * con posizionamento nella parte INFERIORE dello schermo.
 *
 * Features:
 * - Toast di successo (verde) e errore (rosso)
 * - Posizionamento in basso con margine dal bordo
 * - Layout personalizzati da Figma
 * - Durata configurabile
 *
 * Usage:
 *   ToastHelper.showSuccess(context, "Operazione completata!")
 *   ToastHelper.showError(context, "Si è verificato un errore")
 */
object ToastHelper {

    /**
     * Mostra un toast di SUCCESSO nella parte inferiore dello schermo
     *
     * @param context Context dell'applicazione
     * @param message Messaggio da mostrare
     * @param duration Toast.LENGTH_SHORT (default) o Toast.LENGTH_LONG
     */
    fun showSuccess(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        showCustomToast(
            context = context,
            message = message,
            layoutResId = R.layout.layout_toast_success,
            duration = duration
        )
    }

    /**
     * Mostra un toast di ERRORE nella parte inferiore dello schermo
     *
     * @param context Context dell'applicazione
     * @param message Messaggio da mostrare
     * @param duration Toast.LENGTH_SHORT (default) o Toast.LENGTH_LONG
     */
    fun showError(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        showCustomToast(
            context = context,
            message = message,
            layoutResId = R.layout.layout_toast_error,
            duration = duration
        )
    }

    /**
     * Metodo interno per creare e mostrare toast personalizzati
     *
     * @param context Context dell'applicazione
     * @param message Messaggio da mostrare
     * @param layoutResId ID del layout custom (toast_success o toast_error)
     * @param duration Durata del toast
     */
    private fun showCustomToast(
        context: Context,
        message: String,
        layoutResId: Int,
        duration: Int
    ) {
        // Inflater del layout personalizzato
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(layoutResId, null)

        // Imposta il messaggio nel TextView
        val textView = layout.findViewById<TextView>(R.id.tvToastMessage)
        textView.text = message

        // Crea il Toast con il layout personalizzato
        val toast = Toast(context).apply {
            // ⭐ POSIZIONAMENTO IN BASSO
            setGravity(
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                0,  // offset X (0 = centrato orizzontalmente)
                120 // offset Y (distanza dal bordo inferiore in dp - circa 120dp)
            )
            this.duration = duration
            view = layout
        }

        // Mostra il toast
        toast.show()
    }

    /**
     * Variante con margine personalizzabile dal bordo inferiore
     *
     * @param context Context dell'applicazione
     * @param message Messaggio da mostrare
     * @param isSuccess true per toast success, false per toast error
     * @param duration Durata del toast
     * @param bottomMarginDp Margine dal bordo inferiore in dp (default 120dp)
     */
    fun showCustomBottomToast(
        context: Context,
        message: String,
        isSuccess: Boolean = true,
        duration: Int = Toast.LENGTH_SHORT,
        bottomMarginDp: Int = 120
    ) {
        val layoutResId = if (isSuccess) {
            R.layout.layout_toast_success
        } else {
            R.layout.layout_toast_error
        }

        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(layoutResId, null)

        val textView = layout.findViewById<TextView>(R.id.tvToastMessage)
        textView.text = message

        val toast = Toast(context).apply {
            setGravity(
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                0,
                bottomMarginDp
            )
            this.duration = duration
            view = layout
        }

        toast.show()
    }

    /**
     * Mostra toast sopra la Bottom Navigation Bar (circa 80dp dal bordo)
     * Utile quando hai una bottom bar fissa
     */
    fun showAboveBottomNav(
        context: Context,
        message: String,
        isSuccess: Boolean = true,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        showCustomBottomToast(
            context = context,
            message = message,
            isSuccess = isSuccess,
            duration = duration,
            bottomMarginDp = 80 // Più vicino al bordo per stare sopra la bottom nav
        )
    }
}