package com.example.conti.ui.movimenti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conti.R
import com.example.conti.databinding.FragmentMovimentiBinding
import com.example.conti.models.Transaction
import com.example.conti.ui.adapters.TransactionAdapter
import com.example.conti.utils.CurrencyUtils
import com.example.conti.utils.MessageHelper
import kotlinx.coroutines.launch

/**
 * Fragment per la sezione Movimenti.
 *
 * ✅ VERSIONE AGGIORNATA:
 * - Mostra il saldo corrente dell'account (che include il saldo iniziale)
 * - Il bilancio NON è più calcolato come entrate - uscite, ma è il balance dell'account
 * - Supporta navigazione a pagina intera per aggiungere movimenti
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
        setupFAB()
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

    /**
     * Setup FAB per aggiungere movimento
     */
    private fun setupFAB() {
        // Mostra il FAB solo se abbiamo un accountId
        if (accountId != null) {
            binding.fabAggiungiMovimento.visibility = View.VISIBLE

            binding.fabAggiungiMovimento.setOnClickListener {
                showAddTransactionPage()
            }
        } else {
            binding.fabAggiungiMovimento.visibility = View.GONE
        }
    }

    /**
     * Naviga alla pagina per aggiungere movimento
     */
    private fun showAddTransactionPage() {
        if (accountId == null) {
            // ✅ Toast ERROR in basso
            MessageHelper.showError(
                requireContext(),
                "Seleziona prima un conto"
            )
            return
        }

        val bundle = bundleOf(
            AddTransactionFragment.ARG_ACCOUNT_ID to accountId,
            AddTransactionFragment.ARG_ACCOUNT_NAME to (accountName ?: "Conto")
        )

        findNavController().navigate(R.id.action_movimenti_to_add_transaction, bundle)
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

        val hintText = if (accountId != null) {
            "💡 Tocca il pulsante + per aggiungere il primo movimento!"
        } else {
            "💡 Vai alla sezione Conti per aggiungere movimenti."
        }

        binding.tvPlaceholder.text = """
            📊 $message
            
            $hintText
        """.trimIndent()
    }

    /**
     * ✅ AGGIORNATO: Mostra il saldo corrente dell'account invece del bilancio calcolato
     */
    private fun showTransactions(state: MovimentiUiState.Success) {
        binding.tvPlaceholder.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE

        // Aggiorna statistiche
        binding.tvTotaleEntrate.text = CurrencyUtils.formatImporto(state.totalIncome)
        binding.tvTotaleUscite.text = CurrencyUtils.formatImporto(state.totalExpenses)

        // ✅ IMPORTANTE: Mostra il saldo corrente dell'account (include già il saldo iniziale)
        binding.tvBilancio.text = CurrencyUtils.formatImporto(state.currentBalance)

        // Colore del saldo corrente
        val bilancioColor = if (state.currentBalance >= 0) {
            android.graphics.Color.parseColor("#4CAF50") // Verde
        } else {
            android.graphics.Color.parseColor("#F44336") // Rosso
        }
        binding.tvBilancio.setTextColor(bilancioColor)

        // Aggiorna lista movimenti
        transactionAdapter.submitList(state.transactions)

        android.util.Log.d("MovimentiFragment", """
            ✅ Visualizzazione aggiornata:
               - ${state.transactions.size} movimenti
               - Entrate: ${state.totalIncome}€
               - Uscite: ${state.totalExpenses}€
               - Saldo Corrente: ${state.currentBalance}€
        """.trimIndent())
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

        // TODO: Mostra dettaglio movimento o dialog per modifica/elimina
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