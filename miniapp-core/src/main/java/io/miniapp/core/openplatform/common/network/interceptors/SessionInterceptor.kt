package io.miniapp.core.openplatform.common.network.interceptors

import io.miniapp.core.openplatform.AuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

internal class SessionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if(AuthManager.isAuth()) {
            val newRequestBuilder = request.newBuilder()
            newRequestBuilder.header("Authorization", "Bearer ${AuthManager.getToken()}")
            request = newRequestBuilder.build()
        }

        return chain.proceed(request).let { resp->
            if (resp.code == 401 && !request.url.pathSegments.contains("auth")) {
                AuthManager.clearToken()
                val newToken = runBlocking { AuthManager._refreshToken?.let { it() } }
                return if (!newToken.isNullOrEmpty()) {
                    val newRequest = request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                    chain.proceed(newRequest)
                } else {
                    resp
                }
            } else {
                resp
            }
        }
    }
}