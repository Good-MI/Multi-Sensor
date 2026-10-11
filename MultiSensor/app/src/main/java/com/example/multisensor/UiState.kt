package com.example.multisensor

import androidx.compose.ui.geometry.Offset

enum class ThemeMode {
    AUTO,
    LIGHT,
    DARK
}

data class SensorAvailability(
    val hasAccelerometer: Boolean = false,
    val hasMagnetometer: Boolean = false,
    val hasLightSensor: Boolean = false
)

data class RawAccelerometer(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)

data class UiState(
    val ballOffset: Offset = Offset.Zero,
    val lux: Float = 0f,
    val isDarkMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val headingDegrees: Float = 0f,
    val cardinal: String = "N",
    val sensorAvailability: SensorAvailability = SensorAvailability(),
    val rawAccelerometer: RawAccelerometer = RawAccelerometer(),
    val isMuted: Boolean = false
)
