package io.miniapp.core.openplatform

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.Keep
import io.miniapp.core.PLUGIN_OPEN_PLATFORM
import io.miniapp.core.Plugin
import io.miniapp.core.PluginName
import io.miniapp.core.PluginsManager
import io.miniapp.core.openplatform.ai.AIService
import io.miniapp.core.openplatform.ai.AIServiceImpl
import io.miniapp.core.openplatform.bot.BotService
import io.miniapp.core.openplatform.bot.BotServiceImpl
import io.miniapp.core.openplatform.miniapp.MiniAppService
import io.miniapp.core.openplatform.miniapp.MiniAppServiceImpl
import io.miniapp.core.openplatform.miniapp.ui.webview.WebAppLruCache
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ThrowsIllegalStateException
@Keep
interface OpenPlatformPlugin : Plugin {
    fun signIn(context: Context,
               verifier: String,
               apiHost: String,
               isDev: Boolean = false,
               idTokenProvider: suspend () -> String,
               onVerifierSuccess: () -> Unit,
               onVerifierFailure: (Int, String?) -> Unit)

    fun signOut(context: Context)

    fun isAuth(): Boolean

    fun getMiniAppService() : MiniAppService

    fun getBotService() : BotService

    fun getAiService() : AIService
}

internal class OpenPlatformPluginImpl(private val  pluginName: @PluginName String) :
    OpenPlatformPlugin {

    companion object {
        fun getInstance(): OpenPlatformPluginImpl {
            return PluginsManager.getPlugin(PLUGIN_OPEN_PLATFORM)!!
        }
    }
    private val _botService by lazy {
        BotServiceImpl()
    }

    private val _aiService by lazy {
        AIServiceImpl()
    }

    override fun getMiniAppService()  = MiniAppServiceImpl.getInstance()

    override fun getBotService() = _botService
    override fun getAiService() = _aiService

    override fun isAuth(): Boolean {
        return AuthManager.isAuth()
    }

    @SuppressLint("CommitPrefEdits")
    override fun signIn(
        context: Context,
        verifier: String,
        apiHost: String,
        isDev: Boolean,
        idTokenProvider: suspend () -> String,
        onVerifierSuccess: () -> Unit,
        onVerifierFailure: (Int, String?) -> Unit) {

        AuthManager.init(context, verifier, apiHost, isDev, idTokenProvider)

        if (AuthManager.isAuth()) {
            onVerifierSuccess.invoke()
            return
        }

        MainScope().launch {
            val result = AuthManager.signIn()
            result.fold(
                onSuccess = {
                    onVerifierSuccess.invoke()
                },
                onFailure = { exception ->
                    when (exception) {
                        is retrofit2.HttpException -> {
                            onVerifierFailure.invoke(exception.code(), exception.message)
                        }
                        else -> {
                            onVerifierFailure.invoke(500, exception.message)
                        }
                    }
                }
            )
        }
    }

    @SuppressLint("CommitPrefEdits")
    override fun signOut(context: Context) {
        AuthManager.signOut()
        WebAppLruCache.removeAll()
    }

    override fun getName(): String {
        return pluginName
    }

    override fun load(): Boolean {
        return true
    }

    override fun unLoad() {
        WebAppLruCache.removeAll()
    }

}