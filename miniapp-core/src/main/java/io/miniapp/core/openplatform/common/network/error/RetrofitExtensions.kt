
package io.miniapp.core.openplatform.common.network.error

import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ResponseBody
import io.miniapp.core.openplatform.common.network.MoshiProvider
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun okhttp3.Call.awaitResponse(): okhttp3.Response {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }

        enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }
}

/**
 * Convert a retrofit Response to a Failure, and eventually parse errorBody to convert it to a [MiniAppXError].
 */
fun <T> Response<T>.toFailure(): MiniAppXError {
    return toFailure(errorBody(), code(), message())
}

fun <T> String.toFailure(): MiniAppXError? {
    val errorBodyStr = this

    try {
        val miniAppXErrorAdapter = MoshiProvider.providesMoshi().adapter(MiniAppXError::class.java)
        return  miniAppXErrorAdapter.fromJson(errorBodyStr)
    } catch (ex: Exception) {
        // This is not a MiniAppXError
        ex.printStackTrace()
    } catch (ex: JsonEncodingException) {
        // This is not a MiniAppXError, HTML code?
        ex.printStackTrace()
    }

    return null
}

private fun toFailure(errorBody: ResponseBody?, httpCode: Int, message: String?): MiniAppXError {
    if (errorBody == null) {
        return MiniAppXError(httpCode, message)
    }

    try {
        val errorBodyStr = errorBody.string()
        val miniAppXErrorAdapter = MoshiProvider.providesMoshi().adapter(MiniAppXError::class.java)
        return  miniAppXErrorAdapter.fromJson(errorBodyStr) ?: MiniAppXError(httpCode, message)
    } catch (ex: Exception) {
        // This is not a MiniAppXError
        ex.printStackTrace()
    } catch (ex: JsonEncodingException) {
        // This is not a MiniAppXError, HTML code?
        ex.printStackTrace()
    }

    return MiniAppXError(httpCode, message)
}
