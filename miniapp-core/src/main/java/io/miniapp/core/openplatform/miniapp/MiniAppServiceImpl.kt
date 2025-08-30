package io.miniapp.core.openplatform.miniapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.ArrayMap
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.miniapp.bridge.BridgeProvider
import io.miniapp.core.openplatform.common.apis.data.DAppDto
import io.miniapp.core.openplatform.common.apis.data.MiniAppDto
import io.miniapp.core.openplatform.common.data.LRUSharedPreferencesCache
import io.miniapp.core.openplatform.common.data.OpenServiceRepository
import io.miniapp.core.openplatform.common.network.error.MiniAppXError
import io.miniapp.core.openplatform.common.network.error.toFailure
import io.miniapp.core.openplatform.miniapp.ui.DefaultResourcesProvider
import io.miniapp.core.openplatform.miniapp.ui.DefaultWebViewFragment
import io.miniapp.core.openplatform.miniapp.ui.DefaultWebViewSheet
import io.miniapp.core.openplatform.miniapp.ui.FloatingWindowManager
import io.miniapp.core.openplatform.miniapp.ui.webview.WebAppLruCache
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.utils.HomeScreenShortcutUtils
import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import io.miniapp.core.openplatform.miniapp.utils.WebViewPermissionUtils
import io.miniapp.core.openplatform.miniapp.utils.toInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.lang.ref.WeakReference
import kotlin.Exception
import kotlin.coroutines.resume


internal class MiniAppServiceImpl : MiniAppService {

    companion object {

        @SuppressLint("StaticFieldLeak")
        private val _instance: MiniAppServiceImpl = MiniAppServiceImpl()
        fun getInstance() = _instance

        private val MINIAPP = "MINIAPP"
        private val WEBPAGE = "WEBPAGE"
    }

    var appConfig: AppConfig? = null
    var mContext: Context? = null
    var miniAppHost = listOf("https://miniappx.io")
    var appName = "Sample"
    var webAppName = "MiniAppX"
    var appDelegate: IAppDelegate? = null
    var resourcesProvider: IResourcesProvider? = null

    private val preloadApps: MutableList<WeakReference<IMiniApp>> = mutableListOf()

    private val repository by lazy {
        OpenServiceRepository.getInstance()
    }

    fun parasError(throwable: Throwable): MiniAppXError {
        if (throwable is MiniAppXError) {
            appDelegate?.onApiError(throwable.code, throwable.error)
            return throwable
        }

        return (throwable as? HttpException)?.let {
            val miniAppXError = it.response()?.toFailure() ?: MiniAppXError(it.code(), it.message())
            appDelegate?.onApiError(miniAppXError.code, miniAppXError.error)
            miniAppXError
        }  ?: MiniAppXError(404, null)
    }

    override fun setup(config: AppConfig) {
        this.appConfig = config

        this.mContext =   config.context.applicationContext ?: config.context

        this.miniAppHost = config.miniAppHost
        this.appDelegate  = config.appDelegate
        this.resourcesProvider = config.resourcesProvider
        this.appName = config.appName
        this.webAppName = config.webAppName

        config.apply {
            LogTimber.plant(LogTimber.DebugTree())
            AndroidUtils.fillStatusBarHeight(mContext!!, true)
            AndroidUtils.checkAndroidTheme(mContext!!, false)
            AndroidUtils.checkDisplaySize(mContext!!, null)
            DefaultResourcesProvider.setLocale(languageCode)
            DefaultResourcesProvider.setUserInterfaceStyle(isDark)
            ActivityStack.init(context = mContext!!)
            WebAppLruCache.resize(maxCachePage)
            HomeScreenShortcutUtils.launchScheme = redirectionUrl
        }
    }

    override suspend fun preload(config: WebAppLaunchParameters) {
        if (config !is WebAppPreloadParameters) {
            return
        }

        val webAppParameters = WebAppParameters.Builder()
            .owner(config.owner)
            .context(config.context)
            .miniAppId(config.miniAppId)
            .botId(config.botId)
            .botName(config.botName)
            .miniAppName(config.miniAppName)
            .type(TYPE_SIMPLE_WEB_VIEW_BUTTON)
            .url(config.url)
            .peer(config.peer)
            .startParam(config.startParam)
            .bridgetProvider(config.bridgetProvider ?: appConfig?.bridgeProviderFactory?.buildBridgeProvider
                (id = config.miniAppId, type = MINIAPP, url = config.url))
            .build() ?: return

        withContext(Dispatchers.Main) {
            attachDialog(config = webAppParameters,
                resourcesProvider = resourcesProvider,
                onDismissListener = null,
                isPreload = true).apply {
                preloadApps.add(WeakReference(this))
            }
        }
    }

