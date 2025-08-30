package io.miniapp.core.openplatform.miniapp.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Browser
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.miniapp.bridge.BridgeProvider
import io.miniapp.core.R
import io.miniapp.core.openplatform.common.apis.data.AppSettings
import io.miniapp.core.openplatform.common.apis.data.CustomMethodsParams
import io.miniapp.core.openplatform.common.apis.data.DAppDto
import io.miniapp.core.openplatform.common.apis.data.LaunchParams
import io.miniapp.core.openplatform.common.apis.data.MiniAppDto
import io.miniapp.core.openplatform.common.data.OpenServiceRepository
import io.miniapp.core.openplatform.miniapp.ActivityStack
import io.miniapp.core.openplatform.miniapp.IMiniApp
import io.miniapp.core.openplatform.miniapp.IResourcesProvider
import io.miniapp.core.openplatform.miniapp.MiniAppServiceImpl
import io.miniapp.core.openplatform.miniapp.ShareDto
import io.miniapp.core.openplatform.miniapp.WebAppParameters
import io.miniapp.core.openplatform.miniapp.ui.proxy.WebAppEventProxy
import io.miniapp.core.openplatform.miniapp.ui.proxy.WebAppProxy
import io.miniapp.core.openplatform.miniapp.ui.views.ActionBar
import io.miniapp.core.openplatform.miniapp.ui.views.BackDrawable
import io.miniapp.core.openplatform.miniapp.ui.views.CubicBezierInterpolator
import io.miniapp.core.openplatform.miniapp.ui.views.PageLoadingView
import io.miniapp.core.openplatform.miniapp.ui.views.RadialProgressView
import io.miniapp.core.openplatform.miniapp.ui.views.SizeNotifierFrameLayout
import io.miniapp.core.openplatform.miniapp.ui.views.ToolBarComponent
import io.miniapp.core.openplatform.miniapp.ui.views.VerticalPositionAutoAnimator
import io.miniapp.core.openplatform.miniapp.ui.views.WebProgressView
import io.miniapp.core.openplatform.miniapp.ui.views.WebViewSwipeContainer
import io.miniapp.core.openplatform.miniapp.ui.webview.WebAppLruCache
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.utils.BiometrySP
import io.miniapp.core.openplatform.miniapp.utils.HomeScreenShortcutUtils
import io.miniapp.core.openplatform.miniapp.utils.JsonUtils
import io.miniapp.core.openplatform.miniapp.utils.LayoutHelper
import io.miniapp.core.openplatform.miniapp.utils.PERMISSIONS_FOR_ROOM_AVATAR
import io.miniapp.core.openplatform.miniapp.utils.PermissionsTools
import io.miniapp.core.openplatform.miniapp.utils.SchemeUtils
import io.miniapp.core.openplatform.miniapp.utils.ThemeUtils
import io.miniapp.core.openplatform.miniapp.utils.UIContextUtil
import io.miniapp.core.openplatform.miniapp.utils.Utilities
import io.miniapp.core.openplatform.miniapp.utils.WebViewPermissionUtils
import io.miniapp.core.openplatform.miniapp.utils.toParams
import io.miniapp.core.openplatform.miniapp.webapp.IMiniAppDelegate
import io.miniapp.core.openplatform.miniapp.webapp.IMiniAppListener
import io.miniapp.core.openplatform.miniapp.webapp.IWebApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.min


