package io.miniappx.sample

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Browser
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RawRes
import io.miniapp.bridge.BridgeProvider
import io.miniappx.sample.phantom.PhantomProvider
import io.miniappx.sample.trust.SchemeUtils
import io.miniappx.sample.trust.TrustWalletProvider
import java.util.LinkedList

class WalletProvider: BridgeProvider {

    override fun getWebClient() = webViewClient

    private var webView: WebView? = null

    private val jsExecuteQueue = LinkedList<String>()

    private var isPageLoaded = false
        set(value) {
            field = value
            if (value) {
                executeJsQueue()
            }
        }

    private val webViewClient = object : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            webView = view
            isPageLoaded = true
            initBridge(view!!.context)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return  onOverrideUrlLoading(view.context, request.url.toString())
        }

        private fun openInBrowser(context: Context, url: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.putExtra(Browser.EXTRA_CREATE_NEW_TAB, true)
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun onOverrideUrlLoading(context: Context, url: String): Boolean {
            try {
                val uri = Uri.parse(url)

                val phantomAction = SchemeUtils.encodeToPhantomAction(uri)
                if (phantomAction != null) {
                    openInBrowser(context, phantomAction)
                    return true
                }

            } catch (_: Exception) {
            }

            return false
        }
    }

    override fun onWebViewCreated(webView: WebView, context: Context) {
        //TrustWalletProvider.injectRpcInterface(context, webView)
        PhantomProvider.injectRpcInterface(context, webView)
    }

    override fun onWebViewDestroy(webView: WebView) {
    }

    private fun initBridge(context: Context) {

        //executeJS(loadProviderJs(context, R.raw.trust_min))
        //executeJS(loadProviderJs(context, R.raw.phantom))
        executeJS(loadProviderJs(context, R.raw.phantom_sdk_inject))
        executeJS(loadProviderJs(context, R.raw.phantom_solana))

        // executeJS(TrustWalletProvider.loadInitJs())
        // executeJS(PhantomProvider.loadInitJs())

//        val rpcUrl = mChain.hosts?.find {
//            !it.isNullOrEmpty()
//        } ?: "https://rpc.alg2.algen.network/"
//        val address = viewModel.getAddress(mChain.chainCode.toString()).orEmpty()
//        val chainId = mChain.chainId
//        val tronRpcUrl = viewModel.findChain(ChainCode.TRON)?.hosts?.firstOrNull()
//            ?: ConfigurationManager.getInstance().getDefaultTronRpcUrl()
//        val tronAddress = viewModel.getAddress(ChainCode.TRON.code).orEmpty()
//
//        val providerJs = DAppWebInitializer.loadProviderJs(requireContext())
//        val initJs = DAppWebInitializer.loadInitJs(
//            chainId = chainId,
//            rpcUrl = rpcUrl,
//            address = address,
//            tronRpcUrl = tronRpcUrl,
//            tronAddress = tronAddress,
//            tribeId = tribeId.orEmpty()
//        )
//
//        webView?.evaluateJavascript(providerJs, null)
//        webView?.evaluateJavascript(initJs, null)
//
//        val script = "document.getElementsByName(\"full-screen\")[0].content"
//        webView?.evaluateJavascript(script) { value ->
//            if (value == null) {
//                return@evaluateJavascript
//            }
//            if (value.equals("1") || value.equals("yes") || value.equals("true")
//                || value.equals("\"yes\"") || value.equals("\"true\"") || value.equals("\"1\"")
//            ) {
//                binding.groupTitle.isVisible = false
//                val dAppUrl = dApp?.findHttpUrl()
//                WebH5Preference.put(key = dAppUrl + "_fullScreen", value = 1, async = true)
//            }
//        }
    }

    private fun loadProviderJs(context: Context, @RawRes res: Int): String {
        return context.resources.openRawResource(res).bufferedReader().use { it.readText() }
    }

    private fun executeJS(code: String) {
        if (isPageLoaded) {
            evaluateJavascript(code)
        } else {
            jsExecuteQueue.add(code)
        }
    }

    private fun evaluateJavascript(code: String) {
        webView?.evaluateJavascript(code, null)
    }

    private fun executeJsQueue() {
        while (jsExecuteQueue.isNotEmpty()) {
            jsExecuteQueue.poll()?.let { evaluateJavascript(it) }
        }
    }
}