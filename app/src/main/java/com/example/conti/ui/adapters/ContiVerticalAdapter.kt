package com.example.conti.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conti.databinding.ItemContoVerticalBinding
import com.example.conti.models.Account
import com.example.conti.models.AccountType
import com.example.conti.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter per visualizzare i conti come card verticali.
 */
class ContiVerticalAdapter(
    private val onAccountClick: (Account) -> Unit
) : ListAdapter<Account, ContiVerticalAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemContoVerticalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountViewHolder(binding, onAccountClick)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AccountViewHolder(
        private val binding: ItemContoVerticalBinding,
        private val onAccountClick: (Account) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: Account) {
            binding.apply {
                // Nome e tipo conto
                tvNomeConto.text = account.accountName
                tvTipoConto.text = account.accountType.displayName

                // Icona in base al tipo di conto
                tvIconaConto.text = when (account.accountType) {
                    AccountType.BUDDYBANK -> "🏦"
                    AccountType.HYPE -> "💳"
                    AccountType.OTHER -> "💰"
                }

                // Saldo
                tvSaldo.text = account.getFormattedBalance()

                // Colore saldo
                val saldoColor = if (account.balance >= 0) {
                    android.graphics.Color.parseColor("#4CAF50") // Verde
                } else {
                    android.graphics.Color.parseColor("#F44336") // Rosso
                }
                tvSaldo.setTextColor(saldoColor)

                // IBAN (mostra solo se presente)
                if (!account.iban.isNullOrBlank()) {
                    tvIban.visibility = View.VISIBLE
                    tvIban.text = formatIban(account.iban)
                } else {
                    tvIban.visibility = View.GONE
                }

                // Ultimo aggiornamento
                if (account.lastUpdated != null) {
                    val date = account.lastUpdated.toDate()
                    tvUltimoAggiornamento.text = "Aggiornato ${formatLastUpdate(date)}"
                } else {
                    tvUltimoAggiornamento.text = "Mai aggiornato"
                }

                // Click listener
                root.setOnClickListener {
                    onAccountClick(account)
                }
            }
        }

        /**
         * Formatta IBAN con spazi ogni 4 caratteri
         */
        private fun formatIban(iban: String): String {
            return iban.chunked(4).joinToString(" ")
        }

        /**
         * Formatta la data dell'ultimo aggiornamento in modo user-friendly
         */
        private fun formatLastUpdate(date: Date): String {
            val now = Calendar.getInstance()
            val updateTime = Calendar.getInstance().apply { time = date }

            val diffMillis = now.timeInMillis - updateTime.timeInMillis
            val diffMinutes = diffMillis / (60 * 1000)
            val diffHours = diffMillis / (60 * 60 * 1000)
            val diffDays = diffMillis / (24 * 60 * 60 * 1000)

            return when {
                diffMinutes < 1 -> "pochi secondi fa"
                diffMinutes < 60 -> "$diffMinutes minuti fa"
                diffHours < 24 -> {
                    if (diffHours == 1L) "1 ora fa" else "$diffHours ore fa"
                }
                diffDays < 7 -> {
                    if (diffDays == 1L) "ieri" else "$diffDays giorni fa"
                }
                else -> {
                    val format = SimpleDateFormat("dd/MM/yyyy 'alle' HH:mm", Locale.ITALIAN)
                    format.format(date)
                }
            }
        }
    }

    class AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem.accountId == newItem.accountId
        }

        override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem == newItem
        }
    }
}