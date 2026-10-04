package com.system.sysstatus.overlay

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.system.sysstatus.R
import com.system.sysstatus.ui.AppLogoLayers

private val customIcons = listOf(
    R.drawable.ic_float_pulse,
    R.drawable.ic_float_bolt,
    R.drawable.ic_float_chip,
    R.drawable.ic_float_gauge
)

// index 0 = the app icon, 1..4 = custom icons
@Composable
fun FloatIcon(index: Int, modifier: Modifier = Modifier) {
    if (index <= 0) {
        AppLogoLayers(modifier)
    } else {
        Image(
            painter = painterResource(customIcons[(index - 1).coerceIn(0, customIcons.lastIndex)]),
            contentDescription = null,
            modifier = modifier
        )
    }
}
