package com.system.sysstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.system.sysstatus.overlay.FloatIcon
import com.system.sysstatus.overlay.FloatingBubble
import com.system.sysstatus.overlay.FloatingPrefs
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(floatingOn: Boolean, onFloating: () -> Unit) {
    val ctx = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var size by remember { mutableIntStateOf(FloatingPrefs.size(ctx)) }
    var icon by remember { mutableIntStateOf(FloatingPrefs.icon(ctx)) }
    var alpha by remember { mutableFloatStateOf(FloatingPrefs.alpha(ctx)) }
    var snap by remember { mutableStateOf(FloatingPrefs.snap(ctx)) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.padding(vertical = 4.dp))

        // 1. On / off
        SettingCard {
            SwitchRow(
                title = "Floating window",
                subtitle = if (floatingOn) "On · shown over other apps" else "Off",
                checked = floatingOn,
                onChange = { onFloating() }
            )
        }

        // 2. Size
        SettingCard {
            Text("Icon size", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                StepButton("−", enabled = size > FloatingPrefs.MIN_SIZE) {
                    size = (size - FloatingPrefs.SIZE_STEP).coerceAtLeast(FloatingPrefs.MIN_SIZE)
                    FloatingPrefs.setSize(ctx, size)
                }
                Box(
                    Modifier.size(112.dp).clip(RoundedCornerShape(20.dp)).background(colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(size.dp)) { FloatingBubble(icon, alpha) }
                }
                StepButton("+", enabled = size < FloatingPrefs.MAX_SIZE) {
                    size = (size + FloatingPrefs.SIZE_STEP).coerceAtMost(FloatingPrefs.MAX_SIZE)
                    FloatingPrefs.setSize(ctx, size)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("$size dp", fontSize = 13.sp, color = colors.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        // 3. Icon
        SettingCard {
            Text("Icon", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 0 until FloatingPrefs.ICON_COUNT) {
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (icon == i) Accent.Blue else Color.Transparent, CircleShape)
                            .clickable { icon = i; FloatingPrefs.setIcon(ctx, i) }
                            .padding(4.dp)
                    ) { FloatIcon(i, Modifier.fillMaxSize().clip(CircleShape)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (icon == 0) "App icon (default)" else "Custom icon $icon",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )
        }

        // 4. Transparency
        SettingCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Transparency", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Text("${(alpha * 100).roundToInt()}%", fontSize = 14.sp, color = colors.onSurfaceVariant)
            }
            Slider(
                value = alpha,
                onValueChange = { alpha = it; FloatingPrefs.setAlpha(ctx, it) },
                valueRange = FloatingPrefs.MIN_ALPHA..1f,
                colors = SliderDefaults.colors(thumbColor = Accent.Blue, activeTrackColor = Accent.Blue)
            )
        }

        // 5. Snap
        SettingCard {
            SwitchRow(
                title = "Snap to edge",
                subtitle = "The icon sticks to the nearest side when you let go",
                checked = snap,
                onChange = { snap = it; FloatingPrefs.setSnap(ctx, it) }
            )
        }

        Text(
            "Drag the icon to move it. The panel can be moved by dragging its top bar.",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Text(subtitle, fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Accent.Green, checkedThumbColor = Color.White)
        )
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.3f))
    }
}
