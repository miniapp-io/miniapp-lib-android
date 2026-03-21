package io.miniapp.core.openplatform

import android.annotation.SuppressLint
import android.content.Context
import io.miniapp.core.openplatform.common.data.LRUSharedPreferencesCache
import io.miniapp.core.openplatform.common.data.OpenServiceRepository
import io.miniapp.core.openplatform.common.data.SessionProvider
import io.miniapp.core.openplatform.common.network.OkHttpClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object AuthManager {
    private val _cacheKey = "__open_platform_verifier_data"
    @SuppressLint("StaticFieldLeak")
    private var _sessionProvider: SessionProvider? = null
    private val sessionLock = ReentrantLock()

    private val isRefreshing = AtomicBoolean(false)

    private val repository by lazy {
        OpenServiceRepository.getInstance()
    }

    private var verifier: String? = null
    private var apiHost: String? = null
    private var isDev: Boolean = false
    private var idTokenProvider: (suspend () -> String)? = null

    var _refreshToken: (()-> String?)? = null

    fun init(
        context: Context,
        verifier: String,
        apiHost: String,
        isDev: Boolean = false,
        idTokenProvider: suspend () -> String
    ) {
        this.verifier = verifier
        this.apiHost = apiHost
        this.isDev = isDev
        this.idTokenProvider = idTokenProvider

        OkHttpClientProvider.isDev = isDev
        OkHttpClientProvider.apiHost = apiHost

        sessionLock.withLock {
            if (this._sessionProvider == null) {
                this._sessionProvider = SessionProvider(context)
                LRUSharedPreferencesCache.init(context)
            }
        }

        this._refreshToken = {
            runBlocking {
                refreshToken().getOrNull()
            }
        }
    }

    @ThrowsIllegalStateException
    suspend fun signIn(): Result<String> = withContext(Dispatchers.IO) {
        try {
            waitForRequest()

            if (performAuth(true) is AuthResult.Success) {
                Result.success("Authentication ok")
            } else {
                Result.failure(IllegalStateException("Authentication failed"))
            }
        } catch (e: Exception) {
            cleanupRefreshOperation()
            Result.failure(e)
        }
    }

    private suspend fun waitForRequest() = withContext(Dispatchers.IO)  {
        try {
            withTimeout(60_000) {
                while (isRefreshing.get()) {
                    delay(300)
                }
            }
        } catch (e: TimeoutCancellationException) {
            isRefreshing.set(false)
            throw e
        }
    }

    private suspend fun cleanupRefreshOperation() {
        isRefreshing.set(false)
    }

    private suspend fun performAuth(forceRefresh: Boolean): AuthResult {
        val verifier = this.verifier ?: throw IllegalStateException("AuthManager not initialized")
        val idTokenProvider = this.idTokenProvider ?: throw IllegalStateException("ID Token provider not configured")

        val idToken = if (forceRefresh) {
            LRUSharedPreferencesCache.saveValue(_cacheKey, null)
            idTokenProvider.invoke().also {
                LRUSharedPreferencesCache.saveValue(_cacheKey, it)
            }
        } else {
            LRUSharedPreferencesCache.getValue(_cacheKey) ?: idTokenProvider.invoke().also {
                LRUSharedPreferencesCache.saveValue(_cacheKey, it)
            }
        }

        isRefreshing.set(false)

        return try {
            var authResult: AuthResult? = null
            repository.auth(verifier, idToken)
                .catch { e ->
                    authResult = when (e) {
                        is HttpException -> {
                            if (e.code() == 401 && !forceRefresh) {
                                LRUSharedPreferencesCache.saveValue(_cacheKey, null)
                                performAuth(true)
                            } else {
                                AuthResult.Error(e)
                            }
                        }
                        else -> {
                            AuthResult.Error(e)
                        }
                    }
                }
                .collect { verifierDto ->
                    authResult = AuthResult.Success(verifierDto.accessToken)
                }

            return authResult ?: AuthResult.Error(IllegalStateException("Authentication failed"))
        } catch (e: Exception) {
            AuthResult.Error(e)
        } finally {
            cleanupRefreshOperation()
        }
    }

    private suspend fun refreshToken(): Result<String?> = withContext(Dispatchers.IO) {

        waitForRequest()

        if (isAuth()) {
            Result.success(_sessionProvider?.token)
        } else {
            try {
                when (val tokenResult = performAuth(true)) {
                    is AuthResult.Success -> {
                        sessionLock.withLock {
                            _sessionProvider?.token = tokenResult.token
                        }
                        Result.success(tokenResult.token)
                    }
                    is AuthResult.Error -> {
                        Result.failure(tokenResult.exception)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                cleanupRefreshOperation()
            }
        }
    }

    fun isAuth(): Boolean {
        return sessionLock.withLock {
            true == _sessionProvider?.isAuth()
        }
    }

    fun getToken(): String? {
        return sessionLock.withLock {
            _sessionProvider?.token
        }
    }

    fun clearToken() {
        sessionLock.withLock {
            _sessionProvider?.token = null
        }
    }

    suspend fun signOut() {
        sessionLock.withLock {
            _sessionProvider?.token = null
        }
        isRefreshing.set(false)
        LRUSharedPreferencesCache.saveValue(_cacheKey, null)
    }

    private sealed class AuthResult {
        data class Success(val token: String) : AuthResult()
        data class Error(val exception: Throwable) : AuthResult()
    }
}