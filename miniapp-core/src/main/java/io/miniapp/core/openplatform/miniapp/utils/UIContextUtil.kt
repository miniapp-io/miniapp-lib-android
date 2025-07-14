package io.miniapp.core.openplatform.miniapp.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import java.io.File

internal object UIContextUtil {

    fun findActivity(context: Context?): Activity? {
        if (context is Activity) {
            return context
        }
        return if (context is ContextWrapper) {
            findActivity(context.baseContext)
        } else null
    }
}