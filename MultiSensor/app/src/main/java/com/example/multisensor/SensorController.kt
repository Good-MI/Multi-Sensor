package com.example.multisensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2

class SensorController(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val _sensorAvailability = MutableStateFlow(
        SensorAvailability(
            hasAccelerometer = accelerometer != null,
            hasMagnetometer = magnetometer != null,
            hasLightSensor = lightSensor != null
        )
    )
    val sensorAvailability: StateFlow<SensorAvailability> = _sensorAvailability.asStateFlow()

    private val _filteredAcceleration = MutableStateFlow(FloatArray(3))
    val filteredAcceleration: StateFlow<FloatArray> = _filteredAcceleration.asStateFlow()

    private val _rawAcceleration = MutableStateFlow(FloatArray(3))
    val rawAcceleration: StateFlow<FloatArray> = _rawAcceleration.asStateFlow()

    private val _lux = MutableStateFlow<Float?>(null)
    val lux: StateFlow<Float?> = _lux.asStateFlow()

    private val _headingDegrees = MutableStateFlow(0f)
    val headingDegrees: StateFlow<Float> = _headingDegrees.asStateFlow()

    private val gravityReadings = FloatArray(3)
    private val geomagneticReadings = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var smoothedAngleSin = 0f
    private var smoothedAngleCos = 1f

    companion object {
        private const val ACCEL_LOW_PASS_ALPHA = 0.15f
        private const val COMPASS_SMOOTH_FACTOR = 0.08f
        private const val RAD_TO_DEG = (180.0 / Math.PI).toFloat()
    }

    fun register() {
        // MOTION Category: Accelerometer
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // POSITION Category: Magnetometer
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // ENVIRONMENT Category: Light Sensor
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val currentRaw = FloatArray(3)
                currentRaw[0] = event.values[0]
                currentRaw[1] = event.values[1]
                currentRaw[2] = event.values[2]
                _rawAcceleration.value = currentRaw

                /**
                 * Standard single-pole low pass filter:
                 * gravity[i] = alpha * gravity[i] + (1 - alpha) * raw[i]
                 */
                gravityReadings[0] = ACCEL_LOW_PASS_ALPHA * gravityReadings[0] +
                        (1f - ACCEL_LOW_PASS_ALPHA) * event.values[0]
                gravityReadings[1] = ACCEL_LOW_PASS_ALPHA * gravityReadings[1] +
                        (1f - ACCEL_LOW_PASS_ALPHA) * event.values[1]
                gravityReadings[2] = ACCEL_LOW_PASS_ALPHA * gravityReadings[2] +
                        (1f - ACCEL_LOW_PASS_ALPHA) * event.values[2]
                hasGravity = true

                val currentFiltered = FloatArray(3)
                currentFiltered[0] = gravityReadings[0]
                currentFiltered[1] = gravityReadings[1]
                currentFiltered[2] = gravityReadings[2]
                _filteredAcceleration.value = currentFiltered

                computeOrientation()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagneticReadings[0] = event.values[0]
                geomagneticReadings[1] = event.values[1]
                geomagneticReadings[2] = event.values[2]
                hasGeomagnetic = true

                computeOrientation()
            }

            Sensor.TYPE_LIGHT -> {
                _lux.value = event.values[0]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    /**
     * Resolves heading degrees and eliminates angle wrap-around discontinuities
     * (359 deg -> 0 deg) by continuously interpolating vector sine and cosine components.
     */
    private fun computeOrientation() {
        if (!hasGravity || !hasGeomagnetic) return

        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            gravityReadings,
            geomagneticReadings
        )

        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthRad = orientationAngles[0]

            val currentSin = kotlin.math.sin(azimuthRad)
            val currentCos = kotlin.math.cos(azimuthRad)

            smoothedAngleSin += COMPASS_SMOOTH_FACTOR * (currentSin - smoothedAngleSin)
            smoothedAngleCos += COMPASS_SMOOTH_FACTOR * (currentCos - smoothedAngleCos)

            val smoothedRad = atan2(smoothedAngleSin, smoothedAngleCos)
            var degrees = smoothedRad * RAD_TO_DEG
            if (degrees < 0f) {
                degrees += 360f
            }
            _headingDegrees.value = degrees
        }
    }
}
