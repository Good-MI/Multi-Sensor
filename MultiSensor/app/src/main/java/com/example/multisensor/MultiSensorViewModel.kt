package com.example.multisensor

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

class MultiSensorViewModel(application: Application) : AndroidViewModel(application) {

    val sensorController = SensorController(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var ballVelocityX = 0f
    private var ballVelocityY = 0f
    private var arenaWidth = 0f
    private var arenaHeight = 0f

    // 1. BOUNCE QUOTA & STATE MACHINE VARIABLES
    private var bounceCount = 0
    private var isGroundedToWall = false
    private var previousTiltX = 0f
    private var previousTiltY = 0f

    private var lastCollisionTimeMs = 0L
    private var hasReceivedValidLightReading = false

    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    // Primary media stream generator
    private val primaryToneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    } catch (e: Exception) {
        Log.w("BounceSound", "Failed to initialize STREAM_MUSIC ToneGenerator", e)
        null
    }

    // Fallback system stream generator
    private val fallbackToneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
    } catch (e: Exception) {
        Log.w("BounceSound", "Failed to initialize STREAM_SYSTEM ToneGenerator", e)
        null
    }

    companion object {
        private const val TAG = "BounceSound"
        const val BALL_RADIUS_DP = 14f
        private const val RESTITUTION = 0.35f
        private const val ACCELEROMETER_SENSITIVITY = 1.2f
        private const val SOUND_DEBOUNCE_MS = 180L
        private const val MAX_BOUNCES_PER_TILT = 3

        private const val DARK_LUX_THRESHOLD = 10f
        private const val LIGHT_LUX_THRESHOLD = 15f
    }

    init {
        viewModelScope.launch {
            sensorController.sensorAvailability.collect { availability ->
                _uiState.update { it.copy(sensorAvailability = availability) }
            }
        }

        viewModelScope.launch {
            sensorController.rawAcceleration.collect { raw ->
                _uiState.update {
                    it.copy(
                        rawAccelerometer = RawAccelerometer(x = raw[0], y = raw[1], z = raw[2])
                    )
                }
            }
        }

        viewModelScope.launch {
            sensorController.filteredAcceleration.collect { filtered ->
                processPhysics(filtered[0], filtered[1])
            }
        }

        viewModelScope.launch {
            sensorController.lux.collect { luxValue ->
                if (luxValue != null) {
                    if (luxValue == 0f && !hasReceivedValidLightReading) {
                        // Keep light theme on initial 0f reading
                    } else {
                        hasReceivedValidLightReading = true
                        evaluateTheme(luxValue)
                    }
                }
            }
        }

        viewModelScope.launch {
            sensorController.headingDegrees.collect { degrees ->
                _uiState.update {
                    it.copy(
                        headingDegrees = degrees,
                        cardinal = calculateCardinal(degrees)
                    )
                }
            }
        }
    }

    fun onResume() {
        sensorController.register()
    }

    fun onPause() {
        sensorController.unregister()
    }

    fun onArenaSizeChanged(width: Float, height: Float, density: Float) {
        arenaWidth = width
        arenaHeight = height
        if (_uiState.value.ballOffset == Offset.Zero && width > 0f && height > 0f) {
            _uiState.update { it.copy(ballOffset = Offset(width / 2f, height / 2f)) }
        }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
        if (!_uiState.value.isMuted) {
            playBeep()
        }
    }

    fun selectThemeMode(mode: ThemeMode) {
        _uiState.update { current ->
            val isDark = when (mode) {
                ThemeMode.AUTO -> current.isDarkMode
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            current.copy(themeMode = mode, isDarkMode = isDark)
        }
        if (mode == ThemeMode.AUTO) {
            evaluateTheme(_uiState.value.lux)
        }
    }

    private fun evaluateTheme(luxValue: Float) {
        _uiState.update { current ->
            val targetDarkMode = when (current.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> {
                    if (!current.sensorAvailability.hasLightSensor) {
                        current.isDarkMode
                    } else {
                        when {
                            luxValue < DARK_LUX_THRESHOLD -> true
                            luxValue > LIGHT_LUX_THRESHOLD -> false
                            else -> current.isDarkMode
                        }
                    }
                }
            }
            current.copy(lux = luxValue, isDarkMode = targetDarkMode)
        }
    }

    private fun processPhysics(ax: Float, ay: Float) {
        if (arenaWidth <= 0f || arenaHeight <= 0f) return

        val density = getApplication<Application>().resources.displayMetrics.density
        val radius = BALL_RADIUS_DP * density

        val minX = radius
        val maxX = arenaWidth - radius
        val minY = radius
        val maxY = arenaHeight - radius

        val currentX = _uiState.value.ballOffset.x
        val currentY = _uiState.value.ballOffset.y

        val gravityScale = density * 0.5f

        val accelX = -ax * gravityScale * ACCELEROMETER_SENSITIVITY
        val accelY = ay * gravityScale * ACCELEROMETER_SENSITIVITY

        // 3. RESET ONLY ON PHONE TILT REVERSAL OR FREE AREA ENTRY
        // Detect if user tilted phone in opposite direction or changed orientation significantly
        val tiltReversedX = (accelX * previousTiltX < -0.05f) || (abs(accelX - previousTiltX) > 1.5f)
        val tiltReversedY = (accelY * previousTiltY < -0.05f) || (abs(accelY - previousTiltY) > 1.5f)

        previousTiltX = accelX
        previousTiltY = accelY

        val freeAreaMargin = 20f * density
        val isInFreeArea = currentX > (minX + freeAreaMargin) &&
                currentX < (maxX - freeAreaMargin) &&
                currentY > (minY + freeAreaMargin) &&
                currentY < (maxY - freeAreaMargin)

        // Reset bounce quota & disengage grounded lock when tilt reverses or ball enters free area
        if (tiltReversedX || tiltReversedY || isInFreeArea) {
            bounceCount = 0
            isGroundedToWall = false
        }

        // Apply physics acceleration if not grounded to wall
        if (!isGroundedToWall) {
            ballVelocityX += accelX
            ballVelocityY += accelY

            ballVelocityX *= 0.95f
            ballVelocityY *= 0.95f
        } else {
            // While grounded, allow sliding along wall if tilt component allows, but force velocity into wall to 0
            if ((currentX <= minX && accelX <= 0f) || (currentX >= maxX && accelX >= 0f)) {
                ballVelocityX = 0f
            } else {
                ballVelocityX += accelX
                ballVelocityX *= 0.95f
            }

            if ((currentY <= minY && accelY <= 0f) || (currentY >= maxY && accelY >= 0f)) {
                ballVelocityY = 0f
            } else {
                ballVelocityY += accelY
                ballVelocityY *= 0.95f
            }
        }

        val candidateX = currentX + ballVelocityX
        val candidateY = currentY + ballVelocityY

        // Clamping position
        val posX = candidateX.coerceIn(minX, maxX)
        val posY = candidateY.coerceIn(minY, maxY)

        val hitX = (candidateX <= minX && ballVelocityX < 0f) || (candidateX >= maxX && ballVelocityX > 0f)
        val hitY = (candidateY <= minY && ballVelocityY < 0f) || (candidateY >= maxY && ballVelocityY > 0f)

        var shouldPlaySound = false

        // 2. BOUNCE LIMIT RULE (1-3 Times Max)
        if ((hitX || hitY) && !isGroundedToWall) {
            if (bounceCount < MAX_BOUNCES_PER_TILT) {
                bounceCount++
                shouldPlaySound = true

                if (hitX) ballVelocityX = -ballVelocityX * RESTITUTION
                if (hitY) ballVelocityY = -ballVelocityY * RESTITUTION

                Log.d(TAG, "Bounce $bounceCount / $MAX_BOUNCES_PER_TILT executed")
            }

            if (bounceCount >= MAX_BOUNCES_PER_TILT) {
                isGroundedToWall = true
                ballVelocityX = 0f
                ballVelocityY = 0f
                shouldPlaySound = false
                Log.d(TAG, "Bounce limit reached ($MAX_BOUNCES_PER_TILT). Grounded state engaged. Sound MUTED.")
            }
        }

        _uiState.update { it.copy(ballOffset = Offset(posX, posY)) }

        if (shouldPlaySound) {
            triggerCollisionFeedback()
        }
    }

    private fun triggerCollisionFeedback() {
        if (_uiState.value.isMuted) {
            Log.d(TAG, "Sound muted by user toggle, skipping sound")
            return
        }

        val now = SystemClock.uptimeMillis()
        if (now - lastCollisionTimeMs < SOUND_DEBOUNCE_MS) {
            return
        }
        lastCollisionTimeMs = now

        Log.d(TAG, "Single shot bounce sound played")
        playBeep()

        vibrator?.let {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(20L)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibration failed", e)
            }
        }
    }

    private fun playBeep() {
        var played = false
        try {
            played = primaryToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Primary tone playback failed", e)
        }

        if (!played) {
            try {
                fallbackToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
            } catch (e: Exception) {
                Log.e(TAG, "Fallback tone playback failed", e)
            }
        }
    }

    private fun calculateCardinal(degrees: Float): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (((degrees + 22.5f) % 360f) / 45f).toInt()
        return directions[index]
    }

    override fun onCleared() {
        super.onCleared()
        sensorController.unregister()
        try {
            primaryToneGenerator?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing primary tone generator", e)
        }
        try {
            fallbackToneGenerator?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing fallback tone generator", e)
        }
    }
}
