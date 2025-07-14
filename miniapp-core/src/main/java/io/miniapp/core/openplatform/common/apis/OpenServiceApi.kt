package io.miniapp.core.openplatform.common.apis

import io.miniapp.core.openplatform.common.apis.constants.ApiConstants
import io.miniapp.core.openplatform.common.apis.data.AuthParams
import io.miniapp.core.openplatform.common.apis.data.BotDto
import io.miniapp.core.openplatform.common.apis.data.BotMenusResponse
import io.miniapp.core.openplatform.common.apis.data.CustomMethodsParams
import io.miniapp.core.openplatform.common.apis.data.CustomMethodsResp
import io.miniapp.core.openplatform.common.apis.data.DAppDto
import io.miniapp.core.openplatform.common.apis.data.InlineButtonCallbackParams
import io.miniapp.core.openplatform.common.apis.data.LaunchDAppDto
import io.miniapp.core.openplatform.common.apis.data.LaunchMiniAppDto
import io.miniapp.core.openplatform.common.apis.data.LaunchParams
import io.miniapp.core.openplatform.common.apis.data.MiniAppDto
import io.miniapp.core.openplatform.common.apis.data.MiniAppResponse
import io.miniapp.core.openplatform.common.apis.data.VerifierDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface OpenServiceApi {

    /**
     * request mini app launch url
     */
    @POST(ApiConstants.URI_REQUEST_VERIFIER)
    suspend fun verifier(@Body params: AuthParams): VerifierDto

    /**
     * request bot extend info by id or name
     */
    @GET(ApiConstants.URI_REQUEST_BOT_INFO)
    suspend fun getBotInfo(@Path("idOrName") idOrName: String): BotDto

    /**
     * request mini app launch url
     */
    @POST(ApiConstants.URI_REQUEST_LAUNCH_URL)
    suspend fun launch(@Body params: LaunchParams): LaunchMiniAppDto

    /**
     * request mini app info by id
     */
    @GET(ApiConstants.URI_MINI_APP_INFO_BY_ID)
    suspend fun getMiniAppInfo(@Path("id") id: String): MiniAppDto

    /**
     * request mini app info by names
     */
    @GET(ApiConstants.URI_REQUEST_APP_INFO_BY_NAME)
    suspend fun getMiniAppInfo(@Path("idOrName") botIdOrName: String, @Path("appName") appName: String ): MiniAppDto

    /**
     * batch request mini-app
     */
    @GET(ApiConstants.URI_BATCH_MINI_APP)
    suspend fun batchGetMiniApp(@Query("id") appIds: List<String>): MiniAppResponse

    /**
     * request d app info by id
     */
    @GET(ApiConstants.URI_D_APP_INFO_BY_ID)
    suspend fun getDAppInfo(@Path("id") id: String): DAppDto

    /**
     * request d app info by id
     */
    @GET(ApiConstants.URI_D_APP_LAUNCH)
    suspend fun getDAppLaunchInfo(@Query("targetUrl") targetUrl: String, @Query("targetId") targetId: String?): LaunchDAppDto

    /**
     * request bot menus
     */
    @GET(ApiConstants.URI_BOT_MENUS)
    suspend fun getBotMenus(): BotMenusResponse

    /**
     * invoke custom method
     */
    @POST(ApiConstants.URI_INVOKE_CUSTOM_METHODS)
    suspend fun invokeCustomMethod(@Body params: CustomMethodsParams): CustomMethodsResp

    /**
     * inline button callback
     */
    @POST(ApiConstants.URI_INLINE_CALLBACK)
    suspend fun inlineButtonCallback(@Body params: InlineButtonCallbackParams)

}