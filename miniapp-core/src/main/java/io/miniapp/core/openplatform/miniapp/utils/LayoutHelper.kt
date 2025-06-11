package io.miniapp.core.openplatform.miniapp.utils

import android.widget.FrameLayout

internal object LayoutHelper {
    const val MATCH_PARENT = -1
    const val WRAP_CONTENT = -2
    private fun getSize(size: Int): Int {
        return (if (size < 0) size else AndroidUtils.dp(size.toInt())).toInt()
    }

    //endregion
    //region FrameLayout
    fun createFrame(
        width: Int,
        height: Int,
        gravity: Int,
        leftMargin: Int,
        topMargin: Int,
        rightMargin: Int,
        bottomMargin: Int
    ): FrameLayout.LayoutParams {
        val layoutParams = FrameLayout.LayoutParams(
            getSize(width),
            getSize(height),
            gravity
        )
        layoutParams.setMargins(
            AndroidUtils.dp(leftMargin),
            AndroidUtils.dp(topMargin),
            AndroidUtils.dp(rightMargin),
            AndroidUtils.dp(bottomMargin)
        )
        return layoutParams
    }

    fun createFrameWithSize(width: Int, height: Int, gravity: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            width,
            height,
            gravity
        )
    }

    fun createFrame(width: Int, height: Int, gravity: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            getSize(width),
            getSize(height),
            gravity
        )
    }

    fun createFrame(width: Int, height: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            getSize(width),
            getSize(height)
        )
    }
}
