package io.miniapp.bridge

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep

@Keep
interface BridgeProvider {
    fun onWebViewCreated(webView: WebView, context: Context)

    fun onWebViewDestroy(webView: WebView)

    fun getWebClient(): WebViewClient?
}

@Keep
fun interface BridgeProviderFactory {
    fun buildBridgeProvider(id: String?, type: String, url: String?): BridgeProvider?
}