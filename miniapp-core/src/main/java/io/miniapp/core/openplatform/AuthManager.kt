package io.miniapp.core.openplatform

import android.annotation.SuppressLint
import android.content.Context
import io.miniapp.core.openplatform.common.data.LRUSharedPreferencesCache
import io.miniapp.core.openplatform.common.data.OpenServiceRepository
import io.miniapp.core.openplatform.common.data.SessionProvider
import io.miniapp.core.openplatform.common.network.OkHttpClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object AuthManager {
    private val _cacheKey = "__open_platform_verifier_data"
    @SuppressLint("StaticFieldLeak")
    private var _sessionProvider: SessionProvider? = null
    private val sessionLock = ReentrantLock()

    private val refreshState = MutableStateFlow(false)

    private val repository by lazy {
        OpenServiceRepository.getInstance()
    }

    private var verifier: String? = null
    private var apiHost: String? = null
    private var isDev: Boolean = false
    private var idTokenProvider: (suspend () -> String)? = null

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
    }

    suspend fun refreshToken(): String? {
        return withTimeout(60_000) {
            fetchToken().getOrNull()
        }
    }

    @ThrowsIllegalStateException
    suspend fun signIn(): Result<String?> = withContext(Dispatchers.IO) {
        withTimeout(60_000) {
            fetchToken()
        }
    }

    private suspend fun waitForRequest() = withContext(Dispatchers.IO) {
        try {
            withTimeout(60_000) {
                refreshState
                    .filter { !it }
                    .first()
            }
        } catch (e: TimeoutCancellationException) {
            cleanupRefreshOperation()
            e.printStackTrace()
        }
    }

    private fun cleanupRefreshOperation() {
        refreshState.value = false
    }

    private suspend fun setRefreshOperation() {
        refreshState.value = true
    }

    private suspend fun getIdToken(forceRefresh: Boolean): String {
        val idTokenProvider = this.idTokenProvider ?: throw IllegalStateException("ID Token provider not configured")
        return if (forceRefresh) {
            LRUSharedPreferencesCache.saveValue(_cacheKey, null)
            idTokenProvider.invoke().also {
                LRUSharedPreferencesCache.saveValue(_cacheKey, it)
            }
        } else {
            LRUSharedPreferencesCache.getValue(_cacheKey) ?: idTokenProvider.invoke().also {
                LRUSharedPreferencesCache.saveValue(_cacheKey, it)
            }
        }
    }

    private suspend fun performAuth(forceRefresh: Boolean): AuthResult {

        var currentForceRefresh = forceRefresh
        var retryCount = 3

        while (retryCount > 0) {
            try {
                val verifier = this.verifier ?: return AuthResult.Error(IllegalStateException("AuthManager not initialized"))
                val idToken = getIdToken(currentForceRefresh)
                val verifierDto = repository.auth(verifier, idToken).first()
                return AuthResult.Success(verifierDto.accessToken)
            } catch (e: HttpException) {
                when {
                    e.code() == 401 && !currentForceRefresh -> {
                        currentForceRefresh = true
                        retryCount--
                        continue
                    }
                    else -> return AuthResult.Error(e)
                }
            } catch (e: Exception) {
                return AuthResult.Error(e)
            }
        }

        return AuthResult.Error(IllegalStateException("Authentication failed"))
    }

    private suspend fun fetchToken(): Result<String?> = withContext(Dispatchers.IO) {
        waitForRequest()

        val isAuthenticated = sessionLock.withLock { isAuth() }

        setRefreshOperation()

        try {
            if (isAuthenticated) {
                val token = sessionLock.withLock { _sessionProvider?.token }
                Result.success(token)
            } else {
                when (val tokenResult = performAuth(true)) {
                    is AuthResult.Success -> {
                        sessionLock.withLock {
                            _sessionProvider?.token = tokenResult.token
                        }
                        Result.success(tokenResult.token)
                    }
                    is AuthResult.Error -> Result.failure(tokenResult.exception)
                }
            }
        } finally {
            cleanupRefreshOperation()
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
        cleanupRefreshOperation()
        LRUSharedPreferencesCache.saveValue(_cacheKey, null)
    }

    private sealed class AuthResult {
        data class Success(val token: String) : AuthResult()
        data class Error(val exception: Throwable) : AuthResult()
    }
}