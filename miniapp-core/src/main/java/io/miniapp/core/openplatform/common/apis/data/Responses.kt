package io.miniapp.core.openplatform.common.apis.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class VerifierDto(
    @Json(name = "access_token")
    val accessToken: String
)
@JsonClass(generateAdapter = true)
internal data class LaunchMiniAppDto(
    val url: String,
    @Json(name = "miniapp_obj")
    val miniAppDto: MiniAppDto?
)

@JsonClass(generateAdapter = true)
internal data class LaunchDAppDto(
    @Json(name = "redirect_url")
    val redirectUrl: String,
    @Json(name = "is_risk")
    val isRisk:Boolean
)

@JsonClass(generateAdapter = true)
internal data class DAppDto(
    val id: String,
    val title: String?,
    val url: String?,
    val description: String?,
    @Json(name = "short_description")
    val shortDescription: String?,
    @Json(name = "icon_url")
    val iconUrl: String?,
    @Json(name = "banner_url")
    val bannerUrl: String?,
    @Json(name = "create_at")
    val createAt: Long?,
    @Json(name = "update_at")
    val updateAt: Long?,
    @Json(name = "is_share_enabled")
    val isShareEnabled: Boolean?
)
@JsonClass(generateAdapter = true)
internal data class AppSettings(
    /**
     * Page style, default is 'default'
     * default: default fullscreen mode
     * modal: modal dialog mode
     */
    @Json(name = "view_style")
    val viewStyle: String?,

    /**
     * Toolbar style (title, back button, background color), default is 'default'
     * default: default system toolbar, shows title, back button, with background color
     * custom: custom toolbar on web, hides toolbar, i.e. no title, back button, or background
     */
    @Json(name = "navigation_style")
    val navigationStyle: String?,

    /**
     * Horizontal gesture switch, default is true
     * true: allow horizontal gestures
     * false: disable horizontal gestures
     */
    @Json(name = "allow_horizontal_swipe")
    val allowHorizontalSwipe: Boolean?,

    /**
     * Vertical gesture switch, default is false
     * true: allow vertical gestures
     * false: disable vertical gestures
     * Web SDK supports controlling vertical gestures, prefer settings in Web
     */
    @Json(name = "allow_vertical_swipe")
    val allowVerticalSwipe: Boolean?
)

@JsonClass(generateAdapter = true)
internal data class MiniAppDto(
    val id: String,
    val identifier: String?,
    val title: String?,
    val description: String?,
    @Json(name = "short_description")
    val shortDescription: String?,
    @Json(name = "icon_url")
    val iconUrl: String?,
    @Json(name = "banner_url")
    val bannerUrl: String?,
    @Json(name = "bot_id")
    val botId: String?,
    @Json(name = "bot_identifier")
    val botName: String?,
    @Json(name = "create_at")
    val createAt: Long?,
    @Json(name = "update_at")
    val updateAt: Long?,
    @Json(name = "options")
    val options: AppSettings?,
    @Json(name = "is_share_enabled")
    val isShareEnabled: Boolean?
)

@JsonClass(generateAdapter = true)
internal data class MiniAppResponse(
    val items: List<MiniAppDto>?,
)

@JsonClass(generateAdapter = true)
internal class BotDto(
    val id: String?,
    val name: String?,
    val token: String?,
    @Json(name = "user_id")
    val userId: String?,
    val provider: String?,
    val identifier: String?,
    val bio: String?,
    @Json(name = "avatar_url")
    val avatarUrl: String?,
    val metadata: Map<String, String>?,
    val commands: List<CommandDto>?,
    @Json(name = "created_at")
    val createdAt: String?,
    @Json(name = "updated_at")
    val updatedAt: String?
)
@JsonClass(generateAdapter = true)
internal data class CommandDto(
    val command: String?,
    val type: String?,
    val description: String?,
    val options: List<OptionDto>?,
    val scope: ScopeDto?,
    @Json(name = "language_code")
    val languageCode: String?
)

@JsonClass(generateAdapter = true)
internal data class OptionDto(
    val name: String?,
    val type: String?,
    val required: Boolean?,
    val description: String?
)

@JsonClass(generateAdapter = true)
internal data class ScopeDto(
    val type: String?,
    @Json(name = "chat_id")
    val chatId: String?,
    @Json(name = "user_id")
    val userId: String?
)

@JsonClass(generateAdapter = true)
internal data class CustomMethodsResp(
    val result:String?,
)

@JsonClass(generateAdapter = true)
internal data class BotMenuDto(
    val name: String?,
    val description: String?
)

@JsonClass(generateAdapter = true)
internal data class BotMenusResponse(
    val items: List<BotMenuDto>?,
)