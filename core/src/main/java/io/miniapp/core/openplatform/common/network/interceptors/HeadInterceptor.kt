package io.miniapp.core.openplatform.common.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response

internal class HeadInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val newRequestBuilder = request.newBuilder()
        newRequestBuilder.header("Content-Type", "application/json")
        request = newRequestBuilder.build()

        return chain.proceed(request)
    }
}