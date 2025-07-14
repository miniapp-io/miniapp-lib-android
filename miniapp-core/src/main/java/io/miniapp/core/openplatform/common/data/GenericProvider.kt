package io.miniapp.core.openplatform.common.data

internal fun interface GenericProvider<F, T> {
    fun provide(obj: F): T
}