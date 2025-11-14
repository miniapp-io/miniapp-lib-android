package io.miniappx.sample.phantom

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import com.iwebpp.crypto.TweetNacl
import com.iwebpp.crypto.TweetNaclFast
import org.bitcoinj.base.Base58
import org.json.JSONObject


object PhantomProvider {

    val providerMap: MutableMap<String,PhantomRpcInterface> = mutableMapOf()

    // 防重复调用：记录最近处理的 URI
    private var lastProcessedUri: String? = null
    private var lastProcessedTime: Long = 0
    private const val DUPLICATE_THRESHOLD_MS = 1000L // 1秒内的重复调用将被忽略

    fun onWalletIntent(uri: Uri) {

        val uriString = uri.toString()
        val currentTime = System.currentTimeMillis()

        // 防重复调用检查
        if (uriString == lastProcessedUri &&
            (currentTime - lastProcessedTime) < DUPLICATE_THRESHOLD_MS) {
            Log.d("PhantomRPC", "检测到重复调用，忽略: $uriString")
            return
        }

        // 更新记录
        lastProcessedUri = uriString
        lastProcessedTime = currentTime

        Log.d("PhantomRPC", "收到钱包意图: $uriString")

        if (uri.pathSegments.size == 3) {

            val host = uri.pathSegments[0]
            val id = uri.pathSegments[1]
            val method = uri.pathSegments[2]

            Log.d("PhantomRPC", "解析意图 - host: $host, id: $id, method: $method")

            val provider = providerMap[id] ?: run {
                Log.w("PhantomRPC", "未找到对应的 provider: $id")
                return
            }

            provider.handleDeepLinkUri(uri)
        }
    }


    /**
     * 处理钱包意图（Phantom 应用返回的深度链接）
     * 根据官方文档：解密 data 字段获得用户公钥等 JSON 数据
     * 添加防重复调用机制
     */
    fun onWalletIntentEx(uri: Uri) {
        try {
            val uriString = uri.toString()
            val currentTime = System.currentTimeMillis()

            // 防重复调用检查
            if (uriString == lastProcessedUri &&
                (currentTime - lastProcessedTime) < DUPLICATE_THRESHOLD_MS) {
                Log.d("PhantomRPC", "检测到重复调用，忽略: $uriString")
                return
            }

            // 更新记录
            lastProcessedUri = uriString
            lastProcessedTime = currentTime

            Log.d("PhantomRPC", "收到钱包意图: $uriString")

            if (uri.pathSegments.size == 3) {

                val host = uri.pathSegments[0]
                val id = uri.pathSegments[1]
                val method = uri.pathSegments[2]

                Log.d("PhantomRPC", "解析意图 - host: $host, id: $id, method: $method")

                val provider = providerMap[id] ?: run {
                    Log.w("PhantomRPC", "未找到对应的 provider: $id")
                    return
                }

                // 检查错误
                val errorCode = uri.getQueryParameter("errorCode")
                if (errorCode != null) {
                    val errorMessage = uri.getQueryParameter("errorMessage") ?: "Unknown error"
                    Log.e("PhantomRPC", "钱包返回错误: $errorCode - $errorMessage")

                    provider.sendResponse(mapOf(
                        "jsonrpc" to "2.0",
                        "id" to id,
                        "result" to null,
                        "error" to mapOf(
                            "code" to errorCode.toIntOrNull(),
                            "message" to errorMessage
                        )
                    ))
                    return
                }

                // 解析深度链接参数
                val phantomEncryptionPublicKey = uri.getQueryParameter("phantom_encryption_public_key")
                val data = uri.getQueryParameter("data")
                val nonce = uri.getQueryParameter("nonce")

                if (phantomEncryptionPublicKey.isNullOrEmpty() || data.isNullOrEmpty() || nonce.isNullOrEmpty()) {
                    Log.e("PhantomRPC", "缺少必要的参数")
                    Log.e("PhantomRPC", "phantom_encryption_public_key: $phantomEncryptionPublicKey")
                    Log.e("PhantomRPC", "data: $data")
                    Log.e("PhantomRPC", "nonce: $nonce")
                    return
                }

                Log.d("PhantomRPC", "Phantom 加密公钥: $phantomEncryptionPublicKey")
                Log.d("PhantomRPC", "加密数据: $data")
                Log.d("PhantomRPC", "Nonce: $nonce")


                // 使用 ECDH 生成的共享密钥解密数据
                val decryptedData = decryptPhantomData(
                    provider = provider,
                    encryptedData = data,
                    nonce = nonce,
                    phantomPublicKey = phantomEncryptionPublicKey
                )

                if (decryptedData != null) {
                    // 解析解密后的 JSON 数据
                    val userData = parseDecryptedData(decryptedData)
                    if (userData != null) {
                        Log.d("PhantomRPC", "成功解密用户数据: $userData")

                        // 处理连接成功
                        handlePhantomConnectionSuccess(provider, userData)
                    } else {
                        Log.e("PhantomRPC", "解析解密数据失败")
                    }
                } else {
                    Log.e("PhantomRPC", "解密 Phantom 数据失败")
                }
            }

        } catch (e: Exception) {
            Log.e("PhantomRPC", "处理钱包意图失败", e)
        }
    }

