package com.example.conti.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entità che rappresenta un conto corrente/bancario.
 *
 * Ogni conto può essere:
 * - Sincronizzato da Excel (isFromExcel = true)
 * - Gestito manualmente dall'utente (isFromExcel = false)
 *
 * @param id Identificativo univoco autogenerato
 * @param nome Nome descrittivo del conto (es. "Conto Principale", "Carta Risparmio")
 * @param istituto Nome della banca (es. "Intesa Sanpaolo", "UniCredit")
 * @param saldoIniziale Saldo di partenza al momento della creazione del conto
 * @param colore Colore in formato esadecimale per identificare visivamente il conto (#RRGGBB)
 * @param isFromExcel true se i movimenti vengono importati da Excel, false se inseriti manualmente
 * @param pathExcel Percorso del file Excel se isFromExcel = true (es. "/storage/emulated/0/Dropbox/movimenti.xlsx")
 * @param dataUltimoAggiornamento Timestamp dell'ultimo aggiornamento da Excel (in millisecondi)
 */
@Entity(tableName = "conti")
data class Conto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,
    val istituto: String,
    val saldoIniziale: Double,
    val colore: String = "#4CAF50", // Verde di default
    val isFromExcel: Boolean = false,
    val pathExcel: String? = null,
    val dataUltimoAggiornamento: Long? = null
)