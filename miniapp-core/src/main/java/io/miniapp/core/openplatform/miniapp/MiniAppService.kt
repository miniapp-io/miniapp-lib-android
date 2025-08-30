package io.miniapp.core.openplatform.miniapp
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.annotation.Keep
import androidx.lifecycle.LifecycleOwner
import com.squareup.moshi.Json
import io.miniapp.bridge.BridgeProvider
import io.miniapp.bridge.BridgeProviderFactory
import io.miniapp.core.openplatform.ThrowsIllegalStateException
import org.json.JSONObject

/**
 * Define WebApp Types
 */
@Keep
const val TYPE_WEB_VIEW_BUTTON = 0
@Keep
const val TYPE_SIMPLE_WEB_VIEW_BUTTON = 1
@Keep
const val TYPE_BOT_MENU_BUTTON = 2
@Keep
const val TYPE_WEB_VIEW_BOT_APP = 3

/**
 * Define WebView Type
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(value = [TYPE_WEB_VIEW_BUTTON, TYPE_SIMPLE_WEB_VIEW_BUTTON,
    TYPE_BOT_MENU_BUTTON, TYPE_WEB_VIEW_BOT_APP])
@Keep
annotation class WebViewType

@Keep
open class DataResult<T> {
    @Keep
    data class Success<T>(val data: T) : DataResult<T>()

    @Keep
    data class Failure<T>(val throwable: Throwable) : DataResult<T>()

    fun isSuccess() = this is Success

    fun data() = (this as? Success)?.data

    fun throwable() = (this as? Failure)?.throwable
}

/**
 * WebAppParameters
 * @property owner LifecycleOwner
 * @property context Context
 * @constructor
 */
@Keep
open class WebAppLaunchParameters(open val owner: LifecycleOwner,
                                  open val context: Context)


/**
 * Base Launch Parameters Builder
 */
@Keep
abstract class BaseBuilder<T : WebAppLaunchParameters> {

    protected var owner: LifecycleOwner? = null
    protected var context: Context? = null
    protected var botId: String? = null
    protected var botName: String? = null
    protected var miniAppId: String? = null
    protected var miniAppName: String? = null

    protected var id: String? = null

    protected var useWeChatStyle: Boolean = true
    protected var url: String? = null
    protected var startParam: String? = null
    protected var params: Map<String,String>? = null
    protected var useModalStyle: Boolean = false
    protected var useCustomNavigation: Boolean = false
    protected var isLocal: Boolean = false
    protected var isSystem: Boolean = false
    protected var useCache: Boolean = true
    protected var isLaunchUrl: Boolean = false
    protected var autoExpand: Boolean = false
    protected var peer: Peer? = null

    protected var parentView: ViewGroup? = null
    protected var layoutParams: LayoutParams? = null

    protected var bridgetProvider: BridgeProvider? = null
    protected var onDismissListener: (() -> Unit)? = null
    protected var onErrorCallback: ((Int,String?) -> Unit)? = null
    protected var actionBarBuilder: ((()->Unit, ()->Unit) -> Pair<FrameLayout.LayoutParams, View>)? = null

    fun id(id: String?) = apply {
        this.id = id
    }

    fun owner(owner: LifecycleOwner) = apply {
        this.owner = owner
    }

    fun context(context: Context) = apply {
        this.context = context
    }

    fun botId(botId: String?) = apply {
        this.botId = botId
    }

    fun botName(botName: String?) = apply {
        this.botName = botName
    }

    fun miniAppId(miniAppId: String?) = apply {
        this.miniAppId = miniAppId
    }

    fun miniAppName(miniAppName: String?) = apply {
        this.miniAppName = miniAppName
    }

    fun useWeChatStyle(useWeChatStyle: Boolean) = apply {
        this.useWeChatStyle = useWeChatStyle
    }

    fun url(url: String?) = apply {
        this.url = url
    }

    fun startParam(startParam: String?) = apply {
        this.startParam = startParam
    }

    fun params(params: Map<String,String>?) = apply {
        this.params = params
    }

    fun useModalStyle(useModalStyle: Boolean) = apply {
        this.useModalStyle = useModalStyle
    }

