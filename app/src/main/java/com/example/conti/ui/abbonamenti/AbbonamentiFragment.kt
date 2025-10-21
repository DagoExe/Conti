package com.example.conti.ui.abbonamenti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.conti.R

/**
 * AbbonamentiFragment - Gestisce gli abbonamenti e spese ricorrenti.
 *
 * TODO: Implementare la lista degli abbonamenti.
 */
class AbbonamentiFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_abbonamenti, container, false)

        val textView = root.findViewById<TextView>(R.id.tvPlaceholder)
        textView.text = "📅 Abbonamenti\n\nQuesta sezione mostrerà tutti gli abbonamenti attivi\ne le spese ricorrenti."

        return root
    }
}