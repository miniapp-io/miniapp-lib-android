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
import io.miniapp.core.openplatform.common.data.LRUSharedPreferencesCache
import io.miniapp.core.openplatform.common.data.OpenServiceRepository
import io.miniapp.core.openplatform.common.data.SessionProvider
import io.miniapp.core.openplatform.common.network.OkHttpClientProvider
import io.miniapp.core.openplatform.miniapp.MiniAppService
import io.miniapp.core.openplatform.miniapp.MiniAppServiceImpl
import io.miniapp.core.openplatform.miniapp.ui.webview.WebAppLruCache
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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

        private const val TAG = "OpenPlatformPluginImpl"
    }

    private val _cacheKey = "__open_platform_verifier_data"

    private var _sessionProvider : SessionProvider? = null
    val sessionProvider : () -> SessionProvider? = {
       _sessionProvider
    }

    private var isRefreshing = false

    private val repository by lazy {
        OpenServiceRepository.getInstance()
    }

    private var verifier: String? = null

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
        return true==_sessionProvider?.isAuth()
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

        OkHttpClientProvider.isDev = isDev
        OkHttpClientProvider.apiHost = apiHost

        this.verifier = verifier

        if (this._sessionProvider == null) {
            this._sessionProvider = SessionProvider(context)
            LRUSharedPreferencesCache.init(context)
        }

        val verifierProcess: suspend ((()-> Unit)?) -> Unit = { retry ->

            isRefreshing = true

            var useCacheToken = true
            val idToken = LRUSharedPreferencesCache.getValue(_cacheKey) ?: let {
                useCacheToken = false
                val token = idTokenProvider.invoke()
                LRUSharedPreferencesCache.saveValue(_cacheKey, token)
                token
            }

            repository.auth(verifier, idToken).catch {
                isRefreshing = false
                when(it) {
                    is retrofit2.HttpException -> {
                        if (useCacheToken && it.code()==401) {
                            LRUSharedPreferencesCache.saveValue(_cacheKey, null)
                            retry?.invoke() ?: onVerifierFailure.invoke(it.code(), it.message)
                        } else {
                            onVerifierFailure.invoke(it.code(), it.message)
                        }
                    }
                    else -> {
                        onVerifierFailure.invoke(404, it.message)
                    }
                }
            }.collect {
                _sessionProvider?.apply {
                    token = it.accessToken
                }
                isRefreshing = false
                onVerifierSuccess.invoke()
            }
        }

        if (this._sessionProvider?.refreshToken == null) {
            this._sessionProvider?.refreshToken = {
                if (isRefreshing && _sessionProvider?.token == null) {
                    runBlocking {
                        while (isRefreshing && _sessionProvider?.token == null) {
                            kotlinx.coroutines.delay(300)
                        }
                    }
                } else {
                    isRefreshing = true
                    LRUSharedPreferencesCache.saveValue(_cacheKey, null)
                    verifierProcess.invoke(null)
                }
                _sessionProvider?.token
            }
        }

        if(true==this._sessionProvider?.isAuth()) {
            onVerifierSuccess.invoke()
            return
        }

        MainScope().launch {
            verifierProcess {
                MainScope().launch {
                    verifierProcess.invoke(null)
                }
            }
        }
    }

    @SuppressLint("CommitPrefEdits")
    override fun signOut(context: Context) {
        _sessionProvider?.token = null
        LRUSharedPreferencesCache.saveValue(_cacheKey, null)
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