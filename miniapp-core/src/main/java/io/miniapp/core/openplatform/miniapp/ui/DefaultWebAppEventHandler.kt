package io.miniapp.core.openplatform.miniapp.ui

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.TextUtils
import android.widget.TextView
import io.miniapp.core.openplatform.miniapp.IResourcesProvider
import io.miniapp.core.openplatform.miniapp.MiniAppServiceImpl
import io.miniapp.core.openplatform.miniapp.ui.views.PopupButton
import io.miniapp.core.openplatform.miniapp.utils.BotWebViewVibrationEffect
import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import io.miniapp.core.openplatform.miniapp.webapp.IMiniAppDelegate
import io.miniapp.core.openplatform.miniapp.webapp.IWebAppEventHandler
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

internal class DefaultWebAppEventHandler(private val context: Context,
                                         private val resourcesProvider: IResourcesProvider,
                                         private val miniAppDelegate: IMiniAppDelegate
) : IWebAppEventHandler {

    companion object {
        private val WHITELISTED_SCHEMES: List<String> = mutableListOf("http", "https")
        private const val DIALOG_SEQUENTIAL_COOL_DOWN_TIME = 3000
    }

    private var currentDialog: AlertDialog? = null
    private var lastDialogClosed: Long = 0
    private var dialogSequentialOpenTimes = 0
    private var lastDialogCoolDownTime: Long = 0

    private var lastButtonColor: Int = resourcesProvider.getColor("button_color")
    private var lastButtonTextColor: Int = resourcesProvider.getColor("button_text_color")
    private var lastButtonText = ""
    private var buttonData: JSONObject? = null

    override fun onWellKnown(isTelegram: Boolean) {
        TODO("Not yet implemented")
    }

    override fun handleWebScrollMessage(eventType: String, eventData: JSONArray?): Boolean {
        when( eventType ) {
            "allowScroll",
            "web_app_allow_scroll" -> {
                eventData?.also {
                    onWebAllowScroll(it)
                }
            }
        }
        return true
    }

    override fun handleMessage(eventType: String, eventData: JSONObject?): Boolean {
        LogTimber.tag("# DefaultWebAppEventHandler").d(eventType)

        when( eventType ) {
            "web_app_ready"-> {
                onWebAppReady()
            }
            "web_app_switch_inline_query" ->{
                eventData?.also {
                    onWebAppSwitchInlineQuery(it)
                }
            }
            "web_app_set_background_color" ->{
                eventData?.also {
                    onWebAppSetBackgroundColor(it)
                }
            }
            "web_app_set_header_color" -> {
                eventData?.also {
                    onWebAppSetActionBarColor(it)
                }
            }
            "web_app_setup_settings_button" -> {
                eventData?.also {
                    onWebAppSetupSettingButton(it)
                }
            }
            "web_app_request_theme" -> {
                onWebAppRequestTheme()
            }
            "web_app_request_viewport" -> {
                onWebAppRequestViewport()
            }
            "web_app_close" -> {
                onCloseRequested()
            }
            "web_app_expand"-> {
                onWebAppExpand();
            }
            "web_app_setup_closing_behavior"-> {
                eventData?.also {
                    onWebAppSetupClosingBehavior(it)
                }
            }
            "web_app_setup_swipe_behavior" -> {
                eventData?.also {
                    onWebAppSetupExpandBehavior(it)
                }
            }
            "web_app_setup_main_button" -> {
                eventData?.also {
                    onWebAppSetupMainButton(it)
                }
            }
            "web_app_request_fullscreen" -> {
                onWebAppSetupFullScreen(true)
            }
            "web_app_exit_fullscreen" -> {
                onWebAppSetupFullScreen(false)
            }
            "web_app_setup_show_head" -> {
                eventData?.also {
                    onWebAppSetupShowHead(it)
                }
            }
            "web_app_setup_back_button" -> {
                eventData?.also {
                    onWebAppSetupBackButton(it)
                }
            }
            "web_app_data_send" -> {
                eventData?.also {
                    onWebAppSendData(it)
                }
            }
            "web_app_read_text_from_clipboard"-> {
                eventData?.also {
                    onWebAppReadTextFromClipboard(it)
                }
            }
            "web_app_open_popup"-> {
                eventData?.also {
                    onWebAppPopup(it)
                }
            }
            "web_app_close_scan_qr_popup"-> {
                onWebCloseQrCodeScan()
            }
            "web_app_open_scan_qr_popup"-> {
                onWebOpenQrCodeScan(eventData)
            }
            "web_app_open_link"-> {
                eventData?.also {
                    onWebAppOpenLink(it)
                }
            }
            "web_app_open_tg_link" -> {
                eventData?.also {
                    onWebAppOpenTgLink(it)
                }
            }
            "web_app_open_webapp_link"-> {
                eventData?.also {
                    onWebAppOpenDyLink(it)
                }
            }
            "web_app_request_write_access"-> {
                miniAppDelegate.requestSendMessageAccess()
            }
            "web_app_request_phone"-> {
                miniAppDelegate.requestPhone()
            }
            "web_app_trigger_haptic_feedback" -> {
                eventData?.also {
                    onWebAppTriggerHapticFeedback(it)
                }
            }
            "web_app_add_to_home_screen" -> {
                miniAppDelegate.addHomeScreenShortcut()
            }
            "web_app_check_home_screen" -> {
                miniAppDelegate.checkScreenShortcut()
            }
            "web_app_request_safe_area" -> {
                miniAppDelegate.requestSafeArea()
            }
            "web_app_request_content_safe_area" -> {
                miniAppDelegate.requestContentSafeArea()
            }
            else-> {
                return false
            }
        }
        return true
    }

    fun restoreButtonData() {
        buttonData?.also {
            onWebAppSetupMainButton(it)
        }
    }

    private fun onWebAllowScroll(eventData: JSONArray) {
        var x = true
        var y = true
        try {
            x = eventData.optBoolean(0, true)
            y = eventData.optBoolean(1, true)
        } catch (e: java.lang.Exception) {
        }
        miniAppDelegate.allowThisScroll(x, y)
    }

    private fun onWebAppReady() {
        miniAppDelegate.setPageReady()
    }

    private fun onWebAppSwitchInlineQuery(eventData: JSONObject) {
        try {
            val types: MutableList<String> = java.util.ArrayList()
            val arr = eventData.getJSONArray("chat_types")
            for (i in 0 until arr.length()) {
                types.add(arr.getString(i))
            }
            MiniAppServiceImpl.getInstance().appDelegate?.switchInlineQuery(miniAppDelegate.app(), eventData.getString("query"), types)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onCloseRequested() {
        miniAppDelegate.hideAndDestroy()
    }

    private fun onWebAppExpand() {
        miniAppDelegate.expandView(true)
    }

    private fun onWebCloseQrCodeScan() {
        miniAppDelegate.closeQrCodeScan()
    }

    private fun onWebOpenQrCodeScan(eventData: JSONObject?) {
        val subTitle = eventData?.optString("text")
        miniAppDelegate.requestQrCodeScan(subTitle)
    }

    private fun onWebAppSetupSettingButton(eventData: JSONObject) {
        try {
            val newVisible = eventData.optBoolean("is_visible")
            miniAppDelegate.setSettingsButtonVisible(newVisible)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppSetActionBarColor(eventData: JSONObject) {
        try {
            val overrideColorString = eventData.optString("color")
            if (!TextUtils.isEmpty(overrideColorString)) {
                val color = Color.parseColor(overrideColorString)
                if (color != 0) {
                    miniAppDelegate.setActionBarColor(color, true)
                }
            } else {
                val key = eventData.optString("color_key")
                val color = resourcesProvider.getColor(key)
                miniAppDelegate.setActionBarColor(color, false)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppSetBackgroundColor(eventData: JSONObject) {
        try {
            val color = Color.parseColor(
                    eventData.optString(
                        "color",
                        "#ffffff"
                    )
                ) or -0x1000000
            miniAppDelegate.setContainerColor(color)
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppRequestViewport() {
        miniAppDelegate.invalidateViewPortHeight(force = true, fromWebApp = true)
    }

    private fun onWebAppRequestTheme() {
        miniAppDelegate.requestTheme()
    }

    private fun onWebAppReadTextFromClipboard(eventData: JSONObject) {
        try {
            val reqId = eventData.getString("req_id")
            if (!miniAppDelegate.isClipboardAvailable()) {
                miniAppDelegate.clipboardData(reqId, "")
                return
            }
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboardManager.primaryClip
            val data = text?.toString() ?: ""
            miniAppDelegate.clipboardData(reqId, data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppSetupClosingBehavior(eventData: JSONObject) {
        try {
            miniAppDelegate.setupClosingBehavior(eventData.optBoolean("need_confirmation"))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppSetupExpandBehavior(eventData: JSONObject) {
        try {
            miniAppDelegate.setupExpandBehavior(eventData.optBoolean("allow_vertical_swipe"))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppSendData(eventData: JSONObject) {
        try {
            miniAppDelegate.sendWebViewData(eventData.optString("data"))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppSetupMainButton(eventData: JSONObject) {
        try {
            val isActive = eventData.optBoolean("is_active", false)
            val text = eventData.optString("text", lastButtonText).trim { it <= ' ' }
            val isVisible = eventData.optBoolean("is_visible", false) && !TextUtils.isEmpty(text)
            val color =
                if (eventData.has("color")) Color.parseColor(eventData.optString("color")) else lastButtonColor
            val textColor =
                if (eventData.has("text_color")) Color.parseColor(eventData.optString("text_color")) else lastButtonTextColor
            val isProgressVisible = eventData.optBoolean("is_progress_visible", false) && isVisible
            lastButtonColor = color
            lastButtonTextColor = textColor
            lastButtonText = text
            buttonData = eventData
            miniAppDelegate.setupMainButton(
                isVisible,
                isActive,
                text,
                color,
                textColor,
                isProgressVisible
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: java.lang.IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppSetupFullScreen(isFullscreen: Boolean) {
        try {
            miniAppDelegate.setupFullScreen(isFullscreen)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppSetupShowHead(eventData: JSONObject) {
        try {
            miniAppDelegate.setupShowHead(eventData.optBoolean("show_head"))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppSetupBackButton(eventData: JSONObject) {
        try {
            miniAppDelegate.setupBackButton(eventData.optBoolean("is_visible"))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    private fun onWebAppPopup(eventData: JSONObject) {
        try {
            if (currentDialog != null) {
                return
            }

            if (System.currentTimeMillis() - lastDialogClosed <= 150) {
                dialogSequentialOpenTimes++
                if (dialogSequentialOpenTimes >= 3) {
                    dialogSequentialOpenTimes = 0
                    lastDialogCoolDownTime = System.currentTimeMillis()
                    return
                }
            }

            if (System.currentTimeMillis() - lastDialogCoolDownTime <= DIALOG_SEQUENTIAL_COOL_DOWN_TIME) {
                return
            }

            val title = eventData.optString("title")
            val message = eventData.getString("message")
            val buttons = eventData.getJSONArray("buttons")

            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)

            val buttonsList: MutableList<PopupButton> = ArrayList()
            for (i in 0 until buttons.length()) {
                buttonsList.add(
                    PopupButton(
                        buttons.getJSONObject(
                            i
                        )
                    )
                )
            }
            if (buttonsList.size > 3) {
                return
            }
            val notifiedClose = AtomicBoolean()
            if (buttonsList.size >= 1) {
                val btn = buttonsList[0]
                builder.setPositiveButton(btn.text) { dialog, _ ->
                    dialog.dismiss()
                    try {
                        miniAppDelegate.closePopUp(btn.id)
                        notifiedClose.set(true)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            if (buttonsList.size >= 2) {
                val btn = buttonsList[1]
                builder.setNegativeButton(btn.text) { dialog, _ ->
                    dialog.dismiss()
                    try {
                        miniAppDelegate.closePopUp(btn.id)
                        notifiedClose.set(true)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            if (buttonsList.size == 3) {
                val btn = buttonsList[2]
                builder.setNeutralButton(btn.text) { dialog, _ ->
                    dialog.dismiss()
                    try {
                        miniAppDelegate.closePopUp(btn.id)
                        notifiedClose.set(true)
                    } catch (e: JSONException) {
                       e.printStackTrace()
                    }
                }
            }
            builder.setOnDismissListener {
                if (!notifiedClose.get()) {
                    miniAppDelegate.closePopUp("")
                }
                currentDialog = null
                lastDialogClosed = System.currentTimeMillis()
            }

            currentDialog = builder.show()

            if (buttonsList.size >= 1) {
                val btn= buttonsList[0]
                if (btn.textColorKey.isNotEmpty()) {
                    val textView = currentDialog!!.getButton(AlertDialog.BUTTON_POSITIVE) as TextView
                    textView.setTextColor(resourcesProvider.getColor(btn.textColorKey))
                }
            }
            if (buttonsList.size >= 2) {
                val btn = buttonsList[1]
                if (btn.textColorKey.isNotEmpty()) {
                    val textView = currentDialog!!.getButton(AlertDialog.BUTTON_NEGATIVE) as TextView
                    textView.setTextColor(resourcesProvider.getColor(btn.textColorKey))
                }
            }
            if (buttonsList.size == 3) {
                val btn = buttonsList[2]
                if (btn.textColorKey.isNotEmpty()) {
                    val textView = currentDialog!!.getButton(AlertDialog.BUTTON_NEUTRAL) as TextView
                    textView.setTextColor(resourcesProvider.getColor(btn.textColorKey))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onWebAppOpenLink(eventData: JSONObject) {
        try {
            val uri = Uri.parse(eventData.optString("url"))
            if (WHITELISTED_SCHEMES.contains(uri.scheme)) {
                miniAppDelegate.openUrl( uri.toString(), false )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onWebAppOpenDyLink(eventData: JSONObject) {
        try {
            var pathFull = eventData.optString("path_full")
            if (pathFull.startsWith("/")) {
                pathFull = pathFull.substring(1)
            }
            miniAppDelegate.openUrl( "${MiniAppServiceImpl.getInstance().miniAppHost[0]}/${pathFull}", true )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppOpenTgLink(eventData: JSONObject) {
        try {
            var pathFull = eventData.optString("path_full")
            if (pathFull.startsWith("/")) {
                pathFull = pathFull.substring(1)
            }
            miniAppDelegate.openUrl( "${MiniAppServiceImpl.getInstance().miniAppHost[0]}/${pathFull}", true )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onWebAppTriggerHapticFeedback(eventData: JSONObject) {
        try {
            val type = eventData.optString("type")
            var vibrationEffect: BotWebViewVibrationEffect? = null
            when (type) {
                "impact" -> {
                    when (eventData.optString("impact_style")) {
                        "light" -> {
                            vibrationEffect = BotWebViewVibrationEffect.IMPACT_LIGHT
                        }

                        "medium" -> {
                            vibrationEffect = BotWebViewVibrationEffect.IMPACT_MEDIUM
                        }

                        "heavy" -> {
                            vibrationEffect = BotWebViewVibrationEffect.IMPACT_HEAVY
                        }

                        "rigid" -> {
                            vibrationEffect = BotWebViewVibrationEffect.IMPACT_RIGID
                        }

                        "soft" -> {
                            vibrationEffect = BotWebViewVibrationEffect.IMPACT_SOFT
                        }
                    }
                }

                "notification" -> {
                    when (eventData.optString("notification_type")) {
                        "error" -> {
                            vibrationEffect = BotWebViewVibrationEffect.NOTIFICATION_ERROR
                        }

                        "success" -> {
                            vibrationEffect = BotWebViewVibrationEffect.NOTIFICATION_SUCCESS
                        }

                        "warning" -> {
                            vibrationEffect = BotWebViewVibrationEffect.NOTIFICATION_WARNING
                        }
                    }
                }

                "selection_change" -> {
                    vibrationEffect = BotWebViewVibrationEffect.SELECTION_CHANGE
                }
            }
            vibrationEffect?.vibrate()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}