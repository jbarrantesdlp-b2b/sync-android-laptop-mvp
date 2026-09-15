package com.example.syncapp.iot

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

data class IotSnapshot(
    val lightLux: Float? = null,
    val accelG: Float? = null,
    val proximityCm: Float? = null,
    val batteryPct: Int? = null,
    val charging: Boolean = false,
    val steps: Int? = null,
    val tempC: Float? = null,
    val humidity: Float? = null,
    val pressureHpa: Float? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val available: List<String> = emptyList()
) {
    fun toPayloadJson(device: String): String {
        fun n(v: Float?) = v?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "null"
        fun d(v: Double?) = v?.let { String.format(java.util.Locale.US, "%.5f", it) } ?: "null"
        val names = available.joinToString(",") { "\"$it\"" }
        val safeDevice = device.replace("\\", "\\\\").replace("\"", "\\\"")
        return "{" +
            "\"source\":\"phone\"," +
            "\"device\":\"$safeDevice\"," +
            "\"sensors\":{" +
            "\"lightLux\":${n(lightLux)}," +
            "\"accelG\":${n(accelG)}," +
            "\"proximityCm\":${n(proximityCm)}," +
            "\"batteryPct\":${batteryPct ?: "null"}," +
            "\"charging\":$charging," +
            "\"steps\":${steps ?: "null"}," +
            "\"tempC\":${n(tempC)}," +
            "\"humidity\":${n(humidity)}," +
            "\"pressureHpa\":${n(pressureHpa)}," +
            "\"lat\":${d(lat)}," +
            "\"lng\":${d(lng)}" +
            "},\"available\":[$names]}"
    }
}

class IotSensorHub(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val _snapshot = MutableStateFlow(IotSnapshot())
    val snapshot: StateFlow<IotSnapshot> = _snapshot.asStateFlow()

    private var running = false
    private val present = mutableSetOf<String>()

    fun start() {
        if (running) return
        running = true
        present.clear()
        register(Sensor.TYPE_LIGHT, "luz")
        register(Sensor.TYPE_ACCELEROMETER, "movimiento")
        register(Sensor.TYPE_PROXIMITY, "proximidad")
        register(Sensor.TYPE_AMBIENT_TEMPERATURE, "temperatura")
        register(Sensor.TYPE_RELATIVE_HUMIDITY, "humedad")
        register(Sensor.TYPE_PRESSURE, "presion")
        register(Sensor.TYPE_STEP_COUNTER, "pasos")
        present.add("bateria")
        readBattery()
        readLocation()
        publish { it.copy(available = present.sorted()) }
    }

    fun stop() {
        running = false
        try { sensorManager.unregisterListener(this) } catch (_: Exception) {}
    }

    private fun register(type: Int, label: String) {
        val sensor = sensorManager.getDefaultSensor(type) ?: return
        present.add(label)
        try { sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI) } catch (_: Exception) {}
    }

    private fun publish(update: (IotSnapshot) -> IotSnapshot) {
        _snapshot.value = update(_snapshot.value)
    }

    private fun readBattery() {
        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else null
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            publish { it.copy(batteryPct = pct, charging = charging) }
        } catch (_: Exception) {}
    }

    private fun readLocation() {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            @Suppress("DEPRECATION")
            val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc != null) {
                present.add("gps")
                publish { it.copy(lat = loc.latitude, lng = loc.longitude, available = present.sorted()) }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        when (e.sensor.type) {
            Sensor.TYPE_LIGHT -> publish { it.copy(lightLux = e.values.firstOrNull()) }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = e.values.getOrNull(0) ?: 0f
                val y = e.values.getOrNull(1) ?: 0f
                val z = e.values.getOrNull(2) ?: 0f
                val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                publish { it.copy(accelG = g) }
            }
            Sensor.TYPE_PROXIMITY -> publish { it.copy(proximityCm = e.values.firstOrNull()) }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> publish { it.copy(tempC = e.values.firstOrNull()) }
            Sensor.TYPE_RELATIVE_HUMIDITY -> publish { it.copy(humidity = e.values.firstOrNull()) }
            Sensor.TYPE_PRESSURE -> publish { it.copy(pressureHpa = e.values.firstOrNull()) }
            Sensor.TYPE_STEP_COUNTER -> publish { it.copy(steps = e.values.firstOrNull()?.toInt()) }
        }
        if (e.sensor.type == Sensor.TYPE_LIGHT || e.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            readBattery()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        fun deviceName(): String = Build.MODEL ?: "Android"
    }
}
