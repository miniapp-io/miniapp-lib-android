package io.miniapp.core.openplatform.common.network.error

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenWeb3Error(val code:Int, val error: String?): Throwable()