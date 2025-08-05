package io.miniapp.core.openplatform.miniapp.ui.proxy
import android.app.AlertDialog
import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.miniapp.core.openplatform.miniapp.IResourcesProvider
import io.miniapp.core.openplatform.miniapp.MiniAppServiceImpl
import io.miniapp.core.openplatform.miniapp.utils.BiometrySP
import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import io.miniapp.core.openplatform.miniapp.webapp.IMiniAppDelegate
import kotlinx.coroutines.launch
import org.json.JSONObject

internal class WebAppEventProxy(private val context: Context,
                                private val resourceProvider: IResourcesProvider,
                                private val miniAppDelegate: IMiniAppDelegate,
                                private val owner: LifecycleOwner,
                                private val sp: BiometrySP?) {

    private val miniApp = miniAppDelegate.app()
    private val webApp = miniAppDelegate.webApp()

    fun release() {
        webApp?.removeObserver(owner.lifecycle)
    }

    init {
        webApp?.addObserver(owner.lifecycle)

        webApp?.subscribeMessage("web_app_invoke_custom_method") { eventData->
            owner.lifecycleScope.launch {
                eventData?.also {
                    try {

                        val beginTime = System.currentTimeMillis()

                        val reqId = eventData.getString("req_id")
                        val method = eventData.getString("method")
                        val params = eventData.optJSONObject("params")?.toString()

                        val isDeal = MiniAppServiceImpl.getInstance().appDelegate?.callCustomMethod(miniApp, method, params) {
                            val data = JSONObject()
                            data.put("req_id", reqId)
                            data.put("result", it)
                            webApp.postCustomEventToMiniApp(data)

                            LogTimber.tag("=====>custom method").d("Done! time: ${System.currentTimeMillis()-beginTime} eventData=$eventData")
                        } ?: false

                        if (!isDeal) {
                           val result = miniAppDelegate.invokeCustomMethod(method, params)
                            val data = JSONObject()
                            data.put("req_id", reqId)
                            data.put("result", result)
                            webApp.postCustomEventToMiniApp(data)

                            LogTimber.tag("=====>custom method").d("Done! time: ${System.currentTimeMillis()-beginTime} eventData=$eventData")
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@also
                    }
                }
            }
            true
        }


        webApp?.subscribeMessage("web_app_biometry_get_info") { eventData ->
            owner.lifecycleScope.launch {
                val canUseBiometryAuth = MiniAppServiceImpl.getInstance().appDelegate?.canUseBiometryAuth(miniApp) ?: false
                sp?.availableType = if(canUseBiometryAuth) "unknown" else null
                webApp.postCommonEventToMiniApp("biometry_info_received", sp?.getStatus())
            }
            true
        }

        webApp?.subscribeMessage("web_app_biometry_request_access") { eventData ->
            if (true== sp?.disabled) {
                notifyBiometryReceived()
                return@subscribeMessage true
            }

            var reason: String? = null
            try {
                reason = eventData?.getString("reason")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            if (false == sp?.access_granted) {
                val cancel = arrayOf<Runnable?>(Runnable {
                    sp.access_requested = true
                    notifyBiometryReceived()
                })
                val alert = AlertDialog.Builder(context)
                if (TextUtils.isEmpty(reason)) {
                    alert.setTitle(resourceProvider.getString("dialog_allow_biometry_title"))
                    alert.setMessage(resourceProvider.getString("dialog_allow_biometry_message").replace("**%s**", sp.cacheKey ))
                } else {
                    alert.setTitle(resourceProvider.getString("dialog_allow_biometry_message").replace("**%s**", sp.cacheKey ))
                    alert.setMessage(reason)
                }
                alert.setPositiveButton(resourceProvider.getString("action_allow")) { di, w ->
                    if (cancel[0] != null) {
                        cancel[0] = null
                    }
                    sp.access_requested = true

                    owner.lifecycleScope.launch {
                        val token = MiniAppServiceImpl.getInstance().appDelegate?.updateBiometryToken(miniApp,null, reason)
                        sp.encrypted_token = token
                        sp.access_granted = !token.isNullOrEmpty()
                        notifyBiometryReceived()
                    }
                }
                alert.setNegativeButton(resourceProvider.getString(resourceProvider.getString("action_cancel"))) { di, w ->
                    if (cancel[0] != null) {
                        cancel[0] = null
                    }
                    sp.access_requested = true
                    sp.disabled = true
                    notifyBiometryReceived()
                }
                alert.setOnDismissListener { di ->
                    if (cancel[0] != null) {
                        cancel[0]!!.run()
                        cancel[0] = null
                    }
                }
                alert.show()
            } else {
                if (false == sp?.access_requested) {
                    sp.access_requested = true
                }
                notifyBiometryReceived()
            }

            true
        }

        webApp?.subscribeMessage("web_app_biometry_request_auth") { eventData ->
            owner.lifecycleScope.launch {
                try {
                    if (false == sp?.access_granted) {
                        val auth = JSONObject()
                        auth.put("status", "failed")
                        webApp.postCommonEventToMiniApp("biometry_auth_requested", auth)
                        return@launch
                    }

                    val reason = eventData?.getString("reason")
                    val token = MiniAppServiceImpl.getInstance().appDelegate?.updateBiometryToken(miniApp,null, reason)

                    sp?.encrypted_token = token
                    sp?.access_granted = !token.isNullOrEmpty()

                    val data = JSONObject()
                    data.put("status", if(token!=null) "authorized" else "failed")
                    data.put("token", token)

                    webApp.postCommonEventToMiniApp("biometry_auth_requested", data)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
            true
        }

        webApp?.subscribeMessage("web_app_biometry_update_token") { eventData ->
            owner.lifecycleScope.launch {
                try {

                    if (false == sp?.access_granted) {
                        val auth = JSONObject()
                        auth.put("status", "failed")
                        webApp.postCommonEventToMiniApp("biometry_token_updated", auth)
                        return@launch
                    }

                    val reason = eventData?.getString("reason")
                    val token = eventData?.getString("token")
                    val tokenNew = MiniAppServiceImpl.getInstance().appDelegate?.updateBiometryToken(miniApp, token, reason)

                    sp?.encrypted_token = tokenNew
                    sp?.access_granted = !tokenNew.isNullOrEmpty()

                    val data = JSONObject()
                    data.put("status", if(tokenNew!=null) "updated" else "failed")

                    webApp.postCommonEventToMiniApp("biometry_token_updated", data)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
            true
        }

        webApp?.subscribeMessage("web_app_biometry_open_settings") { eventData ->
            MiniAppServiceImpl.getInstance().appDelegate?.openBiometrySettings(miniApp)
            true
        }
    }

    private fun notifyBiometryReceived() {
        webApp?.postCommonEventToMiniApp("biometry_info_received", sp?.getStatus())
    }
}