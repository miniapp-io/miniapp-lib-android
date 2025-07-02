package io.miniapp.core.openplatform.miniapp.ui.views

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.math.MathUtils
import androidx.core.view.GestureDetectorCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import io.miniapp.core.openplatform.common.data.GenericProvider
import io.miniapp.core.openplatform.miniapp.ui.webview.DefaultAppWebView
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import kotlin.math.abs
import kotlin.math.max

internal open class WebViewSwipeContainer(context: Context) : FrameLayout(context) {
    private val gestureDetector: GestureDetectorCompat
    var isSwipeInProgress = false
    var enableGesture = true
    private var isSwipeDisallowed = false
    private var topActionBarOffsetY = AndroidUtils.getCurrentActionBarHeight(context)
    private var offsetY = 0f
    private var pendingOffsetY = -1f
    private var pendingSwipeOffsetY = Int.MIN_VALUE
    private var swipeOffsetY = 0f
    private var isSwipeOffsetAnimationDisallowed = false
    private var offsetYAnimator: SpringAnimation? = null
    private var flingInProgress = false
    private var webView: DefaultAppWebView? = null
    private var scrollListener: Runnable? = null
    private var scrollEndListener: Runnable? = null
    private var delegate: Delegate? = null
    private var scrollAnimator: SpringAnimation? = null
    private var swipeStickyRange = 0
    private var isKeyboardVisible: GenericProvider<Any?, Boolean> = GenericProvider { _ -> false }

    private var shouldWaitWebViewScroll = true
    private var allowedScrollX = false
    private var allowedScrollY: Boolean = false
    fun setShouldWaitWebViewScroll(value: Boolean) {
        shouldWaitWebViewScroll = value
    }

    fun allowThisScroll(x: Boolean, y: Boolean) {
        allowedScrollX = x
        allowedScrollY = y
    }

    fun allowingScroll(x: Boolean): Boolean {
        return webView == null || false == webView?.injectedJS || if (x) allowedScrollX else allowedScrollY
    }

    companion object {
        val SWIPE_OFFSET_Y = object : FloatPropertyCompat<WebViewSwipeContainer>("swipeOffsetY") {
            override fun getValue(`object`: WebViewSwipeContainer): Float {
                // Logic for returning property value
                return `object`.getSwipeOffsetY()
            }

            override fun setValue(`object`: WebViewSwipeContainer, value: Float) {
                // Logic for setting property value
                `object`.setSwipeOffsetY(value)
            }
        }
    }

