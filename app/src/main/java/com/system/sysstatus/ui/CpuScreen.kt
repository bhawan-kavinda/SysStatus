package com.system.sysstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.system.sysstatus.data.CoreStats
import com.system.sysstatus.data.UiState

@Composable
fun CpuScreen(state: UiState) {
    val colors = MaterialTheme.colorScheme
    val c = state.cpu
    val h = state.history
    val shape = RoundedCornerShape(22.dp)

    DetailScaffold(
        title = "Processor",
        subtitle = "${c.coreCount} logical cores" + if (c.hardware.isNotBlank()) " · ${c.hardware}" else ""
    ) {
        GraphCard(
            icon = Ic.Cpu,
            label = "CPU load",
            value = if (c.loadAvailable) f0(c.totalLoad) else null,
            unit = "%",
            accent = Accent.Pink,
            history = h.cpuLoad,
            minValue = 0f,
            maxValue = 100f,
            emptyText = "$NO_ACCESS (/proc/stat is not readable)"
        )

        Text(
            "Cores",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.outlineVariant, shape)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (c.cores.none { it.freqKhz > 0 || it.loadPercent >= 0f }) {
                Box(Modifier.padding(vertical = 8.dp)) {
                    NoDataText("$NO_ACCESS (per-core load and frequency are not readable)", 14.sp)
                }
            } else {
                c.cores.forEach { CoreRow(it) }
            }
        }
        Spacer(Modifier.height(14.dp))

        InfoGroup(
            title = "Temperatures",
            rows = if (c.zones.isEmpty()) {
                listOf(InfoItem(Ic.Temp, "Thermal zones", null, NO_ACCESS))
            } else {
                c.zones.map { InfoItem(Ic.Temp, it.type, "${f1(it.tempC)} °C") }
            }
        )

        InfoGroup(
            title = "Details",
            rows = listOf(
                InfoItem(Ic.Cpu, "Logical cores", "${c.coreCount}"),
                InfoItem(Ic.Chip, "Hardware", c.hardware.ifBlank { null }),
                InfoItem(Ic.Chip, "ABI", c.abi.ifBlank { null }),
                InfoItem(Ic.Pulse, "Governor", c.governor.ifBlank { null }, NO_ACCESS),
                InfoItem(Ic.Pulse, "Max frequency", if (c.maxFreqKhz > 0) formatKhz(c.maxFreqKhz) else null, NO_ACCESS),
                InfoItem(Ic.Temp, "Thermal status", c.thermalStatus)
            )
        )

        FootNote("Android may block apps from reading kernel files. Anything blocked is shown as “No access”, never estimated.")
    }
}

@Composable
private fun CoreRow(core: CoreStats) {
    val colors = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Core ${core.index}", fontSize = 13.sp, color = colors.onSurfaceVariant, modifier = Modifier.width(56.dp))
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.onSurface.copy(alpha = 0.08f))
        ) {
            if (core.loadPercent >= 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((core.loadPercent / 100f).coerceIn(0f, 1f))
                        .background(Accent.Pink)
                )
            }
        }
        Text(
            text = if (core.loadPercent >= 0f) "${f0(core.loadPercent)}%" else NO_DATA,
            fontSize = 12.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(58.dp)
                .padding(start = 8.dp)
        )
        Text(
            text = if (core.freqKhz > 0) formatKhz(core.freqKhz) else NO_DATA,
            fontSize = 12.sp,
            color = colors.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(74.dp)
        )
    }
}
