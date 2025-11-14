package io.miniappx.sample.trust

import android.net.Uri
import java.net.URLEncoder
import java.util.Locale
import java.util.regex.Pattern


object SchemeUtils {

    const val TON_SCHEME = "ton"
    const val TONKEEPER_SCHEME = "tonkeeper"
    const val TONCONNECT_SCHEME = "tc"

    const val TONKEEPER_HOST = "app.tonkeeper.com"

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

    fun isTonConnectUri(uri: Uri): Boolean {
        if (uri.scheme == TONCONNECT_SCHEME) {
            return true
        }
        return getTonkeeperUriFirstPath(uri) == "ton-connect"
    }

    fun getTonkeeperUriFirstPath(uri: Uri): String {
        val pathSegments = uri.pathSegments.toMutableList()
        if (uri.scheme == TONKEEPER_SCHEME) {
            pathSegments.add(0, uri.host!!)
        }
        if (pathSegments.isEmpty()) {
            return ""
        }
        return pathSegments.first()
    }

    fun encodeToTonBridgeAction(standardUrl: Uri?): String? {
        if(standardUrl?.host?.lowercase() == "bridge.tonapi.io") {
           return standardUrl.getQueryParameter("topic").takeIf {
                it == "sendTransaction"
            }?.let {
                "https://openweb3.io/apps/9?startapp=tonconnect-ret__back-ct__${System.currentTimeMillis()}"
            }
        }
        return null
    }

    private fun String.oneLineReplaceRedirectLink(newValue: String = "https://t.dejoy.io/dapp_data"): String {
        return if (contains("redirect_link=")) {
            replace(Regex("redirect_link=[^&]*"), "redirect_link=${URLEncoder.encode(newValue, "UTF-8")}")
        } else if (contains("ref=")) {
            replace(Regex("ref=[^&]*"), "ref=${URLEncoder.encode(newValue, "UTF-8")}")
        } else {
            return this
        }
    }

    fun encodeToPhantomAction(standardUrl: Uri): String? {
        // https://phantom.com/ul/v1/connect?app_url=https%3A%2F%2Fmagiceden.io%2Fme&cluster=mainnet-beta&dapp_encryption_public_key=HQ1qXrwq3u8bUxCF5yBYdA89eGKCYtMjqknJFDofsLpd&redirect_link=https%3A%2F%2Fmagiceden.io%2Fme
        // https://phantom.app/ul/browse/https://www.happypump.ai/en?privy_connector=solana_adapter&privy_wallet_client=phantom&privy_connect_only=false?ref=https://www.happypump.ai/en?privy_connector=solana_adapter&privy_wallet_client=phantom&privy_connect_only=false

        if (standardUrl.host?.lowercase() == "phantom.app" ||
            standardUrl.host?.lowercase() == "phantom.com" ) {
            return standardUrl.toString().oneLineReplaceRedirectLink()
        }
        return null
    }

    fun encodeToTonAction(standardUrl: Uri): String? {

        var action: String? = null

        if (isTonConnectUri(standardUrl)) {
            action = "tonconnect-"
        } else {
            return null
        }

        val encoded = URLEncoder.encode(standardUrl.query?.replace("\\", ""), "UTF-8")

        return "${action}${encodeToTgParamsFormat(encoded)}".replace("-ret__back", "") + "-ret__back-ct__${System.currentTimeMillis()}"
    }

    private fun encodeToTgParamsFormat(url: String): String {
        return url.replace("%5C","")
            .replace("-","--2D")
            .replace("_","--5F")
            .replace("%3D","__")
            .replace(".","--2E")
            .replace("%26","-")
            .replace("%7B", "--7B")
            .replace("%22", "--22")
            .replace("%3A", "--3A")
            .replace("%2F", "--2F")
            .replace("%5B", "--5B")
            .replace("%5D", "--5D")
            .replace("%7D", "--7D")
            .replace("%2C", "--2C")
            .replace("%5F", "--5F")
    }
}