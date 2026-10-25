package com.danielmoly.launcher

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/** Icono de carpeta: fondo translúcido redondeado con hasta 4 miniaturas de sus apps. */
class FolderIcon(icons: List<Drawable>) : Drawable() {

    // Copias propias: cambiar los límites de un icono compartido descolocaría otras vistas.
    private val icons = icons.take(4).map { it.constantState?.newDrawable()?.mutate() ?: it }

    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF }
    private val rect = RectF()

    override fun draw(canvas: Canvas) {
        val b = bounds
        rect.set(b)
        val radius = b.width() * 0.25f
        canvas.drawRoundRect(rect, radius, radius, background)

        val padding = b.width() * 0.14f
        val gap = b.width() * 0.06f
        val cell = (b.width() - 2 * padding - gap) / 2
        icons.forEachIndexed { i, icon ->
            val left = b.left + padding + (i % 2) * (cell + gap)
            val top = b.top + padding + (i / 2) * (cell + gap)
            icon.setBounds(left.toInt(), top.toInt(), (left + cell).toInt(), (top + cell).toInt())
            icon.draw(canvas)
        }
    }

    override fun setAlpha(alpha: Int) {
        background.alpha = alpha
        icons.forEach { it.alpha = alpha }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        background.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
