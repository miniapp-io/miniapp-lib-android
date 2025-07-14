package io.miniapp.core.openplatform.common.network

import io.miniapp.core.openplatform.common.data.SessionProvider
import io.miniapp.core.openplatform.common.network.interceptors.CurlLoggingInterceptor
import io.miniapp.core.openplatform.common.network.interceptors.FormattedJsonHttpLogger
import io.miniapp.core.openplatform.common.network.interceptors.HeadInterceptor
import io.miniapp.core.openplatform.common.network.interceptors.SessionInterceptor
import io.miniapp.core.openplatform.common.network.ssl.CertUtil
import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import okhttp3.ConnectionSpec
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.Collections
import java.util.concurrent.TimeUnit

internal object OkHttpClientProvider {

    var isDev: Boolean = false
    var apiHost: String? = null

    private fun getBaseUrl(): String {
        return apiHost!!
    }

    val retrofitFactory: (() -> SessionProvider?) -> Retrofit = {
        RetrofitFactory(providesMoshi()).create(
            providesOkHttpClient(it),
            baseUrl = getBaseUrl()
        )
    }

     private fun OkHttpClient.Builder.addSocketFactory(): OkHttpClient.Builder {
        try {
            val pair = CertUtil.newPinnedSSLSocketFactory()
            sslSocketFactory(pair.sslSocketFactory, pair.x509TrustManager)
        } catch (e: Exception) {
            LogTimber.e(e, "addSocketFactory failed")
        }

        return this
    }

    private fun providesHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = FormattedJsonHttpLogger(HttpLoggingInterceptor.Level.BODY)
        val interceptor = HttpLoggingInterceptor(logger)
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return interceptor
    }

   private fun providesOkHttpClient(provider: ()-> SessionProvider?) : OkHttpClient {
        val spec = ConnectionSpec.Builder(ConnectionSpec.RESTRICTED_TLS).build()
        val dispatcher = Dispatcher().apply {
            maxRequestsPerHost = 20
        }
        return OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HeadInterceptor())
            .addInterceptor(SessionInterceptor(provider))
            .addInterceptor(CurlLoggingInterceptor())
            .addNetworkInterceptor(providesHttpLoggingInterceptor())
            .addSocketFactory()
            .dispatcher(dispatcher)
            .connectionSpecs(Collections.singletonList(spec))
            .build()
    }

    private fun providesMoshi() = MoshiProvider.providesMoshi()
}