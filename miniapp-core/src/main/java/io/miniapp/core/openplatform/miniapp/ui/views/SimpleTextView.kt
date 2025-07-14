package io.miniapp.core.openplatform.miniapp.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Region
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal class SimpleTextView(context: Context) : View(context), Drawable.Callback {
    private var layout: Layout? = null
    private var firstLineLayout: Layout? = null
    private var fullLayout: Layout? = null
    private var partLayout: Layout? = null
    private val textPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var gravity = Gravity.LEFT or Gravity.TOP
    private var maxLines = 1
    private var text: CharSequence? = null
    private val spannableStringBuilder: SpannableStringBuilder? = null
    var leftDrawable: Drawable? = null
        private set
    var rightDrawable: Drawable? = null
        private set
    private var rightDrawable2: Drawable? = null
    private var replacedDrawable: Drawable? = null
    private var replacedText: String? = null
    private var replacingDrawableTextIndex = 0
    private var replacingDrawableTextOffset = 0f
    private var rightDrawableScale = 1.0f
    private var drawablePadding: Int = AndroidUtils.dp(4)
    private var leftDrawableTopPadding = 0
    private var rightDrawableTopPadding = 0
    private var buildFullLayout = false
    private var fullAlpha = 0f
    private var widthWrapContent = false
    private var wrapBackgroundDrawable: Drawable? = null
    private var scrollNonFitText = false
    private var textDoesNotFit = false
    private var scrollingOffset = 0f
    private var lastUpdateTime: Long = 0
    private var currentScrollDelay = 0
    private var fadePaint: Paint? = null
    private var fadePaintBack: Paint? = null
    private var fadeEllpsizePaint: Paint? = null
    private var fadeEllpsizePaintWidth = 0
    private var lastWidth = 0
    private var offsetX = 0
    private var offsetY = 0
    private var textWidth = 0
    private var totalWidth = 0
    var textHeight = 0
        private set
    var rightDrawableX = 0
    var rightDrawableY = 0
    private var wasLayout = false
    var rightDrawableOutside = false
    private var rightDrawableInside = false
    private var ellipsizeByGradient = false
    private var ellipsizeByGradientLeft = false
    private var forceEllipsizeByGradientLeft: Boolean? = null
    private var ellipsizeByGradientWidthDp = 16
    private var paddingRight = 0
    private var minWidth = 0
    private var fullLayoutAdditionalWidth = 0
    private var fullLayoutLeftOffset = 0
    private var fullLayoutLeftCharactersOffset = 0f
    private var minusWidth = 0
    private var fullTextMaxLines = 3
    private val path = Path()
    private var canHideRightDrawable = false
    private var rightDrawableHidden = false
    private var rightDrawableOnClickListener: OnClickListener? = null
    private var maybeClick = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var attachedToWindow = false
    private var mAlignment = Layout.Alignment.ALIGN_NORMAL

    init {
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)
    }

    fun setContentColor(color: Int) {
        textPaint.setColor(color)
        invalidate()
    }

    fun setLinkTextColor(color: Int) {
        textPaint.linkColor = color
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attachedToWindow = false
        wasLayout = false
    }

    fun setTextSize(sizeInDp: Int) {
        val newSize: Int = AndroidUtils.dp(sizeInDp)
        if (newSize.toFloat() == textPaint.textSize) {
            return
        }
        textPaint.textSize = newSize.toFloat()
        if (!recreateLayoutMaybe()) {
            invalidate()
        }
    }

    fun setBuildFullLayout(value: Boolean) {
        buildFullLayout = value
    }

    fun setFullAlpha(value: Float) {
        fullAlpha = value
        invalidate()
    }

    fun getFullAlpha(): Float {
        return fullAlpha
    }

    fun setScrollNonFitText(value: Boolean) {
        if (scrollNonFitText == value) {
            return
        }
        scrollNonFitText = value
        updateFadePaints()
        requestLayout()
    }

    fun setEllipsizeByGradient(value: Boolean) {
        setEllipsizeByGradient(value, null)
    }

    fun setEllipsizeByGradient(value: Int) {
        setEllipsizeByGradient(value, null)
    }

    fun setEllipsizeByGradient(value: Boolean, forceLeft: Boolean?) {
        if (scrollNonFitText == value) {
            return
        }
        ellipsizeByGradient = value
        forceEllipsizeByGradientLeft = forceLeft
        updateFadePaints()
    }

    fun setEllipsizeByGradient(value: Int, forceLeft: Boolean?) {
        setEllipsizeByGradient(true, forceLeft)
        ellipsizeByGradientWidthDp = value
        updateFadePaints()
    }

    fun setWidthWrapContent(value: Boolean) {
        widthWrapContent = value
    }

    private fun updateFadePaints() {
        if ((fadePaint == null || fadePaintBack == null) && scrollNonFitText) {
            fadePaint = Paint()
            fadePaint!!.setShader(
                LinearGradient(
                    0f,
                    0f,
                    AndroidUtils.dp(6).toFloat(),
                    0f,
                    intArrayOf(-0x1, 0),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            )
            fadePaint!!.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
            fadePaintBack = Paint()
            fadePaintBack!!.setShader(
                LinearGradient(
                    0f,
                    0f,
                    AndroidUtils.dp(6).toFloat(),
                    0f,
                    intArrayOf(0, -0x1),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            )
            fadePaintBack!!.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
        }
        val ellipsizeLeft: Boolean = forceEllipsizeByGradientLeft ?: false
        //            ellipsizeLeft = getAlignment() == Layout.Alignment.ALIGN_NORMAL && LocaleController.isRTL || getAlignment() == Layout.Alignment.ALIGN_OPPOSITE && !LocaleController.isRTL;
        if ((fadeEllpsizePaint == null || fadeEllpsizePaintWidth != AndroidUtils.dp(
                ellipsizeByGradientWidthDp
            ) || ellipsizeByGradientLeft != ellipsizeLeft) && ellipsizeByGradient
        ) {
            if (fadeEllpsizePaint == null) {
                fadeEllpsizePaint = Paint()
            }
            ellipsizeByGradientLeft = ellipsizeLeft
            if (ellipsizeByGradientLeft) {
                fadeEllpsizePaint!!.setShader(
                    LinearGradient(
                        0f,
                        0f,
                        AndroidUtils.dp(ellipsizeByGradientWidthDp).also {
                            fadeEllpsizePaintWidth = it
                        }.toFloat(),
                        0f,
                        intArrayOf(-0x1, 0),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                )
            } else {
                fadeEllpsizePaint!!.setShader(
                    LinearGradient(
                        0f,
                        0f,
                        AndroidUtils.dp(ellipsizeByGradientWidthDp).also {
                            fadeEllpsizePaintWidth = it
                        }.toFloat(),
                        0f,
                        intArrayOf(0, -0x1),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                )
            }
            fadeEllpsizePaint!!.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
        }
    }

    fun setMaxLines(value: Int) {
        maxLines = value
    }

    fun setGravity(value: Int) {
        gravity = value
    }

    fun setTypeface(typeface: Typeface?) {
        textPaint.setTypeface(typeface)
    }

    val sideDrawablesSize: Int
        get() {
            var size = 0
            if (leftDrawable != null) {
                size += leftDrawable!!.intrinsicWidth + drawablePadding
            }
            if (rightDrawable != null) {
                val dw = (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt()
                size += dw + drawablePadding
            }
            if (rightDrawable2 != null) {
                val dw = (rightDrawable2!!.intrinsicWidth * rightDrawableScale).toInt()
                size += dw + drawablePadding
            }
            return size
        }
    val paint: Paint
        get() = textPaint

    private fun calcOffset(width: Int) {
        if (layout == null) {
            return
        }
        if (layout!!.lineCount > 0) {
            textWidth = ceil(layout!!.getLineWidth(0).toDouble()).toInt()
            textHeight = if (fullLayout != null) {
                fullLayout!!.getLineBottom(fullLayout!!.lineCount - 1)
            } else if (maxLines > 1 && layout!!.lineCount > 0) {
                layout!!.getLineBottom(layout!!.lineCount - 1)
            } else {
                layout!!.getLineBottom(0)
            }
            offsetX =
                if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL) {
                    (width - textWidth) / 2 - layout!!.getLineLeft(0).toInt()
                } else if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.LEFT) {
                    if (firstLineLayout != null) {
                        -firstLineLayout!!.getLineLeft(0).toInt()
                    } else {
                        -layout!!.getLineLeft(0).toInt()
                    }
                } else if (layout!!.getLineLeft(0) == 0f) {
                    if (firstLineLayout != null) {
                        (width - firstLineLayout!!.getLineWidth(0)).toInt()
                    } else {
                        width - textWidth
                    }
                } else {
                    -AndroidUtils.dp(8)
                }
            offsetX += getPaddingLeft()
            var rightDrawableWidth = 0
            if (rightDrawableInside) {
                if (rightDrawable != null && !rightDrawableOutside) {
                    rightDrawableWidth += (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt()
                }
                if (rightDrawable2 != null && !rightDrawableOutside) {
                    rightDrawableWidth += (rightDrawable2!!.intrinsicWidth * rightDrawableScale).toInt()
                }
            }
            textDoesNotFit = textWidth + rightDrawableWidth > width - paddingRight
            if (fullLayout != null && fullLayoutAdditionalWidth > 0) {
                fullLayoutLeftCharactersOffset =
                    fullLayout!!.getPrimaryHorizontal(0) - firstLineLayout!!.getPrimaryHorizontal(0)
            }
        }
        replacingDrawableTextOffset = if (replacingDrawableTextIndex >= 0) {
            layout!!.getPrimaryHorizontal(replacingDrawableTextIndex)
        } else {
            0f
        }
    }

    protected fun createLayout(width: Int): Boolean {
        var width = width
        var text = text
        replacingDrawableTextIndex = -1
        rightDrawableHidden = false
        if (text != null) {
            try {
                if (leftDrawable != null) {
                    width -= leftDrawable!!.intrinsicWidth
                    width -= drawablePadding
                }
                var rightDrawableWidth = 0
                if (!rightDrawableInside) {
                    if (rightDrawable != null && !rightDrawableOutside) {
                        rightDrawableWidth += (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt()
                        width -= rightDrawableWidth
                        width -= drawablePadding
                    }
                    if (rightDrawable2 != null && !rightDrawableOutside) {
                        rightDrawableWidth += (rightDrawable2!!.intrinsicWidth * rightDrawableScale).toInt()
                        width -= rightDrawableWidth
                        width -= drawablePadding
                    }
                }
                if (replacedText != null && replacedDrawable != null) {
                    replacingDrawableTextIndex = text.toString().indexOf(replacedText!!)
                    if (replacingDrawableTextIndex >= 0) {
                        val builder = SpannableStringBuilder.valueOf(text)
                        builder.setSpan(
                            FixedWidthSpan(replacedDrawable!!.intrinsicWidth),
                            replacingDrawableTextIndex,
                            replacingDrawableTextIndex + replacedText!!.length,
                            0
                        )
                        text = builder
                    } else {
                        width -= replacedDrawable!!.intrinsicWidth
                        width -= drawablePadding
                    }
                }
                if (canHideRightDrawable && rightDrawableWidth != 0 && !rightDrawableOutside) {
                    val string = TextUtils.ellipsize(
                        text,
                        textPaint,
                        width.toFloat(),
                        TextUtils.TruncateAt.END
                    )
                    if (text != string) {
                        rightDrawableHidden = true
                        width += rightDrawableWidth
                        width += drawablePadding
                    }
                }
                if (buildFullLayout) {
                    var string = text
                    if (!ellipsizeByGradient) {
                        string = TextUtils.ellipsize(
                            string,
                            textPaint,
                            width.toFloat(),
                            TextUtils.TruncateAt.END
                        )
                    }
                    if (!ellipsizeByGradient && string != text) {
                        fullLayout = StaticLayoutEx.createStaticLayout(
                            text!!,
                            textPaint,
                            width,
                            alignment,
                            1.0f,
                            0.0f,
                            false,
                            TextUtils.TruncateAt.END,
                            width,
                            fullTextMaxLines,
                            false
                        )
                        if (fullLayout != null) {
                            val end = fullLayout!!.getLineEnd(0)
                            val start = fullLayout!!.getLineStart(1)
                            val substr = text.subSequence(0, end)
                            val full = SpannableStringBuilder.valueOf(text)
                            full.setSpan(EmptyStubSpan(), 0, start, 0)
                            var part: CharSequence
                            part = if (end < string!!.length) {
                                string.subSequence(end, string.length)
                            } else {
                                "…"
                            }
                            firstLineLayout = StaticLayout(
                                string,
                                0,
                                string.length,
                                textPaint,
                                if (scrollNonFitText) AndroidUtils.dp(2000) else width + AndroidUtils.dp(
                                    8
                                ),
                                alignment,
                                1.0f,
                                0.0f,
                                false
                            )
                            layout = StaticLayout(
                                substr,
                                0,
                                substr.length,
                                textPaint,
                                if (scrollNonFitText) AndroidUtils.dp(2000) else width + AndroidUtils.dp(
                                    8
                                ),
                                alignment,
                                1.0f,
                                0.0f,
                                false
                            )
                            if ((layout as StaticLayout).getLineLeft(0) != 0f) {
                                part = "\u200F" + part
                            }
                            partLayout = StaticLayout(
                                part,
                                0,
                                part.length,
                                textPaint,
                                if (scrollNonFitText) AndroidUtils.dp(2000) else width + AndroidUtils.dp(
                                    8
                                ),
                                alignment,
                                1.0f,
                                0.0f,
                                false
                            )
                            fullLayout = StaticLayoutEx.createStaticLayout(
                                full,
                                textPaint,
                                width + AndroidUtils.dp(8) + fullLayoutAdditionalWidth,
                                alignment,
                                1.0f,
                                0.0f,
                                false,
                                TextUtils.TruncateAt.END,
                                width + fullLayoutAdditionalWidth,
                                fullTextMaxLines,
                                false
                            )
                        }
                    } else {
                        layout = StaticLayout(
                            string,
                            0,
                            string!!.length,
                            textPaint,
                            if (scrollNonFitText || ellipsizeByGradient) AndroidUtils.dp(2000) else width + AndroidUtils.dp(
                                8
                            ),
                            alignment,
                            1.0f,
                            0.0f,
                            false
                        )
                        fullLayout = null
                        partLayout = null
                        firstLineLayout = null
                    }
                } else if (maxLines > 1) {
                    layout = StaticLayoutEx.createStaticLayout(
                        text!!,
                        textPaint,
                        width,
                        alignment,
                        1.0f,
                        0.0f,
                        false,
                        TextUtils.TruncateAt.END,
                        width,
                        maxLines,
                        false
                    )
                } else {
                    val string: CharSequence?
                    string = if (scrollNonFitText || ellipsizeByGradient) {
                        text
                    } else {
                        TextUtils.ellipsize(
                            text,
                            textPaint,
                            width.toFloat(),
                            TextUtils.TruncateAt.END
                        )
                    }
                    /*if (layout != null && TextUtils.equals(layout.getText(), string)) {
                        calcOffset(width);
                        return false;
                    }*/layout = StaticLayout(
                        string,
                        0,
                        string!!.length,
                        textPaint,
                        if (scrollNonFitText || ellipsizeByGradient) AndroidUtils.dp(2000) else width + AndroidUtils.dp(
                            8
                        ),
                        alignment,
                        1.0f,
                        0.0f,
                        false
                    )
                }
                calcOffset(width)
            } catch (ignore: Exception) {
            }
        } else {
            layout = null
            textWidth = 0
            textHeight = 0
        }
        invalidate()
        return true
    }

    private var alignment: Layout.Alignment
        get() = mAlignment
        set(alignment) {
            mAlignment = alignment
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (lastWidth != AndroidUtils.displaySize.x) {
            lastWidth = AndroidUtils.displaySize.x
            scrollingOffset = 0f
            currentScrollDelay = SCROLL_DELAY_MS
        }
        createLayout(width - getPaddingLeft() - getPaddingRight() - minusWidth - (if (rightDrawableOutside && rightDrawable != null) rightDrawable!!.intrinsicWidth + drawablePadding else 0) - if (rightDrawableOutside && rightDrawable2 != null) rightDrawable2!!.intrinsicWidth + drawablePadding else 0)
        val finalHeight: Int = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            height
        } else {
            paddingTop + textHeight + paddingBottom
        }
        if (widthWrapContent) {
//            textWidth = (int) Math.ceil(layout.getLineWidth(0));
            width = min(
                width.toDouble(),
                (getPaddingLeft() + textWidth + getPaddingRight() + minusWidth + (if (rightDrawableOutside && rightDrawable != null) rightDrawable!!.intrinsicWidth + drawablePadding else 0) + if (rightDrawableOutside && rightDrawable2 != null) rightDrawable2!!.intrinsicWidth + drawablePadding else 0).toDouble()
            )
                .toInt()
        }
        setMeasuredDimension(width, finalHeight)
        offsetY = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
            paddingTop + (measuredHeight - paddingTop - paddingBottom - textHeight) / 2
        } else {
            paddingTop
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        wasLayout = true
    }

    fun getTextWidth(): Int {
        return textWidth + if (rightDrawableInside) (if (rightDrawable != null) (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt() else 0) + (if (rightDrawable2 != null) (rightDrawable2!!.intrinsicWidth * rightDrawableScale).toInt() else 0) else 0
    }

    val rightDrawableWidth: Int
        get() = if (rightDrawable == null) 0 else (drawablePadding + rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt()

    fun setLeftDrawableTopPadding(value: Int) {
        leftDrawableTopPadding = value
    }

    fun setRightDrawableTopPadding(value: Int) {
        rightDrawableTopPadding = value
    }

    fun setLeftDrawable(resId: Int) {
        setLeftDrawable(if (resId == 0) null else context.resources.getDrawable(resId))
    }

    fun setRightDrawable(resId: Int) {
        setRightDrawable(if (resId == 0) null else context.resources.getDrawable(resId))
    }

    fun setMinWidth(width: Int) {
        minWidth = width
    }

    @Deprecated("Deprecated in Java")
    override fun setBackgroundDrawable(background: Drawable) {
        if (maxLines > 1) {
            super.setBackgroundDrawable(background)
            return
        }
        wrapBackgroundDrawable = background
    }

//    override fun getBackground(): Drawable {
//        return wrapBackgroundDrawable ?: super.getBackground()
//    }

    fun setLeftDrawable(drawable: Drawable?) {
        if (leftDrawable === drawable) {
            return
        }
        if (leftDrawable != null) {
            leftDrawable!!.callback = null
        }
        leftDrawable = drawable
        if (drawable != null) {
            drawable.callback = this
        }
        if (!recreateLayoutMaybe()) {
            invalidate()
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who === rightDrawable || who === rightDrawable2 || who === leftDrawable || super.verifyDrawable(
            who
        )
    }

    fun replaceTextWithDrawable(drawable: Drawable?, replacedText: String?) {
        if (replacedDrawable === drawable) {
            return
        }
        if (replacedDrawable != null) {
            replacedDrawable!!.callback = null
        }
        replacedDrawable = drawable
        if (drawable != null) {
            drawable.callback = this
        }
        if (!recreateLayoutMaybe()) {
            invalidate()
        }
        this.replacedText = replacedText
    }

    fun setMinusWidth(value: Int) {
        if (value == minusWidth) {
            return
        }
        minusWidth = value
        if (!recreateLayoutMaybe()) {
            invalidate()
        }
    }

    fun setRightDrawable(drawable: Drawable?) {
        if (rightDrawable === drawable) {
            return
        }
        if (rightDrawable != null) {
            rightDrawable!!.callback = null
        }
        rightDrawable = drawable
        if (drawable != null) {
            drawable.callback = this
        }
        if (!recreateLayoutMaybe()) {
            invalidate()
        }
    }

    fun setRightDrawable2(drawable: Drawable?) {
        if (rightDrawable2 === drawable) {
            return
        }
        if (rightDrawable2 != null) {
            rightDrawable2!!.callback = null
        }
        rightDrawable2 = drawable
        if (drawable != null) {
            drawable.callback = this
        }
        if (!recreateLayoutMaybe()) {
            invalidate()
        }
    }

    fun getRightDrawable2(): Drawable? {
        return rightDrawable2
    }

    fun setRightDrawableScale(scale: Float) {
        rightDrawableScale = scale
    }

    fun setSideDrawablesColor(color: Int) {
        //Theme.setDrawableColor(rightDrawable, color)
        //Theme.setDrawableColor(leftDrawable, color)
    }

    fun setText(value: CharSequence?): Boolean {
        return setText(value, false)
    }

    fun setText(value: CharSequence?, force: Boolean): Boolean {
        if (text == null && value == null || !force && text != null && text == value) {
            return false
        }
        text = value
        currentScrollDelay = SCROLL_DELAY_MS
        recreateLayoutMaybe()
        return true
    }

    fun resetScrolling() {
        scrollingOffset = 0f
    }

    fun copyScrolling(textView: SimpleTextView) {
        scrollingOffset = textView.scrollingOffset
    }

    fun setDrawablePadding(value: Int) {
        if (drawablePadding == value) {
            return
        }
        drawablePadding = value
        if (!recreateLayoutMaybe()) {
            invalidate()
        }
    }

    private fun recreateLayoutMaybe(): Boolean {
        if (wasLayout && measuredHeight != 0 && !buildFullLayout) {
            val result =
                createLayout(maxTextWidth - getPaddingLeft() - getPaddingRight() - minusWidth)
            offsetY = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                (measuredHeight - textHeight) / 2
            } else {
                paddingTop
            }
            return result
        } else {
            requestLayout()
        }
        return true
    }

    fun getText(): CharSequence {
        return text ?: ""
    }

    val lineCount: Int
        get() {
            var count = 0
            if (layout != null) {
                count += layout!!.lineCount
            }
            if (fullLayout != null) {
                count += fullLayout!!.lineCount
            }
            return count
        }
    val textStartX: Int
        get() {
            if (layout == null) {
                return 0
            }
            var textOffsetX = 0
            if (leftDrawable != null) {
                if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.LEFT) {
                    textOffsetX += drawablePadding + leftDrawable!!.intrinsicWidth
                }
            }
            if (replacedDrawable != null && replacingDrawableTextIndex < 0) {
                if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.LEFT) {
                    textOffsetX += drawablePadding + replacedDrawable!!.intrinsicWidth
                }
            }
            return x.toInt() + offsetX + textOffsetX
        }
    val textStartY: Int
        get() = if (layout == null) {
            0
        } else y.toInt()

    fun setRightPadding(padding: Int) {
        if (paddingRight != padding) {
            paddingRight = padding
            var width = maxTextWidth - getPaddingLeft() - getPaddingRight() - minusWidth
            if (leftDrawable != null) {
                width -= leftDrawable!!.intrinsicWidth
                width -= drawablePadding
            }
            var rightDrawableWidth = 0
            if (!rightDrawableInside) {
                if (rightDrawable != null && !rightDrawableOutside) {
                    rightDrawableWidth =
                        (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt()
                    width -= rightDrawableWidth
                    width -= drawablePadding
                }
                if (rightDrawable2 != null && !rightDrawableOutside) {
                    rightDrawableWidth =
                        (rightDrawable2!!.intrinsicWidth * rightDrawableScale).toInt()
                    width -= rightDrawableWidth
                    width -= drawablePadding
                }
            }
            if (replacedText != null && replacedDrawable != null) {
                if (text.toString().indexOf(replacedText!!).also {
                        replacingDrawableTextIndex = it
                    } < 0) {
                    width -= replacedDrawable!!.intrinsicWidth
                    width -= drawablePadding
                }
            }
            if (canHideRightDrawable && rightDrawableWidth != 0 && !rightDrawableOutside) {
                val string =
                    TextUtils.ellipsize(text, textPaint, width.toFloat(), TextUtils.TruncateAt.END)
                if (text != string) {
                    rightDrawableHidden = true
                    width += rightDrawableWidth
                    width += drawablePadding
                }
            }
            calcOffset(width)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var textOffsetX = 0
        val fade = scrollNonFitText && (textDoesNotFit || scrollingOffset != 0f)
        var restore = Int.MIN_VALUE
        if (fade || ellipsizeByGradient) {
            restore = canvas.saveLayerAlpha(
                0f,
                0f,
                measuredWidth.toFloat(),
                measuredHeight.toFloat(),
                255,
                Canvas.ALL_SAVE_FLAG
            )
        }
        totalWidth = textWidth
        if (leftDrawable != null) {
            var x = -scrollingOffset.toInt()
            if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL) {
                x += offsetX
            }
            val y: Int
            y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                (measuredHeight - leftDrawable!!.intrinsicHeight) / 2 + leftDrawableTopPadding
            } else {
                paddingTop + (textHeight - leftDrawable!!.intrinsicHeight) / 2 + leftDrawableTopPadding
            }
            leftDrawable!!.setBounds(
                x,
                y,
                x + leftDrawable!!.intrinsicWidth,
                y + leftDrawable!!.intrinsicHeight
            )
            leftDrawable!!.draw(canvas)
            if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.LEFT || gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL) {
                textOffsetX += drawablePadding + leftDrawable!!.intrinsicWidth
            }
            totalWidth += drawablePadding + leftDrawable!!.intrinsicWidth
        }
        if (replacedDrawable != null && replacedText != null) {
            var x = (-scrollingOffset + replacingDrawableTextOffset).toInt()
            if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL) {
                x += offsetX
            }
            val y: Int
            y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                (measuredHeight - replacedDrawable!!.intrinsicHeight) / 2 + leftDrawableTopPadding
            } else {
                (textHeight - replacedDrawable!!.intrinsicHeight) / 2 + leftDrawableTopPadding
            }
            replacedDrawable!!.setBounds(
                x,
                y,
                x + replacedDrawable!!.intrinsicWidth,
                y + replacedDrawable!!.intrinsicHeight
            )
            replacedDrawable!!.draw(canvas)
            if (replacingDrawableTextIndex < 0) {
                if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.LEFT || gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL) {
                    textOffsetX += drawablePadding + replacedDrawable!!.intrinsicWidth
                }
                totalWidth += drawablePadding + replacedDrawable!!.intrinsicWidth
            }
        }
        if (rightDrawable != null && !rightDrawableHidden && rightDrawableScale > 0 && !rightDrawableOutside && !rightDrawableInside) {
            var x = textOffsetX + textWidth + drawablePadding + -scrollingOffset.toInt()
            if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL ||
                gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.RIGHT
            ) {
                x += offsetX
            }
            val dw = (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt()
            val dh = (rightDrawable!!.intrinsicHeight * rightDrawableScale).toInt()
            val y: Int
            y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                (measuredHeight - dh) / 2 + rightDrawableTopPadding
            } else {
                paddingTop + (textHeight - dh) / 2 + rightDrawableTopPadding
            }
            rightDrawable!!.setBounds(x, y, x + dw, y + dh)
            rightDrawableX = x + (dw shr 1)
            rightDrawableY = y + (dh shr 1)
            rightDrawable!!.draw(canvas)
            totalWidth += drawablePadding + dw
        }
        if (rightDrawable2 != null && !rightDrawableHidden && rightDrawableScale > 0 && !rightDrawableOutside && !rightDrawableInside) {
            var x = textOffsetX + textWidth + drawablePadding + -scrollingOffset.toInt()
            if (rightDrawable != null) {
                x += (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt() + drawablePadding
            }
            if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL ||
                gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.RIGHT
            ) {
                x += offsetX
            }
            val dw = (rightDrawable2!!.intrinsicWidth * rightDrawableScale).toInt()
            val dh = (rightDrawable2!!.intrinsicHeight * rightDrawableScale).toInt()
            val y: Int
            y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                (measuredHeight - dh) / 2 + rightDrawableTopPadding
            } else {
                paddingTop + (textHeight - dh) / 2 + rightDrawableTopPadding
            }
            rightDrawable2!!.setBounds(x, y, x + dw, y + dh)
            rightDrawable2!!.draw(canvas)
            totalWidth += drawablePadding + dw
        }
        val nextScrollX: Int = totalWidth + AndroidUtils.dp(DIST_BETWEEN_SCROLLING_TEXT)
        if (scrollingOffset != 0f) {
            if (leftDrawable != null) {
                val x = -scrollingOffset.toInt() + nextScrollX
                val y: Int
                y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                    (measuredHeight - leftDrawable!!.intrinsicHeight) / 2 + leftDrawableTopPadding
                } else {
                    paddingTop + (textHeight - leftDrawable!!.intrinsicHeight) / 2 + leftDrawableTopPadding
                }
                leftDrawable!!.setBounds(
                    x,
                    y,
                    x + leftDrawable!!.intrinsicWidth,
                    y + leftDrawable!!.intrinsicHeight
                )
                leftDrawable!!.draw(canvas)
            }
            if (rightDrawable != null && !rightDrawableOutside) {
                val dw = (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt()
                val dh = (rightDrawable!!.intrinsicHeight * rightDrawableScale).toInt()
                val x =
                    textOffsetX + textWidth + drawablePadding + -scrollingOffset.toInt() + nextScrollX
                val y: Int
                y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                    (measuredHeight - dh) / 2 + rightDrawableTopPadding
                } else {
                    paddingTop + (textHeight - dh) / 2 + rightDrawableTopPadding
                }
                rightDrawable!!.setBounds(x, y, x + dw, y + dh)
                rightDrawable!!.draw(canvas)
            }
            if (rightDrawable2 != null && !rightDrawableOutside) {
                val dw = (rightDrawable2!!.intrinsicWidth * rightDrawableScale).toInt()
                val dh = (rightDrawable2!!.intrinsicHeight * rightDrawableScale).toInt()
                var x =
                    textOffsetX + textWidth + drawablePadding + -scrollingOffset.toInt() + nextScrollX
                if (rightDrawable != null) {
                    x += (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt() + drawablePadding
                }
                val y: Int
                y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                    (measuredHeight - dh) / 2 + rightDrawableTopPadding
                } else {
                    paddingTop + (textHeight - dh) / 2 + rightDrawableTopPadding
                }
                rightDrawable2!!.setBounds(x, y, x + dw, y + dh)
                rightDrawable2!!.draw(canvas)
            }
        }
        if (layout != null) {
            if (rightDrawableOutside || ellipsizeByGradient || paddingRight > 0) {
                canvas.save()
                canvas.clipRect(
                    0,
                    0,
                    maxTextWidth - paddingRight - AndroidUtils.dp(0),
                    measuredHeight
                )
            }
            if (wrapBackgroundDrawable != null) {
                val cx = (offsetX + textOffsetX - scrollingOffset).toInt() + textWidth / 2
                val w = max(
                    (textWidth + getPaddingLeft() + getPaddingRight()).toDouble(),
                    minWidth.toDouble()
                )
                    .toInt()
                val x = cx - w / 2
                wrapBackgroundDrawable!!.setBounds(x, 0, x + w, measuredHeight)
                wrapBackgroundDrawable!!.draw(canvas)
            }
            if (offsetX + textOffsetX != 0 || offsetY != 0 || scrollingOffset != 0f) {
                canvas.save()
                canvas.translate(offsetX + textOffsetX - scrollingOffset, offsetY.toFloat())
                if (scrollingOffset != 0f) {
                    //canvas.clipRect(0, 0, getMeasuredWidth(), getMeasuredHeight());
                }
            }
            drawLayout(canvas)
            if (partLayout != null && fullAlpha < 1.0f) {
                val prevAlpha = textPaint.alpha
                textPaint.setAlpha((255 * (1.0f - fullAlpha)).toInt())
                canvas.save()
                var partOffset = 0f
                if (partLayout!!.text.length == 1) {
                    partOffset =
                        if (fullTextMaxLines == 1) AndroidUtils.dp(0.5f).toFloat() else AndroidUtils.dp(
                            4
                        ).toFloat()
                }
                if (layout!!.getLineLeft(0) != 0f) {
                    canvas.translate(-layout!!.getLineWidth(0) + partOffset, 0f)
                } else {
                    canvas.translate(layout!!.getLineWidth(0) - partOffset, 0f)
                }
                canvas.translate(
                    -fullLayoutLeftOffset * fullAlpha + fullLayoutLeftCharactersOffset * fullAlpha,
                    0f
                )
                partLayout!!.draw(canvas)
                canvas.restore()
                textPaint.setAlpha(prevAlpha)
            }
            if (fullLayout != null && fullAlpha > 0) {
                val prevAlpha = textPaint.alpha
                textPaint.setAlpha((255 * fullAlpha).toInt())
                canvas.translate(
                    -fullLayoutLeftOffset * fullAlpha + fullLayoutLeftCharactersOffset * fullAlpha - fullLayoutLeftCharactersOffset,
                    0f
                )
                fullLayout!!.draw(canvas)
                textPaint.setAlpha(prevAlpha)
            }
            if (scrollingOffset != 0f) {
                canvas.translate(nextScrollX.toFloat(), 0f)
                drawLayout(canvas)
            }
            if (offsetX + textOffsetX != 0 || offsetY != 0 || scrollingOffset != 0f) {
                canvas.restore()
            }
            if (rightDrawable != null && !rightDrawableHidden && rightDrawableScale > 0 && !rightDrawableOutside && rightDrawableInside) {
                var x = textOffsetX + textWidth + drawablePadding + -scrollingOffset.toInt()
                if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL ||
                    gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.RIGHT
                ) {
                    x += offsetX
                }
                val dw = (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt()
                val dh = (rightDrawable!!.intrinsicHeight * rightDrawableScale).toInt()
                val y: Int
                y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                    (measuredHeight - dh) / 2 + rightDrawableTopPadding
                } else {
                    paddingTop + (textHeight - dh) / 2 + rightDrawableTopPadding
                }
                rightDrawable!!.setBounds(x, y, x + dw, y + dh)
                rightDrawableX = x + (dw shr 1)
                rightDrawableY = y + (dh shr 1)
                rightDrawable!!.draw(canvas)
                totalWidth += drawablePadding + dw
            }
            if (rightDrawable2 != null && !rightDrawableHidden && rightDrawableScale > 0 && !rightDrawableOutside && rightDrawableInside) {
                var x = textOffsetX + textWidth + drawablePadding + -scrollingOffset.toInt()
                if (rightDrawable != null) {
                    x += (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt() + drawablePadding
                }
                if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL ||
                    gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.RIGHT
                ) {
                    x += offsetX
                }
                val dw = (rightDrawable2!!.intrinsicWidth * rightDrawableScale).toInt()
                val dh = (rightDrawable2!!.intrinsicHeight * rightDrawableScale).toInt()
                val y: Int
                y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                    (measuredHeight - dh) / 2 + rightDrawableTopPadding
                } else {
                    paddingTop + (textHeight - dh) / 2 + rightDrawableTopPadding
                }
                rightDrawable2!!.setBounds(x, y, x + dw, y + dh)
                rightDrawable2!!.draw(canvas)
                totalWidth += drawablePadding + dw
            }
            if (fade) {
                if (scrollingOffset < AndroidUtils.dp(10)) {
                    fadePaint!!.setAlpha((255 * (scrollingOffset / AndroidUtils.dp(10))) as Int)
                } else if (scrollingOffset > totalWidth + AndroidUtils.dp(
                        DIST_BETWEEN_SCROLLING_TEXT
                    ) - AndroidUtils.dp(10)
                ) {
                    val dist: Float = scrollingOffset - (totalWidth + AndroidUtils.dp(
                        DIST_BETWEEN_SCROLLING_TEXT
                    ) - AndroidUtils.dp(10))
                    fadePaint!!.setAlpha((255 * (1.0f - dist / AndroidUtils.dp(10))) as Int)
                } else {
                    fadePaint!!.setAlpha(255)
                }
                canvas.drawRect(
                    0f, 0f, AndroidUtils.dp(6).toFloat(), measuredHeight.toFloat(),
                    fadePaint!!
                )
                canvas.save()
                canvas.translate(maxTextWidth - paddingRight - AndroidUtils.dp(6).toFloat(), 0f)
                canvas.drawRect(
                    0f, 0f, AndroidUtils.dp(6).toFloat(), measuredHeight.toFloat(),
                    fadePaintBack!!
                )
                canvas.restore()
            } else if (ellipsizeByGradient && textDoesNotFit && fadeEllpsizePaint != null) {
                canvas.save()
                updateFadePaints()
                if (!ellipsizeByGradientLeft) {
                    canvas.translate(
                        maxTextWidth.toFloat() - paddingRight - fadeEllpsizePaintWidth, 0f
                    )
                }
                canvas.drawRect(
                    0f, 0f, fadeEllpsizePaintWidth.toFloat(), measuredHeight.toFloat(),
                    fadeEllpsizePaint!!
                )
                canvas.restore()
            }
            updateScrollAnimation()
            if (rightDrawableOutside || ellipsizeByGradient || paddingRight > 0) {
                canvas.restore()
            }
        }
        if (fade || ellipsizeByGradient) {
            canvas.restoreToCount(restore)
        }
        if (rightDrawable != null && rightDrawableOutside) {
            val x = min(
                (textOffsetX + textWidth + drawablePadding + (if (scrollingOffset == 0f) -nextScrollX else -scrollingOffset.toInt()) + nextScrollX).toDouble(),
                (maxTextWidth - paddingRight + drawablePadding).toDouble()
            )
                .toInt()
            val dw = (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt()
            val dh = (rightDrawable!!.intrinsicHeight * rightDrawableScale).toInt()
            val y: Int
            y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                (measuredHeight - dh) / 2 + rightDrawableTopPadding
            } else {
                paddingTop + (textHeight - dh) / 2 + rightDrawableTopPadding
            }
            rightDrawable!!.setBounds(x, y, x + dw, y + dh)
            rightDrawableX = x + (dw shr 1)
            rightDrawableY = y + (dh shr 1)
            rightDrawable!!.draw(canvas)
        }
        if (rightDrawable2 != null && rightDrawableOutside) {
            var x = min(
                (
                        textOffsetX + textWidth + drawablePadding + (if (scrollingOffset == 0f) -nextScrollX else -scrollingOffset.toInt()) + nextScrollX).toDouble(),
                (
                        maxTextWidth - paddingRight + drawablePadding
                        ).toDouble()
            ).toInt()
            if (rightDrawable != null) {
                x += (rightDrawable!!.intrinsicWidth * rightDrawableScale).toInt() + drawablePadding
            }
            val dw = (rightDrawable2!!.intrinsicWidth * rightDrawableScale).toInt()
            val dh = (rightDrawable2!!.intrinsicHeight * rightDrawableScale).toInt()
            val y: Int
            y = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                (measuredHeight - dh) / 2 + rightDrawableTopPadding
            } else {
                paddingTop + (textHeight - dh) / 2 + rightDrawableTopPadding
            }
            rightDrawable2!!.setBounds(x, y, x + dw, y + dh)
            rightDrawable2!!.draw(canvas)
        }
    }

    val maxTextWidth: Int
        get() = measuredWidth - (if (rightDrawableOutside && rightDrawable != null) rightDrawable!!.intrinsicWidth + drawablePadding else 0) - (if (rightDrawableOutside && rightDrawable2 != null) rightDrawable2!!.intrinsicWidth + drawablePadding else 0)

    private fun drawLayout(canvas: Canvas) {
        if (fullAlpha > 0 && fullLayoutLeftOffset != 0) {
            canvas.save()
            canvas.translate(
                -fullLayoutLeftOffset * fullAlpha + fullLayoutLeftCharactersOffset * fullAlpha,
                0f
            )
            canvas.save()
            clipOutSpoilers(canvas)
            layout!!.draw(canvas)
            canvas.restore()
            drawSpoilers(canvas)
            canvas.restore()
        } else {
            canvas.save()
            clipOutSpoilers(canvas)
            layout!!.draw(canvas)
            canvas.restore()
            drawSpoilers(canvas)
        }
    }

    private fun clipOutSpoilers(canvas: Canvas) {
        path.rewind()
        canvas.clipPath(path, Region.Op.DIFFERENCE)
    }

    private fun drawSpoilers(canvas: Canvas) {
    }

    private fun updateScrollAnimation() {
        if (!scrollNonFitText || !textDoesNotFit && scrollingOffset == 0f) {
            return
        }
        val newUpdateTime = SystemClock.elapsedRealtime()
        var dt = newUpdateTime - lastUpdateTime
        if (dt > 17) {
            dt = 17
        }
        if (currentScrollDelay > 0) {
            currentScrollDelay -= dt.toInt()
        } else {
            val totalDistance: Int = totalWidth + AndroidUtils.dp(DIST_BETWEEN_SCROLLING_TEXT)
            val pixelsPerSecond: Float
            if (scrollingOffset < AndroidUtils.dp(SCROLL_SLOWDOWN_PX)) {
                pixelsPerSecond =
                    PIXELS_PER_SECOND_SLOW + (PIXELS_PER_SECOND - PIXELS_PER_SECOND_SLOW) * (scrollingOffset / AndroidUtils.dp(
                        SCROLL_SLOWDOWN_PX
                    ))
            } else if (scrollingOffset >= totalDistance - AndroidUtils.dp(SCROLL_SLOWDOWN_PX)) {
                val dist: Float = scrollingOffset - (totalDistance - AndroidUtils.dp(
                    SCROLL_SLOWDOWN_PX
                ))
                pixelsPerSecond =
                    PIXELS_PER_SECOND - (PIXELS_PER_SECOND - PIXELS_PER_SECOND_SLOW) * (dist / AndroidUtils.dp(
                        SCROLL_SLOWDOWN_PX
                    ))
            } else {
                pixelsPerSecond = PIXELS_PER_SECOND.toFloat()
            }
            scrollingOffset += dt / 1000.0f * AndroidUtils.dp(pixelsPerSecond)
            lastUpdateTime = newUpdateTime
            if (scrollingOffset > totalDistance) {
                scrollingOffset = 0f
                currentScrollDelay = SCROLL_DELAY_MS
            }
        }
        invalidate()
    }

    override fun invalidateDrawable(who: Drawable) {
        if (who === leftDrawable) {
            invalidate(leftDrawable!!.getBounds())
        } else if (who === rightDrawable) {
            invalidate(rightDrawable!!.getBounds())
        } else if (who === rightDrawable2) {
            invalidate(rightDrawable2!!.getBounds())
        } else if (who === replacedDrawable) {
            invalidate(replacedDrawable!!.getBounds())
        }
    }

    override fun hasOverlappingRendering(): Boolean {
        return false
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.isVisibleToUser = true
        info.setClassName("android.widget.TextView")
        info.setText(text)
    }

    fun setFullLayoutAdditionalWidth(fullLayoutAdditionalWidth: Int, fullLayoutLeftOffset: Int) {
        if (this.fullLayoutAdditionalWidth != fullLayoutAdditionalWidth || this.fullLayoutLeftOffset != fullLayoutLeftOffset) {
            this.fullLayoutAdditionalWidth = fullLayoutAdditionalWidth
            this.fullLayoutLeftOffset = fullLayoutLeftOffset
            createLayout(maxTextWidth - getPaddingLeft() - getPaddingRight() - minusWidth)
        }
    }

    fun setFullTextMaxLines(fullTextMaxLines: Int) {
        this.fullTextMaxLines = fullTextMaxLines
    }

    var textColor: Int
        get() = textPaint.color
        set(color) {
            textPaint.setColor(color)
            invalidate()
        }

    fun setCanHideRightDrawable(b: Boolean) {
        canHideRightDrawable = b
    }

    // right drawable is ellipsized with text
    fun setRightDrawableInside(inside: Boolean) {
        rightDrawableInside = inside
    }

    fun setRightDrawableOnClick(onClickListener: OnClickListener?) {
        rightDrawableOnClickListener = onClickListener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (rightDrawableOnClickListener != null && rightDrawable != null) {
            AndroidUtils.rectTmp.set(
                rightDrawableX.toFloat() - AndroidUtils.dp(16),
                rightDrawableY.toFloat() - AndroidUtils.dp(16),
                rightDrawableX.toFloat() + AndroidUtils.dp(16),
                rightDrawableY.toFloat() + AndroidUtils.dp(16)
            )
            if (event.action == MotionEvent.ACTION_DOWN && AndroidUtils.rectTmp.contains(
                    event.x,
                    event.y
                )
            ) {
                maybeClick = true
                touchDownX = event.x
                touchDownY = event.y
                parent.requestDisallowInterceptTouchEvent(true)
                if (rightDrawable is PressableDrawable) {
                    (rightDrawable as PressableDrawable).isPressed = true
                }
            } else if (event.action == MotionEvent.ACTION_MOVE && maybeClick) {
                if (abs((event.x - touchDownX).toDouble()) >= AndroidUtils.touchSlop || abs((event.y - touchDownY).toDouble()) >= AndroidUtils.touchSlop) {
                    maybeClick = false
                    parent.requestDisallowInterceptTouchEvent(false)
                    if (rightDrawable is PressableDrawable) {
                        (rightDrawable as PressableDrawable).isPressed = false
                    }
                }
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                if (maybeClick && event.action == MotionEvent.ACTION_UP) {
                    rightDrawableOnClickListener!!.onClick(this)
                    if (rightDrawable is PressableDrawable) {
                        (rightDrawable as PressableDrawable).isPressed = false
                    }
                }
                maybeClick = false
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event) || maybeClick
    }

    interface PressableDrawable {
        var isPressed: Boolean
    }

    companion object {
        private const val PIXELS_PER_SECOND = 50
        private const val PIXELS_PER_SECOND_SLOW = 30
        private const val DIST_BETWEEN_SCROLLING_TEXT = 16
        private const val SCROLL_DELAY_MS = 500
        private const val SCROLL_SLOWDOWN_PX = 100
    }
}
