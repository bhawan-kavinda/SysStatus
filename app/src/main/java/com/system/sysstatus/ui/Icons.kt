package com.system.sysstatus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Line icons drawn from SVG path data on a 24 x 24 grid, so no icon library is needed
enum class Ic(val path: String) {
    Home("M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"),
    Battery("M4.5 7h12A2.5 2.5 0 0 1 19 9.5v5a2.5 2.5 0 0 1-2.5 2.5h-12A2.5 2.5 0 0 1 2 14.5v-5A2.5 2.5 0 0 1 4.5 7z M22 11v2"),
    Bolt("M13 2L4 14h7l-1 8 9-12h-7z"),
    Ram("M5 7h14a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z M8 3v4M12 3v4M16 3v4M8 17v4M12 17v4M16 17v4"),
    Swap("M7 7h13M17 4l3 3-3 3M17 17H4M7 14l-3 3 3 3"),
    Cpu("M8 6h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z M10 10h4v4h-4z M9 2v4M15 2v4M9 18v4M15 18v4M2 9h4M2 15h4M18 9h4M18 15h4"),
    Temp("M14 14.8V5a2 2 0 0 0-4 0v9.8a4 4 0 1 0 4 0z"),
    Terminal("M4 17l6-5-6-5M12 19h8"),
    Pulse("M3 12h4l3-8 4 16 3-8h4"),
    Heart("M20.8 5.6a5 5 0 0 0-7.1 0L12 7.3l-1.7-1.7a5 5 0 0 0-7.1 7.1L12 21.5l8.8-8.8a5 5 0 0 0 0-7.1z"),
    Clock("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z M12 7v5l3 2"),
    Plug("M9 2v6M15 2v6M6 8h12v4a6 6 0 0 1-12 0zM12 18v4"),
    Cycle("M20 11a8 8 0 1 0-2.3 5.7M20 4v7h-7"),
    Chevron("M9 6l6 6-6 6"),
    Chip("M7 4h10a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3z M9 12h6M12 9v6"),
    Lock("M7 11h10a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1z M8 11V8a4 4 0 0 1 8 0v3"),
    Tune("M4 7h8M18 7h2M4 17h2M12 17h8M15 7m-3 0a3 3 0 1 0 6 0a3 3 0 1 0-6 0M9 17m-3 0a3 3 0 1 0 6 0a3 3 0 1 0-6 0")
}

@Composable
fun SysIcon(icon: Ic, tint: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    val path = remember(icon) { PathParser().parsePathString(icon.path).toPath() }
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        scale(scaleX = unit, scaleY = unit, pivot = Offset.Zero) {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