    fun useCustomNavigation(useCustomNavigation: Boolean) = apply {
        this.useCustomNavigation = useCustomNavigation
    }

    fun isLocal(isLocal: Boolean ) = apply {
        this.isLocal = isLocal
    }

    fun isSystem(isSystem: Boolean) = apply {
        this.isSystem = isSystem
    }

    fun useCache(useCache: Boolean) = apply {
        this.useCache = useCache
    }

    fun isLaunchUrl(isLaunchUrl: Boolean) = apply {
        this.isLaunchUrl = isLaunchUrl
    }

    fun autoExpand(autoExpand: Boolean) = apply {
        this.autoExpand = autoExpand
    }

    fun peer(peer: Peer?) = apply {
        this.peer = peer
    }

    fun parentView(parentView: ViewGroup) = apply {
        this.parentView = parentView
    }

    fun layoutParams(layoutParams: LayoutParams) = apply {
        this.layoutParams = layoutParams
    }

    fun bridgetProvider(bridgetProvider: BridgeProvider) = apply {
        this.bridgetProvider = bridgetProvider
    }

    fun actionBarBuilder(actionBarBuilder: (()->Unit, ()->Unit) -> Pair<FrameLayout.LayoutParams, View>) = apply {
        this.actionBarBuilder = actionBarBuilder
    }

    fun onDismissListener(onDismissListener: (() -> Unit)) = apply {
        this.onDismissListener = onDismissListener
    }

    fun onErrorCallback(onErrorCallback: ((Int,String?) -> Unit)) = apply {
        this.onErrorCallback = onErrorCallback
    }

    open fun require() {
        requireNotNull(this.owner) { "Invalid owner" }
        requireNotNull(this.context) { "Invalid context" }
    }

    abstract fun build(): T
}

@Keep
data class Peer(
    val userId: String?,
    val roomId: String?,
    val accessHash: String?
)

class DAppLaunchWParameters private constructor(
    override val owner: LifecycleOwner,
    override val context: Context,
    val id: String?,
    val url: String?,
    val bridgetProvider: BridgeProvider?,
    val actionBarBuilder: ((()->Unit, ()->Unit) -> Pair<FrameLayout.LayoutParams, View>)? = null,
    var onErrorCallback: ((Int,String?) -> Unit)? = null,
    val onDismissListener: (() -> Unit)? = null) : WebAppLaunchParameters(owner = owner, context = context)  {

    @Keep
    class Builder : BaseBuilder<DAppLaunchWParameters>() {

        override fun require() {
            super.require()
            if (true == this.url?.isBlank() && true == this.id?.isBlank()) {
                throw IllegalStateException("invalid d-app")
            }
        }

        @ThrowsIllegalStateException
        override fun build(): DAppLaunchWParameters {
            require()

            return DAppLaunchWParameters(
                owner = this.owner!!,
                context = this.context!!,
                id = this.id,
                url = this.url,
                bridgetProvider = this.bridgetProvider,
                actionBarBuilder = this.actionBarBuilder,
                onDismissListener = this.onDismissListener,
                onErrorCallback = this.onErrorCallback
            )
        }
    }
}

/**
 * WebAppPreloadParameters
 * @property owner LifecycleOwner
 * @property context Context
 * @property botName String?
 * @property miniAppName String?
 * @property startParam String?
 * @property accessHash String?
 * @property url String?
 * @constructor
 */
class WebAppPreloadParameters private constructor (
    override val owner: LifecycleOwner,
    override val context: Context,
    val botId: String? = null,
    val botName: String? = null,
    val miniAppId: String? = null,
    val miniAppName: String? = null,
    val startParam: String? = null,
    val params: Map<String,String>? = null,
    val peer: Peer? = null,
    val bridgetProvider: BridgeProvider? = null,
    val url: String? = null) : WebAppLaunchParameters(owner = owner, context = context) {
    @Keep
    class  Builder : BaseBuilder<WebAppPreloadParameters>() {

        override fun require() {
            super.require()
            if (true == this.miniAppId?.isEmpty() &&
                (true == this.url?.isEmpty() &&
                        ((true == this.botName?.isEmpty() && true == this.botId?.isEmpty())
                                || true == this.miniAppName?.isEmpty()))) {
                throw IllegalStateException("invalid mini-app")
            }
        }

        @ThrowsIllegalStateException
        override fun build(): WebAppPreloadParameters {
            require()

            return  WebAppPreloadParameters(
                owner = this.owner!!,
                context = this.context!!,
                miniAppId = this.miniAppId,
                miniAppName = this.miniAppName,
                botId = this.botId,
                botName = this.botName,
                startParam = this.startParam,
                params = this.params,
                peer = this.peer,
                url = this.url,
                bridgetProvider = this.bridgetProvider
            )
        }
    }
}

