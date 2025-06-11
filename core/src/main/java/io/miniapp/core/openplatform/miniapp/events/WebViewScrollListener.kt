package io.miniapp.core.openplatform.miniapp.events

import android.webkit.WebView

internal interface WebViewScrollListener {
    /**
     * Called when WebView scrolls
     *
     * @param webView   WebView that scrolled
     * @param dx        Delta X
     * @param dy        Delta Y
     */
    fun onWebViewScrolled(webView: WebView, dx: Int, dy: Int)
}