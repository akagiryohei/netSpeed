package dev.akagiryohei.netspeed.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the last measurement compares against the current thresholds. */
enum class SpeedRating { EXCELLENT, GOOD, POOR, UNKNOWN }

/** What the monitoring loop is doing right now; the UI maps this to localized text. */
enum class MonitoringStatus { STOPPED, MEASURING_LATENCY, MEASURING_DOWNLOAD, MEASURING_UPLOAD, WAITING, OFFLINE_RETRYING }

data class SpeedUiState(
    val isMonitoring: Boolean = false,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val latencyMs: Long? = null,
    val rating: SpeedRating = SpeedRating.UNKNOWN,
    val samplesCollected: Int = 0,
    val status: MonitoringStatus = MonitoringStatus.STOPPED,
    val networkType: NetworkType = NetworkType.UNKNOWN,
    val dataSaverActive: Boolean = false,
    /** Last ~30 download samples (Mbps), oldest first, for the sparkline. */
    val history: List<Double> = emptyList(),
)

/** Default Mbps / ms cut points that drive the traffic-light coloring in the UI. */
object SpeedThresholds {
    const val DEFAULT_GOOD_MBPS = 25.0
    const val DEFAULT_FAIR_MBPS = 5.0
    const val DEFAULT_GOOD_LATENCY_MS = 60L
    const val DEFAULT_FAIR_LATENCY_MS = 150L
}

/** Rating is the worse of the two dimensions: a fast-but-laggy VPN link should not read "excellent". */
fun rateSpeed(
    downloadMbps: Double?,
    latencyMs: Long?,
    goodMbps: Double,
    fairMbps: Double,
    goodLatencyMs: Long,
    fairLatencyMs: Long,
): SpeedRating {
    if (downloadMbps == null) return SpeedRating.UNKNOWN

    val speedScore = when {
        downloadMbps >= goodMbps -> 2
        downloadMbps >= fairMbps -> 1
        else -> 0
    }
    val latencyScore = when {
        latencyMs == null -> 1
        latencyMs <= goodLatencyMs -> 2
        latencyMs <= fairLatencyMs -> 1
        else -> 0
    }

    return when (minOf(speedScore, latencyScore)) {
        2 -> SpeedRating.EXCELLENT
        1 -> SpeedRating.GOOD
        else -> SpeedRating.POOR
    }
}

/** Payload sizes and cadence used for one measurement round. */
private data class MeasurementProfile(
    val downloadBytes: Int,
    val uploadBytes: Int,
    val intervalMillis: Long,
)

private val NORMAL_PROFILE = MeasurementProfile(downloadBytes = 1_000_000, uploadBytes = 500_000, intervalMillis = 8_000L)
private val DATA_SAVER_PROFILE = MeasurementProfile(downloadBytes = 200_000, uploadBytes = 100_000, intervalMillis = 15_000L)
private const val MAX_BACKOFF_MILLIS = 60_000L
private const val HISTORY_LIMIT = 30

/**
 * Drives the ON/OFF monitoring loop: while ON, repeatedly measures latency + download/upload
 * throughput and republishes [uiState] so the UI (and, on Android, a foreground-service
 * notification) can keep the numbers live — e.g. to watch a VPN connection settle after its
 * handshake completes.
 *
 * On a cellular connection, a much smaller/less frequent profile is used automatically to avoid
 * burning mobile data, unless the user opts out via [allowFullSpeedOnCellular].
 */
