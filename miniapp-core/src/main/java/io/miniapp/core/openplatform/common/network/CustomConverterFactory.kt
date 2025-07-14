package io.miniapp.core.openplatform.common.network

import io.miniapp.core.openplatform.common.network.error.MiniAppXError
import io.miniapp.core.openplatform.common.network.error.toFailure
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

internal  class CustomConverterFactory(private val proxyFactory: Converter.Factory) : Converter.Factory() {

    override fun toString(): String {
        return proxyFactory.toString()
    }

    override fun hashCode(): Int {
        return proxyFactory.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return proxyFactory == other
    }

    override fun stringConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, String>? {
        return proxyFactory.stringConverter(type, annotations, retrofit)
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        return proxyFactory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {

        return proxyFactory.responseBodyConverter(type, annotations, retrofit)?.let { converter ->
            Converter<ResponseBody, Any> { value ->
                val buffer = Buffer()
                buffer.writeAll(value.source())
                val bodyValue = buffer.readUtf8()
                try {
                    val newResponseBody = bodyValue.toResponseBody(value.contentType())
                    converter.convert(newResponseBody)
                } catch (e: Exception) {
                    throw bodyValue.toFailure<MiniAppXError>() ?: e
                }
            }
        }
    }
}