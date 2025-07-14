package io.miniapp.core.openplatform.miniapp.ui.webview

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import io.miniapp.core.openplatform.miniapp.events.WebViewEventListener
import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import java.lang.ref.WeakReference

/**
 * This class inherits from WebViewClient. It has to be used with a WebView.
 * It's responsible for dispatching events to the WebViewEventListener
 */
internal class DefaultWebViewClient(eventListener: WebViewEventListener?) : WebViewClient() {

    private var mInError: Boolean = false

    private var eventListener: WeakReference<WebViewEventListener> = WeakReference(eventListener)

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        eventListener.get()?.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        mInError = false
        val shouldOverrideUrlLoading = eventListener.get()?.shouldOverrideUrlLoading(view, request) ?: false
        if (!shouldOverrideUrlLoading) {
            eventListener.get()?.pageWillStart(request.url.toString())
        }
        return shouldOverrideUrlLoading
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        mInError = false
        val shouldOverrideUrlLoading = eventListener.get()?.shouldOverrideUrlLoading(view, url) ?: false
        if (!shouldOverrideUrlLoading) {
            eventListener.get()?.pageWillStart(url)
        }
        return shouldOverrideUrlLoading
    }

//    override fun onReceivedError(
//        view: WebView?,
//        request: WebResourceRequest?,
//        error: WebResourceError?
//    ) {
//        super.onReceivedError(view, request, error)
//        // Handle error, show custom error page or prompt message
//        if (error != null) {
//            Log.e("WebViewError", "Error code: ${error.errorCode}, description: ${error.description}")
//        }
//    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        return eventListener.get()?.shouldInterceptRequest(view, request) ?: super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        mInError = false
        eventListener.get()?.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

//        view.loadUrl("javascript:(function() { " +
//                "var parent = document.getElementsByTagName('head').item(0); " +
//                "var script = document.createElement('script'); " +
//                "script.type = 'text/javascript'; " +
//                "script.src = 'https://cdn.jsdelivr.net/npm/eruda'; " +
//                "parent.appendChild(script)" +
//                "})()")

        if (!mInError) {
            LogTimber.tag("DefaultWebViewClient#onPageFinished").d(url)
            eventListener.get()?.onPageFinished(view, url)
        }
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        eventListener.get()?.onPageCommitVisible()
        super.onPageCommitVisible(view, url)
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        super.onReceivedHttpError(view, request, errorResponse)
        LogTimber.tag("DefaultWebViewClient#onReceivedHttpError").d(errorResponse.reasonPhrase)
        eventListener.get()?.onHttpError(
                request.url.toString(),
                errorResponse.statusCode,
                errorResponse.reasonPhrase
        )
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest, error: WebResourceError) {
        super.onReceivedError(view, request, error)
        if (!mInError) {
            LogTimber.tag("DefaultWebViewClient#onReceivedError").d(error.description.toString())
            mInError = eventListener.get()?.onPageError(request.url.toString(), error.errorCode, error.description.toString()) ?: false
        }
    }
}
