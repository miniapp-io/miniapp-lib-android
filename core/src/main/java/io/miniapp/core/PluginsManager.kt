package io.miniapp.core
import androidx.annotation.Keep
import androidx.annotation.StringDef
import io.miniapp.core.openplatform.OpenPlatformPluginImpl

/**
 * Define Plugin Type
 */

@Keep
const val PLUGIN_OPEN_PLATFORM = "OPEN_PLATFORM"

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@StringDef(value = [PLUGIN_OPEN_PLATFORM])
@Keep
annotation class PluginName

@Keep
object PluginsManager {

    private val plugins: MutableMap<String, Plugin> = mutableMapOf()

    init {
        registerPlugin(OpenPlatformPluginImpl(PLUGIN_OPEN_PLATFORM).apply { load() })
    }

    fun registerPlugin(plugin: Plugin) {
        plugins[plugin.getName()] = plugin
    }

    fun unregisterPlugin(@PluginName pluginName: String) {
        plugins.remove(pluginName)
    }

    fun <T : Plugin> getPlugin(@PluginName pluginName: String): T? {
        return plugins[pluginName] as T?
    }
}

@Keep
interface Plugin {
    fun getName(): @PluginName String
    fun load(): Boolean
    fun unLoad()
}
