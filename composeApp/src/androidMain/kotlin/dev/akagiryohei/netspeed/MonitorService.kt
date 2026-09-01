package dev.akagiryohei.netspeed

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dev.akagiryohei.netspeed.core.AppGraph
import dev.akagiryohei.netspeed.core.SpeedUiState
import dev.akagiryohei.netspeed.core.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps [AppGraph.controller]'s measurement loop alive (and visible via a persistent
 * notification) while the app is backgrounded, e.g. while the user switches over to a VPN app
 * to toggle it on/off and watch the number react. It does not own start/stop of the monitoring
 * loop itself — the UI's ON/OFF button does that; this service just mirrors that state into a
 * notification and stops itself once monitoring turns off.
 */
class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification(buildNotification(AppGraph.controller.uiState.value))

        collectJob?.cancel()
        collectJob = scope.launch {
            AppGraph.controller.uiState.collect { state ->
                if (!state.isMonitoring) {
                    stopSelf()
                    return@collect
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(state))
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        collectJob?.cancel()
        super.onDestroy()
    }

    private fun startForegroundWithNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: SpeedUiState): Notification {
        val speedText = state.downloadMbps?.let { String.format("%.1f Mbps", it) }
            ?: Strings.of("計測中…", "Measuring…")
        val latencyText = state.latencyMs?.let { "Ping ${it}ms" } ?: ""
        val contentText = listOf(speedText, latencyText).filter { it.isNotBlank() }.joinToString("  •  ")

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("NetSpeed")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_netspeed)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            Strings.of("ネットワーク速度モニター", "Network speed monitor"),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "netspeed_monitor"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, MonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
