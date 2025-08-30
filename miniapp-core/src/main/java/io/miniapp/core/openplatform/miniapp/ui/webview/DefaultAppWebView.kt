package io.miniapp.core.openplatform.miniapp.ui.webview

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.miniapp.core.R
import io.miniapp.core.openplatform.common.apis.data.AppSettings
import io.miniapp.core.openplatform.miniapp.ActivityStack
import io.miniapp.core.openplatform.miniapp.IMiniApp
import io.miniapp.core.openplatform.miniapp.events.WebViewScrollListener
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.utils.UiThreadUtil
import io.miniapp.core.openplatform.miniapp.utils.UiThreadUtil.runOnUiThread
import io.miniapp.core.openplatform.miniapp.utils.WebViewResourceHelper
import io.miniapp.core.openplatform.miniapp.webapp.IWebApp
import io.miniapp.core.openplatform.miniapp.webapp.IWebAppEventHandler
import org.json.JSONArray
import org.json.JSONObject


@SuppressLint("ViewConstructor")
internal class DefaultAppWebView(context: Context) : WebView(context), IWebAppEventHandler {
    companion object {
        var _webAppId = 0
    }

    var webAppId = ++_webAppId
    var isPageLoaded: Boolean = false
    var cacheData: String? = null
    var miniApp: IMiniApp? = null
    var owner: LifecycleOwner? = null
    var webApp: IWebApp? = null

    var headColor:Int? = null
    var bgColor:Int? = null
    var isBackButtonVisible:Boolean? = null
    var isCloseConfirm: Boolean? = null
    var isExpanded: Boolean? = null
    var allowExpand: Boolean? = null
    var isSettingVisible: Boolean? = null
    var showFullscreen: Boolean? = null
    var appSettings: AppSettings? = null
    
    // Expiration time property, used to control the expiration state of mini apps
    var expirationTime: Long? = null
    val isExpired: Boolean
        get() = expirationTime?.let { System.currentTimeMillis() > it } ?: false

    var metadata: JSONObject? = null

    val pageImage: String?
        get() {
            return try {
                metadata?.getString("image")
            } catch (e: Throwable) {
                null
            }
        }

    val pageTitle: String?
        get() {
            return try {
                metadata?.getString("title")
            } catch (e: Throwable) {
                null
            }
        }

    val pageDescription: String?
        get() {
            return try {
                metadata?.getString("description")
            } catch (e: Throwable) {
                null
            }
        }

    val pageParams: String?
        get() {
            return try {
                metadata?.getString("params")
            } catch (e: Throwable) {
                null
            }
        }

    var pageIcon: String? = null

    var injectedJS: Boolean = false

    private var prevScrollX = 0
    private var prevScrollY = 0
    private var lastClickMs: Long = 0
    private var _isParentDismiss = false

    private var _webViewScrollListener: WebViewScrollListener? = null
    private var dismissHandler: ((() -> Unit) -> Unit)? = null

    var webEventHandler: IWebAppEventHandler? = null

    init {
        WebViewResourceHelper.addChromeResourceIfNeeded(context)
        // Set expiration time to 1 hour from now
        expirationTime = System.currentTimeMillis() + (60 * 60 * 1000)
    }

    private data class SelectItem(
        val text: String,
        val value: String,
        val isGroup: Boolean = false,
        val isDisabled: Boolean = false,
        val indent: Boolean = false
    )

    @JavascriptInterface
    fun showCustomSelect(selectId: String, currentSelectedValue: String, optionsJson: String) {
        runOnUiThread {
            try {
                val options = JSONArray(optionsJson)
                val items = ArrayList<SelectItem>()
                var initialSelectedPosition = -1
                var currentIndex = 0

                fun processOptions(optionsArray: JSONArray) {
                    for (i in 0 until optionsArray.length()) {
                        val option = optionsArray.getJSONObject(i)
                        when (option.getString("type")) {
                            "group" -> {
                                items.add(SelectItem(
                                    text = option.getString("label"),
                                    value = "",
                                    isGroup = true,
                                    isDisabled = true
                                ))

                                currentIndex++

                                val groupOptions = option.getJSONArray("options")
                                for (j in 0 until groupOptions.length()) {
                                    val groupOption = groupOptions.getJSONObject(j)
                                    items.add(SelectItem(
                                        text = groupOption.getString("text"),
                                        value = groupOption.getString("value"),
                                        isGroup = false,
                                        isDisabled = groupOption.optBoolean("disabled", false),
                                        indent = true
                                    ))

                                    if (groupOption.getString("value") == currentSelectedValue) {
                                        initialSelectedPosition = currentIndex
                                    }
                                    currentIndex++
                                }
                            }
                            "option" -> {
                                items.add(SelectItem(
                                    text = option.getString("text"),
                                    value = option.getString("value"),
                                    isGroup = false,
                                    isDisabled = option.optBoolean("disabled", false)
                                ))

                                if (option.getString("value") == currentSelectedValue) {
                                    initialSelectedPosition = currentIndex
                                }
                                currentIndex++
                            }
                        }
                    }
                }

                processOptions(options)

                val activity = ActivityStack.getCurrentActivity()!!

                val adapter = object : ArrayAdapter<SelectItem>(
                    activity,
                    R.layout.item_select_option,
                    R.id.text_option,
                    items
                ) {
                    private var selectedPosition = initialSelectedPosition

                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent)
                        val item = items[position]

                        // Set text and style
                        val textView = view.findViewById<TextView>(R.id.text_option)
                        textView.text = item.text

                        // Handle indentation
                        val params = textView.layoutParams as MarginLayoutParams
                        params.marginStart = if (item.indent) AndroidUtils.dp(16) else 0
                        textView.layoutParams = params

                        // Set group title style
                        if (item.isGroup) {
                            textView.setTypeface(null, Typeface.BOLD)
                        } else {
                            textView.setTypeface(null, Typeface.NORMAL)
                        }

                        // Set checkmark icon for selected item
                        val checkmark = view.findViewById<ImageView>(R.id.checkmark)
                        checkmark.visibility = if (position == selectedPosition && !item.isGroup) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                        return view
                    }

                    fun setSelectedPosition(position: Int) {
                        selectedPosition = position
                        notifyDataSetChanged()
                    }
                }

                val dialog = Dialog(activity)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.dialog_custom_select)