/**
 * WebAppLaunchWithDialogParameters
 * @property owner LifecycleOwner
 * @property context Context
 * @property botName String?
 * @property miniAppName String?
 * @property url String?
 * @property startParam String?
 * @property type Int
 * @property useWeChatStyle Boolean
 * @property useModalStyle Boolean
 * @property isLocal Boolean
 * @property isLaunchUrl Boolean
 * @property autoExpand Boolean
 * @property useCache Boolean
 * @property accessHash String?
 * @property onDismissListener Function0<Unit>?
 * @constructor
 */
class WebAppLaunchWithDialogParameters private constructor(
    override val owner: LifecycleOwner,
    override val context: Context,
    val botId: String? = null,
    val botName: String?,
    val miniAppId: String?,
    val miniAppName: String?,
    val url: String? = null,
    val startParam: String? = null,
    val params: Map<String,String>? = null,
    @WebViewType
    val type: Int = TYPE_SIMPLE_WEB_VIEW_BUTTON,
    val useWeChatStyle: Boolean = true,
    val useModalStyle: Boolean = false,
    val useCustomNavigation: Boolean = false,
    val isLocal: Boolean = false,
    val isSystem: Boolean = false,

    val isLaunchUrl: Boolean = false,
    val autoExpand: Boolean = false,
    val useCache: Boolean = true,
    val peer: Peer? = null,
    val bridgetProvider: BridgeProvider? = null,
    val actionBarBuilder: ((()->Unit, ()->Unit) -> Pair<FrameLayout.LayoutParams, View>)? = null,
    var onErrorCallback: ((Int,String?) -> Unit)? = null,
    val onDismissListener: (() -> Unit)? = null) : WebAppLaunchParameters(owner = owner, context = context ) {

    @Keep
    class Builder: BaseBuilder<WebAppLaunchWithDialogParameters>() {

        override fun require() {
            super.require()

            if (true == this.miniAppId?.isEmpty() &&
                (true == this.url?.isEmpty() &&
                        ((true == this.botName?.isEmpty() && true == this.botId?.isEmpty())
                                || true == this.miniAppName?.isEmpty()))) {
                throw IllegalStateException("invalid mini-app")
            }
        }

        @ThrowsIllegalStateException
        override fun build(): WebAppLaunchWithDialogParameters {
            require()

            return WebAppLaunchWithDialogParameters(
                owner = this.owner!!,
                context = this.context!!,
                botId = this.botId,
                botName = this.botName,
                miniAppId = this.miniAppId,
                miniAppName = this.miniAppName,
                useWeChatStyle = this.useWeChatStyle,
                url = this.url,
                startParam = this.startParam,
                params = this.params,
                useModalStyle = this.useModalStyle,
                useCustomNavigation = this.useCustomNavigation,
                isLocal = this.isLocal,
                isSystem = this.isSystem,
                useCache = this.useCache,
                autoExpand = this.autoExpand,
                peer = this.peer,
                isLaunchUrl = this.isLaunchUrl,
                bridgetProvider = this.bridgetProvider,
                actionBarBuilder = actionBarBuilder,
                onDismissListener = onDismissListener,
                onErrorCallback = onErrorCallback
            )
        }
    }
}

