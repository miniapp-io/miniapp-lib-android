package io.miniapp.core.openplatform.miniapp.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.LifecycleOwner
import io.miniapp.core.openplatform.common.apis.data.AppSettings
import io.miniapp.core.openplatform.miniapp.IMiniApp
import io.miniapp.core.openplatform.miniapp.events.WebViewScrollListener
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.utils.UiThreadUtil
import io.miniapp.core.openplatform.miniapp.utils.WebViewResourceHelper
import io.miniapp.core.openplatform.miniapp.webapp.IWebApp
import io.miniapp.core.openplatform.miniapp.webapp.IWebAppEventHandler
import org.json.JSONArray
import org.json.JSONObject


@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
internal class DefaultAppWebView(context: Context) : WebView(context), IWebAppEventHandler {
    companion object {
        var _webAppId = 0
    }

    var webAppId = ++_webAppId
    var isPageLoaded: Boolean = false
    var cacheData: String? = null
    var miniApp: IMiniApp? = null
    var owner: LifecycleOwner? = null
    var webApp: IWebApp? = null

    var headColor:Int? = null
    var bgColor:Int? = null
    var isBackButtonVisible:Boolean? = null
    var isCloseConfirm: Boolean? = null
    var isExpanded: Boolean? = null
    var allowVerticalSwipe: Boolean? = null
    var isSettingVisible: Boolean? = null
    var showFullscreen: Boolean? = null
    var appSettings: AppSettings? = null
    
    // Expiration time property, used to control the expiration state of mini apps
    var expirationTime: Long? = null

    var orientationLocked: Boolean? = null

    val isExpired: Boolean
        get() = expirationTime?.let { System.currentTimeMillis() > it } ?: false

    var metadata: JSONObject? = null

    val pageImage: String?
        get() {
            return try {
                metadata?.getString("image")
            } catch (e: Throwable) {
                null
            }
        }

    val pageTitle: String?
        get() {
            return try {
                metadata?.getString("title")
            } catch (e: Throwable) {
                null
            }
        }

    val pageDescription: String?
        get() {
            return try {
                metadata?.getString("description")
            } catch (e: Throwable) {
                null
            }
        }

    val pageParams: String?
        get() {
            return try {
                metadata?.getString("params")
            } catch (e: Throwable) {
                null
            }
        }

    var pageIcon: String? = null

    var injectedJS: Boolean = false

    private var prevScrollX = 0
    private var prevScrollY = 0
    private var lastClickMs: Long = 0
    private var _isParentDismiss = false

    private var _webViewScrollListener: WebViewScrollListener? = null
    private var dismissHandler: ((() -> Unit) -> Unit)? = null

    var webEventHandler: IWebAppEventHandler? = null

    init {
        WebViewResourceHelper.addChromeResourceIfNeeded(context)
        // Set expiration time to 1 hour from now
        expirationTime = System.currentTimeMillis() + (60 * 60 * 1000)
    }

    @JavascriptInterface
    fun postEvent(eventType: String, eventData: String?) {
        UiThreadUtil.runOnUiThread {
            try {
                JSONArray(eventData ?: "").also {
                    handleWebScrollMessage(eventType, it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun post(eventType: String, eventData: String?) {
        UiThreadUtil.runOnUiThread {
            try {
                JSONArray(eventData ?: "").also {
                    handleWebScrollMessage(eventType, it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun resolveShare(json: String?, file: ByteArray?, fileName: String?, fileMimeType: String?) {
        // TODO
    }

    fun dismiss(complete :() -> Unit) {
        dismissHandler?.invoke(complete)
        dismissHandler = null
        setDismissFlag()
    }

    fun isTopLevel() = webAppId == _webAppId

    fun refreshAppId() {
        webAppId = ++_webAppId
    }

    fun setDismissFlag(){
        webEventHandler = null
        _isParentDismiss = true
        owner = null
        miniApp = null
    }

    fun isParentDismiss() = _isParentDismiss

    override fun hasOverlappingRendering(): Boolean {
        return false
    }

    fun setWebViewScrollListener(webViewScrollListener: WebViewScrollListener?) {
        this._webViewScrollListener = webViewScrollListener
    }

    fun setDismissHandler(handle: ((() -> Unit) -> Unit)?) {
        _isParentDismiss = false
        dismissHandler = handle
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        _webViewScrollListener?.onWebViewScrolled(
            this,
            scrollX - prevScrollX,
            scrollY - prevScrollY
        )
        prevScrollX = scrollX
        prevScrollY = scrollY
    }

    override fun setScrollX(value: Int) {
        super.setScrollX(value)
        prevScrollX = value
    }

    override fun setScrollY(value: Int) {
        super.setScrollY(value)
        prevScrollY = value
    }

    override fun onCheckIsTextEditor(): Boolean {
        return isFocusable
    }

    override fun reload() {
        CookieManager.getInstance().flush()
        super.reload()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(heightMeasureSpec),
                MeasureSpec.EXACTLY
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            lastClickMs = System.currentTimeMillis()
        }
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        AndroidUtils.checkAndroidTheme(context, true)
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        AndroidUtils.checkAndroidTheme(context, false)
        super.onDetachedFromWindow()
    }

    override fun handleMessage(eventType: String, eventData: JSONObject?): Boolean {
       return webEventHandler?.handleMessage(eventType, eventData) ?: false
    }

    override fun handleWebScrollMessage(eventType: String, eventData: JSONArray?): Boolean {
       return webEventHandler?.handleWebScrollMessage(eventType, eventData) ?: false
    }

    override fun onWellKnown(isTelegram: Boolean) {
        webEventHandler?.onWellKnown(isTelegram)
    }

    fun releaseRef(pause: Boolean) {
        if (pause) {
            onPause()
        }
    }

    fun clearAfterDismiss() {
        // Make sure you remove the WebView from its parent view before doing anything.
        webChromeClient = null
        webViewClient = WebViewClient()
        webApp?.destroy()
        webApp = null
        stopLoading()
        onPause()
        clearHistory()
        // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
        loadUrl("about:blank")
        removeAllViews()
        destroy()
    }

    fun goToHomePage() {
        val backList = copyBackForwardList()
        if (backList.currentIndex > 0) {
            goBackOrForward(-backList.currentIndex)
        }
    }
}