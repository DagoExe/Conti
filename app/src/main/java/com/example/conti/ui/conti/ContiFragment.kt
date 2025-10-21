package com.example.conti.ui.conti

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conti.ContiApplication
import com.example.conti.databinding.FragmentContiBinding
import com.example.conti.ui.ViewModelFactory
import com.example.conti.ui.adapters.ContiAdapter
import com.example.conti.ui.home.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * ContiFragment - Gestisce la visualizzazione e configurazione dei conti.
 *
 * Funzionalità:
 * - Visualizza conti da Excel e conti manuali (tramite tab)
 * - Permette di aggiungere nuovi conti
 * - Permette di importare movimenti da Excel per conti esistenti
 * - Permette di modificare/eliminare conti
 */
class ContiFragment : Fragment() {

    private var _binding: FragmentContiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val application = requireActivity().application as ContiApplication
        ViewModelFactory(application.repository)
    }

    private lateinit var contiAdapter: ContiAdapter

    // Launcher per selezionare file
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("ContiFragment", "=== File picker result received ===")
        android.util.Log.d("ContiFragment", "Result code: ${result.resultCode}")

        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                android.util.Log.d("ContiFragment", "File URI: $uri")

                // Ottieni il percorso del file
                val filePath = getRealPathFromURI(uri)
                android.util.Log.d("ContiFragment", "File path: $filePath")

                if (filePath != null) {
                    // Usa l'ID del conto memorizzato
                    val contoId = contoIdPerImport
                    android.util.Log.d("ContiFragment", "ContoId per import: $contoId")

                    if (contoId != null) {
                        mostraDialogImportExcel(filePath, contoId)
                    } else {
                        android.util.Log.e("ContiFragment", "❌ ContoId è null!")
                        Toast.makeText(requireContext(), "Errore: ID conto non trovato", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.util.Log.e("ContiFragment", "❌ Impossibile ottenere il percorso del file")
                    Toast.makeText(requireContext(), "Impossibile leggere il file", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                android.util.Log.e("ContiFragment", "❌ URI del file è null")
            }
        } else {
            android.util.Log.w("ContiFragment", "File picker cancellato o errore")
        }
    }

    // Launcher per richiedere permessi
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            selezionaFileExcel()
        } else {
            Toast.makeText(requireContext(), "Permesso necessario per leggere i file", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        android.util.Log.d("ContiFragment", "=== onCreateView chiamato ===")
        _binding = FragmentContiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        android.util.Log.d("ContiFragment", "=== onViewCreated chiamato ===")

        setupRecyclerView()
        setupTabs()
        setupFab()
        setupObservers()

        android.util.Log.d("ContiFragment", "Setup completato")
    }

    /**
     * Configura la RecyclerView.
     */
    private fun setupRecyclerView() {
        contiAdapter = ContiAdapter(
            onContoClick = { conto ->
                mostraDialogOpzioniConto(conto)
            }
        )

        binding.rvConti.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contiAdapter
        }
    }

    /**
     * Configura i tab per filtrare conti Excel / Manuali.
     */
    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> mostraContiExcel()
                    1 -> mostraContiManuali()
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    /**
     * Configura il FAB per aggiungere conti.
     */
    private fun setupFab() {
        android.util.Log.d("ContiFragment", "setupFab() chiamato")

        binding.fabAggiungi.setOnClickListener {
            android.util.Log.d("ContiFragment", "=== FAB CLICCATO ===")
            val selectedTab = binding.tabLayout.selectedTabPosition
            android.util.Log.d("ContiFragment", "Tab selezionato: $selectedTab")

            if (selectedTab == 0) {
                // Aggiungi conto da Excel
                android.util.Log.d("ContiFragment", "Mostrando dialog conto Excel")
                mostraDialogAggiungiContoExcel()
            } else {
                // Aggiungi conto manuale
                android.util.Log.d("ContiFragment", "Mostrando dialog conto manuale")
                mostraDialogAggiungiContoManuale()
            }
        }
    }

    /**
     * Configura gli observer.
     */
    private fun setupObservers() {
        viewModel.conti.observe(viewLifecycleOwner) { conti ->
            // Mostra i conti in base al tab selezionato
            val selectedTab = binding.tabLayout.selectedTabPosition
            if (selectedTab == 0) {
                mostraContiExcel()
            } else {
                mostraContiManuali()
            }
        }
    }

    /**
     * Mostra solo i conti sincronizzati da Excel.
     */
    private fun mostraContiExcel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.repository.getContiFromExcel().collect { conti ->
                if (conti.isEmpty()) {
                    binding.rvConti.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = "Nessun conto Excel configurato"
                    binding.tvEmptyDescription.text = "Aggiungi un conto collegato a un file Excel"
                    binding.fabAggiungi.text = "Aggiungi da Excel"
                } else {
                    binding.rvConti.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                    contiAdapter.submitList(conti)
                }
            }
        }
    }

    /**
     * Mostra solo i conti inseriti manualmente.
     */
    private fun mostraContiManuali() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.repository.getContiManuali().collect { conti ->
                if (conti.isEmpty()) {
                    binding.rvConti.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = "Nessun conto manuale"
                    binding.tvEmptyDescription.text = "Aggiungi un conto per inserire movimenti manualmente"
                    binding.fabAggiungi.text = "Aggiungi Manuale"
                } else {
                    binding.rvConti.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                    contiAdapter.submitList(conti)
                }
            }
        }
    }

    /**
     * Mostra dialog per aggiungere un conto da Excel.
     */
    private fun mostraDialogAggiungiContoExcel() {
        android.util.Log.d("ContiFragment", "=== mostraDialogAggiungiContoExcel chiamato ===")

        val dialogView = layoutInflater.inflate(com.example.conti.R.layout.dialog_aggiungi_conto_excel, null)

        // Ottieni i riferimenti ai campi
        val etNomeConto = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.conti.R.id.etNomeConto)
        val etIstituto = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.conti.R.id.etIstituto)
        val etSaldoIniziale = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.conti.R.id.etSaldoIniziale)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Aggiungi Conto da Excel")
            .setView(dialogView)
            .setPositiveButton("Crea e Importa") { _, _ ->
                val nome = etNomeConto?.text?.toString() ?: ""
                val istituto = etIstituto?.text?.toString() ?: ""
                val saldoStr = etSaldoIniziale?.text?.toString() ?: "0"

                android.util.Log.d("ContiFragment", "Dati inseriti - Nome: $nome, Istituto: $istituto, Saldo: $saldoStr")

                if (nome.isBlank()) {
                    Toast.makeText(requireContext(), "Inserisci il nome del conto", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val saldoIniziale = saldoStr.toDoubleOrNull() ?: 0.0

                // PRIMA: Crea il conto
                val nuovoConto = com.example.conti.data.database.entities.Conto(
                    nome = nome,
                    istituto = istituto,
                    saldoIniziale = saldoIniziale,
                    colore = "#2196F3",
                    isFromExcel = true,
                    pathExcel = null
                )

                android.util.Log.d("ContiFragment", "Creazione conto: $nuovoConto")

                // Inserisci il conto nel database
                viewModel.inserisciConto(
                    conto = nuovoConto,
                    onSuccess = { contoId ->
                        android.util.Log.d("ContiFragment", "✅ Conto creato con ID: $contoId")
                        Toast.makeText(requireContext(), "Conto creato! Ora seleziona il file Excel", Toast.LENGTH_LONG).show()

                        // POI: Apri il file picker per importare i movimenti
                        contoIdPerImport = contoId
                        checkPermissionAndSelectFile()
                    },
                    onError = { errore ->
                        android.util.Log.e("ContiFragment", "❌ Errore creazione conto: $errore")
                        Toast.makeText(requireContext(), "Errore: $errore", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // Variabile per memorizzare l'ID del conto da importare
    private var contoIdPerImport: Long? = null

    /**
     * Mostra dialog per aggiungere un conto manuale.
     */
    private fun mostraDialogAggiungiContoManuale() {
        // TODO: Implementare dialog per aggiunta conto manuale
        Toast.makeText(requireContext(), "Funzionalità in arrivo", Toast.LENGTH_SHORT).show()
    }

    /**
     * Mostra dialog con opzioni per un conto (modifica, elimina, aggiorna da Excel).
     */
    private fun mostraDialogOpzioniConto(conto: com.example.conti.data.database.entities.Conto) {
        val opzioni = if (conto.isFromExcel) {
            arrayOf("Aggiorna da Excel", "Modifica", "Elimina")
        } else {
            arrayOf("Modifica", "Elimina")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(conto.nome)
            .setItems(opzioni) { _, which ->
                when {
                    conto.isFromExcel && which == 0 -> aggiornaContoExcel(conto)
                    conto.isFromExcel && which == 1 -> modificaConto(conto)
                    conto.isFromExcel && which == 2 -> eliminaConto(conto)
                    !conto.isFromExcel && which == 0 -> modificaConto(conto)
                    !conto.isFromExcel && which == 1 -> eliminaConto(conto)
                }
            }
            .show()
    }

    /**
     * Aggiorna i movimenti di un conto da Excel.
     */
    private fun aggiornaContoExcel(conto: com.example.conti.data.database.entities.Conto) {
        if (conto.pathExcel != null) {
            mostraDialogImportExcel(conto.pathExcel, conto.id)
        } else {
            checkPermissionAndSelectFileForConto(conto.id)
        }
    }

    /**
     * Modifica un conto.
     */
    private fun modificaConto(conto: com.example.conti.data.database.entities.Conto) {
        // TODO: Implementare dialog modifica
        Toast.makeText(requireContext(), "Funzionalità in arrivo", Toast.LENGTH_SHORT).show()
    }

    /**
     * Elimina un conto.
     */
    private fun eliminaConto(conto: com.example.conti.data.database.entities.Conto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Elimina Conto")
            .setMessage("Sei sicuro di voler eliminare '${conto.nome}'? Tutti i movimenti associati verranno eliminati.")
            .setPositiveButton("Elimina") { _, _ ->
                viewModel.eliminaConto(
                    conto = conto,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Conto eliminato", Toast.LENGTH_SHORT).show()
                    },
                    onError = { errore ->
                        Toast.makeText(requireContext(), "Errore: $errore", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    /**
     * Verifica i permessi e seleziona il file.
     */
    private fun checkPermissionAndSelectFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - Non serve permesso per READ_EXTERNAL_STORAGE
            selezionaFileExcel()
        } else {
            // Android 12 e precedenti
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    selezionaFileExcel()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    /**
     * Apre il file picker per selezionare un file Excel.
     */
    private fun selezionaFileExcel() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
                "application/vnd.ms-excel" // .xls
            ))
        }
        pickFileLauncher.launch(intent)
    }

    /**
     * Ottiene il percorso reale del file da URI.
     */
    private fun getRealPathFromURI(uri: android.net.Uri): String? {
        // Per semplicità, usiamo il content resolver per copiare il file
        // In una app di produzione, dovresti gestire meglio gli URI
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = java.io.File(requireContext().cacheDir, "temp_excel.xlsx")
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Mostra dialog per importare movimenti da Excel.
     */
    private fun mostraDialogImportExcel(filePath: String, contoId: Long? = null) {
        android.util.Log.d("ContiFragment", "=== INIZIO IMPORT EXCEL ===")
        android.util.Log.d("ContiFragment", "File path: $filePath")
        android.util.Log.d("ContiFragment", "Conto ID: $contoId")

        Toast.makeText(requireContext(), "Importazione da: $filePath", Toast.LENGTH_SHORT).show()

        if (contoId != null) {
            // Aggiorna conto esistente
            viewModel.aggiornaMovimentiDaExcel(
                contoId = contoId,
                filePath = filePath,
                onSuccess = { numeroMovimenti ->
                    android.util.Log.d("ContiFragment", "✅ IMPORT SUCCESS: $numeroMovimenti movimenti importati")
                    Toast.makeText(requireContext(),
                        "✅ Importati $numeroMovimenti movimenti con successo!",
                        Toast.LENGTH_LONG).show()

                    // Verifica i movimenti nel database
                    verificaMovimentiImportati(contoId)
                },
                onError = { errore ->
                    android.util.Log.e("ContiFragment", "❌ IMPORT ERROR: $errore")
                    Toast.makeText(requireContext(), "❌ Errore: $errore", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            android.util.Log.w("ContiFragment", "⚠️ ContoId è null - impossibile importare")
        }
    }

    /**
     * Verifica che i movimenti siano stati effettivamente salvati nel database.
     */
    private fun verificaMovimentiImportati(contoId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.repository.getMovimentiByContoId(contoId).collect { movimenti ->
                android.util.Log.d("ContiFragment", "📊 Movimenti nel database per conto $contoId: ${movimenti.size}")

                if (movimenti.isNotEmpty()) {
                    // Mostra i primi 3 movimenti come esempio
                    movimenti.take(3).forEachIndexed { index, movimento ->
                        android.util.Log.d("ContiFragment", "  ${index + 1}. ${movimento.data} - ${movimento.descrizione}: ${movimento.importo}€")
                    }
                }
            }
        }
    }

    private fun checkPermissionAndSelectFileForConto(contoId: Long) {
        // Implementazione simile a checkPermissionAndSelectFile
        checkPermissionAndSelectFile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}