    override suspend fun launch(config: WebAppLaunchParameters) : IMiniApp?  = suspendCancellableCoroutine { continuation ->

        // Create a list to store all requests
        val dismissRequests = preloadApps.firstOrNull { cacheApp->

            val miniAppId: String? = (config as? WebAppLaunchWithDialogParameters)?.miniAppId
                ?: (config as? WebAppLaunchWithDialogParameters)?.url?.let {
                    Uri.parse(it)
                }?.takeIf {
                    isWebAppShortLink(it)
                }?.let {
                    val segments = ArrayList<String>(it.pathSegments)
                    if (segments.size == 2) {
                        if(segments[0].lowercase() == "apps") {
                            // app scheme
                            segments[1]
                        } else {
                            val botName = segments[0]
                            val appName = segments[1]
                            LRUSharedPreferencesCache.getValue("${botName}_${appName}")
                        }
                    } else {
                        null
                    }
                }

            miniAppId?.let {
                cacheApp.get()?.getAppId() == it
            } == true

        }?.let { app ->
            CompletableDeferred<Boolean>().also { deferred ->
               val dismissing =  app.get()?.requestDismiss(force = true, immediately = true, isSilent = true) {
                    deferred.complete(true) // Called when completed
                } ?: false
                if (!dismissing) {
                    deferred.complete(true)
                }
            }
        }

        config.owner.lifecycleScope.launch {
            try {
                // Wait for all requestDismiss to complete
                dismissRequests?.await()

                when(config) {
                    is WebAppLaunchWithDialogParameters ->  launchWithDialog(config) {
                        continuation.resume(it)
                    }
                    is WebAppLaunchWithParentParameters ->  launchAndAttachTo(config) {
                        continuation.resume(it)
                    }
                    is DAppLaunchWParameters -> launchDApp(config) {
                        continuation.resume(it)
                    }
                    else -> continuation.resume(null)
                }
            } catch (e: Throwable) {
                continuation.resume(null) // Or you can pass error information
            }
        }
    }

    private suspend fun launchDApp(config: DAppLaunchWParameters, callback: (IMiniApp?) -> Unit) {

        val dApp: DAppDto? = config.id?.let {
            getDAppInfoById(it).let { result ->
                if (result.isSuccess()) {
                    result.data()
                } else {
                    val error = result.throwable()?.let {  throwable->
                        parasError(throwable)
                    }
                    if (error is MiniAppXError && error.code == 460) {
                        config.onErrorCallback?.invoke(error.code, error.message)
                        return
                    }
                    null
                }
            }
        }

        val dAppUrl = dApp?.url ?: config.url ?: return

        val parameters = WebAppParameters.Builder()
            .context(config.context)
            .owner(config.owner)
            .url(dAppUrl)
            .dAppDto(dApp)
            .autoExpand(true)
            .isDApp(true)
            .onErrorCallback(config.onErrorCallback)
            .actionBarBuilder(config.actionBarBuilder)
            .bridgetProvider(config.bridgetProvider ?: appConfig?.bridgeProviderFactory?.buildBridgeProvider(id = config.id, type = WEBPAGE, url = config.url))
            .build() ?: return

        withContext(Dispatchers.Main) {
            attachDialog(
                config = parameters,
                onDismissListener = config.onDismissListener
            ).also(callback)
        }
    }

    private suspend fun getMiniAppById(appId: String): DataResult<MiniAppDto?> =  suspendCancellableCoroutine { continuation ->
        MainScope().launch {
            repository.requestMiniApp(appId)
                .catch {
                    it.printStackTrace()
                    if (continuation.isActive) {
                        continuation.resume(DataResult.Failure(it))
                    }
                }.collect {
                    if (continuation.isActive) {
                        continuation.resume(DataResult.Success(it))
                    }
                }
        }
    }

    override suspend fun getMiniAppInfo(appId: String): DataResult<MiniAppInfo?> {
        val data = getMiniAppById(appId)
        return if (data.isSuccess()) {
            val successData = data as DataResult.Success
            DataResult.Success(data = successData.data?.toInfo())
        } else {
            val failureData = data as DataResult.Failure
            DataResult.Failure(failureData.throwable)
        }
    }

