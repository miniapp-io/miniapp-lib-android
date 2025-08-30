package io.miniapp.core.openplatform.miniapp.ui.webview

import android.util.LruCache
import java.lang.ref.WeakReference

internal object WebAppLruCache {

    internal class WebViewCache : LruCache<String, WeakReference<DefaultAppWebView>>(
        DEFAULT_MAX_SIZE
    ) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String?,
            oldValue: WeakReference<DefaultAppWebView>?,
            newValue: WeakReference<DefaultAppWebView>?
        ) {
            if (true==oldValue?.get()?.isParentDismiss()) {
                oldValue.get()?.clearAfterDismiss()
            }
        }
    }

    private const val DEFAULT_MAX_SIZE = 5

    private val webViewCache = WebViewCache()

    fun resize(size: Int) {
        if (webViewCache.maxSize() != size) {
            webViewCache.resize(size)
        }
    }

    fun get(key: String): DefaultAppWebView? {
        return webViewCache.get(key)?.get()
    }

    fun put(key: String, webView: DefaultAppWebView) {
        webViewCache.put(key, WeakReference(webView))
    }

    fun remove(key: String): DefaultAppWebView? {
        return webViewCache.remove(key)?.get()
    }

    fun removeAll() {
        webViewCache.evictAll()
    }
}