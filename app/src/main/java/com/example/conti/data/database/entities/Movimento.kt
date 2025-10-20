package com.example.conti.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Entità che rappresenta un singolo movimento bancario (entrata o uscita).
 *
 * Ogni movimento è collegato a un Conto tramite foreign key.
 * Se il conto viene eliminato, tutti i suoi movimenti vengono eliminati automaticamente (CASCADE).
 *
 * Convenzioni:
 * - importo POSITIVO = Entrata (es. +1000.00 per stipendio)
 * - importo NEGATIVO = Uscita (es. -50.00 per spesa)
 *
 * @param id Identificativo univoco autogenerato
 * @param contoId ID del conto a cui appartiene il movimento
 * @param data Data del movimento (LocalDate per compatibilità con Java 8+ Time API)
 * @param descrizione Descrizione del movimento (es. "Stipendio", "Spesa supermercato")
 * @param importo Importo del movimento (positivo per entrate, negativo per uscite)
 * @param categoria Categoria per raggruppare i movimenti (es. "Stipendio", "Abbonamento", "Spesa", "Risparmio")
 * @param note Note aggiuntive opzionali
 * @param isRicorrente true se è un movimento ricorrente (es. abbonamento mensile)
 * @param idAbbonamento ID dell'abbonamento collegato (se isRicorrente = true)
 * @param dataInserimento Data in cui il movimento è stato inserito nel database
 */
@Entity(
    tableName = "movimenti",
    foreignKeys = [
        ForeignKey(
            entity = Conto::class,
            parentColumns = ["id"],
            childColumns = ["contoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contoId"]),
        Index(value = ["data"]),
        Index(value = ["categoria"])
    ]
)
data class Movimento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val contoId: Long,
    val data: LocalDate,
    val descrizione: String,
    val importo: Double,
    val categoria: String,
    val note: String? = null,
    val isRicorrente: Boolean = false,
    val idAbbonamento: Long? = null,
    val dataInserimento: LocalDate = LocalDate.now()
)