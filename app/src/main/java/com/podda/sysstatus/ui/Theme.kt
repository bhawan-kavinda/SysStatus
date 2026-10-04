package com.podda.sysstatus.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Accent {
    val Blue = Color(0xFF2A78D6)
    val Orange = Color(0xFFEB6834)
    val Green = Color(0xFF1BAF7A)
    val Violet = Color(0xFF6250D6)
    val Yellow = Color(0xFFEDA100)
    val Pink = Color(0xFFE87BA4)
}

private val LightColors = lightColorScheme(
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF6C6C70)
)

private val DarkColors = darkColorScheme(
    background = Color.Black,
    surface = Color(0xFF1C1C1E),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF98989D)
)

@Composable
fun SysTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
