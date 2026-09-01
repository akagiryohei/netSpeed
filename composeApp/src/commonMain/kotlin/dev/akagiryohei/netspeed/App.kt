package dev.akagiryohei.netspeed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.akagiryohei.netspeed.core.AppGraph
import dev.akagiryohei.netspeed.core.MonitoringStatus
import dev.akagiryohei.netspeed.core.NetworkType
import dev.akagiryohei.netspeed.core.SpeedMonitorController
import dev.akagiryohei.netspeed.core.SpeedRating
import dev.akagiryohei.netspeed.core.SpeedThresholds
import dev.akagiryohei.netspeed.core.SpeedUiState
import dev.akagiryohei.netspeed.core.Strings

private val ColorExcellent = Color(0xFF2E7D32) // green - fast & responsive
private val ColorGood = Color(0xFFF9A825) // amber - usable but not great
private val ColorPoor = Color(0xFFC62828) // red - the VPN-handshake-but-crawling case
private val ColorUnknown = Color(0xFF9E9E9E)

private const val UPLOAD_GOOD_MBPS = 10.0
private const val UPLOAD_FAIR_MBPS = 2.0

@Composable
fun App() {
    val controller = AppGraph.controller
    val state by controller.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NetSpeed", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            Strings.of("ネットワーク速度モニター", "Network Speed Monitor"),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                NetworkBadge(state)

                Spacer(Modifier.height(28.dp))
                SpeedGauge(state)
                Spacer(Modifier.height(16.dp))
                HistorySparkline(state)

                Spacer(Modifier.height(20.dp))
                Text(
                    text = statusText(state.status),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.samplesCollected > 0) {
                    Text(
                        text = Strings.of("計測回数: ${state.samplesCollected}", "Samples: ${state.samplesCollected}"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { controller.toggle() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isMonitoring) ColorPoor else ColorExcellent,
                        ),
                    ) {
                        Text(
                            if (state.isMonitoring) Strings.of("計測を停止 (OFF)", "Stop (OFF)")
                            else Strings.of("計測を開始 (ON)", "Start (ON)"),
                        )
                    }
                    TextButton(onClick = { showSettings = true }) {
                        Text(Strings.of("⚙ 設定", "⚙ Settings"))
                    }
                }
            }
        }

        if (showSettings) {
            SettingsDialog(controller = controller, onDismiss = { showSettings = false })
        }
    }
}

@Composable
private fun NetworkBadge(state: SpeedUiState) {
    val label = when (state.networkType) {
        NetworkType.VPN -> Strings.of("VPN経由", "Via VPN")
        NetworkType.WIFI -> Strings.of("Wi-Fi", "Wi-Fi")
        NetworkType.CELLULAR -> Strings.of("モバイル回線", "Cellular")
        NetworkType.ETHERNET -> Strings.of("有線LAN", "Ethernet")
        NetworkType.UNKNOWN -> Strings.of("接続確認中", "Detecting…")
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        if (state.dataSaverActive) {
            Spacer(Modifier.height(4.dp))
            Text(
                Strings.of("データ節約モード（テストサイズを縮小中）", "Data saver (smaller test size)"),
                fontSize = 11.sp,
                color = ColorGood,
            )
        }
    }
}

@Composable
private fun SpeedGauge(state: SpeedUiState) {
    val ratingColor = colorForRating(state.rating)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(color = ratingColor.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.downloadMbps?.let(::formatOneDecimal) ?: "--",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = ratingColor,
                )
                Text("Mbps " + Strings.of("(下り)", "(down)"), fontSize = 16.sp, color = ratingColor)
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Ping: " + (state.latencyMs?.let { "$it ms" } ?: "--"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colorForLatency(state.latencyMs),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = Strings.of("上り: ", "Up: ") + (state.uploadMbps?.let { formatOneDecimal(it) + " Mbps" } ?: "--"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorForUpload(state.uploadMbps),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = ratingLabel(state.rating),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ratingColor,
        )
    }
}

@Composable
private fun HistorySparkline(state: SpeedUiState) {
    val history = state.history
    if (history.size < 2) return
    val lineColor = colorForRating(state.rating)
    val maxValue = (history.maxOrNull() ?: 1.0).coerceAtLeast(1.0)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .height(48.dp),
    ) {
        val stepX = size.width / (history.size - 1).coerceAtLeast(1)
        var previous: Offset? = null
        history.forEachIndexed { index, value ->
            val x = stepX * index
            val y = size.height - (value / maxValue * size.height).toFloat()
            val point = Offset(x, y)
            previous?.let { start ->
                drawLine(color = lineColor, start = start, end = point, strokeWidth = 4f, cap = StrokeCap.Round)
            }
            previous = point
        }
    }
}

