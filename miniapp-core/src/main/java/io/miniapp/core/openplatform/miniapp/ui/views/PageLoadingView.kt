package io.miniapp.core.openplatform.miniapp.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.IResourcesProvider

@SuppressLint("ViewConstructor")
internal class PageLoadingView(
    context: Context,
    private val resourcesProvider: IResourcesProvider
) : FrameLayout(context) {

    // Define member variables
    private lateinit var iconImageContainer: RoundCornerFrameLayout
    private lateinit var imageView: ImageView
    private var errorTextView: TextView? = null

    init {
        initView()
    }

    private fun initView() {
        // Load view layout
        inflate(context, R.layout.layout_webpage_loading, this)

        // Get control references
        iconImageContainer = findViewById(R.id.iconImageContainer)
        imageView = findViewById(R.id.iconImageView)

        imageView.visibility = VISIBLE
        errorTextView?.visibility = GONE
        visibility = GONE

        updateIconUrl(null)
    }
    fun updateIconUrl(url: String?) {
        if (url.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.icon_loading_default)
            iconImageContainer.useCornerRadius = false
            return
        }
        iconImageContainer.useCornerRadius = true

        try {
            Glide.with(context.applicationContext)
                .load(url)
                .placeholder(R.drawable.icon_loading_default)
                .into(imageView)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun showLoading() {
        if (visibility == VISIBLE) {
            return
        }
        imageView.visibility = VISIBLE
        errorTextView?.visibility = GONE
        animationShow(true)
    }

    fun showError(errorMessage: String) {
        imageView.visibility = GONE
        errorTextView?.apply {
            visibility = VISIBLE
            text = errorMessage
        }
    }

    fun hide() {
        if (visibility != VISIBLE) {
            return
        }
        animationShow(false)
    }

    private fun animationShow(visible: Boolean) {
        val currentAlpha = alpha
        val fadeO = if(visible) AlphaAnimation(currentAlpha, 1f) else AlphaAnimation(currentAlpha, 0f)
        fadeO.duration = 200
        fadeO.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                visibility = VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                if (!visible) {
                    visibility = GONE
                }
            }
        })
        clearAnimation()
        startAnimation(fadeO)
    }
}
