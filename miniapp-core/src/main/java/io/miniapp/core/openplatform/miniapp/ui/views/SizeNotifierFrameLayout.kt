package io.miniapp.core.openplatform.miniapp.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.graphics.text.MeasuredText
import android.view.View
import android.widget.FrameLayout
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import kotlin.math.max

internal open class SizeNotifierFrameLayout(context: Context) : FrameLayout(context) {
    private val rect = Rect()
    private var backgroundImage: Drawable? = null
    private var backgroundMotion = false
    private var oldBackgroundDrawable: Drawable? = null
    private var oldBackgroundMotion = false
    protected var curKeyboardHeight = 0
    private var bottomClip = 0
    protected var delegate: SizeNotifierFrameLayoutDelegate? = null
    private var occupyStatusBar = true
    private var parallaxEffect: WallpaperParallaxEffect? = null
    private var translationX = 0f
    private var translationY = 0f
    private var bgAngle = 0f
    private var parallaxScale = 1.0f
    private var backgroundTranslationY = 0
    private var paused = true
    private var emojiHeight = 0
    private var emojiOffset = 0f
    private var animationInProgress = false
    private var skipBackgroundDrawing = false
    protected var backgroundView: View? = null
    var attached = false

    //blur variables
    var needBlur = false
    var needBlurBottom = false
    var blurIsRunning = false
    var blurGeneratingTuskIsRunning = false
    var currentBitmap: BlurBitmap? = null
    var prevBitmap: BlurBitmap? = null
    var unusedBitmaps = ArrayList<BlurBitmap?>(10)
    var blurBehindViews = ArrayList<View>()
    var matrix1 = Matrix()
    var matrix2 = Matrix()
    var blurPaintTop = Paint()
    var blurPaintTop2 = Paint()
    var blurPaintBottom = Paint()
    var blurPaintBottom2 = Paint()
    private var selectedBlurPaint: Paint? = null
    private var selectedBlurPaint2: Paint? = null
    var saturation = 0f
    var blurCrossfadeProgress = 0f
    private val DOWN_SCALE = 12f
    private val TOP_CLIP_OFFSET = (10 + DOWN_SCALE * 2).toInt()
    var blurCrossfade: ValueAnimator? = null
    var invalidateBlur = false
    var count = 0
    var times = 0
    var count2 = 0
    var times2 = 0
    private var themeAnimationValue = 1f

    //
    fun invalidateBlur() {
        invalidateBlur = true
        if (!blurIsRunning || blurGeneratingTuskIsRunning) {
            return
        }
        invalidate()
    }

    fun invalidateBackground() {
        if (backgroundView != null) {
            backgroundView!!.invalidate()
        }
    }

    val bottomPadding: Int
        get() = 0

    fun interface SizeNotifierFrameLayoutDelegate {
        fun onSizeChanged(keyboardHeight: Int, isWidthGreater: Boolean)
    }

    private inner class BackgroundView(context: Context?) : View(context) {
        override fun onDraw(canvas: Canvas) {
            if (backgroundImage == null || skipBackgroundDrawing) {
                return
            }
        }
    }

    fun setBackgroundImage(bitmap: Drawable, motion: Boolean) {
        if (backgroundImage === bitmap) {
            return
        }
    }

    private fun checkMotion() {
        val motion = oldBackgroundMotion || backgroundMotion
        if (motion) {
            if (parallaxEffect == null) {
                parallaxEffect = WallpaperParallaxEffect(context)
                parallaxEffect!!.setCallback { offsetX: Int, offsetY: Int, angle: Float ->
                    translationX = offsetX.toFloat()
                    translationY = offsetY.toFloat()
                    bgAngle = angle
                    if (backgroundView != null) {
                        backgroundView!!.invalidate()
                    }
                }
                if (measuredWidth != 0 && measuredHeight != 0) {
                    parallaxScale = parallaxEffect!!.getScale(measuredWidth, measuredHeight)
                }
            }
            if (!paused) {
                parallaxEffect!!.setEnabled(true)
            }
        } else if (parallaxEffect != null) {
            parallaxEffect!!.setEnabled(false)
            parallaxEffect = null
            parallaxScale = 1.0f
            translationX = 0f
            translationY = 0f
        }
    }

    private fun checkLayerType() {
//        if (parallaxEffect == null && backgroundDrawable instanceof MotionBackgroundDrawable && SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_HIGH) {
//            backgroundView.setLayerType(LAYER_TYPE_HARDWARE, null);
//        } else {
//            backgroundView.setLayerType(LAYER_TYPE_NONE, null);
//        }
    }

    fun setSizeDelegate(delegate: SizeNotifierFrameLayoutDelegate) {
        this.delegate = delegate
    }

    fun setOccupyStatusBar(value: Boolean) {
        occupyStatusBar = value
    }

    fun onPause() {
        if (parallaxEffect != null) {
            parallaxEffect!!.setEnabled(false)
        }
        paused = true
    }

    fun onResume() {
        if (parallaxEffect != null) {
            parallaxEffect!!.setEnabled(true)
        }
        paused = false
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        notifyHeightChanged()
    }

