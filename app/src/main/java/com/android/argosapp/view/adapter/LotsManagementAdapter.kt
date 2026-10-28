package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.databinding.ItemLotBinding
import com.android.argosapp.model.Lot
import java.time.format.DateTimeFormatter
import java.util.Locale

class LotsManagementAdapter : RecyclerView.Adapter<LotsManagementAdapter.LotViewHolder>() {
    private var lots: List<Lot> = emptyList()

    fun submitList(lots: List<Lot>) {
        this.lots = lots
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LotViewHolder =
        LotViewHolder(ItemLotBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: LotViewHolder, position: Int) = holder.bind(lots[position])

    override fun getItemCount(): Int = lots.size

    class LotViewHolder(private val binding: ItemLotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lot: Lot) {
            val context = binding.root.context
            binding.lotName.text = lot.lotName
            binding.lotCategory.text = context.getString(
                R.string.lots_card_category,
                lot.category ?: context.getString(R.string.home_calendar_unknown_value),
            )
            binding.lotSlaughterDate.text = context.getString(
                R.string.lots_card_release_date,
                lot.releaseSlaughterDate?.format(DATE_FORMATTER)
                    ?: context.getString(R.string.home_calendar_unknown_value),
            )
        }

        private companion object {
            val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
        }
    }
}
