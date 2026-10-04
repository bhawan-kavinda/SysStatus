package com.podda.sysstatus.data

import android.app.ActivityManager
import android.content.Context
import java.io.File

data class MemoryStats(
    val totalMb: Long = 0,
    val availableMb: Long = 0,
    val cachedMb: Long = 0,
    val buffersMb: Long = 0,
    val swapTotalMb: Long = 0,
    val swapFreeMb: Long = 0
) {
    val usedMb: Long get() = (totalMb - availableMb).coerceAtLeast(0)
    val usedPercent: Float get() = if (totalMb > 0) usedMb * 100f / totalMb else 0f
    val swapUsedMb: Long get() = (swapTotalMb - swapFreeMb).coerceAtLeast(0)
    val swapPercent: Float get() = if (swapTotalMb > 0) swapUsedMb * 100f / swapTotalMb else 0f
}

class MemoryCollector(context: Context) {

    private val activityManager =
        context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun read(): MemoryStats {
        val fields = readMeminfo()
        if (fields.isEmpty() || !fields.containsKey("MemTotal")) return readFallback()

        return MemoryStats(
            totalMb = kbToMb(fields["MemTotal"]),
            availableMb = kbToMb(fields["MemAvailable"] ?: fields["MemFree"]),
            cachedMb = kbToMb(fields["Cached"]),
            buffersMb = kbToMb(fields["Buffers"]),
            swapTotalMb = kbToMb(fields["SwapTotal"]),
            swapFreeMb = kbToMb(fields["SwapFree"])
        )
    }

    // Parses lines such as "MemTotal:        7890000 kB"
    private fun readMeminfo(): Map<String, Long> {
        return try {
            val result = HashMap<String, Long>()
            File("/proc/meminfo").bufferedReader().useLines { lines ->
                for (line in lines) {
                    val colon = line.indexOf(':')
                    if (colon <= 0) continue
                    val key = line.substring(0, colon)
                    val number = line.substring(colon + 1).trim().substringBefore(' ').toLongOrNull()
                        ?: continue
                    result[key] = number
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Used only when /proc/meminfo is not readable on a device
    private fun readFallback(): MemoryStats {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        return MemoryStats(
            totalMb = info.totalMem / MB,
            availableMb = info.availMem / MB
        )
    }

    private fun kbToMb(kb: Long?): Long = (kb ?: 0L) / 1024

    private companion object {
        const val MB = 1024L * 1024L
    }
}
