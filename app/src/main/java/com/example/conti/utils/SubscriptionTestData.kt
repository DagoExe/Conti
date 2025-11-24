package com.example.conti.utils

import com.example.conti.data.repository.FirestoreRepository
import com.example.conti.data.repository.deleteSubscription
import com.example.conti.models.PaymentFrequency
import com.example.conti.models.Subscription
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * Utility per creare abbonamenti di test.
 *
 * UTILIZZO:
 * ```kotlin
 * // In AbbonamentiFragment o durante il testing
 * SubscriptionTestData.createSampleSubscriptions()
 * ```
 *
 * ⚠️ ATTENZIONE: Questa classe è solo per testing/sviluppo.
 * Non utilizzare in produzione.
 */
object SubscriptionTestData {

    private val firestoreRepository = FirestoreRepository()

    /**
     * Crea abbonamenti di esempio per testing
     *
     * Include abbonamenti popolari come:
     * - Netflix
     * - Spotify
     * - Disney+
     * - Amazon Prime
     * - Xbox Game Pass
     * - Palestra
     * - ecc.
     */
    fun createSampleSubscriptions() {
        CoroutineScope(Dispatchers.IO).launch {
            android.util.Log.d("SubscriptionTestData", "🚀 Inizio creazione abbonamenti di test...")

            val subscriptions = listOf(
                // Netflix - Mensile
                Subscription(
                    accountId = "hype", // Assumi Hype come conto
                    name = "Netflix",
                    description = "Piano Standard (2 schermi)",
                    amount = 12.99,
                    frequency = "MONTHLY",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(3),
                    nextRenewalDate = getDateDaysFromNow(5),
                    isActive = true,
                    notes = "Condiviso con famiglia"
                ),

                // Spotify - Mensile
                Subscription(
                    accountId = "hype",
                    name = "Spotify Premium",
                    description = "Piano Individuale",
                    amount = 9.99,
                    frequency = "MONTHLY",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(6),
                    nextRenewalDate = getDateDaysFromNow(12),
                    isActive = true
                ),

                // Disney+ - Annuale
                Subscription(
                    accountId = "buddybank",
                    name = "Disney+",
                    description = "Piano Annuale",
                    amount = 89.90,
                    frequency = "ANNUAL",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(2),
                    nextRenewalDate = getDateDaysFromNow(300),
                    isActive = true,
                    notes = "Pagato in anticipo per sconto"
                ),

                // Amazon Prime - Annuale
                Subscription(
                    accountId = "buddybank",
                    name = "Amazon Prime",
                    description = "Spedizioni e Prime Video",
                    amount = 49.90,
                    frequency = "ANNUAL",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(8),
                    nextRenewalDate = getDateDaysFromNow(120),
                    isActive = true
                ),

                // YouTube Premium - Mensile
                Subscription(
                    accountId = "hype",
                    name = "YouTube Premium",
                    description = "Senza pubblicità",
                    amount = 11.99,
                    frequency = "MONTHLY",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(1),
                    nextRenewalDate = getDateDaysFromNow(18),
                    isActive = true
                ),

                // Xbox Game Pass - Mensile
                Subscription(
                    accountId = "hype",
                    name = "Xbox Game Pass Ultimate",
                    description = "Console + PC + Cloud",
                    amount = 14.99,
                    frequency = "MONTHLY",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(4),
                    nextRenewalDate = getDateDaysFromNow(8),
                    isActive = true
                ),

                // Palestra - Trimestrale
                Subscription(
                    accountId = "buddybank",
                    name = "Palestra FitGym",
                    description = "Abbonamento Trimestrale",
                    amount = 150.00,
                    frequency = "QUARTERLY",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(2),
                    nextRenewalDate = getDateDaysFromNow(45),
                    isActive = true,
                    notes = "Rinnovo a marzo"
                ),

                // Telefono Mobile - Mensile
                Subscription(
                    accountId = "buddybank",
                    name = "Iliad Mobile",
                    description = "Piano 150GB 5G",
                    amount = 9.99,
                    frequency = "MONTHLY",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(12),
                    nextRenewalDate = getDateDaysFromNow(3),
                    isActive = true
                ),

                // Internet Fibra - Mensile
                Subscription(
                    accountId = "buddybank",
                    name = "Fibra TIM",
                    description = "1 Gbit/s",
                    amount = 24.90,
                    frequency = "MONTHLY",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(18),
                    nextRenewalDate = getDateDaysFromNow(15),
                    isActive = true
                ),

                // Apple iCloud - Mensile
                Subscription(
                    accountId = "hype",
                    name = "iCloud Storage",
                    description = "50 GB",
                    amount = 0.99,
                    frequency = "MONTHLY",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(24),
                    nextRenewalDate = getDateDaysFromNow(6),
                    isActive = true
                ),

                // Audible - Mensile (INATTIVO)
                Subscription(
                    accountId = "hype",
                    name = "Audible",
                    description = "1 Audiolibro/mese",
                    amount = 9.99,
                    frequency = "MONTHLY",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(10),
                    nextRenewalDate = getDateDaysFromNow(-30), // Scaduto
                    isActive = false,
                    notes = "Disattivato temporaneamente"
                ),

                // PlayStation Plus - Annuale
                Subscription(
                    accountId = "hype",
                    name = "PlayStation Plus",
                    description = "Piano Essential",
                    amount = 59.99,
                    frequency = "ANNUAL",
                    category = "Abbonamento",
                    startDate = getDateMonthsAgo(5),
                    nextRenewalDate = getDateDaysFromNow(210),
                    isActive = true
                )
            )

            // Salva gli abbonamenti
            var successCount = 0
            var errorCount = 0

            subscriptions.forEach { subscription ->
                firestoreRepository.createSubscription(subscription)
                    .onSuccess {
                        successCount++
                        android.util.Log.d("SubscriptionTestData", "✅ Abbonamento creato: ${subscription.name}")
                    }
                    .onFailure { e ->
                        errorCount++
                        android.util.Log.e("SubscriptionTestData", "❌ Errore creando abbonamento: ${subscription.name}", e)
                    }
            }

            // Aspetta un po' per permettere il completamento
            kotlinx.coroutines.delay(2000)

            android.util.Log.d("SubscriptionTestData", """
                ════════════════════════════════════════
                ✅ Abbonamenti creati: $successCount
                ❌ Errori: $errorCount
                📊 Totale: ${subscriptions.size}
                ════════════════════════════════════════
            """.trimIndent())

            // Calcola statistiche
            val activeSubs = subscriptions.filter { it.isActive }
            val monthlyTotal = activeSubs.sumOf { it.getMonthlyCost() }
            val annualTotal = activeSubs.sumOf { it.getAnnualCost() }

            android.util.Log.d("SubscriptionTestData", """
                📊 RIEPILOGO COSTI:
                   Abbonamenti Attivi: ${activeSubs.size}
                   Costo Mensile: €${"%.2f".format(monthlyTotal)}
                   Costo Annuale: €${"%.2f".format(annualTotal)}
            """.trimIndent())
        }
    }

