package com.system.sysstatus.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    surfaceVariant = Color(0xFFE9E9EF),
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF6C6C70),
    outlineVariant = Color(0xFFE3E3E8)
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF0B0B0F),
    surface = Color(0xFF17171D),
    surfaceVariant = Color(0xFF22222A),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF9A9AA3),
    outlineVariant = Color(0xFF2A2A33)
)

@Composable
fun SysTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    ) {
        // Surface provides LocalContentColor; without it Text defaults to black (invisible on dark cards)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}
