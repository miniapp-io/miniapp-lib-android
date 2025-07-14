package io.miniapp.core.openplatform.common.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

internal class SessionProvider(val context: Context) {

    companion object {
        const val GLOBAL = "__open_platform_auth_"
    }

    private val provider: SharedPreferences =
        context.getSharedPreferences(GLOBAL, Activity.MODE_PRIVATE)

    var refreshToken: (suspend ()-> String?)? = null

    var token: String? = null
        set(value) {
            synchronized(this) {
                field = value
                save()
            }
        }

    fun isAuth(): Boolean {
        synchronized(this) {
            return !token.isNullOrBlank()
        }
    }

    fun isTokenExpirationTime() : Boolean {
        return false
    }

    init {
        load()
    }

    private fun load() {
        synchronized(this) {
            token = provider.getString("id", null)
        }
    }

    private fun save() {
        val edit = provider.edit()
        edit.putString("id", token)
        edit.apply()
    }
}