package io.miniapp.core.openplatform.common.apis.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AuthParams(
    val verifier: String,
    val options: Map<String, Any>
)

@JsonClass(generateAdapter = true)
internal data class LaunchParams(
    @Json(name = "app_id")
    val appId:String,
    @Json(name = "start_params")
    val startParams: String?,
    @Json(name = "theme_params")
    val themeParams: Map<String, Any>,
    val platform: String = "Android",
    @Json(name = "language_code")
    val languageCode: String = "en",
    val peer: PeerParams?,
    @Json(name = "tg_webapp_version")
    val tgWebappVersion: String = "8.0"
)

@JsonClass(generateAdapter = true)
internal data class PeerParams(
    @Json(name = "user_id")
    val userId: String?,
    @Json(name = "room_id")
    val roomId: String?,
    @Json(name = "access_hash")
    val accessHash: String?
)

@JsonClass(generateAdapter = true)
internal data class CustomMethodsParams(
    val method:String,
    val params: String?,
    @Json(name = "app_id")
    val appId: String?
)

@JsonClass(generateAdapter = true)
internal data class CompletionParams(
    @Json(name = "prompt_id")
    val promptId: Int,
    val args: Map<String, String>
)

@JsonClass(generateAdapter = true)
internal data class InlineButtonCallbackParams(
    @Json(name = "chat_id")
    val chatId: String,      // Group ID or room ID
    @Json(name = "bot_id")
    val botId: String,       // Bot user ID that sent the button message
    @Json(name = "message_id")
    val messageId: String,   // Button message ID
    @Json(name = "callback_data")
    val callbackData: String  // Button callback data
)