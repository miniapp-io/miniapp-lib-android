package io.miniapp.core.openplatform.common.data

import io.miniapp.core.openplatform.common.apis.data.BotDto
import io.miniapp.core.openplatform.common.apis.data.CustomMethodsParams
import io.miniapp.core.openplatform.common.apis.data.DAppDto
import io.miniapp.core.openplatform.common.apis.data.InlineButtonCallbackParams
import io.miniapp.core.openplatform.common.apis.data.LaunchDAppDto
import io.miniapp.core.openplatform.common.apis.data.LaunchMiniAppDto
import io.miniapp.core.openplatform.common.apis.data.LaunchParams
import io.miniapp.core.openplatform.common.apis.data.MiniAppDto
import io.miniapp.core.openplatform.common.apis.data.MiniAppResponse
import io.miniapp.core.openplatform.common.apis.data.VerifierDto
import kotlinx.coroutines.flow.Flow

internal interface IOpenServiceDataSource {
    suspend fun auth(verifier: String, idToken: String): Flow<VerifierDto>
    suspend fun getBotInfo(botIdOrName: String): Flow<BotDto>
    suspend fun inlineCallback(params: InlineButtonCallbackParams): Flow<Unit>
    suspend fun requestLaunchInfo(params: LaunchParams): Flow<LaunchMiniAppDto>
    suspend fun requestMiniApp(appId: String): Flow<MiniAppDto>
    suspend fun requestMiniApp(appName: String, botIdOrName: String): Flow<MiniAppDto>
    suspend fun batchRequestMiniApp(appIds: List<String>): Flow<MiniAppResponse>
    suspend fun requestDApp(dAppId: String): Flow<DAppDto>
    suspend fun invokeCustomMethod(params: CustomMethodsParams): Flow<String?>
    suspend fun requestDAppLaunchUrl(url: String, id: String?): Flow<LaunchDAppDto>
}