package io.miniapp.core.openplatform.miniapp.ui.proxy

import io.miniapp.core.openplatform.miniapp.IResourcesProvider
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.webapp.IWebApp
import org.json.JSONException
import org.json.JSONObject

internal class WebAppProxy(val webApp: IWebApp, val resourcesProvider: IResourcesProvider) {

    private var buttonData: String? = null
    private var currentPaymentSlug: String? = null
    private var lastClickMs: Long = 0

    fun reload() {
        lastClickMs = 0
    }

    fun notifyViewportChange(viewPortHeight: Int, isStable: Boolean, lastExpanded:Boolean) {
        try {
            val data = JSONObject()
            data.put("height", viewPortHeight / AndroidUtils.density)
            data.put("is_state_stable", isStable)
            data.put("is_expanded", lastExpanded)
            webApp.postCommonEventToMiniApp("viewport_changed", data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun notifySafeAreaChanged(data: JSONObject?) {
        webApp.postCommonEventToMiniApp("safe_area_changed", data)
    }

    fun notifyContentSafeAreaChanged(data: JSONObject?) {
        webApp.postCommonEventToMiniApp("content_safe_area_changed", data)
    }

    fun notifyThemeChanged() {
        webApp.postCommonEventToMiniApp("theme_changed", resourcesProvider.makeThemeParams())
    }

    fun notifyPopupClosed(btnId:String) {
        webApp.postCommonEventToMiniApp("popup_closed", JSONObject().put("button_id", btnId))
    }

    fun notifyQrPopupClosed() {
        webApp.postCommonEventToMiniApp("scan_qr_popup_closed", null)
    }

    fun responseQrScanResult(result: String) {
        webApp.postCommonEventToMiniApp("qr_text_received", JSONObject().put("data", result))
    }

    fun clipboardData(reqId: String, content: String) {
        webApp.postCommonEventToMiniApp("clipboard_text_received", JSONObject().put("req_id", reqId).put("data", content))
    }

    fun responseWriteAccess(allowed:Boolean) {
        val data = JSONObject()
        data.put("status", if (allowed) "allowed" else "cancelled")
        webApp.postCommonEventToMiniApp("write_access_requested", data)
    }

    fun responsePhone(sent:Boolean) {
        val data = JSONObject()
        data.put("status", if (sent) "sent" else "cancelled")
        webApp.postCommonEventToMiniApp("phone_requested", data)
    }

    fun responseBackPress() {
        webApp.postCommonEventToMiniApp("back_button_pressed", null)
    }

    fun responseShortcutStatus(isAdd: Boolean, isFailure: Boolean, status: String?) {
        if (isAdd) {
            webApp.postCommonEventToMiniApp("home_screen_added", null)
            return
        }
        if (isFailure) {
            val data = JSONObject()
            data.put("error", "UNSUPPORTED")
            webApp.postCommonEventToMiniApp(
                "home_screen_failed",
                data
            )
        } else {
            val data = JSONObject()
            data.put("status", status ?: "unsupported")
            webApp.postCommonEventToMiniApp(
                "home_screen_checked",
                data
            )
        }
    }

    fun responseFullscreenChange(isFullscreen: Boolean) {
        val data = JSONObject()
        data.put("is_fullscreen", isFullscreen)
        webApp.postCommonEventToMiniApp("fullscreen_changed", data)
    }

    fun restoreButtonData() {
        if (buttonData != null) {
            webApp.postCommonEventToMiniApp("web_app_setup_main_button", JSONObject())
        }
    }

    @JvmOverloads
    fun onInvoiceStatusUpdate(slug: String?, status: String?, ignoreCurrentCheck: Boolean = false) {
        try {
            val data = JSONObject()
            data.put("slug", slug)
            data.put("status", status)
            webApp.postCommonEventToMiniApp("invoice_closed", data)
            if (!ignoreCurrentCheck && currentPaymentSlug == slug) {
                currentPaymentSlug = null
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun onSettingsButtonPressed() {
        lastClickMs = System.currentTimeMillis()
        webApp.postCommonEventToMiniApp("settings_button_pressed", null)
    }

    fun onMainButtonPressed() {
        lastClickMs = System.currentTimeMillis()
        webApp.postCommonEventToMiniApp("main_button_pressed", null)
    }
}