package io.miniapp.core.openplatform.miniapp.utils

import android.R
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.util.StateSet
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

internal object ThemeUtils {

    const val RIPPLE_MASK_CIRCLE_20DP = 1
    const val RIPPLE_MASK_ALL = 2
    const val RIPPLE_MASK_CIRCLE_TO_BOUND_EDGE = 3
    const val RIPPLE_MASK_CIRCLE_TO_BOUND_CORNER = 4
    const val RIPPLE_MASK_CIRCLE_AUTO = 5
    const val RIPPLE_MASK_ROUNDRECT_6DP = 7

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun createSelectorDrawable(color: Int): Drawable {
        return createSelectorDrawable(color, RIPPLE_MASK_CIRCLE_20DP, -1)
    }

    fun createSelectorDrawable(color: Int, maskType: Int): Drawable {
        return createSelectorDrawable(color, maskType, -1)
    }
    fun createSelectorDrawable(color: Int, maskType: Int, radius: Int): Drawable {
        return if (Build.VERSION.SDK_INT >= 21) {
            var maskDrawable: Drawable? = null
            if ((maskType == RIPPLE_MASK_CIRCLE_20DP || maskType == 5) && Build.VERSION.SDK_INT >= 23) {
                maskDrawable = null
            } else if (maskType == RIPPLE_MASK_CIRCLE_20DP || maskType == RIPPLE_MASK_CIRCLE_TO_BOUND_EDGE || maskType == RIPPLE_MASK_CIRCLE_TO_BOUND_CORNER || maskType == RIPPLE_MASK_CIRCLE_AUTO || maskType == 6 || maskType == RIPPLE_MASK_ROUNDRECT_6DP) {
                maskPaint.setColor(-0x1)
                maskDrawable = object : Drawable() {
                    var rect: RectF? = null
                    override fun draw(canvas: Canvas) {
                        val bounds = getBounds()
                        if (maskType == RIPPLE_MASK_ROUNDRECT_6DP) {
                            if (rect == null) {
                                rect = RectF()
                            }
                            rect!!.set(bounds)
                            val rad = if (radius <= 0) AndroidUtils.dp(6) else radius.toFloat()
                            canvas.drawRoundRect(rect!!, rad.toFloat(), rad.toFloat(), maskPaint)
                        } else {
                            val rad: Int
                            rad = if (maskType == RIPPLE_MASK_CIRCLE_20DP || maskType == 6) {
                                if (radius <= 0) AndroidUtils.dp(20) else radius
                            } else if (maskType == RIPPLE_MASK_CIRCLE_TO_BOUND_EDGE) {
                                (max(
                                    bounds.width().toDouble(),
                                    bounds.height().toDouble()
                                ) / 2).toInt()
                            } else {
                                // RIPPLE_MASK_CIRCLE_AUTO = 5
                                // RIPPLE_MASK_CIRCLE_TO_BOUND_CORNER = 4
                                ceil(sqrt(((bounds.left - bounds.centerX()) * (bounds.left - bounds.centerX()) + (bounds.top - bounds.centerY()) * (bounds.top - bounds.centerY())).toDouble()))
                                    .toInt()
                            }
                            canvas.drawCircle(
                                bounds.centerX().toFloat(),
                                bounds.centerY().toFloat(),
                                rad.toFloat(),
                                maskPaint
                            )
                        }
                    }

                    override fun setAlpha(alpha: Int) {}
                    override fun setColorFilter(colorFilter: ColorFilter?) {}
                    override fun getOpacity(): Int {
                        return PixelFormat.UNKNOWN
                    }
                }
            } else if (maskType == RIPPLE_MASK_ALL) {
                maskDrawable = ColorDrawable(-0x1)
            }
            val colorStateList = ColorStateList(arrayOf(StateSet.WILD_CARD), intArrayOf(color))
            val rippleDrawable = RippleDrawable(colorStateList, null, maskDrawable)
            if (Build.VERSION.SDK_INT >= 23) {
                if (maskType == RIPPLE_MASK_CIRCLE_20DP) {
                    rippleDrawable.setRadius(if (radius <= 0) AndroidUtils.dp(20) else radius)
                } else if (maskType == RIPPLE_MASK_CIRCLE_AUTO) {
                    rippleDrawable.setRadius(RippleDrawable.RADIUS_AUTO)
                }
            }
            rippleDrawable
        } else {
            val stateListDrawable = StateListDrawable()
            stateListDrawable.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(color))
            stateListDrawable.addState(intArrayOf(android.R.attr.state_selected), ColorDrawable(color))
            stateListDrawable.addState(StateSet.WILD_CARD, ColorDrawable(0x00000000))
            stateListDrawable
        }
    }

    fun createSelectorWithBackgroundDrawable(backgroundColor: Int, color: Int): Drawable {
        return if (Build.VERSION.SDK_INT >= 21) {
            val maskDrawable: Drawable = ColorDrawable(backgroundColor)
            val colorStateList = ColorStateList(arrayOf(StateSet.WILD_CARD), intArrayOf(color))
            RippleDrawable(colorStateList, ColorDrawable(backgroundColor), maskDrawable)
        } else {
            val stateListDrawable = StateListDrawable()
            stateListDrawable.addState(intArrayOf(R.attr.state_pressed), ColorDrawable(color))
            stateListDrawable.addState(intArrayOf(R.attr.state_selected), ColorDrawable(color))
            stateListDrawable.addState(StateSet.WILD_CARD, ColorDrawable(backgroundColor))
            stateListDrawable
        }
    }
}