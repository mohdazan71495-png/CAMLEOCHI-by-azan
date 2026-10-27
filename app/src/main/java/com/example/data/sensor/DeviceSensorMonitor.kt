package com.example.data.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class RawHardwareSensorReadings(
    val pressureHpa: Float? = null,
    val ambientTempC: Float? = null,
    val lightLux: Float? = null,
    val relativeHumidityPercent: Float? = null,
    val hasBarometer: Boolean = false,
    val hasThermometer: Boolean = false,
    val hasLightSensor: Boolean = false,
    val hasHumiditySensor: Boolean = false
)

data class GpsCoordinatesResult(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Int
)

class DeviceSensorMonitor(private val context: Context) {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun observeHardwareSensors(): Flow<RawHardwareSensorReadings> = callbackFlow {
        val manager = sensorManager
        if (manager == null) {
            trySend(RawHardwareSensorReadings())
            awaitClose { }
            return@callbackFlow
        }

        val pressureSensor = manager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val tempSensor = manager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        val lightSensor = manager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val humiditySensor = manager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

        var currentState = RawHardwareSensorReadings(
            hasBarometer = pressureSensor != null,
            hasThermometer = tempSensor != null,
            hasLightSensor = lightSensor != null,
            hasHumiditySensor = humiditySensor != null
        )
        trySend(currentState)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val ev = event ?: return
                val value = ev.values?.firstOrNull() ?: return
                currentState = when (ev.sensor.type) {
                    Sensor.TYPE_PRESSURE -> currentState.copy(pressureHpa = value)
                    Sensor.TYPE_AMBIENT_TEMPERATURE -> currentState.copy(ambientTempC = value)
                    Sensor.TYPE_LIGHT -> currentState.copy(lightLux = value)
                    Sensor.TYPE_RELATIVE_HUMIDITY -> currentState.copy(relativeHumidityPercent = value)
                    else -> currentState
                }
                trySend(currentState)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        pressureSensor?.let {
            manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        tempSensor?.let {
            manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        lightSensor?.let {
            manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        humiditySensor?.let {
            manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            manager.unregisterListener(listener)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentGpsCoordinates(): GpsCoordinatesResult? {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        if (location != null) {
                            continuation.resume(
                                GpsCoordinatesResult(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    altitudeMeters = if (location.hasAltitude()) location.altitude.toInt() else 120
                                )
                            )
                        } else {
                            continuation.resume(null)
                        }
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }

            continuation.invokeOnCancellation {
                cts.cancel()
            }
        }
    }
}
