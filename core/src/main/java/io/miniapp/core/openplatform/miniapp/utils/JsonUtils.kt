package io.miniapp.core.openplatform.miniapp.utils

import org.json.JSONObject


object JsonUtils {

    private fun obj(key1: String, value: Any): JSONObject? {
        return try {
            val obj = JSONObject()
            obj.put(key1, value)
            obj
        } catch (e: Exception) {
            null
        }
    }

    private fun obj(key1: String, value: Any, key2: String, value2: Any): JSONObject? {
        return try {
            val obj = JSONObject()
            obj.put(key1, value)
            obj.put(key2, value2)
            obj
        } catch (e: Exception) {
            null
        }
    }

    private fun obj(
        key1: String,
        value: Any,
        key2: String,
        value2: Any,
        key3: String,
        value3: Any
    ): JSONObject? {
        return try {
            val obj = JSONObject()
            obj.put(key1, value)
            obj.put(key2, value2)
            obj.put(key3, value3)
            obj
        } catch (e: Exception) {
            null
        }
    }

    fun obj(
        key1: String,
        value: Any,
        key2: String,
        value2: Any,
        key3: String,
        value3: Any,
        key4: String,
        value4: Any
    ): JSONObject? {
        return try {
            val obj = JSONObject()
            obj.put(key1, value)
            obj.put(key2, value2)
            obj.put(key3, value3)
            obj.put(key4, value4)
            obj
        } catch (e: Exception) {
            null
        }
    }
}