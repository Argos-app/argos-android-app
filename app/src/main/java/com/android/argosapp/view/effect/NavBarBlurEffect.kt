package com.android.argosapp.view.effect

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.core.widget.NestedScrollView
import com.android.argosapp.R

class NavBarBlurEffect(
    private val source: View,
    private val backdrop: ImageView,
) {
    private var bitmap: Bitmap? = null
    private var refreshScheduled = false
    private val refreshAction = Runnable {
        refreshScheduled = false
        updateBlur()
    }

    fun render() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val radius = 16f * backdrop.resources.displayMetrics.density
            backdrop.setRenderEffect(
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP),
            )
        }
        bindSource()
    }

    fun bindSource() {
        source.findViewById<NestedScrollView>(R.id.homeContentScroll)
            ?.setOnScrollChangeListener { _, _, _, _, _ -> scheduleRefresh() }
        scheduleRefresh()
    }

    fun clear() {
        backdrop.removeCallbacks(refreshAction)
        refreshScheduled = false
        backdrop.setImageDrawable(null)
        bitmap?.recycle()
        bitmap = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            backdrop.setRenderEffect(null)
        }
    }

    private fun scheduleRefresh() {
        if (refreshScheduled) return
        refreshScheduled = true
        backdrop.postOnAnimation(refreshAction)
    }

    private fun updateBlur() {
        if (source.width == 0 || source.height == 0 || backdrop.width == 0 || backdrop.height == 0) {
            return
        }

        val targetBitmap = bitmap
            ?.takeIf { it.width == backdrop.width && it.height == backdrop.height }
            ?: Bitmap.createBitmap(backdrop.width, backdrop.height, Bitmap.Config.ARGB_8888).also {
                bitmap?.recycle()
                bitmap = it
                backdrop.setImageBitmap(it)
            }
        targetBitmap.eraseColor(Color.TRANSPARENT)

        val sourceLocation = IntArray(2)
        val backdropLocation = IntArray(2)
        source.getLocationOnScreen(sourceLocation)
        backdrop.getLocationOnScreen(backdropLocation)

        val canvas = Canvas(targetBitmap)
        canvas.translate(
            (sourceLocation[0] - backdropLocation[0]).toFloat(),
            (sourceLocation[1] - backdropLocation[1]).toFloat(),
        )
        source.draw(canvas)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val radius = (12f * backdrop.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            blurBitmap(targetBitmap, radius)
        }
        backdrop.invalidate()
    }

    private fun blurBitmap(bitmap: Bitmap, radius: Int) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        val intermediate = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        blurAxis(pixels, intermediate, width, height, radius, horizontal = true)
        blurAxis(intermediate, pixels, width, height, radius, horizontal = false)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun blurAxis(
        source: IntArray,
        destination: IntArray,
        width: Int,
        height: Int,
        radius: Int,
        horizontal: Boolean,
    ) {
        val lineLength = if (horizontal) width else height
        val lineCount = if (horizontal) height else width
        val windowSize = radius * 2 + 1

        fun index(line: Int, position: Int): Int =
            if (horizontal) line * width + position else position * width + line

        for (line in 0 until lineCount) {
            var alpha = 0
            var red = 0
            var green = 0
            var blue = 0

            for (offset in -radius..radius) {
                val color = source[index(line, offset.coerceIn(0, lineLength - 1))]
                alpha += Color.alpha(color)
                red += Color.red(color)
                green += Color.green(color)
                blue += Color.blue(color)
            }

            for (position in 0 until lineLength) {
                destination[index(line, position)] = Color.argb(
                    alpha / windowSize,
                    red / windowSize,
                    green / windowSize,
                    blue / windowSize,
                )

                val removed = source[index(line, (position - radius).coerceIn(0, lineLength - 1))]
                val added = source[index(line, (position + radius + 1).coerceIn(0, lineLength - 1))]
                alpha += Color.alpha(added) - Color.alpha(removed)
                red += Color.red(added) - Color.red(removed)
                green += Color.green(added) - Color.green(removed)
                blue += Color.blue(added) - Color.blue(removed)
            }
        }
    }
}