    private suspend fun getDAppInfoById(dAppId: String): DataResult<DAppDto?> =  suspendCancellableCoroutine { continuation ->
        MainScope().launch {
            repository.requestDApp(dAppId)
                .catch {
                    it.printStackTrace()
                    if (continuation.isActive) {
                        continuation.resume(DataResult.Failure(it))
                    }
                }.collect {
                    if (continuation.isActive) {
                        continuation.resume(DataResult.Success(it))
                    }
                }
        }
    }

    override suspend fun getDAppInfo(dAppId: String): DataResult<DAppInfo?> {
        val result = getDAppInfoById(dAppId)
        if (result.isSuccess()) {
            return DataResult.Success((result as DataResult.Success).data?.toInfo())
        }
        return DataResult.Failure((result as DataResult.Failure).throwable)
    }

    override fun clearCache() {
        WebAppLruCache.removeAll()
    }

    private suspend fun getMiniApp(botIdOrName: String, appName: String): MiniAppDto?  =  suspendCancellableCoroutine { continuation ->
        MainScope().launch {
            repository.requestMiniApp(appName, botIdOrName)
                .catch { throwable ->
                    throwable.printStackTrace()
                    parasError(throwable)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }.collect {
                    if (continuation.isActive) {
                        continuation.resume(it)
                    }
                }
        }
    }

    override suspend fun batchGetMiniApps(appIds: List<String>): DataResult<List<MiniAppInfo>?> =  suspendCancellableCoroutine { continuation ->
        MainScope().launch {
            repository.batchRequestMiniApp(appIds)
                .catch {
                    it.printStackTrace()
                    if (continuation.isActive) {
                        continuation.resume(DataResult.Failure(it))
                    }
                }.collect {
                    if (continuation.isActive) {
                        continuation.resume(DataResult.Success(it.items?.map { dto -> dto.toInfo() }))
                    }
                }
        }
    }

    private suspend fun launchWithDialog(config: WebAppLaunchWithDialogParameters, callback: (IMiniApp?) -> Unit) {
        val parameters = if (true==config.url?.isNotEmpty()) {
            buildWebParameters(
                context = config.context,
                owner = config.owner,
                url = config.url,
                startParams = config.startParam,
                params = config.params,
                isLaunchUrl = config.isLaunchUrl,
                useModalStyle = config.useModalStyle,
                useCustomNavigation = config.useCustomNavigation,
                isSystem = config.isSystem,
                useWeChatStyle = config.useWeChatStyle,
                type = config.type,
                useCache = config.useCache,
                peer = config.peer,
                autoExpand = config.autoExpand,
                onErrorCallback = config.onErrorCallback,
                bridgetProvider = config.bridgetProvider ?: appConfig?.bridgeProviderFactory?.buildBridgeProvider(
                    id = config.miniAppId, type = MINIAPP, url = config.url
                ),
                actionBarBuilder = config.actionBarBuilder)
        } else {
            WebAppParameters.Builder()
                .context(config.context)
                .owner(config.owner)
                .botId(config.botId)
                .botName(config.botName)
                .miniAppId(config.miniAppId)
                .miniAppName(config.miniAppName ?: "")
                .useWeChatStyle(config.useWeChatStyle)
                .type(config.type)
                .useModalStyle(config.useModalStyle)
                .useCustomNavigation(config.useCustomNavigation)
                .isLocal(config.isLocal)
                .isSystem(config.isSystem)
                .useCache(config.useCache)
                .autoExpand(config.autoExpand)
                .startParam(config.startParam)
                .params(config.params)
                .peer(config.peer)
                .onErrorCallback(config.onErrorCallback)
                .bridgetProvider(config.bridgetProvider ?: appConfig?.bridgeProviderFactory?.buildBridgeProvider(
                    id = config.miniAppId, type = MINIAPP, url = config.url
                ))
                .actionBarBuilder(config.actionBarBuilder)
                .build()
        }

        parameters ?: run {
            callback(null)
            return
        }

        withContext(Dispatchers.Main) {
            attachDialog(config = parameters,
                resourcesProvider = resourcesProvider,
                onDismissListener = config.onDismissListener).also(callback)
        }
    }

