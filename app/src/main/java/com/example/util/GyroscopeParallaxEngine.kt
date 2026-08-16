package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.launch

/**
 * Direct View-based GyroscopeParallaxEngine for standard Android Views.
 */
class GyroscopeParallaxEngine(private val targetBackgroundCanvas: View) : SensorEventListener {

    private val translationIntensity = 18f

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // Read angular velocity along X and Y hardware axes
            val velocityX = event.values[0]
            val velocityY = event.values[1]

            // Convert velocity vectors directly into real-time layout pixel translation coordinates
            targetBackgroundCanvas.translationX = velocityY * translationIntensity
            targetBackgroundCanvas.translationY = velocityX * translationIntensity
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Calibration stub
    }

    companion object {
        const val MAX_BACKGROUND_OFFSET_DP = 28f
        const val MAX_FOREGROUND_OFFSET_DP = 10f
        private const val SENSITIVITY = 14f
        private const val DECAY_FACTOR = 0.94f

        /**
         * Tracks Gyroscope events and returns reactive ParallaxOffset state for Compose.
         */
        @Composable
        fun rememberParallaxOffset(
            enabled: Boolean = true,
            maxBackgroundOffset: Float = MAX_BACKGROUND_OFFSET_DP,
            maxForegroundOffset: Float = MAX_FOREGROUND_OFFSET_DP
        ): ParallaxOffset {
            val isPreview = LocalInspectionMode.current
            if (isPreview || !enabled) {
                return remember { ParallaxOffset() }
            }

            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            val animBgX = remember { Animatable(0f) }
            val animBgY = remember { Animatable(0f) }
            val animFgX = remember { Animatable(0f) }
            val animFgY = remember { Animatable(0f) }

            var rawRoll by remember { mutableFloatStateOf(0f) }
            var rawPitch by remember { mutableFloatStateOf(0f) }

            DisposableEffect(enabled, context) {
                val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                val gyroscopeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
                    ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                    ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

                var accumulatedX = 0f
                var accumulatedY = 0f
                var lastTimestamp = 0L

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event == null) return

                        val now = event.timestamp
                        val dt = if (lastTimestamp != 0L) {
                            (now - lastTimestamp) * 1.0e-9f
                        } else {
                            0.02f
                        }
                        lastTimestamp = now

                        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                            val gyroPitch = event.values[0]
                            val gyroRoll = event.values[1]

                            accumulatedX = (accumulatedX * DECAY_FACTOR) + (gyroRoll * SENSITIVITY * dt * 60f)
                            accumulatedY = (accumulatedY * DECAY_FACTOR) + (gyroPitch * SENSITIVITY * dt * 60f)

                            val clampedNormX = (accumulatedX / 30f).coerceIn(-1f, 1f)
                            val clampedNormY = (accumulatedY / 30f).coerceIn(-1f, 1f)

                            val targetBgX = -clampedNormX * maxBackgroundOffset
                            val targetBgY = -clampedNormY * maxBackgroundOffset
                            val targetFgX = clampedNormX * maxForegroundOffset
                            val targetFgY = clampedNormY * maxForegroundOffset

                            rawRoll = gyroRoll
                            rawPitch = gyroPitch

                            coroutineScope.launch {
                                animBgX.animateTo(targetBgX, spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy))
                            }
                            coroutineScope.launch {
                                animBgY.animateTo(targetBgY, spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy))
                            }
                            coroutineScope.launch {
                                animFgX.animateTo(targetFgX, spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy))
                            }
                            coroutineScope.launch {
                                animFgY.animateTo(targetFgY, spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy))
                            }
                        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                            val ax = event.values[0] / 9.81f
                            val ay = event.values[1] / 9.81f

                            val targetBgX = (ax * maxBackgroundOffset).coerceIn(-maxBackgroundOffset, maxBackgroundOffset)
                            val targetBgY = (-ay * maxBackgroundOffset).coerceIn(-maxBackgroundOffset, maxBackgroundOffset)
                            val targetFgX = (-ax * maxForegroundOffset).coerceIn(-maxForegroundOffset, maxForegroundOffset)
                            val targetFgY = (ay * maxForegroundOffset).coerceIn(-maxForegroundOffset, maxForegroundOffset)

                            rawRoll = ax
                            rawPitch = ay

                            coroutineScope.launch {
                                animBgX.animateTo(targetBgX, spring(stiffness = Spring.StiffnessLow))
                            }
                            coroutineScope.launch {
                                animBgY.animateTo(targetBgY, spring(stiffness = Spring.StiffnessLow))
                            }
                            coroutineScope.launch {
                                animFgX.animateTo(targetFgX, spring(stiffness = Spring.StiffnessLow))
                            }
                            coroutineScope.launch {
                                animFgY.animateTo(targetFgY, spring(stiffness = Spring.StiffnessLow))
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                gyroscopeSensor?.let { sensor ->
                    sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
                }

                onDispose {
                    sensorManager?.unregisterListener(listener)
                }
            }

            return ParallaxOffset(
                backgroundX = animBgX.value,
                backgroundY = animBgY.value,
                foregroundX = animFgX.value,
                foregroundY = animFgY.value,
                roll = rawRoll,
                pitch = rawPitch
            )
        }
    }
}

/**
 * State representing dynamic 2D canvas shifts driven by Gyroscope device tilt.
 */
@Stable
data class ParallaxOffset(
    val backgroundX: Float = 0f,
    val backgroundY: Float = 0f,
    val foregroundX: Float = 0f,
    val foregroundY: Float = 0f,
    val roll: Float = 0f,
    val pitch: Float = 0f
)
