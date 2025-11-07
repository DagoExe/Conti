package com.example.conti.ui.rate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.conti.databinding.FragmentRateBinding

/**
 * Fragment per la sezione Rate.
 *
 * Gestisce la visualizzazione e il monitoraggio delle rate di pagamento,
 * come finanziamenti, prestiti personali, mutui, ecc.
 */
class RateFragment : Fragment() {

    private var _binding: FragmentRateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        binding.tvPlaceholder.text = """
            💳 Rate e Finanziamenti
            
            Benvenuto nella sezione rate!
            
            Qui potrai:
            • Monitorare rate di finanziamenti
            • Tenere traccia di prestiti personali
            • Gestire rate del mutuo
            • Visualizzare scadenze e importi
            
            🚧 Funzionalità in arrivo...
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}