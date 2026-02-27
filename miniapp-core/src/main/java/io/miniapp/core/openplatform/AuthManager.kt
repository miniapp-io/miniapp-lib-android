package io.miniapp.core.openplatform

import android.content.Context
import androidx.annotation.GuardedBy
import io.miniapp.core.openplatform.common.data.LRUSharedPreferencesCache
import io.miniapp.core.openplatform.common.data.OpenServiceRepository
import io.miniapp.core.openplatform.common.data.SessionProvider
import io.miniapp.core.openplatform.common.network.OkHttpClientProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object AuthManager {
    private val _cacheKey = "__open_platform_verifier_data"
    private var _sessionProvider: SessionProvider? = null
    private val sessionLock = ReentrantLock()

    @GuardedBy("refreshLock")
    private var isRefreshing = false
    private val refreshLock = ReentrantLock()

    private var currentRefreshOperation: CompletableDeferred<String?>? = null

    private val repository by lazy {
        OpenServiceRepository.getInstance()
    }

    private var verifier: String? = null
    private var apiHost: String? = null
    private var isDev: Boolean = false
    private var idTokenProvider: (suspend () -> String)? = null

    var _refreshToken: (suspend ()-> String?)? = null

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
    suspend fun signIn(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sessionLock.withLock {
                if (true == _sessionProvider?.isAuth()) {
                    return@withContext Result.success(Unit)
                }
            }

            authenticate()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun authenticate(forceRefresh: Boolean = false) {
        val refreshOperation = getOrCreateRefreshOperation(forceRefresh)

        if (refreshOperation != null) {
            val token = refreshOperation.await()
            sessionLock.withLock {
                _sessionProvider?.token = token
            }
            return
        }

        performNewAuthentication(forceRefresh)
    }

    private suspend fun getOrCreateRefreshOperation(forceRefresh: Boolean): CompletableDeferred<String?>? {
        refreshLock.withLock {
            if (isRefreshing && !forceRefresh) {
                return currentRefreshOperation
            }

            if (forceRefresh || !isRefreshing) {
                isRefreshing = true
                currentRefreshOperation = CompletableDeferred()
                return null
            }

            return currentRefreshOperation
        }
    }

    private suspend fun performNewAuthentication(forceRefresh: Boolean) {
        try {
            val tokenResult = performAuth(forceRefresh)

            when (tokenResult) {
                is AuthResult.Success -> {
                    sessionLock.withLock {
                        _sessionProvider?.token = tokenResult.token
                    }
                    completeRefreshOperation(tokenResult.token)
                }
                is AuthResult.Error -> {
                    completeRefreshOperationWithError(tokenResult.exception)
                    throw tokenResult.exception
                }
            }
        } catch (e: Exception) {
            completeRefreshOperationWithError(e)
            throw e
        } finally {
            cleanupRefreshOperation()
        }
    }

    private fun completeRefreshOperation(token: String?) {
        refreshLock.withLock {
            currentRefreshOperation?.complete(token)
            currentRefreshOperation = null
        }
    }

    private fun completeRefreshOperationWithError(exception: Throwable) {
        refreshLock.withLock {
            currentRefreshOperation?.completeExceptionally(exception)
            currentRefreshOperation = null
        }
    }

    private fun cleanupRefreshOperation() {
        refreshLock.withLock {
            isRefreshing = false
            if (currentRefreshOperation?.isCompleted == true) {
                currentRefreshOperation?.complete(null)
                currentRefreshOperation = null
            }
        }
    }

    private suspend fun performAuth(forceRefresh: Boolean): AuthResult {
        val verifier = this.verifier ?: throw IllegalStateException("AuthManager not initialized")
        val idTokenProvider = this.idTokenProvider ?: throw IllegalStateException("ID Token provider not configured")

        // 获取 ID Token
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
        }
    }

    private suspend fun refreshToken(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val refreshOperation = refreshLock.withLock {
                if (isRefreshing) {
                    currentRefreshOperation
                } else {
                    isRefreshing = true
                    currentRefreshOperation = CompletableDeferred()
                    null
                }
            }

            if (refreshOperation != null) {
                val token = try {
                    refreshOperation.await()
                } catch (e: Exception) {
                    return@withContext refreshToken()
                }
                return@withContext Result.success(token)
            }

            try {
                when (val tokenResult = performAuth(true)) {
                    is AuthResult.Success -> {
                        sessionLock.withLock {
                            _sessionProvider?.token = tokenResult.token
                        }
                        completeRefreshOperation(tokenResult.token)
                        Result.success(tokenResult.token)
                    }
                    is AuthResult.Error -> {
                        completeRefreshOperationWithError(tokenResult.exception)
                        Result.failure(tokenResult.exception)
                    }
                }
            } catch (e: Exception) {
                completeRefreshOperationWithError(e)
                Result.failure(e)
            } finally {
                cleanupRefreshOperation()
            }
        } catch (e: Exception) {
            Result.failure(e)
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

    fun signOut() {
        sessionLock.withLock {
            _sessionProvider?.token = null
        }
        refreshLock.withLock {
            isRefreshing = false
            currentRefreshOperation?.cancel()
            currentRefreshOperation = null
        }
        LRUSharedPreferencesCache.saveValue(_cacheKey, null)
    }

    private sealed class AuthResult {
        data class Success(val token: String) : AuthResult()
        data class Error(val exception: Throwable) : AuthResult()
    }
}