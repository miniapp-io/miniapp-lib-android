package io.miniapp.core.openplatform.miniapp.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import org.json.JSONException
import org.json.JSONObject
import java.security.SecureRandom

internal class BiometrySP(
    val context: Context,
    val cacheKey: String,
    private val currentAccount: String) {

    companion object {
        const val PREF = "2botbiometry_"

        const val GLOBAL = "miniappsp_"

        fun getGlobalSP(context: Context): SharedPreferences {
            return context.getSharedPreferences(GLOBAL, Activity.MODE_PRIVATE)
        }
    }

    var disabled = false
        set(value) {
            field = value
            save()
        }

    var access_granted = false
        set(value) {
            field = value
            save()
        }

    var access_requested = false
        set(value) {
            field = value
            save()
        }

    var encrypted_token: String? = null
        set(value) {
            field = value
            save()
        }

    var availableType: String? = null
        set(value) {
            field = value
            save()
        }

    init {
        load()
    }

    private fun load() {
        val prefs = context.getSharedPreferences(PREF + currentAccount, Activity.MODE_PRIVATE)
        encrypted_token = prefs.getString(cacheKey, null)
        access_granted = encrypted_token != null
        access_requested = access_granted || prefs.getBoolean(cacheKey + "_requested", false)
        disabled = prefs.getBoolean(cacheKey + "_disabled", false)
    }

    private fun save() {
        val prefs = context.getSharedPreferences(PREF + currentAccount, Activity.MODE_PRIVATE)
        val edit = prefs.edit()
        if (access_requested) {
            edit.putBoolean(cacheKey + "_requested", true)
        } else {
            edit.remove(cacheKey + "_requested")
        }
        if (access_granted) {
            edit.putString(cacheKey, if (encrypted_token == null) "" else encrypted_token)
        } else {
            edit.remove(cacheKey)
        }
        if (disabled) {
            edit.putBoolean(cacheKey + "_disabled", true)
        } else {
            edit.remove(cacheKey + "_disabled")
        }
        edit.apply()
    }

    @Throws(JSONException::class)
    fun getStatus(): JSONObject {
        val `object` = JSONObject()
        if (availableType != null) {
            `object`.put("available", true)
            `object`.put("type", availableType)
        } else {
            `object`.put("available", false)
        }
        `object`.put("access_requested", access_requested)
        `object`.put("access_granted", access_granted && !disabled)
        `object`.put("token_saved", !TextUtils.isEmpty(encrypted_token))
        `object`.put("device_id", getDeviceId())
        return `object`
    }

    private fun getDeviceId(): String? {
        val prefs = context.getSharedPreferences(PREF + currentAccount, Activity.MODE_PRIVATE)
        var deviceId = prefs.getString("device_id$cacheKey", null)
        if (deviceId == null) {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            prefs.edit().putString("device_id$cacheKey", Utilities.bytesToHex(bytes).also {
                deviceId = it
            }).apply()
        }
        return deviceId
    }
}