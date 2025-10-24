package com.example.conti.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conti.databinding.FragmentHomeBinding
import com.example.conti.models.Account
import com.example.conti.utils.CurrencyUtils
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ✅ SEMPLICEMENTE COSÌ - Nessuna Factory necessaria
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var accountAdapter: HomeAccountAdapter

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
        observeViewModel()
    }

    private fun setupRecyclerView() {
        accountAdapter = HomeAccountAdapter { account ->
            onAccountClicked(account)
        }

        binding.rvConti.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = accountAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is HomeUiState.Loading -> {
                        showLoading(true)
                    }
                    is HomeUiState.Empty -> {
                        showLoading(false)
                        showEmptyState()
                    }
                    is HomeUiState.Success -> {
                        showLoading(false)
                        updateUI(state)
                    }
                    is HomeUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        // TODO: Implementa UI loading se necessario
    }

    private fun showEmptyState() {
        binding.layoutNoConti.visibility = View.VISIBLE
        binding.rvConti.visibility = View.GONE
    }

    private fun updateUI(state: HomeUiState.Success) {
        binding.layoutNoConti.visibility = View.GONE
        binding.rvConti.visibility = View.VISIBLE

        // Aggiorna saldo totale
        binding.tvSaldoTotale.text = CurrencyUtils.formatImporto(state.totalBalance)
        binding.tvNumeroConti.text = "${state.accounts.size} conti"

        // Aggiorna statistiche mensili
        binding.tvEntrateMese.text = CurrencyUtils.formatImporto(state.monthlyIncome)
        binding.tvUsciteMese.text = CurrencyUtils.formatImporto(Math.abs(state.monthlyExpenses))

        // Aggiorna abbonamenti
        binding.tvNumeroAbbonamenti.text = state.activeSubscriptions.toString()
        binding.tvCostoAbbonamenti.text = CurrencyUtils.formatImporto(state.subscriptionsCost)

        // Aggiorna lista conti
        accountAdapter.submitList(state.accounts)
    }

    private fun showError(message: String) {
        android.util.Log.e("HomeFragment", "Errore: $message")
        // TODO: Mostra Toast o Snackbar con errore
    }

    private fun onAccountClicked(account: Account) {
        android.util.Log.d("HomeFragment", "Click su account: ${account.accountName}")
        // TODO: Naviga al dettaglio account o mostra dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}