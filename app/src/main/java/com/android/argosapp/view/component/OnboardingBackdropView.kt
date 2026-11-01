package com.android.argosapp.view.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.android.argosapp.R

/** Draws the brand panel and its state-specific curved edge behind the onboarding artwork. */
class OnboardingBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
    }
    private val curve = Path()
    private var step = 0

    fun showStep(step: Int) {
        this.step = step.coerceIn(0, 2)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) return

        fillPaint.color = ContextCompat.getColor(context, R.color.argos_green)
        canvas.drawRect(0f, 0f, width, height, fillPaint)

        detailPaint.color = ContextCompat.getColor(context, R.color.argos_onboarding_decoration)
        drawDecorations(canvas, width, height)

        curve.reset()
        when (step) {
            0 -> {
                curve.moveTo(0f, height * 0.38f)
                curve.quadTo(width * 0.16f, height * 0.88f, width, height * 0.91f)
            }
            1 -> {
                curve.moveTo(0f, height * 0.91f)
                curve.quadTo(width * 0.52f, height * 1.02f, width, height * 0.9f)
            }
            else -> {
                curve.moveTo(0f, height * 0.91f)
                curve.quadTo(width * 0.82f, height * 0.81f, width, height * 0.36f)
            }
        }
        curve.lineTo(width, height)
        curve.lineTo(0f, height)
        curve.close()
        fillPaint.color = ContextCompat.getColor(context, R.color.argos_background)
        canvas.drawPath(curve, fillPaint)
    }

    private fun drawDecorations(canvas: Canvas, width: Float, height: Float) {
        val density = resources.displayMetrics.density
        val dotRadius = 1.5f * density
        for (row in 0..3) {
            for (column in 0..4) {
                canvas.drawCircle(20f * density + column * 6f * density, 22f * density + row * 6f * density, dotRadius, detailPaint)
            }
        }

        val circleRadius = 35f * density
        val circleCenterY = if (step == 1) height * 0.43f else height * 0.31f
        canvas.drawCircle(12f * density, circleCenterY, circleRadius, detailPaint)
        canvas.drawCircle(width - 10f * density, 18f * density, 44f * density, detailPaint)

        val plusX = width * 0.76f
        val plusY = height * 0.17f
        canvas.drawLine(plusX - 8f * density, plusY, plusX + 8f * density, plusY, detailPaint)
        canvas.drawLine(plusX, plusY - 8f * density, plusX, plusY + 8f * density, detailPaint)

        for (index in 0..3) {
            canvas.drawCircle(width * 0.84f + index * 5f * density, height * 0.73f, dotRadius, detailPaint)
        }
    }
}
