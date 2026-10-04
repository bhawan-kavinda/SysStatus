package com.system.sysstatus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.system.sysstatus.R

// Both launcher-icon layers, scaled so the circle shows exactly what the launcher mask shows
@Composable
fun AppLogoLayers(modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().scale(1.5f)
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().scale(1.5f)
        )
    }
}

// The launcher icon, drawn from the same resources, so the app icon and the floating icon match
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = CircleShape) {
    AppLogoLayers(modifier.size(size).clip(shape))
}

// Shown on first launch (and from Home) to explain the floating window and the one setting it needs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingSetupSheet(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            AppLogo(56.dp)
            Spacer(Modifier.height(16.dp))
            Text("Floating window", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                "Keep battery, RAM and CPU in a small icon on top of any app. Tap it for details, drag it anywhere.",
                fontSize = 15.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "To turn it on, allow one setting",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Step(1, "Tap “Open settings” below.")
            Step(2, "Choose System Status in the list if it asks.")
            Step(3, "Switch on “Allow display over other apps”, then come back here.")
            Spacer(Modifier.height(8.dp))
            Text(
                "The icon appears as soon as you return. You can turn it off any time from Home.",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent.Blue, contentColor = Color.White)
            ) { Text("Open settings", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Not now", fontSize = 15.sp, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.Start) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) { Text("$number", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onSurface) }
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = colors.onSurface)
    }
}
