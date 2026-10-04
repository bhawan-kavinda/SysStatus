package com.podda.sysstatus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Sparkline(
    values: List<Float>,
    color: Color,
    minValue: Float,
    maxValue: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas

        val range = (maxValue - minValue).coerceAtLeast(0.0001f)
        val strokePx = 2.dp.toPx()
        val dotRadius = 3.dp.toPx()
        // Keep the line and the end dot fully inside the canvas
        val padY = dotRadius + strokePx
        val usableHeight = (size.height - padY * 2).coerceAtLeast(1f)
        val stepX = (size.width - dotRadius * 2) / (values.size - 1)

        val line = Path()
        var lastX = 0f
        var lastY = 0f
        values.forEachIndexed { index, value ->
            val x = dotRadius + index * stepX
            val y = padY + usableHeight -
                ((value.coerceIn(minValue, maxValue) - minValue) / range) * usableHeight
            if (index == 0) {
                line.moveTo(x, y)
            } else {
                // Smooth curve between points
                val midX = (lastX + x) / 2f
                line.cubicTo(midX, lastY, midX, y, x, y)
            }
            lastX = x
            lastY = y
        }

        val area = Path().apply {
            addPath(line)
            lineTo(lastX, size.height)
            lineTo(dotRadius, size.height)
            close()
        }

        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0f))
            )
        )
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(color = color, radius = dotRadius, center = Offset(lastX, lastY))
    }
}

@Composable
fun BatteryRing(
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 88.dp
) {
    val colors = MaterialTheme.colorScheme
    val track = colors.onSurface.copy(alpha = 0.08f)

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.12f
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * percent.coerceIn(0, 100) / 100f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$percent%",
            fontSize = (diameter.value * 0.24f).sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
    }
}

@Composable
fun StatTile(
    title: String,
    value: String,
    unit: String,
    accent: Color,
    history: List<Float>,
    minValue: Float,
    maxValue: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Column(
        modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .then(clickModifier)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Sparkline(
            values = history,
            color = accent,
            minValue = minValue,
            maxValue = maxValue,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        )
    }
}
