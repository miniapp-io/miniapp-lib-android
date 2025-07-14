package io.miniapp.core.openplatform.miniapp.ext

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible

fun TextView.setTextOrHide(newText: CharSequence?, hideWhenBlank: Boolean = true, vararg relatedViews: View = emptyArray()) {
    if (newText == null ||
        (newText.isBlank() && hideWhenBlank)) {
        isVisible = false
        relatedViews.forEach { it.isVisible = false }
    } else {
        this.text = newText
        isVisible = true
        relatedViews.forEach { it.isVisible = true }
    }
}