    init {
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        gestureDetector = GestureDetectorCompat(context, object : SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (isSwipeDisallowed || (shouldWaitWebViewScroll && !allowingScroll(false))) {
                    return false
                }
                if (velocityY >= 700 && (webView == null || webView!!.scrollY == 0)) {
                    flingInProgress = true
                    if (swipeOffsetY >= swipeStickyRange) {
                        if (delegate != null) {
                            delegate!!.onDismiss()
                        }
                    } else {
                        stickTo(0f)
                    }
                    return true
                } else if (velocityY <= -700 && swipeOffsetY > -offsetY + topActionBarOffsetY) {
                    flingInProgress = true
                    stickTo(-offsetY + topActionBarOffsetY)
                    return true
                }
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!isSwipeInProgress && !isSwipeDisallowed && enableGesture && (!shouldWaitWebViewScroll || swipeOffsetY != -offsetY + topActionBarOffsetY || allowingScroll(false))) {
                    if (isKeyboardVisible.provide(null) && swipeOffsetY == -offsetY + topActionBarOffsetY) {
                        isSwipeDisallowed = true
                    } else if (abs(distanceY.toDouble()) >= touchSlop && abs(distanceY.toDouble()) * 1.5f >= abs(
                            distanceX.toDouble()
                        ) && (swipeOffsetY != -offsetY + topActionBarOffsetY || webView == null || distanceY < 0 && webView!!.scrollY == 0)
                    ) {
                        isSwipeInProgress = true
                        val ev = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
                        for (i in 0 until childCount) {
                            getChildAt(i).dispatchTouchEvent(ev)
                        }
                        ev.recycle()
                        return true
                    } else if (webView != null && webView!!.canScrollHorizontally(if (distanceX >= 0) 1 else -1) || abs(
                            distanceX.toDouble()
                        ) >= touchSlop && abs(distanceX.toDouble()) * 1.5f >= abs(distanceY.toDouble())
                    ) {
                        isSwipeDisallowed = true
                    }
                }
                if (isSwipeInProgress) {
                    if (distanceY < 0) {
                        if (swipeOffsetY > -offsetY + topActionBarOffsetY) {
                            swipeOffsetY -= distanceY
                        } else if (webView != null) {
                            val newWebScrollY = webView!!.scrollY + distanceY
                            webView!!.scrollY =
                                MathUtils.clamp(
                                    newWebScrollY, 0f, max(
                                        webView!!.getContentHeight(), webView!!.height
                                    ).toFloat() - topActionBarOffsetY
                                ).toInt()
                            if (newWebScrollY < 0) {
                                swipeOffsetY -= newWebScrollY
                            }
                        } else {
                            swipeOffsetY -= distanceY
                        }
                    } else {
                        swipeOffsetY -= distanceY
                        if (webView != null && swipeOffsetY < -offsetY + topActionBarOffsetY) {
                            val newWebScrollY = webView!!.scrollY - (swipeOffsetY + offsetY - topActionBarOffsetY)
                            webView!!.scrollY =
                                MathUtils.clamp(
                                    newWebScrollY, 0f, max(
                                        webView!!.getContentHeight(), webView!!.height
                                    ).toFloat() - topActionBarOffsetY
                                ).toInt()
                        }
                    }
                    swipeOffsetY = MathUtils.clamp(
                        swipeOffsetY,
                        -offsetY + topActionBarOffsetY,
                        height - offsetY + topActionBarOffsetY
                    )
                    invalidateTranslation()
                    return true
                }
                return true
            }
        })
        updateStickyRange()
    }

    fun setIsKeyboardVisible(isKeyboardVisible: GenericProvider<Any?, Boolean>) {
        this.isKeyboardVisible = isKeyboardVisible
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateStickyRange()
    }

    private fun updateStickyRange() {
        swipeStickyRange = AndroidUtils.dp( if (AndroidUtils.displaySize.x > AndroidUtils.displaySize.y) 8f else 64f)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
        if (disallowIntercept) {
            isSwipeDisallowed = true
            isSwipeInProgress = false
        }
    }

    fun setSwipeOffsetAnimationDisallowed(swipeOffsetAnimationDisallowed: Boolean) {
        isSwipeOffsetAnimationDisallowed = swipeOffsetAnimationDisallowed
    }

    fun setScrollListener(scrollListener: Runnable?) {
        this.scrollListener = scrollListener
    }

    fun setScrollEndListener(scrollEndListener: Runnable?) {
        this.scrollEndListener = scrollEndListener
    }

    fun setWebView(webView: DefaultAppWebView?) {
        this.webView = webView
    }

    fun setTopActionBarOffsetY(topActionBarOffsetY: Int) {
        this.topActionBarOffsetY = topActionBarOffsetY
        invalidateTranslation()
    }

    fun setSwipeOffsetY(swipeOffsetY: Float) {
        this.swipeOffsetY = swipeOffsetY
        invalidateTranslation()
    }

    fun setOffsetY(offsetY: Float) {
        if (pendingSwipeOffsetY != Int.MIN_VALUE) {
            pendingOffsetY = offsetY
            return
        }
        if (offsetYAnimator != null) {
            offsetYAnimator!!.cancel()
        }
        val wasOffsetY = this.offsetY
        val deltaOffsetY = offsetY - wasOffsetY
        val wasOnTop: Boolean =
            abs((swipeOffsetY + wasOffsetY - topActionBarOffsetY).toDouble()) <= AndroidUtils.dp(1f)
        if (!isSwipeOffsetAnimationDisallowed) {
            if (offsetYAnimator != null) {
                offsetYAnimator!!.cancel()
            }
            offsetYAnimator = SpringAnimation(FloatValueHolder(wasOffsetY))
                .setSpring(
                    SpringForce(offsetY)
                        .setStiffness(1400f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                )
                .addUpdateListener { animation: DynamicAnimation<*>?, value: Float, velocity: Float ->
                    this.offsetY = value
                    val progress = (value - wasOffsetY) / deltaOffsetY
                    if (wasOnTop) {
                        swipeOffsetY = MathUtils.clamp(
                            swipeOffsetY - progress * max(
                                0f,
                                deltaOffsetY
                            ),
                            -this.offsetY + topActionBarOffsetY,
                            height - this.offsetY + topActionBarOffsetY
                        )
                    }
                    if (scrollAnimator != null && scrollAnimator!!.spring
                            .finalPosition == -wasOffsetY + topActionBarOffsetY
                    ) {
                        scrollAnimator!!.spring.setFinalPosition(-offsetY + topActionBarOffsetY)
                    }
                    invalidateTranslation()
                }
                .addEndListener { animation: DynamicAnimation<*>?, canceled: Boolean, value: Float, velocity: Float ->
                    offsetYAnimator = null
                    if (!canceled) {
                        this@WebViewSwipeContainer.offsetY = offsetY
                        invalidateTranslation()
                    } else {
                        pendingOffsetY = offsetY
                    }
                }
            offsetYAnimator?.start()
        } else {
            this.offsetY = offsetY
            if (wasOnTop) {
                swipeOffsetY = MathUtils.clamp(
                    swipeOffsetY - max(0f, deltaOffsetY),
                    -this.offsetY + topActionBarOffsetY,
                    height - this.offsetY + topActionBarOffsetY
                )
            }
            invalidateTranslation()
        }
    }

    private fun invalidateTranslation() {
        translationY = max(topActionBarOffsetY.toFloat(), offsetY + swipeOffsetY)
        scrollListener?.run()
//        if (Bulletin.getVisibleBulletin() != null) {
//            val bulletin: Bulletin = Bulletin.getVisibleBulletin()
//            bulletin.updatePosition()
//        }
    }

    fun getTopActionBarOffsetY(): Int {
        return topActionBarOffsetY
    }

    fun getOffsetY(): Float {
        return offsetY
    }

    fun getSwipeOffsetY(): Float {
        return swipeOffsetY
    }

    fun setDelegate(delegate: Delegate?) {
        this.delegate = delegate
    }

    fun setGesture(enable:Boolean) {
        enableGesture = enable
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isSwipeInProgress && ev.actionIndex != 0) {
            return false
        }

        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (shouldWaitWebViewScroll) {
                allowedScrollX = false
                allowedScrollY = false
            }
        }

        if(!enableGesture) {
            return super.dispatchTouchEvent(ev)
        }

        val rawEvent = MotionEvent.obtain(ev)
        val index = ev.actionIndex
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            rawEvent.setLocation(ev.getRawX(index), ev.getRawY(index))
        } else {
            val offsetX = ev.rawX - ev.x
            val offsetY = ev.rawY - ev.y
            rawEvent.setLocation(ev.getX(index) + offsetX, ev.getY(index) + offsetY)
        }
        val detector = gestureDetector.onTouchEvent(rawEvent)
        rawEvent.recycle()
        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            isSwipeDisallowed = false
            isSwipeInProgress = false
            if (flingInProgress) {
                flingInProgress = false
            } else if (enableGesture && (!shouldWaitWebViewScroll || (swipeOffsetY != -offsetY+topActionBarOffsetY) || allowingScroll(false))) {
                if (swipeOffsetY <= -swipeStickyRange) {
                    stickTo(-offsetY + topActionBarOffsetY)
                } else if (swipeOffsetY > -swipeStickyRange && swipeOffsetY <= swipeStickyRange) {
                    stickTo(0f)
                } else {
                    if (delegate != null) {
                        delegate!!.onDismiss()
                    }
                }
            }
        }
        val superTouch = super.dispatchTouchEvent(ev)
        return if (!superTouch && !detector && ev.action == MotionEvent.ACTION_DOWN) {
            true
        } else superTouch || detector
    }

    @JvmOverloads
    fun stickTo(offset: Float, callback: Runnable? = null) {
        if (swipeOffsetY == offset || scrollAnimator != null && scrollAnimator!!.spring.finalPosition == offset) {
            callback?.run()
            if (scrollEndListener != null) {
                scrollEndListener!!.run()
            }
            return
        }
        pendingSwipeOffsetY = offset.toInt()
        if (offsetYAnimator != null) {
            offsetYAnimator!!.cancel()
        }
        if (scrollAnimator != null) {
            scrollAnimator!!.cancel()
        }
        scrollAnimator = SpringAnimation(this, SWIPE_OFFSET_Y, offset)
            .setSpring(
                SpringForce(offset)
                    .setStiffness(1400f)
                    .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
            )
            .addEndListener { animation, canceled, value, velocity ->
                if (animation === scrollAnimator) {
                    scrollAnimator = null
                    callback?.run()
                    if (scrollEndListener != null) {
                        scrollEndListener!!.run()
                    }
                    if (pendingOffsetY != -1f) {
                        val wasDisallowed = isSwipeOffsetAnimationDisallowed
                        isSwipeOffsetAnimationDisallowed = true
                        setOffsetY(pendingOffsetY)
                        pendingOffsetY = -1f
                        isSwipeOffsetAnimationDisallowed = wasDisallowed
                    }
                    pendingSwipeOffsetY = Int.MIN_VALUE
                }
            }
        scrollAnimator?.start()
    }

    fun interface Delegate {
        /**
         * Called to dismiss parent layout
         */
        fun onDismiss()
    }

}