/**
 * WebAppLaunchWithParentParameters
 * @property owner LifecycleOwner
 * @property context Context
 * @property botName String?
 * @property miniAppName String?
 * @property url String?
 * @property startParam String?
 * @property type Int
 * @property useWeChatStyle Boolean
 * @property useModalStyle Boolean
 * @property isLocal Boolean
 * @property useCache Boolean
 * @property isLaunchUrl Boolean
 * @property autoExpand Boolean
 * @property accessHash String?
 * @property parentView ViewGroup?
 * @property layoutParams LayoutParams?
 * @property onDismissListener Function0<Unit>?
 * @constructor
 */
class WebAppLaunchWithParentParameters private constructor(
    override val owner: LifecycleOwner,
    override val context: Context,
    val botId: String?,
    val botName: String?,
    val miniAppId: String?,
    val miniAppName: String?,
    val url: String? = null,
    val startParam: String? = null,
    val params: Map<String,String>? = null,
    @WebViewType
    val type: Int = TYPE_BOT_MENU_BUTTON,
    val useWeChatStyle: Boolean = true,
    val useModalStyle: Boolean = false,
    val useCustomNavigation: Boolean = false,
    val isLocal: Boolean = false,
    val isSystem: Boolean = false,
    val useCache: Boolean = true,
    val isLaunchUrl: Boolean = false,
    val autoExpand: Boolean = false,
    var peer: Peer? = null,
    val parentView: ViewGroup? = null,
    val layoutParams: LayoutParams? = null,
    val bridgetProvider: BridgeProvider? = null,
    val actionBarBuilder: ((()->Unit, ()->Unit) -> Pair<FrameLayout.LayoutParams, View>)? = null,
    var onErrorCallback: ((Int,String?) -> Unit)? = null,
    val onDismissListener: (() -> Unit)? = null) : WebAppLaunchParameters(owner = owner, context = context ) {

    @Keep
    class Builder : BaseBuilder<WebAppLaunchWithParentParameters>() {

        override fun require() {
            super.require()
            requireNotNull(this.parentView) { "Invalid parentView" }
            requireNotNull(this.layoutParams) { "Invalid layoutParams" }
            if (true == this.miniAppId?.isEmpty() &&
                (true == this.url?.isEmpty() &&
                        ((true == this.botName?.isEmpty() && true == this.botId?.isEmpty())
                                || true == this.miniAppName?.isEmpty()))) {
                throw IllegalStateException("invalid mini-app")
            }
        }

        @ThrowsIllegalStateException
        override fun build(): WebAppLaunchWithParentParameters {
            require()

            return WebAppLaunchWithParentParameters(
                owner = this.owner!!,
                context = this.context!!,
                botId = this.botId,
                botName = this.botName,
                miniAppId = this.miniAppId,
                miniAppName = this.miniAppName!!,
                useWeChatStyle = this.useWeChatStyle,
                url = this.url,
                startParam = this.startParam,
                params = this.params,
                useModalStyle = this.useModalStyle,
                useCustomNavigation = this.useCustomNavigation,
                isLocal = this.isLocal,
                isSystem = this.isSystem,
                useCache = this.useCache,
                isLaunchUrl = this.isLaunchUrl,
                autoExpand = this.autoExpand,
                peer = this.peer,
                parentView = this.parentView,
                layoutParams = this.layoutParams,
                bridgetProvider = this.bridgetProvider,
                actionBarBuilder = this.actionBarBuilder,
                onDismissListener = this.onDismissListener,
                onErrorCallback = this.onErrorCallback
            )
        }
    }
}

