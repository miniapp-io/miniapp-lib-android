

package io.miniapp.core.openplatform.common.network

import com.squareup.moshi.Moshi
import io.miniapp.core.openplatform.common.network.utils.ensureTrailingSlash
import io.miniapp.core.openplatform.common.network.utils.Lazy
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal class RetrofitFactory(private val moshi: Moshi) {

    /**
     * Use only for authentication service.
     */
    fun create(okHttpClient: OkHttpClient, baseUrl: String): Retrofit {
        return Retrofit.Builder()
                .baseUrl(baseUrl.ensureTrailingSlash())
                .client(okHttpClient)
                .addConverterFactory(UnitConverterFactory)
                .addConverterFactory(CustomConverterFactory(MoshiConverterFactory.create(moshi)))
                .build()
    }

    fun create(okHttpClient: Lazy<OkHttpClient>, baseUrl: String): Retrofit {
        return Retrofit.Builder()
                .baseUrl(baseUrl.ensureTrailingSlash())
                .callFactory { request -> okHttpClient.lazyGet().newCall(request) }
                .addConverterFactory(UnitConverterFactory)
                .addConverterFactory(CustomConverterFactory(MoshiConverterFactory.create(moshi)))
                .build()
    }
}
