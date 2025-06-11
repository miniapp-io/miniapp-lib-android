

package io.miniapp.core.openplatform.common.network

import com.squareup.moshi.Moshi
import io.miniapp.core.openplatform.common.network.annotation.SerializeNulls
import io.miniapp.core.openplatform.common.network.parsing.CipherSuiteMoshiAdapter
import io.miniapp.core.openplatform.common.network.parsing.ForceToBooleanJsonAdapter
import io.miniapp.core.openplatform.common.network.parsing.TlsVersionMoshiAdapter
import io.miniapp.core.openplatform.common.network.parsing.UriMoshiAdapter

internal object MoshiProvider {

    private val builder: Moshi.Builder =  Moshi.Builder()

    private val moshi = builder
            .add(UriMoshiAdapter())
            .add(ForceToBooleanJsonAdapter())
            .add(CipherSuiteMoshiAdapter())
            .add(TlsVersionMoshiAdapter())
            .add(SerializeNulls.JSON_ADAPTER_FACTORY)
            .build()

    fun providesMoshi(): Moshi =  moshi

    inline fun <reified T> fromJson(json: String): T? {
        return try {
            moshi.adapter(T::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    inline fun <reified T> toJson(value: T): String {
        return moshi.adapter(T::class.java).toJson(value)
    }
}