@Keep
class AppConfig private constructor(
    val context: Context,
    val appName: String,
    val webAppName: String,
    val miniAppHost: List<String>,
    val languageCode: String,
    val isDark: Boolean,
    val maxCachePage: Int,
    val resourcesProvider: IResourcesProvider?,
    val bridgeProviderFactory: BridgeProviderFactory? = null,
    val appDelegate: IAppDelegate,
    val floatWindowWidth: Int,
    val floatWindowHeight: Int,
    val redirectionUrl: String?,
    val privacyUrl: String?,
    val termsOfServiceUrl: String?
) {
    @Keep
    class Builder(
        private val context: Context,
        private val appName: String,
        private val webAppName: String,
        private val miniAppHost: List<String>,
        private val appDelegate: IAppDelegate
    ) {
        private var languageCode: String = "en"
        private var isDark: Boolean = false
        private var maxCachePage: Int = 5
        private var resourcesProvider: IResourcesProvider? = null
        private var bridgeProviderFactory: BridgeProviderFactory? = null
        private var floatWindowWidth: Int = 86
        private var floatWindowHeight: Int = 128
        private var redirectionUrl: String? = null
        private var privacyUrl: String? = null
        private var termsOfServiceUrl: String? = null

        fun languageCode(languageCode: String) = apply { this.languageCode = languageCode }
        fun isDark(isDark: Boolean) = apply { this.isDark = isDark }
        fun maxCachePage(maxCachePage: Int) = apply { this.maxCachePage = maxCachePage }
        fun resourcesProvider(resourcesProvider: IResourcesProvider?) = apply { this.resourcesProvider = resourcesProvider }
        fun bridgeProviderFactory(bridgeProviderFactory: BridgeProviderFactory?) = apply { this.bridgeProviderFactory = bridgeProviderFactory }
        fun floatWindowSize(width: Int, height: Int) = apply {
            this.floatWindowWidth = width
            this.floatWindowHeight = height
        }

        fun redirectionUrl(redirectionUrl: String) = apply { this.redirectionUrl = redirectionUrl }
        fun privacyUrl(privacyUrl: String) = apply { this.privacyUrl = privacyUrl }
        fun termsOfServiceUrl(termsOfServiceUrl: String) = apply { this.termsOfServiceUrl = termsOfServiceUrl }

        fun build(): AppConfig {
            return AppConfig(
                context = context,
                appName = appName,
                webAppName = webAppName,
                miniAppHost = miniAppHost,
                languageCode = languageCode,
                isDark = isDark,
                maxCachePage = maxCachePage,
                resourcesProvider = resourcesProvider,
                bridgeProviderFactory = bridgeProviderFactory,
                appDelegate = appDelegate,
                floatWindowWidth = floatWindowWidth,
                floatWindowHeight = floatWindowHeight,
                redirectionUrl = redirectionUrl,
                privacyUrl = privacyUrl,
                termsOfServiceUrl = termsOfServiceUrl
            )
        }
    }
}

@Keep
data class DAppInfo(
    val id: String?,
    val title: String?,
    val url: String?,
    val description: String?,
    val shortDescription: String?,
    val iconUrl: String?,
    val bannerUrl: String?,
)
@Keep
data class MiniAppInfo(
    val id: String?,
    val identifier: String?,
    val title: String?,
    val description: String?,
    val shortDescription: String?,
    val iconUrl: String?,
    val bannerUrl: String?,
    val botId: String?,
    val botName: String?,
    val createAt: Long?,
    val updateAt: Long?
)

@Keep
interface MiniAppService {
    fun setup(config: AppConfig)

    fun setThemeStyle(isDark: Boolean)

    fun setLanguage(languageCode: String)

    suspend fun preload(config: WebAppLaunchParameters)

    suspend fun launch(config: WebAppLaunchParameters) : IMiniApp?

    suspend fun batchGetMiniApps(appIds: List<String>): DataResult<List<MiniAppInfo>?>

    suspend fun getMiniAppInfo(appId: String): DataResult<MiniAppInfo?>

    suspend fun getDAppInfo(dAppId: String): DataResult<DAppInfo?>

    fun clearCache()
}

@Keep
data class ShareDto(
    val type: String,
    val id: String?,
    val identifier: String?,
    val title: String?,
    val url: String?,
    val description: String?,
    val iconUrl: String?,
    val bannerUrl: String?,
    val params: String?
)

@Keep
interface IMiniApp {
    fun reloadPage()
    fun requestDismiss(force: Boolean = false, immediately: Boolean = false, isSilent: Boolean = false, complete: (() -> Unit)? = null): Boolean
    suspend fun getShareUrl(): String?
    suspend fun getShareInfo(): ShareDto?
    fun isSystem(): Boolean
    fun minimization()
    fun maximize()
    fun clickMenu(type: String)
    fun getAppId(): String?
    fun capture(): Bitmap?
}

