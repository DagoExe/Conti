package com.example.conti.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conti.databinding.ItemContoBinding
import com.example.conti.models.Account
import com.example.conti.utils.CurrencyUtils

/**
 * Adapter per la lista di account nella Home
 */
class HomeAccountAdapter(
    private val onAccountClick: (Account) -> Unit
) : ListAdapter<Account, HomeAccountAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemContoBinding.inflate(
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
        private val binding: ItemContoBinding,
        private val onAccountClick: (Account) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: Account) {
            binding.tvNomeConto.text = account.accountName
            binding.tvIstituto.text = account.accountType.displayName
            binding.tvSaldo.text = account.getFormattedBalance()

            // Colore del saldo
            val color = CurrencyUtils.getColoreImporto(account.balance)
            binding.tvSaldo.setTextColor(android.graphics.Color.parseColor(color))

            // Nascondi badge Excel per ora
            binding.badgeExcel.visibility = android.view.View.GONE

            // Click listener
            binding.root.setOnClickListener {
                onAccountClick(account)
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