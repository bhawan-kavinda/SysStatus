package com.system.sysstatus.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.system.sysstatus.data.LogEntry
import com.system.sysstatus.data.LogStatus
import com.system.sysstatus.data.RequestLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Terminal palette, fixed so the screen stays black in both light and dark system themes
private val TermBg = Color(0xFF000000)
private val TermFg = Color(0xFFE6E6E6)
private val TermDim = Color(0xFF8E8E93)
private val TermGreen = Color(0xFF30D158)
private val TermYellow = Color(0xFFFFD60A)
private val TermRed = Color(0xFFFF5F56)
private val TermCyan = Color(0xFF64D2FF)

private const val TIME_WIDTH = 13

@Composable
fun LogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val live by RequestLog.entries.collectAsStateWithLifecycle()
    val totals by RequestLog.totals.collectAsStateWithLifecycle()

    var paused by remember { mutableStateOf(false) }
    var frozen by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    // The log stores newest first; a terminal shows oldest first with the newest at the bottom
    val display = remember(paused, frozen, live) { (if (paused) frozen else live).asReversed() }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val internet = remember { hasInternetPermission(context) }
    val listState = rememberLazyListState()

    // Light status/navigation bar icons on the black background, restored on exit
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars ?: false
        val previousNav = controller?.isAppearanceLightNavigationBars ?: false
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = previousStatus
            controller?.isAppearanceLightNavigationBars = previousNav
        }
    }

    // Follow the newest line unless paused
    val newestId = display.lastOrNull()?.id
    LaunchedEffect(newestId, paused) {
        if (!paused && display.isNotEmpty()) listState.scrollToItem(display.lastIndex)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(TermBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        TermText("sysstatus@device:~$ tail -f datalog", TermGreen, FontWeight.Bold)
        TermText(
            "total=${totals.total} ok=${totals.ok} no_data=${totals.noData} failed=${totals.failed}",
            TermDim
        )
        TermText(
            when (internet) {
                false -> "network: none (no INTERNET permission declared)"
                true -> "network: INTERNET permission is declared"
                null -> "network: could not be checked"
            },
            TermDim
        )
        TermText("recording only while this screen is open", TermDim)
        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TermButton("[back]", onBack)
            TermButton(if (paused) "[resume]" else "[pause]") {
                if (!paused) frozen = live
                paused = !paused
            }
            TermButton("[clear]") {
                RequestLog.clear()
                frozen = emptyList()
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(items = display, key = { it.id }) { entry ->
                Text(
                    text = entryText(entry, timeFormat.format(Date(entry.timeMs))),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TermText(text: String, color: Color, weight: FontWeight = FontWeight.Normal) {
    Text(
        text = text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = weight,
        fontSize = 12.sp,
        lineHeight = 17.sp
    )
}

@Composable
private fun TermButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = TermCyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    )
}

// Three lines per request: the request, the reply status, and the raw data received
private fun entryText(entry: LogEntry, time: String): AnnotatedString {
    val indent = " ".repeat(TIME_WIDTH)
    val statusColor = when (entry.status) {
        LogStatus.OK -> TermGreen
        LogStatus.NO_DATA -> TermYellow
        LogStatus.FAILED -> TermRed
    }
    val statusLabel = when (entry.status) {
        LogStatus.OK -> "OK"
        LogStatus.NO_DATA -> "NO DATA"
        LogStatus.FAILED -> "FAILED"
    }

    return buildAnnotatedString {
        withStyle(SpanStyle(color = TermDim)) { append("$time ") }
        withStyle(SpanStyle(color = TermCyan)) { append("> ") }
        withStyle(SpanStyle(color = TermFg)) { append("${entry.source}: ${entry.call}\n") }

        withStyle(SpanStyle(color = TermDim)) { append("$indent< ") }
        withStyle(SpanStyle(color = statusColor, fontWeight = FontWeight.Bold)) { append(statusLabel) }
        withStyle(SpanStyle(color = TermDim)) { append(" (${entry.micros} µs)\n") }

        withStyle(SpanStyle(color = TermDim)) { append("$indent= ") }
        withStyle(SpanStyle(color = TermFg)) { append(entry.result) }
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
