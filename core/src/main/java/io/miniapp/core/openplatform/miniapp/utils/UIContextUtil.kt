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

    fun getFilesDirFixed(context: Context): File {
        for (a in 0..9) {
            try {
                val path = context.applicationContext.filesDir
                return path
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            val info: ApplicationInfo = context.applicationContext.applicationInfo
            val path = File(info.dataDir, "files")
            path.mkdirs()
            return path
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return File("/data/data/io.openweb3.sample/files")
    }
}