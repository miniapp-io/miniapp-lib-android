package io.miniapp.core.openplatform.miniapp.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.ColorUtils
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import io.miniapp.bridge.BridgeProvider
import io.miniapp.core.R
import io.miniapp.core.openplatform.common.network.utils.isValidUrl
import io.miniapp.core.openplatform.miniapp.ActivityStack
import io.miniapp.core.openplatform.miniapp.IMiniApp
import io.miniapp.core.openplatform.miniapp.WebAppParameters
import io.miniapp.core.openplatform.miniapp.events.WebEventListener
import io.miniapp.core.openplatform.miniapp.events.WebViewScrollListener
import io.miniapp.core.openplatform.miniapp.ui.webview.DefaultAppWebView
import io.miniapp.core.openplatform.miniapp.ui.webview.DefaultWebChromeClient
import io.miniapp.core.openplatform.miniapp.ui.webview.DefaultWebViewClient
import io.miniapp.core.openplatform.miniapp.ui.webview.WebAppLruCache
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.utils.UIContextUtil
import io.miniapp.core.openplatform.miniapp.webapp.IWebAppEventHandler

internal abstract class AbsWebViewContainer(
    context: Context
) : FrameLayout(context) {

    private var webView: DefaultAppWebView? = null
    private var mUrl: String? = null
    private var updateUrlOnly: Boolean = false

    private var webViewScrollListener: WebViewScrollListener? = null
    private var webViewNotAvailable = false
    private var isFlickeringCenter = false
    private var webViewProgressListener: Consumer<Float>? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

    var isPageLoaded = false
        private set

    private var isSettingsButtonVisible = false
    private var hasUserPermissions = false
    private var onPermissionsRequestResultCallback: Runnable? = null
    private var parentActivity: Activity? = null
    private var currentDialog: AlertDialog? = null

    private var lastDialogType = -1
    private var shownDialogsCount = 0
    private var blockedDialogsUntil: Long = 0

    init {
        parentActivity = UIContextUtil.findActivity(context)
        isFocusable = false
    }

    private val webEventListener by lazy {
        object: WebEventListener {

            override fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>,
                                           fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
                return ActivityStack.showFileChooser(filePathCallback, fileChooserParams)
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                onRequestPermission(request, false)
            }

            override fun requestGeoLocation(request: PermissionRequest) {
                onRequestPermission(request, true)
            }

            override fun onFaviconChanged(icon: Bitmap?) {
            }

            override fun onTitleChanged(title: String) {
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                getBridgeProvider()?.getWebClient()?.onPageStarted(view, url, favicon)
                webView?.injectedJS = false
            }

            override fun onPageFinished(view: WebView, url: String) {
                getBridgeProvider()?.getWebClient()?.onPageFinished(view, url)
                webView?.injectSelect()
                setPageFinished(url)
            }

            override fun onShowCustomView(
                view: View?,
                callback: WebChromeClient.CustomViewCallback?
            ) {
                showCustomView(view, callback)
            }

            override fun onHideCustomView() {
                hideCustomView()
            }

            override fun onPageCommitVisible() {
                if (isDApp()) {
                    webView?.injectedJS = true
                    AndroidUtils.readRes(R.raw.webview_ext)?.replace(
                        "\$DEBUG$",
                        "false"
                    )?.also {
                        webView?.evaluateJavascript(it, null)
                    }

                    AndroidUtils.readRes(R.raw.webview_share)?.also {
                        webView?.evaluateJavascript(it, null)
                    }

                } else {
                    webView?.injectedJS = true
                    AndroidUtils.readRes(R.raw.webview_app_ext)?.replace(
                        "\$DEBUG$",
                        "false"
                    )?.also {
                        webView?.evaluateJavascript(it, null)
                    }
                }
            }

            override fun onPageError(url: String, errorCode: Int, description: String): Boolean {
               return setPageError(url = url, errorCode = errorCode, description = description)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
                isNewWindow: Boolean
            ): Boolean {
                if (request.url.toString() == mUrl) {
                    return false
                }

                if (true == getBridgeProvider()?.getWebClient()?.shouldOverrideUrlLoading(view, request)) {
                    return true
                }

                return onOverrideUrlLoading(request.url.toString(), isNewWindow).apply {
                    if (this) {
                        setKeyboardFocusable(false)
                    }
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String, isNewWindow: Boolean): Boolean {
                if (url==mUrl) {
                    return false
                }

                if (true == getBridgeProvider()?.getWebClient()?.shouldOverrideUrlLoading(view, url)) {
                    return true
                }

                return onOverrideUrlLoading(url, isNewWindow).apply {
                    if (this) {
                        setKeyboardFocusable(false)
                    }
                }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                doUpdateVisitedHistory(view)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return getBridgeProvider()?.getWebClient()?.shouldInterceptRequest(view, request)
            }

            override fun onProgressChanged(process: Float) {
                webViewProgressListener?.accept(process)
            }
        }
    }

    open fun doUpdateVisitedHistory(view: WebView?) {

    }

    open fun onOverrideUrlLoading(url: String, isNewWindow: Boolean): Boolean {
        return false
    }

    open fun onRequestPermission(request: PermissionRequest, isLocation: Boolean) {

    }

    open fun showCustomView(
        view: View?,
        callback: WebChromeClient.CustomViewCallback?
    ) {

    }

    open fun hideCustomView() {
    }

    open fun setPageFinished(url:String) {
        webView?.isPageLoaded = true

        if (isPageLoaded) {
            webView?.alpha = 1.0f
            return
        }

        webView?.also {
            val set = AnimatorSet()
            set.playTogether(
                ObjectAnimator.ofFloat(webView, ALPHA, 1f),
            )
            set.start()
        }

        mUrl = url
        isPageLoaded = true
        isFocusable = true
        updateKeyboardFocusable()
    }

    open fun setPageError(url: String, errorCode: Int, description: String): Boolean {
        return false
    }

    open fun dismissSilent(complete: () -> Unit) {

    }
    open fun onComplete(isUseCache: Boolean) {
    }

    fun hasUserPermissions(): Boolean {
        return hasUserPermissions
    }

    fun setParentActivity(parentActivity: Activity?) {
        this.parentActivity = parentActivity
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>?,
        grantResults: IntArray?
    ) {
        if (requestCode == REQUEST_CODE_WEB_PERMISSION) {
            if (onPermissionsRequestResultCallback != null) {
                onPermissionsRequestResultCallback!!.run()
                onPermissionsRequestResultCallback = null
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_WEB_VIEW_FILE && mFilePathCallback != null) {
            var results: Array<Uri>? = null
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.dataString != null) {
                    results = arrayOf(Uri.parse(data.dataString))
                }
            }
            mFilePathCallback!!.onReceiveValue(results)
            mFilePathCallback = null
        }
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        return super.drawChild(canvas, child, drawingTime)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    open fun getCacheKey(): String?  {
        return null
    }

    open fun getCacheData(): String? {
        return null
    }

    open fun isMarketPlace(): Boolean {
        return false
    }

    open fun getConfig(): WebAppParameters? {
        return null
    }

    fun setWebViewProgressListener(webViewProgressListener: Consumer<Float>?) {
        this.webViewProgressListener = webViewProgressListener
    }

    fun reload() {
        if (isSettingsButtonVisible) {
            isSettingsButtonVisible = false
        }
        checkCreateWebView { _ ->
            isPageLoaded = false
            hasUserPermissions = false
            if (webView != null && mUrl != null) {
                webView?.alpha = 0.0f
                webView?.animate()?.cancel()
                webView?.animate()?.alpha(0F)?.start()
                webView?.reload()
            }
        }
    }

    private fun WebView.isWebViewStateDestroyed(): Boolean {
        val history = copyBackForwardList()
        return history.size == 0
    }

    fun loadUrl(url: String?, headers: Map<String,String>? = null, callback: ()-> Unit) {
        isPageLoaded = false
        hasUserPermissions = false
        mUrl = url
        checkCreateWebView {
            if (!it || false==webView?.isPageLoaded) {
                webView?.onResume()
                callback.invoke()
                if (updateUrlOnly) {

                } else {
                    headers?.also { h->
                        webView?.loadUrl(url ?: "about:blank", h)
                    } ?: webView?.loadUrl(url ?: "about:blank")
                }
            } else {
                try {
                    webView?.onResume()
                    val cacheData = webView?.cacheData
                    if(cacheData != getCacheData()
                        || false == getConfig()?.useCache
                        || webView?.url == null
                        || true==webView?.isWebViewStateDestroyed()) {
                        isPageLoaded = false
                        webView?.alpha = 0.0f
                        webView?.isPageLoaded = false
                        webView?.goToHomePage()
                        callback.invoke()
                        headers?.also { h->
                            webView?.loadUrl(url ?: "about:blank", h)
                        } ?: webView?.loadUrl(url ?: "about:blank")
                    } else {
                        setPageFinished(url!!)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onComplete(it)
            webView?.cacheData = getCacheData()
        }
    }

    fun getLoadUrl() = mUrl

    fun shouldLoadNewUrl() : Boolean {
        return mUrl.isNullOrBlank() || (true == webView?.isExpired)
    }

    fun destroyWebView(pause: Boolean) {
        webView?.apply {
            removeHandler()
            getCacheKey()?.also {
                WebAppLruCache.put(it, this)
                setDismissFlag()
                releaseRef(pause)
                setWebViewScrollListener(null)
            } ?: run {
                setDismissFlag()
                clearAfterDismiss()
            }
            removeAllViews()
        }
        isPageLoaded = false
        webView = null
    }

    fun didReceivedNotification(id: Int, account: Int, vararg args: Any) {
    }

    fun setWebViewScrollListener(webViewScrollListener: WebViewScrollListener?) {
        this.webViewScrollListener = webViewScrollListener
    }

    fun setDelegate(delegate: IWebAppEventHandler?) {
        this.webView?.webEventHandler = delegate
    }

    private var isSaveWebState = false
    fun saveWebViewState(){
        webView?.also {
            isSaveWebState = true
        }
    }

    fun restoreWebView() {
        webView?.also {
            removeWebView()
            it.onResume()
            addView(it)
            requestLayout()
        }
    }

    private fun removeWebView() {
        if (webView?.parent != null) {
            (webView?.parent as? ViewGroup)?.removeView(webView)
            invalidate()
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun setupWebView(handler: (Boolean) -> Unit) {
        if (webView != null) {
            webView!!.destroy()
            removeWebView()
        }
        var isUseCache = false
        val cacheKey = getCacheKey()

        updateUrlOnly = false

        webView = cacheKey?.let { key->
            WebAppLruCache.get(key = key)?.let {
                if(it.cacheData == getCacheData()) {
                    it
                } else {
                    if (isMarketPlace() && it.isPageLoaded) {
                        updateUrlOnly = true
                        it
                    } else {
                        if (it.isParentDismiss()) {
                            it.clearAfterDismiss()
                        }
                        null
                    }
                }
            }?.apply {
                isUseCache = true
                mUrl = url
                onResume()
                if (false == getConfig()?.useCache) {
                    goToHomePage()
                }
            }} ?: DefaultAppWebView(context.applicationContext)

        val setupView : (Boolean) -> Unit = { _ ->

            webView?.setupForMiniApp(!isDApp())

            val defaultWebChromeClient = DefaultWebChromeClient(parentActivity!!, webEventListener)
            webView?.webChromeClient = defaultWebChromeClient

            val defaultWebViewClient = DefaultWebViewClient(webEventListener)
            webView?.webViewClient = defaultWebViewClient

            getBridgeProvider()?.also {
                it.onWebViewCreated(webView!!, context)
            }

            if (!isDApp() && !isUseCache) {
                webView?.registerWebApp()
            }

            if (!isDApp()) {
                webView?.addHandler()
            }

            webView?.registerWebTouchEvent()

            webView?.isVerticalScrollBarEnabled = false
            webView?.owner = getOwner()

            webView?.miniApp = getMiniApp()
            webView?.setWebViewScrollListener(webViewScrollListener)
            webView?.setDismissHandler { complete ->
                dismissSilent(complete)
            }

            val useCacheData = webView?.cacheData == getCacheData() || getConfig()?.useCache == false
            webView?.alpha = if (useCacheData && true==webView?.isPageLoaded) 1.0f else 0.0f

            if (!useCacheData) {
                webView?.headColor = null
                webView?.bgColor = null
            }

            removeWebView()
            addView(webView)

            onWebViewCreated(isUseCache)

            handler.invoke(isUseCache)
        }

        if(!isUseCache) {
            cacheKey?.apply {
                WebAppLruCache.put(this, webView!!)
            }
        } else {
            if (!webView!!.isParentDismiss()) {
                if (webView!!.isTopLevel()) {
                    // kill current use the top same webapp
                    if (!FloatingWindowManager.isAppOnMinimization(webView?.miniApp) || webView!!.owner != getOwner()) {
                        webView = null
                        dismissSilent {  }
                        return
                    }
                } else {
                    // kill the old webapp not on top
                    webView!!.dismiss{}
                }
            }
            webView?.refreshAppId()
        }

        setupView(isUseCache)
    }

    private fun runWithPermissions(permissions: Array<String>, callback: Consumer<Boolean>) {
        if (checkPermissions(permissions)) {
            callback.accept(true)
        } else {
            onPermissionsRequestResultCallback = Runnable {
                callback.accept(
                    checkPermissions(permissions)
                )
            }
            if (parentActivity != null) {
                parentActivity!!.requestPermissions(permissions, REQUEST_CODE_WEB_PERMISSION)
            }
        }
    }

    private fun checkPermissions(permissions: Array<String>): Boolean {
        for (perm in permissions) {
            if (context.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun setKeyboardFocusable(focusable: Boolean) {
        keyboardFocusable = focusable
        updateKeyboardFocusable()
    }

    private var keyboardFocusable = false
    private var wasFocusable = false

    private fun updateKeyboardFocusable() {
        val focusable = keyboardFocusable && isPageLoaded && false
        if (wasFocusable != focusable) {
            if (!focusable) {
                setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS)
                this.isFocusable = false
                //                webView.setFocusable(false);
                if (webView != null) {
                    webView!!.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS)
                    webView!!.clearFocus()
                }
                AndroidUtils.hideKeyboard(this)
            } else {
                setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS)
                this.isFocusable = true
                //                webView.setFocusable(true);
                if (webView != null) {
                    webView!!.setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS)
                }
            }
        }
        wasFocusable = focusable
    }

    private fun setupFlickerParams(center: Boolean) {
        isFlickeringCenter = center
    }

    fun checkCreateWebView(handler: (Boolean) -> Unit) {
        if (webView == null && !webViewNotAvailable) {
            try {
                setupWebView(handler)
            } catch (t: Throwable) {
                removeWebView()
            }
        } else {
            handler.invoke(true)
        }
    }

    private fun unknownError(errCode: String? = null) {
    }

    private fun error(reason: String) {
    }

    private fun ignoreDialog(type: Int): Boolean {
        if (currentDialog != null) {
            return true
        }
        if (blockedDialogsUntil > 0 && System.currentTimeMillis() < blockedDialogsUntil) {
            return true
        }
        if (lastDialogType == type && shownDialogsCount > 3) {
            blockedDialogsUntil = System.currentTimeMillis() + 3 * 1000L
            shownDialogsCount = 0
            return true
        }
        return false
    }

    private fun showDialog(type: Int, dialog: AlertDialog?, onDismiss: Runnable?): Boolean {
        return true
    }

    private fun openQrScanActivity() {
        if (parentActivity == null) {
            return
        }
    }

    private fun getColor(colorKey: Int): Int {
        return AndroidUtils.getColor(context, colorKey)
    }

    private fun formatColor(colorKey: Int): String {
        val color = getColor(colorKey)
        return "#" + hexFixed(Color.red(color)) + hexFixed(Color.green(color)) + hexFixed(
            Color.blue(
                color
            )
        )
    }

    private fun hexFixed(h: Int): String {
        var hex = Integer.toHexString(h)
        if (hex.length < 2) {
            hex = "0$hex"
        }
        return hex
    }

    fun getWebView() = webView

    fun getWebApp() = webView?.webApp

    abstract fun getOwner(): LifecycleOwner?

    abstract fun getMiniApp(): IMiniApp

    abstract fun onWebViewCreated(isUseCache: Boolean)

    abstract fun getBridgeProvider(): BridgeProvider?

    abstract fun isDApp(): Boolean

    companion object {
        const val REQUEST_CODE_WEB_VIEW_FILE = 3000
        private const val REQUEST_CODE_WEB_PERMISSION = 4000

        fun getMainButtonRippleColor(buttonColor: Int): Int {
            return if (ColorUtils.calculateLuminance(buttonColor) >= 0.3f) 0x12000000 else 0x16FFFFFF
        }
    }
}
