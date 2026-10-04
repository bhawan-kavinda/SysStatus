package com.system.sysstatus.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class History(
    val powerW: List<Float> = emptyList(),
    val currentMa: List<Float> = emptyList(),
    val ramPercent: List<Float> = emptyList(),
    val swapPercent: List<Float> = emptyList(),
    val batteryTempC: List<Float> = emptyList(),
    val cpuLoad: List<Float> = emptyList()
)

data class UiState(
    // False only until the first real reading arrives
    val loaded: Boolean = false,
    val battery: BatteryStats = BatteryStats(),
    val memory: MemoryStats = MemoryStats(),
    val cpu: CpuStats = CpuStats(),
    val history: History = History()
)

private class RollingBuffer(private val capacity: Int) {
    private val items = ArrayDeque<Float>()

    fun add(value: Float): List<Float> {
        if (items.size >= capacity) items.removeFirst()
        items.addLast(value)
        return items.toList()
    }

    fun snapshot(): List<Float> = items.toList()

    // A graph only receives a point when the system really reported a value
    fun addIf(real: Boolean, value: Float): List<Float> = if (real) add(value) else snapshot()
}

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val batteryCollector = BatteryCollector(app)
    private val memoryCollector = MemoryCollector(app)
    private val cpuCollector = CpuCollector(app)

    // Polling runs only while the UI is collecting (plus a 5s grace period)
    val ui: StateFlow<UiState> = flow {
        val power = RollingBuffer(HISTORY_SIZE)
        val current = RollingBuffer(HISTORY_SIZE)
        val ram = RollingBuffer(HISTORY_SIZE)
        val swap = RollingBuffer(HISTORY_SIZE)
        val temp = RollingBuffer(HISTORY_SIZE)
        val cpuLoad = RollingBuffer(HISTORY_SIZE)

        while (true) {
            val battery = batteryCollector.read()
            val memory = memoryCollector.read()
            val cpu = cpuCollector.read()
            val currentReal = battery.available && battery.currentAvailable
            emit(
                UiState(
                    loaded = true,
                    battery = battery,
                    memory = memory,
                    cpu = cpu,
                    history = History(
                        powerW = power.addIf(currentReal, battery.powerW),
                        currentMa = current.addIf(currentReal, battery.currentMa.toFloat()),
                        ramPercent = ram.addIf(memory.totalMb > 0, memory.usedPercent),
                        swapPercent = swap.addIf(memory.detailAvailable && memory.swapTotalMb > 0, memory.swapPercent),
                        batteryTempC = temp.addIf(battery.available, battery.temperatureC),
                        cpuLoad = cpuLoad.addIf(cpu.loadAvailable, cpu.totalLoad)
                    )
                )
            )
            delay(INTERVAL_MS)
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private companion object {
        const val INTERVAL_MS = 3_000L
        const val HISTORY_SIZE = 60
    }
}
