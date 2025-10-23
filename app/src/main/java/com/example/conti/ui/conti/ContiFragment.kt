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
import com.example.conti.models.Account
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
 *
 * VERSIONE FIRESTORE
 */
class ContiFragment : Fragment() {

    private var _binding: FragmentContiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val application = requireActivity().application as ContiApplication
        ViewModelFactory(application.repository)
    }

    private lateinit var contiAdapter: ContiAdapter

    // ID del conto per cui importare movimenti
    private var accountIdForImport: String? = null

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

                if (filePath != null && accountIdForImport != null) {
                    importExcelFile(filePath, accountIdForImport!!)
                } else {
                    Toast.makeText(requireContext(), "Errore durante la selezione del file", Toast.LENGTH_SHORT).show()
                }
            }
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
        _binding = FragmentContiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        setupFab()
        setupObservers()
    }

    /**
     * Configura la RecyclerView.
     */
    private fun setupRecyclerView() {
        contiAdapter = ContiAdapter(
            onContoClick = { account ->
                mostraDialogOpzioniConto(account)
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
        binding.fabAggiungi.setOnClickListener {
            val selectedTab = binding.tabLayout.selectedTabPosition

            if (selectedTab == 0) {
                // Aggiungi conto da Excel
                mostraDialogAggiungiContoExcel()
            } else {
                // Aggiungi conto manuale
                mostraDialogAggiungiContoManuale()
            }
        }
    }

    /**
     * Configura gli observer.
     */
    private fun setupObservers() {
        viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
            // Aggiorna la vista in base al tab selezionato
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
            viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
                val excelAccounts = accounts.filter { it.isFromExcel }

                if (excelAccounts.isEmpty()) {
                    binding.rvConti.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = "Nessun conto Excel configurato"
                    binding.tvEmptyDescription.text = "Aggiungi un conto collegato a un file Excel"
                } else {
                    binding.rvConti.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                    contiAdapter.submitList(excelAccounts)
                }
            }
        }
    }

    /**
     * Mostra solo i conti inseriti manualmente.
     */
    private fun mostraContiManuali() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
                val manualAccounts = accounts.filter { !it.isFromExcel }

                if (manualAccounts.isEmpty()) {
                    binding.rvConti.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = "Nessun conto manuale"
                    binding.tvEmptyDescription.text = "Aggiungi un conto per inserire movimenti manualmente"
                } else {
                    binding.rvConti.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                    contiAdapter.submitList(manualAccounts)
                }
            }
        }
    }

    /**
     * Mostra dialog per aggiungere un conto da Excel.
     */
    private fun mostraDialogAggiungiContoExcel() {
        val dialogView = layoutInflater.inflate(com.example.conti.R.layout.dialog_aggiungi_conto_excel, null)

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

                if (nome.isBlank()) {
                    Toast.makeText(requireContext(), "Inserisci il nome del conto", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val saldoIniziale = saldoStr.toDoubleOrNull() ?: 0.0

                // Crea il nuovo account
                val nuovoAccount = Account(
                    name = nome,
                    bankName = istituto,
                    initialBalance = saldoIniziale,
                    balance = saldoIniziale,
                    accountType = "excel",
                    isFromExcel = true,
                    color = "#2196F3"
                )

                // Crea l'account in Firestore
                viewModel.createAccount(
                    account = nuovoAccount,
                    onSuccess = { accountId ->
                        android.util.Log.d("ContiFragment", "✅ Account creato con ID: $accountId")
                        Toast.makeText(requireContext(), "Conto creato! Ora seleziona il file Excel", Toast.LENGTH_LONG).show()

                        // Memorizza l'ID per l'import
                        accountIdForImport = accountId
                        checkPermissionAndSelectFile()
                    },
                    onError = { errore ->
                        android.util.Log.e("ContiFragment", "❌ Errore creazione account: $errore")
                        Toast.makeText(requireContext(), "Errore: $errore", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    /**
     * Mostra dialog per aggiungere un conto manuale.
     */
    private fun mostraDialogAggiungiContoManuale() {
        val dialogView = layoutInflater.inflate(com.example.conti.R.layout.dialog_aggiungi_conto_excel, null)

        val etNomeConto = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.conti.R.id.etNomeConto)
        val etIstituto = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.conti.R.id.etIstituto)
        val etSaldoIniziale = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.conti.R.id.etSaldoIniziale)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Aggiungi Conto Manuale")
            .setView(dialogView)
            .setPositiveButton("Crea") { _, _ ->
                val nome = etNomeConto?.text?.toString() ?: ""
                val istituto = etIstituto?.text?.toString() ?: ""
                val saldoStr = etSaldoIniziale?.text?.toString() ?: "0"

                if (nome.isBlank()) {
                    Toast.makeText(requireContext(), "Inserisci il nome del conto", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val saldoIniziale = saldoStr.toDoubleOrNull() ?: 0.0

                val nuovoAccount = Account(
                    name = nome,
                    bankName = istituto,
                    initialBalance = saldoIniziale,
                    balance = saldoIniziale,
                    accountType = "manual",
                    isFromExcel = false,
                    color = "#4CAF50"
                )

                viewModel.createAccount(
                    account = nuovoAccount,
                    onSuccess = { accountId ->
                        Toast.makeText(requireContext(), "Conto creato con successo!", Toast.LENGTH_SHORT).show()
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
     * Mostra dialog con opzioni per un conto.
     */
    private fun mostraDialogOpzioniConto(account: Account) {
        val opzioni = if (account.isFromExcel) {
            arrayOf("Aggiorna da Excel", "Modifica", "Elimina")
        } else {
            arrayOf("Modifica", "Elimina")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(account.name)
            .setItems(opzioni) { _, which ->
                when {
                    account.isFromExcel && which == 0 -> aggiornaContoExcel(account)
                    account.isFromExcel && which == 1 -> modificaConto(account)
                    account.isFromExcel && which == 2 -> eliminaConto(account)
                    !account.isFromExcel && which == 0 -> modificaConto(account)
                    !account.isFromExcel && which == 1 -> eliminaConto(account)
                }
            }
            .show()
    }

    /**
     * Aggiorna i movimenti di un conto da Excel.
     */
    private fun aggiornaContoExcel(account: Account) {
        accountIdForImport = account.id
        checkPermissionAndSelectFile()
    }

    /**
     * Modifica un conto.
     */
    private fun modificaConto(account: Account) {
        Toast.makeText(requireContext(), "Funzionalità in arrivo", Toast.LENGTH_SHORT).show()
    }

    /**
     * Elimina un conto.
     */
    private fun eliminaConto(account: Account) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Elimina Conto")
            .setMessage("Sei sicuro di voler eliminare '${account.name}'? Tutti i movimenti associati verranno eliminati.")
            .setPositiveButton("Elimina") { _, _ ->
                viewModel.deleteAccount(
                    accountId = account.id,
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
            // Android 13+ - Non serve permesso
            selezionaFileExcel()
        } else {
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
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel"
            ))
        }
        pickFileLauncher.launch(intent)
    }

    /**
     * Ottiene il percorso reale del file da URI.
     */
    private fun getRealPathFromURI(uri: android.net.Uri): String? {
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
     * Importa movimenti da file Excel.
     */
    private fun importExcelFile(filePath: String, accountId: String) {
        Toast.makeText(requireContext(), "Importazione da Excel...", Toast.LENGTH_SHORT).show()

        viewModel.importTransactionsFromExcel(
            accountId = accountId,
            filePath = filePath,
            onSuccess = { count ->
                Toast.makeText(
                    requireContext(),
                    "✅ Importati $count movimenti con successo!",
                    Toast.LENGTH_LONG
                ).show()
            },
            onError = { errore ->
                Toast.makeText(
                    requireContext(),
                    "❌ Errore: $errore",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}