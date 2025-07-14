package io.miniapp.core.openplatform.miniapp.ui.views

import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextUtils.TruncateAt
import android.text.style.ReplacementSpan
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import java.lang.reflect.Constructor
import kotlin.math.max


object StaticLayoutEx {
    private const val TEXT_DIR_CLASS = "android.text.TextDirectionHeuristic"
    private const val TEXT_DIRS_CLASS = "android.text.TextDirectionHeuristics"
    private const val TEXT_DIR_FIRSTSTRONG_LTR = "FIRSTSTRONG_LTR"
    private var initialized = false
    private var sConstructor: Constructor<StaticLayout>? = null
    private lateinit var sConstructorArgs: Array<Any?>
    private var sTextDirection: Any? = null
    var alignments = Layout.Alignment.values()
    fun ALIGN_RIGHT(): Layout.Alignment {
        return if (alignments.size >= 5) alignments[4] else Layout.Alignment.ALIGN_OPPOSITE
    }

    fun ALIGN_LEFT(): Layout.Alignment {
        return if (alignments.size >= 5) alignments[3] else Layout.Alignment.ALIGN_NORMAL
    }

    fun init() {
        if (initialized) {
            return
        }
        try {
            val textDirClass: Class<*>
            if (Build.VERSION.SDK_INT >= 18) {
                textDirClass = TextDirectionHeuristic::class.java
                sTextDirection = TextDirectionHeuristics.FIRSTSTRONG_LTR
            } else {
                val loader = StaticLayoutEx::class.java.getClassLoader()
                textDirClass = loader.loadClass(TEXT_DIR_CLASS)
                val textDirsClass = loader.loadClass(TEXT_DIRS_CLASS)
                sTextDirection = textDirsClass.getField(TEXT_DIR_FIRSTSTRONG_LTR)[textDirsClass]
            }
            val signature = arrayOf(
                CharSequence::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                TextPaint::class.java,
                Int::class.javaPrimitiveType,
                Layout.Alignment::class.java,
                textDirClass,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                TruncateAt::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            sConstructor = StaticLayout::class.java.getDeclaredConstructor(*signature)
            sConstructor?.isAccessible = true
            sConstructorArgs = arrayOfNulls(signature.size)
            initialized = true
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun createStaticLayout2(
        source: CharSequence,
        paint: TextPaint?,
        width: Int,
        align: Layout.Alignment?,
        spacingmult: Float,
        spacingadd: Float,
        includepad: Boolean,
        ellipsize: TruncateAt?,
        ellipsisWidth: Int,
        maxLines: Int
    ): StaticLayout? {
        return if (Build.VERSION.SDK_INT >= 23) {
            val builder = StaticLayout.Builder.obtain(
                source, 0, source.length,
                paint!!, ellipsisWidth
            )
                .setAlignment(align!!)
                .setLineSpacing(spacingadd, spacingmult)
                .setIncludePad(includepad)
                .setEllipsize(TruncateAt.END)
                .setEllipsizedWidth(ellipsisWidth)
                .setMaxLines(maxLines)
                .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
            builder.build()
        } else {
            createStaticLayout(
                source,
                paint,
                width,
                align,
                spacingmult,
                spacingadd,
                includepad,
                ellipsize,
                ellipsisWidth,
                maxLines,
                true
            )
        }
    }

    fun createStaticLayout(
        source: CharSequence,
        paint: TextPaint?,
        width: Int,
        align: Layout.Alignment?,
        spacingmult: Float,
        spacingadd: Float,
        includepad: Boolean,
        ellipsize: TruncateAt?,
        ellipsisWidth: Int,
        maxLines: Int
    ): StaticLayout? {
        return createStaticLayout(
            source,
            paint,
            width,
            align,
            spacingmult,
            spacingadd,
            includepad,
            ellipsize,
            ellipsisWidth,
            maxLines,
            true
        )
    }

    fun createStaticLayout(
        source: CharSequence,
        paint: TextPaint?,
        outerWidth: Int,
        align: Layout.Alignment?,
        spacingMult: Float,
        spacingAdd: Float,
        includePad: Boolean,
        ellipsize: TruncateAt?,
        ellipsisWidth: Int,
        maxLines: Int,
        canContainUrl: Boolean
    ): StaticLayout? {
        /*if (Build.VERSION.SDK_INT >= 14) {
            init();
            try {
                sConstructorArgs[0] = source;
                sConstructorArgs[1] = bufstart;
                sConstructorArgs[2] = bufend;
                sConstructorArgs[3] = paint;
                sConstructorArgs[4] = outerWidth;
                sConstructorArgs[5] = align;
                sConstructorArgs[6] = sTextDirection;
                sConstructorArgs[7] = spacingMult;
                sConstructorArgs[8] = spacingAdd;
                sConstructorArgs[9] = includePad;
                sConstructorArgs[10] = ellipsize;
                sConstructorArgs[11] = ellipsisWidth;
                sConstructorArgs[12] = maxLines;
                return sConstructor.newInstance(sConstructorArgs);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }*/
        var source = source
        try {
            return if (maxLines == 1) {
                val index = TextUtils.indexOf(source, "\n") - 1
                if (index > 0) {
                    source =
                        SpannableStringBuilder.valueOf(source.subSequence(0, index)).append("…")
                }
                val text =
                    TextUtils.ellipsize(source, paint, ellipsisWidth.toFloat(), TruncateAt.END)
                StaticLayout(
                    text,
                    0,
                    text.length,
                    paint,
                    outerWidth,
                    align,
                    spacingMult,
                    spacingAdd,
                    includePad
                )
            } else {
                var layout: StaticLayout
                if (Build.VERSION.SDK_INT >= 23) {
                    var builder = StaticLayout.Builder.obtain(
                        source, 0, source.length,
                        paint!!, outerWidth
                    )
                        .setAlignment(align!!)
                        .setLineSpacing(spacingAdd, spacingMult)
                        .setIncludePad(includePad)
                        .setEllipsize(null)
                        .setEllipsizedWidth(ellipsisWidth)
                        .setMaxLines(maxLines)
                        .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
                        .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                    layout = builder.build()
                    var realWidthLarger = false
                    for (l in 0 until layout.lineCount) {
                        if (layout.getLineRight(l) > outerWidth) {
                            realWidthLarger = true
                            break
                        }
                    }
                    if (realWidthLarger) {
                        builder = StaticLayout.Builder.obtain(
                            source, 0, source.length,
                            paint, outerWidth
                        )
                            .setAlignment(align)
                            .setLineSpacing(spacingAdd, spacingMult)
                            .setIncludePad(includePad)
                            .setEllipsize(null)
                            .setEllipsizedWidth(ellipsisWidth)
                            .setMaxLines(maxLines)
                            .setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE)
                            .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                        layout = builder.build()
                    }
                } else {
                    layout = StaticLayout(
                        source,
                        paint,
                        outerWidth,
                        align,
                        spacingMult,
                        spacingAdd,
                        includePad
                    )
                }
                if (layout.lineCount <= maxLines) {
                    layout
                } else {
                    var off: Int
                    var start: Int
                    val left = layout.getLineLeft(maxLines - 1)
                    val lineWidth = layout.getLineWidth(maxLines - 1)
                    off = if (left != 0f) {
                        layout.getOffsetForHorizontal(maxLines - 1, left)
                    } else {
                        layout.getOffsetForHorizontal(maxLines - 1, lineWidth)
                    }
                    if (lineWidth < ellipsisWidth - AndroidUtils.dp(10)) {
                        off += 3
                    }
                    val stringBuilder = SpannableStringBuilder(
                        source.subSequence(
                            0,
                            max(0.0, (off - 3).toDouble()).toInt()
                        )
                    )
                    stringBuilder.append("\u2026")
                    if (Build.VERSION.SDK_INT >= 23) {
                        val builder = StaticLayout.Builder.obtain(
                            stringBuilder, 0, stringBuilder.length,
                            paint!!, outerWidth
                        )
                            .setAlignment(align!!)
                            .setLineSpacing(spacingAdd, spacingMult)
                            .setIncludePad(includePad)
                            .setEllipsize(
                                if (stringBuilder.getSpans(
                                        0, stringBuilder.length,
                                        ReplacementSpan::class.java
                                    ).isNotEmpty()
                                ) null else ellipsize
                            )
                            .setEllipsizedWidth(ellipsisWidth)
                            .setMaxLines(maxLines)
                            .setBreakStrategy(if (canContainUrl) LineBreaker.BREAK_STRATEGY_HIGH_QUALITY else LineBreaker.BREAK_STRATEGY_SIMPLE)
                            .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                        builder.build()
                    } else {
                        StaticLayout(
                            stringBuilder,
                            paint,
                            outerWidth,
                            align,
                            spacingMult,
                            spacingAdd,
                            includePad
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
