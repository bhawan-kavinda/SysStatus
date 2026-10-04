package com.podda.sysstatus.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.podda.sysstatus.data.UiState

@Composable
fun MemoryScreen(state: UiState, onBack: () -> Unit) {
    val m = state.memory
    val h = state.history
    val swapOn = m.swapTotalMb > 0

    DetailScaffold(title = "Memory", onBack = onBack) {
        GraphCard(
            label = "RAM used",
            value = f1(m.usedMb / 1024f),
            unit = "of ${f1(m.totalMb / 1024f)} GB",
            accent = Accent.Violet,
            history = h.ramPercent,
            minValue = 0f,
            maxValue = 100f
        )
        Spacer(Modifier.height(12.dp))

        GraphCard(
            label = "Swap used",
            value = if (swapOn) f1(m.swapUsedMb / 1024f) else "Off",
            unit = if (swapOn) "of ${f1(m.swapTotalMb / 1024f)} GB" else "",
            accent = Accent.Blue,
            history = h.swapPercent,
            minValue = 0f,
            maxValue = 100f
        )
        Spacer(Modifier.height(14.dp))

        InfoGroup(
            title = "RAM",
            rows = listOf(
                "Total" to formatMb(m.totalMb),
                "Used" to "${formatMb(m.usedMb)} (${f1(m.usedPercent)}%)",
                "Available" to formatMb(m.availableMb),
                "Cached" to formatMb(m.cachedMb),
                "Buffers" to formatMb(m.buffersMb)
            )
        )

        InfoGroup(
            title = "Swap",
            rows = if (swapOn) {
                listOf(
                    "Total" to formatMb(m.swapTotalMb),
                    "Used" to "${formatMb(m.swapUsedMb)} (${f1(m.swapPercent)}%)",
                    "Free" to formatMb(m.swapFreeMb)
                )
            } else {
                listOf("Status" to "Not enabled on this device")
            }
        )
    }
}