    private fun mergeMaps(map1: Map<String, String>?, map2: Map<String, String>?): Map<String, String> {
        return (map1 ?: emptyMap()) + (map2 ?: emptyMap())
    }

    private suspend fun buildWebParameters(context: Context,
                                           owner: LifecycleOwner,
                                           url: String,
                                           @WebViewType type: Int,
                                           startParams: String? = null,
                                           params: Map<String, String>?,
                                           isLaunchUrl: Boolean = false,
                                           useModalStyle: Boolean = false,
                                           useCustomNavigation: Boolean = false,
                                           isSystem: Boolean = false,
                                           useWeChatStyle: Boolean = true,
                                           autoExpand: Boolean = false,
                                           useCache: Boolean = true,
                                           peer: Peer?,
                                           onErrorCallback: ((Int,String?) -> Unit)?,
                                           bridgetProvider: BridgeProvider?,
                                           actionBarBuilder: ((()->Unit, ()->Unit) -> Pair<FrameLayout.LayoutParams, View>)?): WebAppParameters?  =  suspendCancellableCoroutine { continuation ->

        try {
            val uri = Uri.parse(url)

            if (isWebAppShortLink(uri)) {
                owner.lifecycleScope.launch {
                    if(schemeRouter(url =url, callback = { app, dApp, urlStartParams, urlParams ->
                            dApp?.let { dapp->
                                WebAppParameters.Builder()
                                    .context(context)
                                    .owner(owner)
                                    .url(dapp.url)
                                    .dAppDto(dapp)
                                    .isDApp(true)
                                    .type(TYPE_SIMPLE_WEB_VIEW_BUTTON)
                                    .actionBarBuilder(actionBarBuilder)
                                    .onErrorCallback(onErrorCallback)
                                    .build()?.let {
                                        continuation.resume(it)
                                        true
                                    } == true
                            } ?: (app?.let {
                                WebAppParameters.Builder()
                                    .context(context)
                                    .owner(owner)
                                    .useWeChatStyle(useWeChatStyle)
                                    .type(type)
                                    .useModalStyle(useModalStyle)
                                    .useCustomNavigation(useCustomNavigation)
                                    .isSystem(isSystem)
                                    .useCache(useCache)
                                    .autoExpand(autoExpand)
                                    .startParam(urlStartParams ?: startParams)
                                    .params(mergeMaps(urlParams,params))
                                    .peer(peer)
                                    .miniAppDto(app)
                                    .bridgetProvider(bridgetProvider)
                                    .actionBarBuilder(actionBarBuilder)
                                    .onErrorCallback(onErrorCallback)
                                    .build()?.let {
                                        continuation.resume(it)
                                        true
                                    } == true
                            } == true)
                        }
                        )) {
                        return@launch
                    } else {
                        continuation.resume(null)
                    }
                }
            } else if(isLaunchUrl) {
                WebAppParameters.Builder()
                    .context(context)
                    .owner(owner)
                    .botName("")
                    .miniAppName("")
                    .useWeChatStyle(useWeChatStyle)
                    .type(type)
                    .useModalStyle(useModalStyle)
                    .useCustomNavigation(useCustomNavigation)
                    .isSystem(isSystem)
                    .url(uri.toString())
                    .params(params)
                    .autoExpand(autoExpand)
                    .useCache(false)
                    .peer(peer)
                    .bridgetProvider(bridgetProvider)
                    .actionBarBuilder(actionBarBuilder)
                    .onErrorCallback(onErrorCallback)
                    .build().also {
                        continuation.resume(it)
                        return@suspendCancellableCoroutine
                    }
            } else {
                continuation.resume(null)
            }
        } catch (e: Throwable) {
            continuation.resume(null)
            e.printStackTrace()
        }
    }
    private suspend fun launchAndAttachTo(config: WebAppLaunchWithParentParameters, callback: (IMiniApp?) -> Unit) {
        val parameters = if (true==config.url?.isNotEmpty()) {
            buildWebParameters(
                context = config.context,
                owner = config.owner,
                url = config.url,
                startParams = config.startParam,
                params = config.params,
                isLaunchUrl = config.isLaunchUrl,
                useModalStyle = config.useModalStyle,
                useCustomNavigation = config.useCustomNavigation,
                useWeChatStyle = config.useWeChatStyle,
                type = config.type,
                useCache = config.useCache,
                peer = config.peer,
                onErrorCallback = config.onErrorCallback,
                bridgetProvider = config.bridgetProvider ?: appConfig?.bridgeProviderFactory?.buildBridgeProvider(
                    id = config.miniAppId, type = MINIAPP, url = config.url
                ),
                actionBarBuilder = config.actionBarBuilder)
        } else {
            WebAppParameters.Builder()
                .context(config.context)
                .owner(config.owner)
                .botId(config.botId)
                .botName(config.botName)
                .miniAppId(config.miniAppId)
                .miniAppName(config.miniAppName ?: "")
                .useWeChatStyle(config.useWeChatStyle)
                .type(config.type)
                .isLocal(config.isLocal)
                .useModalStyle(config.useModalStyle)
                .useCustomNavigation(config.useCustomNavigation)
                .isSystem(config.isSystem)
                .useCache(config.useCache)
                .autoExpand(config.autoExpand)
                .startParam(config.startParam)
                .params(config.params)
                .peer(config.peer)
                .onErrorCallback(config.onErrorCallback)
                .bridgetProvider(config.bridgetProvider ?: appConfig?.bridgeProviderFactory?.buildBridgeProvider(
                    id = config.miniAppId, type = MINIAPP, url = config.url
                ))
                .actionBarBuilder(config.actionBarBuilder)
                .build()
        }

        parameters ?: run {
            callback(null)
            return
        }

        withContext(Dispatchers.Main) {
            val sheet = DefaultWebViewFragment(
                context = config.context,
                parentView = config.parentView,
                launchConfig = parameters,
                resourcesProvider = resourcesProvider ?: DefaultResourcesProvider,
                owner = config.owner
            ) {
                config.onDismissListener?.invoke()
            }

            val container = FrameLayout(config.context)
            container.setOnClickListener {
                if(!sheet.requestDismiss()) {
                    return@setOnClickListener
                }

                config.parentView?.removeView(container)
            }
            config.parentView?.addView(container,
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
            )

            sheet.attachMiniApp()
            config.parentView?.addView(sheet, config.layoutParams)
            sheet.showAfterAttach()

            callback(sheet)
        }
    }

