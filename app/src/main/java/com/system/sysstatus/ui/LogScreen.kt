package com.system.sysstatus.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.system.sysstatus.data.LogEntry
import com.system.sysstatus.data.LogStatus
import com.system.sysstatus.data.RequestLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val live by RequestLog.entries.collectAsStateWithLifecycle()
    val totals by RequestLog.totals.collectAsStateWithLifecycle()

    var paused by remember { mutableStateOf(false) }
    var frozen by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    val shown = if (paused) frozen else live

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val internet = remember { hasInternetPermission(context) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "‹ Home",
            fontSize = 17.sp,
            color = Accent.Blue,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Text(
            text = "Data log",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
            modifier = Modifier.padding(start = 4.dp)
        )
        Text(
            text = "Every row is one real call to Android, recorded only while this screen is open. Compare the raw result with the numbers on the other screens.",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CountChip("Total", totals.total, colors.onSurface, Modifier.weight(1f))
            CountChip("OK", totals.ok, Accent.Green, Modifier.weight(1f))
            CountChip("No data", totals.noData, Accent.Yellow, Modifier.weight(1f))
            CountChip("Failed", totals.failed, Accent.Orange, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))

        Text(
            text = when (internet) {
                false -> "Network access: none. This app has no INTERNET permission, so it cannot send data anywhere."
                true -> "Network access: INTERNET permission is declared."
                null -> "Network access: could not be checked."
            },
            fontSize = 12.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill(if (paused) "Resume" else "Pause") {
                if (!paused) frozen = live
                paused = !paused
            }
            ActionPill("Clear") {
                RequestLog.clear()
                frozen = emptyList()
            }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = shown, key = { it.id }) { entry ->
                LogRow(entry, timeFormat)
            }
        }
    }
}

@Composable
private fun CountChip(label: String, count: Long, tint: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 11.sp, color = colors.onSurfaceVariant)
        Text(
            count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}

@Composable
private fun ActionPill(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun LogRow(entry: LogEntry, timeFormat: SimpleDateFormat) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    val tint = when (entry.status) {
        LogStatus.OK -> Accent.Green
        LogStatus.NO_DATA -> Accent.Yellow
        LogStatus.FAILED -> Accent.Orange
    }
    val label = when (entry.status) {
        LogStatus.OK -> "OK"
        LogStatus.NO_DATA -> "NO DATA"
        LogStatus.FAILED -> "FAILED"
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tint.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = timeFormat.format(Date(entry.timeMs)),
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${entry.micros} µs",
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${entry.source} · ${entry.call}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = entry.result,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = colors.onSurfaceVariant
        )
    }
}

// Null when the check itself fails, so the screen never claims "no network" without proof
private fun hasInternetPermission(context: Context): Boolean? = try {
    @Suppress("DEPRECATION")
    val info = context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.GET_PERMISSIONS
    )
    info.requestedPermissions?.contains(Manifest.permission.INTERNET) == true
} catch (e: Exception) {
    null
}
