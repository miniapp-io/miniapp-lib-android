package io.miniapp.core.openplatform.miniapp.utils

import android.net.Uri
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
}