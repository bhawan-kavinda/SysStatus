package com.podda.sysstatus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.podda.sysstatus.data.StatsViewModel
import com.podda.sysstatus.ui.BatteryScreen
import com.podda.sysstatus.ui.HomeScreen
import com.podda.sysstatus.ui.MemoryScreen
import com.podda.sysstatus.ui.Screen
import com.podda.sysstatus.ui.SysTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SysTheme {
                val state by viewModel.ui.collectAsStateWithLifecycle()
                var screen by rememberSaveable { mutableStateOf(Screen.Home) }

                BackHandler(enabled = screen != Screen.Home) {
                    screen = Screen.Home
                }

                when (screen) {
                    Screen.Home -> HomeScreen(state, onOpen = { screen = it })
                    Screen.Battery -> BatteryScreen(state, onBack = { screen = Screen.Home })
                    Screen.Memory -> MemoryScreen(state, onBack = { screen = Screen.Home })
                }
            }
        }
    }
}
