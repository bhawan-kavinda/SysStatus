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
    val batteryTempC: List<Float> = emptyList()
)

data class UiState(
    val battery: BatteryStats = BatteryStats(),
    val memory: MemoryStats = MemoryStats(),
    val history: History = History()
)

private class RollingBuffer(private val capacity: Int) {
    private val items = ArrayDeque<Float>()

    fun add(value: Float): List<Float> {
        if (items.size >= capacity) items.removeFirst()
        items.addLast(value)
        return items.toList()
    }
}

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val batteryCollector = BatteryCollector(app)
    private val memoryCollector = MemoryCollector(app)

    // Polling runs only while the UI is collecting (plus a 5s grace period)
    val ui: StateFlow<UiState> = flow {
        val power = RollingBuffer(HISTORY_SIZE)
        val current = RollingBuffer(HISTORY_SIZE)
        val ram = RollingBuffer(HISTORY_SIZE)
        val swap = RollingBuffer(HISTORY_SIZE)
        val temp = RollingBuffer(HISTORY_SIZE)

        while (true) {
            val battery = batteryCollector.read()
            val memory = memoryCollector.read()
            emit(
                UiState(
                    battery = battery,
                    memory = memory,
                    history = History(
                        powerW = power.add(battery.powerW),
                        currentMa = current.add(battery.currentMa.toFloat()),
                        ramPercent = ram.add(memory.usedPercent),
                        swapPercent = swap.add(memory.swapPercent),
                        batteryTempC = temp.add(battery.temperatureC)
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