    override fun setThemeStyle(isDark: Boolean) {
        DefaultResourcesProvider.setUserInterfaceStyle(isDark)
    }

    override fun setLanguage(languageCode: String) {
        DefaultResourcesProvider.setLocale(languageCode)
    }

    private fun isWebAppShortLink(uri: Uri): Boolean {
        val lowercaseMiniAppHost = miniAppHost.map { it.lowercase() }
        val lowercaseUrlHost = uri.toString().lowercase()

        val isHostInMiniApp = lowercaseMiniAppHost.any { host ->
            lowercaseUrlHost.startsWith(host)
        }

        return isHostInMiniApp
    }

   private fun getQueryMap(uri: Uri?): Map<String, String>? {
        if (uri == null) return null
        val result = mutableMapOf<String, String>()
        for (name in uri.queryParameterNames) {
            result[name] = uri.getQueryParameter(name) ?: ""
        }
        return result
    }

    private suspend fun schemeRouter(url: String,
                                      callback : (MiniAppDto?, DAppDto?, String?, Map<String,String>?) -> Boolean): Boolean  {
        try {
            val uri = Uri.parse(url) ?: return false
            val segments = ArrayList<String>(uri.pathSegments)

            if (segments.size==1) {
                callback.invoke(null,null, null, null)
                return true
            }

            if (segments.size == 2) {
                if(segments[0].lowercase() == "apps") {
                    // app scheme
                    return getMiniAppById(segments[1]).takeIf {
                        if(it.isSuccess()) {
                            true
                        } else {
                            parasError((it as DataResult.Failure).throwable)
                            false
                        }
                    }?.let {
                        val startParams = uri.getQueryParameter("startapp")
                        val params = getQueryMap(uri)?.filter { pair-> pair.key != "startapp" }
                        callback((it as DataResult.Success).data, null, startParams, params)
                    } ?: false
                } else if(segments[0].lowercase() == "dapp") {
                    // dapp scheme
                    return getDAppInfoById(segments[1]).takeIf {
                        if(it.isSuccess()) {
                            true
                        } else {
                           parasError((it as DataResult.Failure).throwable)
                            false
                        }
                    }?.let {
                        val startParams = uri.getQueryParameter("startapp")
                        callback(null, (it as DataResult.Success).data, startParams, null)
                    } ?: false
                }

                val startParams = uri.getQueryParameter("startapp")
                val params = getQueryMap(uri)?.filter { pair-> pair.key != "startapp" }

                val botName = segments[0]
                val miniAppName = segments[1]

                return getMiniApp(botName, miniAppName)?.let {
                    callback(it, null, startParams, params)
                } ?: false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        return false
    }

    fun openUrl(launchConfig: WebAppParameters, url: String, isInternal: Boolean) {
        if (isInternal) {
            launchConfig.owner.lifecycleScope.launch {
                if(schemeRouter(url =url, callback = { app, dapp, startParams, urlParams ->
                        dapp?.let {
                            WebAppParameters.Builder()
                                .context(launchConfig.context)
                                .owner(launchConfig.owner)
                                .url(it.url)
                                .dAppDto(it)
                                .isDApp(true)
                                .onErrorCallback(launchConfig.onErrorCallback)
                                .bridgetProvider(appConfig?.bridgeProviderFactory?.buildBridgeProvider(
                                    id = dapp.id, type = WEBPAGE, url = it.url
                                ))
                                .type(TYPE_SIMPLE_WEB_VIEW_BUTTON)
                                .actionBarBuilder(launchConfig.actionBarBuilder)
                                .build()?.let {
                                    attachDialog(config = it,
                                        resourcesProvider = resourcesProvider)
                                    true
                                } == true
                        } ?: (app?.let {
                                WebAppParameters.Builder()
                                    .context(launchConfig.context)
                                    .owner(launchConfig.owner)
                                    .onErrorCallback(launchConfig.onErrorCallback)
                                    .miniAppDto(app)
                                    .startParam(startParams)
                                    .params(urlParams)
                                    .bridgetProvider(appConfig?.bridgeProviderFactory?.buildBridgeProvider(
                                        id = it.id, type = MINIAPP, url = null
                                    ))
                                    .type(TYPE_SIMPLE_WEB_VIEW_BUTTON)
                                    .peer(launchConfig.peer)
                                    .actionBarBuilder(launchConfig.actionBarBuilder)
                                    .build()?.let {
                                        attachDialog(config = it,
                                            resourcesProvider = resourcesProvider)
                                        true
                                    } == true
                            } == true)
                    }
                )) {
                    return@launch
                } else {
                    openWithDApp(launchConfig, url)
               }
            }
        } else {
            openWithDApp(launchConfig, url)
        }
    }

    private fun openWithDApp(config: WebAppParameters, url: String) {
        try {
            if(isWebAppShortLink(Uri.parse(url))) {
                return
            }

            val parameters = WebAppParameters.Builder()
                .context(config.context)
                .owner(config.owner)
                .url(url)
                .useCache(false)
                .autoExpand(true)
                .isDApp(true)
                .onErrorCallback(config.onErrorCallback)
                .actionBarBuilder(config.actionBarBuilder)
                .bridgetProvider(appConfig?.bridgeProviderFactory?.buildBridgeProvider(
                    id = null, type = WEBPAGE, url = url
                ))
                .build() ?: return

            attachDialog(
                config = parameters
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun attachDialog(
        config: WebAppParameters,
        resourcesProvider: IResourcesProvider? = null,
        onDismissListener: (() -> Unit)? = null,
        isPreload: Boolean = false
    ): IMiniApp {

        val cacheKey = config.cacheKey()

        val cacheMiniApp = cacheKey?.let {
                WebAppLruCache.get(it)
            }?.let {
                if (it.isExpired) {
                    WebAppLruCache.remove(cacheKey)
                    it.miniApp?.requestDismiss(force = true, immediately = true, isSilent = true)
                    null
                }
                else if (it.miniApp != null && it.isTopLevel() && FloatingWindowManager.isAppOnMinimization(it.webAppId) ) {
                    it
                } else {
                    WebAppLruCache.remove(cacheKey)
                    it.miniApp?.requestDismiss(force = true, immediately = true, isSilent = true)
                    null
                }
            }?.let{
                if (config.owner != (it.miniApp as? DefaultWebViewFragment)?.owner) {
                    WebAppLruCache.remove(cacheKey)
                    it.miniApp?.requestDismiss(force = true, immediately = true, isSilent = true)
                    null
                } else {
                    FloatingWindowManager.maximize()
                    it
                }
             }?.miniApp

        if (cacheMiniApp !=null ) {
            return cacheMiniApp
        }

        FloatingWindowManager.closeFloatingWindow(force = true, immediately=true)

        val sheet = DefaultWebViewSheet(
            context = config.context,
            owner = config.owner,
            launchConfig = config,
            isPreload = isPreload,
            resourcesProvider = resourcesProvider ?: DefaultResourcesProvider
        ) {
            onDismissListener?.invoke()
        }
        sheet.attachMiniApp()
        if (!isPreload) {
            sheet.showAfterAttach()
        }

        return sheet
    }
}

internal object ActivityStack {

    private var currentActivity: WeakReference<ComponentActivity>? = null
    private var permissionResultLauncher: ArrayMap<String, ActivityResultLauncher<Array<String>>> = ArrayMap()
    private var fileChooserResultLauncher: ArrayMap<String, ActivityResultLauncher<Intent>> = ArrayMap()

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>,
                        fileChooserParams: WebChromeClient.FileChooserParams): Boolean {

        ActivityStack.filePathCallback?.onReceiveValue(null)
        ActivityStack.filePathCallback = filePathCallback
        getFileChooseResultLauncher()?.also {
            it.launch(fileChooserParams.createIntent())
        }
        return true
    }

    internal class MyActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if ( activity is ComponentActivity) {

                val resultCall = activity.registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    WebViewPermissionUtils.onPermissionResult(result)
                }
                permissionResultLauncher[activity.toKeyString()] = resultCall

                val fileChooseResultCall = activity.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        result.data?.dataString?.also {
                            val results = arrayOf<Uri>(Uri.parse(it))
                            filePathCallback?.onReceiveValue(results)
                            filePathCallback = null
                        }
                    } else {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = null
                    }
                }
                fileChooserResultLauncher[activity.toKeyString()] = fileChooseResultCall
            }
        }

