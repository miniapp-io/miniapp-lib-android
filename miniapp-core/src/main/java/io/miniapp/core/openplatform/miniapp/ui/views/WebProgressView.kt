package io.miniapp.core.openplatform.miniapp.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import io.miniapp.core.openplatform.miniapp.IResourcesProvider
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils

@SuppressLint("ViewConstructor")
internal open class WebProgressView(context: Context, private val resourcesProvider: IResourcesProvider) : View(context) {

    private val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var loadProgress = 0f
    private var springAnimation: SpringAnimation? = null

    init {
        bluePaint.setColor(Color.parseColor("#0000ff"))
        bluePaint.style = Paint.Style.STROKE
        bluePaint.strokeWidth = AndroidUtils.dpf2(2f)
        bluePaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        springAnimation = SpringAnimation(this, LOAD_PROGRESS_PROPERTY)
            .setSpring(
                SpringForce()
                    .setStiffness(400f)
                    .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
            )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        springAnimation!!.cancel()
        springAnimation = null
    }

    fun setLoadProgressAnimated(loadProgress: Float) {
        if (springAnimation == null) {
            setLoadProgress(loadProgress)
            return
        }
        springAnimation!!.spring.setFinalPosition(loadProgress * 100f)
        springAnimation!!.start()
    }

    fun setLoadProgress(loadProgress: Float) {
        this.loadProgress = loadProgress
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val y = height - bluePaint.strokeWidth / 2f
        canvas.drawLine(0f, y, width * loadProgress, y, bluePaint)
    }

    companion object {
        private val LOAD_PROGRESS_PROPERTY = object : FloatPropertyCompat<WebProgressView>("loadProgress") {
            override fun getValue(`object`: WebProgressView): Float {
                // Logic for returning property value
                return `object`.loadProgress * 100f
            }

            override fun setValue(`object`: WebProgressView, value: Float) {
                // Logic for setting property value
                `object`.setLoadProgress(value/100f)
            }
        }
    }
}