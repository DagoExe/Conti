package com.example.conti.data.repository

import com.example.conti.models.Subscription
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Estensioni per FirestoreRepository - Operazioni CRUD complete sugli abbonamenti
 *
 * Questi metodi completano la gestione degli abbonamenti aggiungendo:
 * - Aggiornamento abbonamenti esistenti
 * - Disattivazione/Riattivazione
 * - Eliminazione
 * - Aggiornamento date di rinnovo
 */

/**
 * Aggiorna un abbonamento esistente
 */
suspend fun FirestoreRepository.updateSubscription(subscription: Subscription): Result<Unit> {
    return try {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Utente non autenticato")

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("subscriptions")
            .document(subscription.id)
            .set(
                subscription.copy(lastUpdated = Timestamp.now()),
                SetOptions.merge()
            )
            .await()

        android.util.Log.d("FirestoreRepository", "✅ Abbonamento aggiornato: ${subscription.id}")
        Result.success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("FirestoreRepository", "❌ Errore aggiornamento abbonamento", e)
        Result.failure(e)
    }
}

/**
 * Disattiva un abbonamento (lo marca come inattivo senza eliminarlo)
 */
suspend fun FirestoreRepository.deactivateSubscription(subscriptionId: String): Result<Unit> {
    return try {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Utente non autenticato")

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("subscriptions")
            .document(subscriptionId)
            .update(
                mapOf(
                    "isActive" to false,
                    "lastUpdated" to Timestamp.now()
                )
            )
            .await()

        android.util.Log.d("FirestoreRepository", "✅ Abbonamento disattivato: $subscriptionId")
        Result.success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("FirestoreRepository", "❌ Errore disattivazione abbonamento", e)
        Result.failure(e)
    }
}

/**
 * Riattiva un abbonamento precedentemente disattivato
 */
suspend fun FirestoreRepository.reactivateSubscription(subscriptionId: String): Result<Unit> {
    return try {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Utente non autenticato")

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("subscriptions")
            .document(subscriptionId)
            .update(
                mapOf(
                    "isActive" to true,
                    "lastUpdated" to Timestamp.now()
                )
            )
            .await()

        android.util.Log.d("FirestoreRepository", "✅ Abbonamento riattivato: $subscriptionId")
        Result.success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("FirestoreRepository", "❌ Errore riattivazione abbonamento", e)
        Result.failure(e)
    }
}

/**
 * Elimina definitivamente un abbonamento
 */
suspend fun FirestoreRepository.deleteSubscription(subscriptionId: String): Result<Unit> {
    return try {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Utente non autenticato")

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("subscriptions")
            .document(subscriptionId)
            .delete()
            .await()

        android.util.Log.d("FirestoreRepository", "✅ Abbonamento eliminato: $subscriptionId")
        Result.success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("FirestoreRepository", "❌ Errore eliminazione abbonamento", e)
        Result.failure(e)
    }
}

/**
 * Aggiorna la data del prossimo rinnovo di un abbonamento
 *
 * Utile per processare i rinnovi automatici:
 * - Quando la data di rinnovo è passata
 * - Per creare una transazione di pagamento
 * - Per calcolare la prossima data di rinnovo
 */
suspend fun FirestoreRepository.updateNextRenewal(
    subscriptionId: String,
    newRenewalDate: Timestamp
): Result<Unit> {
    return try {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Utente non autenticato")

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("subscriptions")
            .document(subscriptionId)
            .update(
                mapOf(
                    "nextRenewalDate" to newRenewalDate,
                    "lastUpdated" to Timestamp.now()
                )
            )
            .await()

        android.util.Log.d("FirestoreRepository", "✅ Data rinnovo aggiornata per: $subscriptionId")
        Result.success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("FirestoreRepository", "❌ Errore aggiornamento data rinnovo", e)
        Result.failure(e)
    }
}

/**
 * Ottiene un singolo abbonamento per ID
 */
