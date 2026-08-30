package com.android.argosapp.ui.component

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

fun showErrorFeedback(
    context: Context,
    message: String,
    backgroundColor: Int,
    textColor: Int,
) {
    val density = context.resources.displayMetrics.density
    val toastView = TextView(context).apply {
        text = message
        setTextColor(textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        maxLines = 2
        setPadding(
            (16 * density).toInt(),
            (10 * density).toInt(),
            (16 * density).toInt(),
            (10 * density).toInt(),
        )
        background = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 24 * density
        }
    }

    Toast(context).apply {
        duration = Toast.LENGTH_LONG
        view = toastView
        setGravity(
            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            0,
            (64 * density).toInt(),
        )
    }.show()

    vibrateOnError(context)
}

private fun vibrateOnError(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE),
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(180L)
    }
}
