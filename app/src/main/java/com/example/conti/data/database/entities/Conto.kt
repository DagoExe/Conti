package com.example.conti.data.database.entities

import java.time.LocalDate

/**
 * Data class per rappresentare un conto corrente/bancario.
 *
 * ⚠️ VERSIONE PLAIN - SENZA ROOM
 *
 * Questa è una semplice data class Kotlin utilizzata temporaneamente per:
 * 1. Supportare la lettura da Excel (ExcelReader richiede un contoId Long)
 * 2. Mantenere compatibilità durante la migrazione a Firestore
 *
 * Dopo la conversione completa a Firestore, questa classe può essere:
 * - Rimossa completamente, OPPURE
 * - Mantenuta come DTO per conversioni Excel
 */
data class Conto(
    /**
     * ID del conto (autogenerato)
     */
    val id: Long = 0,

    /**
     * Nome del conto (es. "Conto Corrente Principale")
     */
    val nome: String,

    /**
     * Istituto bancario (es. "BuddyBank", "Hype")
     */
    val istituto: String,

    /**
     * Saldo iniziale del conto
     */
    val saldoIniziale: Double,

    /**
     * Colore identificativo (formato esadecimale, es. "#4CAF50")
     */
    val colore: String,

    /**
     * Indica se il conto è sincronizzato da un file Excel
     */
    val isFromExcel: Boolean,

    /**
     * Percorso del file Excel (se isFromExcel = true)
     */
    val pathExcel: String? = null,

    /**
     * Data dell'ultimo aggiornamento
     */
    val dataUltimoAggiornamento: LocalDate? = null
) {
    /**
     * Calcola il saldo corrente (saldo iniziale + movimenti).
     * NOTA: Questo metodo richiede i movimenti dal database.
     */
    fun calcolaSaldoCorrente(sommaMovimenti: Double): Double {
        return saldoIniziale + sommaMovimenti
    }

    /**
     * Converte questo Conto in un Account Firestore.
     */
    fun toFirestoreAccount(firestoreId: String = ""): com.example.conti.models.Account {
        return com.example.conti.models.Account(
            id = firestoreId.ifEmpty { id.toString() },
            name = nome,
            bankName = istituto,
            accountType = if (isFromExcel) "excel" else "manual",
            balance = saldoIniziale,
            initialBalance = saldoIniziale,
            color = colore,
            isFromExcel = isFromExcel,
            excelPath = pathExcel
        )
    }
}