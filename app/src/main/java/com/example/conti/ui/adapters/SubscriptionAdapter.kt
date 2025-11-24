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

/**
 * Adapter per visualizzare la lista degli abbonamenti.
 *
 * Features:
 * - Badge per frequenza (Mensile, Annuale, ecc.)
 * - Indicatore scadenza imminente
 * - Costo mensile ed equivalente
 * - Associazione con conto
 */
class SubscriptionAdapter(
    private val onSubscriptionClick: (Subscription) -> Unit,
    private val onSubscriptionLongClick: (Subscription) -> Unit = {},
    private val accountsMap: Map<String, Account> = emptyMap()
) : ListAdapter<Subscription, SubscriptionAdapter.SubscriptionViewHolder>(SubscriptionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val binding = ItemSubscriptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubscriptionViewHolder(binding, onSubscriptionClick, onSubscriptionLongClick, accountsMap)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Aggiorna la mappa degli account
     */
    fun updateAccountsMap(accounts: Map<String, Account>) {
        // Questo metodo può essere chiamato quando la lista degli account cambia
        // Per ora non implementato, ma può essere utile in futuro
    }

    class SubscriptionViewHolder(
        private val binding: ItemSubscriptionBinding,
        private val onSubscriptionClick: (Subscription) -> Unit,
        private val onSubscriptionLongClick: (Subscription) -> Unit,
        private val accountsMap: Map<String, Account>
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subscription: Subscription) {
            binding.apply {
                // Nome e descrizione
                tvNomeAbbonamento.text = subscription.name

                if (!subscription.description.isNullOrBlank()) {
                    tvDescrizioneAbbonamento.visibility = View.VISIBLE
                    tvDescrizioneAbbonamento.text = subscription.description
                } else {
                    tvDescrizioneAbbonamento.visibility = View.GONE
                }

                // Icona in base al nome
                tvIconaAbbonamento.text = getSubscriptionIcon(subscription.name)

                // Badge frequenza
                val frequencyText = when (subscription.frequency) {
                    "MONTHLY" -> "📅 Mensile"
                    "QUARTERLY" -> "📅 Trimestrale"
                    "SEMIANNUAL" -> "📅 Semestrale"
                    "ANNUAL" -> "📅 Annuale"
                    else -> "📅 Mensile"
                }
                tvFrequenza.text = frequencyText

                // Costo
                tvCostoAbbonamento.text = CurrencyUtils.formatImporto(subscription.amount)

                // Costo mensile equivalente (se non è già mensile)
                if (subscription.frequency != "MONTHLY") {
                    tvCostoMensile.visibility = View.VISIBLE
                    tvCostoMensile.text = "${CurrencyUtils.formatImporto(subscription.getMonthlyCost())}/mese"
                } else {
                    tvCostoMensile.visibility = View.GONE
                }

                // Data prossimo rinnovo
                val nextRenewal = subscription.nextRenewalDate.toDate()
                tvProssimoRinnovo.text = "Rinnovo: ${formatDate(nextRenewal)}"

                // Badge scadenza imminente (entro 7 giorni)
                if (isExpiringSoon(nextRenewal)) {
                    badgeScadenza.visibility = View.VISIBLE
                    badgeScadenza.text = "⚠️ In scadenza"
                } else {
                    badgeScadenza.visibility = View.GONE
                }

                // Conto associato
                val account = accountsMap[subscription.accountId]
                if (account != null) {
                    tvContoAssociato.visibility = View.VISIBLE
                    tvContoAssociato.text = "💳 ${account.accountName}"
                } else {
                    tvContoAssociato.visibility = View.VISIBLE
                    tvContoAssociato.text = "💳 Conto non trovato"
                }

                // Indicatore attivo/inattivo
                if (subscription.isActive) {
                    indicatorActive.setCardBackgroundColor(
                        android.graphics.Color.parseColor("#4CAF50") // Verde
                    )
                    tvNomeAbbonamento.alpha = 1.0f
                } else {
                    indicatorActive.setCardBackgroundColor(
                        android.graphics.Color.parseColor("#757575") // Grigio
                    )
                    tvNomeAbbonamento.alpha = 0.5f
                }

                // Click listeners
                root.setOnClickListener {
                    onSubscriptionClick(subscription)
                }

                root.setOnLongClickListener {
                    onSubscriptionLongClick(subscription)
                    true
                }
            }
        }

        /**
         * Formatta la data in modo user-friendly
         */
        private fun formatDate(date: Date): String {
            val now = Calendar.getInstance()
            val renewalDate = Calendar.getInstance().apply { time = date }

            val diffMillis = renewalDate.timeInMillis - now.timeInMillis
            val diffDays = (diffMillis / (24 * 60 * 60 * 1000)).toInt()

            return when {
                // Scaduto
                diffDays < 0 -> "Scaduto"
                // Oggi
                diffDays == 0 -> "Oggi"
                // Domani
                diffDays == 1 -> "Domani"
                // Questa settimana
                diffDays in 2..7 -> "Tra $diffDays giorni"
                // Questo mese
                now.get(Calendar.YEAR) == renewalDate.get(Calendar.YEAR) &&
                        now.get(Calendar.MONTH) == renewalDate.get(Calendar.MONTH) -> {
                    SimpleDateFormat("dd MMM", Locale.ITALIAN).format(date)
                }
                // Altro
                else -> SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(date)
            }
        }

        /**
         * Verifica se un abbonamento è in scadenza nei prossimi 7 giorni
         */
        private fun isExpiringSoon(renewalDate: Date): Boolean {
            val now = Calendar.getInstance()
            val renewal = Calendar.getInstance().apply { time = renewalDate }

            val diffMillis = renewal.timeInMillis - now.timeInMillis
            val diffDays = diffMillis / (24 * 60 * 60 * 1000)

            return diffDays in 0..7
        }

        /**
         * Restituisce un'icona in base al nome dell'abbonamento
         */
        private fun getSubscriptionIcon(name: String): String {
            val nameLower = name.lowercase()
            return when {
                nameLower.contains("netflix") -> "🎬"
                nameLower.contains("spotify") -> "🎵"
                nameLower.contains("disney") -> "🏰"
                nameLower.contains("amazon") || nameLower.contains("prime") -> "📦"
                nameLower.contains("youtube") -> "▶️"
                nameLower.contains("apple") -> "🍎"
                nameLower.contains("microsoft") || nameLower.contains("office") -> "💻"
                nameLower.contains("xbox") || nameLower.contains("playstation") || nameLower.contains("nintendo") -> "🎮"
                nameLower.contains("gym") || nameLower.contains("palestra") -> "💪"
                nameLower.contains("telefono") || nameLower.contains("mobile") -> "📱"
                nameLower.contains("internet") || nameLower.contains("fibra") -> "🌐"
                nameLower.contains("cloud") || nameLower.contains("storage") -> "☁️"
                nameLower.contains("giornale") || nameLower.contains("news") -> "📰"
                else -> "📅"
            }
        }
    }

    class SubscriptionDiffCallback : DiffUtil.ItemCallback<Subscription>() {
        override fun areItemsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            return oldItem == newItem
        }
    }
}