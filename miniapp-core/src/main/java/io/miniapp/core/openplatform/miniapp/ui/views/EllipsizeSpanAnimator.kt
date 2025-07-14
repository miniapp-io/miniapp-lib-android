package io.miniapp.core.openplatform.miniapp.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.view.View


internal class EllipsizeSpanAnimator(parentView: View) {
    private val ellSpans = arrayOf(TextAlphaSpan(), TextAlphaSpan(), TextAlphaSpan())
    private val ellAnimator: AnimatorSet
    var attachedToWindow = false
    var ellipsizedViews = ArrayList<View>()

    init {
        ellAnimator = AnimatorSet()
        ellAnimator.playTogether(
            createEllipsizeAnimator(ellSpans[0], 0, 255, 0, 300),
            createEllipsizeAnimator(ellSpans[1], 0, 255, 150, 300),
            createEllipsizeAnimator(ellSpans[2], 0, 255, 300, 300),
            createEllipsizeAnimator(ellSpans[0], 255, 0, 1000, 400),
            createEllipsizeAnimator(ellSpans[1], 255, 0, 1000, 400),
            createEllipsizeAnimator(ellSpans[2], 255, 0, 1000, 400)
        )
        ellAnimator.addListener(object : AnimatorListenerAdapter() {
            private val restarter = Runnable {
                if (attachedToWindow && !ellipsizedViews.isEmpty() && !ellAnimator.isRunning()) {
                    try {
                        ellAnimator.start()
                    } catch (ignored: Exception) {
                    }
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                if (attachedToWindow) {
                    parentView.postDelayed(restarter, 300)
                }
            }
        })
    }

    fun wrap(string: SpannableString, start: Int) {
        string.setSpan(ellSpans[0], start, start + 1, 0)
        string.setSpan(ellSpans[1], start + 1, start + 2, 0)
        string.setSpan(ellSpans[2], start + 2, start + 3, 0)
    }

    fun onAttachedToWindow() {
        attachedToWindow = true
        if (!ellAnimator.isRunning()) {
            ellAnimator.start()
        }
    }

    fun onDetachedFromWindow() {
        attachedToWindow = false
        ellAnimator.cancel()
    }

    fun reset() {
        for (s in ellSpans) {
            s.setAlpha(0)
        }
    }

    private fun createEllipsizeAnimator(
        target: TextAlphaSpan,
        startVal: Int,
        endVal: Int,
        startDelay: Int,
        duration: Int
    ): Animator {
        val a = ValueAnimator.ofInt(startVal, endVal)
        a.addUpdateListener { valueAnimator: ValueAnimator ->
            target.setAlpha(valueAnimator.getAnimatedValue() as Int)
            for (i in ellipsizedViews.indices) {
                if (!HwEmojis.isHwEnabled) {
                    ellipsizedViews[i].invalidate()
                }
            }
        }
        a.setDuration(duration.toLong())
        a.setStartDelay(startDelay.toLong())
        a.interpolator = CubicBezierInterpolator.DEFAULT
        return a
    }

    fun addView(view: View) {
        if (ellipsizedViews.isEmpty()) {
            ellAnimator.start()
        }
        if (!ellipsizedViews.contains(view)) {
            ellipsizedViews.add(view)
        }
    }

    fun removeView(view: View) {
        ellipsizedViews.remove(view)
        if (ellipsizedViews.isEmpty()) {
            ellAnimator.cancel()
        }
    }

    private class TextAlphaSpan : CharacterStyle() {
        private var alpha = 0
        fun setAlpha(alpha: Int) {
            this.alpha = alpha
        }

        override fun updateDrawState(tp: TextPaint) {
            tp.setAlpha((tp.alpha * (alpha / 255f)).toInt())
        }
    }
}