package com.example.conti.data.database.dao

import androidx.room.*
import com.example.conti.data.database.entities.Movimento
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object per l'entità Movimento.
 *
 * Fornisce metodi per:
 * - CRUD sui movimenti
 * - Query filtrate per conto, data, categoria
 * - Calcolo dei saldi
 * - Statistiche sui movimenti
 */
@Dao
interface MovimentoDao {

    // ========== OPERAZIONI BASE (CRUD) ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movimento: Movimento): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movimenti: List<Movimento>)

    @Update
    suspend fun update(movimento: Movimento)

    @Delete
    suspend fun delete(movimento: Movimento)

    @Query("DELETE FROM movimenti WHERE contoId = :contoId")
    suspend fun deleteAllByContoId(contoId: Long)

    // ========== QUERY PER SINGOLO CONTO ==========

    /**
     * Ottiene tutti i movimenti di un conto, ordinati per data decrescente (più recenti prima).
     */
    @Query("SELECT * FROM movimenti WHERE contoId = :contoId ORDER BY data DESC")
    fun getMovimentiByContoId(contoId: Long): Flow<List<Movimento>>

    /**
     * Ottiene i movimenti di un conto in un intervallo di date.
     */
    @Query("""
        SELECT * FROM movimenti 
        WHERE contoId = :contoId 
        AND data BETWEEN :dataInizio AND :dataFine 
        ORDER BY data DESC
    """)
    fun getMovimentiByContoIdAndDateRange(
        contoId: Long,
        dataInizio: LocalDate,
        dataFine: LocalDate
    ): Flow<List<Movimento>>

    // ========== QUERY PER TUTTI I CONTI ==========

    /**
     * Ottiene tutti i movimenti di tutti i conti, ordinati per data.
     */
    @Query("SELECT * FROM movimenti ORDER BY data DESC")
    fun getAllMovimenti(): Flow<List<Movimento>>

    /**
     * Ottiene tutti i movimenti in un intervallo di date (utile per report mensili/annuali).
     */
    @Query("""
        SELECT * FROM movimenti 
        WHERE data BETWEEN :dataInizio AND :dataFine 
        ORDER BY data DESC
    """)
    fun getMovimentiByDateRange(
        dataInizio: LocalDate,
        dataFine: LocalDate
    ): Flow<List<Movimento>>

    // ========== QUERY PER CATEGORIA ==========

    /**
     * Ottiene tutti i movimenti di una specifica categoria.
     * Utile per analizzare abbonamenti, spese ricorrenti, ecc.
     */
    @Query("SELECT * FROM movimenti WHERE categoria = :categoria ORDER BY data DESC")
    fun getMovimentiByCategoria(categoria: String): Flow<List<Movimento>>

    /**
     * Ottiene tutte le categorie distinte presenti nei movimenti.
     * Utile per popolare filtri o dropdown nell'UI.
     */
    @Query("SELECT DISTINCT categoria FROM movimenti ORDER BY categoria ASC")
    fun getAllCategorie(): Flow<List<String>>

    // ========== CALCOLI SALDO ==========

    /**
     * Calcola il saldo totale di un conto (somma di tutti i movimenti).
     * @return La somma di tutti gli importi (positivi e negativi)
     */
    @Query("SELECT COALESCE(SUM(importo), 0) FROM movimenti WHERE contoId = :contoId")
    fun getSaldoByContoId(contoId: Long): Flow<Double>

    /**
     * Calcola il saldo totale di un conto fino a una certa data.
     * @return La somma degli importi fino alla data specificata
     */
    @Query("""
        SELECT COALESCE(SUM(importo), 0) 
        FROM movimenti 
        WHERE contoId = :contoId AND data <= :data
    """)
    fun getSaldoByContoIdAndData(contoId: Long, data: LocalDate): Flow<Double>

    // ========== STATISTICHE ==========

    /**
     * Calcola il totale delle entrate (importi positivi) per un conto in un periodo.
     */
    @Query("""
        SELECT COALESCE(SUM(importo), 0) 
        FROM movimenti 
        WHERE contoId = :contoId 
        AND data BETWEEN :dataInizio AND :dataFine 
        AND importo > 0
    """)
    fun getTotaleEntrateByContoIdAndDateRange(
        contoId: Long,
        dataInizio: LocalDate,
        dataFine: LocalDate
    ): Flow<Double>

    /**
     * Calcola il totale delle uscite (importi negativi) per un conto in un periodo.
     */
    @Query("""
        SELECT COALESCE(SUM(ABS(importo)), 0) 
        FROM movimenti 
        WHERE contoId = :contoId 
        AND data BETWEEN :dataInizio AND :dataFine 
        AND importo < 0
    """)
    fun getTotaleUsciteByContoIdAndDateRange(
        contoId: Long,
        dataInizio: LocalDate,
        dataFine: LocalDate
    ): Flow<Double>

    /**
     * Ottiene il numero totale di movimenti per un conto.
     */
    @Query("SELECT COUNT(*) FROM movimenti WHERE contoId = :contoId")
    fun getCountMovimentiByContoId(contoId: Long): Flow<Int>

    // ========== MOVIMENTI RICORRENTI ==========

    /**
     * Ottiene tutti i movimenti ricorrenti (collegati a un abbonamento).
     */
    @Query("SELECT * FROM movimenti WHERE isRicorrente = 1 ORDER BY data DESC")
    fun getMovimentiRicorrenti(): Flow<List<Movimento>>

    /**
     * Ottiene i movimenti collegati a uno specifico abbonamento.
     */
    @Query("SELECT * FROM movimenti WHERE idAbbonamento = :abbonamentoId ORDER BY data DESC")
    fun getMovimentiByAbbonamentoId(abbonamentoId: Long): Flow<List<Movimento>>
}