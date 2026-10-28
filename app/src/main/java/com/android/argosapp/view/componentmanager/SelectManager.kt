package com.android.argosapp.view.componentmanager

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.android.argosapp.R

class SelectManager {
    private var currentPopup: PopupWindow? = null

    class Dropdown internal constructor(
        val popup: PopupWindow,
        val list: ListView,
        val adapter: ArrayAdapter<String>,
    ) {
        fun appendOptions(options: List<String>) {
            val firstVisible = list.firstVisiblePosition
            val topOffset = list.getChildAt(0)?.top ?: 0
            adapter.addAll(options)
            list.setSelectionFromTop(firstVisible, topOffset)
        }
    }

    fun showOptions(
        anchor: View,
        options: List<String>,
        onSelected: (String) -> Unit,
        dropdownIcon: View? = null,
        footer: View? = null,
        footerHeightDp: Int = 48,
        onDismiss: (() -> Unit)? = null,
    ): Dropdown {
        dismiss()
        val context = anchor.context
        fun dp(size: Int) = (size * context.resources.displayMetrics.density).toInt()

        val list = ListView(context).apply {
            divider = ColorDrawable(context.getColor(R.color.argos_border))
            dividerHeight = dp(1)
            setPadding(0, dp(8), 0, dp(8))
            clipToPadding = false
            footer?.let { addFooterView(it, null, false) }
        }
        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, options.toMutableList()) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                (super.getView(position, convertView, parent) as TextView).apply {
                    typeface = ResourcesCompat.getFont(context, R.font.dm_sans)
                    textSize = 14f
                    setTextColor(context.getColor(R.color.argos_text_primary))
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    minHeight = dp(48)
                }
        }
        list.adapter = adapter

        val popup = PopupWindow(
            list,
            anchor.width,
            minOf(dp(options.size * 48 + 16 + if (footer != null) footerHeightDp else 0), dp(288), context.resources.displayMetrics.heightPixels / 2),
            true,
        ).apply {
            setBackgroundDrawable(GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), context.getColor(R.color.argos_border))
            })
            elevation = dp(8).toFloat()
            isOutsideTouchable = true
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        }
        popup.setOnDismissListener {
            if (currentPopup === popup) currentPopup = null
            dropdownIcon?.rotation = 0f
            onDismiss?.invoke()
        }
        list.setOnItemClickListener { _, _, position, _ ->
            if (position < adapter.count) {
                onSelected(adapter.getItem(position).orEmpty())
                popup.dismiss()
            }
        }
        currentPopup = popup
        val location = IntArray(2)
        anchor.getLocationInWindow(location)
        popup.showAtLocation(anchor, Gravity.TOP or Gravity.LEFT, location[0], location[1] + anchor.height + dp(6))
        dropdownIcon?.rotation = 180f
        return Dropdown(popup, list, adapter)
    }

    fun dismiss() {
        currentPopup?.dismiss()
        currentPopup = null
    }
}
