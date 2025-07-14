package io.miniapp.core.openplatform.miniapp.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import io.miniapp.core.R
import io.miniapp.core.databinding.ViewMenuActionButtonBinding
import io.miniapp.core.openplatform.miniapp.ext.setTextOrHide

class MenuActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val views: ViewMenuActionButtonBinding

    override fun setOnClickListener(l: OnClickListener?) {
        views.actionClickableZone.setOnClickListener(l)
    }

    var title: String? = null
        set(value) {
            field = value
            views.title.setTextOrHide(value)
        }

    var toIconRes: Int? = null
        set(value) {
            field = value
            views.toIcon.isVisible = true
            toIconRes?.let { views.toIcon.setImageResource(it) }
        }

    var toIcon: Drawable? = null
        set(value) {
            field = value
            views.toIcon.isVisible = true
            views.toIcon.setImageDrawable(value)
        }

    var tint: Int? = null
        set(value) {
            field = value
            views.toIcon.imageTintList = value?.let { ColorStateList.valueOf(value) }
        }

    var titleTextColor: Int? = null
        set(value) {
            field = value
            value?.let { views.title.setTextColor(it) }
        }

    init {
        inflate(context, R.layout.view_menu_action_button, this)
        views = ViewMenuActionButtonBinding.bind(this)

        attrs?.apply {
            context.withStyledAttributes(attrs, R.styleable.OW3ActionButton) {
                title = getString(R.styleable.OW3ActionButton_actionTitle) ?: ""
                toIcon = getDrawable(R.styleable.OW3ActionButton_leftIcon)

                getColor(R.styleable.OW3ActionButton_tint, -1000).also {
                    if (it!=-1000) {
                        tint = it
                    }
                }
                getColor(R.styleable.OW3ActionButton_titleTextColor, -1000).also {
                    if (it!=-1000) {
                        titleTextColor = it
                    }
                }
            }
        }

    }
}