package com.system.sysstatus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.system.sysstatus.data.RequestLog
import com.system.sysstatus.data.StatsViewModel
import com.system.sysstatus.overlay.FloatingPrefs
import com.system.sysstatus.overlay.OverlayService
import com.system.sysstatus.ui.BatteryScreen
import com.system.sysstatus.ui.CpuScreen
import com.system.sysstatus.ui.FloatingSetupSheet
import com.system.sysstatus.ui.HomeScreen
import com.system.sysstatus.ui.LoadingView
import com.system.sysstatus.ui.LogScreen
import com.system.sysstatus.ui.MemoryScreen
import com.system.sysstatus.ui.Screen
import com.system.sysstatus.ui.SettingsScreen
import com.system.sysstatus.ui.SysNavBar
import com.system.sysstatus.ui.SysTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StatsViewModel by viewModels()

    // First-launch sheet that explains the floating window and the setting it needs
    private var showSheet by mutableStateOf(false)
    private var floatingOn by mutableStateOf(false)
    private var overlayGranted by mutableStateOf(false)
    private var waitingForOverlay = false

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showSheet = savedInstanceState?.getBoolean(KEY_SHEET)
            ?: !FloatingPrefs.onboardingSeen(this)
        waitingForOverlay = savedInstanceState?.getBoolean(KEY_WAITING) ?: false

        setContent {
            SysTheme {
                val state by viewModel.ui.collectAsStateWithLifecycle()
                var screen by rememberSaveable { mutableStateOf(Screen.Home) }

                // Requests are logged only while the Log screen is open
                LaunchedEffect(screen) { RequestLog.enabled = screen == Screen.Log }

                BackHandler(enabled = screen != Screen.Home) {
                    screen = Screen.Home
                }

                if (screen == Screen.Log) {
                    // The terminal keeps its own full-screen layout and [back] button
                    LogScreen(onBack = { screen = Screen.Home })
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            if (!state.loaded) {
                                LoadingView()
                            } else {
                                when (screen) {
                                    Screen.Home -> HomeScreen(
                                        state,
                                        onOpen = { screen = it },
                                        floatingOn = floatingOn,
                                        onFloating = ::toggleFloating
                                    )
                                    Screen.Battery -> BatteryScreen(state)
                                    Screen.Memory -> MemoryScreen(state)
                                    Screen.Cpu -> CpuScreen(state)
                                    Screen.Settings -> SettingsScreen(
                                        floatingOn = floatingOn,
                                        onFloating = ::toggleFloating
                                    )
                                    Screen.Log -> Unit
                                }
                            }
                        }
                        SysNavBar(current = screen, onSelect = { screen = it })
                    }
                }

                if (showSheet) {
                    FloatingSetupSheet(
                        onOpenSettings = ::requestOverlayAccess,
                        onDismiss = ::dismissSheet
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SHEET, showSheet)
        outState.putBoolean(KEY_WAITING, waitingForOverlay)
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = Settings.canDrawOverlays(this)

        when {
            // Back from the system settings page: start right away if the user allowed it
            waitingForOverlay -> {
                waitingForOverlay = false
                if (overlayGranted) {
                    enableFloating()
                    dismissSheet()
                }
            }
            // The user took the permission away in system settings
            FloatingPrefs.isEnabled(this) && !overlayGranted -> disableFloating()
            // Keep it running after the app was reopened
            FloatingPrefs.isEnabled(this) && !OverlayService.running -> OverlayService.start(this)
        }
        floatingOn = FloatingPrefs.isEnabled(this) && overlayGranted
    }

    private fun requestOverlayAccess() {
        if (Settings.canDrawOverlays(this)) {
            enableFloating()
            dismissSheet()
        } else {
            waitingForOverlay = true
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
    }

    private fun toggleFloating() {
        when {
            floatingOn -> disableFloating()
            overlayGranted -> enableFloating()
            else -> showSheet = true
        }
    }

    private fun enableFloating() {
        FloatingPrefs.setEnabled(this, true)
        floatingOn = true
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        OverlayService.start(this)
    }

    private fun disableFloating() {
        FloatingPrefs.setEnabled(this, false)
        floatingOn = false
        OverlayService.stop(this)
    }

    private fun dismissSheet() {
        FloatingPrefs.setOnboardingSeen(this, true)
        showSheet = false
    }

    private companion object {
        const val KEY_SHEET = "floating_sheet"
        const val KEY_WAITING = "floating_waiting"
    }
}
