package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.databinding.ItemMedicationStockBinding
import com.android.argosapp.model.MedicationStockItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MedicationStockAdapter : ListAdapter<MedicationStockItem, MedicationStockAdapter.Holder>(Diff) {
    private val expandedItems = mutableSetOf<String>()

    class Holder(private val binding: ItemMedicationStockBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MedicationStockItem, expanded: Boolean) {
            val context = binding.root.context
            val unknown = context.getString(R.string.stock_unknown_value)
            binding.medicationName.text = item.medicineName
            binding.summaryExpirationDate.text = context.getString(
                R.string.stock_expiration_date,
                item.expiryDate?.let(::formatDate) ?: unknown,
            )
            binding.summaryQuantity.text = context.getString(R.string.stock_quantity, item.quantity ?: unknown)
            binding.supplier.text = context.getString(R.string.stock_supplier_value, item.supplier ?: unknown)
            binding.manufactureDate.text = context.getString(
                R.string.stock_manufacture_value,
                item.manufactureDate?.let(::formatDate) ?: unknown,
            )
            binding.expirationDate.text = context.getString(
                R.string.stock_expiration_date,
                item.expiryDate?.let(::formatDate) ?: unknown,
            )
            binding.dose.text = context.getString(R.string.stock_dose_value, item.dose ?: unknown)
            binding.quantity.text = context.getString(R.string.stock_quantity, item.quantity ?: unknown)
            binding.withdrawalDays.text = item.withdrawalDays?.let {
                context.resources.getQuantityString(R.plurals.stock_withdrawal_days, it, it)
            } ?: context.getString(R.string.stock_withdrawal_days_unknown)
            binding.batch.text = context.getString(R.string.stock_batch_value, item.batch ?: unknown)
            binding.activeIngredient.text = context.getString(
                R.string.stock_active_ingredient_value,
                item.activeIngredient?.takeIf(String::isNotBlank) ?: unknown,
            )
            binding.stockCategory.text = item.therapeuticCategory?.takeIf(String::isNotBlank) ?: unknown
            binding.summaryRow.visibility = if (expanded) View.GONE else View.VISIBLE
            binding.detailsContainer.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.expandIcon.rotation = if (expanded) 180f else 0f
        }

        private fun formatDate(value: String): String =
            runCatching { LocalDate.parse(value).format(DATE_FORMAT) }.getOrDefault(value)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemMedicationStockBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        holder.bind(item, item.id in expandedItems)
        holder.itemView.setOnClickListener {
            if (!expandedItems.add(item.id)) expandedItems.remove(item.id)
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) notifyItemChanged(currentPosition)
        }
    }

    private object Diff : DiffUtil.ItemCallback<MedicationStockItem>() {
        override fun areItemsTheSame(old: MedicationStockItem, new: MedicationStockItem) = old.id == new.id
        override fun areContentsTheSame(old: MedicationStockItem, new: MedicationStockItem) = old == new
    }

    companion object { private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy") }
}
