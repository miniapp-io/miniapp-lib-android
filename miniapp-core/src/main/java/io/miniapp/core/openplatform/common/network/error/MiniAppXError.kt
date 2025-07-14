package io.miniapp.core.openplatform.common.network.error

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MiniAppXError(val code:Int, val error: String?): Throwable()