        override fun onActivityStarted(activity: Activity) {
            // Callback when Activity starts
        }

        override fun onActivityResumed(activity: Activity) {
            // Callback when Activity resumes
            if (activity is ComponentActivity) {
                currentActivity = WeakReference(activity)
            }
        }

        override fun onActivityPaused(activity: Activity) {
            // Callback when Activity pauses
        }

        override fun onActivityStopped(activity: Activity) {
            // Callback when Activity stops
        }

        override fun onActivityDestroyed(activity: Activity) {
            // Callback when Activity is destroyed
            permissionResultLauncher.remove(activity.toKeyString())
            fileChooserResultLauncher.remove(activity.toKeyString())
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            // Callback when Activity saves state
        }
    }

    fun getCurrentResultLauncher() = permissionResultLauncher[currentActivity?.get()?.toKeyString()]

    fun getFileChooseResultLauncher() = fileChooserResultLauncher[currentActivity?.get()?.toKeyString()]

    fun getCurrentActivity() = currentActivity?.get()

    private fun Activity.toKeyString(): String {
        return "${this.packageName}#${this.localClassName}"
    }
    fun init(context: Context) {
        if (context is Application) {
            context.registerActivityLifecycleCallbacks(MyActivityLifecycleCallbacks())
        }
    }
}

