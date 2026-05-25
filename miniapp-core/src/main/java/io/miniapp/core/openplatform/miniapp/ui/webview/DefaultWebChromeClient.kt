package io.miniapp.core.openplatform.miniapp.ui.webview

import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.events.WebEventListener
import io.miniapp.core.openplatform.miniapp.ui.setAgent
import io.miniapp.core.openplatform.miniapp.utils.CheckWebViewPermissionsUseCase
import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import io.miniapp.core.openplatform.miniapp.utils.PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING
import io.miniapp.core.openplatform.miniapp.utils.SchemeUtils
import java.lang.ref.WeakReference
import androidx.core.graphics.createBitmap


internal class DefaultWebChromeClient(
    private val parentActivity: Activity,
    listener: WebEventListener?
) : WebChromeClient() {

    companion object {
        private const val POPUP_WIDTH_RATIO = 0.8f
        private const val POPUP_MIN_HEIGHT_RATIO = 0.5f
        private const val POPUP_MAX_HEIGHT_RATIO = 0.8f
    }

    var popupDialog: Dialog? = null
    var newWebView: WebView? = null

    private var eventListener: WeakReference<WebEventListener> = WeakReference(listener)

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        if (view == newWebView) return
        LogTimber.tag("WebChromeClient").d(
            "onReceivedIcon favicon=" + if (icon == null) "null" else icon.getWidth()
                .toString() + "x" + icon.getHeight()
        )
        eventListener.get()?.onFaviconChanged(icon)
        super.onReceivedIcon(view, icon)
    }

    override fun onReceivedTitle(view: WebView?, title: String) {
        if (view == newWebView) return
        LogTimber.tag("WebChromeClient").d("onReceivedTitle title=$title")
        eventListener.get()?.onTitleChanged(title)
        super.onReceivedTitle(view, title)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        if (CheckWebViewPermissionsUseCase.execute(parentActivity, request)) {
            request.grant(request.resources)
        } else {
            eventListener.get()?.onPermissionRequest(request)
        }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback
    ) {

        val request = object : PermissionRequest() {

            override fun getOrigin(): Uri {
                return Uri.parse(origin)
            }

            override fun getResources(): Array<String> {
                return PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING
            }

            override fun grant(resources: Array<out String>?) {
                callback.invoke(origin, true, false)
            }

            override fun deny() {
                callback.invoke(origin, false, false)
            }
        }

        if (CheckWebViewPermissionsUseCase.execute(parentActivity, request)) {
            callback.invoke(origin, true, false)
        } else {
            eventListener.get()?.requestGeoLocation(request)
        }
    }

    override fun onGeolocationPermissionsHidePrompt() {
    }

    override fun getDefaultVideoPoster(): Bitmap? {
        return createBitmap(10, 10)
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {

        newWebView = WebView(parentActivity).apply {
            // Set new WebView configuration
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // Set User Agent
            setAgent(false)
        }

        if (isDialog) {
            popupDialog?.dismiss()
            popupDialog = Dialog(parentActivity, R.style.TransparentDialog).apply {
                setOnDismissListener {
                    destroyPopupWebView()
                }
            }
            popupDialog?.setContentView(
                newWebView!!,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            popupDialog?.show()
            popupDialog?.let { applyPopupDialogWindowSize(it) }
        }

        // Popup WebView must share this WebChromeClient; otherwise window.close() in the popup
        // invokes onCloseWindow on another instance and cannot dismiss popupDialog.
        newWebView?.webChromeClient = this
        newWebView?.webViewClient = createPopupWebViewClient(isDialog)

        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = newWebView
        resultMsg.sendToTarget()

        return true
    }

    private fun applyPopupDialogWindowSize(dialog: Dialog) {
        val window = dialog.window ?: return
        val metrics = parentActivity.resources.displayMetrics
        val width = (metrics.widthPixels * POPUP_WIDTH_RATIO).toInt()
        val minHeight = (metrics.heightPixels * POPUP_MIN_HEIGHT_RATIO).toInt()
        val maxHeight = (metrics.heightPixels * POPUP_MAX_HEIGHT_RATIO).toInt()
        val height = maxHeight.coerceIn(minHeight, maxHeight)
        window.setLayout(width, height)
        window.setGravity(Gravity.CENTER)
    }

    private fun createPopupWebViewClient(isDialog: Boolean): WebViewClient {
        return object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handlePopupUrlLoading(view, url, isDialog)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url?.toString() ?: return false
                return handlePopupUrlLoading(view, url, isDialog)
            }

        }
    }

    private fun handlePopupUrlLoading(view: WebView, url: String, isDialog: Boolean): Boolean {
        Log.d("WebChromeClient", "popup shouldOverrideUrlLoading isDialog=$isDialog url=$url")
        if (isDialog) {
            if (shouldClosePopupOnRedirect(url)) {
                view.stopLoading()
                closePopUpWebView()
                return true
            }
            return false
        }
        deferDestroyPopupAndOpenExternal(view, url)
        return true
    }

    /** Common OAuth completion signals: window.close(), about:blank, or a non-http(s) callback scheme. */
    private fun shouldClosePopupOnRedirect(url: String): Boolean {
        if (url.isBlank() || url == "about:blank") return true
        val scheme = url.toUri().scheme?.lowercase() ?: return false
        return scheme != "http" && scheme != "https"
    }

    private fun destroyPopupWebView() {
        newWebView?.stopLoading()
        newWebView?.destroy()
        newWebView = null
    }

    fun closePopUpWebView() {
        val dialog = popupDialog ?: run {
            destroyPopupWebView()
            return
        }
        popupDialog = null
        newWebView?.stopLoading()
        if (dialog.isShowing) {
            dialog.dismiss()
        } else {
            destroyPopupWebView()
        }
    }

    private fun deferDestroyPopupAndOpenExternal(view: WebView, url: String) {
        view.stopLoading()
        Handler(Looper.getMainLooper()).post {
            if (parentActivity.isFinishing) return@post
            closePopUpWebView()
            SchemeUtils.openInBrowser(parentActivity, url)
        }
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        // Chromium invokes this when the login page calls window.close().
        if (window == null || window == newWebView) {
            closePopUpWebView()
        }
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        return eventListener.get()?.onShowFileChooser(filePathCallback, fileChooserParams) ?: false
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        if (view == newWebView) return
        eventListener.get()?.onProgressChanged(newProgress / 100f)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        eventListener.get()?.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        eventListener.get()?.onHideCustomView()
    }
}