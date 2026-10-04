package com.podda.sysstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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

internal fun f1(value: Float): String = String.format(Locale.US, "%.1f", value)

internal fun f2(value: Float): String = String.format(Locale.US, "%.2f", value)

internal fun formatMb(mb: Long): String =
    if (mb >= 1024) "${f1(mb / 1024f)} GB" else "$mb MB"

@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
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
            .navigationBarsPadding()
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
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun GraphCard(
    label: String,
    value: String,
    unit: String,
    accent: Color,
    history: List<Float>,
    minValue: Float,
    maxValue: Float,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(14.dp)
    ) {
        Text(text = label, fontSize = 13.sp, color = colors.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, fontSize = 34.sp, fontWeight = FontWeight.Medium)
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    fontSize = 16.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
        }
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

@Composable
fun InfoGroup(title: String, rows: List<Pair<String, String>>) {
    val colors = MaterialTheme.colorScheme

    Text(
        text = title,
        fontSize = 13.sp,
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
    ) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 14.dp),
                    thickness = 0.5.dp,
                    color = colors.onSurface.copy(alpha = 0.1f)
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, fontSize = 15.sp)
                Text(
                    text = value,
                    fontSize = 15.sp,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
    Spacer(Modifier.height(14.dp))
}