suspend fun FirestoreRepository.getSubscription(subscriptionId: String): Result<Subscription> {
    return try {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Utente non autenticato")

        val snapshot = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("subscriptions")
            .document(subscriptionId)
            .get()
            .await()

        val subscription = snapshot.toObject(Subscription::class.java)
            ?: throw Exception("Abbonamento non trovato")

        Result.success(subscription)
    } catch (e: Exception) {
        android.util.Log.e("FirestoreRepository", "❌ Errore caricamento abbonamento", e)
        Result.failure(e)
    }
}

/**
 * Ottiene tutti gli abbonamenti in scadenza nei prossimi N giorni
 *
 * Utile per:
 * - Notifiche di scadenza imminente
 * - Dashboard riepilogativa
 * - Promemoria di pagamento
 */
suspend fun FirestoreRepository.getExpiringSubscriptions(daysThreshold: Int = 7): Result<List<Subscription>> {
    return try {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Utente non autenticato")

        // Calcola la data limite
        val now = java.util.Calendar.getInstance()
        val futureDate = java.util.Calendar.getInstance()
        futureDate.add(java.util.Calendar.DAY_OF_YEAR, daysThreshold)

        val snapshot = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("subscriptions")
            .whereEqualTo("isActive", true)
            .whereGreaterThanOrEqualTo("nextRenewalDate", Timestamp(now.time))
            .whereLessThanOrEqualTo("nextRenewalDate", Timestamp(futureDate.time))
            .get()
            .await()

        val subscriptions = snapshot.documents.mapNotNull {
            it.toObject(Subscription::class.java)
        }

        android.util.Log.d("FirestoreRepository", "✅ Trovati ${subscriptions.size} abbonamenti in scadenza")
        Result.success(subscriptions)
    } catch (e: Exception) {
        android.util.Log.e("FirestoreRepository", "❌ Errore caricamento abbonamenti in scadenza", e)
        Result.failure(e)
    }
}

/**
 * Processa i rinnovi automatici per un abbonamento
 *
 * Questo metodo:
 * 1. Crea una transazione di pagamento
 * 2. Aggiorna la data del prossimo rinnovo
 * 3. Aggiorna il saldo del conto
 *
 * Può essere chiamato:
 * - Manualmente dall'utente
 * - Automaticamente da un worker in background
 * - Su notifica/promemoria
 */
suspend fun FirestoreRepository.processSubscriptionRenewal(subscription: Subscription): Result<Unit> {
    return try {
        // 1. Crea transazione di pagamento
        val transaction = com.example.conti.models.Transaction(
            accountId = subscription.accountId,
            amount = -subscription.amount, // Negativo perché è un'uscita
            description = "Rinnovo ${subscription.name}",
            category = subscription.category,
            notes = "Pagamento automatico abbonamento",
            date = Timestamp.now(),
            type = "expense",
            isRecurring = true,
            subscriptionId = subscription.id
        )

        addTransaction(transaction).getOrThrow()

        // 2. Calcola prossima data di rinnovo
        val calendar = java.util.Calendar.getInstance()
        calendar.time = subscription.nextRenewalDate.toDate()

        when (subscription.frequency) {
            "MONTHLY" -> calendar.add(java.util.Calendar.MONTH, 1)
            "QUARTERLY" -> calendar.add(java.util.Calendar.MONTH, 3)
            "SEMIANNUAL" -> calendar.add(java.util.Calendar.MONTH, 6)
            "ANNUAL" -> calendar.add(java.util.Calendar.YEAR, 1)
        }

        val nextRenewal = Timestamp(calendar.time)

        // 3. Aggiorna abbonamento
        updateNextRenewal(subscription.id, nextRenewal).getOrThrow()

        android.util.Log.d("FirestoreRepository", "✅ Rinnovo processato per: ${subscription.name}")
        Result.success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("FirestoreRepository", "❌ Errore processamento rinnovo", e)
        Result.failure(e)
    }
}