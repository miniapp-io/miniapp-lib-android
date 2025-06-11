

package io.miniapp.core.openplatform.common.network.interceptors

import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class FormattedJsonHttpLogger(
        private val level: HttpLoggingInterceptor.Level
) : HttpLoggingInterceptor.Logger {

    companion object {
        private const val INDENT_SPACE = 2
    }

    /**
     * Log the message and try to log it again as a JSON formatted string.
     * Note: it can consume a lot of memory but it is only in DEBUG mode.
     *
     * @param message
     */
    @Synchronized
    override fun log(message: String) {
        LogTimber.v(message)

        // Try to log formatted Json only if there is a chance that [message] contains Json.
        // It can be only the case if we log the bodies of Http requests.
        if (level != HttpLoggingInterceptor.Level.BODY) return

        if (message.startsWith("{")) {
            // JSON Detected
            try {
                val o = JSONObject(message)
                logJson(o.toString(INDENT_SPACE))
            } catch (e: JSONException) {
                // Finally this is not a JSON string...
                LogTimber.e(e)
            }
        } else if (message.startsWith("[")) {
            // JSON Array detected
            try {
                val o = JSONArray(message)
                logJson(o.toString(INDENT_SPACE))
            } catch (e: JSONException) {
                // Finally not JSON...
                LogTimber.e(e)
            }
        }
        // Else not a json string to log
    }

    private fun logJson(formattedJson: String) {
        formattedJson
                .lines()
                .dropLastWhile { it.isEmpty() }
                .forEach { LogTimber.v(it) }
    }
}
