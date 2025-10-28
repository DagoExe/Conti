package com.example.conti.ui.conti

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
import com.example.conti.databinding.FragmentContiBinding
import com.example.conti.models.Account
import com.example.conti.ui.account.AccountsUiState
import com.example.conti.ui.account.AccountsViewModel
import com.example.conti.ui.adapters.ContiVerticalAdapter
import kotlinx.coroutines.launch

/**
 * Fragment per la sezione Conti.
 *
 * ✅ Visualizza i conti come card verticali
 * ✅ Click su card → naviga ai movimenti di quel conto
 */
class ContiFragment : Fragment() {

    private var _binding: FragmentContiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountsViewModel by viewModels()
    private lateinit var contiAdapter: ContiVerticalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupUI()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        contiAdapter = ContiVerticalAdapter { account ->
            onAccountClicked(account)
        }

        binding.rvConti.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contiAdapter
        }
    }

    private fun setupUI() {
        // Nascondi tab layout per ora
        binding.tabLayout.visibility = View.GONE

        // FAB per aggiungere conto
        binding.fabAggiungi.setOnClickListener {
            showAddAccountDialog()
        }
    }

    /**
     * Mostra il dialog per aggiungere un nuovo conto
     */
    private fun showAddAccountDialog() {
        val dialog = AddAccountDialogFragment.newInstance()
        dialog.show(childFragmentManager, AddAccountDialogFragment.TAG)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AccountsUiState.Loading -> {
                        showLoading(true)
                    }
                    is AccountsUiState.Empty -> {
                        showLoading(false)
                        showEmptyState()
                    }
                    is AccountsUiState.Success -> {
                        showLoading(false)
                        showAccounts(state.accounts)
                    }
                    is AccountsUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        if (loading) {
            binding.rvConti.visibility = View.GONE
            binding.layoutEmpty.visibility = View.GONE
            // TODO: Mostra loading spinner
        }
    }

    private fun showEmptyState() {
        binding.rvConti.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.fabAggiungi.visibility = View.VISIBLE

        binding.tvEmptyMessage.text = "Nessun conto presente"
        binding.tvEmptyDescription.text = "Tocca + per aggiungere il tuo primo conto"
    }

    private fun showAccounts(accounts: List<Account>) {
        binding.rvConti.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
        binding.fabAggiungi.visibility = View.VISIBLE

        contiAdapter.submitList(accounts)

        android.util.Log.d("ContiFragment", "✅ Visualizzati ${accounts.size} conti")
    }

    private fun showError(message: String) {
        binding.rvConti.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.fabAggiungi.visibility = View.VISIBLE

        binding.tvEmptyMessage.text = "Errore"
        binding.tvEmptyDescription.text = message

        android.util.Log.e("ContiFragment", "❌ Errore: $message")
    }

    /**
     * Gestisce il click su un conto.
     * Naviga alla schermata movimenti filtrando per quel conto.
     */
    private fun onAccountClicked(account: Account) {
        android.util.Log.d("ContiFragment", "🖱️ Click su conto: ${account.accountName} (${account.accountId})")

        // Naviga a MovimentiFragment passando accountId e accountName
        val bundle = bundleOf(
            "accountId" to account.accountId,
            "accountName" to account.accountName
        )

        findNavController().navigate(R.id.action_conti_to_movimenti, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}