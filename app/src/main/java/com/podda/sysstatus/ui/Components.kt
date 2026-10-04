package com.podda.sysstatus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
        val stepX = size.width / (values.size - 1)

        val line = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value.coerceIn(minValue, maxValue) - minValue) / range) * size.height
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }

        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(path = area, color = color.copy(alpha = 0.12f))
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun BatteryRing(
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 88.dp
) {
    val track = MaterialTheme.colorScheme.background

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.11f
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
            fontSize = (diameter.value * 0.23f).sp,
            fontWeight = FontWeight.Medium
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
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .then(clickModifier)
            .padding(12.dp)
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent)
        )
        Spacer(Modifier.height(8.dp))
        Text(text = title, fontSize = 13.sp, color = colors.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Sparkline(
            values = history,
            color = accent,
            minValue = minValue,
            maxValue = maxValue,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        )
    }
}
