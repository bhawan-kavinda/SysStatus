package com.system.sysstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Tab(val screen: Screen, val icon: Ic, val label: String)

private val tabs = listOf(
    Tab(Screen.Home, Ic.Home, "Home"),
    Tab(Screen.Battery, Ic.Battery, "Battery"),
    Tab(Screen.Memory, Ic.Ram, "Memory"),
    Tab(Screen.Cpu, Ic.Cpu, "CPU"),
    Tab(Screen.Log, Ic.Terminal, "Log"),
    Tab(Screen.Settings, Ic.Tune, "Settings")
)

@Composable
fun SysNavBar(current: Screen, onSelect: (Screen) -> Unit) {
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
        Row(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
            tabs.forEach { tab ->
                val on = tab.screen == current
                val tint = if (on) Accent.Blue else colors.onSurfaceVariant
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(tab.screen) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)
                ) {
                    SysIcon(tab.icon, tint, 22.dp)
                    Text(
                        tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = tint
                    )
                }
            }
        }
    }
}
