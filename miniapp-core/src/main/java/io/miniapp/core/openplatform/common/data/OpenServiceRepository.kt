package io.miniapp.core.openplatform.common.data

import io.miniapp.core.openplatform.OpenPlatformPluginImpl
import io.miniapp.core.openplatform.common.apis.data.CustomMethodsParams
import io.miniapp.core.openplatform.common.apis.data.DAppDto
import io.miniapp.core.openplatform.common.apis.data.InlineButtonCallbackParams
import io.miniapp.core.openplatform.common.apis.data.LaunchParams
import io.miniapp.core.openplatform.common.apis.data.MiniAppDto
import io.miniapp.core.openplatform.common.network.MoshiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

internal class OpenServiceRepository private constructor(): IOpenServiceDataSource {

    companion object {
        private val _instance = OpenServiceRepository()
        fun getInstance(): IOpenServiceDataSource = _instance
    }

    private val remoteDataSource by lazy {
        OpenServiceRemoteDataSource(OpenPlatformPluginImpl.getInstance().sessionProvider)
    }

    override suspend fun auth(verifier: String, idToken: String) =
        remoteDataSource.auth(verifier, idToken)

    override suspend fun getBotInfo(botIdOrName: String) =
        remoteDataSource.getBotInfo(botIdOrName)

    override suspend fun inlineCallback(params: InlineButtonCallbackParams) =
        remoteDataSource.inlineCallback(params)

    override suspend fun requestLaunchInfo(params: LaunchParams) =
        remoteDataSource.requestLaunchInfo(params)

    override suspend fun requestMiniApp(appId: String): Flow<MiniAppDto> {
        val cacheKey = "miniapp_${appId}"

        return LRUSharedPreferencesCache.getValue(cacheKey)?.let {
            flow {
                var emitResult = false
                try {
                    val data = MoshiProvider.fromJson<MiniAppDto>(it)!!
                    emit(data)
                    emitResult = true
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                withContext(Dispatchers.IO) {
                    remoteDataSource.requestMiniApp(appId).collect { app ->
                        LRUSharedPreferencesCache.saveValue(app.spKey(), appId)
                        LRUSharedPreferencesCache.saveValue(cacheKey, MoshiProvider.toJson(app))
                        if (!emitResult) {
                            emit(app)
                        }
                    }
                }
            }
        } ?: remoteDataSource.requestMiniApp(appId).onEach {
            // Save request response data to cache
            LRUSharedPreferencesCache.saveValue(it.spKey(), appId)
            LRUSharedPreferencesCache.saveValue(cacheKey, MoshiProvider.toJson(it))
        }
    }

    private fun MiniAppDto.spKey() : String {
        return "${this.botName}_${this.identifier}"
    }

    override suspend fun requestMiniApp(appName: String, botIdOrName: String): Flow<MiniAppDto> {
        return LRUSharedPreferencesCache.getValue("${botIdOrName}_${appName}")?.let {
            requestMiniApp(it)
        } ?: remoteDataSource.requestMiniApp(appName, botIdOrName).onEach {
            LRUSharedPreferencesCache.saveValue(it.spKey(), it.id ?: "0")
            LRUSharedPreferencesCache.saveValue(
                "miniapp_${it.id}",
                MoshiProvider.toJson(it)
            )
        }
    }

    override suspend fun batchRequestMiniApp(appIds: List<String>) =
        remoteDataSource.batchRequestMiniApp(appIds).onEach {
            it.items?.forEach { app->
                LRUSharedPreferencesCache.saveValue(app.spKey(), app.id ?: "0")
                LRUSharedPreferencesCache.saveValue("miniapp_${app.id}", MoshiProvider.toJson(app))
            }
        }

    override suspend fun requestDApp(dAppId: String): Flow<DAppDto> {
        val cacheKey = "dapp_${dAppId}"

        return LRUSharedPreferencesCache.getValue(cacheKey)?.let { cachedData ->
            flow {
                var emitResult = false
                try {
                    val data = MoshiProvider.fromJson<DAppDto>(cachedData)!!
                    emit(data)
                    emitResult = true
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

                remoteDataSource.requestDApp(dAppId).collect { dApp ->
                    LRUSharedPreferencesCache.saveValue(
                        cacheKey,
                        MoshiProvider.toJson(dApp)
                    )
                    if (!emitResult) {
                        emit(dApp)
                    }
                }
            }
        } ?: remoteDataSource.requestDApp(dAppId).onEach { dApp ->
            LRUSharedPreferencesCache.saveValue(
                cacheKey,
                MoshiProvider.toJson(dApp)
            )
        }
    }

    override suspend fun invokeCustomMethod(params: CustomMethodsParams) =
        remoteDataSource.invokeCustomMethod(params)

    override suspend fun requestDAppLaunchUrl(url: String, id: String?) =
        remoteDataSource.requestDAppLaunchUrl(url, id)
}