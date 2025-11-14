package io.miniappx.sample.trust

import android.content.Context
import android.webkit.WebView

object TrustWalletProvider {

    private const val CHAIN_ID = 56
    private const val RPC_URL = "https://bsc-dataseed2.binance.org"

    fun injectRpcInterface(context: Context, webView: WebView) {
        WebAppInterface(context = context, webView = webView, dappUrl = null).apply {
            webView.addJavascriptInterface(this, "_tw_")
        }
    }

    fun loadInitJs(chainId: Int = CHAIN_ID, rpcUrl: String = RPC_URL): String {
        val source = """
        (function() {
        var config = {                
            ethereum: {
                chainId: $chainId,
                rpcUrl: "$rpcUrl"
            },
            solana: {
                cluster: "mainnet-beta",
            },
            isDebug: true
        };
        
        // 初始化 Trust Wallet Providers
        if (typeof trustwallet !== 'undefined') {
            // Trust Wallet Ethereum Provider
            trustwallet.ethereum = new trustwallet.Provider(config);
            
            // Trust Wallet Solana Provider
            trustwallet.solana = new trustwallet.SolanaProvider(config);
            
            trustwallet.postMessage = (json) => {
                if (window._tw_) {
                    window._tw_.postMessage(JSON.stringify(json));
                }
            };
            
            console.log('Trust Wallet Providers initialized successfully');
        } else {
            console.warn('Trust Wallet not available');
        }
        
    })();
    """
        return source
    }
}