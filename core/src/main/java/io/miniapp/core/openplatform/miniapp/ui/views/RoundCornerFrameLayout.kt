package io.miniapp.core.openplatform.miniapp.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import io.miniapp.core.R

class RoundCornerFrameLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var cornerRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            16f,
            context.resources.displayMetrics
    )

    var useCornerRadius: Boolean = true
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.OW3ViewContainer) {
            cornerRadius = getDimension(R.styleable.OW3ViewContainer_cornerRadius, cornerRadius)
        }
    }

    private val clipPath = Path()

    override fun dispatchDraw(canvas: Canvas) {
        if (useCornerRadius) {
            clipPath.reset()
            clipPath.addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
        }
        super.dispatchDraw(canvas)
    }
}
