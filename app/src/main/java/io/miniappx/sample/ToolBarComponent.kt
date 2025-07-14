package io.miniappx.sample

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import io.miniapp.core.R
import androidx.appcompat.content.res.AppCompatResources
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils

internal class ToolBarComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val loadingIndicator: ImageButton
    private val ratingLabel: TextView
    private val starImageView: ImageView
    private val imageButton: ImageButton
    private val verticalLine: View

    var dismiss: (() -> Unit)? = null
    var share: (() -> Unit)? = null

    var isLoading: Boolean = true
        set(value) {
            field = value
            updateUI()
        }

    init {
        //setBackgroundColor(Color.parseColor("#4D000000"))
        // Set rounded corner background
        background = AppCompatResources.getDrawable(context, R.drawable.toolbar_component_background)

        loadingIndicator = ImageButton(context).apply {
            layoutParams = LayoutParams(AndroidUtils.dp(35), AndroidUtils.dp(35)).apply {
                setMargins(AndroidUtils.dp(8), 0, 0, 0)
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
            //setColorFilter(Color.YELLOW)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(R.drawable.icon_share)
            setPadding(AndroidUtils.dp(8), AndroidUtils.dp(8), AndroidUtils.dp(8), AndroidUtils.dp(8))
        }
        addView(loadingIndicator)
        loadingIndicator.setOnClickListener {
            sharePressed()
        }

        ratingLabel = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(8, 0, 0, 0)
            }
            textSize = 15f
            setTextColor(Color.WHITE)
        }
        addView(ratingLabel)

        starImageView = ImageView(context).apply {
            layoutParams = LayoutParams(AndroidUtils.dp(15), AndroidUtils.dp(15)).apply {
                setMargins(AndroidUtils.dp(8), 0, 0, 0)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(R.drawable.icon_star)
            //setColorFilter(Color.YELLOW)
        }
        addView(starImageView)

        imageButton = ImageButton(context).apply {
            layoutParams = LayoutParams(AndroidUtils.dp(35), AndroidUtils.dp(35)).apply {
                setMargins(0, 0, AndroidUtils.dp(8), 0)
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(R.drawable.icon_close)
            setPadding(AndroidUtils.dp(8), AndroidUtils.dp(8), AndroidUtils.dp(8), AndroidUtils.dp(8))
        }
        addView(imageButton)

        imageButton.setOnClickListener { closePressed() }

        verticalLine = View(context).apply {
            layoutParams = LayoutParams(1, LayoutParams.MATCH_PARENT).apply {
                setMargins(0, AndroidUtils.dp(10), 0, AndroidUtils.dp(10))
                gravity = Gravity.CENTER
            }
            setBackgroundColor(Color.WHITE)
            alpha = 0.3f
        }
        addView(verticalLine)

        updateUI()
    }

    private fun updateUI() {
        if (isLoading) {
            loadingIndicator.visibility = View.VISIBLE
            ratingLabel.visibility = View.GONE
            starImageView.visibility = View.GONE
        } else {
            loadingIndicator.visibility = View.GONE
            ratingLabel.visibility = View.VISIBLE
            starImageView.visibility = View.VISIBLE
        }
    }

    private fun closePressed() {
        dismiss?.invoke()
    }

    private fun sharePressed() {
        share?.invoke()
    }
}