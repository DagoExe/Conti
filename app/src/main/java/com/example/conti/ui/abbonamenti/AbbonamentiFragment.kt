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
 * ✅ AbbonamentiFragment - VERSIONE DEBUG
 *
 * Questa versione ha logging esteso per identificare problemi di visualizzazione
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
                onSubscriptionClicked(subscription)
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

            // ✅ IMPORTANTE: Verifica che RecyclerView sia visibile
            visibility = View.VISIBLE
        }

        android.util.Log.d(TAG, "✅ RecyclerView configurata")
        android.util.Log.d(TAG, "   - LayoutManager: ${binding.rvAbbonamenti.layoutManager}")
        android.util.Log.d(TAG, "   - Adapter: ${binding.rvAbbonamenti.adapter}")
    }

    private fun setupFAB() {
        android.util.Log.d(TAG, "🎯 Setup FAB...")

        binding.fabAggiungiAbbonamento.setOnClickListener {
            android.util.Log.d(TAG, "🎯 FAB cliccato - Apro dialog")
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

        // Osserva lo stato del filtro
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showActiveOnly.collect { showActiveOnly ->
                android.util.Log.d(TAG, "🔍 Filtro cambiato: showActiveOnly = $showActiveOnly")

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
                android.util.Log.d(TAG, "═══════════════════════════════════════")
                android.util.Log.d(TAG, "📊 NUOVO STATO UI: ${state::class.simpleName}")
                android.util.Log.d(TAG, "═══════════════════════════════════════")

                when (state) {
                    is AbbonamentiUiState.Loading -> {
                        android.util.Log.d(TAG, "⏳ Stato: LOADING")
                        showLoading(true)
                    }
                    is AbbonamentiUiState.Empty -> {
                        android.util.Log.d(TAG, "📭 Stato: EMPTY")
                        android.util.Log.d(TAG, "   Messaggio: ${state.message}")
                        showLoading(false)
                        showEmptyState(state.message)
                    }
                    is AbbonamentiUiState.Success -> {
                        android.util.Log.d(TAG, "✅ Stato: SUCCESS")
                        android.util.Log.d(TAG, "   Abbonamenti: ${state.subscriptions.size}")
                        android.util.Log.d(TAG, "   Costo Mensile: €${state.monthlyTotal}")
                        android.util.Log.d(TAG, "   Costo Annuale: €${state.annualTotal}")
                        android.util.Log.d(TAG, "   Attivi: ${state.activeCount}")

                        // Log dettaglio abbonamenti
                        state.subscriptions.forEachIndexed { index, sub ->
                            android.util.Log.d(TAG, "   [$index] ${sub.name} - €${sub.amount} - ${if (sub.isActive) "ATTIVO" else "INATTIVO"}")
                        }

                        showLoading(false)
                        showSubscriptions(state)
                    }
                    is AbbonamentiUiState.Error -> {
                        android.util.Log.e(TAG, "❌ Stato: ERROR")
                        android.util.Log.e(TAG, "   Messaggio: ${state.message}")
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }

        android.util.Log.d(TAG, "✅ Osservazione ViewModel attiva")
    }

    private fun showLoading(loading: Boolean) {
        android.util.Log.d(TAG, "⏳ showLoading($loading)")

        if (loading) {
            binding.rvAbbonamenti.visibility = View.GONE
            binding.layoutEmpty.visibility = View.GONE
            binding.layoutStats.visibility = View.GONE
            android.util.Log.d(TAG, "   - RecyclerView: GONE")
            android.util.Log.d(TAG, "   - Empty: GONE")
            android.util.Log.d(TAG, "   - Stats: GONE")
        }
    }

    private fun showEmptyState(message: String) {
        android.util.Log.d(TAG, "📭 showEmptyState(\"$message\")")

        binding.rvAbbonamenti.visibility = View.GONE
        binding.layoutStats.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.fabAggiungiAbbonamento.visibility = View.VISIBLE

        binding.tvEmptyMessage.text = message
        binding.tvEmptyDescription.text = "Tocca + per aggiungere il tuo primo abbonamento"

        android.util.Log.d(TAG, "   - RecyclerView: GONE")
        android.util.Log.d(TAG, "   - Stats: GONE")
        android.util.Log.d(TAG, "   - Empty: VISIBLE")
        android.util.Log.d(TAG, "   - FAB: VISIBLE")
    }

    private fun showSubscriptions(state: AbbonamentiUiState.Success) {
        android.util.Log.d(TAG, "✅ showSubscriptions()")
        android.util.Log.d(TAG, "   Abbonamenti da mostrare: ${state.subscriptions.size}")

        // ✅ IMPORTANTE: Nascondi empty, mostra lista
        binding.layoutEmpty.visibility = View.GONE
        binding.rvAbbonamenti.visibility = View.VISIBLE
        binding.layoutStats.visibility = View.VISIBLE
        binding.fabAggiungiAbbonamento.visibility = View.VISIBLE

        android.util.Log.d(TAG, "   - Empty: GONE")
        android.util.Log.d(TAG, "   - RecyclerView: VISIBLE")
        android.util.Log.d(TAG, "   - Stats: VISIBLE")
        android.util.Log.d(TAG, "   - FAB: VISIBLE")

        // Aggiorna statistiche
        binding.tvCostoMensile.text = CurrencyUtils.formatImporto(state.monthlyTotal)
        binding.tvCostoAnnuale.text = CurrencyUtils.formatImporto(state.annualTotal)
        binding.tvNumeroAbbonamenti.text = "${state.activeCount} attivi"

        android.util.Log.d(TAG, "   Statistiche aggiornate:")
        android.util.Log.d(TAG, "   - Mensile: ${binding.tvCostoMensile.text}")
        android.util.Log.d(TAG, "   - Annuale: ${binding.tvCostoAnnuale.text}")
        android.util.Log.d(TAG, "   - Numero: ${binding.tvNumeroAbbonamenti.text}")

        // ✅ CRITICO: Aggiorna lista nell'adapter
        android.util.Log.d(TAG, "   Invio lista a adapter...")
        subscriptionAdapter.submitList(state.subscriptions)

        // ✅ VERIFICA: Controlla che l'adapter abbia ricevuto i dati
        binding.rvAbbonamenti.post {
            val itemCount = subscriptionAdapter.itemCount
            android.util.Log.d(TAG, "   ✅ Adapter itemCount: $itemCount")

            if (itemCount == 0) {
                android.util.Log.e(TAG, "   ❌ PROBLEMA: Adapter ha 0 items ma dovrebbe averne ${state.subscriptions.size}!")
            } else {
                android.util.Log.d(TAG, "   ✅ Adapter popolato correttamente")
            }
        }
    }

    private fun showError(message: String) {
        android.util.Log.e(TAG, "❌ showError(\"$message\")")

        binding.rvAbbonamenti.visibility = View.GONE
        binding.layoutStats.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.fabAggiungiAbbonamento.visibility = View.VISIBLE

        binding.tvEmptyMessage.text = "Errore"
        binding.tvEmptyDescription.text = message
    }

    private fun showAddSubscriptionDialog() {
        android.util.Log.d(TAG, "🎯 Apertura dialog aggiungi abbonamento...")

        val dialog = AddSubscriptionDialogFragment.newInstance()
        dialog.show(childFragmentManager, AddSubscriptionDialogFragment.TAG)

        android.util.Log.d(TAG, "✅ Dialog mostrato")
    }

    private fun onSubscriptionClicked(subscription: Subscription) {
        android.util.Log.d(TAG, "🖱️ onSubscriptionClicked: ${subscription.name}")
        showSubscriptionDetails(subscription)
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
                    0 -> editSubscription(subscription)
                    1 -> toggleSubscriptionStatus(subscription)
                    2 -> deleteSubscription(subscription)
                }
            }
            .show()
    }

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
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(subscription.name)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Modifica") { _, _ ->
                editSubscription(subscription)
            }
            .show()
    }

    private fun editSubscription(subscription: Subscription) {
        android.util.Log.d(TAG, "✏️ editSubscription: ${subscription.name}")
        android.widget.Toast.makeText(
            requireContext(),
            "⚠️ Funzione modifica in arrivo",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun toggleSubscriptionStatus(subscription: Subscription) {
        android.util.Log.d(TAG, "⏯️ toggleSubscriptionStatus: ${subscription.name}")

        if (subscription.isActive) {
            viewModel.deactivateSubscription(subscription.id)
        } else {
            android.util.Log.d(TAG, "⚠️ Riattivazione non ancora implementata")
        }
    }

    private fun deleteSubscription(subscription: Subscription) {
        android.util.Log.d(TAG, "🗑️ deleteSubscription: ${subscription.name}")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🗑️ Elimina Abbonamento")
            .setMessage("Sei sicuro di voler eliminare \"${subscription.name}\"?")
            .setPositiveButton("Elimina") { _, _ ->
                android.util.Log.d(TAG, "   Confermata eliminazione")
                viewModel.deleteSubscription(subscription.id)
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
        private const val TAG = "AbbonamentiFrag_DEBUG"
    }
}