    fun measureKeyboardHeight(): Int {
        val rootView = getRootView()
        getWindowVisibleDisplayFrame(rect)
        if (rect.bottom == 0 && rect.top == 0) {
            return 0
        }
        val usableViewHeight: Int =
            rootView.height - (if (rect.top != 0) AndroidUtils.statusBarHeight else 0) - AndroidUtils.getViewInset(
                rootView
            )
        return max(0, usableViewHeight - (rect.bottom - rect.top)).also {
            curKeyboardHeight = it
        }
    }

    fun getKeyboardHeight(): Int {
        return curKeyboardHeight
    }

    private fun notifyHeightChanged() {
        if (parallaxEffect != null) {
            parallaxScale = parallaxEffect!!.getScale(measuredWidth, measuredHeight)
        }
        if (delegate != null) {
            curKeyboardHeight = measureKeyboardHeight()
            val isWidthGreater: Boolean =
                AndroidUtils.displaySize.x > AndroidUtils.displaySize.y
            post {
                if (delegate != null) {
                    delegate!!.onSizeChanged(curKeyboardHeight, isWidthGreater)
                }
            }
        }
    }

    fun setBottomClip(value: Int) {
        if (value != bottomClip) {
            bottomClip = value
            if (backgroundView != null) {
                backgroundView!!.invalidate()
            }
        }
    }

    fun setBackgroundTranslation(translation: Int) {
        if (translation != backgroundTranslationY) {
            backgroundTranslationY = translation
            if (backgroundView != null) {
                backgroundView!!.invalidate()
            }
        }
    }

    fun getBackgroundTranslationY(): Int {
        return 0
    }

    val backgroundSizeY: Int
        get() {
            val offset = 0
            return measuredHeight - offset
        }

    val heightWithKeyboard: Int
        get() = curKeyboardHeight + measuredHeight

    fun setEmojiKeyboardHeight(height: Int) {
        if (emojiHeight != height) {
            emojiHeight = height
            if (backgroundView != null) {
                backgroundView!!.invalidate()
            }
        }
    }

    fun setEmojiOffset(animInProgress: Boolean, offset: Float) {
        if (emojiOffset != offset || animationInProgress != animInProgress) {
            emojiOffset = offset
            animationInProgress = animInProgress
            if (backgroundView != null) {
                backgroundView!!.invalidate()
            }
        }
    }

    private fun checkSnowflake(canvas: Canvas) {
    }

    protected val isActionBarVisible: Boolean
        get() = true

