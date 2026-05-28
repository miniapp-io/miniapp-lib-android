package io.miniapp.core.openplatform.common.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

internal class SessionProvider(val context: Context) {

    companion object {
        const val GLOBAL = "__open_platform_auth_"
        private const val KEY_TOKEN = "id"
        private const val KEY_EXPIRES = "expires_at"
        private const val TOKEN_EXPIRE_SKEW_SECONDS = 30L
    }

    private val provider: SharedPreferences =
        context.getSharedPreferences(GLOBAL, Activity.MODE_PRIVATE)

    var token: String? = null
        set(value) {
            synchronized(this) {
                field = value
                if (!suppressPersist) {
                    save()
                }
            }
        }

    var expires: Long? = null
        set(value) {
            synchronized(this) {
                field = value
                if (!suppressPersist) {
                    save()
                }
            }
        }

    @Volatile
    private var suppressPersist = false

    fun isAuth(): Boolean {
        synchronized(this) {
            return !token.isNullOrBlank() && !isTokenExpirationTime()
        }
    }

    fun isTokenExpirationTime(): Boolean {
        synchronized(this) {
            val expiresAt = expires ?: 0
            val expiresAtSeconds = if (expiresAt > 9_999_999_999L) expiresAt / 1000 else expiresAt
            val nowSeconds = System.currentTimeMillis() / 1000
            return expiresAtSeconds <= nowSeconds + TOKEN_EXPIRE_SKEW_SECONDS
        }
    }

    init {
        load()
    }

    private fun load() {
        synchronized(this) {
            try {
                suppressPersist = true
                token = provider.getString(KEY_TOKEN, null)
                expires = if (provider.contains(KEY_EXPIRES)) {
                    provider.getLong(KEY_EXPIRES, 0L)
                } else {
                    null
                }
            } finally {
                suppressPersist = false
            }

            if (isTokenExpirationTime()) {
                token = null
                expires = null
            }
        }
    }

    private fun save() {
        val edit = provider.edit()
        edit.putString(KEY_TOKEN, token)
        if (expires == null) {
            edit.remove(KEY_EXPIRES)
        } else {
            edit.putLong(KEY_EXPIRES, expires ?: 0L)
        }
        edit.apply()
    }
}