package io.miniapp.core.openplatform.common.data

import android.content.Context
import android.content.SharedPreferences
import android.util.LruCache

internal object LRUSharedPreferencesCache {
    private const val GLOBAL = "__open_platform_local_"
    private const val MAX_SIZE = 100 // Maximum storage count

    private lateinit var sharedPreferences: SharedPreferences
    private val cache = LruCache<String, Unit>(MAX_SIZE) // Only store keys, not data

    // Initialization method
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(GLOBAL, Context.MODE_PRIVATE)
    }

    fun getValue(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun saveValue(key: String, value: String?) {
        // Check if cache is full
        if (cache.size() >= MAX_SIZE) {
            // Get the key that will be deleted
            val oldestKey = cache.snapshot().keys.firstOrNull()
            if (oldestKey != null) {
                removeValue(oldestKey) // Remove the oldest key
            }
        }

        // Put new key into cache
        cache.put(key, Unit)

        // Save new value to SharedPreferences
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun removeValue(key: String) {
        cache.remove(key) // Remove key from cache
        with(sharedPreferences.edit()) {
            remove(key)
            apply()
        }
    }
}


