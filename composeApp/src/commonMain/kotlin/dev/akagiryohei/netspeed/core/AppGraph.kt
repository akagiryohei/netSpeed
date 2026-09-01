package dev.akagiryohei.netspeed.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-wide singletons. A single [SpeedMonitorController] instance (rather than one scoped to
 * a Composable) is what lets the Android foreground service and the UI observe the exact same
 * ongoing measurement loop instead of racing two independent ones.
 */
object AppGraph {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val httpClient by lazy { createHttpClient() }
    private val settings by lazy { createSettingsStore() }

    val controller: SpeedMonitorController by lazy {
        SpeedMonitorController(NetworkSpeedTester(httpClient), applicationScope, settings)
    }
}
