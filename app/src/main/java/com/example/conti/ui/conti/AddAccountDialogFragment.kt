package com.example.conti.ui.conti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.conti.R
import com.example.conti.databinding.DialogAggiungiContoBinding
import com.example.conti.models.Account
import com.example.conti.models.AccountType
import com.example.conti.ui.account.AccountsViewModel
import com.example.conti.utils.CurrencyUtils
import com.example.conti.utils.MessageHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

/**
 * ✨ DialogFragment per aggiungere un nuovo conto - DARK MODE PREMIUM
 *
 * AGGIORNATO con:
 * - Tema dark premium MONIO
 * - Dropdown con layout personalizzato
 * - Toast posizionati in basso
 * - Validazione IBAN migliorata
 * - Helper text
 */
class AddAccountDialogFragment : DialogFragment() {

    private var _binding: DialogAggiungiContoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountsViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAggiungiContoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdown()
        setupButtons()
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
     * ✅ Setup del dropdown con layout personalizzato per dark theme
     */
    private fun setupDropdown() {
        val tipiConto = listOf(
            "🏦 Conto Corrente BuddyBank",
            "💳 Hype Card",
            "🏛️ Altro Conto"
        )

        // ✅ Usa layout personalizzato per dropdown dark theme
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown_menu,  // Layout personalizzato dark
            tipiConto
        )

        binding.actvTipoConto.setAdapter(adapter)

        // Pre-seleziona il primo elemento
        binding.actvTipoConto.setText(tipiConto[0], false)
    }

    /**
     * Setup dei pulsanti
     */
    private fun setupButtons() {
        binding.btnAnnulla.setOnClickListener {
            dismiss()
        }

        binding.btnSalva.setOnClickListener {
            salvaAccount()
        }
    }

    /**
     * ✅ Valida e salva il nuovo account con toast in basso
     */
    private fun salvaAccount() {
        // === VALIDAZIONE CAMPI ===

        // Nome Conto
        val nomeConto = binding.etNomeConto.text.toString().trim()
        if (nomeConto.isBlank()) {
            binding.tilNomeConto.error = "Inserisci il nome del conto"
            return
        }
        binding.tilNomeConto.error = null

        // Tipo Conto
        val tipoContoSelezionato = binding.actvTipoConto.text.toString()
        val accountType = when {
            tipoContoSelezionato.contains("BuddyBank") -> AccountType.BUDDYBANK
            tipoContoSelezionato.contains("Hype") -> AccountType.HYPE
            else -> AccountType.OTHER
        }

        // Saldo Iniziale
        val saldoString = binding.etSaldoIniziale.text.toString().trim()
        val saldo = CurrencyUtils.parseImporto(saldoString)
        if (saldo == null) {
            binding.tilSaldoIniziale.error = "Inserisci un importo valido"
            return
        }
        binding.tilSaldoIniziale.error = null

        // IBAN (opzionale)
        val iban = binding.etIban.text.toString().trim().uppercase()
            .takeIf { it.isNotBlank() }

        // Valida IBAN se presente
        if (iban != null && !isIbanValid(iban)) {
            binding.tilIban.error = "IBAN non valido"
            return
        }
        binding.tilIban.error = null

        // === CREA ACCOUNT ===

        val accountId = generateAccountId(nomeConto)

        val account = Account(
            accountId = accountId,
            accountName = nomeConto,
            accountType = accountType,
            balance = saldo,
            currency = "EUR",
            iban = iban,
            lastUpdated = Timestamp.now()
        )

        // === SALVA TRAMITE VIEWMODEL ===

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveAccount(account)

            // ✅ Toast SUCCESS in basso
            MessageHelper.showSuccess(
                requireContext(),
                "✅ Conto \"$nomeConto\" creato con successo"
            )

            dismiss()
        }
    }

    /**
     * Genera un ID univoco per il conto basato sul nome
     */
    private fun generateAccountId(nomeConto: String): String {
        val timestamp = System.currentTimeMillis()
        val cleanName = nomeConto
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .take(20)

        return "${cleanName}_$timestamp"
    }

    /**
     * ✅ Validazione IBAN completa con checksum MOD-97
     */
    private fun isIbanValid(iban: String): Boolean {
        val cleanIban = iban.replace(" ", "").uppercase()

        // Verifica lunghezza
        if (cleanIban.length < 15 || cleanIban.length > 34) return false

        // Verifica formato base
        if (!cleanIban.matches(Regex("^[A-Z]{2}\\d{2}[A-Z0-9]+$"))) return false

        // Validazione checksum (algoritmo MOD-97)
        try {
            val rearranged = cleanIban.substring(4) + cleanIban.substring(0, 4)
            val numericIban = rearranged.map { char ->
                if (char.isDigit()) char.toString()
                else (char.code - 'A'.code + 10).toString()
            }.joinToString("")

            var remainder = 0
            for (char in numericIban) {
                remainder = (remainder * 10 + char.digitToInt()) % 97
            }

            return remainder == 1
        } catch (e: Exception) {
            return false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddAccountDialog"

        fun newInstance() = AddAccountDialogFragment()
    }
}