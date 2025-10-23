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
 * Mostra:
 * - Saldo totale di tutti i conti
 * - Statistiche del mese corrente (entrate/uscite)
 * - Riepilogo abbonamenti attivi
 * - Lista dei conti con saldi
 *
 * VERSIONE FIRESTORE - CON GESTIONE ERRORI MIGLIORATA
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
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore onCreateView", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            setupRecyclerView()

            // SOLO accounts (niente stats, niente subscriptions)
            viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
                contiAdapter.submitList(accounts)

                if (accounts.isEmpty()) {
                    binding.rvConti.visibility = View.GONE
                    binding.layoutNoConti.visibility = View.VISIBLE
                } else {
                    binding.rvConti.visibility = View.VISIBLE
                    binding.layoutNoConti.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Errore", e)
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
                // findNavController().navigate(...)
            }
        )

        binding.rvConti.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contiAdapter
        }
    }

    /**
     * Configura gli observer per i LiveData del ViewModel.
     * VERSIONE SICURA: controlla che viewLifecycleOwner sia valido.
     */
    private fun setupObservers() {
        // Verifica che viewLifecycleOwner sia inizializzato
        if (!viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.INITIALIZED)) {
            android.util.Log.w("HomeFragment", "⚠️ ViewLifecycleOwner non ancora inizializzato")
            return
        }

        try {
            // Osserva la lista degli account
            viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
                updateAccountsUI(accounts)
            }

            // Osserva le statistiche del mese corrente
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    viewModel.monthlyStats.collect { stats ->
                        updateMonthlyStatsUI(stats)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeFragment", "❌ Errore raccolta monthlyStats", e)
                }
            }

            // Osserva gli abbonamenti
            viewModel.activeSubscriptions.observe(viewLifecycleOwner) { subscriptions: List<com.example.conti.models.Subscription> ->
                updateSubscriptionsUI(subscriptions)
            }


        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore setupObservers", e)
            showError("Errore caricamento dati: ${e.message}")
        }
    }

    /**
     * Aggiorna l'UI con la lista degli account.
     */
    private fun updateAccountsUI(accounts: List<com.example.conti.models.Account>) {
        try {
            if (accounts.isEmpty()) {
                binding.rvConti.visibility = View.GONE
                binding.layoutNoConti.visibility = View.VISIBLE
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
     * Aggiorna l'UI con le statistiche mensili.
     */
    private fun updateMonthlyStatsUI(stats: HomeViewModel.MonthlyStats) {
        try {
            binding.tvEntrateMese.text = CurrencyUtils.formatImporto(stats.totalIncome)
            binding.tvUsciteMese.text = CurrencyUtils.formatImporto(stats.totalExpenses)
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore updateMonthlyStatsUI", e)
        }
    }

    /**
     * Aggiorna l'UI con gli abbonamenti.
     */
    private fun updateSubscriptionsUI(subscriptions: List<com.example.conti.models.Subscription>) {
        try {
            binding.tvNumeroAbbonamenti.text = subscriptions.size.toString()

            val costoMensile = subscriptions.sumOf { it.getMonthlyCost() }
            binding.tvCostoAbbonamenti.text = CurrencyUtils.formatImporto(costoMensile)
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore updateSubscriptionsUI", e)
        }
    }

    /**
     * Mostra un messaggio di errore all'utente.
     */
    private fun showError(message: String) {
        try {
            // Nasconde tutto e mostra messaggio
            binding.rvConti.visibility = View.GONE
            binding.layoutNoConti.visibility = View.VISIBLE

            // Usa i TextView del layout per mostrare l'errore
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
            _binding = null
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "❌ Errore onDestroyView", e)
        }
    }
}