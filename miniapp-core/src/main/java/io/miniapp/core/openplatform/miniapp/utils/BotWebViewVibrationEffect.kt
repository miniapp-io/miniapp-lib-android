package io.miniapp.core.openplatform.miniapp.utils

import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.RequiresApi

internal enum class BotWebViewVibrationEffect(
    val timings: LongArray,
    val amplitudes: IntArray,
    val fallbackTimings: LongArray
) {
    IMPACT_LIGHT(longArrayOf(7), intArrayOf(65), longArrayOf(60)),
    IMPACT_MEDIUM(longArrayOf(7), intArrayOf(145), longArrayOf(70)),
    IMPACT_HEAVY(longArrayOf(7), intArrayOf(255), longArrayOf(80)),
    IMPACT_RIGID(longArrayOf(3), intArrayOf(225), longArrayOf(50)),
    IMPACT_SOFT(longArrayOf(10), intArrayOf(175), longArrayOf(55)),
    NOTIFICATION_ERROR(
        longArrayOf(14, 48, 14, 48, 14, 48, 20),
        intArrayOf(200, 0, 200, 0, 255, 0, 145),
        longArrayOf(40, 60, 40, 60, 65, 60, 40)
    ),
    NOTIFICATION_SUCCESS(longArrayOf(14, 65, 14), intArrayOf(175, 0, 255), longArrayOf(50, 60, 65)),
    NOTIFICATION_WARNING(longArrayOf(14, 64, 14), intArrayOf(225, 0, 175), longArrayOf(65, 60, 40)),
    SELECTION_CHANGE(longArrayOf(1), intArrayOf(65), longArrayOf(30)),
    APP_ERROR(
        longArrayOf(30, 10, 150, 10),
        intArrayOf(0, 100, 0, 100),
        longArrayOf(40, 60, 40, 60, 65, 60, 40)
    );

    private var vibrationEffect: Any? = null

    @get:RequiresApi(Build.VERSION_CODES.O)
    val vibrationEffectForOreo: VibrationEffect?
        get() {
            if (vibrationEffect == null) {
                vibrationEffect = if (false== AndroidUtils.getVibrator()?.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(fallbackTimings, -1)
                } else {
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                }
            }
            return vibrationEffect as VibrationEffect?
        }

    fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AndroidUtils.getVibrator()?.vibrate(vibrationEffectForOreo)
        } else {
            AndroidUtils.getVibrator()?.vibrate(fallbackTimings, -1)
        }
    }
}
