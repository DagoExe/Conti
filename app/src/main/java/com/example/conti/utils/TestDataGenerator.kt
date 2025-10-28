package com.example.conti.utils

import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.models.Transaction
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * Utility per creare transazioni di test.
 *
 * USO:
 * ```
 * TestDataGenerator.createSampleTransactions()
 * ```
 */
object TestDataGenerator {

    private val firestoreRepository = FirestoreRepository()

    /**
     * Crea transazioni di test per BuddyBank e Hype
     */
    fun createSampleTransactions() {
        CoroutineScope(Dispatchers.IO).launch {
            android.util.Log.d("TestDataGenerator", "🚀 Inizio creazione transazioni di test...")

            val transactions = mutableListOf<Transaction>()

            // ========================================
            // TRANSAZIONI BUDDYBANK
            // ========================================

            // Stipendio
            transactions.add(
                Transaction(
                    accountId = "buddybank",
                    amount = 1500.0,
                    description = "Stipendio Mensile",
                    category = "Stipendio",
                    date = getDateDaysAgo(5),
                    type = "income",
                    notes = "Accredito stipendio"
                )
            )

            // Affitto
            transactions.add(
                Transaction(
                    accountId = "buddybank",
                    amount = -650.0,
                    description = "Pagamento Affitto Gennaio",
                    category = "Affitto",
                    date = getDateDaysAgo(3),
                    type = "expense",
                    notes = "Bonifico a proprietario"
                )
            )

            // Bollette
            transactions.add(
                Transaction(
                    accountId = "buddybank",
                    amount = -85.0,
                    description = "Bolletta Luce - Enel",
                    category = "Bollette",
                    date = getDateDaysAgo(2),
                    type = "expense"
                )
            )

            transactions.add(
                Transaction(
                    accountId = "buddybank",
                    amount = -42.0,
                    description = "Bolletta Gas - Eni",
                    category = "Bollette",
                    date = getDateDaysAgo(2),
                    type = "expense"
                )
            )

            // Bonifico risparmi
            transactions.add(
                Transaction(
                    accountId = "buddybank",
                    amount = -200.0,
                    description = "Trasferimento a Conto Risparmio",
                    category = "Risparmio",
                    date = getDateDaysAgo(1),
                    type = "transfer"
                )
            )

            // ========================================
            // TRANSAZIONI HYPE
            // ========================================

            // Spesa supermercato
            transactions.add(
                Transaction(
                    accountId = "hype",
                    amount = -68.50,
                    description = "Spesa Supermercato Conad",
                    category = "Spesa",
                    date = getDateDaysAgo(6),
                    type = "expense"
                )
            )

            transactions.add(
                Transaction(
                    accountId = "hype",
                    amount = -45.20,
                    description = "Spesa Eurospin",
                    category = "Spesa",
                    date = getDateDaysAgo(3),
                    type = "expense"
                )
            )

            // Ristoranti
            transactions.add(
                Transaction(
                    accountId = "hype",
                    amount = -32.0,
                    description = "Pizza con amici",
                    category = "Ristorante",
                    date = getDateDaysAgo(5),
                    type = "expense"
                )
            )

            transactions.add(
                Transaction(
                    accountId = "hype",
                    amount = -18.50,
                    description = "Pranzo al bar",
                    category = "Ristorante",
                    date = getDateDaysAgo(2),
                    type = "expense"
                )
            )

            // Benzina
            transactions.add(
                Transaction(
                    accountId = "hype",
                    amount = -55.0,
                    description = "Rifornimento ENI",
                    category = "Benzina",
                    date = getDateDaysAgo(4),
                    type = "expense"
                )
            )

            transactions.add(
                Transaction(
                    accountId = "hype",
                    amount = -48.30,
                    description = "Rifornimento Q8",
                    category = "Benzina",
                    date = getDateDaysAgo(1),
                    type = "expense"
                )
            )

            // Abbonamenti
            transactions.add(
                Transaction(
                    accountId = "hype",
                    amount = -9.99,
                    description = "Netflix",
                    category = "Abbonamento",
                    date = getDateDaysAgo(7),
                    type = "expense",
                    isRecurring = true
                )
            )

            transactions.add(
                Transaction(
                    accountId = "hype",
                    amount = -4.99,
                    description = "Spotify Premium",
                    category = "Abbonamento",
                    date = getDateDaysAgo(6),
                    type = "expense",
                    isRecurring = true
                )
            )

            // Shopping
            transactions.add(
                Transaction(
                    accountId = "hype",
                    amount = -89.90,
                    description = "Scarpe Nike",
                    category = "Shopping",
                    date = getDateDaysAgo(8),
                    type = "expense"
                )
            )

            // Trasporti
            transactions.add(
                Transaction(
                    accountId = "hype",
                    amount = -25.0,
                    description = "Abbonamento Bus Mensile",
                    category = "Trasporti",
                    date = getDateDaysAgo(9),
                    type = "expense"
                )
            )

            // ========================================
            // SALVA LE TRANSAZIONI
            // ========================================

            var successCount = 0
            var errorCount = 0

            transactions.forEach { transaction ->
                firestoreRepository.addTransaction(transaction)
                    .onSuccess {
                        successCount++
                        android.util.Log.d("TestDataGenerator", "✅ Transazione creata: ${transaction.description}")
                    }
                    .onFailure { e ->
                        errorCount++
                        android.util.Log.e("TestDataGenerator", "❌ Errore creando transazione: ${transaction.description}", e)
                    }
            }

            // Aspetta un po' per permettere il completamento
            kotlinx.coroutines.delay(2000)

            android.util.Log.d("TestDataGenerator", "════════════════════════════════")
            android.util.Log.d("TestDataGenerator", "✅ Transazioni create: $successCount")
            android.util.Log.d("TestDataGenerator", "❌ Errori: $errorCount")
            android.util.Log.d("TestDataGenerator", "📊 Totale: ${transactions.size}")
            android.util.Log.d("TestDataGenerator", "════════════════════════════════")
        }
    }

    /**
     * Restituisce un timestamp X giorni fa
     */
    private fun getDateDaysAgo(daysAgo: Int): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return Timestamp(calendar.time)
    }
}