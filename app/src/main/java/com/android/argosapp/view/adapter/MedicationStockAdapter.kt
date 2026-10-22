package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.databinding.ItemMedicationStockBinding
import com.android.argosapp.model.MedicationStockItem
import java.time.format.DateTimeFormatter

class MedicationStockAdapter : ListAdapter<MedicationStockItem, MedicationStockAdapter.Holder>(Diff) {
    class Holder(private val binding: ItemMedicationStockBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MedicationStockItem) {
            binding.medicationName.text = item.medicationName
            binding.expirationDate.text = binding.root.context.getString(R.string.stock_expiration_date, item.expirationDate.format(DATE_FORMAT))
            binding.quantity.text = binding.root.context.getString(R.string.stock_quantity, item.quantity)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemMedicationStockBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<MedicationStockItem>() {
        override fun areItemsTheSame(old: MedicationStockItem, new: MedicationStockItem) = old.id == new.id
        override fun areContentsTheSame(old: MedicationStockItem, new: MedicationStockItem) = old == new
    }
    companion object { private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy") }
}
