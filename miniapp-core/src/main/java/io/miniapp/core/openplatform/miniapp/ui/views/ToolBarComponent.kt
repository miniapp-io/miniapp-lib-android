package io.miniapp.core.openplatform.miniapp.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.ImageViewCompat
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.IResourcesProvider
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils

@SuppressLint("ViewConstructor")
internal class ToolBarComponent @JvmOverloads constructor(
    val resourcesProvider: IResourcesProvider,
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val shareActionButton: ImageButton
    private val miniSizeButton: ImageButton
    private val closeButton: ImageButton
    private val verticalLine1: View
    private val verticalLine2: View

    var dismiss: (() -> Unit)? = null
    var share: ((View) -> Unit)? = null
    var mini: (() -> Unit)? = null

    private val bthHeight = AndroidUtils.dp(30)
    private val bthMargin = AndroidUtils.dp(5)

    init {
        // Set rounded corner background
        background = if (resourcesProvider.isDark()) {
            AppCompatResources.getDrawable(context, R.drawable.toolbar_component_background_night)
        } else {
            AppCompatResources.getDrawable(context, R.drawable.toolbar_component_background)
        }

        shareActionButton = ImageButton(context).apply {
            id = R.id.actionShare
            layoutParams = LayoutParams(bthHeight, bthHeight).apply {
                startToStart = LayoutParams.PARENT_ID
                endToStart = R.id.actionMiniButton
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
            //setColorFilter(Color.YELLOW)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(R.drawable.ic_action_share)
            val tintColor = resourcesProvider.getColor("text_color")
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tintColor))
            setPadding(bthMargin, bthMargin, bthMargin, bthMargin)
        }
        addView(shareActionButton)
        shareActionButton.setOnClickListener {
            sharePressed()
        }

        miniSizeButton = ImageButton(context).apply {
            id = R.id.actionMiniButton
            layoutParams = LayoutParams(bthHeight, bthHeight).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(R.drawable.ic_action_minisize)
            val tintColor = resourcesProvider.getColor("text_color")
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tintColor))
            setPadding(bthMargin, bthMargin, bthMargin, bthMargin)
        }
        addView(miniSizeButton)
        miniSizeButton.setOnClickListener { miniSizePressed() }

        closeButton = ImageButton(context).apply {
            id = R.id.actionCloseButton
            layoutParams = LayoutParams(bthHeight, bthHeight).apply {
                startToEnd = R.id.actionMiniButton
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(R.drawable.ic_action_close)
            val tintColor = resourcesProvider.getColor("text_color")
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tintColor))
            setPadding(bthMargin, bthMargin, bthMargin, bthMargin)
        }
        addView(closeButton)
        closeButton.setOnClickListener { closePressed() }

        verticalLine1 = View(context).apply {
            layoutParams = LayoutParams(1, LayoutParams.MATCH_PARENT).apply {
                setMargins(0, bthMargin, 0, bthMargin)
                startToEnd = R.id.actionShare
                endToStart = R.id.actionMiniButton
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
            setBackgroundColor(resourcesProvider.getColor("text_color"))
            alpha = 0.3f
        }
        addView(verticalLine1)

        verticalLine2 = View(context).apply {
            layoutParams = LayoutParams(1, LayoutParams.MATCH_PARENT).apply {
                setMargins(0, bthMargin, 0, bthMargin)
                startToEnd = R.id.actionMiniButton
                endToStart = R.id.actionCloseButton
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
            setBackgroundColor(resourcesProvider.getColor("text_color"))
            alpha = 0.3f
        }
        addView(verticalLine2)
    }

    private fun closePressed() {
        dismiss?.invoke()
    }

    private fun miniSizePressed() {
        mini?.invoke()
    }

    private fun sharePressed() {
        share?.invoke(shareActionButton)
    }
}