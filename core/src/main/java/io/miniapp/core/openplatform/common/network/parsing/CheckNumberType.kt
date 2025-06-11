

package io.miniapp.core.openplatform.common.network.parsing

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.io.IOException
import java.lang.reflect.Type
import java.math.BigDecimal
import kotlin.math.ceil
import kotlin.math.floor

/**
 * This is used to check if NUMBER in json is integer or double, so we can preserve typing when serializing/deserializing in a row.
 */
internal interface CheckNumberType {

    companion object {
        val JSON_ADAPTER_FACTORY = object : JsonAdapter.Factory {
            override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
                if (type !== Any::class.java) {
                    return null
                }
                val delegate: JsonAdapter<Any> = moshi.nextAdapter(this, Any::class.java, emptySet())
                return object : JsonAdapter<Any?>() {
                    @Throws(IOException::class)
                    override fun fromJson(reader: JsonReader): Any? {
                        return if (reader.peek() !== JsonReader.Token.NUMBER) {
                            delegate.fromJson(reader)
                        } else {
                            val numberAsString = reader.nextString()
                            val decimal = BigDecimal(numberAsString)
                            if (decimal.scale() <= 0) {
                                decimal.longValueExact()
                            } else {
                                decimal.toDouble()
                            }
                        }
                    }

                    override fun toJson(writer: JsonWriter, value: Any?) {
                        if (value is Number) {
                            val double = value.toDouble()
                            if (ceil(double) == floor(double)) {
                                writer.value(value.toLong())
                            } else {
                                writer.value(value.toDouble())
                            }
                        } else {
                            delegate.toJson(writer, value)
                        }
                    }
                }
            }
        }
    }
}
