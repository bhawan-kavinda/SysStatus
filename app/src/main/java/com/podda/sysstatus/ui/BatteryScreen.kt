package com.podda.sysstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.podda.sysstatus.data.UiState

private const val NOT_REPORTED = "Not reported"

private fun formatMinutes(minutes: Int): String =
    if (minutes < 0) NOT_REPORTED else "${minutes / 60}h ${minutes % 60}m"

@Composable
fun BatteryScreen(state: UiState, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val b = state.battery
    val h = state.history

    val currentText = if (b.currentAvailable) {
        (if (b.currentMa > 0) "+" else "") + "${b.currentMa} mA"
    } else {
        NOT_REPORTED
    }

    DetailScaffold(title = "Battery", onBack = onBack) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BatteryRing(percent = b.levelPercent, color = Accent.Green, diameter = 110.dp)
            Column {
                Text(b.statusText, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = if (b.plugged == "None") "On battery" else "Plugged in: ${b.plugged}",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )
                if (b.isCharging && b.chargeTimeRemainingMin > 0) {
                    Text(
                        text = "Full in ${formatMinutes(b.chargeTimeRemainingMin)}",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        GraphCard(
            label = "Power",
            value = if (b.currentAvailable) f1(b.powerW) else "—",
            unit = if (b.currentAvailable) "W" else "",
            accent = Accent.Green,
            history = h.powerW,
            minValue = -8f,
            maxValue = 16f
        )
        Spacer(Modifier.height(12.dp))

        GraphCard(
            label = "Current",
            value = if (b.currentAvailable) "${b.currentMa}" else "—",
            unit = if (b.currentAvailable) "mA" else "",
            accent = Accent.Blue,
            history = h.currentMa,
            minValue = -4500f,
            maxValue = 4500f
        )
        Spacer(Modifier.height(12.dp))

        GraphCard(
            label = "Temperature",
            value = f1(b.temperatureC),
            unit = "°C",
            accent = Accent.Orange,
            history = h.batteryTempC,
            minValue = 25f,
            maxValue = 50f
        )
        Spacer(Modifier.height(14.dp))

        InfoGroup(
            title = "Live",
            rows = listOf(
                "Voltage" to "${f2(b.voltageV)} V",
                "Current" to currentText,
                "Power" to if (b.currentAvailable) "${f1(b.powerW)} W" else NOT_REPORTED,
                "Temperature" to "${f1(b.temperatureC)} °C"
            )
        )

        InfoGroup(
            title = "Health",
            rows = listOf(
                "Health" to b.health,
                "Technology" to b.technology.ifBlank { "Unknown" },
                "Cycle count" to if (b.cycleCount > 0) "${b.cycleCount}" else NOT_REPORTED,
                "State of health" to if (b.stateOfHealthPercent > 0) "${b.stateOfHealthPercent}%" else NOT_REPORTED,
                "Remaining capacity" to if (b.chargeCounterMah > 0) "${b.chargeCounterMah} mAh" else NOT_REPORTED
            )
        )

        InfoGroup(
            title = "Charging",
            rows = listOf(
                "Status" to b.statusText,
                "Power source" to b.plugged,
                "Time to full" to if (b.isCharging) formatMinutes(b.chargeTimeRemainingMin) else "—"
            )
        )

        Text(
            text = "Some values depend on your device and Android version.",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
