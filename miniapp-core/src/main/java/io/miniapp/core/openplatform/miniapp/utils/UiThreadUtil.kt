package io.miniapp.core.openplatform.miniapp.utils

import android.os.Handler
import android.os.Looper

internal object UiThreadUtil {
    private var sMainHandler: Handler? = null

    fun isOnUiThread(): Boolean {
        return Looper.getMainLooper().thread === Thread.currentThread()
    }

    fun runOnUiThread(runnable: Runnable?) {
        runOnUiThread(runnable, 0L)
    }

    fun runOnUiThread(runnable: Runnable?, delayInMs: Long) {
        synchronized(UiThreadUtil::class.java) {
            if (sMainHandler == null) {
                sMainHandler = Handler(Looper.getMainLooper())
            }
        }
        sMainHandler!!.postDelayed(runnable!!, delayInMs)
    }
}