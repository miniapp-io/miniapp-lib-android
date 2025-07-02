package io.miniapp.core.openplatform.miniapp.ui

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import com.hjq.window1.EasyWindow
import com.hjq.window1.draggable.SpringBackDraggable
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.IMiniApp
import io.miniapp.core.openplatform.miniapp.ui.views.RoundCornerConstraintLayout
import io.miniapp.core.openplatform.miniapp.ui.webview.DefaultAppWebView
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import java.lang.ref.WeakReference

internal object FloatingWindowManager {

    private var miniApp: IMiniApp? = null
    private var webAppId: Int = -1
    private var webViewRef: WeakReference<WebView> = WeakReference(null)

    // Hide and destroy floating window
    private fun hideFloatingWindow() {
        EasyWindow.recycleAll()
    }


    private fun removeWebView() {
        webViewRef.get()?.also { webView->
            if (webView.parent != null) {
                (webView.parent as? ViewGroup)?.removeView(webView)
            }
        }
    }

    fun closeAfterConfirm() {
        webViewRef.get()?.also {
            removeWebView()
        }
        miniApp = null
        webAppId = -1
        webViewRef = WeakReference(null)
        hideFloatingWindow()
    }

    fun closeFloatingWindow(force: Boolean = false, immediately: Boolean = true) {
       if(miniApp?.requestDismiss(force = force, immediately = immediately, isSilent = true) ?: force) {
           closeAfterConfirm()
       }
    }

    fun showFloatingWindow(activity: Activity,
                           miniApp: IMiniApp,
                           webView: DefaultAppWebView?,
                           iconUrl: String?,
                           width: Int?,
                           height: Int?) {

        if (miniApp== FloatingWindowManager.miniApp) {
            return
        }

        closeFloatingWindow(force = true, immediately = true)

        FloatingWindowManager.miniApp = miniApp
        webAppId = webView?.webAppId ?: -1
        webViewRef = WeakReference(webView)

        val springBackDraggable = SpringBackDraggable(SpringBackDraggable.ORIENTATION_HORIZONTAL).apply {
            isAllowMoveToScreenNotch = false
        }

        removeWebView()

        EasyWindow.with(activity)
            .setContentView(R.layout.layout_window_message)
            .setAnimStyle(R.style.FloatWindowAnimStyle)
            .setDraggable(springBackDraggable)
            .setGravity(Gravity.END or Gravity.BOTTOM)
            .setYOffset(AndroidUtils.dp(120))
            .apply {
                if (width != null && height != null) {
                    (findViewById<RoundCornerConstraintLayout>(R.id.floatContainer) as RoundCornerConstraintLayout).also {
                        val layoutParams =  it.layoutParams
                        layoutParams.width = AndroidUtils.dp(width)
                        layoutParams.height = AndroidUtils.dp(height)
                        it.layoutParams = layoutParams
                    }

                    (findViewById<FrameLayout>(R.id.webContainer) as FrameLayout).also {
                        val layoutParams =  it.layoutParams
                        layoutParams.width = AndroidUtils.dp(width) * 4
                        layoutParams.height = AndroidUtils.dp(height) * 4
                        it.layoutParams = layoutParams
                    }
                }

                (findViewById<ImageView>(R.id.imageView) as ImageView).also {

                }
            }
            .apply {
                webView?.also {
                    (findViewById<FrameLayout>(R.id.webContainer) as FrameLayout).also {
                        // Set the pivot point for scaling to the top left corner
                        it.pivotX = 0f
                        it.pivotY = 0f
                        it.addView(webView)
                    }
                }
            }
            .setOnClickListener(R.id.closeButton) { _, _ ->
                closeFloatingWindow(force = true, immediately = false)
            }
            .setOnClickListener(R.id.clickMarkView) { _, _ ->
                maximize()
            }
            .show()
    }

    fun isAppOnMinimization(appId: Int): Boolean {
        return webAppId != -1 &&  appId == webAppId
    }

    fun isAppOnMinimization(app: IMiniApp?): Boolean {
        return app != null &&  app == miniApp
    }

    fun maximize(): Boolean {
        return miniApp?.let {
            removeWebView()
            webViewRef.get()?.also { webView->
                webView.onResume()
            }
            miniApp?.maximize()
            miniApp = null
            webAppId = -1
            webViewRef = WeakReference(null)
            hideFloatingWindow()
            true
        } ?: false
    }
}