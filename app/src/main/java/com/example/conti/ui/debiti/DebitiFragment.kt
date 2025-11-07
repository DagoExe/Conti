package com.example.conti.ui.debiti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.conti.databinding.FragmentDebitiBinding

/**
 * Fragment per la sezione Debiti.
 *
 * Gestisce il tracciamento di debiti personali, prestiti da amici/familiari,
 * e soldi prestati ad altri.
 */
class DebitiFragment : Fragment() {

    private var _binding: FragmentDebitiBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebitiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        binding.tvPlaceholder.text = """
            📝 Debiti e Crediti
            
            Benvenuto nella sezione debiti!
            
            Qui potrai:
            • Tracciare debiti verso amici/familiari
            • Registrare soldi prestati ad altri
            • Impostare promemoria di pagamento
            • Tenere uno storico dei rimborsi
            
            🚧 Funzionalità in arrivo...
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}