package com.system.sysstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.system.sysstatus.data.UiState

@Composable
fun BatteryScreen(state: UiState) {
    val colors = MaterialTheme.colorScheme
    val b = state.battery
    val h = state.history

    if (!b.available) {
        DetailScaffold(title = "Battery", subtitle = NO_DATA) {
            NoDataCard(Ic.Battery, "Battery", "No data: Android returned no battery status")
        }
        return
    }

    val shape = RoundedCornerShape(26.dp)
    val tint = if (!b.isCharging && b.levelPercent <= 20) Accent.Orange else Accent.Green
    val currentOk = b.currentAvailable

    DetailScaffold(
        title = "Battery",
        subtitle = if (b.plugged == "None") "${b.statusText} · on battery" else "${b.statusText} · plugged in: ${b.plugged}"
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.outlineVariant, shape)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            BatteryRing(percent = b.levelPercent, color = tint, diameter = 112.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(b.statusText, if (b.isCharging) Ic.Bolt else Ic.Battery, tint)
                Text(
                    text = if (b.plugged == "None") "On battery" else "Plugged in: ${b.plugged}",
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant
                )
                if (b.isCharging) {
                    if (b.chargeTimeRemainingMin > 0) {
                        Text("Full in ${formatHm(b.chargeTimeRemainingMin)}", fontSize = 14.sp, color = colors.onSurfaceVariant)
                    } else {
                        NoDataText("Time to full: no data", 14.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        GraphCard(
            icon = Ic.Pulse,
            label = "Power",
            value = if (currentOk) f1(b.powerW) else null,
            unit = "W",
            accent = Accent.Green,
            history = h.powerW,
            minValue = -8f,
            maxValue = 16f
        )
        GraphCard(
            icon = Ic.Bolt,
            label = "Current",
            value = if (currentOk) "${b.currentMa}" else null,
            unit = "mA",
            accent = Accent.Blue,
            history = h.currentMa,
            minValue = -4500f,
            maxValue = 4500f
        )
        GraphCard(
            icon = Ic.Temp,
            label = "Temperature",
            value = f1(b.temperatureC),
            unit = "°C",
            accent = Accent.Orange,
            history = h.batteryTempC,
            minValue = 25f,
            maxValue = 50f
        )

        InfoGroup(
            title = "Live",
            rows = listOf(
                InfoItem(Ic.Bolt, "Voltage", if (b.voltageV > 0f) "${f2(b.voltageV)} V" else null),
                InfoItem(Ic.Pulse, "Current", if (currentOk) (if (b.currentMa > 0) "+" else "") + "${b.currentMa} mA" else null),
                InfoItem(Ic.Pulse, "Power", if (currentOk) "${f1(b.powerW)} W" else null),
                InfoItem(Ic.Temp, "Temperature", "${f1(b.temperatureC)} °C")
            )
        )

        InfoGroup(
            title = "Health",
            rows = listOf(
                InfoItem(Ic.Heart, "Health", reported(b.health)),
                InfoItem(Ic.Chip, "Technology", reported(b.technology)),
                InfoItem(Ic.Cycle, "Cycle count", if (b.cycleCount > 0) "${b.cycleCount}" else null),
                InfoItem(Ic.Heart, "State of health", if (b.stateOfHealthPercent > 0) "${b.stateOfHealthPercent}%" else null),
                InfoItem(Ic.Battery, "Remaining capacity", if (b.chargeCounterMah > 0) "${b.chargeCounterMah} mAh" else null)
            )
        )

        InfoGroup(
            title = "Charging",
            rows = listOf(
                InfoItem(Ic.Bolt, "Status", b.statusText),
                InfoItem(Ic.Plug, "Power source", b.plugged),
                InfoItem(
                    Ic.Clock,
                    "Time to full",
                    when {
                        !b.isCharging -> "Not charging"
                        b.chargeTimeRemainingMin > 0 -> formatHm(b.chargeTimeRemainingMin)
                        else -> null
                    }
                )
            )
        )

        FootNote("Some values depend on your device and Android version. Values the device does not report show “No data”.")
    }
}
