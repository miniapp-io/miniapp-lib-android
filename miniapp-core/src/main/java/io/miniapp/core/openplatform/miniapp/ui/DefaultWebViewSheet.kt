package io.miniapp.core.openplatform.miniapp.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.LifecycleOwner
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.IMiniApp
import io.miniapp.core.openplatform.miniapp.IResourcesProvider
import io.miniapp.core.openplatform.miniapp.ShareDto
import io.miniapp.core.openplatform.miniapp.WebAppParameters
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.webapp.IMiniAppListener

internal class DefaultWebViewSheet(context: Context,
                                   owner: LifecycleOwner,
                                   val launchConfig: WebAppParameters,
                                   isPreload: Boolean = false,
                                   resourcesProvider: IResourcesProvider,
                                   onDismissListener: ()-> Unit) : IMiniApp, Dialog(context, R.style.TransparentDialog),
    IMiniAppListener {

    private var onBackInvokedCallback: OnBackInvokedCallback? = null
    private val webViewFragment: DefaultWebViewFragment
    private var springAnimation: SpringAnimation? = null

    init {
        webViewFragment = DefaultWebViewFragment(
            context = context,
            owner = owner,
            launchConfig = launchConfig,
            resourcesProvider = resourcesProvider,
            isPreload = isPreload,
            defaultDelegate = this) {

        }

        setContentView(
            webViewFragment,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setOnDismissListener {
            onDismissListener.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window = window

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window!!.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        } else {
            window!!.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        val params = window.attributes
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        params.gravity = Gravity.TOP or Gravity.START
        params.dimAmount = 0f
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.setAttributes(params)
        window.statusBarColor = Color.TRANSPARENT

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.decorView.windowInsetsController?.hide(WindowInsets.Type.navigationBars())
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
                val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val navigationBarHeight =
                    insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                webViewFragment.setPadding(0, 0, 0, imeHeight - navigationBarHeight)
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AndroidUtils.setLightNavigationBar(
                window,
                ColorUtils.calculateLuminance(webViewFragment.windowBackgroundColor) >= 0.9
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedCallback = OnBackInvokedCallback {
                if (!webViewFragment.requestDismiss(false)) {
                    return@OnBackInvokedCallback
                }
                if(webViewFragment.checkDismissByUser()){
                    dismiss()
                }
            }.also {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(0, it)
            }
        }

        if (springAnimation == null) {
            springAnimation = SpringAnimation(this, ACTION_BAR_TRANSITION_PROGRESS_VALUE)
                .setSpring(SpringForce()
                    .setStiffness(1200f)
                    .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        onBackInvokedCallback?.also {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
        }

        if (springAnimation != null) {
            springAnimation!!.cancel()
            springAnimation = null
        }
    }

    override fun onSpringProcess(progress: Float) {
        springAnimation?.apply {
            if (spring.finalPosition != progress) {
                spring.setFinalPosition(progress)
                start()
            }
        }
    }

    override fun dismissImmediately(isSilent:Boolean, complete: (() -> Unit)?) {
        webViewFragment.dismissWebView(isSilent) {
            complete?.invoke()
            dismissNative()
        }
    }

    override fun onUpdateLightStatusBar(enable: Boolean) {
        window?.decorView?.also {
            AndroidUtils.setLightStatusBar(
                it,
                enable
            )
        }
    }

    private fun dismissNative() {
        try {
            super.dismiss()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun attachMiniApp() {
        webViewFragment.attachMiniApp()
    }

    override fun show() {
        webViewFragment.showAfterAttach()
        super.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!webViewFragment.requestDismiss(force = false, immediately = false)) {
            return
        }
        if(webViewFragment.checkDismissByUser()){
            dismiss()
        }
    }

    override fun dismiss() {
       webViewFragment.dismissWebView {
           dismissNative()
       }
    }

    fun showAfterAttach() {
        show()
    }

    override fun reloadPage() {
        webViewFragment.reloadPage()
    }

    override fun requestDismiss(force: Boolean, immediately: Boolean, isSilent: Boolean, complete: (() -> Unit)?): Boolean {
        return webViewFragment.requestDismiss(force, immediately, isSilent, complete)
    }

    override suspend fun getShareUrl(): String? {
        return webViewFragment.getShareUrl()
    }

    override suspend fun getShareInfo(): ShareDto? {
        return webViewFragment.getShareInfo()
    }

    override fun isSystem(): Boolean {
        return webViewFragment.isSystem()
    }

    override fun maximize() {
        webViewFragment.maximize()
        super.show()
    }

    override fun minimization() {
        webViewFragment.minimization()
        super.hide()
    }

    override fun getAppId(): String? {
        return webViewFragment.getAppId()
    }

    override fun capture(): Bitmap? {
        return webViewFragment.capture()
    }

    override fun clickMenu(type: String) {
        webViewFragment.clickMenu(type)
    }

    override fun onMaximize() {
        super.show()
    }

    override fun getParent(): IMiniApp {
        return this
    }

    override fun onMinimization() {
        super.hide()
    }

    fun hideOnly() {
        webViewFragment.hideOnly()
    }

   fun hideAndDestroy() {
        webViewFragment.hideAndDestroy()
    }

    companion object {
        private val ACTION_BAR_TRANSITION_PROGRESS_VALUE = object : FloatPropertyCompat<DefaultWebViewSheet>("actionBarTransitionProgress") {
            override fun getValue(`object`: DefaultWebViewSheet): Float {
                // Logic for returning property value
                return `object`.webViewFragment.getSpringPos() * 100f
            }

            override fun setValue(`object`: DefaultWebViewSheet, value: Float) {
                // Logic for setting property value
                `object`.webViewFragment.updateSpringPos(value/100f)
            }
        }
    }
}
