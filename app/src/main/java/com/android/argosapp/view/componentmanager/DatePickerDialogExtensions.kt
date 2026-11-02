package com.android.argosapp.view.componentmanager

import android.app.DatePickerDialog
import android.view.ViewGroup

fun DatePickerDialog.showCalendarSized() {
    show()
    window?.decorView?.post {
        val resources = context.resources
        val tablet = resources.configuration.screenWidthDp >= 600
        val width = if (tablet) {
            (360 * resources.displayMetrics.density).toInt()
        } else {
            ViewGroup.LayoutParams.MATCH_PARENT
        }
        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