internal class DefaultWebViewFragment(
    context: Context,
    val owner: LifecycleOwner,
    private var launchConfig: WebAppParameters? = null,
    private val parentView: ViewGroup? = null,
    private val defaultDelegate: IMiniAppListener? = null,
    private val isPreload: Boolean = false,
    private val resourcesProvider: IResourcesProvider,
    private val onDismissListener: () -> Unit,
) : IMiniApp, IMiniAppDelegate, SizeNotifierFrameLayout(context), DefaultLifecycleObserver {

    private var miniAppDto: MiniAppDto? = launchConfig?.miniAppDto
    private var appSettings: AppSettings? = launchConfig?.miniAppDto?.options
    private var appId: String? = launchConfig?.miniAppId ?: launchConfig?.miniAppDto?.id

    private var springAnimation: SpringAnimation? = null
    private val swipeContainer: WebViewSwipeContainer
    private val webViewContainer: AbsWebViewContainer
    private var webAppProxy: WebAppProxy? = null
    private var pageLoadingView: PageLoadingView
    private var progressView: WebProgressView
    private var actionBar: ActionBar
    private var mainButton: TextView
    private var mainButtonHeight: Int = 0
    private var mainButtonAutoAnimator: VerticalPositionAutoAnimator? = null
    private var radialProgressAutoAnimator: VerticalPositionAutoAnimator? = null
    private var radialProgressView: RadialProgressView
    private var toolBarComponent: View? = null

    private var mainButtonWasVisible = false
    private var mainButtonProgressWasVisible = false
    private var useWeChatStyle = true
    private var useCacheWebView = false

    private var isViewPortByMeasureSuppressed = false

    private var overrideBackgroundColor = false
    private var lineColor: Int = resourcesProvider.getColor("sheet_scroll_up")
    private var actionBarColor = resourcesProvider.getColor("bg_color")
    var windowBackgroundColor = resourcesProvider.getColor("bg_color")
        private set

    private var showStatusBar: Boolean = true
    private var showActionBar: Boolean = true
    private var actionBarShadow: Drawable? = null

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val actionBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dimPaint = Paint()

    private val insets = Rect()
    private val navInsets = Rect()
    private val lastInsets = Rect(0, 0, 0, 0)
    private var lastInsetsTopMargin = 0
    private var keyboardInset = 0

    private var needCloseConfirmation = false
    private var isExpanded = false
    private var canExpand:Boolean? = null
    private var lastExpanded = false
    private var showFullScreenMod : Boolean? = null

    private var lastClickMs: Long = 0
    private var lastSwipeTime: Long = 0
    private var ignoreLayout = false
    private var wasLightStatusBar: Boolean? = null
    private var isContainerLightStatusBar: Boolean? = null
    private var actionBarTransitionProgress = 0f
    private var dismissed = false
    private var isBackButtonVisible = false
    private var lineBarHeight = 24

    private var fullScreenView: View? = null

    private var loadPageFail = false

    private val allJobs = Job()

    private var webAppEventProxy: WebAppEventProxy? = null

    private val biometrySP: BiometrySP?  by lazy {
        launchConfig?.cacheKey()?.let {
            BiometrySP(context = context,
                cacheKey = it,
                currentAccount =  "default")
        }
    }

    private var settingMenuButtonVisible: Boolean = false

    private val miniAppRepository by lazy {
        OpenServiceRepository.getInstance()
    }

    private fun updateActionBarTitle() {
        val title = miniAppDto?.title ?: launchConfig?.miniAppName ?: launchConfig?.dAppDto?.title ?: webViewContainer.getWebView()?.pageTitle ?: ""
        actionBar.setTitle( title, null)
        if (true == launchConfig?.isDApp) {
            actionBar.setSubtitle(webViewContainer.getWebView()?.url)
        }
    }

    private fun runTestWebApp() {
        owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {
            loadUrl("file:///android_asset/${launchConfig?.miniAppName}.html#tgWebAppData=user%3D%257B%2522id%2522%253A6132055853%252C%2522first_name%2522%253A%2522billb%2522%252C%2522last_name%2522%253A%2522%2522%252C%2522username%2522%253A%2522billb008%2522%252C%2522language_code%2522%253A%2522zh-hans%2522%252C%2522allows_write_to_pm%2522%253Atrue%252C%2522photo_url%2522%253A%2522https%253A%255C%252F%255C%252Ft.me%255C%252Fi%255C%252Fuserpic%255C%252F320%255C%252F_ooPOu3U0aIepjoddkmjGdRrmxif7NVgrDl8Hu3MxK0FRU8w8HNdRPfzvuQDqU8k.svg%2522%257D%26chat_instance%3D6874471725191745609%26chat_type%3Dsender%26auth_date%3D1731772698%26hash%3D4b28282a3ca6bb84534560442d4550b9dd68954099447267aacc60c10a4920da&tgWebAppVersion=8.0&tgWebAppPlatform=android&tgWebAppThemeParams=%7B%22bg_color%22%3A%22%23ffffff%22%2C%22section_bg_color%22%3A%22%23ffffff%22%2C%22secondary_bg_color%22%3A%22%23f0f0f0%22%2C%22text_color%22%3A%22%23222222%22%2C%22hint_color%22%3A%22%23a8a8a8%22%2C%22link_color%22%3A%22%232678b6%22%2C%22button_color%22%3A%22%2350a8eb%22%2C%22button_text_color%22%3A%22%23ffffff%22%2C%22header_bg_color%22%3A%22%23527da3%22%2C%22accent_text_color%22%3A%22%231c93e3%22%2C%22section_header_text_color%22%3A%22%233a95d5%22%2C%22subtitle_text_color%22%3A%22%2382868a%22%2C%22destructive_text_color%22%3A%22%23cc2929%22%2C%22section_separator_color%22%3A%22%23d9d9d9%22%2C%22bottom_bar_bg_color%22%3A%22%23f0f0f0%22%7D")
            actionBar.setTitle(launchConfig?.miniAppName ?: "", null)
        }
    }

    private fun requestMiniAppInfo(callback: (MiniAppDto?) -> Unit) {

        if (miniAppDto!=null) {
            callback(miniAppDto)
            return
        }

        owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {

            if (true == launchConfig?.miniAppId?.isNotEmpty()) {
                launchConfig?.miniAppId?.also {
                    miniAppRepository.requestMiniApp(
                        appId = it
                    ).catch { throwable->
                        throwable.printStackTrace()
                        val miniAppXError = MiniAppServiceImpl.getInstance().parasError(throwable)
                        if (miniAppXError.code == 460) {
                            hideAndDestroy(immediately = true, isSilent = true)
                        }
                        launchConfig?.onErrorCallback?.invoke(miniAppXError.code, miniAppXError.error)
                        callback(null)
                    }.collect { app->
                        appId = app.id
                        miniAppDto = app
                        launchConfig?.miniAppName = app.identifier
                        launchConfig?.miniAppId = app.id
                        launchConfig?.botName = app.botName
                        launchConfig?.botId = app.botId
                        withContext(Dispatchers.Main) {
                            updateActionBarTitle()
                            callback(app)
                        }
                    }
                }
                return@launch
            }


            val idOrName = launchConfig?.botId ?: launchConfig?.botName ?: return@launch
            val miniAppName = launchConfig?.miniAppName ?: return@launch
            if (idOrName.isEmpty() || miniAppName.isEmpty()) {
                return@launch
            }

            miniAppRepository.requestMiniApp(
                appName = miniAppName,
                botIdOrName = idOrName
            ).catch { throwable->
                throwable.printStackTrace()
                MiniAppServiceImpl.getInstance().parasError(throwable)
                callback(null)
            }.collect {
                miniAppDto = it
                launchConfig?.miniAppName = it.identifier
                launchConfig?.miniAppId = it.id
                launchConfig?.botName = it.botName
                launchConfig?.botId = it.botId
                withContext(Dispatchers.Main) {
                    callback(it)
                }
            }
        }
    }

    private fun request110Url(url: String, id: String?) {
        owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {
            miniAppRepository.requestDAppLaunchUrl(url, id).catch { throwable->
                loadPageFail = true
                val miniAppXError = MiniAppServiceImpl.getInstance().parasError(throwable)
                if (miniAppXError.code == 460) {
                    hideAndDestroy(true, true)
                }
                launchConfig?.onErrorCallback?.invoke(miniAppXError.code, miniAppXError.error)
                throwable.printStackTrace()
            }.collect {
                withContext(Dispatchers.Main) {
                    loadPageFail = false
                    loadUrl(it.redirectUrl)
                    autoExpandPage()
                    //runTestWebApp()
                }
            }
        }
    }

    private fun requestLaunchUrl() {

        if(null != launchConfig?.url) {
            loadPageFail = false
            if (true == launchConfig?.isDApp) {
                request110Url(launchConfig?.url!!, launchConfig?.dAppDto?.id)
            } else {
                loadUrl(launchConfig!!.url!!)
            }
            return
        }

        if (true==launchConfig?.isLocal) {
            loadPageFail = false
            runTestWebApp()
            return
        }

        requestMiniAppInfo {

            it?.id ?: return@requestMiniAppInfo

            webViewContainer.getWebView()?.appSettings = it.options
            appSettings = it.options
            miniAppDto = it
            pageLoadingView.updateIconUrl(it.iconUrl )

            owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {
                val launchParams = LaunchParams(
                    appId = it.id,
                    startParams = launchConfig?.startParam,
                    peer = launchConfig?.peer?.toParams(),
                    themeParams = resourcesProvider.getThemes(),
                    languageCode = DefaultResourcesProvider.getLanguageCode()
                )

                miniAppRepository.requestLaunchInfo(
                    params = launchParams
                ).catch { throwable->
                    loadPageFail = true
                    val miniAppXError = MiniAppServiceImpl.getInstance().parasError(throwable)
                    if (miniAppXError.code == 460) {
                        hideAndDestroy(immediately = true, isSilent = true)
                    }
                    launchConfig?.onErrorCallback?.invoke(miniAppXError.code, miniAppXError.error)
                }.collect {
                    withContext(Dispatchers.Main) {
                        loadPageFail = false

                        val encodeParams = if (!launchConfig?.params.isNullOrEmpty()) {
                            val queryBuilder = Uri.Builder()
                            launchConfig?.params?.forEach { query->
                                queryBuilder.appendQueryParameter(query.key, query.value)
                            }
                            "&"+queryBuilder.build().query
                        } else {
                            ""
                        }

                        loadUrl(it.url+encodeParams)
                        autoExpandPage()
                        //runTestWebApp()
                    }
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
        webAppEventProxy?.release()
        webAppEventProxy = null

        FloatingWindowManager.closeFloatingWindow(force = true, immediately = true)

        if (defaultDelegate != null) {
            defaultDelegate.dismissImmediately()
        } else {
            dismissImmediately()
        }

        super.onDestroy(owner)
    }

    private fun isFullScreenMod() : Boolean {
        if (true == showFullScreenMod) {
            return true
        }
        return !useModalStyle() && useCustomNavigation()
    }

    init {

      setOnApplyWindowInsetsListener { v, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets, v)
            val navInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())
            this.navInsets.set(navInsets.left, navInsets.top, navInsets.right, navInsets.bottom)
            val cutoutInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            this.insets[cutoutInsets.left.coerceAtLeast(insets.stableInsetLeft), cutoutInsets.top.coerceAtLeast(
                insets.stableInsetTop
            ), cutoutInsets.right.coerceAtLeast(insets.stableInsetRight)] =
                cutoutInsets.bottom.coerceAtLeast(insets.stableInsetBottom)

            val keyboardInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.ime())
            val keyboardHeight: Int = keyboardInsets.bottom
            if (keyboardHeight > this.insets.bottom && keyboardHeight > AndroidUtils.dp(20)) {
                this.keyboardInset = keyboardHeight
            } else {
                this.keyboardInset = 0
            }
            if (Build.VERSION.SDK_INT >= 30) {
                return@setOnApplyWindowInsetsListener WindowInsets.CONSUMED
            } else {
                return@setOnApplyWindowInsetsListener insets.consumeSystemWindowInsets()
            }
        }

        useWeChatStyle = launchConfig?.useWeChatStyle ?: true

        owner.lifecycle.addObserver(this)

        setWillNotDraw(false)

        actionBar = buildActionBar()
        updateActionBarColors()

        radialProgressView = buildRadialProgressView()

        swipeContainer = object : WebViewSwipeContainer(context) {

            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                var tempWidthMeasureSpec = widthMeasureSpec
                val availableHeight = MeasureSpec.getSize(heightMeasureSpec)
                var padding = if (!AndroidUtils.isTablet(context) && AndroidUtils.displaySize.x > AndroidUtils.displaySize.y) {
                        (availableHeight / 3.5f).toInt()
                    } else {
                        availableHeight / 5 * 2
                    }

                if (padding < 0) {
                    padding = 0
                }

                if (getOffsetY().toInt() != padding && !dismissed) {
                    ignoreLayout = true
                    setOffsetY(padding.toFloat())
                    ignoreLayout = false
                }

                if (AndroidUtils.isTablet(context) && !AndroidUtils.isInMultiWindow && !AndroidUtils.isSmallTablet()) {
                    tempWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        (AndroidUtils.displaySize.x.coerceAtMost(AndroidUtils.displaySize.y) * 0.8f).toInt(),
                        MeasureSpec.EXACTLY
                    )
                }

                var tempHeightMeasureSpec = heightMeasureSpec

                if (!isFullScreenMod()) {
                    tempHeightMeasureSpec =  if(useCustomNavigation() || !showActionBar) {
                        MeasureSpec.getSize(heightMeasureSpec) -
                                (if (isStatusBarVisible()) AndroidUtils.statusBarHeight else 0) +
                                AndroidUtils.dp(lineBarHeight) -
                                (if (mainButtonWasVisible) mainButtonHeight else 0)
                    } else{
                        MeasureSpec.getSize(heightMeasureSpec) -
                                ActionBar.getCurrentActionBarHeight(context)  -
                                (if (isStatusBarVisible()) AndroidUtils.statusBarHeight else 0) +
                                AndroidUtils.dp(lineBarHeight) -
                                (if (mainButtonWasVisible) mainButtonHeight else 0)
                    }
                }

                super.onMeasure(
                    tempWidthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(
                        tempHeightMeasureSpec,
                        MeasureSpec.EXACTLY
                    )
                )
            }

            override fun requestLayout() {
                if (ignoreLayout) {
                    return
                }
                super.requestLayout()
            }
        }
        webViewContainer = object : AbsWebViewContainer(context) {

            override fun onWebViewCreated(isUseCache: Boolean) {

                this@DefaultWebViewFragment.useCacheWebView = isUseCache

                setDelegate(DefaultWebAppEventHandler(context, resourcesProvider, this@DefaultWebViewFragment))

                getWebApp()?.also {
                    webAppProxy = WebAppProxy(it, resourcesProvider)
                }

                swipeContainer.setWebView(getWebView())

                getWebView()?.also { webView->
                    windowBackgroundColor = webView.bgColor ?: windowBackgroundColor
                    webView.setBackgroundColor(windowBackgroundColor)
                    webView.headColor?.also {
                        setActionBarColor(it, true)
                    }
                    webView.isBackButtonVisible?.also {
                        setupBackButton(it)
                    }
                    webView.isCloseConfirm?.also {
                        setupClosingBehavior(it)
                    }
                    webView.isSettingVisible?.also {
                        settingMenuButtonVisible = it
                    }
                    webView.showFullscreen?.also {
                        showFullScreenMod = it
                    }
                    webView.appSettings?.also {
                        appSettings = it
                    }

                    webView.appSettings = appSettings
                }

                webAppEventProxy =  WebAppEventProxy(
                    context,
                    resourcesProvider,
                    this@DefaultWebViewFragment,
                    owner,
                    biometrySP)

                if (isUseCache) {
                    invalidateViewPortHeight(true)
                    requestTheme()
                }
            }

            override fun getOwner(): LifecycleOwner? {
                return launchConfig?.owner
            }

            override fun getMiniApp(): IMiniApp {
                return this@DefaultWebViewFragment
            }

            override fun getBridgeProvider(): BridgeProvider? {
                return launchConfig?.bridgeProvider
            }

            override fun isDApp(): Boolean {
                return launchConfig?.isDApp ?: false
            }

            override fun getCacheKey(): String?  {
                return launchConfig?.cacheKey()
            }

            override fun getCacheData(): String {
                return "${launchConfig?.cacheData()}"
            }

            override fun isMarketPlace(): Boolean {
                return false //launchConfig?.miniAppId == "10"
            }

            override fun getConfig(): WebAppParameters? {
                return launchConfig
            }

            override fun setPageFinished(url:String) {
                super.setPageFinished(url)
                invalidateViewPortHeight(true)
                hideLoadingView()
                updateActionBarTitle()
                if (isDApp()) {
                    getWebView()?.getPageData {
                        updateActionBarTitle()
                    }
                }
            }

            override fun onRequestPermission(request: PermissionRequest, isLocation: Boolean) {
                ActivityStack.getCurrentResultLauncher()?.also {
                    WebViewPermissionUtils.promptForPermissions(
                        title = resourcesProvider.getString("webview_permissions_resource_title"),
                        message = webPermissionToHumanReadable(permissions = request.resources, isLocation = isLocation),
                        request = request,
                        activity = UIContextUtil.findActivity(context)!!,
                        activityResultLauncher = it
                    )
                }
            }

            override fun onOverrideUrlLoading(url: String, isNewWindow: Boolean): Boolean {
                try {
                    val uri = Uri.parse(url)
                    if (uri.scheme?.lowercase() == "file" ||
                        uri.scheme?.lowercase() == "blob") {
                        return false
                    }

                    if (uri.scheme?.lowercase() != "http" &&
                        uri.scheme?.lowercase() != "https") {
                        openInBrowser(context, url)
                        return true
                    }

                    if (SchemeUtils.isInternalUri(Uri.parse(url), hosts = MiniAppServiceImpl.getInstance().miniAppHost)) {
                        openUrl(url, true)
                        return true
                    }

                    if (isNewWindow) {
                        openInBrowser(context, url)
                        return true
                    }

                } catch (_: Exception) {
                }

                return false
            }

            override fun doUpdateVisitedHistory(view: WebView?) {
                updateBackButtonState()
            }

            override fun setPageError(url: String, errorCode: Int, description: String): Boolean {
                return url==getLoadUrl()
            }

            override fun dismissSilent(complete: () -> Unit) {
                dismissImmediately(true, complete)
            }

            override fun showCustomView(
                view: View?,
                callback: WebChromeClient.CustomViewCallback?
            ) {
                view?.also {
                    fullScreenView = it
                    this@DefaultWebViewFragment.addView(
                        it,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }

            override fun hideCustomView() {
                fullScreenView?.also {
                    this@DefaultWebViewFragment.removeView(it)
                }
                fullScreenView = null
            }
        }

        linePaint.style = Paint.Style.FILL_AND_STROKE
        linePaint.strokeWidth = AndroidUtils.dp(4f).toFloat()
        linePaint.strokeCap = Paint.Cap.ROUND
        dimPaint.setColor(0x40000000)

        setSizeDelegate { keyboardHeight, isWidthGreater ->
            if (keyboardHeight > AndroidUtils.dp(20)) {
                swipeContainer.stickTo(-swipeContainer.getOffsetY() + swipeContainer.getTopActionBarOffsetY())
            }
        }

        webViewContainer.setWebViewProgressListener { progress ->
            updateLoadingProcess(progress)
        }

        pageLoadingView = PageLoadingView(context, resourcesProvider)
        swipeContainer.addView(
            pageLoadingView,
            LayoutHelper.MATCH_PARENT,
            LayoutHelper.MATCH_PARENT
        )

        swipeContainer.addView(
            webViewContainer,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        )

        swipeContainer.setDelegate {
            if (!checkDismissByUser()) {
                swipeContainer.stickTo(0f)
            } else {
                dismissImmediately()
            }
        }

        swipeContainer.setScrollListener {
            if (swipeContainer.getSwipeOffsetY() > 0) {
                val newAlpha = (1f - MathUtils.clamp(
                    swipeContainer.getSwipeOffsetY().toInt() / swipeContainer.getHeight()
                        .toFloat(), 0f, 1f
                ))
                dimPaint.setAlpha(
                    (0x40 * newAlpha).toInt()
                )
            } else {
                dimPaint.setAlpha(0x40)
            }
            invalidate()
            invalidateViewPortHeight(false)

            val progress: Float = 1f - swipeContainer.getTopActionBarOffsetY().toFloat()
                .coerceAtMost(swipeContainer.getTranslationY() - swipeContainer.getTopActionBarOffsetY()) / swipeContainer.getTopActionBarOffsetY()
            val newPos = (if (progress > 0.5f) 1 else 0) * 100f

            defaultDelegate?.onSpringProcess(newPos)

            if (isFullScreenMod() || useCustomNavigation()) {
                updateToolBarPosition(swipeContainer.translationY)
            }

            if (springAnimation != null) {
                if (springAnimation!!.spring.finalPosition != newPos) {
                    springAnimation!!.spring.setFinalPosition(newPos)
                    springAnimation!!.start()
                }
            }
            lastSwipeTime = System.currentTimeMillis()
        }

        swipeContainer.setScrollEndListener {
            if (!dismissed) {
                isViewPortByMeasureSuppressed = false
                invalidateViewPortHeight(false)
                isExpanded = (swipeContainer.getSwipeOffsetY() == -swipeContainer.getOffsetY() + swipeContainer.getTopActionBarOffsetY())
            }
        }

        setWebViewTopOffsetY()

        swipeContainer.setSwipeOffsetAnimationDisallowed(true)

        swipeContainer.setIsKeyboardVisible {
            getKeyboardHeight() >= AndroidUtils.dp(20)
        }

        addView(
            swipeContainer,
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                0,
                lineBarHeight,
                0,
                0
            )
        )

        addView(
            object : WebProgressView(context, resourcesProvider) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    var tmpWidthMeasureSpec = widthMeasureSpec
                    if (AndroidUtils.isTablet(context) && !AndroidUtils.isInMultiWindow && !AndroidUtils.isSmallTablet()) {
                        tmpWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            (AndroidUtils.displaySize.x.coerceAtMost(AndroidUtils.displaySize.y) * 0.8f).toInt(),
                            MeasureSpec.EXACTLY
                        )
                    }
                    super.onMeasure(tmpWidthMeasureSpec, heightMeasureSpec)
                }
            }.also { progressView = it },
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                0,
                0,
                0,
                0
            )
        )

        mainButton = buildMainButton()

        toolBarComponent = buildToolBarComponent(this)
        toolBarComponent?.visibility = View.GONE
    }

    private fun hideLoadingView() {
        pageLoadingView.hide()
    }

    private fun updateBackButtonState() {
        if (true == launchConfig?.isDApp) {
            setupBackButton(true==webViewContainer.getWebView()?.canGoBack())
        }
    }

    private fun updateLoadingProcess(progress: Float) {
        progressView.setLoadProgressAnimated(progress)
        if (progress == 1f) {
            val animator = ValueAnimator.ofFloat(1f, 0f).setDuration(200)
            animator.interpolator = CubicBezierInterpolator.DEFAULT
            animator.addUpdateListener { animation: ValueAnimator ->
                progressView.setAlpha(
                    (animation.getAnimatedValue() as Float)
                )
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    progressView.visibility = GONE
                }
            })
            animator.start()
        }
    }

    fun webPermissionToHumanReadable(permissions: Array<String>? = null, isLocation: Boolean = false, isQrcode: Boolean = false): String {
        if (isLocation) {
            return resourcesProvider.getString("webview_request_geolocation_permission", MiniAppServiceImpl.getInstance().appName )
        }

        if (isQrcode) {
            return resourcesProvider.getString("webview_request_camera_permission",
                MiniAppServiceImpl.getInstance().appName )
        }

        if (permissions==null) {
            return ""
        }

        if (permissions.size > 1){
            if ( permissions[0]==PermissionRequest.RESOURCE_AUDIO_CAPTURE && permissions[1]==PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                return resourcesProvider.getString("webview_request_camera_mic_permission", MiniAppServiceImpl.getInstance().appName )
            }
        }

        val permission = permissions[0]

        return when (permission) {
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> resourcesProvider.getString("webview_request_microphone_permission", MiniAppServiceImpl.getInstance().appName)
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> resourcesProvider.getString("webview_request_camera_permission", MiniAppServiceImpl.getInstance().appName)
            else -> permissions.toString()
        }
    }

    private fun buildMainButton() : TextView {
        val btn = object : TextView(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                var tempWidthMeasureSpec = widthMeasureSpec
                if (AndroidUtils.isTablet(context) && !AndroidUtils.isInMultiWindow && !AndroidUtils.isSmallTablet()) {
                    tempWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        (AndroidUtils.displaySize.x.coerceAtMost(AndroidUtils.displaySize.y) * 0.8f).toInt(), MeasureSpec.EXACTLY
                    )
                }
                super.onMeasure(tempWidthMeasureSpec, heightMeasureSpec)
            }
        }
        btn.visibility = GONE
        btn.setAlpha(0f)
        btn.setSingleLine()
        btn.setGravity(Gravity.CENTER)
        val padding: Int = AndroidUtils.dp(16)
        btn.setPadding(padding, 0, padding, 0)
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        btn.setOnClickListener { v: View? -> webAppProxy?.onMainButtonPressed() }
        mainButtonHeight = AndroidUtils.dp(48)
        addView(
            btn,
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                48,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = AndroidUtils.navigationBarHeight
            }
        )
        mainButtonAutoAnimator = VerticalPositionAutoAnimator.attach(btn)
        return btn
    }

    private fun buildRadialProgressView() : RadialProgressView {
        val radialProgress = object : RadialProgressView(context, resourcesProvider) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                val params = layoutParams as MarginLayoutParams
                if (AndroidUtils.isTablet(context) && !AndroidUtils.isInMultiWindow && !AndroidUtils.isSmallTablet()) {
                    params.rightMargin = (AndroidUtils.dp(10) + Math.min(
                        AndroidUtils.displaySize.x,
                        AndroidUtils.displaySize.y
                    ))
                } else {
                    params.rightMargin = AndroidUtils.dp(10)
                }
            }
        }
        radialProgress.setSize(AndroidUtils.dp(18))
        radialProgress.setAlpha(0f)
        radialProgress.scaleX = 0.1f
        radialProgress.scaleY = 0.1f
        radialProgress.visibility = GONE

        addView(
            radialProgress,
            LayoutHelper.createFrame(28, 28, Gravity.BOTTOM or Gravity.RIGHT, 0, 0, 10, 10)
        )
        radialProgressAutoAnimator = VerticalPositionAutoAnimator.attach(radialProgress)

        return radialProgress
    }

    private fun setWebViewTopOffsetY() {
        if(!isFullScreenMod()) {
            if (!useCustomNavigation() && showActionBar) {
                swipeContainer.setTopActionBarOffsetY(
                    ActionBar.getCurrentActionBarHeight(context)
                            + (if(isStatusBarVisible()) AndroidUtils.statusBarHeight else 0)
                            -  AndroidUtils.dp(lineBarHeight)
                )
            } else {
                swipeContainer.setTopActionBarOffsetY(
                            + (if(isStatusBarVisible()) AndroidUtils.statusBarHeight else 0)
                            -  AndroidUtils.dp(lineBarHeight)
                )
            }
        } else {
            swipeContainer.setTopActionBarOffsetY(0)
        }
    }

    private fun buildToolBarComponent(root: ViewGroup) : View {

        var toolBar = launchConfig?.actionBarBuilder?.let {
            it( {
                if(backPress()) {
                    hideAndDestroy()
                }
            }, {
                pressShareApp(null)
            } ).let {
                root.addView(it.second, it.first)
                it.second
            }
        }

        if (null!=toolBar) {
            return toolBar
        }

        toolBar = ToolBarComponent(resourcesProvider, context)
        toolBar.dismiss = {
            hideAndDestroy()
        }

        toolBar.mini = {
            minimization()
        }

        toolBar.share = {
            pressShareApp(it)
        }

        val layoutParams = LayoutParams(
            AndroidUtils.dp(109),
            AndroidUtils.dp(30)
        )

        layoutParams.topMargin = AndroidUtils.statusBarHeight + (ActionBar.getCurrentActionBarHeight(context) - AndroidUtils.dp(32))/2

        if (isRtl()) {
            layoutParams.marginStart = AndroidUtils.dp(15)
            layoutParams.gravity = Gravity.TOP or Gravity.START
        } else {
            layoutParams.marginEnd = AndroidUtils.dp(15)
            layoutParams.gravity = Gravity.TOP or Gravity.END
        }

        root.addView(toolBar, layoutParams)

        return toolBar
    }

    private fun isRtl(): Boolean {
        return resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    }

    private fun Context.toFrameManager() : FragmentManager? {
        if (this is FragmentActivity) {
            return supportFragmentManager
        }
        return null
    }

    private fun getVisibleMenusType() : List<MenusBottomSheet.MenuItem> {

        return mutableListOf(
            MenusBottomSheet.MenuItem(
                "RELOAD",
                R.drawable.ic_menu_reload_page,
                resourcesProvider.getString("action_reload")
            ),
            MenusBottomSheet.MenuItem(
                "SHORTCUT",
                R.drawable.ic_menu_shortcut,
                resourcesProvider.getString("action_shortcut")
            ),
        ).apply {

            if (miniAppDto?.isShareEnabled == true
                || launchConfig?.dAppDto?.isShareEnabled == true ||
                (launchConfig?.dAppDto == null && true == launchConfig?.isDApp)) {
                add(0,
                    MenusBottomSheet.MenuItem(
                        "SHARE",
                        R.drawable.ic_menu_share_to_chat,
                        resourcesProvider.getString("action_share")
                    )
                )
            }

            if (settingMenuButtonVisible) {
                add(
                    MenusBottomSheet.MenuItem(
                        "SETTINGS",
                        R.drawable.ic_menu_settings,
                        resourcesProvider.getString("action_settings")
                    )
                )
            }

            add(
                MenusBottomSheet.MenuItem(
                    "FEEDBACK",
                    R.drawable.ic_menu_feedback,
                    resourcesProvider.getString("action_feedback")
                )
            )
            getTermsOfServiceUrl()?.also {
                add(
                    MenusBottomSheet.MenuItem(
                        "TERMS",
                        R.drawable.ic_menu_agreement,
                        resourcesProvider.getString("action_terms")
                    )
                )
            }

            getPrivacyUrl()?.also {
                add(
                    MenusBottomSheet.MenuItem(
                        "PRIVACY",
                        R.drawable.ic_menu_privacy,
                        resourcesProvider.getString("action_privacy")
                    )
                )
            }
        }
    }

    private fun pressShareApp(view: View?) {

        val menus = getVisibleMenusType()

        if (true == MiniAppServiceImpl.getInstance().appDelegate?.onMoreButtonClick(this, menus.map { it.id })) {
            return
        }

        launchConfig?.context?.toFrameManager()?.also {
            MenusBottomSheet.newInstance(getVisibleMenusType()) { id ->
                clickMenu(id)
            }.show( it,  "MenusBottomSheet")
        }
//
//        view?.also {
//            powerMenuBuilder().showAsAnchorRightTop(it)
//        }
    }

    private fun buildActionBar() : ActionBar {

        val bar = object : ActionBar(context, resourcesProvider, { parent-> buildToolBarComponent(parent) } ) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                var widthMeasureSpecTemp = widthMeasureSpec
                if (AndroidUtils.isTablet(context) && !AndroidUtils.isInMultiWindow && !AndroidUtils.isSmallTablet()) {
                    widthMeasureSpecTemp = MeasureSpec.makeMeasureSpec(
                        (AndroidUtils.displaySize.x.coerceAtMost(AndroidUtils.displaySize.y) * 0.8f).toInt(),
                        MeasureSpec.EXACTLY
                    )
                }
                super.onMeasure(widthMeasureSpecTemp, heightMeasureSpec)
            }
        }

        actionBarShadow =
            ContextCompat.getDrawable(context, R.drawable.header_shadow)!!.mutate()

        bar.setBackgroundColor(actionBarColor)
        bar.setBackButtonDrawable(BackDrawable(close = true))
        bar.setTitle( miniAppDto?.title ?: launchConfig?.miniAppName ?: "", null)
        bar.setAlpha(0f)

        if (useWeChatStyle) {
            bar.useWeChatMenu()
            bar.actionBarMenuOnItemClick = (object : ActionBar.ActionBarMenuOnItemClick {
                override fun onItemClick(id: Int) {
                    if (-1 == id) {
                        if(backPress()) {
                            hideAndDestroy()
                        }
                    } else if (-2 == id) {
                        pressShareApp(null)
                    }
                }
            })
        }

        addView(
            bar,
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            )
        )

        return bar
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        AndroidUtils.rectTmp.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRect(AndroidUtils.rectTmp, dimPaint)

        if(!isFullScreenMod()) {
            actionBarPaint.setColor(actionBarColor)
            var radius: Float =
                AndroidUtils.dp(16) * (if (AndroidUtils.isTablet(context)) 1f else 1f - actionBarTransitionProgress)

            val isExpandView = (swipeContainer.getSwipeOffsetY() == -swipeContainer.getOffsetY() + swipeContainer.getTopActionBarOffsetY())
            if(isExpandView) {
                radius = 0f
            }

            AndroidUtils.rectTmp.set(
                swipeContainer.left.toFloat(),
                Utilities.lerp(swipeContainer.translationY, 0, actionBarTransitionProgress).toFloat(),
                swipeContainer.right.toFloat(),
                swipeContainer.translationY + AndroidUtils.dp(lineBarHeight) + radius
            )

            // draw the round panel
            canvas.drawRoundRect(AndroidUtils.rectTmp, radius, radius, actionBarPaint)
        }

        backgroundPaint.setColor(windowBackgroundColor)

        AndroidUtils.rectTmp.set(
            swipeContainer.left.toFloat(),
            if (isFullScreenMod()) swipeContainer.translationY else  (swipeContainer.translationY + AndroidUtils.dp(lineBarHeight)),
            swipeContainer.right.toFloat(),
            height.toFloat()
        )
        canvas.drawRect(AndroidUtils.rectTmp, backgroundPaint)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val transitionProgress: Float =
            if (AndroidUtils.isTablet(context)) 0f else actionBarTransitionProgress

        var y = if (AndroidUtils.isTablet(context)) {
            Utilities.lerp(
                swipeContainer.translationY + AndroidUtils.dp(12),
                AndroidUtils.statusBarHeight / 2,
                actionBarTransitionProgress
            ).toFloat()
        } else {
            Utilities.lerp(
                swipeContainer.translationY,
                AndroidUtils.statusBarHeight + ActionBar.getCurrentActionBarHeight(context) / 2,
                transitionProgress
            ).toFloat() + AndroidUtils.dp(12)
        }

        val isExpandView = (swipeContainer.getSwipeOffsetY() == -swipeContainer.getOffsetY() + swipeContainer.getTopActionBarOffsetY())

        if(!isExpandView && !isFullScreenMod()) {

            linePaint.setColor(lineColor)
            linePaint.setAlpha((linePaint.alpha * (1f - min(0.5f, transitionProgress) / 0.5f)).toInt())

            canvas.save()
            val scale = 1f - transitionProgress
            canvas.scale(scale, scale, width / 2f, y)
            canvas.drawLine(
                width / 2f - AndroidUtils.dp(16),
                y,
                width / 2f + AndroidUtils.dp(16),
                y,
                linePaint
            )
            canvas.restore()
        }

        if(showActionBar && !isFullScreenMod() && !useCustomNavigation()) {
            actionBarShadow?.alpha = (actionBar.alpha * 0xFF).toInt()
            y = actionBar.y + actionBar.translationY + actionBar.height
            actionBarShadow?.setBounds(
                0,
                y.toInt(),
                width,
                (y + actionBarShadow!!.intrinsicHeight).toInt()
            )
            actionBarShadow?.draw(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if ( event.action == MotionEvent.ACTION_DOWN
            && (event.y <= Utilities.lerp(swipeContainer.translationY + AndroidUtils.dp(lineBarHeight), 0, actionBarTransitionProgress)
                    || event.x > swipeContainer.right
                    || event.x < swipeContainer.left)
        ) {
            if (checkDismissByUser()) {
                dismissImmediately()
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun updateActionBarColors() {
        if (!overrideBackgroundColor) {
            actionBar.updateItemTextColor()
        }
    }

    private fun updateLightStatusBar() {

        val lightColorValue = ColorUtils.calculateLuminance(actionBarColor)

        val lightStatusBar =
            !AndroidUtils.isTablet(context) &&
                    lightColorValue >= 0.5
                    && actionBarTransitionProgress >= 0.85f

        if (wasLightStatusBar==null) {
            wasLightStatusBar = AndroidUtils.isStatusBarLight(this)
            isContainerLightStatusBar = wasLightStatusBar
        }

        if (wasLightStatusBar == lightStatusBar) {
            return
        }
        wasLightStatusBar = lightStatusBar

        if (defaultDelegate != null) {
            defaultDelegate.onUpdateLightStatusBar(lightStatusBar)
            return
        }

        AndroidUtils.setLightStatusBar(this, lightStatusBar)
    }

    override fun app(): IMiniApp {
        return this
    }

    override fun allowThisScroll(x: Boolean, y: Boolean) {
        swipeContainer.allowThisScroll(x, y)
    }

    override fun webApp(): IWebApp? {
        return webViewContainer.getWebApp()
    }

    override fun setPageReady() {
        webViewContainer.setPageFinished(webViewContainer.getWebView()?.url ?: "")
    }

    override fun hideAndDestroy(immediately: Boolean, isSilent: Boolean, complete: (() -> Unit)?): Boolean {
        if (immediately || checkDismissByUser()) {
            dismissImmediately(isSilent, complete)
            return true
        }
        return false
    }
    override fun dismissImmediately(isSilent: Boolean, complete: (() -> Unit)?) {

        allJobs.cancel()

        webViewContainer.getWebView()?.also {
            launchConfig?.bridgeProvider?.onWebViewDestroy(webView = it)
        }

        if (FloatingWindowManager.isAppOnMinimization(defaultDelegate?.getParent() ?: this)) {
            FloatingWindowManager.closeAfterConfirm()
        }
        isContainerLightStatusBar?.also {
            AndroidUtils.setLightStatusBar(this, it)
        }

        if (defaultDelegate != null) {
            defaultDelegate.dismissImmediately(isSilent, complete)
        } else {
            dismissWebView(isSilent, complete)
        }
    }

    override fun expandView(isFromWeb: Boolean) {
        if (isFromWeb) {
            webViewContainer.getWebView()?.isExpanded = true
        }
        if (swipeContainer.isSwipeInProgress) {
            return
        }
        isExpanded = true
        swipeContainer.stickTo(-swipeContainer.getOffsetY() + swipeContainer.getTopActionBarOffsetY())
    }

    override fun setActionBarColor(color: Int, isOverrideColor: Boolean) {
        actionBarColor = color
        overrideBackgroundColor = isOverrideColor
        webViewContainer.getWebView()?.headColor = color
        invalidate()
        val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(200)
        animator.interpolator = CubicBezierInterpolator.DEFAULT
        animator.addUpdateListener { animation: ValueAnimator ->
            actionBarPaint.setColor(actionBarColor)
            invalidateActionBar()
        }
        animator.start()
    }

    override fun setSettingsButtonVisible(visible: Boolean) {
        settingMenuButtonVisible = visible
        webViewContainer.getWebView()?.isSettingVisible = visible
    }

    override fun requestQrCodeScan(subTitle: String?) {
        ActivityStack.getCurrentResultLauncher()?.also {
            PermissionsTools.checkPermissions(
                PERMISSIONS_FOR_ROOM_AVATAR, ActivityStack.getCurrentActivity()!!, it,
                title = resourcesProvider.getString("webview_permissions_resource_title"),
                message = webPermissionToHumanReadable(isQrcode = true) ) { allGranted, _ ->
                if (allGranted) {
                    owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {
                        val result = MiniAppServiceImpl.getInstance().appDelegate?.scanQrCodeForResult(this@DefaultWebViewFragment, subTitle)
                        webAppProxy?.responseQrScanResult(result ?: "")
                        webAppProxy?.notifyQrPopupClosed()
                    }
                } else {
                    webAppProxy?.notifyQrPopupClosed()
                }
            }
        }
    }

    override fun closeQrCodeScan() {
        webAppProxy?.notifyQrPopupClosed()
    }

    override fun setContainerColor(color: Int) {
        windowBackgroundColor = color
        webViewContainer.getWebView()?.bgColor = color
        val from = backgroundPaint.color
        val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(200)
        animator.interpolator = CubicBezierInterpolator.DEFAULT
        animator.addUpdateListener { animation: ValueAnimator ->
            backgroundPaint.setColor(
                ColorUtils.blendARGB(
                    from, color,
                    (animation.getAnimatedValue() as Float)
                )
            )
            updateActionBarColors()
            this@DefaultWebViewFragment.invalidate()
        }
        animator.start()
    }

    fun reportSafeInsets(insets: Rect?, topContentMargin: Int) {
        reportSafeInsets(insets, false)
        reportSafeContentInsets(topContentMargin, false)
    }

    private fun reportSafeInsets(insets: Rect?, force: Boolean) {
        if (insets == null || !force && lastInsets == insets) return
        val density = context.resources.displayMetrics.density
        webAppProxy?.notifySafeAreaChanged(
            JsonUtils.obj(
                "left", insets.left / density,
                "top", insets.top / density,
                "right", insets.right / density,
                "bottom", insets.bottom / density
            )
        )
        lastInsets.set(insets)
    }

    private fun reportSafeContentInsets(topContentMargin: Int, force: Boolean) {
        if (!force && topContentMargin == lastInsetsTopMargin) return
        val density = context.resources.displayMetrics.density
        webAppProxy?.notifyContentSafeAreaChanged(
            JsonUtils.obj(
                "left", 0,
                "top", topContentMargin / density,
                "right", 0,
                "bottom", 0
            )
        )
        lastInsetsTopMargin = topContentMargin
    }

    private var lastViewportHeightReported = 0
    private var lastViewportStateStable = false
    private var lastViewportIsExpanded = false

    override fun invalidateViewPortHeight(force: Boolean, fromWebApp: Boolean) {
        val isStable = swipeContainer.isSwipeInProgress
        invalidate()
        if (!webViewContainer.isPageLoaded && !force) {
            return
        }

        if (isStable) {
            lastExpanded =
                swipeContainer.getSwipeOffsetY() == -swipeContainer.getOffsetY() + swipeContainer.getTopActionBarOffsetY()
        }
        val viewPortHeight =
            (swipeContainer.measuredHeight - swipeContainer.getOffsetY() - swipeContainer.getSwipeOffsetY() + swipeContainer.getTopActionBarOffsetY()).toInt()

        if (
            force || viewPortHeight != lastViewportHeightReported || lastViewportStateStable != isStable || lastViewportIsExpanded != lastExpanded
        ) {
            lastViewportHeightReported = viewPortHeight
            lastViewportStateStable = isStable
            lastViewportIsExpanded = lastExpanded
            webAppProxy?.notifyViewportChange(viewPortHeight.coerceAtLeast(AndroidUtils.getScreenHeight(context)/2), isStable, lastExpanded)
        }
    }

    override fun requestTheme() {
        webAppProxy?.notifyThemeChanged()
    }

    override fun closePopUp(btnId: String) {
        lastClickMs = System.currentTimeMillis()
        webAppProxy?.notifyPopupClosed(btnId)
    }

    override fun sendWebViewData(content: String?) {
        owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {
            MiniAppServiceImpl.getInstance().appDelegate?.apply {
                val needDismiss = sendMessageToPeer(this@DefaultWebViewFragment, content)
                if (needDismiss) {
                    withContext(Dispatchers.Main) {
                        dismissImmediately()
                    }
                }
            }
        }
    }

    override fun setupClosingBehavior(enable: Boolean) {
        needCloseConfirmation = enable
        webViewContainer.getWebView()?.isCloseConfirm = needCloseConfirmation
    }

    override fun requestSendMessageAccess() {
        owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {
            MiniAppServiceImpl.getInstance().appDelegate?.apply {
                if ( checkPeerMessageAccess(this@DefaultWebViewFragment) ) {
                    webAppProxy?.responseWriteAccess(true)
                } else {
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(context)
                            .setTitle(resourcesProvider.getString("dialog_write_access_title"))
                            .setMessage(resourcesProvider.getString("dialog_write_access_message"))
                            .setPositiveButton(resourcesProvider.getString("action_allow")) { _, _ ->
                                owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {
                                    if(requestPeerMessageAccess(this@DefaultWebViewFragment)) {
                                        webAppProxy?.responseWriteAccess(true)
                                    } else {
                                        webAppProxy?.responseWriteAccess(false)
                                    }
                                }
                            }
                            .setNegativeButton(resourcesProvider.getString("action_not_allow")) { _, _ ->
                                webAppProxy?.responseWriteAccess(false)
                            }
                            .create()
                            .show()
                    }
                }
            }
        }
    }

    override fun requestPhone() {
        AlertDialog.Builder(context)
            .setTitle(resourcesProvider.getString("dialog_share_phone_title"))
            .setMessage(resourcesProvider.getString("dialog_share_phone_message"))
            .setPositiveButton(resourcesProvider.getString("action_share_contact")) { _, _ ->
                owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {
                    val status =  MiniAppServiceImpl.getInstance().appDelegate?.requestPhoneNumberToPeer(this@DefaultWebViewFragment)
                    webAppProxy?.responsePhone(status==true)
                }
            }
            .setNegativeButton(resourcesProvider.getString("action_cancel")) { _, _ ->
                webAppProxy?.responsePhone(false)
            }
            .create()
            .show()
    }

    fun checkDismissByUser(): Boolean {
        if (needCloseConfirmation) {
            val dialog = AlertDialog.Builder(context)
                .setTitle(launchConfig?.miniAppName ?: "")
                .setMessage(resourcesProvider.getString("dialog_close_save_message"))
                .setPositiveButton(resourcesProvider.getString("action_close_anyway")) { dialog2, which ->
                    dismissImmediately()
                    FloatingWindowManager.closeAfterConfirm()
                }
                .setNegativeButton(resourcesProvider.getString("action_cancel"), null)
                .create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resourcesProvider.getColor("text_err_color"))
            return false
        }
        return true
    }

    override fun setupExpandBehavior(enable: Boolean) {
        if (enable == canExpand) {
            return
        }
        if (!enable) {
            expandView()
        }
        canExpand = enable
        swipeContainer.setGesture(enable)
        webViewContainer.getWebView()?.allowExpand = canExpand
    }

    override fun isClipboardAvailable(): Boolean {
        return System.currentTimeMillis() - lastClickMs > 10000
    }

    override fun clipboardData(reqId: String, content: String) {
        webAppProxy?.clipboardData(reqId, content)
    }

    override fun setupMainButton(
        isVisible: Boolean,
        isActive: Boolean,
        text: String?,
        color: Int,
        textColor: Int,
        isProgressVisible: Boolean
    ) {

        expandView()

        mainButton.isClickable = isActive
        mainButton.text = text
        mainButton.setTextColor(textColor)
        mainButton.background = getMainButtonRippleDrawable(color)
        if (isVisible != mainButtonWasVisible) {
            mainButtonWasVisible = isVisible
            mainButton.animate().cancel()
            if (isVisible) {
                mainButton.setAlpha(0f)
                mainButton.visibility = VISIBLE
            }
            mainButton.animate().alpha(if (isVisible) 1f else 0f).setDuration(150)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!isVisible) {
                            mainButton.visibility = GONE
                        }
                        swipeContainer.requestLayout()
                    }
                }).start()
        }
        radialProgressView.setProgressColor(textColor)
        if (isProgressVisible != mainButtonProgressWasVisible) {
            mainButtonProgressWasVisible = isProgressVisible
            radialProgressView.animate()?.cancel()
            if (isProgressVisible) {
                radialProgressView.setAlpha(0f)
                radialProgressView.visibility = VISIBLE
            }
            radialProgressView.animate().alpha(if (isProgressVisible) 1f else 0f)
                .scaleX(if (isProgressVisible) 1f else 0.1f)
                .scaleY(if (isProgressVisible) 1f else 0.1f)
                .setDuration(250)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!isProgressVisible) {
                            radialProgressView.visibility = GONE
                        }
                    }
                }).start()
        }
    }

    private fun isStatusBarVisible() : Boolean {
        return showStatusBar && !useCustomNavigation() && !isFullScreenMod()
    }

    private fun allowVerticalSwipe() : Boolean {
        if (true == launchConfig?.isDApp) {
            return false
        }
        if (true==showFullScreenMod) {
            return false
        }
        return canExpand ?: appSettings?.allowVerticalSwipe ?: appSettings?.let { it.viewStyle == "modal"  } ?: false
    }

    private fun allowHorizontalSwipe() : Boolean {
        if (true == launchConfig?.isDApp) {
            return false
        }
        return appSettings?.allowHorizontalSwipe ?: true
    }

    private fun useCustomNavigation() : Boolean {
        if (true==launchConfig?.useCustomNavigation) {
            return true
        }

        return appSettings?.let {
            it.navigationStyle == "custom"
        } ?: false
    }

    private fun useModalStyle() : Boolean {
        if (true==launchConfig?.useModalStyle) {
            return true
        }
        return appSettings?.let {
            it.viewStyle == "modal"
        } ?: false
    }

    private fun updateToolBarPosition(translationY: Float) {
        val statusBarHeight = (if(isFullScreenMod() || !isStatusBarVisible()) AndroidUtils.statusBarHeight else 0)
        val topY = (ActionBar.getCurrentActionBarHeight(context) - AndroidUtils.dp(32))/2
        val toolBarProgress = (translationY + topY + (statusBarHeight - translationY).coerceAtLeast(0f)).coerceAtLeast(translationY+ AndroidUtils.dpf2(20f))

        val layoutParams = toolBarComponent?.layoutParams as? LayoutParams
        layoutParams?.topMargin = toolBarProgress.toInt()

        toolBarComponent?.layoutParams = layoutParams
        toolBarComponent?.requestLayout()
    }

    private fun resetActionBar() {
        if (isFullScreenMod() || useCustomNavigation()) {
            actionBar.visibility = View.GONE
            toolBarComponent?.visibility = if (showActionBar) View.VISIBLE else View.GONE
            onSafaAreChange(true)
        } else {
            actionBar.setOccupyStatusBar(isStatusBarVisible())
            toolBarComponent?.visibility = View.GONE
            actionBar.visibility = if (showActionBar) View.VISIBLE else View.GONE
            onSafaAreChange(false)
        }
    }

    private fun onSafaAreChange(showFull: Boolean) {
        if (showFull) {
            val topMargin = ((toolBarComponent?.layoutParams as? LayoutParams)?.topMargin ?: 0) + (toolBarComponent?.measuredHeight ?: 0)
            reportSafeInsets(Rect(insets.left, 0, insets.right,  insets.bottom),topMargin)
        } else {
            reportSafeInsets(Rect(0, 0, 0, 0), 0)
        }
    }

    override fun setupFullScreen(enable: Boolean) {
        if (showFullScreenMod == enable) {
            return
        }

        showFullScreenMod = enable
        webViewContainer.getWebView()?.showFullscreen = enable

        onSafaAreChange(enable)
        setupWithOptions()
        expandView()
    }

    private fun setupWithOptions() {

        setWebViewTopOffsetY()
        resetActionBar()

        if (isFullScreenMod()) {
            swipeContainer.layoutParams = (swipeContainer.layoutParams as LayoutParams).also {
                it.topMargin = 0
            }
        } else {
            swipeContainer.layoutParams = (swipeContainer.layoutParams as LayoutParams).also {
                it.topMargin =  AndroidUtils.dp(lineBarHeight)
            }
        }

        swipeContainer.requestLayout()
        swipeContainer.setGesture(allowVerticalSwipe())

        webAppProxy?.responseFullscreenChange(isFullScreenMod())
    }

    override fun setupShowHead(visible: Boolean, isUserAction: Boolean) {
        if(showActionBar == visible) {
            return
        }
        val shouldExpandView = (swipeContainer.getSwipeOffsetY() == -swipeContainer.getOffsetY() + swipeContainer.getTopActionBarOffsetY())

        showActionBar = visible

        setWebViewTopOffsetY()
        resetActionBar()
        swipeContainer.requestLayout()

        if (shouldExpandView) {
            expandView()
        }
    }

    override fun setupBackButton(visible: Boolean) {
        webViewContainer.getWebView()?.isBackButtonVisible = visible
        if(visible == isBackButtonVisible) {
            return
        }
        isBackButtonVisible = visible
        actionBar.setBackButtonDrawable(BackDrawable(close = !visible))
        if (useWeChatStyle) {
            actionBar.setBackMenuVisible(visible)
        }
    }

    private fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.putExtra(Browser.EXTRA_CREATE_NEW_TAB, true)
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun openUrl(url: String, isInternal: Boolean) {
        try {
            owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {

                val uri = Uri.parse(url)
                val isMeUrl = SchemeUtils.isInternalUri(uri, hosts = MiniAppServiceImpl.getInstance().miniAppHost)

                if (isMeUrl) {
                    if (interceptAction(url)) {
                        return@launch
                    }

                    val segments = ArrayList<String>(uri.pathSegments)
                    if (segments.size == 1) {
                        openInBrowser(context, url)
                        return@launch
                    }
                }

                val isInternalLink = isInternal || isMeUrl

                launchConfig?.also {
                    MiniAppServiceImpl.getInstance().openUrl(it, url, isInternalLink)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun invokeCustomMethod(method: String, params: String?): String?   =  suspendCancellableCoroutine { continuation ->
        val process : (String) -> Unit =  { appId->
            owner.lifecycleScope.launch(Dispatchers.Main + allJobs) {
                miniAppRepository.invokeCustomMethod(
                    CustomMethodsParams(
                        method,
                        params,
                        appId
                    )
                ).catch {
                    it.printStackTrace()
                }.collect {
                    continuation.resume(it)
                }
            }
        }
        if (appId.isNullOrBlank()) {
            requestMiniAppInfo {
                if (!appId.isNullOrBlank()) {
                    process(appId!!)
                }
            }
        } else {
            process(appId!!)
        }
    }

    private suspend fun interceptAction(link: String) : Boolean {
        try {
            val uri = Uri.parse(link) ?: return false

            val setAsAttachBot = uri.getQueryParameter("startattach")
            val attachMenuBotChoose = uri.getQueryParameter("choose")
            if (null != setAsAttachBot && attachMenuBotChoose != null) {
                val needDismiss = MiniAppServiceImpl.getInstance().appDelegate?.attachWithAction(app = this, action = attachMenuBotChoose, payload = setAsAttachBot) ?: false
                if (needDismiss) {
                    dismissImmediately()
                }
                return true
            }

            val segments = uri.pathSegments

            if (segments.size == 2) {
                if (segments[0].lowercase() == "share" && segments[1].lowercase() == "url") {
                    val shareUrl = uri.getQueryParameter("url")
                    val shareText = uri.getQueryParameter("text")
                    MiniAppServiceImpl.getInstance().appDelegate?.shareLinkOrText(app = this, linkOrText = shareUrl ?: shareText ?: "")
                    return true
                }
            }

            return true== MiniAppServiceImpl.getInstance().appDelegate?.launchScheme(this, uri.toString())

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return  false
    }

    fun updateSpringPos(pos: Float) {
        actionBarTransitionProgress = pos
        invalidate()
        actionBar.setAlpha(pos)
        updateLightStatusBar()
    }

    fun getSpringPos(): Float {
        return actionBarTransitionProgress
    }

    private fun invalidateActionBar() {
        actionBar.setBackgroundColor(actionBarColor)
        actionBar.alpha = actionBarTransitionProgress
        updateLightStatusBar()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!isViewPortByMeasureSuppressed) {
            invalidateViewPortHeight(false)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (defaultDelegate == null && springAnimation == null) {
            springAnimation = SpringAnimation(this, ACTION_BAR_TRANSITION_PROGRESS_VALUE)
                .setSpring(
                    SpringForce()
                        .setStiffness(1200f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        springAnimation?.cancel()
        springAnimation = null
        actionBarTransitionProgress = 0f
    }

    private fun forceRefreshCache() : Boolean {
        return false
    }

    fun attachMiniApp() {
        launchConfig?.cacheKey()?.let {
            WebAppLruCache.get(it)
        }?.also {
            webViewContainer.checkCreateWebView {
                if (it && forceRefreshCache()) {
                    webViewContainer.reload()
                }
            }
        }
        requestLaunchUrl()
    }

    private fun loadUrl(url: String) {
        webViewContainer.loadUrl(url) {
            pageLoadingView.showLoading()
        }
    }

    override fun reloadPage() {
        progressView.setLoadProgress(0f)
        progressView.setAlpha(1f)
        progressView.visibility = VISIBLE
        pageLoadingView.showLoading()
        if (webViewContainer.shouldLoadNewUrl()) {
            requestLaunchUrl()
        } else {
            webViewContainer.reload()
        }
    }

    private var isOnSpringAnimation: Boolean = true

    fun show() {
        setAlpha(0f)
        isViewPortByMeasureSuppressed = true
        addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                v.removeOnLayoutChangeListener(this)
                swipeContainer.setSwipeOffsetY(swipeContainer.height.toFloat())
                setAlpha(1f)

                if(true == launchConfig?.isDApp || (useCacheWebView && ( true == showFullScreenMod || appSettings?.let { !useModalStyle() } == true))) {
                    isOnSpringAnimation = false
                    setupWithOptions()
                    autoExpandPage()
                    return
                }

                SpringAnimation(swipeContainer, WebViewSwipeContainer.SWIPE_OFFSET_Y, 0f)
                    .setSpring(SpringForce(0f).setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY).setStiffness(500.0f))
                    .apply {
                        start()
                        addEndListener { _, _, _, _ ->
                            isOnSpringAnimation = false
                            setupWithOptions()
                            webViewContainer.getWebView()?.allowExpand?.also {
                                setupExpandBehavior(it)
                                return@addEndListener
                            }
                            autoExpandPage()
                    }
                }
            }
        })
    }

    private fun autoExpandPage() {
        if (isOnSpringAnimation) {
            return
        }

        if (true == launchConfig?.autoExpand
            || true == webViewContainer.getWebView()?.isExpanded
            || true == launchConfig?.isDApp
            || !useModalStyle()
            || isFullScreenMod()
            || !allowVerticalSwipe()) {
            expandView()
        }
    }

    fun dismissWebView(silent: Boolean = false, complete: (() -> Unit) ? = null) {
        if (dismissed) {
            if (isPreload) {
                complete?.invoke()
            }
            return
        }
        dismissed = true

        if (isPreload) {
            releaseRef()
            complete?.invoke()
            onDismissListener.invoke()
        } else if(!silent) {
            swipeContainer.stickTo(swipeContainer.height.toFloat() + measureKeyboardHeight()) {
                post {
                    releaseRef()
                    complete?.invoke()
                    onDismissListener.invoke()
                }
            }
        } else {
            releaseRef()
            complete?.invoke()
            onDismissListener.invoke()
        }
    }

    private fun releaseRef() {
        visibility = GONE
        needCloseConfirmation = false
        swipeContainer.setWebView(null)
        webViewContainer.destroyWebView(!isPreload)
        parentView?.removeView(this)
    }

   fun showAfterAttach() {
        show()
    }

    override fun requestDismiss(force: Boolean, immediately: Boolean, isSilent: Boolean, complete: (() -> Unit)?): Boolean {
        if (force || (immediately || backPress())) {
            return hideAndDestroy(immediately, isSilent, complete)
        }
        return false
    }

    override suspend fun getShareUrl(): String?  =  suspendCancellableCoroutine { continuation ->
        owner.lifecycleScope.launch {
            val shareDto = getShareInfo()
            continuation.resume(shareDto?.url)
        }
    }

    private fun MiniAppDto.toShareUrl(): String {
       if (!this.id.isNullOrBlank()) {
          return "${MiniAppServiceImpl.getInstance().miniAppHost.firstOrNull()}/apps/${this.id}"
       }
       return "${MiniAppServiceImpl.getInstance().miniAppHost.firstOrNull()}/${this.botName ?: this.botId}/${this.identifier}"
    }

    private fun String?.resolve(): String? {
       if (this.isNullOrBlank()) {
           return null
       }
        if (this.lowercase().startsWith("http")) {
            return this
        }
        return null
    }

    private fun MiniAppDto.toShareData(): ShareDto {
        return ShareDto(
            type = "MINIAPP",
            id = this.id,
            url = this.toShareUrl(),
            identifier = this.identifier,
            title = webViewContainer.getWebView()?.pageParams?.let { webViewContainer.getWebView()?.pageTitle  } ?:  this.title,
            description = webViewContainer.getWebView()?.pageParams?.let { webViewContainer.getWebView()?.pageDescription } ?: this.description,
            iconUrl =  this.iconUrl.resolve() ?: webViewContainer.getWebView()?.pageIcon,
            bannerUrl = webViewContainer.getWebView()?.pageImage.resolve() ?: this.bannerUrl,
            params = webViewContainer.getWebView()?.pageParams)
    }

    private fun DAppDto?.toShareData(): ShareDto {
        return ShareDto(type = "WEBPAGE",
            id = this?.id,
            url = webViewContainer.getWebView()?.url ?: this?.url,
            identifier = null,
            title = webViewContainer.getWebView()?.pageTitle ?: this?.title,
            description = webViewContainer.getWebView()?.pageDescription ?: this?.description,
            iconUrl = this?.iconUrl.resolve() ?: webViewContainer.getWebView()?.pageIcon,
            bannerUrl = webViewContainer.getWebView()?.pageImage.resolve() ?: this?.bannerUrl,
            params = webViewContainer.getWebView()?.pageParams)
    }

    override suspend fun getShareInfo(): ShareDto?  =  suspendCancellableCoroutine { continuation ->

        webViewContainer.getWebView()?.getPageData {
            if (true==launchConfig?.isDApp) {
                continuation.resume(launchConfig?.dAppDto.toShareData())
                return@getPageData
            }

            webViewContainer.getWebView()?.getPageData {
                if (miniAppDto != null) {
                    continuation.resume(miniAppDto?.toShareData())
                } else {
                    requestMiniAppInfo {
                        continuation.resume(it?.toShareData())
                    }
                }
            }
        }
    }

    override fun isSystem(): Boolean {
        return launchConfig?.isSystem ?: false
    }

    override fun maximize() {
        isVisible = true
        defaultDelegate?.onMaximize()
        webViewContainer.restoreWebView()
        invalidateViewPortHeight(true)
        MiniAppServiceImpl.getInstance().appDelegate?.onMaximize(this)
    }

    override fun minimization() {
        isVisible = false
        webViewContainer.saveWebViewState()
        defaultDelegate?.onMinimization()
        FloatingWindowManager.showFloatingWindow(
            UIContextUtil.findActivity(context)!!,
            defaultDelegate?.getParent() ?: this,
            webViewContainer.getWebView(), "",
            width = MiniAppServiceImpl.getInstance().appConfig?.floatWindowWidth,
            height = MiniAppServiceImpl.getInstance().appConfig?.floatWindowHeight
        )
        MiniAppServiceImpl.getInstance().appDelegate?.onMinimization(this)
    }

    private fun isBackButtonVisible() = isBackButtonVisible

    private fun getPrivacyUrl() : String? {
        return MiniAppServiceImpl.getInstance().appConfig?.privacyUrl
    }

    private fun getTermsOfServiceUrl() : String? {
        return MiniAppServiceImpl.getInstance().appConfig?.termsOfServiceUrl
    }

    override fun getAppId(): String? {
        return launchConfig?.miniAppId ?: miniAppDto?.id ?: launchConfig?.dAppDto?.id
    }

    override fun capture(): Bitmap? {

        if (!webViewContainer.isPageLoaded) {
            return null
        }

        val screenWidth = AndroidUtils.getScreenWidth(context)

        val screenshotHeight = (screenWidth * 0.8).toInt()

        val bitmap = Bitmap.createBitmap(screenWidth, screenshotHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        webViewContainer.getWebView()?.draw(canvas)

        return bitmap
    }

    override fun clickMenu(type: String) {
        when (type) {
            "SETTINGS" -> {
                webAppProxy?.onSettingsButtonPressed()
            }

            "RELOAD" -> {
                reloadPage()
            }

            "FEEDBACK",
            "SHARE" -> {
                MiniAppServiceImpl.getInstance().appDelegate?.onMenuButtonClick(this, type = type)
            }

            "SHORTCUT"-> {
                addHomeScreenShortcut()
            }

            "PRIVACY" -> {
                getPrivacyUrl()?.also {
                    MiniAppServiceImpl.getInstance().openUrl(launchConfig!!, it , false)
                }
            }

            "TERMS" -> {
                getTermsOfServiceUrl()?.also {
                    MiniAppServiceImpl.getInstance().openUrl(launchConfig!!, it, false)
                }
            }
        }
    }

    override fun addHomeScreenShortcut() {
        owner.lifecycleScope.launch {
            getShareUrl()?.also {
                val id = miniAppDto?.id ?: launchConfig?.dAppDto?.id
                val title = miniAppDto?.title ?: launchConfig?.dAppDto?.title ?: webViewContainer.getWebView()?.pageTitle ?: "App"
                val icon = miniAppDto?.iconUrl ?: launchConfig?.dAppDto?.iconUrl ?: webViewContainer.getWebView()?.pageIcon
                val type = if (true == launchConfig?.isDApp) HomeScreenShortcutUtils.SHORTCUT_DAPP else HomeScreenShortcutUtils.SHORTCUT_MINIAPP
                val data = launchConfig?.startParam

                if (HomeScreenShortcutUtils.isShortcutAdded(context, id ?: it, type)) {
                    webAppProxy?.responseShortcutStatus(isAdd = true, isFailure = false, null)
                    return@launch
                }

                HomeScreenShortcutUtils.createShortcutLink(
                    context,
                    it ,
                    title,
                    id ?: it,
                    type,
                    data,
                    icon)

                webAppProxy?.responseShortcutStatus(isAdd = true, isFailure = false, null)
            } ?: webAppProxy?.responseShortcutStatus(isAdd = false, isFailure = true, null)
        }
    }

    override fun checkScreenShortcut() {
        val id = miniAppDto?.id ?: launchConfig?.dAppDto?.id ?: run {
            webAppProxy?.responseShortcutStatus(isAdd = false, isFailure = false, null)
            return
        }
        val type = if (true == launchConfig?.isDApp) HomeScreenShortcutUtils.SHORTCUT_DAPP else HomeScreenShortcutUtils.SHORTCUT_MINIAPP
        if (HomeScreenShortcutUtils.isShortcutAdded(context, id, type)) {
            webAppProxy?.responseShortcutStatus(isAdd = false, isFailure = false, "missed")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                webAppProxy?.responseShortcutStatus(isAdd = false, isFailure = false, "added")
            } else {
                webAppProxy?.responseShortcutStatus(isAdd = false, isFailure = false, null)
            }
        }
    }

    override fun requestSafeArea() {
        reportSafeInsets(lastInsets, true)
    }

    override fun requestContentSafeArea() {
        reportSafeContentInsets(lastInsetsTopMargin, true)
    }

    fun hideOnly() {
        if (checkDismissByUser()) {
            swipeContainer.stickTo(0f)
        }
    }

    private fun backPress(): Boolean {
        if (true==launchConfig?.isDApp) {
            if (true == webViewContainer.getWebView()?.canGoBack()) {
                webViewContainer.getWebView()?.goBack()
                return false
            }
            return true
        }

        if (isBackButtonVisible) {
            webAppProxy?.responseBackPress()
            return false
        }
        return true
    }

    private fun getMainButtonRippleColor(buttonColor: Int): Int {
        return if (ColorUtils.calculateLuminance(buttonColor) >= 0.3f) 0x12000000 else 0x16FFFFFF
    }

    private fun getMainButtonRippleDrawable(buttonColor: Int): Drawable {
        return ThemeUtils.createSelectorWithBackgroundDrawable(
            buttonColor,
            getMainButtonRippleColor(buttonColor)
        )
    }

    companion object {
        private val ACTION_BAR_TRANSITION_PROGRESS_VALUE =
            object : FloatPropertyCompat<DefaultWebViewFragment>("actionBarTransitionProgress") {
                override fun getValue(`object`: DefaultWebViewFragment): Float {
                    // Logic for returning property value
                    return `object`.actionBarTransitionProgress * 100f
                }

                override fun setValue(`object`: DefaultWebViewFragment, value: Float) {
                    // Logic for setting property value
                    `object`.actionBarTransitionProgress = value / 100f
                    `object`.invalidate()
                    `object`.invalidateActionBar()
                }
            }
    }
}