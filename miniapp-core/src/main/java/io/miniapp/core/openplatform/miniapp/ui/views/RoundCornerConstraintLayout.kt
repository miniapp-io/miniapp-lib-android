package io.miniapp.core.openplatform.miniapp.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import androidx.constraintlayout.widget.ConstraintLayout

class RoundCornerConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var cornerRadius = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        10f,
        context.resources.displayMetrics
    )

    private val clipPath = Path()

    override fun dispatchDraw(canvas: Canvas) {
        clipPath.reset()
        clipPath.addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(clipPath)

        super.dispatchDraw(canvas)
    }
}