internal data class WebAppParameters(
    val owner: LifecycleOwner,
    val context: Context,
    var miniAppId: String?,
    var botId: String?,
    var botName: String?,
    var miniAppName: String?,
    val url: String? = null,
    val startParam: String? = null,
    val params: Map<String,String>? = null,
    val peer: Peer? = null,
    @WebViewType
    val type: Int = TYPE_SIMPLE_WEB_VIEW_BUTTON,
    val useWeChatStyle: Boolean = true,
    var autoExpand: Boolean = false,
    val useModalStyle: Boolean = false,
    val useCustomNavigation: Boolean = false,
    val isLocal: Boolean = false,
    val isSystem: Boolean = false,
    val useCache: Boolean = true,
    val isDApp: Boolean = false,
    val miniAppDto: MiniAppDto? = null,
    val dAppDto: DAppDto? = null,
    val bridgeProvider: BridgeProvider?= null,
    val onErrorCallback: ((Int,String?) -> Unit)? = null,
    val actionBarBuilder: ((()->Unit, ()->Unit) -> Pair<FrameLayout.LayoutParams, View>)? = null
    ) {

    fun cacheKey() : String? {
        if (isDApp) {
            return url
        }
        if (!miniAppId.isNullOrBlank()) {
            return miniAppId
        }
        if (miniAppName.isNullOrBlank()) {
            return null
        }
        if (botId.isNullOrBlank() && botName.isNullOrBlank()) {
            return null
        }
        return "__${botId}_${botName}_${miniAppName}"
    }

    fun cacheData(): String {
        return "${cacheKey()}_${startParam}_${params?.map { "${it.key}_${it.value}" }?.joinToString("_")}${DefaultResourcesProvider.getLanguageCode()}_${DefaultResourcesProvider.isDark()}"
    }

    class Builder {

        private var owner: LifecycleOwner? = null
        private var context: Context? = null
        private var miniAppDto: MiniAppDto? = null
        private var dAppDto: DAppDto? = null
        private var miniAppId: String? = null
        private var botId: String? = null
        private var botName: String? = null
        private var miniAppName: String? = null

        @WebViewType
        private var type: Int = TYPE_SIMPLE_WEB_VIEW_BUTTON
        private var useWeChatStyle: Boolean = true
        private var url: String? = null
        private var startParam: String? = null
        private var params: Map<String,String>? = null
        private var useModalStyle: Boolean = false
        private var useCustomNavigation: Boolean = false
        private var isLocal: Boolean = false
        private var isSystem: Boolean = false
        private var useCache: Boolean = true
        private var autoExpand: Boolean = false
        private var isDApp: Boolean = false
        private var peer: Peer? = null

        private var bridgetProvider: BridgeProvider? = null
        private var onErrorCallback: ((Int,String?) -> Unit)? = null
        private var actionBarBuilder: ((()->Unit, ()->Unit) -> Pair<FrameLayout.LayoutParams, View>)? = null

        fun owner(owner: LifecycleOwner) = apply {
            this.owner = owner
        }

        fun context(context: Context) = apply {
            this.context = context
        }

        fun miniAppDto(miniAppDto: MiniAppDto?) = apply {
            this.miniAppDto = miniAppDto
            miniAppDto?.also {
                this.botName = it.botName
                this.miniAppId = it.id
                this.miniAppName = it.identifier
                this.botId = it.botId
            }
        }

        fun dAppDto(dAppDto: DAppDto?) = apply {
            this.dAppDto = dAppDto
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

        fun type(@WebViewType type: Int) = apply {
            this.type = type
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

        fun isLocal(isLocal: Boolean) = apply {
            this.isLocal = isLocal
        }

        fun isSystem(isSystem: Boolean) = apply {
            this.isSystem = isSystem
        }

        fun useCache(useCache: Boolean) = apply {
            this.useCache = useCache
        }

        fun autoExpand(autoExpand: Boolean) = apply {
            this.autoExpand = autoExpand
        }

        fun isDApp(isDApp: Boolean) = apply {
            this.isDApp = isDApp
        }

        fun peer(peer: Peer?) = apply {
            this.peer = peer
        }

        fun actionBarBuilder(actionBarBuilder: ((()->Unit, ()->Unit) -> Pair<FrameLayout.LayoutParams, View>)?) = apply {
            this.actionBarBuilder = actionBarBuilder
        }

        fun bridgetProvider(bridgetProvider: BridgeProvider?) = apply {
            this.bridgetProvider = bridgetProvider
        }

        fun onErrorCallback(onErrorCallback: ((Int,String?) -> Unit)?) = apply {
            this.onErrorCallback = onErrorCallback
        }

        fun build(): WebAppParameters? {

            if(this.owner  == null || this.context == null) return null

            if (true == this.miniAppId?.isEmpty() &&
                (true == this.url?.isEmpty() &&
                        ((true == this.botName?.isEmpty() && true == this.botId?.isEmpty())
                                || true == this.miniAppName?.isEmpty()))) {
                return null
            }

            return WebAppParameters(
                owner = this.owner!!,
                context = this.context!!,
                miniAppId = this.miniAppId,
                botId = this.botId,
                botName = this.botName,
                miniAppName = this.miniAppName,
                type = this.type,
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
                isDApp = this.isDApp,
                peer = this.peer,
                miniAppDto = this.miniAppDto,
                dAppDto = dAppDto,
                actionBarBuilder = this.actionBarBuilder,
                bridgeProvider = this.bridgetProvider,
                onErrorCallback = this.onErrorCallback
            )
        }
    }
}