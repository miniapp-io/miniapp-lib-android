package io.miniapp.core.openplatform.miniapp.ui.views

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.text.style.ReplacementSpan
internal class FixedWidthSpan(private val width: Int) : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: FontMetricsInt?
    ): Int {
        var fm = fm
        if (fm == null) {
            fm = paint.getFontMetricsInt()
        }
        if (fm != null) {
            val h = fm.descent - fm.ascent
            fm.descent = 1 - h
            fm.bottom = fm.descent
            fm.ascent = -1
            fm.top = fm.ascent
        }
        return width
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
    }
}
