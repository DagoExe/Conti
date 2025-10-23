package com.example.conti.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conti.ContiApplication
import com.example.conti.databinding.FragmentHomeBinding
import com.example.conti.ui.ViewModelFactory
import com.example.conti.ui.adapters.ContiAdapter
import com.example.conti.utils.CurrencyUtils

/**
 * HomeFragment - Schermata principale dell'app.
 *
 * Mostra:
 * - Saldo totale di tutti i conti
 * - Statistiche del mese corrente (entrate/uscite)
 * - Riepilogo abbonamenti attivi
 * - Lista dei conti con saldi
 *
 * VERSIONE FIRESTORE
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val application = requireActivity().application as ContiApplication
        ViewModelFactory(application.repository)
    }

    private lateinit var contiAdapter: ContiAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
    }

    /**
     * Configura la RecyclerView per mostrare la lista dei conti.
     */
    private fun setupRecyclerView() {
        contiAdapter = ContiAdapter(
            onContoClick = { account ->
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
     */
    private fun setupObservers() {
        // Osserva la lista degli account
        viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
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
        }

        // Osserva le statistiche del mese corrente
        viewModel.monthlyStats.asLiveData().observe(viewLifecycleOwner) { stats ->
            binding.tvEntrateMese.text = CurrencyUtils.formatImporto(stats.totalIncome)
            binding.tvUsciteMese.text = CurrencyUtils.formatImporto(stats.totalExpenses)
        }

        // Osserva gli abbonamenti
        viewModel.activeSubscriptions.observe(viewLifecycleOwner) { subscriptions ->
            binding.tvNumeroAbbonamenti.text = subscriptions.size.toString()

            val costoMensile = subscriptions.sumOf { it.getMonthlyCost() }
            binding.tvCostoAbbonamenti.text = CurrencyUtils.formatImporto(costoMensile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}