class SpeedMonitorController(
    private val tester: NetworkSpeedTester,
    private val scope: CoroutineScope,
    private val settings: SettingsStore,
) {
    private val _uiState = MutableStateFlow(SpeedUiState())
    val uiState: StateFlow<SpeedUiState> = _uiState

    private var job: Job? = null
    private var consecutiveFailures = 0

    var intervalMillis: Long
        get() = settings.getLong(SettingsKeys.INTERVAL_MS, NORMAL_PROFILE.intervalMillis)
        set(value) = settings.putLong(SettingsKeys.INTERVAL_MS, value)

    var goodMbps: Double
        get() = settings.getDouble(SettingsKeys.GOOD_MBPS, SpeedThresholds.DEFAULT_GOOD_MBPS)
        set(value) = settings.putDouble(SettingsKeys.GOOD_MBPS, value)

    var fairMbps: Double
        get() = settings.getDouble(SettingsKeys.FAIR_MBPS, SpeedThresholds.DEFAULT_FAIR_MBPS)
        set(value) = settings.putDouble(SettingsKeys.FAIR_MBPS, value)

    var goodLatencyMs: Long
        get() = settings.getLong(SettingsKeys.GOOD_LATENCY_MS, SpeedThresholds.DEFAULT_GOOD_LATENCY_MS)
        set(value) = settings.putLong(SettingsKeys.GOOD_LATENCY_MS, value)

    var fairLatencyMs: Long
        get() = settings.getLong(SettingsKeys.FAIR_LATENCY_MS, SpeedThresholds.DEFAULT_FAIR_LATENCY_MS)
        set(value) = settings.putLong(SettingsKeys.FAIR_LATENCY_MS, value)

    var allowFullSpeedOnCellular: Boolean
        get() = settings.getBoolean(SettingsKeys.ALLOW_FULL_SPEED_ON_CELLULAR, false)
        set(value) = settings.putBoolean(SettingsKeys.ALLOW_FULL_SPEED_ON_CELLULAR, value)

    fun toggle() {
        if (_uiState.value.isMonitoring) stop() else start()
    }

    fun start() {
        if (job?.isActive == true) return
        consecutiveFailures = 0
        _uiState.update { it.copy(isMonitoring = true, status = MonitoringStatus.MEASURING_LATENCY) }
        job = scope.launch {
            while (true) {
                runMeasurementRound()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _uiState.update { it.copy(isMonitoring = false, status = MonitoringStatus.STOPPED) }
    }

    private suspend fun runMeasurementRound() {
        val networkType = currentNetworkType()
        val dataSaverActive = networkType == NetworkType.CELLULAR && !allowFullSpeedOnCellular
        val profile = if (dataSaverActive) DATA_SAVER_PROFILE else NORMAL_PROFILE

        _uiState.update { it.copy(status = MonitoringStatus.MEASURING_LATENCY, networkType = networkType, dataSaverActive = dataSaverActive) }
        val latency = tester.measureLatencyMillis()

        _uiState.update { it.copy(status = MonitoringStatus.MEASURING_DOWNLOAD) }
        val download = tester.measureDownloadMbps(profile.downloadBytes)

        _uiState.update { it.copy(status = MonitoringStatus.MEASURING_UPLOAD) }
        val upload = tester.measureUploadMbps(profile.uploadBytes)

        consecutiveFailures = if (latency == null && download == null) consecutiveFailures + 1 else 0

        _uiState.update { current ->
            val resolvedDownload = download ?: current.downloadMbps
            val resolvedLatency = latency ?: current.latencyMs
            val history = if (download != null) {
                (current.history + download).takeLast(HISTORY_LIMIT)
            } else {
                current.history
            }
            current.copy(
                downloadMbps = resolvedDownload,
                uploadMbps = upload ?: current.uploadMbps,
                latencyMs = resolvedLatency,
                rating = rateSpeed(resolvedDownload, resolvedLatency, goodMbps, fairMbps, goodLatencyMs, fairLatencyMs),
                samplesCollected = current.samplesCollected + 1,
                status = if (consecutiveFailures > 0) MonitoringStatus.OFFLINE_RETRYING else MonitoringStatus.WAITING,
                history = history,
            )
        }

        val baseInterval = maxOf(intervalMillis, profile.intervalMillis)
        val delayMillis = if (consecutiveFailures > 0) {
            (baseInterval shl minOf(consecutiveFailures, 4)).coerceAtMost(MAX_BACKOFF_MILLIS)
        } else {
            baseInterval
        }
        delay(delayMillis)
    }
}
