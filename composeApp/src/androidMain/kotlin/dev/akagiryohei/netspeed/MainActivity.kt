package dev.akagiryohei.netspeed

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import dev.akagiryohei.netspeed.core.AppGraph

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val state by AppGraph.controller.uiState.collectAsState()
            // Keeps the measurement loop alive (via a foreground service + notification) once
            // the app is backgrounded, e.g. while the user switches to a VPN app to toggle it.
            LaunchedEffect(state.isMonitoring) {
                if (state.isMonitoring) {
                    MonitorService.start(this@MainActivity)
                } else {
                    MonitorService.stop(this@MainActivity)
                }
            }
            App()
        }
    }
}
