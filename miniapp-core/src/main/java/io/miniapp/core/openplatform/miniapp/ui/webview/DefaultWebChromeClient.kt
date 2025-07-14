package io.miniapp.core.openplatform.miniapp.ui.webview

import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.events.WebEventListener
import io.miniapp.core.openplatform.miniapp.ui.setAgent
import io.miniapp.core.openplatform.miniapp.utils.CheckWebViewPermissionsUseCase
import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import io.miniapp.core.openplatform.miniapp.utils.PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING
import java.lang.ref.WeakReference


internal class DefaultWebChromeClient(
    private val parentActivity: Activity,
    listener: WebEventListener?
) : WebChromeClient() {

    var popupDialog: Dialog? = null
    var newWebView: WebView? = null

    private var eventListener: WeakReference<WebEventListener> = WeakReference(listener)

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        LogTimber.tag("WebChromeClient").d(
            "onReceivedIcon favicon=" + if (icon == null) "null" else icon.getWidth()
                .toString() + "x" + icon.getHeight()
        )
        eventListener.get()?.onFaviconChanged(icon)
        super.onReceivedIcon(view, icon)
    }

    override fun onReceivedTitle(view: WebView?, title: String) {
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
                    newWebView?.destroy()
                    newWebView = null
                }
            }
            popupDialog?.setContentView(newWebView!!)
            popupDialog?.show()
        }

        newWebView?.webChromeClient = this

        eventListener.get()?.also {
            newWebView?.setWebViewClient(object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    Log.d("WebChromeClient shouldInterceptRequest", url)

                    if (isDialog) {
                        return false
                    }
                    return eventListener.get()?.shouldOverrideUrlLoading(view, url, true) ?: false
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    Log.d("WebChromeClient shouldInterceptRequest", request.url?.toString() ?: "ddd")

                    if (isDialog) {
                        return false
                    }
                    return (eventListener.get()?.shouldOverrideUrlLoading(view, request, true) ?: false)
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    Log.d("WebChromeClient shouldInterceptRequest", request?.url?.toString() ?: "ddd")
                    return super.shouldInterceptRequest(view, request)
                }
            })
        }

        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = newWebView
        resultMsg.sendToTarget()

        return true
    }

    private fun closePopUpWebView() {
        popupDialog?.dismiss()
        popupDialog = null
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        closePopUpWebView()
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        return eventListener.get()?.onShowFileChooser(filePathCallback, fileChooserParams) ?: false
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
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