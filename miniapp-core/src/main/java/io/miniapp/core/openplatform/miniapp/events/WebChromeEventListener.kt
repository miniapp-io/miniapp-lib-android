package io.miniapp.core.openplatform.miniapp.events

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

internal interface WebChromeEventListener {

    /**
     * Triggered when a webview loading an url.
     *
     * @param process The process about to be rendered.
     */
    fun onProgressChanged(process:Float) {

    }

    /**
     * Triggered when a webview choose file
     *
     */
    fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>,
                          fileChooserParams: WebChromeClient.FileChooserParams): Boolean

    /**
     * Triggered when the web view requests permissions.
     *
     * @param request The permission request.
     */
    fun onPermissionRequest(request: PermissionRequest)

    /**
     * Triggered when the web view requests geo permissions.
     *
     * @param request The permission request.
     */
    fun requestGeoLocation(request: PermissionRequest)

    /**
     * Triggered when the web view icon changed.
     *
     * @param icon The title changed.
     */
    fun onFaviconChanged(icon: Bitmap?)

    /**
     * Triggered when the web view title changed.
     *
     * @param title The title changed.
     */
    fun onTitleChanged(title: String)

    /**
     * Triggered when the web view set fullscreen
     * @param view View?
     * @param callback CustomViewCallback?
     */
    fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {

    }

    /**
     * Triggered when the web view exit fullscreen
     */
    fun onHideCustomView() {

    }
}
