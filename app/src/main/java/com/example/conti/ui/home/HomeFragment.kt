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
            onContoClick = { conto ->
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
        // Osserva la lista dei conti
        viewModel.conti.observe(viewLifecycleOwner) { conti ->
            if (conti.isEmpty()) {
                binding.rvConti.visibility = View.GONE
                binding.layoutNoConti.visibility = View.VISIBLE
                binding.tvNumeroConti.text = "0 conti"
            } else {
                binding.rvConti.visibility = View.VISIBLE
                binding.layoutNoConti.visibility = View.GONE
                binding.tvNumeroConti.text = "${conti.size} ${if (conti.size == 1) "conto" else "conti"}"

                // Log per debug
                android.util.Log.d("HomeFragment", "📊 Conti caricati: ${conti.size}")
                conti.forEach { conto ->
                    android.util.Log.d("HomeFragment", "  - ${conto.nome} (${conto.istituto})")
                }

                // Calcola e mostra il saldo totale
                calcolaSaldoTotale()
            }
        }

        // Osserva le statistiche del mese corrente
        viewModel.movimentiMeseCorrente.observe(viewLifecycleOwner) { movimenti ->
            val entrate = movimenti.filter { it.importo > 0 }.sumOf { it.importo }
            val uscite = movimenti.filter { it.importo < 0 }.sumOf { kotlin.math.abs(it.importo) }

            binding.tvEntrateMese.text = CurrencyUtils.formatImporto(entrate)
            binding.tvUsciteMese.text = CurrencyUtils.formatImporto(uscite)
        }

        // Osserva gli abbonamenti
        viewModel.numeroAbbonamentiAttivi.observe(viewLifecycleOwner) { numero ->
            binding.tvNumeroAbbonamenti.text = numero.toString()
        }

        viewModel.costoAbbonamentiMensile.observe(viewLifecycleOwner) { costo ->
            binding.tvCostoAbbonamenti.text = CurrencyUtils.formatImporto(costo)
        }
    }

    /**
     * Calcola il saldo totale di tutti i conti.
     */
    private fun calcolaSaldoTotale() {
        viewModel.conti.value?.let { conti ->
            if (conti.isEmpty()) {
                binding.tvSaldoTotale.text = CurrencyUtils.formatImporto(0.0)
                return
            }

            // Somma il saldo iniziale di tutti i conti
            val saldoInizialeTotale = conti.sumOf { it.saldoIniziale }

            // Osserva la somma dei movimenti di tutti i conti
            // Questo è un approccio semplificato - in produzione useresti Combine o Flow
            var saldoMovimentiTotale = 0.0
            var contiProcessati = 0

            conti.forEach { conto ->
                viewModel.repository.getSaldoByContoId(conto.id)
                    .asLiveData()
                    .observe(viewLifecycleOwner) { saldoMovimenti ->
                        saldoMovimentiTotale += saldoMovimenti
                        contiProcessati++

                        // Aggiorna solo quando tutti i conti sono stati processati
                        if (contiProcessati == conti.size) {
                            val saldoTotale = saldoInizialeTotale + saldoMovimentiTotale
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
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}