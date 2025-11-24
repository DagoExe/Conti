package com.example.conti.ui.abbonamenti

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.conti.R
import com.example.conti.data.repository.AccountRepository
import com.example.conti.databinding.DialogAddSubscriptionBinding
import com.example.conti.models.Account
import com.example.conti.models.PaymentFrequency
import com.example.conti.models.Subscription
import com.example.conti.utils.CurrencyUtils
import com.example.conti.utils.MessageHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✨ DialogFragment per aggiungere un nuovo abbonamento - DARK MODE PREMIUM
 *
 * Features:
 * - Selezione conto da lista
 * - Frequenza di pagamento (Mensile, Trimestrale, Semestrale, Annuale)
 * - Selezione data primo pagamento
 * - Calcolo automatico data rinnovo
 * - Validazione campi
 */
class AddSubscriptionDialogFragment : DialogFragment() {

    private var _binding: DialogAddSubscriptionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AbbonamentiViewModel by viewModels({ requireParentFragment() })
    private val accountRepository = AccountRepository.getInstance()

    private var selectedDate: Calendar = Calendar.getInstance()
    private var accountsList: List<Account> = emptyList()
    private var selectedAccountId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadAccounts()
        setupFrequencyDropdown()
        setupDatePicker()
        setupButtons()
        updateDateField()
    }

    override fun onStart() {
        super.onStart()
        // Rendi il dialog full-width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Carica la lista dei conti
     */
    private fun loadAccounts() {
        viewLifecycleOwner.lifecycleScope.launch {
            accountRepository.getAccountsFlow().collect { accounts ->
                accountsList = accounts
                setupAccountsDropdown(accounts)
            }
        }
    }

    /**
     * Setup dropdown conti
     */
    private fun setupAccountsDropdown(accounts: List<Account>) {
        if (accounts.isEmpty()) {
            binding.tilConto.error = "Nessun conto disponibile. Creane uno prima."
            binding.btnSalva.isEnabled = false
            return
        }

        val accountsNames = accounts.map { "${it.accountName} (${it.accountType.displayName})" }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown_menu,
            accountsNames
        )

        binding.actvConto.setAdapter(adapter)

        // Pre-seleziona il primo conto
        binding.actvConto.setText(accountsNames.first(), false)
        selectedAccountId = accounts.first().accountId

        // Listener per salvare l'ID del conto selezionato
        binding.actvConto.setOnItemClickListener { _, _, position, _ ->
            selectedAccountId = accounts[position].accountId
        }
    }

    /**
     * Setup dropdown frequenza
     */
    private fun setupFrequencyDropdown() {
        val frequencies = listOf(
            "📅 Mensile",
            "📅 Trimestrale (ogni 3 mesi)",
            "📅 Semestrale (ogni 6 mesi)",
            "📅 Annuale"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown_menu,
            frequencies
        )

        binding.actvFrequenza.setAdapter(adapter)

        // Pre-seleziona "Mensile"
        binding.actvFrequenza.setText(frequencies[0], false)
    }

    /**
     * Setup DatePicker per data primo pagamento
     */
    private fun setupDatePicker() {
        binding.etDataPrimoPagamento.setOnClickListener {
            showDatePicker()
        }
    }

    /**
     * Mostra il DatePicker
     */
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateDateField()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    /**
     * Aggiorna il campo data con la data selezionata
     */
    private fun updateDateField() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
        binding.etDataPrimoPagamento.setText(dateFormat.format(selectedDate.time))
    }

    /**
     * Setup pulsanti
     */
    private fun setupButtons() {
        binding.btnAnnulla.setOnClickListener {
            dismiss()
        }

        binding.btnSalva.setOnClickListener {
            salvaAbbonamento()
        }
    }

    /**
     * Valida e salva l'abbonamento
     */
    private fun salvaAbbonamento() {
        // === VALIDAZIONE CAMPI ===

        // Nome
        val nome = binding.etNomeAbbonamento.text.toString().trim()
        if (nome.isBlank()) {
            binding.tilNomeAbbonamento.error = "Inserisci il nome dell'abbonamento"
            return
        }
        binding.tilNomeAbbonamento.error = null

        // Conto
        if (selectedAccountId == null) {
            binding.tilConto.error = "Seleziona un conto"
            return
        }
        binding.tilConto.error = null

        // Importo
        val importoString = binding.etImporto.text.toString().trim()
        val importo = CurrencyUtils.parseImporto(importoString)
        if (importo == null || importo <= 0) {
            binding.tilImporto.error = "Inserisci un importo valido"
            return
        }
        binding.tilImporto.error = null

        // Frequenza
        val frequenzaSelezionata = binding.actvFrequenza.text.toString()
        val frequency = when {
            frequenzaSelezionata.contains("Mensile") -> "MONTHLY"
            frequenzaSelezionata.contains("Trimestrale") -> "QUARTERLY"
            frequenzaSelezionata.contains("Semestrale") -> "SEMIANNUAL"
            frequenzaSelezionata.contains("Annuale") -> "ANNUAL"
            else -> "MONTHLY"
        }

        // Descrizione e note (opzionali)
        val descrizione = binding.etDescrizione.text.toString().trim().takeIf { it.isNotBlank() }
        val note = binding.etNote.text.toString().trim().takeIf { it.isNotBlank() }

        // === CALCOLA DATA RINNOVO ===

        val startDate = Timestamp(selectedDate.time)
        val nextRenewalDate = calculateNextRenewal(selectedDate, frequency)

        // === CREA SUBSCRIPTION ===

        val subscription = Subscription(
            accountId = selectedAccountId!!,
            name = nome,
            description = descrizione,
            amount = importo,
            frequency = frequency,
            category = "Abbonamento",
            startDate = startDate,
            nextRenewalDate = Timestamp(nextRenewalDate.time),
            endDate = null,
            isActive = true,
            notes = note
        )

        // === SALVA TRAMITE VIEWMODEL ===

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.createSubscription(subscription)

            MessageHelper.showSuccess(
                requireContext(),
                "✅ Abbonamento \"$nome\" creato"
            )

            dismiss()
        }
    }

    /**
     * Calcola la data del prossimo rinnovo in base alla frequenza
     */
    private fun calculateNextRenewal(startDate: Calendar, frequency: String): Calendar {
        val nextRenewal = startDate.clone() as Calendar

        when (frequency) {
            "MONTHLY" -> nextRenewal.add(Calendar.MONTH, 1)
            "QUARTERLY" -> nextRenewal.add(Calendar.MONTH, 3)
            "SEMIANNUAL" -> nextRenewal.add(Calendar.MONTH, 6)
            "ANNUAL" -> nextRenewal.add(Calendar.YEAR, 1)
        }

        return nextRenewal
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddSubscriptionDialog"

        fun newInstance(): AddSubscriptionDialogFragment {
            return AddSubscriptionDialogFragment()
        }
    }
}