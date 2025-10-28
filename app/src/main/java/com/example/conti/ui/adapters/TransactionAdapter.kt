package com.example.conti.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conti.databinding.ItemTransactionBinding
import com.example.conti.models.Transaction
import com.example.conti.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter per visualizzare le transazioni/movimenti.
 */
class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding, onTransactionClick)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private val onTransactionClick: (Transaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                // Descrizione
                tvDescrizione.text = transaction.description

                // Categoria
                tvCategoria.text = getCategoryIcon(transaction.category) + " " + transaction.category

                // Data
                tvData.text = formatDate(transaction.date.toDate())

                // Importo
                val isIncome = transaction.amount >= 0
                tvImporto.text = CurrencyUtils.formatImporto(kotlin.math.abs(transaction.amount))

                // Colore e icona in base al tipo
                if (isIncome) {
                    tvImporto.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    tvTipoMovimento.text = "⬆️"
                    tvTipoMovimento.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                } else {
                    tvImporto.setTextColor(android.graphics.Color.parseColor("#F44336"))
                    tvTipoMovimento.text = "⬇️"
                    tvTipoMovimento.setTextColor(android.graphics.Color.parseColor("#F44336"))
                }

                // Click listener
                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }

        /**
         * Formatta la data in modo user-friendly
         */
        private fun formatDate(date: Date): String {
            val now = Calendar.getInstance()
            val transactionDate = Calendar.getInstance().apply { time = date }

            return when {
                // Oggi
                now.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == transactionDate.get(Calendar.DAY_OF_YEAR) -> {
                    "Oggi, ${SimpleDateFormat("HH:mm", Locale.ITALIAN).format(date)}"
                }
                // Ieri
                now.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) - 1 == transactionDate.get(Calendar.DAY_OF_YEAR) -> {
                    "Ieri, ${SimpleDateFormat("HH:mm", Locale.ITALIAN).format(date)}"
                }
                // Questa settimana
                now.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                        now.get(Calendar.WEEK_OF_YEAR) == transactionDate.get(Calendar.WEEK_OF_YEAR) -> {
                    SimpleDateFormat("EEEE, HH:mm", Locale.ITALIAN).format(date)
                }
                // Altro
                else -> {
                    SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(date)
                }
            }
        }

        /**
         * Restituisce un'icona in base alla categoria
         */
        private fun getCategoryIcon(category: String): String {
            return when (category.lowercase()) {
                "stipendio" -> "💰"
                "abbonamento" -> "📅"
                "spesa" -> "🛒"
                "ristorante" -> "🍽️"
                "trasporti" -> "🚗"
                "benzina" -> "⛽"
                "bollette" -> "💡"
                "affitto" -> "🏠"
                "salute" -> "🏥"
                "intrattenimento" -> "🎬"
                "shopping" -> "🛍️"
                "risparmio" -> "🐷"
                "investimenti" -> "📈"
                "bonifico" -> "💸"
                "prelievo" -> "🏧"
                else -> "💳"
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}