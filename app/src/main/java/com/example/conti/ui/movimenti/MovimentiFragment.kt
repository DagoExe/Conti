package com.example.conti.ui.movimenti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.conti.R

/**
 * MovimentiFragment - Visualizza tutti i movimenti di tutti i conti.
 *
 * TODO: Implementare la lista completa dei movimenti con filtri.
 */
class MovimentiFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_movimenti, container, false)

        val textView = root.findViewById<TextView>(R.id.tvPlaceholder)
        textView.text = "📝 Movimenti\n\nQuesta sezione mostrerà tutti i movimenti\ndi tutti i conti con filtri per data e categoria."

        return root
    }
}