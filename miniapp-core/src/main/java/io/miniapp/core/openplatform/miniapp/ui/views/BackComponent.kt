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
import androidx.core.graphics.toColorInt

@SuppressLint("ViewConstructor")
internal class BackComponent @JvmOverloads constructor(
    val resourcesProvider: IResourcesProvider,
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val backActionButton: ImageButton
    var back: (() -> Unit)? = null

    private val bthHeight = AndroidUtils.dp(30)
    private val bthMargin = AndroidUtils.dp(5)

    init {
        backActionButton = ImageButton(context).apply {
            id = R.id.actionShare
            layoutParams = LayoutParams(bthHeight, bthHeight).apply {
                startToStart = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
            //setColorFilter(Color.YELLOW)
            scaleType = ImageView.ScaleType.FIT_CENTER

            setBackgroundResource(R.drawable.bg_rounded_circle)
            // Set rounded corner background
            val colorBgBack = "#4D000000".toColorInt()
            backgroundTintList = ColorStateList.valueOf(colorBgBack)

            setImageResource(R.drawable.ic_action_back)
            val tintColor = "#FFFFFF".toColorInt()
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tintColor))
            setPadding(bthMargin, bthMargin, bthMargin, bthMargin)
        }
        addView(backActionButton)
        backActionButton.setOnClickListener {
            backPressed()
        }
    }

    private fun backPressed() {
        back?.invoke()
    }
}