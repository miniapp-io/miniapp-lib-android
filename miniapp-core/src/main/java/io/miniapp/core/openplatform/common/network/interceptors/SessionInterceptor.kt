package io.miniapp.core.openplatform.common.network.interceptors

import io.miniapp.core.openplatform.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class SessionInterceptor : Interceptor {
    private var isRefreshing = false
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isAuthRequest = request.url.pathSegments.contains("auth")

        val token = getTokenSafely()

        if (token != null) {
            val newRequest = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            return chain.proceed(newRequest).let { resp ->
                handleResponse(resp, request, chain)
            }
        } else if (!isAuthRequest) {
            return waitForTokenAndRetry(request, chain)
        }

        return chain.proceed(request)
    }

    private fun getTokenSafely(): String? {
        return if (AuthManager.isAuth()) {
            AuthManager.getToken()
        } else {
            null
        }
    }

    private fun waitForTokenAndRetry(request: Request, chain: Interceptor.Chain): Response {
        lock.withLock {
            if (!isRefreshing) {
                isRefreshing = true
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AuthManager.refreshToken()
                        lock.withLock {
                            isRefreshing = false
                            condition.signalAll()
                        }
                    } catch (e: Exception) {
                        lock.withLock {
                            isRefreshing = false
                            condition.signalAll()
                        }
                    }
                }
            }

            try {
                condition.await(30, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                return chain.proceed(request)
            }
        }

        val newRequest = request.newBuilder()
            .header("Authorization", "Bearer ${AuthManager.getToken()}")
            .build()

        return chain.proceed(newRequest)
    }

    private fun handleResponse(response: Response, originalRequest: Request, chain: Interceptor.Chain): Response {
        if (response.code == 401 && !originalRequest.url.pathSegments.contains("auth")) {
            response.close()
            AuthManager.clearToken()
            return waitForTokenAndRetry(originalRequest, chain)
        }
        return response
    }
}