@Keep
interface IResourcesProvider {
    fun getColor(key: String): Int
    fun getString(key: String): String
    fun getString(key: String, vararg withValue: String): String
    fun getThemes(): Map<String,String>
    fun makeThemeParams(): JSONObject?
    fun isDark(): Boolean
}

@Keep
interface IAppDelegate {

    /**
     * Scheme consumption
     * @param app the invoked app
     * @param url String
     * @return Boolean True: already consumed, False: not consumed
     */
    suspend
    fun launchScheme(app: IMiniApp, url: String): Boolean

    /**
     * Connect app action, use case: wallet transfer select contact, APP enters contact selection page, selects and opens contact page, requests payment jump connection through payload parameter (usually also a WebApp connection)
     * @param app the invoked app
     * @param action String ["users"]
     * @param payload String
     * @return Boolean
     */
    suspend
    fun attachWithAction(app: IMiniApp, action: String, payload: String): Boolean

    /**
     * Open a new session inside the APP
     * @param app the invoked app
     * @param query String
     * @param types List<String>
     */
    fun switchInlineQuery(app: IMiniApp, query: String, types: List<String>)

    /**
     * Share link
     * @param app the invoked app
     * @param linkOrText String
     */
    fun shareLinkOrText(app: IMiniApp, linkOrText: String)

    /**
     * Execute custom method
     * @param app the invoked app
     * @param method String
     * @param params String?
     * @return Boolean True: application layer has consumed and processed, False: application layer has not consumed and processed
     */
    suspend
    fun callCustomMethod(app: IMiniApp, method: String, params: String?, callback: (String?) -> Unit): Boolean

    /**
     * Scan QR code and return QR code content
     * @param app the invoked app
     * @param subTitle String?
     * @return String
     */
    suspend
    fun scanQrCodeForResult(app: IMiniApp, subTitle: String?): String

    /**
     * Check if current session supports and authorizes message sending function
     * @param app the invoked app
     * @return Boolean
     */
    suspend
    fun checkPeerMessageAccess(app: IMiniApp): Boolean

    /**
     * Request current session to authorize message sending function
     * @param app the invoked app
     * @return Boolean
     */
    suspend
    fun requestPeerMessageAccess(app: IMiniApp): Boolean

    /**
     * Send message to current session
     * @param app the invoked app
     * @param content String?
     * @return Boolean
     */
    suspend
    fun sendMessageToPeer(app: IMiniApp, content: String?): Boolean

    /**
     * Request to send phone number to current session
     * @param app the invoked app
     * @return Boolean
     */
    suspend
    fun requestPhoneNumberToPeer(app: IMiniApp) : Boolean

    /**
     * Whether biometry authentication function is supported
     * @param app the invoked app
     * @return Boolean
     */
    suspend
    fun canUseBiometryAuth(app: IMiniApp): Boolean

    /**
     * Request to update biometry authentication
     * @param app the invoked app
     * @param token String?, old token
     * @param reason String?
     * @return String Return signed Token
     */
    suspend
    fun updateBiometryToken(app: IMiniApp, token: String?, reason: String?): String?

    /**
     * Open biometry settings
     * @param app the invoked app
     */
    fun openBiometrySettings(app: IMiniApp)

    /**
     * Minimize button click
     * @param app the invoked app
     */
    fun onMinimization(app: IMiniApp)

    /**
     * Maximize button click
     * @param app the invoked app
     */
    fun onMaximize(app: IMiniApp)

    /**
     * More button click
     * @param app the invoked app
     * @param menuTypes Menu types that need to be displayed
     * Types [SETTINGS、SHARE、FEEDBACK]
     * @return True: application layer has processed the event, engine no longer pops up settings menu, False: application layer has not processed the event, engine pops up dialog
     */
    fun onMoreButtonClick(app: IMiniApp, menuTypes: List<String>): Boolean

    /**
     * Settings menu button click
     * type: click type
     * @param app the invoked app
     */
    fun onMenuButtonClick(app: IMiniApp, type: String)

    /**
     * Api Err
     * @param code Int
     * @param message String?
     */
    fun onApiError(code: Int, message: String?)
}