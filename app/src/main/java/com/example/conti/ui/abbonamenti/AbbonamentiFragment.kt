package com.example.conti.ui.abbonamenti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conti.databinding.FragmentAbbonamentiBinding
import com.example.conti.models.Subscription
import com.example.conti.ui.adapters.SubscriptionAdapter
import com.example.conti.utils.CurrencyUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * ✅ AbbonamentiFragment - VERSIONE COMPLETA CON MODIFICA
 *
 * Features:
 * - Visualizzazione lista abbonamenti
 * - Aggiunta nuovo abbonamento
 * - ✨ MODIFICA abbonamento esistente
 * - Disattivazione/Riattivazione
 * - Eliminazione
 * - Filtro attivi/tutti
 * - Statistiche real-time
 */
class AbbonamentiFragment : Fragment() {

    private var _binding: FragmentAbbonamentiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AbbonamentiViewModel by viewModels()
    private lateinit var subscriptionAdapter: SubscriptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        android.util.Log.d(TAG, "═══════════════════════════════════════")
        android.util.Log.d(TAG, "   ABBONAMENTI FRAGMENT - ONCREATEVIEW")
        android.util.Log.d(TAG, "═══════════════════════════════════════")

        _binding = FragmentAbbonamentiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        android.util.Log.d(TAG, "🔧 onViewCreated - Inizio setup")

        setupRecyclerView()
        setupFAB()
        setupFilterToggle()
        observeViewModel()

