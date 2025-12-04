package io.miniapp.core.openplatform.miniapp.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Browser
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.net.toUri
import io.miniapp.core.R
import java.util.Locale
import java.util.regex.Pattern


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
            CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .build()
                )
                .setStartAnimations(context, R.anim.enter_alpha_in, R.anim.exit_alpha_out)
                .setExitAnimations(context, R.anim.enter_alpha_in, R.anim.exit_alpha_out)
                .apply { session?.let { setSession(it) } }
                .build()
                .launchUrl(context, url.toUri())
        } catch (activityNotFoundException: ActivityNotFoundException) {
            openInDefaultBrowser(context, url)
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