    fun setSkipBackgroundDrawing(skipBackgroundDrawing: Boolean) {
        if (this.skipBackgroundDrawing != skipBackgroundDrawing) {
            this.skipBackgroundDrawing = skipBackgroundDrawing
            if (backgroundView != null) {
                backgroundView!!.invalidate()
            }
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who === backgroundImage || super.verifyDrawable(who)
    }

    private fun startBlur() {
        if (!blurIsRunning || blurGeneratingTuskIsRunning || !invalidateBlur) {
            return
        }
    }

    fun invalidateBlurredViews() {
        for (i in blurBehindViews.indices) {
            blurBehindViews[i].invalidate()
        }
    }

    private val bottomOffset: Float
         get() = measuredHeight.toFloat()

    private val listTranslationY: Float
         get() = 0f

    override fun dispatchDraw(canvas: Canvas) {
        if (blurIsRunning) {
            startBlur()
        }
        super.dispatchDraw(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        if (needBlur && !blurIsRunning) {
            blurIsRunning = true
            invalidateBlur = true
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attached = false
        blurPaintTop.setShader(null)
        blurPaintTop2.setShader(null)
        blurPaintBottom.setShader(null)
        blurPaintBottom2.setShader(null)
        if (blurCrossfade != null) {
            blurCrossfade!!.cancel()
        }
        if (currentBitmap != null) {
            currentBitmap!!.recycle()
            currentBitmap = null
        }
        for (i in unusedBitmaps.indices) {
            if (unusedBitmaps[i] != null) {
                unusedBitmaps[i]!!.recycle()
            }
        }
        unusedBitmaps.clear()
        blurIsRunning = false
    }

    fun blurWasDrawn(): Boolean {
        return currentBitmap != null
    }

    fun drawBlurRect(canvas: Canvas, y: Float, rectTmp: Rect, blurScrimPaint: Paint, top: Boolean) {
    }

    fun drawBlurCircle(
        canvas: Canvas,
        viewY: Float,
        cx: Float,
        cy: Float,
        radius: Float,
        blurScrimPaint: Paint,
        top: Boolean) {
    }

    private fun updateBlurShaderPosition(viewY: Float, top: Boolean) {
        var viewY = viewY
        selectedBlurPaint = if (top) blurPaintTop else blurPaintBottom
        selectedBlurPaint2 = if (top) blurPaintTop2 else blurPaintBottom2
        if (top) {
            viewY += getTranslationY()
        }
        if (selectedBlurPaint!!.shader != null) {
            matrix1.reset()
            matrix2.reset()
            if (!top) {
                var y1 =
                    -viewY + currentBitmap!!.bottomOffset - currentBitmap!!.pixelFixOffset - TOP_CLIP_OFFSET - (currentBitmap!!.drawnLisetTranslationY - (bottomOffset + listTranslationY))
                matrix1.setTranslate(0f, y1)
                matrix1.preScale(currentBitmap!!.bottomScaleX, currentBitmap!!.bottomScaleY)
                if (prevBitmap != null) {
                    y1 =
                        -viewY + prevBitmap!!.bottomOffset - prevBitmap!!.pixelFixOffset - TOP_CLIP_OFFSET - (prevBitmap!!.drawnLisetTranslationY - (bottomOffset + listTranslationY))
                    matrix2.setTranslate(0f, y1)
                    matrix2.preScale(prevBitmap!!.bottomScaleX, prevBitmap!!.bottomScaleY)
                }
            } else {
                matrix1.setTranslate(0f, -viewY - currentBitmap!!.pixelFixOffset - TOP_CLIP_OFFSET)
                matrix1.preScale(currentBitmap!!.topScaleX, currentBitmap!!.topScaleY)
                if (prevBitmap != null) {
                    matrix2.setTranslate(0f, -viewY - prevBitmap!!.pixelFixOffset - TOP_CLIP_OFFSET)
                    matrix2.preScale(prevBitmap!!.topScaleX, prevBitmap!!.topScaleY)
                }
            }
            selectedBlurPaint!!.shader.setLocalMatrix(matrix1)
            if (selectedBlurPaint2!!.shader != null) {
                selectedBlurPaint2!!.shader.setLocalMatrix(matrix1)
            }
        }
    }

    protected val bottomTranslation: Float
         get() = 0f

    class BlurBitmap {
        var needBlurBottom = false
        var pixelFixOffset = 0
        var topCanvas: Canvas? = null
        var topBitmap: Bitmap? = null
        var topScaleX = 0f
        var topScaleY = 0f
        var bottomScaleX = 0f
        var bottomScaleY = 0f
        var bottomOffset = 0f
        var drawnLisetTranslationY = 0f
        var bottomCanvas: Canvas? = null
        var bottomBitmap: Bitmap? = null
        fun recycle() {
            topBitmap!!.recycle()
            if (bottomBitmap != null) {
                bottomBitmap!!.recycle()
            }
        }
    }

    class SimplerCanvas(bitmap: Bitmap?) : Canvas(bitmap!!) {
        // all calls to render text for blur should be replaced with drawRect using SpoilerEffect.layoutDrawMaybe
        override fun drawText(
            text: CharArray,
            index: Int,
            count: Int,
            x: Float,
            y: Float,
            paint: Paint
        ) {
            // NOP
//            super.drawText(text, index, count, x, y, paint);
        }

        override fun drawText(
            text: String,
            start: Int,
            end: Int,
            x: Float,
            y: Float,
            paint: Paint
        ) {
            // NOP
//            super.drawText(text, start, end, x, y, paint);
        }

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            // NOP
//            super.drawText(text, x, y, paint);
        }

        override fun drawText(
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            y: Float,
            paint: Paint
        ) {
            // NOP
//            super.drawText(text, start, end, x, y, paint);
        }

        override fun drawTextRun(
            text: CharSequence,
            start: Int,
            end: Int,
            contextStart: Int,
            contextEnd: Int,
            x: Float,
            y: Float,
            isRtl: Boolean,
            paint: Paint
        ) {
            // NOP
//            super.drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint);
        }

        override fun drawTextRun(
            text: MeasuredText,
            start: Int,
            end: Int,
            contextStart: Int,
            contextEnd: Int,
            x: Float,
            y: Float,
            isRtl: Boolean,
            paint: Paint
        ) {
            // NOP
//            super.drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint);
        }

        override fun drawTextRun(
            text: CharArray,
            index: Int,
            count: Int,
            contextIndex: Int,
            contextCount: Int,
            x: Float,
            y: Float,
            isRtl: Boolean,
            paint: Paint
        ) {
            // NOP
//            super.drawTextRun(text, index, count, contextIndex, contextCount, x, y, isRtl, paint);
        }

        override fun drawTextOnPath(
            text: CharArray,
            index: Int,
            count: Int,
            path: Path,
            hOffset: Float,
            vOffset: Float,
            paint: Paint
        ) {
            // NOP
//            super.drawTextOnPath(text, index, count, path, hOffset, vOffset, paint);
        }

        override fun drawTextOnPath(
            text: String,
            path: Path,
            hOffset: Float,
            vOffset: Float,
            paint: Paint
        ) {
            // NOP
//            super.drawTextOnPath(text, path, hOffset, vOffset, paint);
        }

        override fun clipPath(path: Path): Boolean {
            // NOP
            return false
            //            return super.clipPath(path);
        }

        override fun clipPath(path: Path, op: Region.Op): Boolean {
            // NOP
            return false
            //            return super.clipPath(path, op);
        }
    }
}
