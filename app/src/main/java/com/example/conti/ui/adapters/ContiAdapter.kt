package com.example.conti.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conti.data.database.entities.Conto
import com.example.conti.databinding.ItemContoBinding
import com.example.conti.utils.CurrencyUtils

/**
 * Adapter per visualizzare la lista di conti in una RecyclerView.
 *
 * Utilizza ListAdapter con DiffUtil per aggiornamenti efficienti.
 */
class ContiAdapter(
    private val onContoClick: (Conto) -> Unit
) : ListAdapter<Conto, ContiAdapter.ContoViewHolder>(ContoDiffCallback()) {

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
        private val onContoClick: (Conto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conto: Conto) {
            binding.apply {
                // Nome e istituto
                tvNomeConto.text = conto.nome
                tvIstituto.text = conto.istituto

                // Saldo (solo saldo iniziale per ora, verrà aggiornato con i movimenti)
                tvSaldo.text = CurrencyUtils.formatImporto(conto.saldoIniziale)

                // Colore identificativo
                try {
                    viewColore.setBackgroundColor(Color.parseColor(conto.colore))
                } catch (e: Exception) {
                    viewColore.setBackgroundColor(Color.parseColor("#4CAF50"))
                }

                // Badge se sincronizzato da Excel
                if (conto.isFromExcel) {
                    badgeExcel.visibility = android.view.View.VISIBLE
                } else {
                    badgeExcel.visibility = android.view.View.GONE
                }

                // Click listener
                root.setOnClickListener { onContoClick(conto) }
            }
        }
    }

    /**
     * DiffUtil callback per confrontare i conti e aggiornare solo le differenze.
     */
    private class ContoDiffCallback : DiffUtil.ItemCallback<Conto>() {
        override fun areItemsTheSame(oldItem: Conto, newItem: Conto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conto, newItem: Conto): Boolean {
            return oldItem == newItem
        }
    }
}