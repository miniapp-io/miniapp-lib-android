

package io.miniapp.core.openplatform.common.network.utils

import java.net.URL

internal fun String.isValidUrl(): Boolean {
    return try {
        URL(this)
        true
    } catch (t: Throwable) {
        false
    }
}

/**
 * Ensure string starts with "http". If it is not the case, "https://" is added, only if the String is not empty
 */
internal fun String.ensureProtocol(): String {
    return when {
        isEmpty() -> this
        !startsWith("http") -> "https://$this"
        else -> this
    }
}

/**
 * Ensure string ends with "/", if not empty.
 */
internal fun String.ensureTrailingSlash(): String {
    return when {
        isEmpty() -> this
        !endsWith("/") -> "$this/"
        else -> this
    }
}


interface Lazy<T> {
    /**
     * Return the underlying value, computing the value if necessary. All calls to
     * the same `Lazy` instance will return the same result.
     */
    fun lazyGet(): T
}
