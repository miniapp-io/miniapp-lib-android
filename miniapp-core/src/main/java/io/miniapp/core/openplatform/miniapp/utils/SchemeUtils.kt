package io.miniapp.core.openplatform.miniapp.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Browser
import android.provider.Settings
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.net.toUri
import io.miniapp.core.R
import java.util.Locale
import java.util.regex.Pattern
import kotlin.text.contains

object SchemeUtils {

    private var uriParse: Pattern? = null
    private fun getURIParsePattern(): Pattern? {
        if (uriParse == null) {
            uriParse =
                Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?") // RFC 3986 B
        }
        return uriParse
    }

    private fun getHostAuthority(uri: String?): String? {
        if (uri == null) {
            return null
        }
        // CVE-2017-13274
        val matcher = getURIParsePattern()!!.matcher(uri)
        if (matcher.matches()) {
            var authority = matcher.group(4)
            if (authority != null) {
                authority = authority.lowercase(Locale.getDefault())
            }
            return authority
        }
        return null
    }

    private fun getHostAuthority(uri: Uri?): String? {
        return if (uri == null) {
            null
        } else getHostAuthority(uri.toString())
    }

    fun isInternalUri(uri: Uri?, hosts: List<String>): Boolean {
        var host: String? = getHostAuthority(uri)
        host = host?.lowercase(Locale.getDefault()) ?: ""
        return hosts.firstOrNull {
            it.lowercase().contains(host)
        }?.let {
            true
        } ?: false
    }

    /**
     * Open url in custom tab or, if not available, in the default browser.
     * If several compatible browsers are installed, the user will be proposed to choose one.
     * Ref: https://developer.chrome.com/multidevice/android/customtabs.
     */
    fun openUrlInChromeCustomTab(
        context: Context,
        session: CustomTabsSession?,
        url: String
    ) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .build()
                )
                .setStartAnimations(context, R.anim.enter_alpha_in, R.anim.exit_alpha_out)
                .setExitAnimations(context, R.anim.enter_alpha_in, R.anim.exit_alpha_out)
                .apply { session?.let { setSession(it) } }
                .build()

            val browserPackage = getBestBrowserPackage(context)

            browserPackage?.let {
                customTabsIntent.intent.`package` = it
                LogTimber.tag("CustomTabsHelper").d("Using browser package: $it")
            }

            customTabsIntent.launchUrl(context, url.toUri())

        } catch (activityNotFoundException: ActivityNotFoundException) {
            openInDefaultBrowser(context, url)
        }
    }

    fun getBestBrowserPackage(context: Context): String? {
        return try {
            // 1. 获取所有支持 Custom Tabs 的浏览器
            val servicePackages = getCustomTabsSupportingBrowsers(context)
            LogTimber.tag("CustomTabsHelper").d("Custom Tabs support: $servicePackages")

            val (preferredBrowserPackage, defaultBrowserPackage) = getPreferredAndDefaultBrowser(context)
            LogTimber.tag("CustomTabsHelper")
                .d("Preferred: $preferredBrowserPackage, Default: $defaultBrowserPackage")

            when {
                servicePackages.contains("com.android.chrome") -> {
                    LogTimber.tag("CustomTabsHelper").d("Chrome is available, using it")
                    "com.android.chrome"
                }
                preferredBrowserPackage != null && servicePackages.contains(preferredBrowserPackage) -> {
                    LogTimber.tag("CustomTabsHelper").d("Using preferred browser: $preferredBrowserPackage")
                    preferredBrowserPackage
                }
                defaultBrowserPackage != null && servicePackages.contains(defaultBrowserPackage) -> {
                    LogTimber.tag("CustomTabsHelper").d("Using default browser: $defaultBrowserPackage")
                    defaultBrowserPackage
                }
                servicePackages.isNotEmpty() -> {
                    LogTimber.tag("CustomTabsHelper").d("Using first available: ${servicePackages[0]}")
                    servicePackages[0]
                }
                else -> {
                    LogTimber.tag("CustomTabsHelper").d("No Custom Tabs browser found")
                    null
                }
            }
        } catch (e: Exception) {
            LogTimber.tag("CustomTabsHelper").w(e, "Browser package selection failed:")
            null
        }
    }

    fun getCustomTabsSupportingBrowsers(context: Context): List<String> {
        val packageManager = context.packageManager
        val servicePackages = mutableListOf<String>()

        val serviceIntent = Intent("android.support.customtabs.action.CustomTabsService")

        val resolveInfos = packageManager.queryIntentServices(serviceIntent, 0)

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.serviceInfo.packageName
            servicePackages.add(packageName)
        }

        return servicePackages.distinct()
    }

    fun getPreferredAndDefaultBrowser(context: Context): Pair<String?, String?> {
        val packageManager = context.packageManager

        val viewIntent = Intent(Intent.ACTION_VIEW, "https://miniappx.io".toUri())
        val defaultResolveInfo = packageManager.resolveActivity(viewIntent, 0)
        val defaultBrowserPackage = defaultResolveInfo?.activityInfo?.packageName

        val preferredBrowserPackage = getPreferredBrowserPackage(context)

        return Pair(preferredBrowserPackage, defaultBrowserPackage)
    }

    fun getPreferredBrowserPackage(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val defaultBrowser = Settings.Secure.getString(
                    context.contentResolver,
                    "http_user_agent"
                )
                if (defaultBrowser != null && defaultBrowser.contains("Chrome")) {
                    "com.android.chrome"
                } else {
                    null
                }
            } else {
                val intent = Intent(Intent.ACTION_VIEW, "https://miniappx.io".toUri())
                val resolveInfo = context.packageManager.resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY)
                resolveInfo?.activityInfo?.packageName
            }
        } catch (e: Exception) {
            null
        }
    }

    fun openInDefaultBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            intent.putExtra(Browser.EXTRA_CREATE_NEW_TAB, true)
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openInBrowser(context: Context, url: String) {
        openUrlInChromeCustomTab(context,  null, url)
    }
}