package com.example.conti.ui.movimenti

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conti.R
import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.databinding.FragmentAddTransactionBinding
import com.example.conti.models.Transaction
import com.example.conti.utils.Constants
import com.example.conti.utils.CurrencyUtils
import com.example.conti.utils.MessageHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✨ Fragment per aggiungere un nuovo movimento - PAGE VERSION
 *
 * AGGIORNATO:
 * - ChipGroup per selezione Entrata/Uscita
 * - Colori distintivi (verde/rosso)
 * - Versione robusta testata
 */
class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val firestoreRepository = FirestoreRepository()

    // Argomenti passati al fragment
    private lateinit var accountId: String
    private lateinit var accountName: String

    // Data selezionata (default: oggi)
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recupera argomenti
        arguments?.let {
            accountId = it.getString(ARG_ACCOUNT_ID)
                ?: throw IllegalArgumentException("accountId mancante")
            accountName = it.getString(ARG_ACCOUNT_NAME) ?: "Conto"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupCategoryDropdown()
        setupDatePicker()
        setupButtons()
    }

    /**
     * Setup UI iniziale
     */
    private fun setupUI() {
        // Mostra nome del conto
        binding.tvNomeConto.text = "Conto: $accountName"

        // Imposta data corrente
        updateDateField()

        // ✅ ChipGroup gestisce automaticamente singleSelection
        // "Uscita" è già selezionato di default nel XML (app:checkedChip="@id/chipUscita")
    }

    /**
     * ✅ Setup dropdown categorie con layout personalizzato per dark theme
     */
    private fun setupCategoryDropdown() {
        val categorie = Constants.CATEGORIE_DEFAULT.map { categoria ->
            getCategoryIcon(categoria) + " " + categoria
        }

        // ✅ Usa layout personalizzato per dropdown dark theme
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown_menu,  // Layout personalizzato dark
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
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAnnulla.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSalva.setOnClickListener {
            salvaMovimento()
        }
    }

    /**
     * ✅ Valida e salva il movimento
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

        // ✅ Usa ChipGroup per determinare il tipo
        val isEntrata = binding.chipGroupTipo.checkedChipId == R.id.chipEntrata
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

                        // ✅ Toast SUCCESS
                        MessageHelper.showSuccess(
                            requireContext(),
                            "✅ Movimento aggiunto"
                        )

                        findNavController().popBackStack()
                    }
                    .onFailure { e ->
                        android.util.Log.e(TAG, "❌ Errore salvataggio movimento", e)

                        // ✅ Toast ERROR
                        MessageHelper.showError(
                            requireContext(),
                            "❌ Errore: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Eccezione durante salvataggio", e)

                // ✅ Toast ERROR
                MessageHelper.showError(
                    requireContext(),
                    "❌ Errore: ${e.message}"
                )
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
        const val TAG = "AddTransactionFragment"

        const val ARG_ACCOUNT_ID = "account_id"
        const val ARG_ACCOUNT_NAME = "account_name"
    }
}