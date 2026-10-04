package com.system.sysstatus.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.system.sysstatus.data.BatteryStats
import com.system.sysstatus.data.CpuStats
import com.system.sysstatus.data.MemoryStats
import com.system.sysstatus.ui.Accent
import com.system.sysstatus.ui.AppLogo
import com.system.sysstatus.ui.AppLogoLayers
import com.system.sysstatus.ui.Ic
import com.system.sysstatus.ui.Sparkline
import com.system.sysstatus.ui.SysIcon
import com.system.sysstatus.ui.f0
import com.system.sysstatus.ui.f1
import com.system.sysstatus.ui.formatHm

data class FloatState(
    val loaded: Boolean = false,
    val battery: BatteryStats = BatteryStats(),
    val memory: MemoryStats = MemoryStats(),
    val cpu: CpuStats = CpuStats(),
    val power: List<Float> = emptyList(),
    val ram: List<Float> = emptyList(),
    val cpuLoad: List<Float> = emptyList()
)

private val Low = Color(0xFFE5484D)

// The floating icon: the app logo with a ring showing the battery level
@Composable
fun FloatingBubble(state: FloatState, faded: Boolean) {
    val b = state.battery
    val ring = when {
        !b.available -> Accent.Blue
        b.levelPercent > 50 -> Accent.Green
        b.levelPercent > 20 -> Accent.Orange
        else -> Low
    }
    val alpha by animateFloatAsState(if (faded) 0.65f else 1f, label = "bubbleAlpha")

    Box(
        Modifier
            .fillMaxSize()
            .alpha(alpha)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AppLogoLayers(Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.14f), -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (b.available) {
                drawArc(ring, -90f, 360f * b.levelPercent / 100f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
fun FloatingPanel(
    state: FloatState,
    onMinimize: () -> Unit,
    onOpenApp: () -> Unit,
    onTurnOff: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var tab by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surface)
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 2.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AppLogo(26.dp)
            Spacer(Modifier.width(8.dp))
            Text("SysStatus", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(colors.surfaceVariant).clickable(onClick = onMinimize),
                contentAlignment = Alignment.Center
            ) { Text("–", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.onSurface) }
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceVariant).padding(3.dp)
        ) {
            TabItem("Battery", Ic.Battery, tab == 0, Modifier.weight(1f)) { tab = 0 }
            TabItem("RAM", Ic.Ram, tab == 1, Modifier.weight(1f)) { tab = 1 }
            TabItem("CPU", Ic.Cpu, tab == 2, Modifier.weight(1f)) { tab = 2 }
        }
        Spacer(Modifier.height(10.dp))

        when {
            !state.loaded -> Text("Reading…", fontSize = 13.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(vertical = 40.dp))
            tab == 0 -> BatteryTab(state)
            tab == 1 -> RamTab(state)
            else -> CpuTab(state)
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Turn off",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(99.dp)).clickable(onClick = onTurnOff).padding(horizontal = 10.dp, vertical = 8.dp)
            )
            Text(
                "Open full app",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Accent.Blue).clickable(onClick = onOpenApp).padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TabItem(label: String, icon: Ic, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = if (selected) colors.onSurface else colors.onSurfaceVariant
    Row(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) colors.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SysIcon(icon, tint, 15.dp)
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.background).padding(horizontal = 13.dp, vertical = 11.dp)
    ) { content() }
}

@Composable
private fun Label(text: String) =
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun Value(text: String, unit: String = "") {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (unit.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Text(unit, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun NoData() = Text("No data", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun BatteryTab(state: FloatState) {
    val b = state.battery
    if (!b.available) { Card { NoData() }; return }
    val currentOk = b.currentAvailable
    Card {
        Label(b.statusText)
        Value("${b.levelPercent}", "%")
        Label(if (currentOk) "${f1(b.powerW)} W · ${f1(b.voltageV)} V" else "${f1(b.voltageV)} V")
    }
    if (currentOk) Card {
        Label("Power · last readings")
        Sparkline(state.power, Accent.Green, -8f, 16f, Modifier.fillMaxWidth().height(54.dp))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) { Card { Label("Temperature"); Value(f1(b.temperatureC), "°C") } }
        Box(Modifier.weight(1f)) {
            Card {
                Label("Full in")
                when {
                    !b.isCharging -> Text("Not charging", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 6.dp))
                    b.chargeTimeRemainingMin > 0 -> Value(formatHm(b.chargeTimeRemainingMin))
                    else -> NoData()
                }
            }
        }
    }
}

@Composable
private fun RamTab(state: FloatState) {
    val m = state.memory
    if (m.totalMb <= 0) { Card { NoData() }; return }
    Card {
        Label("RAM in use")
        Value(f1(m.usedMb / 1024f), "of ${f1(m.totalMb / 1024f)} GB")
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((m.usedPercent / 100f).coerceIn(0f, 1f)).clip(RoundedCornerShape(5.dp)).background(Accent.Violet))
        }
    }
    Card {
        Label("Recent usage")
        Sparkline(state.ram, Accent.Violet, 0f, 100f, Modifier.fillMaxWidth().height(54.dp))
    }
    Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Label("Available"); Label("${f1(m.availableMb / 1024f)} GB") }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Label("Used"); Label("${f1(m.usedPercent)}%") }
    }
}

@Composable
private fun CpuTab(state: FloatState) {
    val c = state.cpu
    if (!c.loadAvailable) { Card { Label("CPU load"); NoData() }; return }
    Card {
        Label("CPU load")
        Value(f0(c.totalLoad), "%")
        Sparkline(state.cpuLoad, Accent.Pink, 0f, 100f, Modifier.fillMaxWidth().height(54.dp))
    }
    c.thermalStatus?.let { Card { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Label("Thermal status"); Label(it) } } }
}
