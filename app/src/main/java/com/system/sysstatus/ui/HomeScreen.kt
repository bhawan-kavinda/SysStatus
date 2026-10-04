package com.system.sysstatus.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.system.sysstatus.data.BatteryStats
import com.system.sysstatus.data.CpuStats
import com.system.sysstatus.data.UiState
import kotlin.math.abs

// Device name and Android version come straight from android.os.Build
private fun deviceLine(): String {
    val maker = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
    return "$maker ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"
}

@Composable
fun HomeScreen(state: UiState, onOpen: (Screen) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val battery = state.battery
    val memory = state.memory
    val history = state.history

    val ramOk = memory.totalMb > 0
    val swapKnown = memory.detailAvailable
    val swapOn = swapKnown && memory.swapTotalMb > 0

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("System", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
        Text(deviceLine(), fontSize = 14.sp, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))

        BatteryCard(battery, onClick = { onOpen(Screen.Battery) })
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(
                icon = Ic.Clock,
                label = "Full in",
                value = when {
                    !battery.available -> null
                    !battery.isCharging -> "Not charging"
                    battery.chargeTimeRemainingMin > 0 -> formatHm(battery.chargeTimeRemainingMin)
                    else -> null
                },
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                icon = Ic.Cycle,
                label = "Cycles",
                value = if (battery.cycleCount > 0) "${battery.cycleCount}" else null,
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                icon = Ic.Heart,
                label = "Health",
                value = if (battery.stateOfHealthPercent > 0) "${battery.stateOfHealthPercent}%" else null,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Ic.Ram,
                title = "RAM",
                value = if (ramOk) f1(memory.usedMb / 1024f) else null,
                unit = "of ${f1(memory.totalMb / 1024f)} GB",
                accent = Accent.Violet,
                history = history.ramPercent,
                minValue = 0f,
                maxValue = 100f,
                modifier = Modifier.weight(1f),
                onClick = { onOpen(Screen.Memory) }
            )
            StatTile(
                icon = Ic.Swap,
                title = "Swap",
                value = when {
                    !swapKnown -> null
                    swapOn -> f1(memory.swapUsedMb / 1024f)
                    else -> "Off"
                },
                unit = if (swapOn) "of ${f1(memory.swapTotalMb / 1024f)} GB" else "",
                accent = Accent.Blue,
                history = history.swapPercent,
                minValue = 0f,
                maxValue = 100f,
                modifier = Modifier.weight(1f),
                onClick = { onOpen(Screen.Memory) }
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Ic.Temp,
                title = "Battery temp",
                value = if (battery.available) f1(battery.temperatureC) else null,
                unit = "°C",
                accent = Accent.Orange,
                history = history.batteryTempC,
                minValue = 25f,
                maxValue = 50f,
                modifier = Modifier.weight(1f),
                onClick = { onOpen(Screen.Battery) }
            )
            StatTile(
                icon = Ic.Pulse,
                title = "Power",
                value = if (battery.available && battery.currentAvailable) f1(battery.powerW) else null,
                unit = "W",
                accent = Accent.Green,
                history = history.powerW,
                minValue = -8f,
                maxValue = 16f,
                modifier = Modifier.weight(1f),
                onClick = { onOpen(Screen.Battery) }
            )
        }

        Spacer(Modifier.height(12.dp))
        CpuCard(state.cpu, onClick = { onOpen(Screen.Cpu) })
        Spacer(Modifier.height(12.dp))
        LinkCard(
            icon = Ic.Terminal,
            accent = Accent.Yellow,
            title = "Data log",
            subtitle = "See every system request and its raw result",
            onClick = { onOpen(Screen.Log) }
        )

        Spacer(Modifier.height(16.dp))
        FootNote("Every value is read live from this device. A value the system does not provide is shown as “No data” or “No access”.")
    }
}

internal fun formatHm(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"

@Composable
private fun MiniStat(icon: Ic, label: String, value: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            SysIcon(icon, colors.onSurfaceVariant, 14.dp)
            Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(3.dp))
        if (value == null) {
            NoDataText(NO_DATA, 14.sp)
        } else {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
        }
    }
}

@Composable
private fun BatteryCard(battery: BatteryStats, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)

    if (!battery.available) {
        NoDataCard(Ic.Battery, "Battery", "No data: Android returned no battery status")
        return
    }

    // Low battery (not charging) switches the card to the warning colour
    val tint = if (!battery.isCharging && battery.levelPercent <= 20) Accent.Orange else Accent.Green

    val electrical = listOfNotNull(
        if (battery.voltageV > 0f) "${f2(battery.voltageV)} V" else null,
        if (battery.currentAvailable) "${battery.currentMa} mA" else null
    ).joinToString(" · ")

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.20f), colors.surface)))
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        BatteryRing(percent = battery.levelPercent, color = tint, diameter = 100.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusPill(battery.statusText, if (battery.isCharging) Ic.Bolt else Ic.Battery, tint)
            if (battery.currentAvailable) {
                Text(
                    "${f1(abs(battery.powerW))} W",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            } else {
                NoDataText("Power: no data", 17.sp)
            }
            if (electrical.isNotEmpty()) {
                Text(electrical, fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
            Text(
                "${f1(battery.temperatureC)} °C · ${reported(battery.health) ?: "Health not reported"}",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CpuCard(cpu: CpuStats, onClick: () -> Unit) {
    val subtitle = buildString {
        append(if (cpu.loadAvailable) "Load ${f0(cpu.totalLoad)}%" else "Load: $NO_ACCESS")
        append(" · ${cpu.coreCount} cores")
        if (cpu.maxFreqKhz > 0) append(" · up to ${formatKhz(cpu.maxFreqKhz)}")
    }
    LinkCard(
        icon = Ic.Cpu,
        accent = Accent.Pink,
        title = "Processor",
        subtitle = subtitle,
        onClick = onClick
    )
}

@Composable
private fun LinkCard(icon: Ic, accent: androidx.compose.ui.graphics.Color, title: String, subtitle: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconChip(icon, accent, boxSize = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Text(subtitle, fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        SysIcon(Ic.Chevron, colors.onSurfaceVariant, 20.dp)
    }
}
