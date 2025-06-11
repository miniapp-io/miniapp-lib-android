package io.miniapp.core.openplatform.common.data

import android.content.Context
import android.content.SharedPreferences
import android.util.LruCache

internal object LRUSharedPreferencesCache {
    private const val GLOBAL = "__open_platform_local_"
    private const val MAX_SIZE = 100 // 最大存储数量

    private lateinit var sharedPreferences: SharedPreferences
    private val cache = LruCache<String, Unit>(MAX_SIZE) // 只存储键，不保存数据

    // 初始化方法
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(GLOBAL, Context.MODE_PRIVATE)
    }

    fun getValue(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun saveValue(key: String, value: String?) {
        // 检查缓存是否已满
        if (cache.size() >= MAX_SIZE) {
            // 获取即将被删除的键
            val oldestKey = cache.snapshot().keys.firstOrNull()
            if (oldestKey != null) {
                removeValue(oldestKey) // 删除最旧的键
            }
        }

        // 将新键放入缓存
        cache.put(key, Unit)

        // 保存新的值到 SharedPreferences
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun removeValue(key: String) {
        cache.remove(key) // 从缓存中移除键
        with(sharedPreferences.edit()) {
            remove(key)
            apply()
        }
    }
}


