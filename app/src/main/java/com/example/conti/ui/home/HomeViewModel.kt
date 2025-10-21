package com.example.conti.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.conti.data.database.entities.Conto
import com.example.conti.data.database.entities.Movimento
import com.example.conti.data.repository.MovimentiRepository
import com.example.conti.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel per la schermata Home.
 *
 * Responsabilità:
 * - Fornire i dati dei conti all'UI
 * - Calcolare i saldi totali
 * - Gestire operazioni come refresh da Excel
 *
 * Il ViewModel sopravvive ai cambiamenti di configurazione (rotazione schermo, ecc.)
 * e mantiene i dati in modo reattivo tramite LiveData/Flow.
 */
class HomeViewModel(
    private val repository: MovimentiRepository
) : ViewModel() {

    // ========================================
    // DATI OSSERVABILI
    // ========================================

    /**
     * Lista di tutti i conti, osservabile dall'UI.
     * Si aggiorna automaticamente quando i dati nel database cambiano.
     */
    val conti: LiveData<List<Conto>> = repository.getAllConti().asLiveData()

    /**
     * Verifica se esistono conti nel database.
     * Utile per mostrare una schermata di benvenuto al primo avvio.
     */
    val hasConti: LiveData<Boolean> = repository.hasConti().asLiveData()

    /**
     * Movimenti del mese corrente (tutti i conti).
     */
    val movimentiMeseCorrente: LiveData<List<Movimento>> = run {
        val (inizio, fine) = DateUtils.getPrimoGiornoMeseCorrente() to DateUtils.getUltimoGiornoMeseCorrente()
        repository.getMovimentiByDateRange(inizio, fine).asLiveData()
    }

    /**
     * Costo mensile totale degli abbonamenti attivi.
     */
    val costoAbbonamentiMensile: LiveData<Double> =
        repository.getCostoMensileTotale().asLiveData()

    /**
     * Numero di abbonamenti attivi.
     */
    val numeroAbbonamentiAttivi: LiveData<Int> =
        repository.getCountAbbonamentiAttivi().asLiveData()

    // ========================================
    // DATA CLASS PER STATISTICHE
    // ========================================

    /**
     * Data class che racchiude le statistiche di un conto.
     */
    data class ContoConSaldo(
        val conto: Conto,
        val saldo: Double,
        val numeroMovimenti: Int
    )

    /**
     * Ottiene tutti i conti con i loro saldi calcolati.
     *
     * @return Flow di lista di ContoConSaldo
     */
    fun getContiConSaldi(): Flow<List<ContoConSaldo>> {
        return repository.getAllConti().map { conti ->
            conti.map { conto ->
                // Per ogni conto, calcola il saldo
                val saldoMovimenti = getSaldoSincrono(conto.id)
                val numeroMovimenti = getNumeroMovimentiSincrono(conto.id)
                ContoConSaldo(
                    conto = conto,
                    saldo = conto.saldoIniziale + saldoMovimenti,
                    numeroMovimenti = numeroMovimenti
                )
            }
        }
    }

    // Helper sincroni (da usare solo all'interno di Flow/suspend)
    private suspend fun getSaldoSincrono(contoId: Long): Double {
        var saldo = 0.0
        repository.getSaldoByContoId(contoId).collect { saldo = it }
        return saldo
    }

    private suspend fun getNumeroMovimentiSincrono(contoId: Long): Int {
        var count = 0
        repository.getCountMovimentiByContoId(contoId).collect { count = it }
        return count
    }

    // ========================================
    // OPERAZIONI
    // ========================================

    /**
     * Inserisce un nuovo conto nel database.
     *
     * @param conto Il conto da inserire
     * @param onSuccess Callback chiamata con l'ID del conto inserito
     * @param onError Callback chiamata in caso di errore
     */
    fun inserisciConto(
        conto: Conto,
        onSuccess: (Long) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val id = repository.insertConto(conto)
                onSuccess(id)
            } catch (e: Exception) {
                onError(e.message ?: "Errore durante l'inserimento del conto")
            }
        }
    }

    /**
     * Aggiorna un conto esistente.
     */
    fun aggiornaConto(
        conto: Conto,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateConto(conto)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Errore durante l'aggiornamento del conto")
            }
        }
    }

    /**
     * Elimina un conto dal database.
     * ATTENZIONE: Elimina anche tutti i movimenti associati (CASCADE).
     */
    fun eliminaConto(
        conto: Conto,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteConto(conto)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Errore durante l'eliminazione del conto")
            }
        }
    }

    /**
     * Aggiorna i movimenti di un conto da file Excel.
     *
     * @param contoId ID del conto da aggiornare
     * @param filePath Percorso del file Excel
     * @param onSuccess Callback chiamata con il numero di movimenti importati
     * @param onError Callback chiamata in caso di errore
     */
    fun aggiornaMovimentiDaExcel(
        contoId: Long,
        filePath: String,
        onSuccess: (Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val excelReader = com.example.conti.data.excel.ExcelReader()
                val result = excelReader.leggiExcel(filePath, contoId)

                if (result.errori.isNotEmpty()) {
                    onError("Errori durante l'importazione:\n${result.errori.joinToString("\n")}")
                    return@launch
                }

                if (result.movimenti.isEmpty()) {
                    onError("Nessun movimento trovato nel file Excel")
                    return@launch
                }

                // Sostituisci i movimenti del conto
                repository.sostituisciMovimentiDaExcel(contoId, result.movimenti)
                onSuccess(result.movimenti.size)

            } catch (e: Exception) {
                onError(e.message ?: "Errore durante l'importazione da Excel")
            }
        }
    }

    /**
     * Calcola il saldo totale di tutti i conti.
     *
     * @return Flow che emette il saldo totale
     */
    fun getSaldoTotale(): Flow<Double> {
        return repository.getAllConti().map { conti ->
            conti.sumOf { conto ->
                var saldo = conto.saldoIniziale
                repository.getSaldoByContoId(conto.id).collect { saldo += it }
                saldo
            }
        }
    }

    /**
     * Ottiene le statistiche del mese corrente.
     */
    data class StatisticheMese(
        val totaleEntrate: Double,
        val totaleUscite: Double,
        val bilancio: Double,
        val numeroMovimenti: Int
    )

    fun getStatisticheMeseCorrente(): Flow<StatisticheMese> {
        val (inizio, fine) = DateUtils.getPrimoGiornoMeseCorrente() to DateUtils.getUltimoGiornoMeseCorrente()

        return repository.getMovimentiByDateRange(inizio, fine).map { movimenti ->
            val entrate = movimenti.filter { it.importo > 0 }.sumOf { it.importo }
            val uscite = movimenti.filter { it.importo < 0 }.sumOf { kotlin.math.abs(it.importo) }
            val bilancio = entrate - uscite

            StatisticheMese(
                totaleEntrate = entrate,
                totaleUscite = uscite,
                bilancio = bilancio,
                numeroMovimenti = movimenti.size
            )
        }
    }
}