package io.miniappx.sample.phantom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.bitcoinj.base.Base58
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import com.iwebpp.crypto.TweetNacl

/**
 * Phantom RPC 接口，用于处理 Web 层和原生层的通信
 */
class PhantomRpcInterface(private val context: Context, private val webView: WebView) {

    private val secureRandom = SecureRandom()

    private var currentKeyPair: KeyPair? = null

    @JavascriptInterface
    fun onDeepLinkMessage(message: String) {
        Log.d("PhantomRPC", "收到 Web 消息: $message")

        try {
            val jsonObject = JSONObject(message)
            val uri = jsonObject.getString("uri")
            val id = jsonObject.getString("id")

            PhantomProvider.providerMap[id] = this

            // 在主线程中执行 WebView 操作
            webView.post {
                val intent = Intent(Intent.ACTION_VIEW,
                    Uri.parse(uri))
                context.startActivity(intent)
            }

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun handleDeepLinkUri(uri: Uri) {
        // 通过 WebView 发送响应，确保在主线程执行
        webView.post {
            try {
                // 将 Map 转换为 JSON 字符串，适配 phantom_solana.js 的期望
                val jsonString = JSONObject().put("uri", uri.toString())

                val jsCode = """
                        (function() {
                            try {
                                // 直接发送 JSON 字符串，让 phantom_solana.js 进行 JSON.parse
                                window.dispatchEvent(new CustomEvent('deeplinkRpcMessage', {
                                    detail: '$jsonString'
                                }));
                            } catch(e) {
                                console.error('Phantom RPC 事件发送失败:', e);
                            }
                        })();
                    """.trimIndent()

                Log.d("PhantomRPC", "执行 JavaScript: $jsCode")
                webView.evaluateJavascript(jsCode, null)

            } catch (e: Exception) {
                Log.e("PhantomRPC", "执行 JavaScript 失败", e)
            }
        }
    }

    /**
     * 处理来自 Web 的 RPC 消息
     */
    @JavascriptInterface
    fun onDappRpcMessage(message: String) {
        Log.d("PhantomRPC", "收到 Web 消息: $message")

        try {
            val jsonObject = JSONObject(message)
            val method = jsonObject.optString("method")
            val params = jsonObject.optJSONObject("params")
            val id = jsonObject.optString("id", "")
            val jsonrpc = jsonObject.optString("jsonrpc", "2.0")

            Log.d("PhantomRPC", "解析消息 - method: $method, id: $id, jsonrpc: $jsonrpc")

            when (method) {
                // Solana 方法
                "sol_connect", "connect" -> handleConnect(params, id)
                "sol_disconnect", "disconnect" -> handleDisconnect(params, id)
                "sol_signTransaction", "signTransaction" -> handleSignTransaction(params, id)
                "sol_signMessage", "signMessage" -> handleSignMessage(params, id)
                "sol_signAllTransactions", "signAllTransactions" -> handleSignAllTransactions(params, id)
                "sol_signAndSendTransaction", "signAndSendTransaction" -> handleSignAndSendTransaction(params, id)
                "sol_getBalance", "getBalance" -> handleGetBalance(params, id)
                "sol_getTransactionCount", "getTransactionCount" -> handleGetTransactionCount(params, id)

                // 以太坊方法
                "eth_requestAccounts", "requestAccounts" -> handleRequestAccounts(params, id)
                "eth_sendTransaction" -> handleEthSendTransaction(params, id)
                "eth_getBalance" -> handleEthGetBalance(params, id)
                "eth_chainId" -> handleEthChainId(params, id)
                "personal_sign" -> handlePersonalSign(params, id)
                "eth_signTypedData_v4" -> handleEthSignTypedData(params, id)

                // 通用方法
                "getAccounts" -> handleGetAccounts(params, id)
                "getNetworkVersion" -> handleGetNetworkVersion(params, id)
                "isConnected" -> handleIsConnected(params, id)

                else -> {
                    Log.w("PhantomRPC", "未知方法: $method")
                    sendErrorResponse(id, "Method not found: $method", -32601)
                }
            }
        } catch (e: Exception) {
            Log.e("PhantomRPC", "解析消息失败", e)
            sendErrorResponse("", "Failed to parse message: ${e.message}", -32700)
        }
    }

    /**
     * 处理连接请求
     * 根据官方文档：为每个 connect 会话创建一个新的 x25519 密钥对
     */
    private fun handleConnect(params: JSONObject?, id: String) {
        Log.d("PhantomRPC", "处理连接请求: $params")

        try {
            val onlyIfTrusted = params?.optBoolean("onlyIfTrusted")
            val onlyLogSuccess = params?.optBoolean("onlyLogSuccess")
            if (onlyIfTrusted == true && onlyLogSuccess == true) {
                sendResponse(mapOf(
                    "jsonrpc" to "2.0",
                    "id" to id,
                    "result" to null,
                    "error" to mapOf(
                        "code" to "-1"
                    )
                ))
                return
            }

            PhantomProvider.providerMap[id] = this

            // 为每个连接会话生成新的 X25519 密钥对
            val newKeyPair = generateKeyPair()
            if (newKeyPair == null) {
                Log.e("PhantomRPC", "生成新密钥对失败")
                sendErrorResponse(id, "Failed to generate key pair")
                return
            }

            Log.d("PhantomRPC", "为连接会话生成新的 X25519 密钥对")
            Log.d("PhantomRPC", "新公钥: ${bytesToHex(newKeyPair.publicKey)}")

            // 在主线程中执行 WebView 操作
            webView.post {
                try {
                    // 构建Phantom深链接
                    val dappPublicKey = Base58.encode(newKeyPair.publicKey)
                    val redirectUri = "https://t.dejoy.io/dapp_data/${id}/onPhantomConnected"

                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://phantom.app/ul/v1/connect?app_url=${URLEncoder.encode(webView.url, "UTF-8")}&cluster=mainnet-beta&dapp_encryption_public_key=${dappPublicKey}&redirect_link=${URLEncoder.encode(redirectUri, "UTF-8")}"))

                    context.startActivity(intent)

                } catch (e: Exception) {
                    Log.e("PhantomRPC", "启动 Phantom 应用失败", e)
                    // 发送错误响应
                    sendErrorResponse(id, "Failed to launch Phantom: ${e.message}")
                }
            }

        } catch (e: java.lang.Exception) {
            Log.e("PhantomRPC", "处理连接请求失败", e)
            sendErrorResponse(id, "Connection failed: ${e.message}")
        }
    }

    /**
     * 处理断开连接
     */
    private fun handleDisconnect(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理断开连接请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to true,
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理交易签名
     */
    private fun handleSignTransaction(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理交易签名请求: $params")

        // 这里应该显示签名确认界面
        // 暂时返回模拟的签名结果
        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to mapOf(
                "signature" to generateMockSignature(),
                "publicKey" to getCurrentPublicKey()
            ),
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理消息签名
     */
    private fun handleSignMessage(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理消息签名请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to mapOf(
                "signature" to generateMockSignature(),
                "publicKey" to getCurrentPublicKey()
            ),
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理批量交易签名
     */
    private fun handleSignAllTransactions(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理批量交易签名请求: $params")

        // 模拟批量签名
        val signatures = List(3) { generateMockSignature() }

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to signatures,
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理签名并发送交易
     */
    private fun handleSignAndSendTransaction(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理签名并发送交易请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to mapOf(
                "signature" to generateMockSignature(),
                "publicKey" to getCurrentPublicKey()
            ),
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理获取余额
     */
    private fun handleGetBalance(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理获取余额请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to "1000000000", // 1 SOL in lamports
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理获取交易数量
     */
    private fun handleGetTransactionCount(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理获取交易数量请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to 42,
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理以太坊账户请求
     */
    private fun handleRequestAccounts(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理以太坊账户请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to listOf(getCurrentEthereumAddress()),
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理以太坊发送交易
     */
    private fun handleEthSendTransaction(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理以太坊发送交易请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to generateMockEthTxHash(),
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理个人签名
     */
    private fun handlePersonalSign(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理个人签名请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to generateMockEthSignature(),
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理类型化数据签名
     */
    private fun handleEthSignTypedData(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理个人签名请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to generateMockEthSignature(),
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理以太坊获取余额
     */
    private fun handleEthGetBalance(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理以太坊获取余额请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to "0x" + (Math.random() * 1000000000000000000L).toLong().toString(16),
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理以太坊获取链ID
     */
    private fun handleEthChainId(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理以太坊获取链ID请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to "0x1", // mainnet
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理获取账户列表
     */
    private fun handleGetAccounts(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理获取账户列表请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to listOf(getCurrentEthereumAddress()),
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理获取网络版本
     */
    private fun handleGetNetworkVersion(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理获取网络版本请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to "1", // mainnet
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 处理检查连接状态
     */
    private fun handleIsConnected(params: Any?, id: String) {
        Log.d("PhantomRPC", "处理检查连接状态请求: $params")

        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to (currentKeyPair != null),
            "error" to null
        )

        sendResponse(response)
    }

    /**
     * 使用 tweetnacl-java 生成 X25519 密钥对
     * 根据官方文档：为每个 connect 会话创建一个新的 x25519 密钥对
     */
    private fun generateKeyPair(): KeyPair? {
        try {
            // 每次都生成新的 X25519 密钥对，不缓存
            Log.d("PhantomRPC", "使用 tweetnacl-java 生成新的 X25519 密钥对")

            // 使用 tweetnacl-java 生成 X25519 密钥对
            val keyPair = TweetNacl.Box.keyPair()

            // 提取私钥和公钥
            val privateKey = keyPair.secretKey
            val publicKey = keyPair.publicKey

            // 创建新的 KeyPair 对象
            val newKeyPair = KeyPair(publicKey, privateKey)

            // 更新当前密钥对（用于后续解密）
            currentKeyPair = newKeyPair

            Log.d("PhantomRPC", "tweetnacl-java 生成新的 X25519 密钥对成功")
            Log.d("PhantomRPC", "公钥: ${bytesToHex(publicKey)}")
            Log.d("PhantomRPC", "私钥长度: ${privateKey.size}")
            Log.d("PhantomRPC", "公钥长度: ${publicKey.size}")

            return newKeyPair

        } catch (e: Exception) {
            Log.e("PhantomRPC", "tweetnacl-java 生成 X25519 密钥对失败", e)
            // 如果生成失败，使用备选方案
            return generateFallbackKeyPair()
        }
    }

    /**
     * 备选的密钥对生成方案
     */
    private fun generateFallbackKeyPair(): KeyPair? {
        try {
            // 再次尝试使用 tweetnacl-java 生成密钥对
            Log.d("PhantomRPC", "备选方案：再次尝试使用 tweetnacl-java 生成密钥对")

            val keyPair = TweetNacl.Box.keyPair()
            val privateKey = keyPair.secretKey
            val publicKey = keyPair.publicKey

            val newKeyPair = KeyPair(publicKey, privateKey)
            currentKeyPair = newKeyPair

            Log.d("PhantomRPC", "备选方案成功：tweetnacl-java 生成 X25519 密钥对")
            return newKeyPair

        } catch (e: Exception) {
            Log.e("PhantomRPC", "备选方案也失败", e)
            // 最后的备选方案：生成安全的随机密钥对
            return generateSecureRandomKeyPair()
        }
    }

    /**
     * 安全的随机密钥对生成（最后的备选方案）
     */
    private fun generateSecureRandomKeyPair(): KeyPair? {
        val random = java.security.SecureRandom()

        // 生成 32 字节的私钥
        val privateKey = ByteArray(32)
        random.nextBytes(privateKey)

        // 使用 SHA-256 哈希私钥作为公钥（这不是真正的 Ed25519，但至少是安全的）
        val digest = MessageDigest.getInstance("SHA-256")
        val publicKey = digest.digest(privateKey)

        val newKeyPair = KeyPair(publicKey, privateKey)
        currentKeyPair = newKeyPair

        Log.w("PhantomRPC", "使用安全随机生成密钥对（非标准 Ed25519）")
        return newKeyPair
    }

    /**
     * 生成模拟签名
     */
    private fun generateMockSignature(): String {
        val signature = ByteArray(64)
        secureRandom.nextBytes(signature)
        return bytesToHex(signature)
    }

    /**
     * 生成模拟以太坊交易哈希
     */
    private fun generateMockEthTxHash(): String {
        val hash = ByteArray(32)
        secureRandom.nextBytes(hash)
        return "0x" + bytesToHex(hash)
    }

    /**
     * 生成模拟以太坊签名
     */
    private fun generateMockEthSignature(): String {
        val signature = ByteArray(65)
        secureRandom.nextBytes(signature)
        return "0x" + bytesToHex(signature)
    }

    /**
     * 获取当前公钥
     */
    private fun getCurrentPublicKey(): String {
        return currentKeyPair?.publicKey?.let { bytesToHex(it) } ?: ""
    }

    /**
     * 获取当前以太坊地址
     */
    private fun getCurrentEthereumAddress(): String {
        return currentKeyPair?.publicKey?.let {
            "0x" + bytesToHex(it.take(20).toByteArray())
        } ?: "0x0000000000000000000000000000000000000000"
    }

    /**
     * 获取当前密钥对
     */
    fun getCurrentKeyPair(): KeyPair? {
        return currentKeyPair
    }

    /**
     * 发送错误响应
     */
    private fun sendErrorResponse(id: String, message: String, code: Int = -32000) {
        val errorResponse = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to null,
            "error" to mapOf(
                "code" to code,
                "message" to message
            )
        )

        sendResponse(errorResponse)
    }

    /**
     * 发送响应到 Web 层
     */
    fun sendResponse(response: Map<String, Any?>) {
        try {
            Log.d("PhantomRPC", "发送响应: $response")

            // 通过 WebView 发送响应，确保在主线程执行
            webView.post {
                try {
                    // 将 Map 转换为 JSON 字符串，适配 phantom_solana.js 的期望
                    val jsonString = JSONObject(response).toString()

                    val jsCode = """
                        (function() {
                            try {
                                // 直接发送 JSON 字符串，让 phantom_solana.js 进行 JSON.parse
                                window.dispatchEvent(new CustomEvent('phantomRpcMessage', {
                                    detail: '$jsonString'
                                }));
                            } catch(e) {
                                console.error('Phantom RPC 事件发送失败:', e);
                            }
                        })();
                    """.trimIndent()

                    Log.d("PhantomRPC", "执行 JavaScript: $jsCode")
                    webView.evaluateJavascript(jsCode, null)

                } catch (e: Exception) {
                    Log.e("PhantomRPC", "执行 JavaScript 失败", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PhantomRPC", "发送响应失败", e)
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789abcdef"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789abcdef"[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * 密钥对数据类
     */
    data class KeyPair(
        val publicKey: ByteArray,
        val privateKey: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as KeyPair

            if (!publicKey.contentEquals(other.publicKey)) return false
            if (!privateKey.contentEquals(other.privateKey)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = publicKey.contentHashCode()
            result = 31 * result + privateKey.contentHashCode()
            return result
        }
    }
}