package io.miniapp.core.openplatform.common.data

import io.miniapp.core.openplatform.common.apis.OpenServiceApi
import io.miniapp.core.openplatform.common.apis.data.AuthParams
import io.miniapp.core.openplatform.common.apis.data.CustomMethodsParams
import io.miniapp.core.openplatform.common.apis.data.InlineButtonCallbackParams
import io.miniapp.core.openplatform.common.apis.data.LaunchParams
import io.miniapp.core.openplatform.common.network.OkHttpClientProvider
import kotlinx.coroutines.flow.flow

internal class OpenServiceRemoteDataSource(provider: ()-> SessionProvider?) : IOpenServiceDataSource {

    private val minAppApi = OkHttpClientProvider.retrofitFactory(provider).create(OpenServiceApi::class.java)
    override suspend fun auth(verifier: String, idToken: String) = flow {
        val data = minAppApi.verifier(AuthParams(verifier = verifier, options = mapOf("id_token" to idToken)))
        emit(data)
    }

    override suspend fun getBotInfo(botIdOrName: String) = flow {
        val data = minAppApi.getBotInfo(botIdOrName)
        emit(data)
    }

    override suspend fun inlineCallback(params: InlineButtonCallbackParams) = flow {
        val data = minAppApi.inlineButtonCallback(params)
        emit(data)
    }

    override suspend fun requestLaunchInfo(params: LaunchParams) = flow {
        val data = minAppApi.launch(params)
        emit(data)
    }

    override suspend fun requestMiniApp(appId: String) = flow {
        val data = minAppApi.getMiniAppInfo(appId)
        emit(data)
    }

    override suspend fun requestMiniApp(appName: String, botIdOrName: String) = flow {
        val data = minAppApi.getMiniAppInfo(botIdOrName, appName)
        emit(data)
    }

    override suspend fun batchRequestMiniApp(appIds: List<String>) = flow {
        val data = minAppApi.batchGetMiniApp(appIds)
        emit(data)
    }

    override suspend fun requestDApp(dAppId: String) = flow {
        val data = minAppApi.getDAppInfo(dAppId)
        emit(data)
    }

    override suspend fun invokeCustomMethod(params: CustomMethodsParams) = flow {
        val data = minAppApi.invokeCustomMethod(params)
        emit(data.result)
    }

    override suspend fun requestDAppLaunchUrl(url: String, id: String?) = flow {
        val data = minAppApi.getDAppLaunchInfo(url, id)
        emit(data)
    }
}