package io.miniapp.core.openplatform.miniapp.webapp
import androidx.lifecycle.Lifecycle
import io.miniapp.core.openplatform.miniapp.IMiniApp
import org.json.JSONArray
import org.json.JSONObject

interface IWebApp {
    fun postCommonEventToMiniApp(eventType: String, eventData: JSONObject?)
    fun postCustomEventToMiniApp(eventData: JSONObject?)
    fun addObserver(lifecycle: Lifecycle)
    fun removeObserver(lifecycle: Lifecycle)
    fun subscribeMessage(event:String, subscriber: WebEventSubscriber<JSONObject?>)
    fun unsubscribeMessage(event:String)
    fun addEvenHandler(handler:  IWebAppEventHandler?)
    fun removeEvenHandler()
    fun destroy()
}

interface IWebAppEventHandler {
    fun handleMessage(eventType:String, eventData: JSONObject?): Boolean

    fun handleWebScrollMessage(eventType:String, eventData: JSONArray?): Boolean

    fun onWellKnown(isTelegram: Boolean)
}

fun interface WebEventSubscriber<DATA>  {
    fun handleMessage(message: DATA?): Boolean
}

interface WebEventPublisher<EVENT,DATA> {
    fun subscribe(event:String, webEventSubscriber: WebEventSubscriber<DATA?>)
    fun unsubscribe(event:String)
    fun unsubscribeAll()
    fun notifySubscribers(event: EVENT, message: DATA?): Boolean
}

internal interface IMiniAppDelegate {
    fun webApp() : IWebApp?
    fun app() : IMiniApp
    fun allowThisScroll(x: Boolean, y: Boolean)
    fun setPageReady()
    fun hideAndDestroy(immediately: Boolean = false, isSilent: Boolean = false, complete: (() -> Unit)? = null): Boolean
    fun dismissImmediately(isSilent: Boolean = false, complete: (() -> Unit) ? = null)
    fun expandView(isFromWeb: Boolean = false)
    fun setActionBarColor(color: Int, isOverrideColor: Boolean)
    fun setSettingsButtonVisible(visible:Boolean)
    fun requestQrCodeScan(subTitle: String?)
    fun closeQrCodeScan()
    fun setContainerColor(color: Int)
    fun invalidateViewPortHeight(force:Boolean, fromWebApp: Boolean = false)
    fun requestTheme()
    fun closePopUp(btnId:String)
    fun sendWebViewData(content:String?)
    fun setupClosingBehavior(enable:Boolean)
    fun requestSendMessageAccess()
    fun requestPhone()
    fun setupExpandBehavior(enable:Boolean)
    fun isClipboardAvailable(): Boolean
    fun clipboardData(reqId:String, content:String)
    fun setupMainButton(isVisible: Boolean, isActive: Boolean, text: String?, color: Int, textColor: Int, isProgressVisible: Boolean)
    fun setupFullScreen(enable: Boolean)
    fun setupShowHead(visible: Boolean, isUserAction: Boolean=true)
    fun setupBackButton(visible: Boolean)
    fun openUrl(url: String, isInternal: Boolean)
    fun addHomeScreenShortcut()
    fun checkScreenShortcut()
    fun requestSafeArea()
    fun requestContentSafeArea()
    suspend fun invokeCustomMethod(method: String, params: String?): String?
}

internal interface IMiniAppListener {
    fun getParent(): IMiniApp
    fun onMinimization()
    fun onMaximize()
    fun onSpringProcess(progress: Float)
    fun dismissImmediately(isSilent:Boolean =false, complete: (() -> Unit)? = null)
    fun onUpdateLightStatusBar(enable: Boolean)
}