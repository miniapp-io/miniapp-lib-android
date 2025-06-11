package io.miniapp.core.openplatform.miniapp.ui.views

import android.view.View
import android.view.View.OnLayoutChangeListener
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils


class VerticalPositionAutoAnimator private constructor(
    private val floatingButtonView: View,
    springStiffness: Float
) {
    private val animatorLayoutChangeListener: AnimatorLayoutChangeListener
    private var floatingButtonAnimator: SpringAnimation? = null
    private var offsetY = 0f
    fun addUpdateListener(onAnimationUpdateListener: DynamicAnimation.OnAnimationUpdateListener?) {
        floatingButtonAnimator!!.addUpdateListener(onAnimationUpdateListener)
    }

    fun setOffsetY(offsetY: Float) {
        this.offsetY = offsetY
        if (floatingButtonAnimator!!.isRunning) {
            floatingButtonAnimator!!.spring.setFinalPosition(offsetY)
        } else floatingButtonView.translationY = offsetY
    }

    fun getOffsetY(): Float {
        return offsetY
    }

    init {
        animatorLayoutChangeListener =
            AnimatorLayoutChangeListener(floatingButtonView, springStiffness)
        floatingButtonView.addOnLayoutChangeListener(animatorLayoutChangeListener)
    }

    fun ignoreNextLayout() {
        animatorLayoutChangeListener.ignoreNextLayout = true
    }

    private inner class AnimatorLayoutChangeListener(view: View?, springStiffness: Float) :
        OnLayoutChangeListener {
        private var orientation: Boolean? = null
        var ignoreNextLayout = false

        init {
            floatingButtonAnimator = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y, offsetY)
            floatingButtonAnimator!!.spring.setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
            floatingButtonAnimator!!.spring.setStiffness(springStiffness)
        }

        override fun onLayoutChange(
            v: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            checkOrientation()
            if (oldTop == 0 || oldTop == top || ignoreNextLayout) {
                ignoreNextLayout = false
                return
            }
            floatingButtonAnimator!!.cancel()
            if (v.visibility != View.VISIBLE) {
                v.translationY = offsetY
                return
            }
            floatingButtonAnimator!!.spring.setFinalPosition(offsetY)
            v.translationY = oldTop - top + offsetY
            floatingButtonAnimator!!.start()
        }

        private fun checkOrientation() {
            val orientation: Boolean =
                AndroidUtils.displaySize.x > AndroidUtils.displaySize.y
            if (this.orientation == null || this.orientation != orientation) {
                this.orientation = orientation
                ignoreNextLayout = true
            }
        }
    }

    companion object {
        @JvmOverloads
        fun attach(
            floatingButtonView: View,
            springStiffness: Float = 350f
        ): VerticalPositionAutoAnimator {
            return VerticalPositionAutoAnimator(floatingButtonView, springStiffness)
        }
    }
}