@Composable
private fun SettingsDialog(controller: SpeedMonitorController, onDismiss: () -> Unit) {
    var intervalSeconds by remember { mutableStateOf((controller.intervalMillis / 1000L).toFloat()) }
    var goodMbps by remember { mutableStateOf(controller.goodMbps.toFloat()) }
    var fairMbps by remember { mutableStateOf(controller.fairMbps.toFloat()) }
    var allowFullSpeedOnCellular by remember { mutableStateOf(controller.allowFullSpeedOnCellular) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.of("設定", "Settings")) },
        text = {
            Column {
                Text(Strings.of("計測間隔: ${intervalSeconds.toInt()}秒", "Interval: ${intervalSeconds.toInt()}s"))
                Slider(
                    value = intervalSeconds,
                    onValueChange = {
                        intervalSeconds = it
                        controller.intervalMillis = it.toLong() * 1000L
                    },
                    valueRange = 4f..30f,
                )

                Spacer(Modifier.height(8.dp))
                Text(Strings.of("快適の下限: ${goodMbps.toInt()} Mbps", "\"Excellent\" floor: ${goodMbps.toInt()} Mbps"))
                Slider(
                    value = goodMbps,
                    onValueChange = {
                        goodMbps = it
                        controller.goodMbps = it.toDouble()
                    },
                    valueRange = 5f..100f,
                )

                Spacer(Modifier.height(8.dp))
                Text(Strings.of("普通の下限: ${fairMbps.toInt()} Mbps", "\"Fair\" floor: ${fairMbps.toInt()} Mbps"))
                Slider(
                    value = fairMbps,
                    onValueChange = {
                        fairMbps = it
                        controller.fairMbps = it.toDouble()
                    },
                    valueRange = 1f..30f,
                )

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = allowFullSpeedOnCellular,
                        onCheckedChange = {
                            allowFullSpeedOnCellular = it
                            controller.allowFullSpeedOnCellular = it
                        },
                    )
                    Text(
                        Strings.of(
                            "モバイル回線でもデータ節約をしない",
                            "Don't data-saver on cellular",
                        ),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(Strings.of("閉じる", "Close")) }
        },
    )
}

private fun statusText(status: MonitoringStatus): String = when (status) {
    MonitoringStatus.STOPPED -> Strings.of("計測停止中", "Stopped")
    MonitoringStatus.MEASURING_LATENCY -> Strings.of("レイテンシ計測中…", "Measuring latency…")
    MonitoringStatus.MEASURING_DOWNLOAD -> Strings.of("ダウンロード速度計測中…", "Measuring download…")
    MonitoringStatus.MEASURING_UPLOAD -> Strings.of("アップロード速度計測中…", "Measuring upload…")
    MonitoringStatus.WAITING -> Strings.of("計測中…", "Monitoring…")
    MonitoringStatus.OFFLINE_RETRYING -> Strings.of("接続なし。再試行しています…", "Offline, retrying…")
}

private fun colorForRating(rating: SpeedRating): Color = when (rating) {
    SpeedRating.EXCELLENT -> ColorExcellent
    SpeedRating.GOOD -> ColorGood
    SpeedRating.POOR -> ColorPoor
    SpeedRating.UNKNOWN -> ColorUnknown
}

private fun colorForLatency(latencyMs: Long?): Color = when {
    latencyMs == null -> ColorUnknown
    latencyMs <= SpeedThresholds.DEFAULT_GOOD_LATENCY_MS -> ColorExcellent
    latencyMs <= SpeedThresholds.DEFAULT_FAIR_LATENCY_MS -> ColorGood
    else -> ColorPoor
}

private fun colorForUpload(uploadMbps: Double?): Color = when {
    uploadMbps == null -> ColorUnknown
    uploadMbps >= UPLOAD_GOOD_MBPS -> ColorExcellent
    uploadMbps >= UPLOAD_FAIR_MBPS -> ColorGood
    else -> ColorPoor
}

private fun ratingLabel(rating: SpeedRating): String = when (rating) {
    SpeedRating.EXCELLENT -> Strings.of("快適", "Excellent")
    SpeedRating.GOOD -> Strings.of("普通", "Fair")
    SpeedRating.POOR -> Strings.of("低速", "Poor")
    SpeedRating.UNKNOWN -> Strings.of("計測待ち", "Waiting")
}

/** Common stdlib has no String.format; this keeps formatting identical on every target. */
private fun formatOneDecimal(value: Double): String {
    val scaledTenths = kotlin.math.round(value * 10).toLong().coerceAtLeast(0L)
    val whole = scaledTenths / 10
    val fraction = scaledTenths % 10
    return "$whole.$fraction"
}
