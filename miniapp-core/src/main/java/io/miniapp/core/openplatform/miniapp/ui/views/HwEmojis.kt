package io.miniapp.core.openplatform.miniapp.ui.views

import android.view.View
import java.util.Arrays


internal object HwEmojis {
    private val hwViews: MutableSet<View?> = HashSet()

    @Volatile
    var isHwEnabled = false

    private var task: Runnable? = null
    var isFirstOpen = true

    var isPreparing = false

    var isCascade = false

    private var isBeforePreparing = false
    private var isWeakDevice: Boolean? = null
    fun prepare(runnable: Runnable?, cascade: Boolean) {
        isCascade = cascade
        isPreparing = true
        isBeforePreparing = false
        if (isFirstOpen) {
            isFirstOpen = false
        }
        task = runnable
    }

    fun beforePreparing() {
        //ImageLoader.getInstance().getCacheOutQueue().pause()
        isBeforePreparing = true
    }

    val isHwEnabledOrPreparing: Boolean
        get() = isHwEnabled || isPreparing || isBeforePreparing

    fun exec() {
        if (task != null) {
            task!!.run()
            task = null
        }
    }

    fun grab(view: View): Boolean {
        if (isHwEnabled) {
            hwViews.add(view)
        }
        return isHwEnabled
    }

    fun grabIfWeakDevice(vararg views: View?): Boolean {
        if (isWeakDevice == null) {
            //isWeakDevice = getDevicePerformanceClass() !== PERFORMANCE_CLASS_HIGH
        }
        if (!isWeakDevice!!) {
            return false
        }
        if (isHwEnabled) {
            hwViews.addAll(Arrays.asList(*views))
        }
        return isHwEnabled
    }

    fun enableHw() {
        //ImageLoader.getInstance().getCacheOutQueue().pause()
        isHwEnabled = true
        isPreparing = false
        isBeforePreparing = false
    }

    fun disableHw() {
        //ImageLoader.getInstance().getCacheOutQueue().resume()
        isHwEnabled = false
        isPreparing = false
        isBeforePreparing = false
        task = null
        for (view in hwViews) {
            view?.invalidate()
        }
        hwViews.clear()
    }
}
