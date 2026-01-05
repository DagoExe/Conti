package com.example.conti.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conti.databinding.ItemSubscriptionBinding
import com.example.conti.models.Account
import com.example.conti.models.Subscription
import com.example.conti.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Adapter per la RecyclerView degli abbonamenti.
 *
 * ✅ Features Complete:
 * - Icone automatiche per 30+ servizi noti (Netflix, Spotify, Disney+, ecc.)
 * - Badge frequenza con icona (📅 Mensile, Trimestrale, Semestrale, Annuale)
 * - Badge scadenza per rinnovi imminenti entro 7 giorni (⚠️ In scadenza)
 * - Costo mensile equivalente per abbonamenti non mensili
 * - Indicatore visivo attivo/inattivo (barra laterale colorata verde/grigio)
 * - Formattazione date user-friendly (Oggi, Domani, Tra X giorni, data completa)
 * - Click listener per dettagli
 * - Long-click listener per menu opzioni
 * - DiffUtil per performance ottimali
 * - Aggiornamento dinamico accountsMap con updateAccounts()
 * - Opacity ridotta per abbonamenti inattivi (0.6f)
 * - Supporto descrizione opzionale
 *
 * Usage:
 * ```kotlin
 * val adapter = SubscriptionAdapter(
 *     onSubscriptionClick = { subscription -> showDetails(subscription) },
 *     onSubscriptionLongClick = { subscription -> showOptions(subscription) },
 *     accountsMap = emptyMap() // Inizialmente vuoto
 * )
 *
 * // Aggiorna accounts quando disponibili
 * adapter.updateAccounts(accountMap)
 * ```
 */