                val listView = dialog.findViewById<ListView>(R.id.listView)
                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    val item = items[position]
                    if (!item.isGroup && !item.isDisabled) {
                        adapter.setSelectedPosition(position)

                        val selectedValue = item.value
                        val updateJs = """
                        (function() {
                            var select = document.getElementById('$selectId');
                            if(select) {
                                select.value = "$selectedValue";
                                var event = new Event('change', { bubbles: true });
                                select.dispatchEvent(event);
                            }
                        })();
                    """.trimIndent()

                        evaluateJavascript(updateJs, null)
                        dialog.dismiss()
                    }
                }

                dialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun postEvent(eventType: String, eventData: String?) {
        UiThreadUtil.runOnUiThread {
            try {
                JSONArray(eventData ?: "").also {
                    handleWebScrollMessage(eventType, it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun post(eventType: String, eventData: String?) {
        UiThreadUtil.runOnUiThread {
            try {
                JSONArray(eventData ?: "").also {
                    handleWebScrollMessage(eventType, it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun resolveShare(json: String?, file: ByteArray?, fileName: String?, fileMimeType: String?) {
        // TODO
    }

    fun dismiss(complete :() -> Unit) {
        dismissHandler?.invoke(complete)
        dismissHandler = null
        setDismissFlag()
    }

    fun isTopLevel() = webAppId == _webAppId

    fun refreshAppId() {
        webAppId = ++_webAppId
    }

    fun setDismissFlag(){
        webEventHandler = null
        _isParentDismiss = true
        owner = null
        miniApp = null
    }

    fun isParentDismiss() = _isParentDismiss

    override fun hasOverlappingRendering(): Boolean {
        return false
    }

    fun setWebViewScrollListener(webViewScrollListener: WebViewScrollListener?) {
        this._webViewScrollListener = webViewScrollListener
    }

    fun setDismissHandler(handle: ((() -> Unit) -> Unit)?) {
        _isParentDismiss = false
        dismissHandler = handle
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        _webViewScrollListener?.onWebViewScrolled(
            this,
            scrollX - prevScrollX,
            scrollY - prevScrollY
        )
        prevScrollX = scrollX
        prevScrollY = scrollY
    }

    override fun setScrollX(value: Int) {
        super.setScrollX(value)
        prevScrollX = value
    }

    override fun setScrollY(value: Int) {
        super.setScrollY(value)
        prevScrollY = value
    }

    override fun onCheckIsTextEditor(): Boolean {
        return isFocusable
    }

    override fun reload() {
        CookieManager.getInstance().flush()
        super.reload()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(heightMeasureSpec),
                MeasureSpec.EXACTLY
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            lastClickMs = System.currentTimeMillis()
        }
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        AndroidUtils.checkAndroidTheme(context, true)
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        AndroidUtils.checkAndroidTheme(context, false)
        super.onDetachedFromWindow()
    }

    override fun handleMessage(eventType: String, eventData: JSONObject?): Boolean {
       return webEventHandler?.handleMessage(eventType, eventData) ?: false
    }

    override fun handleWebScrollMessage(eventType: String, eventData: JSONArray?): Boolean {
       return webEventHandler?.handleWebScrollMessage(eventType, eventData) ?: false
    }

    override fun onWellKnown(isTelegram: Boolean) {
        webEventHandler?.onWellKnown(isTelegram)
    }

    fun releaseRef(pause: Boolean) {
        if (pause) {
            onPause()
        }
    }

    fun clearAfterDismiss() {
        // Make sure you remove the WebView from its parent view before doing anything.
        webChromeClient = null
        webViewClient = WebViewClient()
        webApp?.destroy()
        webApp = null
        stopLoading()
        onPause()
        clearHistory()
        // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
        loadUrl("about:blank")
        removeAllViews()
        destroy()
    }

    fun goToHomePage() {
        val backList = copyBackForwardList()
        if (backList.currentIndex > 0) {
            goBackOrForward(-backList.currentIndex)
        }
    }
}