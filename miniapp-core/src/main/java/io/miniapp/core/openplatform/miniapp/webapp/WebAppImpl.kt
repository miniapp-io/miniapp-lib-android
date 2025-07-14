package io.miniapp.core.openplatform.miniapp.webapp

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import io.miniapp.core.openplatform.miniapp.utils.UiThreadUtil
import org.json.JSONObject

internal class WebAppImpl(
    webView: WebView,
    webAppName: String) : IWebApp, DefaultLifecycleObserver {

    companion object {
        const val TAG = "WebAppImpl"
        const val TELEGRAM = "Telegram"
        const val VERSION = "1.0.0"
    }

    private var telegramProvider: WebBridgetProvider
    private var defaultProvider: WebBridgetProvider

   private var eventHandler: IWebAppEventHandler? = null

    init {
        telegramProvider = WebBridgetProvider(webView, TELEGRAM, ::postEvent)
        defaultProvider = WebBridgetProvider(webView, webAppName, ::postEvent)
    }

    private val eventPublisher by lazy {
        WebAppEventWebEventPublisher()
    }

    override fun destroy() {
        eventHandler = null
        telegramProvider.release()
        defaultProvider.release()
    }

    override fun postCommonEventToMiniApp(eventType: String, eventData: JSONObject?) {
        notifyEvent(eventType, eventData)
    }

    override fun postCustomEventToMiniApp(eventData: JSONObject?) {
        notifyEvent("custom_method_invoked", eventData)
    }

    override fun addEvenHandler(handler: IWebAppEventHandler?) {
        eventHandler = handler
    }

    override fun removeEvenHandler() {
        eventHandler = null
    }

    override fun addObserver(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    override fun removeObserver(lifecycle: Lifecycle) {
        lifecycle.removeObserver(this)
    }

    override fun subscribeMessage(event: String, subscriber: WebEventSubscriber<JSONObject?>) {
        eventPublisher.subscribe(event, subscriber)
    }

    override fun unsubscribeMessage(event: String) {
        eventPublisher.unsubscribe(event)
    }

    private fun notifyEvent(event: String, eventData: JSONObject?) {
        telegramProvider.checkWebApp(false) {
            if (telegramProvider.isAppKnown()) {
                telegramProvider.notifyEvent(event, eventData)
            } else {
                defaultProvider.notifyEvent(event, eventData)
            }
        }
    }

    private fun postEvent(eventType: String, eventData: String?) {
        LogTimber.tag(TAG).d("eventType: $eventType, eventData: $eventData ")
        var jsonEvenData: JSONObject? = null
        if (eventData != "undefined") {
            try {
                jsonEvenData = JSONObject(eventData ?: "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dispatchMessage(eventType, jsonEvenData)
    }

    private fun dispatchMessage(eventType: String, eventData: JSONObject?) {

        // The event has already been consumed by a subscriber
        if(eventPublisher.notifySubscribers(eventType, eventData)){
            return
        }

        // Default event handling
        if(true==eventHandler?.handleMessage(eventType, eventData)){
            return
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        eventPublisher.unsubscribeAll()
    }

    internal class WebBridgetProvider(private val webView: WebView,
                             private val provider: String,
                             private val eventDispatcher: (String, String?) -> Unit ) {

        private var isWebAppKnown: Boolean = false
        private var checkWebApp = false

        fun isAppKnown() = isWebAppKnown

        fun checkWebApp(force: Boolean, callback: (Boolean)-> Unit) {
            if(checkWebApp && !force) {
                callback.invoke(isWebAppKnown)
                return
            }
            UiThreadUtil.runOnUiThread {
                webView.evaluateJavascript("(typeof window.${provider} !== 'undefined')") { result ->
                    isWebAppKnown = result?.toBoolean() ?: false
                    if (isWebAppKnown || !force) {
                        checkWebApp = true
                    }
                    callback.invoke(isWebAppKnown)
                }
            }
        }

        init {
            webView.addJavascriptInterface( this, "${provider}WebviewProxy")
            LogTimber.tag(TAG).d("addJavascriptInterface: ${provider}WebviewProxy")
        }

        fun release() {
            webView.removeJavascriptInterface("${provider}WebviewProxy")
            LogTimber.tag(TAG).d("removeJavascriptInterface: ${provider}WebviewProxy")
        }

        fun notifyEvent(event: String, eventData: JSONObject?) {
            checkWebApp(true) {
                if (it) {
                    evaluateJs("window.$provider.WebView.receiveEvent('$event', $eventData)")
                }
            }
        }

        private fun evaluateJs(script: String) {
            UiThreadUtil.runOnUiThread {
                try {
                    webView.evaluateJavascript(script) {
                        LogTimber.tag(TAG).d("evaluateJs  $script success!")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JavascriptInterface
        fun sayHello() {
            LogTimber.tag(TAG).d("Hello! Welcome to MiniAppX Android App. ")
            notifyEvent( "webview:notify", JSONObject().put("msg", "Welcome to Android MiniAppX MiniApp!" ))
        }

        @JavascriptInterface
        fun postEvent(eventType: String, eventData: String?) {
            isWebAppKnown = true
            checkWebApp = true
            UiThreadUtil.runOnUiThread {
                eventDispatcher.invoke(eventType, eventData)
            }
        }
    }

    // Implementation of publisher class
    internal class WebAppEventWebEventPublisher : WebEventPublisher<String, JSONObject> {
        private val subscribers: MutableMap<String, WebEventSubscriber<JSONObject?>> = mutableMapOf()
        override fun subscribe(event:String, webEventSubscriber: WebEventSubscriber<JSONObject?>) {
            subscribers[event] = webEventSubscriber
        }

        override fun unsubscribe(event:String) {
            subscribers.remove(event)
        }

        override fun unsubscribeAll() {
            subscribers.clear()
        }

        override fun notifySubscribers(event:String, message: JSONObject?): Boolean {
            return subscribers[event]?.handleMessage(message) ?: false
        }
    }
}
