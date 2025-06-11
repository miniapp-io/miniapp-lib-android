package io.miniapp.core.openplatform.miniapp.ui
import android.graphics.Color
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.IResourcesProvider
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import org.json.JSONObject
import java.util.Locale

enum class ThemeColors(val colorKey: String, val lightColor: String, val darkColor: String) {
    BgColor("bg_color", "#FFFFFF", "#111111"),
    SecondaryBgColor("secondary_bg_color", "#F0F0F0", "#222222"),
    TextColor("text_color", "#222222", "#FFFFFF"),
    HintColor("hint_color", "#A8A8A8", "#A8A8A8"),
    LinkColor("link_color", "#A8A8A8", "#A8A8A8"),
    ButtonColor("button_color", "#A8A8A8", "#A8A8A8"),
    ButtonTextColor("button_text_color", "#A8A8A8", "#A8A8A8"),
    HeaderBgColor("header_bg_color", "#527DA3", "#527DA3"),
    AccentTextColor("accent_text_color", "#1C93E3", "#FFFFFF"),
    SectionHeaderTextColor("section_header_text_color", "#3A95D5", "#FFFFFF"),
    SubTitleTextColor("subtitle_text_color", "#82868A", "#FFFFFF"),
    DestructiveTextColor("destructive_text_color", "#CC2929", "#FFFFFF"),
    TextErrColor("text_err_color", "#CC4747", "#CC4747"),
    ProgressCircleColor("progress_circle_color", "#1C93E3", "#1C93E3"),
    SheetScrollUpColor("sheet_scroll_up", "#E1E4E8", "#E1E4E8");
}

internal object DefaultResourcesProvider : IResourcesProvider {

    private var currentLanguageCode = "en"

    private var currentLocale = Locale("en")

    private var _isDark =  !AndroidUtils.isLightTheme()

    private val strKeyMap by lazy {
        mapOf(
            "action_close_anyway" to R.string.action_close_anyway,
            "action_cancel" to R.string.action_cancel,
            "action_allow" to R.string.action_allow,
            "action_not_allow" to R.string.action_not_allow,
            "action_share_contact" to R.string.action_share_contact,
            "action_not_now" to R.string.action_not_now,
            "action_block_all" to R.string.action_block_all,

            "action_share" to R.string.action_share,
            "action_feedback" to R.string.action_feedback,
            "action_reload" to R.string.action_relaod,
            "action_privacy" to R.string.action_privacy,
            "action_terms" to R.string.action_terms,
            "action_settings" to R.string.action_settings,
            "action_shortcut" to R.string.action_shortcut,

            "dialog_close_save_message" to R.string.dialog_close_save_message,
            "dialog_share_phone_title" to R.string.dialog_share_phone_title,
            "dialog_share_phone_message" to R.string.dialog_share_phone_message,
            "dialog_write_access_title" to R.string.dialog_write_access_title,
            "dialog_write_access_message" to R.string.dialog_write_access_message,
            "webview_permissions_resource_title" to R.string.webview_permissions_resource_title,
            "webview_request_geolocation_permission" to R.string.webview_request_geolocation_permission,
            "webview_request_microphone_permission" to R.string.webview_request_microphone_permission,
            "webview_request_camera_permission" to R.string.webview_request_camera_permission,
            "webview_request_camera_mic_permission" to R.string.webview_request_camera_mic_permission,
        )
    }

    override fun isDark(): Boolean {
        return _isDark
    }

    fun setUserInterfaceStyle(isDark: Boolean) {
        if (isDark == _isDark) {
            return
        }
        _isDark = isDark
    }

    override fun getThemes() = if (isDark()) ThemeColors.values()
        .associate { it.colorKey to it.darkColor } else ThemeColors.values()
        .associate { it.colorKey to it.lightColor }
    override fun getColor(key: String): Int {
        return ThemeColors.values().firstOrNull{ it.colorKey == key }?.let {
            if (isDark()){
                Color.parseColor(it.darkColor)
            } else {
                Color.parseColor(it.lightColor)
            }
        } ?: Color.parseColor("#FFFFFF")
    }

    override fun getString(key: String): String {
        val strRes = strKeyMap[key]
        return if (null==strRes) {
            ""
        } else {
            AndroidUtils.getString(strRes)
        }
    }

    override fun getString(key: String, vararg withValue: String): String {
        val strRes = strKeyMap[key]
        return if (null==strRes) {
            ""
        } else {
            formatString(AndroidUtils.getString(strRes), *withValue)
        }
    }

    private fun formatString(template: String, vararg values: String): String {
        return template.format(*values)
    }

    override fun makeThemeParams(): JSONObject? {
        try {
            val jsonObject = JSONObject()
            val backgroundColor: Int = getColor("bg_color")
            jsonObject.put("bg_color", backgroundColor)
            jsonObject.put("section_bg_color", getColor("section_bg_color"))
            jsonObject.put("secondary_bg_color", getColor("secondary_bg_color"))
            jsonObject.put("text_color", getColor("text_color"))
            jsonObject.put("hint_color", getColor("hint_color"))
            jsonObject.put("link_color", getColor("link_color"))
            jsonObject.put("button_color", getColor("button_color"))
            jsonObject.put("button_text_color", getColor("button_text_color"))
            jsonObject.put("header_bg_color", getColor("header_bg_color"))
            jsonObject.put("accent_text_color",
                blendOver(
                    backgroundColor,
                    getColor("accent_text_color")
                )
            )
            jsonObject.put("section_header_text_color",
                blendOver(
                    backgroundColor,
                    getColor("section_header_text_color")
                )
            )
            jsonObject.put("subtitle_text_color",
                blendOver(
                    backgroundColor,
                    getColor("subtitle_text_color")
                )
            )
            jsonObject.put("destructive_text_color",
                blendOver(
                    backgroundColor,
                    getColor("destructive_text_color")
                )
            )
            return jsonObject
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun setLocale(languageCode: String) {
        currentLanguageCode = languageCode
        val parts = languageCode.split("-")
        currentLocale = if (parts.size > 1) {
            Locale(parts[0], parts[1])
        } else {
            Locale(parts[0])
        }
    }

    fun getLocale() = currentLocale

    fun getLanguageCode() = currentLanguageCode

    private fun blendOver(backgroundColor: Int, colorText: Int): Int { // B over A
        // over operator: https://en.wikipedia.org/wiki/Alpha_compositing#Description
        val aB = Color.alpha(colorText) / 255f
        val aA = Color.alpha(backgroundColor) / 255f
        val aC = aB + aA * (1 - aB)
        return if (aC == 0f) 0 else Color.argb(
            (aC * 255).toInt(),
            ((Color.red(colorText) * aB + Color.red(backgroundColor) * aA * (1 - aB)) / aC).toInt(),
            ((Color.green(colorText) * aB + Color.green(backgroundColor) * aA * (1 - aB)) / aC).toInt(),
            ((Color.blue(colorText) * aB + Color.blue(backgroundColor) * aA * (1 - aB)) / aC).toInt()
        )
    }
}