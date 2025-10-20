package com.example.conti.data.database.dao

import androidx.room.*
import com.example.conti.data.database.entities.Conto
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object per l'entità Conto.
 *
 * Definisce tutte le operazioni CRUD (Create, Read, Update, Delete) sui conti.
 * Utilizza Flow per osservare i cambiamenti in tempo reale (pattern Observer).
 */
@Dao
interface ContoDao {

    /**
     * Inserisce un nuovo conto nel database.
     * @param conto Il conto da inserire
     * @return L'ID del conto appena inserito
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conto: Conto): Long

    /**
     * Aggiorna un conto esistente.
     * @param conto Il conto con i dati aggiornati
     */
    @Update
    suspend fun update(conto: Conto)

    /**
     * Elimina un conto dal database.
     * Nota: Tutti i movimenti collegati verranno eliminati automaticamente (CASCADE)
     * @param conto Il conto da eliminare
     */
    @Delete
    suspend fun delete(conto: Conto)

    /**
     * Ottiene tutti i conti, ordinati per nome.
     * Utilizza Flow per aggiornamenti reattivi: ogni volta che i dati cambiano,
     * l'UI viene automaticamente notificata.
     * @return Flow che emette la lista aggiornata dei conti
     */
    @Query("SELECT * FROM conti ORDER BY nome ASC")
    fun getAllConti(): Flow<List<Conto>>

    /**
     * Ottiene un singolo conto tramite ID.
     * @param contoId ID del conto da recuperare
     * @return Flow che emette il conto (o null se non esiste)
     */
    @Query("SELECT * FROM conti WHERE id = :contoId")
    fun getContoById(contoId: Long): Flow<Conto?>

    /**
     * Ottiene tutti i conti sincronizzati da Excel.
     * @return Flow con la lista dei conti con isFromExcel = true
     */
    @Query("SELECT * FROM conti WHERE isFromExcel = 1")
    fun getContiFromExcel(): Flow<List<Conto>>

    /**
     * Ottiene tutti i conti inseriti manualmente.
     * @return Flow con la lista dei conti con isFromExcel = false
     */
    @Query("SELECT * FROM conti WHERE isFromExcel = 0")
    fun getContiManuali(): Flow<List<Conto>>

    /**
     * Verifica se esiste almeno un conto.
     * Utile per mostrare un messaggio di benvenuto al primo avvio.
     * @return Flow che emette true se esistono conti, false altrimenti
     */
    @Query("SELECT EXISTS(SELECT 1 FROM conti LIMIT 1)")
    fun hasConti(): Flow<Boolean>

    /**
     * Aggiorna il timestamp dell'ultimo aggiornamento per un conto Excel.
     * @param contoId ID del conto da aggiornare
     * @param timestamp Timestamp in millisecondi
     */
    @Query("UPDATE conti SET dataUltimoAggiornamento = :timestamp WHERE id = :contoId")
    suspend fun updateDataUltimoAggiornamento(contoId: Long, timestamp: Long)
}