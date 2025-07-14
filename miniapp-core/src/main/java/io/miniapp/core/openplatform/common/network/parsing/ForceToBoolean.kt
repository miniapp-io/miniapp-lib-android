

package io.miniapp.core.openplatform.common.network.parsing

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import io.miniapp.core.openplatform.miniapp.utils.LogTimber

@JsonQualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
internal annotation class ForceToBoolean

internal class ForceToBooleanJsonAdapter {
    @ToJson
    fun toJson(@ForceToBoolean b: Boolean): Boolean {
        return b
    }

    @FromJson
    @ForceToBoolean
    fun fromJson(reader: JsonReader): Boolean {
        return when (val token = reader.peek()) {
            JsonReader.Token.NUMBER -> reader.nextInt() != 0
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            else -> {
                LogTimber.e("Expecting a boolean or a int but get: $token")
                reader.skipValue()
                false
            }
        }
    }
}
