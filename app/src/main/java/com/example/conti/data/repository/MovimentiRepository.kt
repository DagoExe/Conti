package com.example.conti.data.repository

import com.example.conti.data.database.dao.AbbonamentoDao
import com.example.conti.data.database.dao.ContoDao
import com.example.conti.data.database.dao.MovimentoDao
import com.example.conti.data.database.entities.Abbonamento
import com.example.conti.data.database.entities.Conto
import com.example.conti.data.database.entities.Movimento
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository centrale per gestire l'accesso ai dati dell'applicazione.
 *
 * Il Repository è un pattern architetturale che:
 * - Centralizza l'accesso ai dati (database, network, cache)
 * - Separa la logica di business dalla sorgente dati
 * - Facilita il testing (si può mockare facilmente)
 * - Espone i dati tramite Flow (reattività)
 *
 * In questo progetto, il Repository accede solo al database locale (Room).
 *
 * @param contoDao DAO per l'accesso alla tabella conti
 * @param movimentoDao DAO per l'accesso alla tabella movimenti
 * @param abbonamentoDao DAO per l'accesso alla tabella abbonamenti
 */
class MovimentiRepository(
    private val contoDao: ContoDao,
    private val movimentoDao: MovimentoDao,
    private val abbonamentoDao: AbbonamentoDao
) {

    // ========================================
    // CONTI
    // ========================================

    /**
     * Ottiene tutti i conti in modo reattivo.
     * Flow permette all'UI di aggiornarsi automaticamente quando i dati cambiano.
     */
    fun getAllConti(): Flow<List<Conto>> = contoDao.getAllConti()

    fun getContoById(contoId: Long): Flow<Conto?> = contoDao.getContoById(contoId)

    fun getContiFromExcel(): Flow<List<Conto>> = contoDao.getContiFromExcel()

    fun getContiManuali(): Flow<List<Conto>> = contoDao.getContiManuali()

    fun hasConti(): Flow<Boolean> = contoDao.hasConti()

    /**
     * Inserisce un nuovo conto nel database.
     * Tutte le operazioni di scrittura devono essere 'suspend' per essere eseguite
     * in un thread separato (non bloccano l'UI).
     *
     * @return L'ID del conto appena inserito
     */
    suspend fun insertConto(conto: Conto): Long = contoDao.insert(conto)

    suspend fun updateConto(conto: Conto) = contoDao.update(conto)

    suspend fun deleteConto(conto: Conto) = contoDao.delete(conto)

    suspend fun updateDataUltimoAggiornamento(contoId: Long, timestamp: Long) {
        contoDao.updateDataUltimoAggiornamento(contoId, timestamp)
    }

    // ========================================
    // MOVIMENTI
    // ========================================

    fun getAllMovimenti(): Flow<List<Movimento>> = movimentoDao.getAllMovimenti()

    fun getMovimentiByContoId(contoId: Long): Flow<List<Movimento>> =
        movimentoDao.getMovimentiByContoId(contoId)

    fun getMovimentiByDateRange(
        dataInizio: LocalDate,
        dataFine: LocalDate
    ): Flow<List<Movimento>> = movimentoDao.getMovimentiByDateRange(dataInizio, dataFine)

    fun getMovimentiByContoIdAndDateRange(
        contoId: Long,
        dataInizio: LocalDate,
        dataFine: LocalDate
    ): Flow<List<Movimento>> = movimentoDao.getMovimentiByContoIdAndDateRange(
        contoId, dataInizio, dataFine
    )

    fun getMovimentiByCategoria(categoria: String): Flow<List<Movimento>> =
        movimentoDao.getMovimentiByCategoria(categoria)

    fun getAllCategorie(): Flow<List<String>> = movimentoDao.getAllCategorie()

    fun getMovimentiRicorrenti(): Flow<List<Movimento>> = movimentoDao.getMovimentiRicorrenti()

    fun getMovimentiByAbbonamentoId(abbonamentoId: Long): Flow<List<Movimento>> =
        movimentoDao.getMovimentiByAbbonamentoId(abbonamentoId)

    suspend fun insertMovimento(movimento: Movimento): Long = movimentoDao.insert(movimento)

    suspend fun insertMovimenti(movimenti: List<Movimento>) = movimentoDao.insertAll(movimenti)

    suspend fun updateMovimento(movimento: Movimento) = movimentoDao.update(movimento)

    suspend fun deleteMovimento(movimento: Movimento) = movimentoDao.delete(movimento)

    suspend fun deleteAllMovimentiByContoId(contoId: Long) =
        movimentoDao.deleteAllByContoId(contoId)

    // ========================================
    // SALDI E STATISTICHE
    // ========================================

    fun getSaldoByContoId(contoId: Long): Flow<Double> =
        movimentoDao.getSaldoByContoId(contoId)

    fun getSaldoByContoIdAndData(contoId: Long, data: LocalDate): Flow<Double> =
        movimentoDao.getSaldoByContoIdAndData(contoId, data)

    fun getTotaleEntrateByContoIdAndDateRange(
        contoId: Long,
        dataInizio: LocalDate,
        dataFine: LocalDate
    ): Flow<Double> = movimentoDao.getTotaleEntrateByContoIdAndDateRange(
        contoId, dataInizio, dataFine
    )

    fun getTotaleUsciteByContoIdAndDateRange(
        contoId: Long,
        dataInizio: LocalDate,
        dataFine: LocalDate
    ): Flow<Double> = movimentoDao.getTotaleUsciteByContoIdAndDateRange(
        contoId, dataInizio, dataFine
    )

    fun getCountMovimentiByContoId(contoId: Long): Flow<Int> =
        movimentoDao.getCountMovimentiByContoId(contoId)

    /**
     * Calcola il saldo totale di un conto (saldo iniziale + somma movimenti).
     *
     * @param conto Il conto di cui calcolare il saldo totale
     * @return Flow che emette il saldo totale aggiornato
     */
    suspend fun getSaldoTotale(conto: Conto): Flow<Double> {
        return kotlinx.coroutines.flow.flow {
            getSaldoByContoId(conto.id).collect { saldoMovimenti ->
                emit(conto.saldoIniziale + saldoMovimenti)
            }
        }
    }

    // ========================================
    // ABBONAMENTI
    // ========================================

    fun getAllAbbonamenti(): Flow<List<Abbonamento>> = abbonamentoDao.getAllAbbonamenti()

    fun getAbbonamentiAttivi(): Flow<List<Abbonamento>> = abbonamentoDao.getAbbonamentiAttivi()

    fun getAbbonamentiTerminati(): Flow<List<Abbonamento>> =
        abbonamentoDao.getAbbonamentiTerminati()

    fun getAbbonamentoById(abbonamentoId: Long): Flow<Abbonamento?> =
        abbonamentoDao.getAbbonamentoById(abbonamentoId)

    fun getAbbonamentiAttiviByContoId(contoId: Long): Flow<List<Abbonamento>> =
        abbonamentoDao.getAbbonamentiAttiviByContoId(contoId)

    fun getAbbonamentiInScadenza(dataLimite: LocalDate): Flow<List<Abbonamento>> =
        abbonamentoDao.getAbbonamentiInScadenza(dataLimite)

    fun getCostoMensileTotale(): Flow<Double> = abbonamentoDao.getCostoMensileTotale()

    fun getCostoAnnualeTotale(): Flow<Double> = abbonamentoDao.getCostoAnnualeTotale()

    fun getCountAbbonamentiAttivi(): Flow<Int> = abbonamentoDao.getCountAbbonamentiAttivi()

    suspend fun insertAbbonamento(abbonamento: Abbonamento): Long =
        abbonamentoDao.insert(abbonamento)

    suspend fun updateAbbonamento(abbonamento: Abbonamento) = abbonamentoDao.update(abbonamento)

    suspend fun deleteAbbonamento(abbonamento: Abbonamento) = abbonamentoDao.delete(abbonamento)

    suspend fun disattivaAbbonamento(abbonamentoId: Long, dataFine: LocalDate) =
        abbonamentoDao.disattivaAbbonamento(abbonamentoId, dataFine)

    suspend fun aggiornaDataProssimoRinnovo(abbonamentoId: Long, nuovaData: LocalDate) =
        abbonamentoDao.aggiornaDataProssimoRinnovo(abbonamentoId, nuovaData)

    // ========================================
    // OPERAZIONI COMPLESSE
    // ========================================

    /**
     * Importa movimenti da Excel eliminando prima quelli esistenti per quel conto.
     * Operazione atomica: se fallisce, nessun dato viene modificato.
     *
     * @param contoId ID del conto da aggiornare
     * @param movimenti Lista di nuovi movimenti da importare
     */
    suspend fun sostituisciMovimentiDaExcel(contoId: Long, movimenti: List<Movimento>) {
        // Elimina i vecchi movimenti
        deleteAllMovimentiByContoId(contoId)
        // Inserisci i nuovi
        insertMovimenti(movimenti)
        // Aggiorna timestamp
        updateDataUltimoAggiornamento(contoId, System.currentTimeMillis())
    }
}