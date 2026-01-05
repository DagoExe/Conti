package com.example.conti.ui.abbonamenti

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.BuildCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.conti.R
import com.example.conti.data.repository.AccountRepository
import com.example.conti.databinding.DialogEditSubscriptionBinding
import com.example.conti.models.PaymentFrequency
import com.example.conti.models.Subscription
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog per modificare un abbonamento esistente.
 *
 * ✅ Features:
 * - Campi pre-popolati con dati abbonamento
 * - Validazione completa
 * - Dropdown frequenza e conti
 * - DatePicker per selezione date
 * - Calcolo automatico nextRenewalDate
 * - Aggiornamento Firestore
 *
 * Usage:
 * ```kotlin
 * val dialog = EditSubscriptionDialogFragment.newInstance(subscription)
 * dialog.show(childFragmentManager, TAG)
 * ```
 */
class EditSubscriptionDialogFragment : DialogFragment() {

    private var _binding: DialogEditSubscriptionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AbbonamentiViewModel by viewModels({ requireParentFragment() })
    private val accountRepository = AccountRepository.getInstance()

    private lateinit var subscription: Subscription
    private var selectedDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Conti_Dialog_FullScreen)

        // Recupera abbonamento da arguments
        subscription = if (BuildCompat.isAtLeastT()) {
            arguments?.getParcelable(ARG_SUBSCRIPTION, Subscription::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_SUBSCRIPTION)
        } ?: throw IllegalArgumentException("Subscription is required")


        // Inizializza data selezionata con nextRenewalDate
        selectedDate.time = subscription.nextRenewalDate.toDate()

        android.util.Log.d(TAG, "📝 Dialog modifica aperto per: ${subscription.name}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupFrequencyDropdown()
        loadAccountsDropdown()
        populateFields()
        setupDatePicker()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "Modifica Abbonamento"
            setNavigationIcon(R.drawable.ic_close)
            setNavigationOnClickListener { dismiss() }
        }
    }

    /**
     * Setup dropdown frequenza pagamento
     */
    private fun setupFrequencyDropdown() {
        val frequencies = PaymentFrequency.entries.map { it.displayName }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            frequencies
        )
        binding.etFrequenza.setAdapter(adapter)

        android.util.Log.d(TAG, "✅ Dropdown frequenza configurato con ${frequencies.size} opzioni")
    }

    /**
     * Carica conti disponibili nel dropdown
     */
    private fun loadAccountsDropdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            val accounts = accountRepository.getAccountsFlow().first()
            val accountNames = accounts.map {
                "${it.accountName} (${it.accountType})"
            }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                accountNames
            )
            binding.etConto.setAdapter(adapter)

            android.util.Log.d(TAG, "✅ Caricati ${accounts.size} conti nel dropdown")
        }
    }

    /**
     * Pre-popola i campi con i dati dell'abbonamento
     */
    private fun populateFields() {
        android.util.Log.d(TAG, "📋 Popolamento campi con dati abbonamento...")

        with(binding) {
            // Nome e descrizione
            etNome.setText(subscription.name)
            etDescrizione.setText(subscription.description ?: "")

            // Importo
            etImporto.setText(subscription.amount.toString())

            // Frequenza
            val frequencyDisplay = PaymentFrequency.fromString(subscription.frequency).displayName
            etFrequenza.setText(frequencyDisplay, false)
            android.util.Log.d(TAG, "   Frequenza: ${subscription.frequency} → $frequencyDisplay")

            // Conto
            viewLifecycleOwner.lifecycleScope.launch {
                val accounts = accountRepository.getAccountsFlow().first()
                val account = accounts.find { it.accountId == subscription.accountId }
                if (account != null) {
                    val accountDisplay = "${account.accountName} (${account.accountType})"
                    etConto.setText(accountDisplay, false)
                    android.util.Log.d(TAG, "   Conto: ${subscription.accountId} → $accountDisplay")
                } else {
                    android.util.Log.w(TAG, "   ⚠️ Conto non trovato: ${subscription.accountId}")
                }
            }

            // Data prossimo rinnovo
            etData.setText(dateFormat.format(subscription.nextRenewalDate.toDate()))
            android.util.Log.d(TAG, "   Data rinnovo: ${dateFormat.format(subscription.nextRenewalDate.toDate())}")

            // Note
            etNote.setText(subscription.notes ?: "")

            // Stato attivo
            switchAttivo.isChecked = subscription.isActive
            android.util.Log.d(TAG, "   Attivo: ${subscription.isActive}")
        }

        android.util.Log.d(TAG, "✅ Campi popolati con successo")
    }

    /**
     * Setup DatePicker per selezione data
     */
    private fun setupDatePicker() {
        binding.etData.setOnClickListener {
            android.util.Log.d(TAG, "📅 Apertura DatePicker...")

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    binding.etData.setText(dateFormat.format(selectedDate.time))
                    android.util.Log.d(TAG, "   Data selezionata: ${dateFormat.format(selectedDate.time)}")
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Setup pulsanti Annulla e Salva
     */
    private fun setupButtons() {
        binding.btnAnnulla.setOnClickListener {
            android.util.Log.d(TAG, "❌ Modifica annullata")
            dismiss()
        }

        binding.btnSalva.setOnClickListener {
            android.util.Log.d(TAG, "💾 Tentativo salvataggio modifiche...")
            saveChanges()
        }
    }

    /**
     * Valida e salva le modifiche
     */
    private fun saveChanges() {
        android.util.Log.d(TAG, "═══════════════════════════════════════")
        android.util.Log.d(TAG, "  VALIDAZIONE E SALVATAGGIO")
        android.util.Log.d(TAG, "═══════════════════════════════════════")

        // Validazione campi
        val nome = binding.etNome.text.toString().trim()
        val importoStr = binding.etImporto.text.toString().trim()
        val frequenzaDisplay = binding.etFrequenza.text.toString()
        val contoDisplay = binding.etConto.text.toString()

        android.util.Log.d(TAG, "📋 Dati inseriti:")
        android.util.Log.d(TAG, "   Nome: $nome")
        android.util.Log.d(TAG, "   Importo: $importoStr")
        android.util.Log.d(TAG, "   Frequenza: $frequenzaDisplay")
        android.util.Log.d(TAG, "   Conto: $contoDisplay")

        // Validazione nome
        if (nome.isEmpty()) {
            binding.tilNome.error = "Il nome è obbligatorio"
            android.util.Log.w(TAG, "❌ Validazione fallita: Nome vuoto")
            return
        }
        binding.tilNome.error = null

        // Validazione importo
        val importo = importoStr.toDoubleOrNull()
        if (importo == null || importo <= 0) {
            binding.tilImporto.error = "Inserisci un importo valido"
            android.util.Log.w(TAG, "❌ Validazione fallita: Importo non valido ($importoStr)")
            return
        }
        binding.tilImporto.error = null

        // Validazione frequenza
        val frequency = PaymentFrequency.entries.find {
            it.displayName == frequenzaDisplay
        }?.name ?: run {
            binding.tilFrequenza.error = "Seleziona una frequenza"
            android.util.Log.w(TAG, "❌ Validazione fallita: Frequenza non valida ($frequenzaDisplay)")
            return
        }
        binding.tilFrequenza.error = null

        // Validazione conto
        if (contoDisplay.isEmpty()) {
            binding.tilConto.error = "Seleziona un conto"
            android.util.Log.w(TAG, "❌ Validazione fallita: Conto non selezionato")
            return
        }
        binding.tilConto.error = null

        // Trova accountId dal display name
        viewLifecycleOwner.lifecycleScope.launch {
            val accounts = accountRepository.getAccountsFlow().first()
            val selectedAccount = accounts.find {
                "${it.accountName} (${it.accountType})" == contoDisplay
            }

            if (selectedAccount == null) {
                binding.tilConto.error = "Conto non valido"
                android.util.Log.w(TAG, "❌ Conto non trovato: $contoDisplay")
                return@launch
            }

            android.util.Log.d(TAG, "✅ Validazione completata con successo")
            android.util.Log.d(TAG, "   Account ID: ${selectedAccount.accountId}")

            // Crea abbonamento aggiornato
            val updatedSubscription = subscription.copy(
                name = nome,
                description = binding.etDescrizione.text.toString().trim().ifEmpty { null },
                amount = importo,
                frequency = frequency,
                accountId = selectedAccount.accountId,
                nextRenewalDate = Timestamp(selectedDate.time),
                notes = binding.etNote.text.toString().trim().ifEmpty { null },
                isActive = binding.switchAttivo.isChecked,
                lastUpdated = Timestamp.now()
            )

            android.util.Log.d(TAG, "📦 Abbonamento aggiornato creato:")
            android.util.Log.d(TAG, "   ID: ${updatedSubscription.id}")
            android.util.Log.d(TAG, "   Nome: ${updatedSubscription.name}")
            android.util.Log.d(TAG, "   Importo: €${updatedSubscription.amount}")
            android.util.Log.d(TAG, "   Frequenza: ${updatedSubscription.frequency}")
            android.util.Log.d(TAG, "   Attivo: ${updatedSubscription.isActive}")

            // Salva su Firestore tramite ViewModel
            viewModel.updateSubscription(updatedSubscription)

            android.util.Log.d(TAG, "✅ Richiesta aggiornamento inviata al ViewModel")
            android.util.Log.d(TAG, "═══════════════════════════════════════")

            // Mostra feedback e chiudi dialog
            android.widget.Toast.makeText(
                requireContext(),
                "✅ Abbonamento aggiornato",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditSubscriptionDialog"
        private const val ARG_SUBSCRIPTION = "subscription"

        /**
         * Crea nuova istanza del dialog con abbonamento da modificare
         */
        fun newInstance(subscription: Subscription): EditSubscriptionDialogFragment {
            return EditSubscriptionDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SUBSCRIPTION, subscription)
                }
            }
        }
    }
}
