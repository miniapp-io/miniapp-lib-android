package io.miniapp.core.openplatform.common.data

import androidx.dynamicanimation.animation.FloatPropertyCompat

internal class SimpleFloatPropertyCompat<T>(
    name: String?,
    private val getter: Getter<T>,
    private val setter: Setter<T>
) :
    FloatPropertyCompat<T>(name) {
    private var multiplier = 1f

    fun setMultiplier(multiplier: Float): SimpleFloatPropertyCompat<T> {
        this.multiplier = multiplier
        return this
    }

    override fun getValue(`object`: T): Float {
        return getter[`object`] * multiplier
    }

    override fun setValue(`object`: T, value: Float) {
        setter[`object`] = value / multiplier
    }

    interface Getter<T> {
        operator fun get(obj: T): Float
    }

    interface Setter<T> {
        operator fun set(obj: T, value: Float)
    }
}
