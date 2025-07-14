package io.miniapp.core.openplatform.bot

import androidx.annotation.Keep
import io.miniapp.core.openplatform.common.apis.data.InlineButtonCallbackParams
import io.miniapp.core.openplatform.common.data.OpenServiceRepository
import io.miniapp.core.openplatform.miniapp.DataResult
import io.miniapp.core.openplatform.miniapp.utils.toInfo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Keep
class BotInfo(
    val id: String?,
    val name: String?,
    val token: String?,
    val userId: String?,
    val provider: String?,
    val identifier: String?,
    val bio: String?,
    val avatarUrl: String?,
    val metadata: Map<String, String>?,
    val command: List<CommandInfo>?,
    val createdAt: String?,
    val updatedAt: String?
)

@Keep
data class CommandInfo(
    val command: String?,
    val type: String?,
    val description: String?,
    val options: List<Option>?,
    val scope: Scope?,
    val languageCode: String?
)

@Keep
data class Option(
    val name: String?,
    val type: String?,
    val required: Boolean?,
    val description: String?
)

@Keep
data class Scope(
    val type: String?,
    val chatId: String?,
    val userId: String?
)

@Keep
data class CallbackParams(
    val chatId: String,      // Group ID or room ID
    val botId: String,       // Bot user ID that sent the button message
    val messageId: String,   // Button message ID
    val callbackData: String  // Button callback data
)

@Keep
interface BotService {
    suspend fun getBotInfo(botId:String): DataResult<BotInfo?>

    suspend fun inlineButtonCallback(params: CallbackParams): DataResult<Unit>
}

internal class BotServiceImpl : BotService {

    private val repository by lazy {
        OpenServiceRepository.getInstance()
    }

    override suspend fun getBotInfo(botId: String): DataResult<BotInfo?> =  suspendCancellableCoroutine { continuation -> run {
        MainScope().launch {
            repository.getBotInfo(botId)
                .catch {
                    it.printStackTrace()
                    continuation.resume(DataResult.Failure(it))
                }.collect {
                continuation.resume(DataResult.Success(it.toInfo()))
            }
        } }
    }

    override suspend fun inlineButtonCallback(params: CallbackParams): DataResult<Unit> =  suspendCancellableCoroutine { continuation -> run {
            MainScope().launch {
                repository.inlineCallback(InlineButtonCallbackParams(botId = params.botId, chatId = params.chatId, messageId = params.messageId, callbackData = params.callbackData))
                    .catch {
                        it.printStackTrace()
                        continuation.resume(DataResult.Failure(it))
                    }.collect {
                        continuation.resume(DataResult.Success(it))
                    }
            }
        }
    }
}