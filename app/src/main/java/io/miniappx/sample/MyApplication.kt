package io.miniappx.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import io.miniapp.core.PLUGIN_OPEN_PLATFORM
import io.miniapp.core.PluginsManager
import io.miniapp.core.openplatform.OpenPlatformPlugin
import io.miniapp.core.openplatform.miniapp.AppConfig
import io.miniapp.core.openplatform.miniapp.IAppDelegate
import io.miniapp.core.openplatform.miniapp.IMiniApp
import io.miniapp.core.openplatform.miniapp.WebAppPreloadParameters
import io.miniappx.sample.trust.TrustWalletProvider
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MyApplication : Application(), IAppDelegate {

    private val currentLan = "en"

    companion object {
        val sendMessageLiveData by lazy {
            MutableLiveData<String?>()
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(updateResources(base, currentLan)) // Set to Arabic
    }

    private fun updateResources(context: Context, language: String): Context {
        val config = context.resources.configuration
        val locale = Locale(language)
        Locale.setDefault(locale)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private var currentActivity: Activity? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (currentActivity == null) {
                    currentActivity = activity
                    sigIn()
                } else {
                    currentActivity = activity
                }
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }

            override fun onActivityDestroyed(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }
        })

        val openPlatformPlugin = PluginsManager.getPlugin<OpenPlatformPlugin>(PLUGIN_OPEN_PLATFORM)!!
        val miniAppService = openPlatformPlugin.getMiniAppService()

        val appConfig = AppConfig.Builder(
            context = this,
            appName = "Sample",
            webAppName = "MiniAppX",
            miniAppHost = listOf("https://openweb3.io","https://miniappx.io","https://t.me"),
            appDelegate = this
        )
            .languageCode("en")
            .isDark(false)
            .maxCachePage(5)
            .bridgeProviderFactory { _,_,_ ->
                WalletProvider()
            }
            .floatWindowSize(90, 159)
            .privacyUrl("https://docs.openweb3.io/docs/standard-miniapp-privacy-policy")
            .termsOfServiceUrl("https://docs.openweb3.io/docs/terms-of-service-for-mini-apps")
            .build()

        miniAppService.setup(config = appConfig)
    }

    private fun sigIn() {
        val openPlatformPlugin = PluginsManager.getPlugin<OpenPlatformPlugin>(PLUGIN_OPEN_PLATFORM)!!
        openPlatformPlugin.signIn(
            context = this,
            isDev = false,
            verifier = "3611ba4b-9a1a-4ac0-97eb-9a5fa5b60ec5",
            apiHost = "https://api.m.openweb3.io",
            idTokenProvider = { idTokenProvider() },
            onVerifierSuccess = {
                currentActivity?.also { activity->
                    if (activity is AppCompatActivity) {
                        preloadApps(activity, activity)
                    }
                }
            },
            onVerifierFailure = { code, message ->
                Log.e("Sample", "Verifier Err, code= $code, message: $message")
            })
    }

    private fun preloadApps(owner: LifecycleOwner, context: Context) {
        listOf("2lv8dp7JjF2AU0iEk2rMYUaySjU","2s8A4zgsdwfLgUsFoMdg8kaW73b").forEach {
            val config = WebAppPreloadParameters.Builder()
                .owner(owner)
                .context(context)
                .miniAppId(it)
                .build()

            MainScope().launch {
                miniAppService.preload( config = config)
            }
        }
    }

    private suspend fun idTokenProvider(): String = suspendCoroutine { continuation -> run {
        continuation.resume("eyJhbGciOiJSUzI1NiIsImtpZCI6ImhrOTV2M3p6YjdjczltNHJlYXN3dXl6aHRoemFkZ3cxczh0eTRjbjYiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjUzNTIzMjc0ODAsImlhdCI6MTc1MjMyNzQ4MCwic3ViIjoidGVzdCIsInVzZXJfaWQiOiJ0ZXN0IiwidXNlcm5hbWUiOiJ0ZXN0In0.Kv82MMZp-QvWnPxHwPFWy0RnJ36AepIYqqaX9S64ABYLnIHk51mXDypTEd5dMUc5kp_UT1H9NxTTArXxsJCRBl84zp2-vN4cswXixVcZj4f_moadTQQvBEnZ52VUh-xIbFMSPFrIjsbDxIsBMUYl5DAMvhr2swnCswdzm5x6qTHoxuqCEZMX0CwJvZZbbMU3w5uwZ2CmoAa27TURdxH0_7UqVGfojrQhTfEld5vfgZztWTQUHZY0t6WmZ1rwOXEJuqaePX3kyhfytECFlx6Nd2xwNw2DHOhDGFEM47AVv8J7riFF3etrkVkhHSz5JXyIhfG7PSldHgdvbRk1qtDGfA")
    }}

    override suspend fun scanQrCodeForResult(app: IMiniApp, subTitle: String?): String {
        showToast()
        return "TODO: To implement in AppDelegate"
    }

    override suspend fun requestPhoneNumberToPeer(app: IMiniApp): Boolean {
        showToast()
        return false
    }

    override suspend fun canUseBiometryAuth(app: IMiniApp): Boolean {
        return true
    }

    override suspend fun updateBiometryToken(app: IMiniApp, token: String?, reason: String?): String? {
        return null
    }

    override fun openBiometrySettings(app: IMiniApp) {
        showToast()
    }

    override suspend fun launchScheme(app: IMiniApp, url: String): Boolean {
        return false
    }

    override  suspend fun attachWithAction(app: IMiniApp, action: String, payload: String): Boolean {
        showToast()
        return true
    }

    override  fun switchInlineQuery(app: IMiniApp, query: String, types: List<String>) {
        showToast()
    }

    @Deprecated("Use shareLink instead")
    override fun shareLinkOrText(app: IMiniApp, linkOrText: String) {
    }

    override fun shareLink(app: IMiniApp, link: String?, text: String?) {
    }

    override suspend fun callCustomMethod(app: IMiniApp, method: String, params: String?, callback: (String?) -> Unit): Boolean {
        return when(method) {
            "getRoomConfig" -> {
                callback.invoke(null)
                true
            }
            "updateRoomConfig" -> {
                callback.invoke(null)
                true
            }
            else -> {
                false
            }
        }
    }

    override suspend fun checkPeerMessageAccess(app: IMiniApp): Boolean {
        showToast()
        return false
    }

    override suspend fun requestPeerMessageAccess(app: IMiniApp): Boolean {
        showToast()
        return true
    }

    override suspend fun sendMessageToPeer(app: IMiniApp, content: String?) : Boolean {
        sendMessageLiveData.postValue(content)
        return true
    }

    override fun onMinimization(app: IMiniApp) {
    }

    override fun onMaximize(app: IMiniApp) {
    }

    override fun onMoreButtonClick(app: IMiniApp, menuTypes: List<String>): Boolean {
        return false
    }

    override fun onMenuButtonClick(app: IMiniApp, type: String) {
        when(type) {
            "SHARE" -> {
                showToast()
                MainScope().launch {
                    val shareInfo = app.getShareInfo()
                    if (null == shareInfo?.iconUrl) {
                        return@launch
                    }
                }
            }
            "FEEDBACK" -> {
                showToast()
            }
        }
    }

    override fun onApiError(code: Int, message: String?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        if (code == 401) {
            sigIn()
        }
    }

    private fun showToast() {
        Toast.makeText(applicationContext,"To implement in AppDelegate", Toast.LENGTH_SHORT).show()
    }
}