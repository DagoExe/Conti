package com.example.conti.ui.movimenti

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.databinding.DialogAggiungiMovimentoBinding
import com.example.conti.models.Transaction
import com.example.conti.utils.Constants
import com.example.conti.utils.CurrencyUtils
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * DialogFragment per aggiungere un nuovo movimento.
 *
 * UTILIZZO:
 * ```
 * val dialog = AddTransactionDialogFragment.newInstance(accountId, accountName)
 * dialog.show(childFragmentManager, TAG)
 * ```
 */
class AddTransactionDialogFragment : DialogFragment() {

    private var _binding: DialogAggiungiMovimentoBinding? = null
    private val binding get() = _binding!!

    private val firestoreRepository = FirestoreRepository()

    // Argomenti passati al dialog
    private lateinit var accountId: String
    private lateinit var accountName: String

    // Data selezionata (default: oggi)
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recupera argomenti
        arguments?.let {
            accountId = it.getString(ARG_ACCOUNT_ID) ?: throw IllegalArgumentException("accountId mancante")
            accountName = it.getString(ARG_ACCOUNT_NAME) ?: "Conto"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAggiungiMovimentoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupCategoryDropdown()
        setupDatePicker()
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
     * Setup UI iniziale
     */
    private fun setupUI() {
        // Mostra nome del conto
        binding.tvNomeConto.text = "Conto: $accountName"

        // Imposta data corrente
        updateDateField()
    }

    /**
     * Setup dropdown categorie
     */
    private fun setupCategoryDropdown() {
        val categorie = Constants.CATEGORIE_DEFAULT.map { categoria ->
            getCategoryIcon(categoria) + " " + categoria
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categorie
        )

        binding.actvCategoria.setAdapter(adapter)

        // Pre-seleziona "Altro" come default
        binding.actvCategoria.setText(categorie.last(), false)
    }

    /**
     * Setup DatePicker
     */
    private fun setupDatePicker() {
        binding.etData.setOnClickListener {
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
        binding.etData.setText(dateFormat.format(selectedDate.time))
    }

    /**
     * Setup pulsanti
     */
    private fun setupButtons() {
        binding.btnAnnulla.setOnClickListener {
            dismiss()
        }

        binding.btnSalva.setOnClickListener {
            salvaMovimento()
        }
    }

    /**
     * Valida e salva il movimento
     */
    private fun salvaMovimento() {
        // === VALIDAZIONE CAMPI ===

        // Descrizione
        val descrizione = binding.etDescrizione.text.toString().trim()
        if (descrizione.isBlank()) {
            binding.tilDescrizione.error = "Inserisci una descrizione"
            return
        }
        binding.tilDescrizione.error = null

        // Importo
        val importoString = binding.etImporto.text.toString().trim()
        val importo = CurrencyUtils.parseImporto(importoString)
        if (importo == null || importo <= 0) {
            binding.tilImporto.error = "Inserisci un importo valido"
            return
        }
        binding.tilImporto.error = null

        // Categoria
        val categoriaConIcona = binding.actvCategoria.text.toString().trim()
        if (categoriaConIcona.isBlank()) {
            binding.tilCategoria.error = "Seleziona una categoria"
            return
        }
        binding.tilCategoria.error = null

        // Rimuovi l'icona dalla categoria
        val categoria = categoriaConIcona.substringAfter(" ").trim()

        // Note (opzionali)
        val note = binding.etNote.text.toString().trim().takeIf { it.isNotBlank() }

        // Tipo (Entrata/Uscita)
        val isEntrata = binding.chipEntrata.isChecked
        val importoFinale = if (isEntrata) importo else -importo
        val tipo = if (isEntrata) "income" else "expense"

        // === CREA TRANSAZIONE ===

        val transaction = Transaction(
            accountId = accountId,
            amount = importoFinale,
            description = descrizione,
            category = categoria,
            notes = note,
            date = Timestamp(selectedDate.time),
            type = tipo,
            isRecurring = false
        )

        // === SALVA SU FIRESTORE ===

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                firestoreRepository.addTransaction(transaction)
                    .onSuccess {
                        android.util.Log.d(TAG, "✅ Movimento salvato con successo")

                        Toast.makeText(
                            requireContext(),
                            "✅ Movimento aggiunto",
                            Toast.LENGTH_SHORT
                        ).show()

                        dismiss()
                    }
                    .onFailure { e ->
                        android.util.Log.e(TAG, "❌ Errore salvataggio movimento", e)

                        Toast.makeText(
                            requireContext(),
                            "❌ Errore: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Eccezione durante salvataggio", e)

                Toast.makeText(
                    requireContext(),
                    "❌ Errore: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Restituisce un'icona per la categoria
     */
    private fun getCategoryIcon(category: String): String {
        return when (category.lowercase()) {
            "stipendio" -> "💰"
            "abbonamento" -> "📅"
            "spesa" -> "🛒"
            "ristorante" -> "🍽️"
            "trasporti" -> "🚗"
            "benzina" -> "⛽"
            "bollette" -> "💡"
            "affitto" -> "🏠"
            "salute" -> "🏥"
            "intrattenimento" -> "🎬"
            "shopping" -> "🛍️"
            "risparmio" -> "🐷"
            "investimenti" -> "📈"
            "bonifico" -> "💸"
            "prelievo" -> "🏧"
            else -> "💳"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddTransactionDialog"

        private const val ARG_ACCOUNT_ID = "account_id"
        private const val ARG_ACCOUNT_NAME = "account_name"

        /**
         * Crea una nuova istanza del dialog
         */
        fun newInstance(accountId: String, accountName: String): AddTransactionDialogFragment {
            return AddTransactionDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ACCOUNT_ID, accountId)
                    putString(ARG_ACCOUNT_NAME, accountName)
                }
            }
        }
    }
}