class SubscriptionAdapter(
    private val onSubscriptionClick: (Subscription) -> Unit,
    private val onSubscriptionLongClick: (Subscription) -> Unit,
    private var accountsMap: Map<String, Account> // ⚠️ var per permettere aggiornamenti
) : ListAdapter<Subscription, SubscriptionAdapter.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TAG = "SubscriptionAdapter"
        private const val EXPIRY_THRESHOLD_DAYS = 7
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubscriptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subscription = getItem(position)
        holder.bind(subscription, accountsMap)
    }

    /**
     * ✅ Aggiorna dinamicamente la mappa degli accounts
     *
     * Chiamare questo metodo quando gli accounts vengono caricati o aggiornati
     * per aggiornare la visualizzazione dei nomi conti negli items.
     */
    fun updateAccounts(newAccountsMap: Map<String, Account>) {
        android.util.Log.d(TAG, "🔄 Aggiornamento accountsMap: ${newAccountsMap.size} conti")
        accountsMap = newAccountsMap
        notifyDataSetChanged() // Forza refresh degli items
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VIEW HOLDER
    // ═══════════════════════════════════════════════════════════════════════

    inner class ViewHolder(
        private val binding: ItemSubscriptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subscription: Subscription, accounts: Map<String, Account>) {
            // Log binding per debug
            android.util.Log.d(TAG, "📋 Binding: ${subscription.name} (${if (subscription.isActive) "ATTIVO" else "INATTIVO"})")

            // ═══════════════════════════════════════════════════════════════
            // INDICATORE ATTIVO/INATTIVO (barra laterale)
            // ═══════════════════════════════════════════════════════════════

            binding.indicatorActive.setCardBackgroundColor(
                if (subscription.isActive) {
                    binding.root.context.getColor(android.R.color.holo_green_light)
                } else {
                    binding.root.context.getColor(android.R.color.darker_gray)
                }
            )

            // ═══════════════════════════════════════════════════════════════
            // ICONA SERVIZIO (emoji automatica basata su nome)
            // ═══════════════════════════════════════════════════════════════

            val icon = getSubscriptionIcon(subscription.name)
            binding.tvIconaAbbonamento.text = icon

            // ═══════════════════════════════════════════════════════════════
            // NOME E DESCRIZIONE
            // ═══════════════════════════════════════════════════════════════

            binding.tvNomeAbbonamento.text = subscription.name

            // Mostra descrizione solo se presente
            if (!subscription.description.isNullOrBlank()) {
                binding.tvDescrizioneAbbonamento.visibility = View.VISIBLE
                binding.tvDescrizioneAbbonamento.text = subscription.description
            } else {
                binding.tvDescrizioneAbbonamento.visibility = View.GONE
            }

            // ═══════════════════════════════════════════════════════════════
            // COSTI
            // ═══════════════════════════════════════════════════════════════

            // Costo principale (importo abbonamento)
            binding.tvCostoAbbonamento.text = CurrencyUtils.formatImporto(subscription.amount)

            // Costo mensile equivalente (solo se non mensile)
            if (subscription.frequency != "MONTHLY") {
                binding.tvCostoMensile.visibility = View.VISIBLE
                val monthlyCost = subscription.getMonthlyCost()
                binding.tvCostoMensile.text = "${CurrencyUtils.formatImporto(monthlyCost)}/mese"
            } else {
                binding.tvCostoMensile.visibility = View.GONE
            }

            // ═══════════════════════════════════════════════════════════════
            // BADGE FREQUENZA
            // ═══════════════════════════════════════════════════════════════

            val frequencyText = when (subscription.frequency) {
                "MONTHLY" -> "📅 Mensile"
                "QUARTERLY" -> "📅 Trimestrale"
                "SEMIANNUAL" -> "📅 Semestrale"
                "ANNUAL" -> "📅 Annuale"
                else -> "📅 Mensile"
            }
            binding.tvFrequenza.text = frequencyText

            // ═══════════════════════════════════════════════════════════════
            // BADGE SCADENZA (se attivo e in scadenza entro 7 giorni)
            // ═══════════════════════════════════════════════════════════════

            if (subscription.isActive && isExpiringSoon(subscription)) {
                binding.badgeScadenza.visibility = View.VISIBLE
                binding.badgeScadenza.text = "⚠️ In scadenza"

                android.util.Log.d(TAG, "   ⚠️ ${subscription.name} in scadenza!")
            } else {
                binding.badgeScadenza.visibility = View.GONE
            }

            // ═══════════════════════════════════════════════════════════════
            // DATA PROSSIMO RINNOVO (formattazione user-friendly)
            // ═══════════════════════════════════════════════════════════════

            val dateText = formatRenewalDate(subscription.nextRenewalDate.toDate())
            binding.tvProssimoRinnovo.text = "Prossimo rinnovo: $dateText"

            // ═══════════════════════════════════════════════════════════════
            // CONTO ASSOCIATO
            // ═══════════════════════════════════════════════════════════════

            val account = accounts[subscription.accountId]
            if (account != null) {
                binding.tvContoAssociato.text = "💳 ${account.accountName}"
                binding.tvContoAssociato.visibility = View.VISIBLE
            } else {
                // Account non trovato (possibile se accountsMap non ancora popolato)
                binding.tvContoAssociato.text = "💳 Conto: ${subscription.accountId}"
                binding.tvContoAssociato.visibility = View.VISIBLE

                android.util.Log.w(TAG, "   ⚠️ Account non trovato: ${subscription.accountId}")
            }

            // ═══════════════════════════════════════════════════════════════
            // CLICK LISTENERS
            // ═══════════════════════════════════════════════════════════════

            // Click normale - Mostra dettagli
            binding.root.setOnClickListener {
                android.util.Log.d(TAG, "🖱️ Click su: ${subscription.name}")
                onSubscriptionClick(subscription)
            }

            // Long click - Mostra menu opzioni
            binding.root.setOnLongClickListener {
                android.util.Log.d(TAG, "🖱️ Long click su: ${subscription.name}")
                onSubscriptionLongClick(subscription)
                true // Consuma l'evento
            }

            // ═══════════════════════════════════════════════════════════════
            // OPACITY SE INATTIVO
            // ═══════════════════════════════════════════════════════════════

            binding.root.alpha = if (subscription.isActive) 1.0f else 0.6f
        }

        // ═══════════════════════════════════════════════════════════════════
        // HELPER - Icona automatica per servizio
        // ═══════════════════════════════════════════════════════════════════

        /**
         * Restituisce l'emoji appropriata basata sul nome del servizio.
         *
         * Supporta 30+ servizi comuni con icone specifiche.
         * Fallback a 📅 per servizi sconosciuti.
         */
        private fun getSubscriptionIcon(name: String): String {
            val nameLower = name.lowercase()

            return when {
                // ═══ STREAMING VIDEO ═══
                nameLower.contains("netflix") -> "🎬"
                nameLower.contains("disney") -> "🏰"
                nameLower.contains("prime") || nameLower.contains("amazon") -> "📦"
                nameLower.contains("hbo") -> "🎭"
                nameLower.contains("apple tv") -> "🍎"
                nameLower.contains("paramount") -> "⭐"
                nameLower.contains("peacock") -> "🦚"
                nameLower.contains("hulu") -> "🟢"
                nameLower.contains("sky") -> "📺"
                nameLower.contains("dazn") -> "⚽"
                nameLower.contains("now tv") -> "📺"

                // ═══ STREAMING AUDIO ═══
                nameLower.contains("spotify") -> "🎵"
                nameLower.contains("youtube") && nameLower.contains("music") -> "▶️"
                nameLower.contains("youtube") && nameLower.contains("premium") -> "▶️"
                nameLower.contains("apple music") -> "🎧"
                nameLower.contains("deezer") -> "🎶"
                nameLower.contains("tidal") -> "🌊"
                nameLower.contains("audible") -> "📚"
                nameLower.contains("podcast") -> "🎙️"

                // ═══ GAMING ═══
                nameLower.contains("xbox") || nameLower.contains("game pass") -> "🎮"
                nameLower.contains("playstation") || nameLower.contains("ps plus") || nameLower.contains("ps+") -> "🎮"
                nameLower.contains("nintendo") || nameLower.contains("switch") -> "🎮"
                nameLower.contains("steam") -> "🎮"
                nameLower.contains("epic games") -> "🎮"
                nameLower.contains("ea play") -> "🎮"
                nameLower.contains("ubisoft") || nameLower.contains("uplay") -> "🎮"

                // ═══ CLOUD & STORAGE ═══
                nameLower.contains("icloud") -> "☁️"
                nameLower.contains("google drive") || nameLower.contains("google one") -> "☁️"
                nameLower.contains("dropbox") -> "☁️"
                nameLower.contains("onedrive") -> "☁️"
                nameLower.contains("mega") -> "☁️"

                // ═══ PRODUTTIVITÀ ═══
                nameLower.contains("microsoft 365") || nameLower.contains("office") -> "💻"
                nameLower.contains("adobe") -> "🎨"
                nameLower.contains("notion") -> "📝"
                nameLower.contains("evernote") -> "📓"
                nameLower.contains("canva") -> "🎨"
                nameLower.contains("grammarly") -> "✍️"
                nameLower.contains("figma") -> "🎨"
                nameLower.contains("github") -> "💻"

                // ═══ TELEFONIA & INTERNET ═══
                nameLower.contains("iliad") || nameLower.contains("tim") || nameLower.contains("vodafone") ||
                        nameLower.contains("wind") || nameLower.contains("tre") || nameLower.contains("mobile") ||
                        nameLower.contains("fastweb") || nameLower.contains("ho mobile") -> "📱"

                nameLower.contains("fibra") || nameLower.contains("internet") || nameLower.contains("wifi") ||
                        nameLower.contains("adsl") -> "🌐"

                // ═══ FITNESS & BENESSERE ═══
                nameLower.contains("palestra") || nameLower.contains("gym") || nameLower.contains("fitness") -> "💪"
                nameLower.contains("yoga") -> "🧘"
                nameLower.contains("peloton") -> "🚴"
                nameLower.contains("strava") -> "🏃"
                nameLower.contains("myfitnesspal") -> "💪"
                nameLower.contains("headspace") || nameLower.contains("calm") -> "🧘"

                // ═══ NEWS & INFORMAZIONE ═══
                nameLower.contains("giornale") || nameLower.contains("newspaper") ||
                        nameLower.contains("times") || nameLower.contains("post") ||
                        nameLower.contains("corriere") || nameLower.contains("repubblica") -> "📰"

                // ═══ FOOD & DELIVERY ═══
                nameLower.contains("deliveroo") || nameLower.contains("glovo") ||
                        nameLower.contains("just eat") || nameLower.contains("uber eats") -> "🍕"

                // ═══ TRASPORTI ═══
                nameLower.contains("uber") || nameLower.contains("lyft") ||
                        nameLower.contains("free now") -> "🚗"
                nameLower.contains("trenitalia") || nameLower.contains("italo") -> "🚄"

                // ═══ ALTRO ═══
                nameLower.contains("insurance") || nameLower.contains("assicurazione") -> "🛡️"
                nameLower.contains("bank") || nameLower.contains("banca") -> "🏦"
                nameLower.contains("vpn") -> "🔒"
                nameLower.contains("antivirus") || nameLower.contains("norton") ||
                        nameLower.contains("mcafee") || nameLower.contains("kaspersky") -> "🛡️"
                nameLower.contains("linkedin") || nameLower.contains("premium") -> "💼"

                // ═══ DEFAULT ═══
                else -> "📅"
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // HELPER - Verifica scadenza imminente
        // ═══════════════════════════════════════════════════════════════════

        /**
         * Verifica se l'abbonamento è in scadenza entro X giorni.
         *
         * @param subscription Abbonamento da verificare
         * @param daysThreshold Soglia giorni (default 7)
         * @return true se scade entro la soglia, false altrimenti
         */
        private fun isExpiringSoon(
            subscription: Subscription,
            daysThreshold: Int = EXPIRY_THRESHOLD_DAYS
        ): Boolean {
            val now = Calendar.getInstance()
            val renewal = Calendar.getInstance()
            renewal.time = subscription.nextRenewalDate.toDate()

            val diffMillis = renewal.timeInMillis - now.timeInMillis
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

            return diffDays in 0..daysThreshold
        }

        // ═══════════════════════════════════════════════════════════════════
        // HELPER - Formattazione data user-friendly
        // ═══════════════════════════════════════════════════════════════════

        /**
         * Formatta la data in modo user-friendly:
         * - Oggi
         * - Domani
         * - Tra X giorni (2-7 giorni)
         * - Scaduto ieri / Scaduto X giorni fa
         * - Data completa (oltre 7 giorni)
         *
         * @param date Data da formattare
         * @return Stringa formattata
         */
        private fun formatRenewalDate(date: Date): String {
            val now = Calendar.getInstance()
            val renewalDate = Calendar.getInstance()
            renewalDate.time = date

            // Resetta ore per confronto solo date
            now.set(Calendar.HOUR_OF_DAY, 0)
            now.set(Calendar.MINUTE, 0)
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)

            renewalDate.set(Calendar.HOUR_OF_DAY, 0)
            renewalDate.set(Calendar.MINUTE, 0)
            renewalDate.set(Calendar.SECOND, 0)
            renewalDate.set(Calendar.MILLISECOND, 0)

            // Calcola differenza in giorni
            val diffMillis = renewalDate.timeInMillis - now.timeInMillis
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

            return when {
                diffDays == 0L -> {
                    // Oggi
                    "Oggi"
                }
                diffDays == 1L -> {
                    // Domani
                    "Domani"
                }
                diffDays in 2..7 -> {
                    // Tra X giorni
                    "Tra $diffDays giorni"
                }
                diffDays == -1L -> {
                    // Scaduto ieri
                    "Scaduto ieri"
                }
                diffDays < -1 -> {
                    // Scaduto X giorni fa
                    val absDays = kotlin.math.abs(diffDays)
                    "Scaduto $absDays giorni fa"
                }
                else -> {
                    // Data completa (oltre 7 giorni)
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN)
                    sdf.format(date)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DIFF CALLBACK - Per performance ottimali
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * DiffUtil.ItemCallback per calcolare differenze tra liste
     * in modo efficiente e aggiornare solo gli items cambiati.
     */
    private class DiffCallback : DiffUtil.ItemCallback<Subscription>() {

        /**
         * Confronta ID per capire se sono lo stesso item.
         * Se true, RecyclerView usa lo stesso ViewHolder.
         */
        override fun areItemsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Confronta tutti i campi per capire se il contenuto è cambiato.
         * Se false, RecyclerView chiama onBindViewHolder per aggiornare la UI.
         */
        override fun areContentsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            // Data class Subscription genera automaticamente equals()
            return oldItem == newItem
        }
    }
}