    /**
     * Restituisce un Timestamp X mesi fa
     */
    private fun getDateMonthsAgo(months: Int): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -months)
        return Timestamp(calendar.time)
    }

    /**
     * Restituisce un Timestamp X giorni da oggi (può essere negativo per date passate)
     */
    private fun getDateDaysFromNow(days: Int): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return Timestamp(calendar.time)
    }

    /**
     * Elimina tutti gli abbonamenti di test
     * ⚠️ ATTENZIONE: Questa funzione elimina TUTTI gli abbonamenti dell'utente
     */
    suspend fun deleteAllSubscriptions() {
        android.util.Log.w("SubscriptionTestData", "⚠️ ELIMINAZIONE TUTTI GLI ABBONAMENTI IN CORSO...")

        try {
            // Carica tutti gli abbonamenti
            firestoreRepository.getAllSubscriptions(activeOnly = false)
                .collect { subscriptions ->
                    subscriptions.forEach { subscription ->
                        firestoreRepository.deleteSubscription(subscription.id)
                            .onSuccess {
                                android.util.Log.d("SubscriptionTestData", "🗑️ Eliminato: ${subscription.name}")
                            }
                    }

                    android.util.Log.d("SubscriptionTestData", "✅ Eliminati ${subscriptions.size} abbonamenti")
                }
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionTestData", "❌ Errore eliminazione abbonamenti", e)
        }
    }

    /**
     * Stampa un riepilogo degli abbonamenti esistenti
     */
    suspend fun printSubscriptionsSummary() {
        try {
            firestoreRepository.getAllSubscriptions(activeOnly = false)
                .collect { subscriptions ->
                    android.util.Log.d("SubscriptionTestData", """
                        ════════════════════════════════════════
                        📊 ABBONAMENTI ESISTENTI (${subscriptions.size})
                        ════════════════════════════════════════
                    """.trimIndent())

                    subscriptions.sortedBy { it.name }.forEach { sub ->
                        val status = if (sub.isActive) "✅" else "❌"
                        val monthly = sub.getMonthlyCost()

                        android.util.Log.d("SubscriptionTestData", """
                            $status ${sub.name}
                               €${sub.amount} - ${sub.frequency}
                               Mensile: €${"%.2f".format(monthly)}
                               Prossimo: ${formatDate(sub.nextRenewalDate.toDate())}
                        """.trimIndent())
                    }

                    val active = subscriptions.filter { it.isActive }
                    val monthlyTotal = active.sumOf { it.getMonthlyCost() }
                    val annualTotal = active.sumOf { it.getAnnualCost() }

                    android.util.Log.d("SubscriptionTestData", """
                        ════════════════════════════════════════
                        💰 TOTALI (solo attivi):
                           Mensile: €${"%.2f".format(monthlyTotal)}
                           Annuale: €${"%.2f".format(annualTotal)}
                        ════════════════════════════════════════
                    """.trimIndent())
                }
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionTestData", "❌ Errore stampa riepilogo", e)
        }
    }

    /**
     * Formatta una data
     */
    private fun formatDate(date: Date): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ITALIAN)
        return sdf.format(date)
    }
}

/**
 * Extension function per usare facilmente nei Fragment
 */
fun androidx.fragment.app.Fragment.createTestSubscriptions() {
    SubscriptionTestData.createSampleSubscriptions()
}