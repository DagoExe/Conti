package com.example.conti.data.database.entities

import java.time.LocalDate

/**
 * Data class per rappresentare un movimento bancario.
 *
 * ⚠️ VERSIONE PLAIN - SENZA ROOM
 *
 * Questa è una semplice data class Kotlin utilizzata per:
 * 1. Ricevere i dati letti da ExcelReader
 * 2. Convertirli in Transaction Firestore
 *
 * Dopo la conversione completa a Firestore, questa classe può essere:
 * - Rimossa completamente, OPPURE
 * - Mantenuta come DTO per conversioni Excel
 */
data class Movimento(
    /**
     * ID del movimento (autogenerato)
     */
    val id: Long = 0,

    /**
     * ID del conto a cui appartiene questo movimento
     */
    val contoId: Long,

    /**
     * Data del movimento
     */
    val data: LocalDate,

    /**
     * Descrizione del movimento
     */
    val descrizione: String,

    /**
     * Importo del movimento
     * - Positivo = entrata
     * - Negativo = uscita
     */
    val importo: Double,

    /**
     * Categoria del movimento (es. "Spesa", "Stipendio", ecc.)
     */
    val categoria: String,

    /**
     * Note opzionali
     */
    val note: String? = null,

    /**
     * Indica se il movimento è ricorrente (collegato a un abbonamento)
     */
    val isRicorrente: Boolean = false,

    /**
     * ID dell'abbonamento associato (se isRicorrente = true)
     */
    val idAbbonamento: Long? = null,

    /**
     * Data di inserimento nel database
     */
    val dataInserimento: LocalDate = LocalDate.now()
) {
    /**
     * Verifica se il movimento è un'entrata
     */
    fun isEntrata(): Boolean = importo > 0

    /**
     * Verifica se il movimento è un'uscita
     */
    fun isUscita(): Boolean = importo < 0

    /**
     * Converte questo Movimento in una Transaction Firestore.
     */
    fun toFirestoreTransaction(
        accountId: String,
        transactionId: String = ""
    ): com.example.conti.models.Transaction {
        return com.example.conti.models.Transaction(
            id = transactionId.ifEmpty { id.toString() },
            accountId = accountId,
            amount = importo,
            description = descrizione,
            category = categoria,
            notes = note,
            date = com.google.firebase.Timestamp(
                java.util.Date.from(
                    data.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                )
            ),
            type = if (importo >= 0) "income" else "expense",
            isRecurring = isRicorrente,
            subscriptionId = idAbbonamento?.toString()
        )
    }
}