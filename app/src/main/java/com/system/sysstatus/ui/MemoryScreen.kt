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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.system.sysstatus.data.UiState

@Composable
fun MemoryScreen(state: UiState) {
    val colors = MaterialTheme.colorScheme
    val m = state.memory
    val h = state.history

    if (m.totalMb <= 0) {
        DetailScaffold(title = "Memory", subtitle = NO_DATA) {
            NoDataCard(Ic.Ram, "Memory", "No data: /proc/meminfo and ActivityManager returned nothing")
        }
        return
    }

    val detail = m.detailAvailable
    val swapOn = detail && m.swapTotalMb > 0
    val shape = RoundedCornerShape(22.dp)

    DetailScaffold(
        title = "Memory",
        subtitle = "${formatMb(m.totalMb)} RAM" + when {
            !detail -> ""
            swapOn -> " · ${formatMb(m.swapTotalMb)} swap"
            else -> " · swap off"
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.outlineVariant, shape)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(Ic.Ram, Accent.Violet)
                Spacer(Modifier.width(10.dp))
                Text("RAM in use", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(f1(m.usedMb / 1024f), fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    " of ${f1(m.totalMb / 1024f)} GB",
                    fontSize = 16.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight((m.usedMb.toFloat() / m.totalMb).coerceIn(0.001f, 1f))
                        .background(Accent.Violet)
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight((1f - m.usedMb.toFloat() / m.totalMb).coerceIn(0.001f, 1f))
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Dot(Accent.Violet)
                Text("Used ${formatMb(m.usedMb)} (${f1(m.usedPercent)}%)", fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Dot(colors.onSurface.copy(alpha = 0.15f))
                Text("Available ${formatMb(m.availableMb)}", fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))

        GraphCard(
            icon = Ic.Ram,
            label = "RAM used",
            value = f1(m.usedMb / 1024f),
            unit = "of ${f1(m.totalMb / 1024f)} GB",
            accent = Accent.Violet,
            history = h.ramPercent,
            minValue = 0f,
            maxValue = 100f
        )
        GraphCard(
            icon = Ic.Swap,
            label = "Swap used",
            value = when {
                !detail -> null
                swapOn -> f1(m.swapUsedMb / 1024f)
                else -> "Off"
            },
            unit = if (swapOn) "of ${f1(m.swapTotalMb / 1024f)} GB" else "",
            accent = Accent.Blue,
            history = h.swapPercent,
            minValue = 0f,
            maxValue = 100f
        )

        InfoGroup(
            title = "RAM",
            rows = listOf(
                InfoItem(Ic.Ram, "Total", formatMb(m.totalMb)),
                InfoItem(Ic.Ram, "Used", "${formatMb(m.usedMb)} (${f1(m.usedPercent)}%)"),
                InfoItem(Ic.Ram, "Available", formatMb(m.availableMb)),
                InfoItem(Ic.Ram, "Cached", if (detail) formatMb(m.cachedMb) else null),
                InfoItem(Ic.Ram, "Buffers", if (detail) formatMb(m.buffersMb) else null)
            )
        )

        InfoGroup(
            title = "Swap",
            rows = when {
                !detail -> listOf(InfoItem(Ic.Swap, "Status", null))
                swapOn -> listOf(
                    InfoItem(Ic.Swap, "Total", formatMb(m.swapTotalMb)),
                    InfoItem(Ic.Swap, "Used", "${formatMb(m.swapUsedMb)} (${f1(m.swapPercent)}%)"),
                    InfoItem(Ic.Swap, "Free", formatMb(m.swapFreeMb))
                )
                else -> listOf(InfoItem(Ic.Swap, "Status", "Not enabled on this device"))
            }
        )

        if (!detail) {
            FootNote("/proc/meminfo could not be read, so only totals from ActivityManager are shown.")
        }
    }
}
