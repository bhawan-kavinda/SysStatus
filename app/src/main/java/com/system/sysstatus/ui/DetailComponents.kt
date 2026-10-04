package com.system.sysstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal fun f0(value: Float): String = String.format(Locale.US, "%.0f", value)

internal fun f1(value: Float): String = String.format(Locale.US, "%.1f", value)

internal fun f2(value: Float): String = String.format(Locale.US, "%.2f", value)

internal fun formatMb(mb: Long): String =
    if (mb >= 1024) "${f1(mb / 1024f)} GB" else "$mb MB"

internal fun formatKhz(khz: Long): String =
    if (khz >= 1_000_000) "${f2(khz / 1_000_000f)} GHz" else "${khz / 1000} MHz"

// The system reports "Unknown" when a device does not provide the value
internal fun reported(value: String): String? =
    if (value.isBlank() || value == "Unknown") null else value

@Composable
fun DetailScaffold(
    title: String,
    subtitle: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )
        if (subtitle != null) {
            Text(subtitle, fontSize = 14.sp, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

// A card that only states why there is nothing to show
@Composable
fun NoDataCard(icon: Ic, title: String, reason: String) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconChip(icon, Accent.Orange, boxSize = 40.dp)
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Text(reason, fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
fun GraphCard(
    icon: Ic,
    label: String,
    value: String?,
    unit: String,
    accent: Color,
    history: List<Float>,
    minValue: Float,
    maxValue: Float,
    emptyText: String = NO_DATA,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon, accent)
            Spacer(Modifier.width(10.dp))
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        if (value == null) {
            NoDataText(emptyText, 20.sp)
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                if (unit.isNotEmpty()) {
                    Text(
                        text = " $unit",
                        fontSize = 16.sp,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
            }
            if (history.size >= 2) {
                Spacer(Modifier.height(10.dp))
                Sparkline(
                    values = history,
                    color = accent,
                    minValue = minValue,
                    maxValue = maxValue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

// value == null means the system did not provide it; the row then shows `missing`
data class InfoItem(
    val icon: Ic,
    val label: String,
    val value: String?,
    val missing: String = NO_DATA
)

@Composable
fun InfoGroup(title: String, rows: List<InfoItem>) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Text(
        text = title,
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
    ) {
        rows.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 46.dp),
                    thickness = 0.5.dp,
                    color = colors.onSurface.copy(alpha = 0.1f)
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SysIcon(item.icon, colors.onSurfaceVariant, 18.dp)
                Spacer(Modifier.width(12.dp))
                Text(text = item.label, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (item.value == null) {
                    NoDataText(item.missing, 15.sp)
                } else {
                    Text(
                        text = item.value,
                        fontSize = 15.sp,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
fun FootNote(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
internal fun Dot(color: Color) {
    Box(
        Modifier
            .width(9.dp)
            .height(9.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
}
