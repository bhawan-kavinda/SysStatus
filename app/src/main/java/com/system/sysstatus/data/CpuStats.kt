package com.system.sysstatus.data

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException
import kotlin.math.abs

data class CoreStats(
    val index: Int,
    // -1 when the value could not be read on this device
    val freqKhz: Long = -1,
    val maxKhz: Long = -1,
    val loadPercent: Float = -1f
)

data class ThermalZone(val type: String, val tempC: Float)

data class CpuStats(
    val coreCount: Int = 0,
    val cores: List<CoreStats> = emptyList(),
    // -1 when /proc/stat is not readable (or on the first pass, before there is a delta)
    val totalLoad: Float = -1f,
    val governor: String = "",
    // Null when the Android version or device does not report it
    val thermalStatus: String? = null,
    val zones: List<ThermalZone> = emptyList(),
    val abi: String = "",
    val hardware: String = ""
) {
    val loadAvailable: Boolean get() = totalLoad >= 0f
    val freqAvailable: Boolean get() = cores.any { it.freqKhz > 0 }
    val maxFreqKhz: Long get() = cores.maxOfOrNull { it.maxKhz } ?: -1L
}

class CpuCollector(context: Context) {

    private val power = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    // Previous /proc/stat counters; key -1 is the whole CPU, other keys are core indexes
    private var previous: Map<Int, LongArray> = emptyMap()

    fun read(): CpuStats {
        val batch = ReadBatch("CPU")
        val stats = collect(batch)
        batch.commit()
        return stats
    }

    private fun collect(batch: ReadBatch): CpuStats {
        val count = Runtime.getRuntime().availableProcessors()

        val counters = batch.trace(
            call = "File(/proc/stat) read + parse",
            fallback = emptyMap<Int, LongArray>(),
            noData = { it.isEmpty() },
            show = { m ->
                val total = m[-1]
                "cpu=" + (total?.joinToString(" ") ?: "n/a") +
                    " (user nice system idle iowait irq softirq steal) cores=${m.size - 1}"
            }
        ) { readProcStat() }

        val old = previous
        if (counters.isNotEmpty()) previous = counters

        val freqs = batch.trace(
            call = "File(/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq)",
            fallback = LongArray(count) { -1L },
            noData = { a -> a.all { it <= 0 } },
            show = { a -> a.joinToString(" ", postfix = " (kHz)") { if (it > 0) it.toString() else "n/a" } }
        ) { readPerCore(count, "scaling_cur_freq") }

        val maxFreqs = batch.trace(
            call = "File(/sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_max_freq)",
            fallback = LongArray(count) { -1L },
            noData = { a -> a.all { it <= 0 } },
            show = { a -> a.joinToString(" ", postfix = " (kHz)") { if (it > 0) it.toString() else "n/a" } }
        ) { readPerCore(count, "cpuinfo_max_freq") }

        val governor = batch.trace(
            call = "File(/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor)",
            fallback = "",
            noData = { it.isBlank() },
            show = { it }
        ) { File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim() }

        val zones = batch.trace(
            call = "File(/sys/class/thermal/thermal_zone*/temp) read + parse",
            fallback = emptyList<ThermalZone>(),
            noData = { it.isEmpty() },
            show = { z -> z.joinToString(" ") { "${it.type}=${it.tempC}°C" } }
        ) { readThermalZones() }

        val thermalStatus: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val code = readThermalStatus(batch)
            if (code >= 0) thermalLabel(code) else null
        } else {
            null
        }

        val cores = (0 until count).map { i ->
            CoreStats(
                index = i,
                freqKhz = freqs.getOrElse(i) { -1L },
                maxKhz = maxFreqs.getOrElse(i) { -1L },
                loadPercent = loadOf(counters[i], old[i])
            )
        }

        return CpuStats(
            coreCount = count,
            cores = cores,
            totalLoad = loadOf(counters[-1], old[-1]),
            governor = governor,
            thermalStatus = thermalStatus,
            zones = zones,
            abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
            hardware = Build.HARDWARE.orEmpty()
        )
    }

    // Parses the leading "cpu" and "cpuN" lines of /proc/stat
    // Errors (for example a permission denial) propagate to ReadBatch.trace
    private fun readProcStat(): Map<Int, LongArray> {
        val result = HashMap<Int, LongArray>()
        File("/proc/stat").bufferedReader().useLines { lines ->
            for (line in lines) {
                if (!line.startsWith("cpu")) break
                val parts = line.trim().split(Regex("\\s+"))
                val name = parts[0]
                val index = if (name == "cpu") -1 else (name.substring(3).toIntOrNull() ?: continue)
                result[index] = LongArray(8) { i -> parts.getOrNull(i + 1)?.toLongOrNull() ?: 0L }
            }
        }
        return result
    }

    // Busy share of the time between two reads; -1 when there is nothing to compare
    private fun loadOf(current: LongArray?, old: LongArray?): Float {
        if (current == null || old == null) return -1f
        val total = current.sum() - old.sum()
        if (total <= 0L) return -1f
        val idle = (current[3] + current[4]) - (old[3] + old[4])
        return ((total - idle) * 100f / total).coerceIn(0f, 100f)
    }

    // One value per core in kHz, -1 for a core that cannot be read (for example offline)
    // Throws the first error when no core could be read at all, so the log shows the real reason
    private fun readPerCore(count: Int, file: String): LongArray {
        var firstError: Exception? = null
        val values = LongArray(count) { i ->
            try {
                File("/sys/devices/system/cpu/cpu$i/cpufreq/$file").readText().trim().toLong()
            } catch (e: Exception) {
                if (firstError == null) firstError = e
                -1L
            }
        }
        val error = firstError
        if (error != null && values.all { it <= 0 }) throw error
        return values
    }

    // Kernel thermal zones; values are usually millidegrees Celsius
    private fun readThermalZones(): List<ThermalZone> {
        val dirs = File("/sys/class/thermal").listFiles { f -> f.name.startsWith("thermal_zone") }
            ?: throw IOException("cannot list /sys/class/thermal")
        val zones = ArrayList<ThermalZone>()
        val sorted = dirs.sortedBy { it.name.removePrefix("thermal_zone").toIntOrNull() ?: Int.MAX_VALUE }
        for (dir in sorted) {
            try {
                val type = File(dir, "type").readText().trim()
                val raw = File(dir, "temp").readText().trim().toLong()
                val celsius = if (abs(raw) >= 1000) raw / 1000f else raw.toFloat()
                // Sensors that report an invalid value are skipped, not displayed
                if (celsius in -30f..150f) zones.add(ThermalZone(type, celsius))
            } catch (e: Exception) {
                // This zone is not readable; the others may still be
            }
        }
        return zones
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readThermalStatus(batch: ReadBatch): Int = batch.trace(
        call = "PowerManager.getCurrentThermalStatus()",
        fallback = -1,
        noData = { it < 0 },
        show = { "$it (${thermalLabel(it)})" }
    ) { power.currentThermalStatus }

    private fun thermalLabel(status: Int) = when (status) {
        0 -> "None"
        1 -> "Light"
        2 -> "Moderate"
        3 -> "Severe"
        4 -> "Critical"
        5 -> "Emergency"
        6 -> "Shutdown"
        else -> "Unknown"
    }
}
