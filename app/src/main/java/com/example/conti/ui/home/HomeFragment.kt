package com.example.conti.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conti.ContiApplication
import com.example.conti.databinding.FragmentHomeBinding
import com.example.conti.ui.ViewModelFactory
import com.example.conti.ui.adapters.ContiAdapter
import com.example.conti.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * HomeFragment - Schermata principale dell'app.
 *
 * ✅ VERSIONE CORRETTA CON:
 * - Migliore gestione errori
 * - Logging dettagliato
 * - Inizializzazione sicura
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        try {
            val application = requireActivity().application as ContiApplication
            ViewModelFactory(application.repository)
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore creazione ViewModel", e)
            throw e
        }
    }

    private lateinit var contiAdapter: ContiAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            android.util.Log.d("HomeFragment", "=== onCreateView START ===")
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            android.util.Log.d("HomeFragment", "✅ Binding inflated correttamente")
            return binding.root
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore onCreateView", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            android.util.Log.d("HomeFragment", "=== onViewCreated START ===")

            // 1. Setup RecyclerView
            setupRecyclerView()
            android.util.Log.d("HomeFragment", "✅ RecyclerView configurata")

            // 2. Mostra layout vuoto inizialmente
            showEmptyState()
            android.util.Log.d("HomeFragment", "✅ Layout vuoto mostrato")

            // 3. Osserva gli account con gestione errori
            setupAccountsObserver()
            android.util.Log.d("HomeFragment", "✅ Observer configurato")

            android.util.Log.d("HomeFragment", "=== onViewCreated END ===")
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ ERRORE CRITICO in onViewCreated", e)
            showError("Errore caricamento: ${e.message}")
        }
    }

    /**
     * Configura la RecyclerView per mostrare la lista dei conti.
     */
    private fun setupRecyclerView() {
        contiAdapter = ContiAdapter(
            onContoClick = { account ->
                android.util.Log.d("HomeFragment", "Conto cliccato: ${account.name}")
                // TODO: Navigare al dettaglio del conto
            }
        )

        binding.rvConti.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contiAdapter
        }
    }

    /**
     * Configura l'observer per gli account con gestione errori robusta.
     */
    private fun setupAccountsObserver() {
        try {
            viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
                try {
                    android.util.Log.d("HomeFragment", "📊 Ricevuti ${accounts.size} account")
                    updateAccountsUI(accounts)
                } catch (e: Exception) {
                    android.util.Log.e("HomeFragment", "❌ Errore aggiornamento UI", e)
                    showError("Errore visualizzazione conti")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore setup observer", e)
            showError("Errore inizializzazione")
        }
    }

    /**
     * Aggiorna l'UI con la lista degli account.
     */
    private fun updateAccountsUI(accounts: List<com.example.conti.models.Account>) {
        try {
            if (accounts.isEmpty()) {
                showEmptyState()
                binding.tvNumeroConti.text = "0 conti"
                binding.tvSaldoTotale.text = CurrencyUtils.formatImporto(0.0)
            } else {
                binding.rvConti.visibility = View.VISIBLE
                binding.layoutNoConti.visibility = View.GONE
                binding.tvNumeroConti.text = "${accounts.size} ${if (accounts.size == 1) "conto" else "conti"}"

                contiAdapter.submitList(accounts)

                // Calcola saldo totale
                val saldoTotale = accounts.sumOf { it.balance }
                binding.tvSaldoTotale.text = CurrencyUtils.formatImporto(saldoTotale)

                // Cambia colore in base al saldo
                val colore = when {
                    saldoTotale > 0 -> android.graphics.Color.parseColor("#4CAF50")
                    saldoTotale < 0 -> android.graphics.Color.parseColor("#F44336")
                    else -> android.graphics.Color.WHITE
                }
                binding.tvSaldoTotale.setTextColor(colore)
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore updateAccountsUI", e)
        }
    }

    /**
     * Mostra lo stato vuoto.
     */
    private fun showEmptyState() {
        binding.rvConti.visibility = View.GONE
        binding.layoutNoConti.visibility = View.VISIBLE
    }

    /**
     * Mostra un messaggio di errore all'utente.
     */
    private fun showError(message: String) {
        try {
            binding.rvConti.visibility = View.GONE
            binding.layoutNoConti.visibility = View.VISIBLE

            val errorView = binding.root.findViewById<android.widget.TextView>(
                com.example.conti.R.id.tvEmptyMessage
            )

            if (errorView != null) {
                errorView.text = "⚠️ Errore"

                val descView = binding.root.findViewById<android.widget.TextView>(
                    com.example.conti.R.id.tvEmptyDescription
                )
                descView?.text = message
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore showError", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            android.util.Log.d("HomeFragment", "=== onDestroyView ===")
            _binding = null
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore onDestroyView", e)
        }
    }
}