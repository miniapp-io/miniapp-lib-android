package io.miniapp.core.openplatform.common.network.interceptors

import io.miniapp.core.openplatform.common.data.SessionProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

internal class SessionInterceptor(private val provider: ()-> SessionProvider?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        provider()?.also {
            if(it.isAuth()) {
                val newRequestBuilder = request.newBuilder()
                newRequestBuilder.header("Authorization", "Bearer ${it.token}")
                request = newRequestBuilder.build()
            }
        }

        return chain.proceed(request).let {
            if (it.code == 401 && !request.url.pathSegments.contains("auth")) {
                provider()?.token = null
                val newToken = runBlocking { provider()?.refreshToken?.let { it() } }
                return if (!newToken.isNullOrEmpty()) {
                    provider()?.token = newToken
                    val newRequest = request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                    chain.proceed(newRequest)
                } else {
                    it
                }
            } else {
                it
            }
        }
    }
}