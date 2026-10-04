package com.podda.sysstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.podda.sysstatus.data.BatteryStats
import com.podda.sysstatus.data.UiState
import kotlin.math.abs

@Composable
fun HomeScreen(state: UiState, onOpen: (Screen) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val battery = state.battery
    val memory = state.memory
    val history = state.history

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Text("System", fontSize = 28.sp, fontWeight = FontWeight.Medium)
        Text("Live device status", fontSize = 13.sp, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        BatteryCard(battery, onClick = { onOpen(Screen.Battery) })
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                title = "RAM",
                value = f1(memory.usedMb / 1024f),
                unit = "of ${f1(memory.totalMb / 1024f)} GB",
                accent = Accent.Violet,
                history = history.ramPercent,
                minValue = 0f,
                maxValue = 100f,
                modifier = Modifier.weight(1f),
                onClick = { onOpen(Screen.Memory) }
            )
            StatTile(
                title = "Swap",
                value = if (memory.swapTotalMb > 0) f1(memory.swapUsedMb / 1024f) else "Off",
                unit = if (memory.swapTotalMb > 0) "of ${f1(memory.swapTotalMb / 1024f)} GB" else "",
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
                title = "Battery temp",
                value = f1(battery.temperatureC),
                unit = "°C",
                accent = Accent.Orange,
                history = history.batteryTempC,
                minValue = 25f,
                maxValue = 50f,
                modifier = Modifier.weight(1f),
                onClick = { onOpen(Screen.Battery) }
            )
            StatTile(
                title = "Power",
                value = if (battery.currentAvailable) f1(battery.powerW) else "—",
                unit = if (battery.currentAvailable) "W" else "",
                accent = Accent.Green,
                history = history.powerW,
                minValue = -8f,
                maxValue = 16f,
                modifier = Modifier.weight(1f),
                onClick = { onOpen(Screen.Battery) }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "CPU, GPU, thermal and more arrive in the next steps.",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BatteryCard(battery: BatteryStats, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    val headline = if (battery.currentAvailable) {
        "${battery.statusText} · ${f1(abs(battery.powerW))} W"
    } else {
        battery.statusText
    }
    val electrical = if (battery.currentAvailable) {
        "${f2(battery.voltageV)} V · ${battery.currentMa} mA"
    } else {
        "${f2(battery.voltageV)} V"
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BatteryRing(percent = battery.levelPercent, color = Accent.Green)
        Column {
            Text("Battery", fontSize = 13.sp, color = colors.onSurfaceVariant)
            Text(headline, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(electrical, fontSize = 13.sp, color = colors.onSurfaceVariant)
            Text(
                "${f1(battery.temperatureC)} °C · ${battery.health}",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )
        }
    }
}
