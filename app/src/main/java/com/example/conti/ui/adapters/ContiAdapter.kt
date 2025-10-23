package com.example.conti.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conti.models.Account
import com.example.conti.databinding.ItemContoBinding
import com.example.conti.utils.CurrencyUtils

/**
 * Adapter per visualizzare la lista di conti in una RecyclerView.
 *
 * Utilizza ListAdapter con DiffUtil per aggiornamenti efficienti.
 * Versione Firestore - usa Account invece di Conto.
 */
class ContiAdapter(
    private val onContoClick: (Account) -> Unit
) : ListAdapter<Account, ContiAdapter.ContoViewHolder>(ContoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContoViewHolder {
        val binding = ItemContoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContoViewHolder(binding, onContoClick)
    }

    override fun onBindViewHolder(holder: ContoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder per un singolo conto.
     */
    class ContoViewHolder(
        private val binding: ItemContoBinding,
        private val onContoClick: (Account) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: Account) {
            binding.apply {
                // Nome e istituto bancario
                tvNomeConto.text = account.name
                tvIstituto.text = account.bankName

                // Saldo corrente
                tvSaldo.text = CurrencyUtils.formatImporto(account.balance)

                // Colore identificativo
                try {
                    viewColore.setBackgroundColor(Color.parseColor(account.color))
                } catch (e: Exception) {
                    viewColore.setBackgroundColor(Color.parseColor("#4CAF50"))
                }

                // Badge se sincronizzato da Excel
                if (account.isFromExcel) {
                    badgeExcel.visibility = android.view.View.VISIBLE
                } else {
                    badgeExcel.visibility = android.view.View.GONE
                }

                // Click listener
                root.setOnClickListener { onContoClick(account) }
            }
        }
    }

    /**
     * DiffUtil callback per confrontare i conti e aggiornare solo le differenze.
     */
    private class ContoDiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem == newItem
        }
    }
}