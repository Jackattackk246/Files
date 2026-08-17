package com.example.util

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * HapticManager - Unified vibration and tactile feedback management utility class.
 * Delivers precise, subtle tactile responses for file item selections, navigation node switches,
 * modal dialog triggers, and theme list item interactions.
 */
class HapticManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Subtle micro-tick on file item click / list selection (10ms low amplitude).
     */
    fun performSelectionTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(10L, 35))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Distinct crisp click for tab navigation, drawer items, and menu actions.
     */
    fun performNavigationClick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(15L, 60))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Subtle double-pulse feedback on theme activation and style profile switching.
     */
    fun performThemeSwitchPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 15, 30, 20)
                val amplitudes = intArrayOf(0, 70, 0, 110)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Heavy tactile pulse on long press or multi-selection activation.
     */
    fun performLongPressHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(25L, 120))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25L)
            }
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: HapticManager? = null

        fun from(context: Context): HapticManager {
            return instance ?: synchronized(this) {
                instance ?: HapticManager(context.applicationContext).also { instance = it }
            }
        }

        fun selectionTick(context: Context) {
            from(context).performSelectionTick()
        }

        fun navigationClick(context: Context) {
            from(context).performNavigationClick()
        }

        fun themeSwitchPulse(context: Context) {
            from(context).performThemeSwitchPulse()
        }

        fun longPress(context: Context) {
            from(context).performLongPressHaptic()
        }
    }
}