    /**
     * 处理 Phantom 连接成功
     */
    private fun handlePhantomConnectionSuccess(provider: PhantomRpcInterface, userData: Map<String, Any>) {
        try {
            Log.d("PhantomRPC", "Phantom 连接成功，用户数据: $userData")

            // 提取公钥和会话信息
            val publicKey = userData["public_key"] as? String
            val session = userData["session"] as? String

            if (publicKey != null) {
                Log.d("PhantomRPC", "用户公钥: $publicKey")
            }

            if (session != null) {
                Log.d("PhantomRPC", "会话 ID: $session")
            }

            // 这里可以添加更多的连接成功处理逻辑
            // 比如更新 UI、保存会话信息等
            provider.sendResponse(userData)

        } catch (e: Exception) {
            Log.e("PhantomRPC", "处理 Phantom 连接成功失败", e)
        }
    }

    /**
     * 解密 Phantom 数据
     * 按照官方 Demo 使用 tweetnacl 的 nacl.box.open.after 方式
     */
    private fun decryptPhantomData(
        provider: PhantomRpcInterface,
        encryptedData: String,
        nonce: String,
        phantomPublicKey: String
    ): Map<String, Any>? {
        try {
            Log.d("PhantomRPC", "开始解密 Phantom 数据")
            Log.d("PhantomRPC", "加密数据: $encryptedData")
            Log.d("PhantomRPC", "Nonce: $nonce")
            Log.d("PhantomRPC", "Phantom 公钥: $phantomPublicKey")

            // 获取当前 DApp 密钥对
            val currentKeyPair = provider.getCurrentKeyPair()
            if (currentKeyPair == null) {
                Log.e("PhantomRPC", "当前密钥对为空")
                return null
            }

            // 按照官方 Demo：使用 nacl.box.before 生成共享密钥
            val sharedSecret = generateSharedSecretWithX25519(Base58.decode(phantomPublicKey), currentKeyPair.privateKey)
            if (sharedSecret == null) {
                Log.e("PhantomRPC", "共享密钥生成失败")
                return null
            }

            Log.d("PhantomRPC", "共享密钥生成成功: ${bytesToHex(sharedSecret)}")

            // 按照官方 Demo：使用 nacl.box.open.after 解密
            val decryptedData = decryptWithTweetnacl(
                encryptedData = encryptedData,
                nonce = nonce,
                sharedSecret = sharedSecret
            )
            if (decryptedData == null) {
                Log.e("PhantomRPC", "解密数据失败")
                return null
            }

            // 解析 JSON 数据
            val jsonString = String(decryptedData, Charsets.UTF_8)
            Log.d("PhantomRPC", "解密后的 JSON: $jsonString")

            val jsonObject = JSONObject(jsonString)
            val result = mutableMapOf<String, Any>()

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = jsonObject.get(key)
            }

            Log.d("PhantomRPC", "解密成功: $result")
            return result

        } catch (e: Exception) {
            Log.e("PhantomRPC", "解密 Phantom 数据失败", e)
            return null
        }
    }

    /**
     * 使用 tweetnacl-java 方式解密
     * 对应官方 Demo 的 nacl.box.open.after
     */
    private fun decryptWithTweetnacl(
        encryptedData: String,
        nonce: String,
        sharedSecret: ByteArray
    ): ByteArray? {
        try {
            Log.d("PhantomRPC", "使用 tweetnacl-java 方式解密")

            // Base58 解码
            val encryptedBytes = Base58.decode(encryptedData)
            val nonceBytes = Base58.decode(nonce)

            Log.d("PhantomRPC", "加密数据长度: ${encryptedBytes.size}")
            Log.d("PhantomRPC", "Nonce 长度: ${nonceBytes.size}")
            Log.d("PhantomRPC", "共享密钥长度: ${sharedSecret.size}")

            // 使用 tweetnacl-java 的底层方法 crypto_box_open_after
            // 方法签名: crypto_box_open_after(byte[] m, byte[] c, byte[] n, byte[] k)
            // 其中: m = 输出缓冲区, c = 密文, n = nonce, k = 预计算密钥
            try {
                Log.d("PhantomRPC", "尝试使用 crypto_box_open_after")
                Log.d("PhantomRPC", "参数: encryptedBytes(${encryptedBytes.size}), nonceBytes(${nonceBytes.size}), sharedSecret(${sharedSecret.size})")

                // 检查数据格式
                Log.d("PhantomRPC", "加密数据前32字节: ${bytesToHex(encryptedBytes.copyOfRange(0, minOf(32, encryptedBytes.size)))}")
                Log.d("PhantomRPC", "加密数据后32字节: ${bytesToHex(encryptedBytes.copyOfRange(maxOf(0, encryptedBytes.size - 32), encryptedBytes.size))}")
                Log.d("PhantomRPC", "Nonce 前8字节: ${bytesToHex(nonceBytes.copyOfRange(0, minOf(8, nonceBytes.size)))}")
                Log.d("PhantomRPC", "Nonce 后8字节: ${bytesToHex(nonceBytes.copyOfRange(maxOf(0, nonceBytes.size - 8), nonceBytes.size))}")

                // 创建输出缓冲区 - 应该是加密数据长度减去32字节的标签
                val expectedDataLength = encryptedBytes.size - 32
                val outputBuffer = ByteArray(expectedDataLength)

                Log.d("PhantomRPC", "预期数据长度: $expectedDataLength")

                val result = TweetNaclFast.crypto_box_afternm(
                    outputBuffer,      // m: 输出缓冲区
                    encryptedBytes,    // c: 密文
                    encryptedBytes.size, // d: 密文长度
                    nonceBytes,        // n: nonce
                    sharedSecret       // k: 预计算密钥
                )

                if (result == 0) {
                    // 成功：result = 0 表示成功
                    Log.d("PhantomRPC", "tweetnacl-java crypto_box_open_after 调用成功，返回码: $result")

                    // 找到实际的数据长度（去掉填充）
                    var actualLength = outputBuffer.size
                    for (i in outputBuffer.indices.reversed()) {
                        if (outputBuffer[i] != 0.toByte()) {
                            actualLength = i + 1
                            break
                        }
                    }

                    val decryptedData = outputBuffer.copyOf(actualLength)
                    Log.d("PhantomRPC", "解密成功，实际数据长度: ${decryptedData.size}")
                    return decryptedData
                } else {
                    Log.d("PhantomRPC", "crypto_box_open_after 返回错误码: $result")

                    // 尝试分析错误原因
                    when (result) {
                        -1 -> Log.d("PhantomRPC", "错误码 -1: 通常表示参数错误或数据格式不正确")
                        -2 -> Log.d("PhantomRPC", "错误码 -2: 通常表示数据长度不足")
                        else -> Log.d("PhantomRPC", "未知错误码: $result")
                    }
                }
            } catch (e: Exception) {
                Log.d("PhantomRPC", "crypto_box_open_after 调用失败: ${e.message}")
                Log.d("PhantomRPC", "异常类型: ${e.javaClass.simpleName}")
            }

            // 备选方案：使用 SecretBox（虽然不完全正确，但可以尝试）
            try {
                Log.d("PhantomRPC", "尝试备选方案：使用 SecretBox")

                // 基于 React 版本分析，数据可能包含32字节的标签
                // 方案1：直接使用原始数据
                var secretBox = TweetNacl.SecretBox(sharedSecret)
                var decryptedData = secretBox.open(encryptedBytes, nonceBytes)

                if (decryptedData != null) {
                    Log.d("PhantomRPC", "SecretBox 方案1成功：直接使用原始数据")
                    return decryptedData
                }

                // 方案2：尝试去掉前32字节（可能是头部）
                if (encryptedBytes.size > 32) {
                    Log.d("PhantomRPC", "尝试 SecretBox 方案2：去掉前32字节")
                    val dataWithoutHeader = encryptedBytes.copyOfRange(32, encryptedBytes.size)
                    secretBox = TweetNacl.SecretBox(sharedSecret)
                    decryptedData = secretBox.open(dataWithoutHeader, nonceBytes)

                    if (decryptedData != null) {
                        Log.d("PhantomRPC", "SecretBox 方案2成功：去掉前32字节")
                        return decryptedData
                    }
                }

                // 方案3：尝试去掉后32字节（可能是 Poly1305 标签）
                if (encryptedBytes.size > 32) {
                    Log.d("PhantomRPC", "尝试 SecretBox 方案3：去掉后32字节（Poly1305标签）")
                    val dataWithoutTag = encryptedBytes.copyOfRange(0, encryptedBytes.size - 32)
                    secretBox = TweetNacl.SecretBox(sharedSecret)
                    decryptedData = secretBox.open(dataWithoutTag, nonceBytes)

                    if (decryptedData != null) {
                        Log.d("PhantomRPC", "SecretBox 方案3成功：去掉后32字节（Poly1305标签）")
                        return decryptedData
                    }
                }

                // 方案4：尝试使用 nonce 的前16字节
                if (nonceBytes.size >= 16) {
                    Log.d("PhantomRPC", "尝试 SecretBox 方案4：使用 nonce 的前16字节")
                    val nonce16 = nonceBytes.copyOfRange(0, 16)
                    secretBox = TweetNacl.SecretBox(sharedSecret)
                    decryptedData = secretBox.open(encryptedBytes, nonce16)

                    if (decryptedData != null) {
                        Log.d("PhantomRPC", "SecretBox 方案4成功：使用 nonce 的前16字节")
                        return decryptedData
                    }
                }

                Log.d("PhantomRPC", "所有 SecretBox 方案都失败")

            } catch (e: Exception) {
                Log.d("PhantomRPC", "SecretBox 解密也失败: ${e.message}")
                Log.d("PhantomRPC", "异常类型: ${e.javaClass.simpleName}")
            }

            Log.e("PhantomRPC", "所有 tweetnacl-java 解密方法都失败")
            return null

        } catch (e: Exception) {
            Log.e("PhantomRPC", "tweetnacl-java 解密失败", e)
            return null
        }
    }

    /**
     * 解析解密后的数据
     * 根据 Phantom JavaScript 实现，connect 返回 public_key 和 session
     */
    private fun parseDecryptedData(decryptedData: Map<String, Any>): Map<String, Any>? {
        return try {
            val jsonString = JSONObject(decryptedData).toString()
            Log.d("PhantomRPC", "解密后的 JSON 字符串: $jsonString")

            // 使用 JSON 解析器解析数据
            val jsonObject = org.json.JSONObject(jsonString)
            val result = mutableMapOf<String, Any>()

            // 根据 Phantom JavaScript 实现，connect 返回的关键字段
            if (jsonObject.has("public_key")) {
                result["public_key"] = jsonObject.getString("public_key")
            }

            if (jsonObject.has("session")) {
                result["session"] = jsonObject.getString("session")
            }

            // 兼容其他可能的字段（signIn 等）
            if (jsonObject.has("address")) {
                result["address"] = jsonObject.getString("address")
            }

            if (jsonObject.has("signedMessage")) {
                result["signedMessage"] = jsonObject.getString("signedMessage")
            }

            if (jsonObject.has("signature")) {
                result["signature"] = jsonObject.getString("signature")
            }

            if (jsonObject.has("publicKey")) {
                result["publicKey"] = jsonObject.getString("publicKey")
            }

            if (jsonObject.has("accounts")) {
                val accountsArray = jsonObject.getJSONArray("accounts")
                val accountsList = mutableListOf<String>()
                for (i in 0 until accountsArray.length()) {
                    accountsList.add(accountsArray.getString(i))
                }
                result["accounts"] = accountsList
            }

            if (jsonObject.has("network")) {
                result["network"] = jsonObject.getString("network")
            }

            if (jsonObject.has("isConnected")) {
                result["isConnected"] = jsonObject.getBoolean("isConnected")
            }

            // 记录解析结果
            Log.d("PhantomRPC", "解析后的用户数据: $result")

            result

        } catch (e: Exception) {
            Log.e("PhantomRPC", "解析解密数据失败", e)
            Log.e("PhantomRPC", "原始数据: $decryptedData")
            null
        }
    }

    /**
     * 使用 X25519 生成共享密钥
     * 对应官方 Demo 的 nacl.box.before(phantomPublicKey, dappPrivateKey)
     */
    private fun generateSharedSecretWithX25519(
        phantomPublicKey: ByteArray,
        dappPrivateKey: ByteArray
    ): ByteArray? {
        try {
            Log.d("PhantomRPC", "开始生成 X25519 共享密钥")
            Log.d("PhantomRPC", "Phantom 公钥 (Base58): $phantomPublicKey")
            Log.d("PhantomRPC", "DApp 私钥: ${bytesToHex(dappPrivateKey)}")

            // 使用 tweetnacl-java 的 crypto_box_beforenm 方法
            // 对应 JavaScript 的 nacl.box.before(phantomPublicKey, dappPrivateKey)
            val sharedSecret = ByteArray(32) // crypto_box_BEFORENMBYTES = 32

            val result = TweetNaclFast.crypto_box_beforenm(
                sharedSecret,           // k: 输出缓冲区
                phantomPublicKey,       // pk: Phantom 公钥
                dappPrivateKey          // sk: DApp 私钥
            )

            if (result == 0) {
                Log.d("PhantomRPC", "X25519 共享密钥生成成功: ${bytesToHex(sharedSecret)}")
                return sharedSecret
            } else {
                Log.e("PhantomRPC", "crypto_box_before 返回错误码: $result")
                return null
            }

        } catch (e: Exception) {
            Log.e("PhantomRPC", "X25519 共享密钥生成失败", e)
            return null
        }
    }


    @SuppressLint("JavascriptInterface")
    fun injectRpcInterface(context: Context, webView: WebView) {
        webView.addJavascriptInterface(PhantomRpcInterface(context, webView), "PhantomRpcProvider")
    }

    /**
     * 创建并注入 Phantom Solana 提供者
     * 按照原版结构：window.phantom.solana: r {_events: Ea, _eventsCount: 3, #e: r, #t: f, #n: f, _}
     */
    fun loadInitJs(config: SolanaConfig = SolanaConfig()): String {
        return """
        (function() {
            'use strict';
            
            // 配置对象，对应原始代码中的注入器参数 r
            var config = {
                network: '${config.network}',
                rpcUrl: '${config.rpcUrl}',
                commitment: '${config.commitment}',
                debug: ${config.debug}
            };
            
            // 创建事件发射器类，对应原版的 Ea
            function createEventEmitter() {
                return {
                    _events: {},
                    _eventsCount: 0,
                    
                    on: function(event, callback) {
                        if (!this._events[event]) {
                            this._events[event] = [];
                        }
                        this._events[event].push(callback);
                        this._eventsCount++;
                    },
                    
                    emit: function(event, ...args) {
                        if (this._events[event]) {
                            this._events[event].forEach(callback => {
                                try {
                                    callback(...args);
                                } catch (error) {
                                    console.error('事件回调执行失败:', error);
                                }
                            });
                        }
                    },
                    
                    removeAllListeners: function(event) {
                        if (event) {
                            delete this._events[event];
                            this._eventsCount--;
                        } else {
                            this._events = {};
                            this._eventsCount = 0;
                        }
                    }
                };
            }
            
            // 创建核心通信对象，对应原版的 #e: r
            function createCoreCommunication() {
                var self = this;
                var messageHandler = null;
                var cleanupCallback = null;
                
                return {
                    start: async function() {
                        console.log('核心通信启动');
                        // 监听来自原生层的 phantomRpcMessage 事件
                        messageHandler = this.handlePhantomMessage.bind(this);
                        window.addEventListener("phantomRpcMessage", messageHandler);
                    },
                    
                    close: async function() {
                        console.log('核心通信关闭');
                        // 移除事件监听器
                        if (messageHandler) {
                            window.removeEventListener("phantomRpcMessage", messageHandler);
                        }
                        // 执行清理回调
                        if (cleanupCallback) {
                            cleanupCallback();
                        }
                    },
                    
                    // 向原生层发送消息
                    postMessage: function(message) {
                        console.log('向原生层发送消息:', message);
                        setTimeout(() => {
                            window.dispatchEvent(new CustomEvent("dappRpcMessage", {
                                detail: message
                            }));
                        }, 0);
                    },
                    
                    // 处理来自原生层的消息
                    handlePhantomMessage: function(event) {
                        console.log('收到原生层消息:', event.detail);
                        // 这里可以处理各种 RPC 消息
                        this.processMessage(event.detail);
                    },
                    
                    // 处理消息内容
                    processMessage: function(message) {
                        try {
                            if (message && typeof message === 'object') {
                                // 根据消息类型处理
                                switch (message.type) {
                                    case 'response':
                                        this.handleResponse(message);
                                        break;
                                    case 'notification':
                                        this.handleNotification(message);
                                        break;
                                    case 'error':
                                        this.handleError(message);
                                        break;
                                    default:
                                        console.log('未知消息类型:', message.type);
                                }
                            }
                        } catch (error) {
                            console.error('处理消息失败:', error);
                        }
                    },
                    
                    // 处理响应消息
                    handleResponse: function(message) {
                        console.log('处理响应消息:', message);
                        // 这里可以触发相应的回调或事件
                    },
                    
                    // 处理通知消息
                    handleNotification: function(message) {
                        console.log('处理通知消息:', message);
                        // 这里可以触发相应的事件
                    },
                    
                    // 处理错误消息
                    handleError: function(message) {
                        console.error('处理错误消息:', message);
                        // 这里可以触发错误事件
                    },
                    
                    // 设置清理回调
                    setCleanupCallback: function(callback) {
                        cleanupCallback = callback;
                    },
                    
                    // 添加消息监听器
                    addListener: function(callback) {
                        console.log('添加通信监听器');
                        this.messageCallback = callback;
                    }
                };
            }
            
            // 创建 Phantom Solana 提供者，对应原版的 r 对象
            function createPhantomSolanaProvider() {
                // 创建事件发射器
                var eventEmitter = createEventEmitter();
                
                // 创建核心通信对象
                var coreComm = createCoreCommunication();
                
                // 创建提供者对象，对应原版的 r
                var provider = r => {
                    // 核心属性，对应原版结构
                    _events: eventEmitter._events,           // Ea
                    _eventsCount: eventEmitter._eventsCount, // 3
                    
                    // 私有属性，对应原版的 #e: r, #t: f, #n: f
                    '#e': coreComm,                          // #e: r
                    '#t': function() {                       // #t: f
                        return this._publicKey;
                    },
                    '#n': async function(e) {                // #n: f
                        console.log('处理通知:', e);
                    },
                    
                    // 其他内部属性
                    _injectionStartMs: Date.now(),
                    _injectionEndMs: null,
                    _publicKey: null,
                    
                    // 公共属性
                    isPhantom: true,
                    isConnected: false,
                    publicKey: null,
                    
                    // 核心方法
                    connect: async function(options = {}) {
                        try {
                            console.log('Phantom Solana: 尝试连接钱包', options);
                            
                            // 模拟连接过程
                            this._publicKey = 'mock_public_key_' + Date.now();
                            this.publicKey = this._publicKey;
                            this.isConnected = true;
                            this._injectionEndMs = Date.now();
                            
                            // 触发连接事件
                            this.emit('connect', { publicKey: this._publicKey });
                            
                            return {
                                publicKey: this._publicKey
                            };
                        } catch (error) {
                            console.error('Phantom Solana 连接失败:', error);
                            throw error;
                        }
                    },
                    
                    disconnect: async function() {
                        try {
                            console.log('Phantom Solana: 断开连接');
                            this._publicKey = null;
                            this.publicKey = null;
                            this.isConnected = false;
                            this.emit('disconnect');
                        } catch (error) {
                            console.error('Phantom Solana 断开连接失败:', error);
                            throw error;
                        }
                    },
                    
                    // 交易相关方法
                    signTransaction: async function(transaction) {
                        if (!this.isConnected) {
                            throw new Error('Wallet not connected');
                        }
                        console.log('Phantom Solana: 签名交易', transaction);
                        return transaction; // 模拟签名
                    },
                    
                    signAllTransactions: async function(transactions = []) {
                        if (!this.isConnected) {
                            throw new Error('Wallet not connected');
                        }
                        console.log('Phantom Solana: 签名多个交易', transactions);
                        return transactions.map(tx => tx); // 模拟签名
                    },
                    
                    signAndSendTransaction: async function(transaction, options = {}) {
                        if (!this.isConnected) {
                            throw new Error('Wallet not connected');
                        }
                        console.log('Phantom Solana: 签署并发送交易', transaction, options);
                        return {
                            signature: 'mock_signature_' + Date.now(),
                            publicKey: this._publicKey
                        };
                    },
                    
                    signAndSendAllTransactions: async function(transactions, options = {}) {
                        if (!this.isConnected) {
                            throw new Error('Wallet not connected');
                        }
                        console.log('Phantom Solana: 签署并发送所有交易', transactions, options);
                        return transactions.map(tx => ({
                            signature: 'mock_signature_' + Date.now(),
                            publicKey: this._publicKey
                        }));
                    },
                    
                    // 消息签名
                    signMessage: async function(message, encoding = "utf8") {
                        if (!this.isConnected) {
                            throw new Error('Wallet not connected');
                        }
                        console.log('Phantom Solana: 签名消息', message, encoding);
                        return {
                            signature: 'mock_signature_' + Date.now(),
                            publicKey: this._publicKey
                        };
                    },
                    
                    // 登录方法
                    signIn: async function(options) {
                        if (!this.isConnected) {
                            throw new Error('Wallet not connected');
                        }
                        console.log('Phantom Solana: 登录', options);
                        return {
                            publicKey: this._publicKey,
                            signature: 'mock_signature_' + Date.now()
                        };
                    },
                    
                    // 通用请求方法，对应原版的 request
                    request: async function({method, params}) {
                        console.log('Phantom Solana: 请求', method, params);
                        
                        switch (method) {
                            case 'connect':
                                return await this.connect(params);
                            case 'disconnect':
                                return await this.disconnect();
                            case 'signTransaction':
                                return await this.signTransaction(params);
                            case 'signAllTransactions':
                                return await this.signAllTransactions(params);
                            case 'signAndSendTransaction':
                                return await this.signAndSendTransaction(params.transaction, params.options);
                            case 'signMessage':
                                return await this.signMessage(params.message, params.encoding);
                            case 'signIn':
                                return await this.signIn(params);
                            default:
                                throw new Error('Unknown method: ' + method);
                        }
                    },
                    
                    // 通知处理方法
                    handleNotification: async function(notification) {
                        console.log('Phantom Solana: 处理通知', notification);
                        // 处理各种通知
                    }
                });
                
                // 启动核心通信
                coreComm.start();
                
                return provider;
            }
            
            // 注入函数，对应原始代码中的 ip.inject(r)
            function injectProvider(config) {
                console.log('注入 Phantom Solana 提供者，配置:', config);
                
                // 创建提供者实例（r 对象）
                var solanaProvider = createPhantomSolanaProvider();
                
                // 注入到 window.phantom.solana = r
                if (typeof window.phantom === 'undefined') {
                    window.phantom = {};
                }
                window.phantom.solana = solanaProvider; // 直接赋值 r 对象
                
                console.log('Phantom Solana 提供者注入成功');
                console.log('注入结构:', {
                    'window.phantom.solana': window.phantom.solana,
                    'isPhantom': window.phantom.solana.isPhantom,
                    '_events': window.phantom.solana._events,
                    '_eventsCount': window.phantom.solana._eventsCount,
                    '#e': window.phantom.solana['#e'],
                    '#t': window.phantom.solana['#t'],
                    '#n': window.phantom.solana['#n']
                });
                
                return solanaProvider;
            }
            
            // 注册钱包到钱包标准，对应原始代码中的 Jo(new cp(window.phantom.solana))
            function registerWalletStandard(provider) {
                try {
                    // 创建钱包信息对象
                    var walletInfo = {
                        name: 'Phantom Solana',
                        url: 'https://phantom.app',
                        icon: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA4IiBoZWlnaHQ9IjEwOCIgdmlld0JveD0iMCAwIDEwOCAxMDgiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMDgiIGhlaWdodD0iMTA4IiByeD0iMjYiIGZpbGw9IiNBQjlGRjIiLz4KPHBhdGggZmlsbC1ydWxlPSJldmVub2RkIiBjbGlwLXJ1bGU9ImV2ZW5vZGQiIGQ9Ik00Ni41MjY3IDY5LjkyMjlDNDIuMDA1NCA3Ni44NTA5IDM0LjQyOTIgODUuNjE4MiAyNC4zNDggODUuNjE4MkMxOS41ODI0IDg1LjYxODIgMTUgODMuNjU2MyAxNSA3NS4xMzQyQzE1IDUzLjQzMDUgNDQuNjMyNiAxOS44MzI3IDcyLjEyNjggMTkuODMyN0M4Ny43NjggMTkuODMyNyA5NCAzMC42ODQ2IDk0IDQzLjAwNzlDOTQgNTguODI1OCA4My43MzU1IDc2LjkxMjIgNzMuNTMyMSA3Ni45MTIyQzcwLjI5MzkgNzYuOTEyMiA2OC43MDUzIDc1LjEzNDIgNjguNzA1MyA3Mi4zMTRDNjguNzA1MyA3MS41NzgzIDY4LjgyNzUgNzAuNzgxMiA2OS4wNzE5IDY5LjkyMjlDNjUuNTg5MyA3NS44Njk5IDU4Ljg2ODUgODEuMzg3OCA1Mi41NzU0IDgxLjM4NzhDNDcuOTkzIDgxLjM4NzggNDUuNjcxMyA3OC41MDYzIDQ1LjY3MTMgNzQuNDU5OEM0NS42NzEzIDcyLjk4ODQgNDUuOTc2OCA3MS40NTU2IDQ2LjUyNjcgNjkuOTIyOVpNODMuNjc2MSA0Mi41Nzk0QzgzLjY3NjEgNDYuMTcwNCA4MS41NTc1IDQ3Ljk2NTggNzkuMTg3NSA0Ny45NjU4Qzc2Ljc4MTYgNDcuOTY1OCA3NC42OTg5IDQ2LjE3MDQgNzQuNjk4OSA0Mi41Nzk0Qzc0LjY5ODkgMzguOTg4NSA3Ni43ODE2IDM3LjE5MzEgNzkuMTg3NSAzNy4xOTMxQzgxLjU1NzUgMzcuMTkzMSA4My42NzYxIDM4Ljk4ODUgODMuNjc2MSA0Mi41Nzk0Wk03MC4yMTAzIDQyLjU3OTVDNzAuMjEwMyA0Ni4xNzA0IDY4LjA5MTYgNDcuOTY1OCA2NS43MjE2IDQ3Ljk2NThDNjMuMzE1NyA0Ny45NjU4IDYxLjIzMyA0Ni4xNzA0IDYxLjIzMyA0Mi41Nzk1QzYxLjIzMyAzOC45ODg1IDYzLjMxNTcgMzcuMTkzMSA2NS43MjE2IDM3LjE5MzFDNjguMDkxNiAzNy4xOTMxIDcwLjIxMDMgMzguOTg4NSA3MC4yMTAzIDQyLjU3OTVaIiBmaWxsPSIjRkZGREY4Ii8+Cjwvc3ZnPgo=',
                        version: '1.0.0'
                    };
                    
                    // 注册到钱包标准
                    if (typeof window.dispatchEvent === 'function') {
                        var event = new CustomEvent('wallet-standard:register-wallet', {
                            detail: {
                                info: walletInfo,
                                provider: provider
                            }
                        });
                        window.dispatchEvent(event);
                        console.log('Phantom Solana 已注册到钱包标准');
                    }
                    
                    return provider;
                } catch (error) {
                    console.error('注册钱包标准失败:', error);
                    return provider;
                }
            }
            
            // 执行注入流程
            try {
                // 第一步：注入提供者（对应 ip.inject(r)）
                var provider = injectProvider(config);
                
                // 第二步：注册到钱包标准（对应 Jo(new cp(window.phantom.solana))）
                var registeredProvider = registerWalletStandard(provider);
                
                console.log('Phantom Solana 注入完成');
                console.log('最终结构:', {
                    'window.phantom.solana': window.phantom.solana,
                    'isPhantom': window.phantom.solana.isPhantom,
                    'isConnected': window.phantom.solana.isConnected,
                    'publicKey': window.phantom.solana.publicKey,
                    '_events': window.phantom.solana._events,
                    '_eventsCount': window.phantom.solana._eventsCount,
                    '#e': window.phantom.solana['#e'],
                    '#t': window.phantom.solana['#t'],
                    '#n': window.phantom.solana['#n']
                });
                
            } catch (error) {
                console.error('Phantom Solana 注入失败:', error);
            }
            
        }();
        """.trimIndent()
    }
    
    /**
     * Solana 配置类
     */
    data class SolanaConfig(
        val network: String = "mainnet-beta",
        val rpcUrl: String = "https://api.mainnet-beta.solana.com",
        val commitment: String = "confirmed",
        val debug: Boolean = true
    )

    /**
     * 将字节数组转换为十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexString = StringBuilder()
        for (byte in bytes) {
            hexString.append(String.format("%02X", byte))
        }
        return hexString.toString()
    }
}