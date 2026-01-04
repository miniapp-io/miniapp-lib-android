package io.miniapp.core.openplatform.miniapp.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import io.miniapp.core.BuildConfig
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.MiniAppServiceImpl
import io.miniapp.core.openplatform.miniapp.ui.webview.DefaultAppWebView
import io.miniapp.core.openplatform.miniapp.ui.webview.WebViewJsBridge
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import io.miniapp.core.openplatform.miniapp.webapp.WebAppImpl
import org.json.JSONObject

/**
 * Inject custom select and datetime picker scripts into WebView
 */
internal fun WebView.injectSelect() {
    // Inject custom select picker script
    AndroidUtils.readRes(R.raw.webview_select)?.also {
        evaluateJavascript(it, null)
    }
    // Inject datetime picker script
    AndroidUtils.readRes(R.raw.webview_datetime)?.also {
        evaluateJavascript(it, null)
    }
}

@SuppressLint("SetJavaScriptEnabled")
internal fun WebView.setupForMiniApp(isApp: Boolean) {

    if (!isApp) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)
        CookieManager.getInstance().flush()
    }

    settings.javaScriptEnabled = true
    settings.setGeolocationEnabled(true)
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.setSupportMultipleWindows(true)
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.allowFileAccessFromFileURLs = false
    settings.allowUniversalAccessFromFileURLs = false

    WebView.setWebContentsDebuggingEnabled(true)

    if (!isApp) {
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.saveFormData = true
        settings.savePassword = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }
    }

    setAgent(isApp)
}

internal fun WebView.setAgent(isApp: Boolean) {
    // Set User Agent
    try {
        var useragent = settings.userAgentString
        useragent = useragent.replace("; wv)", ")")
        useragent = useragent.replace(
            "\\(Linux; Android.+;[^)]+\\)".toRegex(),
            "(Linux; Android " + Build.VERSION.RELEASE + "; K)"
        )
        useragent = useragent.replace("Version/[\\d\\.]+ ".toRegex(), "")
        val perf: Int = AndroidUtils.getDevicePerformanceClass()
        val perfName =
            if (perf == 0) "LOW" else if (perf == 1) "AVERAGE" else "HIGH"
        useragent += (" MiniAppX Android/" + BuildConfig.CORE_VERSION + " (" + AndroidUtils.capitalizeFirst(
            Build.MANUFACTURER
        )) + " " + Build.MODEL + "; Android " + Build.VERSION.RELEASE + "; SDK " + Build.VERSION.SDK_INT + "; " + perfName + ")"
        settings.userAgentString = useragent
    } catch (e: Exception) {
        LogTimber.tag("MiniAppX").e(e)
    }
}

internal fun DefaultAppWebView.registerWebApp() {
    webApp = WebAppImpl(
        webView = this,
        webAppName =  MiniAppServiceImpl.getInstance().webAppName)
}

internal fun DefaultAppWebView.addHandler() {
    webApp?.addEvenHandler(this)
}

internal fun DefaultAppWebView.removeHandler() {
    webApp?.removeEvenHandler()
}

@SuppressLint("JavascriptInterface")
internal fun DefaultAppWebView.registerWebTouchEvent() {
    // Register WebView event handler
    addJavascriptInterface(this, "RunTimeWebviewProxy")
    // Register JS Bridge for native UI components (select, date, time pickers)
    addJavascriptInterface(WebViewJsBridge(this), "NativeUIBridge")
}

internal fun DefaultAppWebView.getPageData(complete: ()-> Unit) {
    getWebFavicon()
    getWebMetaData(complete)
}

internal fun DefaultAppWebView.getWebMetaData(complete: ()-> Unit) {
    val jsCode = """
    (function() {
        var metaTags = document.getElementsByTagName('meta');
        var metaData = {};
        for (var i = 0; i < metaTags.length; i++) {
            var name = metaTags[i].getAttribute('property') || metaTags[i].getAttribute('name');
            if (name) {
                switch(name.toLowerCase()) {
                    case 'og:title':
                        metaData.title = metaTags[i].getAttribute('content');
                        break;
                    case 'og:description':
                    case 'description':
                        metaData.description = metaTags[i].getAttribute('content');
                        break;
                    case 'og:url':
                        metaData.url = metaTags[i].getAttribute('content');
                        break;
                    case 'og:params':
                        metaData.params = metaTags[i].getAttribute('content');
                        break;
                    case 'og:image':
                        metaData.image = metaTags[i].getAttribute('content');
                        break;       
                }
            }
        }
        
        // If title and url are not obtained from meta tags, use the default ones
        if (!metaData.title) {
            metaData.title = document.title;
        }
        if (!metaData.url) {
            metaData.url = window.location.href;
        }
        
        return JSON.stringify(metaData);
    })();
"""

    evaluateJavascript(jsCode) { result ->
        if (result != null && result != "null") {
            try {
                val jsonString = result.removeSurrounding("\"")
                val unescapedJsonString = jsonString.replace("\\\"", "\"")
                metadata = JSONObject(unescapedJsonString)
                complete.invoke()
            } catch (e: Throwable) {
                e.printStackTrace()
                complete.invoke()
            }
        }
    }
}

internal fun DefaultAppWebView.getWebFavicon() {
    evaluateJavascript("document.querySelector('link[rel=\"shortcut icon\"]')?.href") { result ->
        pageIcon = if (result != null && result != "null") {
            // Successfully obtained favicon link
            val iconUrl = result.removeSurrounding("\"")
            iconUrl
        } else {
            try {
                val uri = Uri.parse(url)
                val defaultFavicon = "${uri.scheme}://${uri.host}/favicon.ico"
                defaultFavicon
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }
    }
}
