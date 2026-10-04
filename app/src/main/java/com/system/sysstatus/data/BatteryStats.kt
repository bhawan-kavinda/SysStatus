package com.system.sysstatus.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlin.math.abs

data class BatteryStats(
    val levelPercent: Int = 0,
    val isCharging: Boolean = false,
    val statusText: String = "Unknown",
    val voltageV: Float = 0f,
    val currentAvailable: Boolean = false,
    // Positive while charging, negative while discharging
    val currentMa: Int = 0,
    val powerW: Float = 0f,
    val temperatureC: Float = 0f,
    val health: String = "Unknown",
    val technology: String = "",
    val plugged: String = "None",
    // The values below are -1 when the device or Android version does not report them
    val cycleCount: Int = -1,
    val stateOfHealthPercent: Int = -1,
    val chargeCounterMah: Int = -1,
    val chargeTimeRemainingMin: Int = -1
)

class BatteryCollector(context: Context) {

    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

    fun read(): BatteryStats {
        val intent = appContext.registerReceiver(null, filter) ?: return BatteryStats()

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percent = if (level >= 0 && scale > 0) {
            level * 100 / scale
        } else {
            manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        }

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING

        // Most devices report millivolts, a few report volts
        val rawVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageV = if (rawVoltage > 100) rawVoltage / 1000f else rawVoltage.toFloat()

        // Reported in tenths of a degree Celsius
        val tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f

        val rawCurrent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentAvailable = rawCurrent != Int.MIN_VALUE && rawCurrent != 0
        val magnitudeMa = if (currentAvailable) normalizeToMa(abs(rawCurrent)) else 0

        // Sign differs between vendors, so it is derived from the charge status instead
        val currentMa = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> magnitudeMa
            BatteryManager.BATTERY_STATUS_DISCHARGING -> -magnitudeMa
            else -> 0
        }

        return BatteryStats(
            levelPercent = percent,
            isCharging = charging,
            statusText = statusLabel(status),
            voltageV = voltageV,
            currentAvailable = currentAvailable,
            currentMa = currentMa,
            powerW = voltageV * currentMa / 1000f,
            temperatureC = tempC,
            health = healthLabel(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)),
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty(),
            plugged = pluggedLabel(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)),
            cycleCount = readCycleCount(intent),
            stateOfHealthPercent = readStateOfHealth(),
            chargeCounterMah = readChargeCounterMah(),
            chargeTimeRemainingMin = readChargeTimeRemainingMin(charging)
        )
    }

    // Available from Android 14 (API 34) on devices that report it
    private fun readCycleCount(intent: Intent): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return -1
        val value = intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
        return if (value > 0) value else -1
    }

    // Available from Android 14 (API 34) on devices that report it
    private fun readStateOfHealth(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return -1
        // Hidden constant BATTERY_PROPERTY_STATE_OF_HEALTH (value 10) is not exposed in public SDK stubs
        val value = try { manager.getIntProperty(10) } catch (e: Exception) { -1 }
        return if (value in 1..100) value else -1
    }

    // Remaining charge in microamp-hours, converted to mAh
    private fun readChargeCounterMah(): Int {
        val value = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        return if (value > 0) value / 1000 else -1
    }

    // Available from Android 9 (API 28), only meaningful while charging
    private fun readChargeTimeRemainingMin(charging: Boolean): Int {
        if (!charging || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return -1
        val millis = manager.computeChargeTimeRemaining()
        return if (millis > 0) (millis / 60_000L).toInt() else -1
    }

    // Values of 10000 or more are treated as microamps, smaller values as milliamps
    private fun normalizeToMa(value: Int): Int = if (value >= 10_000) value / 1000 else value

    private fun statusLabel(status: Int) = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
        else -> "Unknown"
    }

    private fun healthLabel(health: Int) = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        else -> "Unknown"
    }

    private fun pluggedLabel(plugged: Int) = when {
        plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> "AC"
        plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB"
        plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> "Wireless"
        else -> "None"
    }
}
