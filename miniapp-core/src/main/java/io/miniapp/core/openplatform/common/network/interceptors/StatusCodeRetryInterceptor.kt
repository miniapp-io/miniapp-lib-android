package io.miniapp.core.openplatform.common.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CancellationException

internal class StatusCodeRetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1000,
    private val retryableStatusCodes: Set<Int> = setOf(408, 429, 500, 502, 503, 504)
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var retryCount = 0
        var response: Response? = null

        while (retryCount <= maxRetries) {
            try {
                response?.close()
                response = chain.proceed(chain.request())

                if (response.isSuccessful || !shouldRetry(response.code, retryCount)) {
                    return response
                }

                retryCount++
                delay(retryDelayMs * retryCount)

            } catch (e: Exception) {
                response?.close()

                if (retryCount >= maxRetries || !RetryInterceptor.isRetryableException(e)) {
                    throw e
                }

                if (isCancellationException(e)) {
                    throw e
                }

                retryCount++
                delay(retryDelayMs * retryCount)
            }
        }

        return response ?: throw IOException("Request failed after $maxRetries retries")
    }

    private fun isCancellationException(throwable: Throwable): Boolean {
        return when (throwable) {
            is CancellationException -> true
            is IOException -> throwable.message?.contains("canceled", ignoreCase = true) == true ||
                    throwable.message?.contains("interrupted", ignoreCase = true) == true
            else -> false
        }
    }

    private fun shouldRetry(statusCode: Int, retryCount: Int): Boolean {
        return retryCount < maxRetries && statusCode in retryableStatusCodes
    }

    private fun delay(ms: Long) {
        if (ms > 0) {
            Thread.sleep(ms)
        }
    }
}