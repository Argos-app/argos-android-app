package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.databinding.ItemMedicationStockBinding
import com.android.argosapp.model.MedicationStockItem
import java.time.format.DateTimeFormatter

class MedicationStockAdapter : ListAdapter<MedicationStockItem, MedicationStockAdapter.Holder>(Diff) {
    private var expandedItemId: String? = null

    inner class Holder(private val binding: ItemMedicationStockBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MedicationStockItem) {
            val expanded = item.id == expandedItemId
            val context = binding.root.context
            binding.medicationName.text = item.medicationName
            binding.expirationDate.text = context.getString(
                R.string.stock_expiration_date,
                item.expirationDate.format(DATE_FORMAT),
            )
            binding.quantity.text = context.getString(R.string.stock_quantity, item.quantity)
            binding.detailsText.text = context.getString(
                R.string.stock_details_format,
                item.supplierName.takeIf(String::isNotBlank) ?: context.getString(R.string.stock_detail_not_informed),
                item.batchNumber ?: context.getString(R.string.stock_detail_not_informed),
                item.manufactureDate?.format(DATE_FORMAT) ?: context.getString(R.string.stock_detail_not_informed),
                item.activeIngredient ?: context.getString(R.string.stock_detail_not_informed),
                item.dose?.stripTrailingZeros()?.toPlainString()?.let { dose ->
                    listOfNotNull(dose, item.doseUnit?.takeIf(String::isNotBlank)).joinToString(" ")
                } ?: context.getString(R.string.stock_detail_not_informed),
                item.withdrawalDays?.let { days ->
                    if (days == 1) context.getString(R.string.stock_detail_withdrawal_day_one)
                    else context.getString(R.string.stock_detail_withdrawal_days, days)
                } ?: context.getString(R.string.stock_detail_not_informed),
                item.therapeuticCategory ?: context.getString(R.string.stock_detail_not_informed),
            )
            binding.detailsContainer.isVisible = expanded
            binding.expandIcon.rotation = if (expanded) 180f else 0f
            binding.stockCard.contentDescription = context.getString(
                if (expanded) R.string.stock_card_accessibility_collapse else R.string.stock_card_accessibility_expand,
                item.medicationName,
                if (expanded) binding.detailsText.text else "",
            )
            binding.stockCard.setOnClickListener { toggleExpanded(item.id) }
        }
    }

    fun submitItems(items: List<MedicationStockItem>, onCommitted: (() -> Unit)? = null) {
        if (expandedItemId != null && items.none { it.id == expandedItemId }) expandedItemId = null
        submitList(items, Runnable { onCommitted?.invoke() })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemMedicationStockBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private fun toggleExpanded(itemId: String) {
        val previousId = expandedItemId
        expandedItemId = if (previousId == itemId) null else itemId
        listOfNotNull(previousId, itemId).distinct().forEach { changedId ->
            val position = currentList.indexOfFirst { it.id == changedId }
            if (position >= 0) notifyItemChanged(position)
        }
    }

    private object Diff : DiffUtil.ItemCallback<MedicationStockItem>() {
        override fun areItemsTheSame(old: MedicationStockItem, new: MedicationStockItem) = old.id == new.id
        override fun areContentsTheSame(old: MedicationStockItem, new: MedicationStockItem) = old == new
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
