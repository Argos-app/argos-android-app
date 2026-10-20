package com.android.argosapp.view.adapter

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.model.MapAddressSuggestion

class MapAddressSuggestionAdapter(
    private val onSuggestionClick: (MapAddressSuggestion) -> Unit,
) : RecyclerView.Adapter<MapAddressSuggestionAdapter.SuggestionViewHolder>() {
    private val suggestions = mutableListOf<MapAddressSuggestion>()

    fun replaceSuggestions(items: List<MapAddressSuggestion>) {
        if (suggestions == items) return
        suggestions.clear()
        suggestions.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val context = parent.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))
            isClickable = true
            isFocusable = true
            val selectableBackground = TypedValue()
            if (context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    selectableBackground,
                    true,
                )
            ) {
                foreground = ContextCompat.getDrawable(context, selectableBackground.resourceId)
            }
        }
        val primary = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.argos_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val secondary = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.argos_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        row.addView(primary, LinearLayout.LayoutParams(-1, -2))
        row.addView(secondary, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(context, 2)
        })
        val divider = View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.argos_divider))
        }
        row.addView(divider, LinearLayout.LayoutParams(-1, dp(context, 1)).apply {
            topMargin = dp(context, 8)
        })
        return SuggestionViewHolder(row, primary, secondary, divider)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.bind(suggestion, position != suggestions.lastIndex)
        holder.itemView.setOnClickListener { onSuggestionClick(suggestion) }
    }

    override fun getItemCount() = suggestions.size

    class SuggestionViewHolder(
        view: View,
        private val primary: TextView,
        private val secondary: TextView,
        private val divider: View,
    ) : RecyclerView.ViewHolder(view) {
        fun bind(suggestion: MapAddressSuggestion, showDivider: Boolean) {
            val context = itemView.context
            val font = ResourcesCompat.getFont(context, R.font.dm_sans)

            primary.text = suggestion.primaryText
            primary.typeface = Typeface.create(font, Typeface.BOLD)
            secondary.text = suggestion.secondaryText
            secondary.typeface = font
            divider.visibility = if (showDivider) View.VISIBLE else View.GONE
        }
    }

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density).toInt()
}