        android.util.Log.d(TAG, "✅ onViewCreated - Setup completato")
    }

    private fun setupRecyclerView() {
        android.util.Log.d(TAG, "📋 Setup RecyclerView...")

        subscriptionAdapter = SubscriptionAdapter(
            onSubscriptionClick = { subscription ->
                android.util.Log.d(TAG, "🖱️ Click su abbonamento: ${subscription.name}")
                showSubscriptionDetails(subscription)
            },
            onSubscriptionLongClick = { subscription ->
                android.util.Log.d(TAG, "🖱️ Long click su abbonamento: ${subscription.name}")
                showSubscriptionOptions(subscription)
            },
            accountsMap = emptyMap()
        )

        binding.rvAbbonamenti.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subscriptionAdapter
            visibility = View.VISIBLE
        }

        android.util.Log.d(TAG, "✅ RecyclerView configurata")
    }

    private fun setupFAB() {
        android.util.Log.d(TAG, "🎯 Setup FAB...")

        binding.fabAggiungiAbbonamento.setOnClickListener {
            android.util.Log.d(TAG, "🎯 FAB cliccato - Apro dialog aggiungi")
            showAddSubscriptionDialog()
        }

        android.util.Log.d(TAG, "✅ FAB configurato")
    }

    private fun setupFilterToggle() {
        android.util.Log.d(TAG, "🔍 Setup filtro...")

        binding.chipFiltroAttivi.setOnClickListener {
            android.util.Log.d(TAG, "🔍 Chip filtro cliccato")
            viewModel.toggleShowActiveOnly()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showActiveOnly.collect { showActiveOnly ->
                android.util.Log.d(TAG, "🔍 Filtro: showActiveOnly = $showActiveOnly")

                binding.chipFiltroAttivi.isChecked = showActiveOnly
                binding.chipFiltroAttivi.text = if (showActiveOnly) {
                    "✅ Solo Attivi"
                } else {
                    "📋 Tutti"
                }
            }
        }

        android.util.Log.d(TAG, "✅ Filtro configurato")
    }

    private fun observeViewModel() {
        android.util.Log.d(TAG, "👀 Inizio osservazione ViewModel...")

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                android.util.Log.d(TAG, "📊 STATO UI: ${state::class.simpleName}")

                when (state) {
                    is AbbonamentiUiState.Loading -> {
                        showLoading(true)
                    }
                    is AbbonamentiUiState.Empty -> {
                        showLoading(false)
                        showEmptyState(state.message)
                    }
                    is AbbonamentiUiState.Success -> {
                        android.util.Log.d(TAG, "✅ SUCCESS: ${state.subscriptions.size} abbonamenti")
                        showLoading(false)
                        showSubscriptions(state)
                    }
                    is AbbonamentiUiState.Error -> {
                        android.util.Log.e(TAG, "❌ ERROR: ${state.message}")
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }

        android.util.Log.d(TAG, "✅ Osservazione ViewModel attiva")
    }

    private fun showLoading(loading: Boolean) {
        if (loading) {
            binding.rvAbbonamenti.visibility = View.GONE
            binding.layoutEmpty.visibility = View.GONE
            binding.layoutStats.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        binding.rvAbbonamenti.visibility = View.GONE
        binding.layoutStats.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.fabAggiungiAbbonamento.visibility = View.VISIBLE

        binding.tvEmptyMessage.text = message
        binding.tvEmptyDescription.text = "Tocca + per aggiungere il tuo primo abbonamento"
    }

    private fun showSubscriptions(state: AbbonamentiUiState.Success) {
        binding.layoutEmpty.visibility = View.GONE
        binding.rvAbbonamenti.visibility = View.VISIBLE
        binding.layoutStats.visibility = View.VISIBLE
        binding.fabAggiungiAbbonamento.visibility = View.VISIBLE

        // Aggiorna statistiche
        binding.tvCostoMensile.text = CurrencyUtils.formatImporto(state.monthlyTotal)
        binding.tvCostoAnnuale.text = CurrencyUtils.formatImporto(state.annualTotal)
        binding.tvNumeroAbbonamenti.text = "${state.activeCount} attivi"

        // Aggiorna lista
        subscriptionAdapter.submitList(state.subscriptions)

        // Aggiorna accountsMap nell'adapter
        if (state.accounts.isNotEmpty()) {
            subscriptionAdapter.updateAccounts(state.accounts)
        }
    }

    private fun showError(message: String) {
        binding.rvAbbonamenti.visibility = View.GONE
        binding.layoutStats.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.fabAggiungiAbbonamento.visibility = View.VISIBLE

        binding.tvEmptyMessage.text = "Errore"
        binding.tvEmptyDescription.text = message
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DIALOG - Aggiungi Abbonamento
    // ═══════════════════════════════════════════════════════════════════════

    private fun showAddSubscriptionDialog() {
        android.util.Log.d(TAG, "🎯 Apertura dialog aggiungi abbonamento...")

        val dialog = AddSubscriptionDialogFragment.newInstance()
        dialog.show(childFragmentManager, AddSubscriptionDialogFragment.TAG)

        android.util.Log.d(TAG, "✅ Dialog mostrato")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ✨ DIALOG - Modifica Abbonamento
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Apre dialog per modificare abbonamento
     */
    private fun showEditSubscriptionDialog(subscription: Subscription) {
        android.util.Log.d(TAG, "✏️ Apertura dialog modifica abbonamento: ${subscription.name}")

        val dialog = EditSubscriptionDialogFragment.newInstance(subscription)
        dialog.show(childFragmentManager, EditSubscriptionDialogFragment.TAG)

        android.util.Log.d(TAG, "✅ Dialog modifica mostrato")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AZIONI - Abbonamento
    // ═══════════════════════════════════════════════════════════════════════

    private fun showSubscriptionDetails(subscription: Subscription) {
        val frequencyText = when (subscription.frequency) {
            "MONTHLY" -> "Mensile"
            "QUARTERLY" -> "Trimestrale"
            "SEMIANNUAL" -> "Semestrale"
            "ANNUAL" -> "Annuale"
            else -> "Mensile"
        }

        val message = """
            📅 Frequenza: $frequencyText
            💰 Costo: ${CurrencyUtils.formatImporto(subscription.amount)}
            💳 Costo Mensile: ${CurrencyUtils.formatImporto(subscription.getMonthlyCost())}
            📆 Prossimo Rinnovo: ${formatDate(subscription.nextRenewalDate.toDate())}
            ${if (!subscription.description.isNullOrBlank()) "\n📝 ${subscription.description}" else ""}
            ${if (!subscription.notes.isNullOrBlank()) "\n📌 ${subscription.notes}" else ""}
            ${if (!subscription.isActive) "\n⚠️ INATTIVO" else ""}
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(subscription.name)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Modifica") { _, _ ->
                showEditSubscriptionDialog(subscription)
            }
            .show()
    }

    private fun showSubscriptionOptions(subscription: Subscription) {
        android.util.Log.d(TAG, "📋 showSubscriptionOptions: ${subscription.name}")

        val options = if (subscription.isActive) {
            arrayOf(
                "✏️ Modifica",
                "⏸️ Disattiva",
                "🗑️ Elimina"
            )
        } else {
            arrayOf(
                "✏️ Modifica",
                "▶️ Riattiva",
                "🗑️ Elimina"
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(subscription.name)
            .setItems(options) { _, which ->
                android.util.Log.d(TAG, "   Opzione selezionata: $which")
                when (which) {
                    0 -> showEditSubscriptionDialog(subscription)  // ✨ MODIFICA
                    1 -> toggleSubscriptionStatus(subscription)
                    2 -> confirmDeleteSubscription(subscription)
                }
            }
            .show()
    }

    private fun toggleSubscriptionStatus(subscription: Subscription) {
        android.util.Log.d(TAG, "⏯️ toggleSubscriptionStatus: ${subscription.name}")

        if (subscription.isActive) {
            // Conferma disattivazione
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("⏸️ Disattiva Abbonamento")
                .setMessage("Sei sicuro di voler disattivare \"${subscription.name}\"?\n\nL'abbonamento non verrà eliminato ma non apparirà più nei calcoli.")
                .setPositiveButton("Disattiva") { _, _ ->
                    android.util.Log.d(TAG, "   Confermata disattivazione")
                    viewModel.deactivateSubscription(subscription.id)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "⏸️ ${subscription.name} disattivato",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Annulla", null)
                .show()
        } else {
            // Riattiva direttamente
            android.util.Log.d(TAG, "   Riattivazione abbonamento")
            viewModel.reactivateSubscription(subscription.id)
            android.widget.Toast.makeText(
                requireContext(),
                "▶️ ${subscription.name} riattivato",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun confirmDeleteSubscription(subscription: Subscription) {
        android.util.Log.d(TAG, "🗑️ confirmDeleteSubscription: ${subscription.name}")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🗑️ Elimina Abbonamento")
            .setMessage("Sei sicuro di voler eliminare definitivamente \"${subscription.name}\"?\n\n⚠️ Questa azione non può essere annullata.")
            .setPositiveButton("Elimina") { _, _ ->
                android.util.Log.d(TAG, "   Confermata eliminazione")
                viewModel.deleteSubscription(subscription.id)
                android.widget.Toast.makeText(
                    requireContext(),
                    "🗑️ ${subscription.name} eliminato",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Annulla") { _, _ ->
                android.util.Log.d(TAG, "   Eliminazione annullata")
            }
            .show()
    }

    private fun formatDate(date: java.util.Date): String {
        val sdf = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.ITALIAN)
        return sdf.format(date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        android.util.Log.d(TAG, "🔚 onDestroyView")
        _binding = null
    }

    companion object {
        private const val TAG = "AbbonamentiFragment"
    }
}