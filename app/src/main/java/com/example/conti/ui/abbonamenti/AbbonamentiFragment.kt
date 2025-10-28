package com.example.conti.ui.abbonamenti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.conti.databinding.FragmentAbbonamentiBinding

/**
 * Fragment per la sezione Abbonamenti.
 *
 * ✅ Mostra un messaggio di benvenuto.
 * 🚧 Funzionalità in sviluppo.
 */
class AbbonamentiFragment : Fragment() {

    private var _binding: FragmentAbbonamentiBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAbbonamentiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Messaggio di benvenuto
        binding.tvPlaceholder.text = """
            📅 Abbonamenti
            
            Benvenuto nella sezione abbonamenti!
            
            Qui potrai:
            • Gestire i tuoi abbonamenti ricorrenti
            • Monitorare scadenze e costi mensili
            • Ricevere notifiche di rinnovo
            
            🚧 Funzionalità in arrivo...
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}