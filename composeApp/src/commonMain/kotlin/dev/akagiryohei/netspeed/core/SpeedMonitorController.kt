package dev.akagiryohei.netspeed.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the last measurement compares against the thresholds in [SpeedThresholds]. */
enum class SpeedRating { EXCELLENT, GOOD, POOR, UNKNOWN }

data class SpeedUiState(
    val isMonitoring: Boolean = false,
    val downloadMbps: Double? = null,
    val latencyMs: Long? = null,
    val rating: SpeedRating = SpeedRating.UNKNOWN,
    val samplesCollected: Int = 0,
    val statusMessage: String = "計測停止中",
)

/** Mbps / ms cut points that drive the traffic-light coloring in the UI. */
object SpeedThresholds {
    const val GOOD_MBPS = 25.0
    const val FAIR_MBPS = 5.0
    const val GOOD_LATENCY_MS = 60L
    const val FAIR_LATENCY_MS = 150L
}

/** Rating is the worse of the two dimensions: a fast-but-laggy VPN link should not read "excellent". */
fun rateSpeed(downloadMbps: Double?, latencyMs: Long?): SpeedRating {
    if (downloadMbps == null) return SpeedRating.UNKNOWN

    val speedScore = when {
        downloadMbps >= SpeedThresholds.GOOD_MBPS -> 2
        downloadMbps >= SpeedThresholds.FAIR_MBPS -> 1
        else -> 0
    }
    val latencyScore = when {
        latencyMs == null -> 1
        latencyMs <= SpeedThresholds.GOOD_LATENCY_MS -> 2
        latencyMs <= SpeedThresholds.FAIR_LATENCY_MS -> 1
        else -> 0
    }

    return when (minOf(speedScore, latencyScore)) {
        2 -> SpeedRating.EXCELLENT
        1 -> SpeedRating.GOOD
        else -> SpeedRating.POOR
    }
}

/**
 * Drives the ON/OFF monitoring loop: while ON, repeatedly measures latency + download
 * throughput and republishes [uiState] so the UI can keep the numbers live, e.g. to watch
 * a VPN connection settle after its handshake completes.
 */
class SpeedMonitorController(
    private val tester: NetworkSpeedTester,
    private val scope: CoroutineScope,
    private val intervalMillis: Long = 4_000L,
) {
    private val _uiState = MutableStateFlow(SpeedUiState())
    val uiState: StateFlow<SpeedUiState> = _uiState

    private var job: Job? = null

    fun toggle() {
        if (_uiState.value.isMonitoring) stop() else start()
    }

    fun start() {
        if (job?.isActive == true) return
        _uiState.update { it.copy(isMonitoring = true, statusMessage = "計測中…") }
        job = scope.launch {
            while (true) {
                _uiState.update { it.copy(statusMessage = "レイテンシ計測中…") }
                val latency = tester.measureLatencyMillis()

                _uiState.update { it.copy(statusMessage = "ダウンロード速度計測中…") }
                val mbps = tester.measureDownloadMbps()

                _uiState.update { current ->
                    val resolvedMbps = mbps ?: current.downloadMbps
                    val resolvedLatency = latency ?: current.latencyMs
                    current.copy(
                        downloadMbps = resolvedMbps,
                        latencyMs = resolvedLatency,
                        rating = rateSpeed(resolvedMbps, resolvedLatency),
                        samplesCollected = current.samplesCollected + 1,
                        statusMessage = if (mbps == null) "計測に失敗しました。再試行します…" else "計測中…",
                    )
                }
                delay(intervalMillis)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _uiState.update { it.copy(isMonitoring = false, statusMessage = "計測停止中") }
    }
}
