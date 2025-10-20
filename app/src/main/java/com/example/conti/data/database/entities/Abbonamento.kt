package com.example.conti.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Entità che rappresenta un abbonamento o spesa ricorrente.
 *
 * Permette di tracciare:
 * - Abbonamenti attivi (Netflix, Spotify, Palestra, ecc.)
 * - Spese ricorrenti (Affitto, Bollette, ecc.)
 * - Storico abbonamenti terminati
 *
 * @param id Identificativo univoco autogenerato
 * @param nome Nome dell'abbonamento (es. "Netflix", "Spotify Premium", "Palestra McFit")
 * @param descrizione Descrizione dettagliata opzionale
 * @param importo Importo della singola rata (es. 12.99 per Netflix mensile)
 * @param frequenza Frequenza di pagamento (MENSILE, TRIMESTRALE, SEMESTRALE, ANNUALE)
 * @param dataInizio Data di attivazione dell'abbonamento
 * @param dataProssimoRinnovo Data del prossimo addebito previsto
 * @param dataFine Data di disdetta (null se ancora attivo)
 * @param contoId ID del conto da cui viene addebitato
 * @param categoria Categoria di spesa (default "Abbonamento")
 * @param isAttivo true se l'abbonamento è ancora attivo, false se disdetto
 * @param note Note aggiuntive (es. "Disdire entro il 15/12")
 */
@Entity(
    tableName = "abbonamenti",
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
        Index(value = ["isAttivo"]),
        Index(value = ["dataProssimoRinnovo"])
    ]
)
data class Abbonamento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,
    val descrizione: String? = null,
    val importo: Double,
    val frequenza: FrequenzaPagamento,
    val dataInizio: LocalDate,
    val dataProssimoRinnovo: LocalDate,
    val dataFine: LocalDate? = null,
    val contoId: Long,
    val categoria: String = "Abbonamento",
    val isAttivo: Boolean = true,
    val note: String? = null
)

/**
 * Enum per le frequenze di pagamento degli abbonamenti.
 *
 * Utilizzato per calcolare automaticamente le date dei prossimi rinnovi
 * e per stimare la spesa mensile/annuale.
 */
enum class FrequenzaPagamento {
    MENSILE,      // Es. Netflix, Spotify
    TRIMESTRALE,  // Es. Abbonamenti trimestrali
    SEMESTRALE,   // Es. Assicurazioni semestrali
    ANNUALE;      // Es. Amazon Prime, domini web

    /**
     * Restituisce il numero di mesi rappresentati dalla frequenza
     */
    fun getMesi(): Int {
        return when (this) {
            MENSILE -> 1
            TRIMESTRALE -> 3
            SEMESTRALE -> 6
            ANNUALE -> 12
        }
    }

    /**
     * Restituisce una descrizione user-friendly della frequenza
     */
    fun getDescrizione(): String {
        return when (this) {
            MENSILE -> "Mensile"
            TRIMESTRALE -> "Trimestrale"
            SEMESTRALE -> "Semestrale"
            ANNUALE -> "Annuale"
        }
    }
}