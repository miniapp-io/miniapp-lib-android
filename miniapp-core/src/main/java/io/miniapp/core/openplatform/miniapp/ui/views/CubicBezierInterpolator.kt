package io.miniapp.core.openplatform.miniapp.ui.views

import android.graphics.PointF
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import androidx.core.graphics.PathParser
import kotlin.math.abs


internal class CubicBezierInterpolator(start: PointF, end: PointF) : Interpolator {
    protected var start: PointF
    protected var end: PointF
    protected var a = PointF()
    protected var b = PointF()
    protected var c = PointF()

    init {
        require(!(start.x < 0 || start.x > 1)) { "startX value must be in the range [0, 1]" }
        require(!(end.x < 0 || end.x > 1)) { "endX value must be in the range [0, 1]" }
        this.start = start
        this.end = end
    }

    constructor(startX: Float, startY: Float, endX: Float, endY: Float) : this(
        PointF(
            startX,
            startY
        ), PointF(endX, endY)
    )

    constructor(startX: Double, startY: Double, endX: Double, endY: Double) : this(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat()
    )

    override fun getInterpolation(time: Float): Float {
        return getBezierCoordinateY(getXForTime(time))
    }

    protected fun getBezierCoordinateY(time: Float): Float {
        c.y = 3 * start.y
        b.y = 3 * (end.y - start.y) - c.y
        a.y = 1 - c.y - b.y
        return time * (c.y + time * (b.y + time * a.y))
    }

    protected fun getXForTime(time: Float): Float {
        var x = time
        var z: Float
        for (i in 1..13) {
            z = getBezierCoordinateX(x) - time
            if (abs(z.toDouble()) < 1e-3) {
                break
            }
            x -= z / getXDerivate(x)
        }
        return x
    }

    private fun getXDerivate(t: Float): Float {
        return c.x + t * (2 * b.x + 3 * a.x * t)
    }

    private fun getBezierCoordinateX(time: Float): Float {
        c.x = 3 * start.x
        b.x = 3 * (end.x - start.x) - c.x
        a.x = 1 - c.x - b.x
        return time * (c.x + time * (b.x + time * a.x))
    }

    companion object {
        val DEFAULT = CubicBezierInterpolator(0.25, 0.1, 0.25, 1.0)
        val EASE_OUT = CubicBezierInterpolator(0.0, 0.0, .58, 1.0)
        val EASE_OUT_QUINT = CubicBezierInterpolator(.23, 1.0, .32, 1.0)
        val EASE_IN = CubicBezierInterpolator(.42, 0.0, 1.0, 1.0)
        val EASE_BOTH = CubicBezierInterpolator(.42, 0.0, .58, 1.0)
        val EASE_OUT_BACK = CubicBezierInterpolator(.34, 1.56, .64, 1.0)
        val Emphasized: Interpolator = PathInterpolator(PathParser.createPathFromPathData("M 0,0 C 0.05, 0, 0.133333, 0.06, 0.166666, 0.4 C 0.208333, 0.82, 0.25, 1, 1, 1"))
    }
}
