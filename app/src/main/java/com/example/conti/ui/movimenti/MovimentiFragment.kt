package com.example.conti.ui.movimenti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conti.databinding.FragmentMovimentiBinding
import com.example.conti.models.Transaction
import com.example.conti.ui.adapters.TransactionAdapter
import com.example.conti.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Fragment per la sezione Movimenti.
 *
 * ✅ Mostra tutti i movimenti o filtrati per conto
 * ✅ Visualizza statistiche (entrate/uscite)
 */
class MovimentiFragment : Fragment() {

    private var _binding: FragmentMovimentiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimentiViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    // Argomenti passati dalla navigazione
    private var accountId: String? = null
    private var accountName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recupera argomenti dalla navigazione
        arguments?.let { args ->
            accountId = args.getString("accountId")
            accountName = args.getString("accountName")

            android.util.Log.d("MovimentiFragment", "📨 Ricevuti argomenti:")
            android.util.Log.d("MovimentiFragment", "   accountId: $accountId")
            android.util.Log.d("MovimentiFragment", "   accountName: $accountName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovimentiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        loadData()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { transaction ->
            onTransactionClicked(transaction)
        }

        binding.rvMovimenti.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is MovimentiUiState.Loading -> {
                        showLoading(true)
                    }
                    is MovimentiUiState.Empty -> {
                        showLoading(false)
                        showEmptyState(state.message)
                    }
                    is MovimentiUiState.Success -> {
                        showLoading(false)
                        showTransactions(state)
                    }
                    is MovimentiUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun loadData() {
        if (accountId != null) {
            // Mostra movimenti filtrati per conto
            android.util.Log.d("MovimentiFragment", "📊 Caricamento movimenti per conto: $accountId")
            viewModel.loadMovimentiByAccount(accountId!!)

            // Aggiorna titolo
            binding.tvTitoloSezione.text = "Movimenti di $accountName"
        } else {
            // Mostra tutti i movimenti
            android.util.Log.d("MovimentiFragment", "📊 Caricamento tutti i movimenti")
            viewModel.loadAllMovimenti()

            binding.tvTitoloSezione.text = "Tutti i Movimenti"
        }
    }

    private fun showLoading(loading: Boolean) {
        if (loading) {
            binding.layoutContent.visibility = View.GONE
            binding.tvPlaceholder.visibility = View.VISIBLE
            binding.tvPlaceholder.text = "⏳ Caricamento movimenti..."
        }
    }

    private fun showEmptyState(message: String) {
        binding.layoutContent.visibility = View.GONE
        binding.tvPlaceholder.visibility = View.VISIBLE

        binding.tvPlaceholder.text = """
            📊 $message
            
            ${if (accountId != null) "Non ci sono ancora movimenti per questo conto." else "Non ci sono ancora movimenti."}
            
            💡 I movimenti verranno mostrati qui quando:
            • Importi transazioni da Excel
            • Aggiungi movimenti manualmente
        """.trimIndent()
    }

    private fun showTransactions(state: MovimentiUiState.Success) {
        binding.tvPlaceholder.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE

        // Aggiorna statistiche
        binding.tvTotaleEntrate.text = CurrencyUtils.formatImporto(state.totalIncome)
        binding.tvTotaleUscite.text = CurrencyUtils.formatImporto(state.totalExpenses)

        val bilancio = state.totalIncome - state.totalExpenses
        binding.tvBilancio.text = CurrencyUtils.formatImporto(bilancio)

        // Colore bilancio
        val bilancioColor = if (bilancio >= 0) {
            android.graphics.Color.parseColor("#4CAF50")
        } else {
            android.graphics.Color.parseColor("#F44336")
        }
        binding.tvBilancio.setTextColor(bilancioColor)

        // Aggiorna lista movimenti
        transactionAdapter.submitList(state.transactions)

        android.util.Log.d("MovimentiFragment", "✅ Visualizzati ${state.transactions.size} movimenti")
    }

    private fun showError(message: String) {
        binding.layoutContent.visibility = View.GONE
        binding.tvPlaceholder.visibility = View.VISIBLE

        binding.tvPlaceholder.text = """
            ❌ Errore
            
            $message
            
            Riprova più tardi o controlla la connessione.
        """.trimIndent()

        android.util.Log.e("MovimentiFragment", "❌ Errore: $message")
    }

    private fun onTransactionClicked(transaction: Transaction) {
        android.util.Log.d("MovimentiFragment", "🖱️ Click su movimento: ${transaction.description}")

        // TODO: Mostra dettaglio movimento
        android.widget.Toast.makeText(
            requireContext(),
            transaction.description,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}