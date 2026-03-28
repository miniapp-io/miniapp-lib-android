package io.miniapp.core.openplatform.common.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException

internal class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1000,
    private val shouldRetry: (Throwable) -> Boolean = { isRetryableException(it) }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var retryCount = 0
        var lastException: Throwable? = null

        while (retryCount <= maxRetries) {
            try {
                val response = chain.proceed(chain.request())

                if (!response.isSuccessful && retryCount < maxRetries) {
                    response.close()
                    retryCount++
                    delay(retryDelayMs * retryCount)
                    continue
                }

                return response

            } catch (e: Exception) {
                lastException = e

                if (isCancellationException(e)) {
                    throw e
                }

                if (retryCount >= maxRetries || !shouldRetry(e)) {
                    break
                }

                retryCount++
                delay(retryDelayMs * retryCount)
            }
        }

        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }

    private fun isCancellationException(throwable: Throwable): Boolean {
        return when (throwable) {
            is CancellationException -> true
            is IOException -> throwable.message?.contains("canceled", ignoreCase = true) == true ||
                    throwable.message?.contains("interrupted", ignoreCase = true) == true
            else -> false
        }
    }

    private fun delay(ms: Long) {
        if (ms > 0) {
            Thread.sleep(ms)
        }
    }

    companion object {
        fun isRetryableException(throwable: Throwable): Boolean {
            return when (throwable) {
                is SocketTimeoutException -> true
                is UnknownHostException -> true
                is IOException -> true
                else -> false
            }
        }
    }
}