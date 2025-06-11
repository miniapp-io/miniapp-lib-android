package io.miniapp.core.openplatform.miniapp.events

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

internal interface WebViewEventListener {

    /**
     * Triggered when a webview page is about to be started.
     *
     * @param url The url about to be rendered.
     */
    fun pageWillStart(url: String) {
        // NO-OP
    }

    /**
     * Triggered when a loading webview page has started.
     *
     * @param url The rendering url.
     */
    fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        // NO-OP
    }

    /**
     * Triggered when a loading webview page has finished loading but has not been rendered yet.
     *
     * @param url The finished url.
     */
    fun onPageFinished(view: WebView, url: String) {
        // NO-OP
    }

    fun onPageCommitVisible() {

    }

    /**
     * Triggered when an error occurred while loading a page.
     *
     * @param url The url that failed.
     * @param errorCode The error code.
     * @param description The error description.
     */
    fun onPageError(url: String, errorCode: Int, description: String): Boolean {
        // NO-OP
        return false
    }

    /**
     * Triggered when an error occurred while loading a page.
     *
     * @param url The url that failed.
     * @param errorCode The error code.
     * @param description The error description.
     */
    fun onHttpError(url: String, errorCode: Int, description: String) {
        // NO-OP
    }

    /**
     * Triggered when a webview load an url.
     *
     * @param url The url about to be rendered.
     * @return true if the method needs to manage some custom handling
     */
    fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest, isNewWindow: Boolean = false): Boolean {
        return false
    }

    /**
     * Triggered when a webview load an url.
     *
     * @param url The url about to be rendered.
     * @return true if the method needs to manage some custom handling
     */
    fun shouldOverrideUrlLoading(view: WebView, url: String, isNewWindow: Boolean = false): Boolean {
        return false
    }

    /**
     * Triggered when webview invoke shouldInterceptRequest
     */
    fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        return  null
    }


    /**
     * Triggered when a webview history change
     *
     * @param view The WebView about to be rendered.
     * @param url The url about to be rendered.
     * @param isReload The url about to be rendered.
     * @return true if the method needs to manage some custom handling
     */
    fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {

    }
}
