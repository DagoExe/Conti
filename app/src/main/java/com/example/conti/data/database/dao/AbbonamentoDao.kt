package com.example.conti.data.database.dao

import androidx.room.*
import com.example.conti.data.database.entities.Abbonamento
import com.example.conti.data.database.entities.FrequenzaPagamento
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object per l'entità Abbonamento.
 *
 * Fornisce metodi per gestire abbonamenti e spese ricorrenti:
 * - CRUD sugli abbonamenti
 * - Filtri per stato (attivi/terminati)
 * - Calcolo costi mensili/annuali
 * - Scadenze imminenti
 */
@Dao
interface AbbonamentoDao {

    // ========== OPERAZIONI BASE (CRUD) ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(abbonamento: Abbonamento): Long

    @Update
    suspend fun update(abbonamento: Abbonamento)

    @Delete
    suspend fun delete(abbonamento: Abbonamento)

    /**
     * Disattiva un abbonamento (imposta isAttivo = false e dataFine).
     * Non elimina i dati storici, permette di mantenere lo storico.
     */
    @Query("UPDATE abbonamenti SET isAttivo = 0, dataFine = :dataFine WHERE id = :abbonamentoId")
    suspend fun disattivaAbbonamento(abbonamentoId: Long, dataFine: LocalDate)

    // ========== QUERY PRINCIPALI ==========

    /**
     * Ottiene tutti gli abbonamenti ordinati per nome.
     */
    @Query("SELECT * FROM abbonamenti ORDER BY nome ASC")
    fun getAllAbbonamenti(): Flow<List<Abbonamento>>

    /**
     * Ottiene solo gli abbonamenti attivi.
     */
    @Query("SELECT * FROM abbonamenti WHERE isAttivo = 1 ORDER BY dataProssimoRinnovo ASC")
    fun getAbbonamentiAttivi(): Flow<List<Abbonamento>>

    /**
     * Ottiene solo gli abbonamenti terminati/disdetti.
     */
    @Query("SELECT * FROM abbonamenti WHERE isAttivo = 0 ORDER BY dataFine DESC")
    fun getAbbonamentiTerminati(): Flow<List<Abbonamento>>

    /**
     * Ottiene un abbonamento specifico tramite ID.
     */
    @Query("SELECT * FROM abbonamenti WHERE id = :abbonamentoId")
    fun getAbbonamentoById(abbonamentoId: Long): Flow<Abbonamento?>

    // ========== QUERY PER CONTO ==========

    /**
     * Ottiene tutti gli abbonamenti attivi collegati a un conto.
     */
    @Query("""
        SELECT * FROM abbonamenti 
        WHERE contoId = :contoId AND isAttivo = 1 
        ORDER BY nome ASC
    """)
    fun getAbbonamentiAttiviByContoId(contoId: Long): Flow<List<Abbonamento>>

    // ========== SCADENZE E RINNOVI ==========

    /**
     * Ottiene gli abbonamenti con rinnovo imminente (entro N giorni).
     * Utile per notifiche o avvisi nell'UI.
     */
    @Query("""
        SELECT * FROM abbonamenti 
        WHERE isAttivo = 1 
        AND dataProssimoRinnovo <= :dataLimite 
        ORDER BY dataProssimoRinnovo ASC
    """)
    fun getAbbonamentiInScadenza(dataLimite: LocalDate): Flow<List<Abbonamento>>

    /**
     * Aggiorna la data del prossimo rinnovo di un abbonamento.
     * Da chiamare dopo aver registrato un pagamento.
     */
    @Query("UPDATE abbonamenti SET dataProssimoRinnovo = :nuovaData WHERE id = :abbonamentoId")
    suspend fun aggiornaDataProssimoRinnovo(abbonamentoId: Long, nuovaData: LocalDate)

    // ========== STATISTICHE E CALCOLI ==========

    /**
     * Calcola il costo mensile totale di tutti gli abbonamenti attivi.
     * Converte automaticamente abbonamenti trimestrali/annuali in costo mensile.
     *
     * Formula:
     * - MENSILE: importo * 1
     * - TRIMESTRALE: importo / 3
     * - SEMESTRALE: importo / 6
     * - ANNUALE: importo / 12
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE frequenza
                WHEN 'MENSILE' THEN importo
                WHEN 'TRIMESTRALE' THEN importo / 3.0
                WHEN 'SEMESTRALE' THEN importo / 6.0
                WHEN 'ANNUALE' THEN importo / 12.0
                ELSE 0
            END
        ), 0) 
        FROM abbonamenti 
        WHERE isAttivo = 1
    """)
    fun getCostoMensileTotale(): Flow<Double>

    /**
     * Calcola il costo annuale totale di tutti gli abbonamenti attivi.
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE frequenza
                WHEN 'MENSILE' THEN importo * 12
                WHEN 'TRIMESTRALE' THEN importo * 4
                WHEN 'SEMESTRALE' THEN importo * 2
                WHEN 'ANNUALE' THEN importo
                ELSE 0
            END
        ), 0) 
        FROM abbonamenti 
        WHERE isAttivo = 1
    """)
    fun getCostoAnnualeTotale(): Flow<Double>

    /**
     * Conta il numero di abbonamenti attivi.
     */
    @Query("SELECT COUNT(*) FROM abbonamenti WHERE isAttivo = 1")
    fun getCountAbbonamentiAttivi(): Flow<Int>

    /**
     * Ottiene gli abbonamenti raggruppati per frequenza.
     * Utile per visualizzare statistiche nell'UI.
     */
    @Query("""
        SELECT frequenza, COUNT(*) as count, SUM(importo) as totale 
        FROM abbonamenti 
        WHERE isAttivo = 1 
        GROUP BY frequenza
    """)
    fun getAbbonamentiGroupedByFrequenza(): Flow<List<FrequenzaStatistica>>
}

/**
 * Data class per le statistiche raggruppate per frequenza.
 */
data class FrequenzaStatistica(
    val frequenza: FrequenzaPagamento,
    val count: Int,
    val totale: Double
)