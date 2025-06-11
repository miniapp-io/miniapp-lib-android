package io.miniapp.core.openplatform.miniapp.utils

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Process
import android.os.SystemClock
import java.util.concurrent.CountDownLatch

internal class DispatchQueue : Thread {
    @Volatile
    private var handler: Handler? = null
    private val syncLatch = CountDownLatch(1)
    private var lastTaskTime: Long = 0
    val index: Int = indexPointer++
    private var threadPriority: Int =
        THREAD_PRIORITY_DEFAULT

    @JvmOverloads
    constructor(threadName: String?, start: Boolean = true) {
        setName(threadName)
        if (start) {
            start()
        }
    }

    constructor(threadName: String?, start: Boolean, priority: Int) {
        threadPriority = priority
        setName(threadName)
        if (start) {
            start()
        }
    }

    fun sendMessage(msg: Message?, delay: Int) {
        try {
            syncLatch.await()
            if (delay <= 0) {
                handler!!.sendMessage(msg!!)
            } else {
                handler!!.sendMessageDelayed(msg!!, delay.toLong())
            }
        } catch (ignore: Exception) {
        }
    }

    fun cancelRunnable(runnable: Runnable?) {
        try {
            syncLatch.await()
            handler!!.removeCallbacks(runnable!!)
        } catch (e: Exception) {
           e.printStackTrace()
        }
    }

    fun cancelRunnables(runnables: Array<Runnable?>) {
        try {
            syncLatch.await()
            for (i in runnables.indices) {
                handler!!.removeCallbacks(runnables[i]!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun postRunnable(runnable: Runnable?): Boolean {
        lastTaskTime = SystemClock.elapsedRealtime()
        return postRunnable(runnable, 0)
    }

    fun postToFrontRunnable(runnable: Runnable?): Boolean {
        try {
            syncLatch.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return handler!!.postAtFrontOfQueue(runnable!!)
    }

    fun postRunnable(runnable: Runnable?, delay: Long): Boolean {
        try {
            syncLatch.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (delay <= 0) {
            handler!!.post(runnable!!)
        } else {
            handler!!.postDelayed(runnable!!, delay)
        }
    }

    fun cleanupQueue() {
        try {
            syncLatch.await()
            handler!!.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handleMessage(inputMessage: Message?) {}
    fun recycle() {
        handler!!.looper.quit()
    }

    override fun run() {
        Looper.prepare()
        handler = Handler(Looper.myLooper()!!) { msg: Message? ->
            handleMessage(msg)
            true
        }
        syncLatch.countDown()
        if (threadPriority != THREAD_PRIORITY_DEFAULT) {
            Process.setThreadPriority(threadPriority)
        }
        Looper.loop()
    }

    val isReady: Boolean
        get() = syncLatch.count == 0L

    companion object {
        private const val THREAD_PRIORITY_DEFAULT = -1000
        private var indexPointer = 0
    }
}
