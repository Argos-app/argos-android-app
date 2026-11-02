package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
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
            binding.lotHeadCount.text = context.getString(
                R.string.lots_card_head_count,
                lot.headCount?.toString() ?: context.getString(R.string.home_calendar_unknown_value),
            )
            binding.lotOpenDate.text = context.getString(
                R.string.lots_card_open_date,
                lot.openDate?.format(DATE_FORMATTER)
                    ?: context.getString(R.string.home_calendar_unknown_value),
            )
            binding.lotCategory.text = context.getString(
                R.string.lots_card_category,
                lot.category ?: context.getString(R.string.home_calendar_unknown_value),
            )
            binding.lotSlaughterDate.text = context.getString(
                R.string.lots_card_release_date,
                lot.releaseSlaughterDate?.format(DATE_FORMATTER)
                    ?: context.getString(R.string.home_calendar_unknown_value),
            )
            binding.lotTreatmentsTitle.text = context.resources.getQuantityString(
                R.plurals.lots_treatments_count,
                lot.animalExceptions.size,
                lot.animalExceptions.size,
            )
            binding.lotTreatmentsDescription.setText(
                if (lot.animalExceptions.isEmpty()) R.string.lots_exceptions_empty
                else R.string.lots_treatments_description,
            )
            binding.lotTreatmentsRows.removeAllViews()
            val density = context.resources.displayMetrics.density
            fun dp(value: Int) = (value * density + 0.5f).toInt()
            lot.animalExceptions.forEach { animal ->
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, dp(8), 0, dp(8))
                }
                val icon = ImageView(context).apply {
                    setImageResource(R.drawable.cow_icon)
                    layoutParams = LinearLayout.LayoutParams(dp(24), dp(20)).apply { marginEnd = dp(8) }
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                val details = TextView(context).apply {
                    text = context.getString(
                        R.string.lots_treatment_row,
                        animal.earCode ?: context.getString(R.string.home_calendar_unknown_value),
                        animal.medicine ?: context.getString(R.string.home_calendar_unknown_value),
                        animal.releaseSlaughterDate?.format(DATE_FORMATTER)
                            ?: context.getString(R.string.home_calendar_unknown_value),
                    )
                    setTextColor(context.getColor(R.color.argos_text_secondary))
                    textSize = 10f
                    typeface = ResourcesCompat.getFont(context, R.font.dm_sans)
                    setLineSpacing(dp(1).toFloat(), 1f)
                }
                row.addView(icon)
                row.addView(details)
                binding.lotTreatmentsRows.addView(row)
                binding.lotTreatmentsRows.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
                    setBackgroundColor(context.getColor(R.color.argos_divider))
                })
            }
        }

        private companion object {
            val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
        }
    }
}
