package io.openweb3.sample

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
import androidx.lifecycle.lifecycleScope
import io.miniapp.core.PLUGIN_OPEN_PLATFORM
import io.miniapp.core.PluginsManager
import io.miniapp.core.openplatform.OpenPlatformPlugin
import io.miniapp.core.openplatform.miniapp.AppConfig
import io.miniapp.core.openplatform.miniapp.IAppDelegate
import io.miniapp.core.openplatform.miniapp.IMiniApp
import io.miniapp.core.openplatform.miniapp.WebAppPreloadParameters
import io.openweb3.sample.trust.TrustWalletProvider
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
            webAppName = "OpenWeb3",
            miniAppHost = listOf("https://openweb3.io","https://t.me"),
            appDelegate = this
        )
            .languageCode("en")
            .isDark(false)
            .maxCachePage(5)
            .bridgeProviderFactory { _,_,_ ->
                TrustWalletProvider()
            }
            .floatWindowSize(90, 159)
            .build()

        miniAppService.setup(config = appConfig)
    }

    private fun sigIn() {
        val openPlatformPlugin = PluginsManager.getPlugin<OpenPlatformPlugin>(PLUGIN_OPEN_PLATFORM)!!
        openPlatformPlugin.signIn(
            context = this,
            isDev = false,
            verifier = "123",
            apiHost = null,
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
        continuation.resume("eyJhbGciOiJSUzI1NiIsImtpZCI6IjM5ZTg2YmMxYjBjMjI5NDBkNDRjZjNmNWI4NWZmZWYwYTZjNmQzNzIiLCJ0eXAiOiJKV1QifQ.eyJ1aWQiOiJtdHNvY2lhbEBnbWFpLmNvbSIsImlzcyI6ImZpcmViYXNlLWFkbWluc2RrLTR2ejMwQG10c29jaWFsLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwiZXhwIjoxNzUxODEzMTUxLCJpYXQiOjE3MjA3MDkxNTEsImF1ZCI6Imh0dHBzOi8vaWRlbnRpdHl0b29sa2l0Lmdvb2dsZWFwaXMuY29tL2dvb2dsZS5pZGVudGl0eS5pZGVudGl0eXRvb2xraXQudjEuSWRlbnRpdHlUb29sa2l0Iiwic3ViIjoiZmlyZWJhc2UtYWRtaW5zZGstNHZ6MzBAbXRzb2NpYWwuaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20ifQ.IxKdRdKZhbiYIbRd7nzieti--cEHNA-65rg01Wl6h64cXVviPlZ5MsaueN4uRUODtYs6mdYMAteoy54Wi0GMJzIGMkClUJtbWTOfW1L43YdB4R4XhhVsx2gvF8iCF0MQrDB8ekfyWEqbBdJdbM0BUH0NjSl1Mg15Ta-Rx1cYsk41vmDULpkqHJl93Xjuu2ts1KY7Rs3kNp1NAj9-gC4kHzUG57dmvLqteb4qMmUN7h2tq_np3rdGUBRzxB_YBOnqICAmJ-u6knV_XT08Ep1fB-H_HqR41W_FMgv3EW-V5pApDddJttNjaTy8rJfy2xL9mOhQV0OH-1vHjm4Mz2Jpeg")
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

    override fun shareLinkOrText(app: IMiniApp, linkOrText: String) {
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