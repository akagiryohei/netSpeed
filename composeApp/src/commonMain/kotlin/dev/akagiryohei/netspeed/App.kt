package dev.akagiryohei.netspeed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.akagiryohei.netspeed.core.NetworkSpeedTester
import dev.akagiryohei.netspeed.core.SpeedMonitorController
import dev.akagiryohei.netspeed.core.SpeedRating
import dev.akagiryohei.netspeed.core.SpeedThresholds
import dev.akagiryohei.netspeed.core.SpeedUiState
import dev.akagiryohei.netspeed.core.createHttpClient

private val ColorExcellent = Color(0xFF2E7D32) // green - fast & responsive
private val ColorGood = Color(0xFFF9A825) // amber - usable but not great
private val ColorPoor = Color(0xFFC62828) // red - the VPN-handshake-but-crawling case
private val ColorUnknown = Color(0xFF9E9E9E)

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val controller = remember { SpeedMonitorController(NetworkSpeedTester(createHttpClient()), scope) }
    val state by controller.uiState.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("NetSpeed", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("ネットワーク速度モニター", fontSize = 14.sp, color = Color.Gray)

                Spacer(Modifier.height(40.dp))
                SpeedGauge(state)
                Spacer(Modifier.height(24.dp))

                Text(
                    text = state.statusMessage,
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
                if (state.samplesCollected > 0) {
                    Text(
                        text = "計測回数: ${state.samplesCollected}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }

                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = { controller.toggle() },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isMonitoring) ColorPoor else ColorExcellent,
                    ),
                ) {
                    Text(if (state.isMonitoring) "計測を停止 (OFF)" else "計測を開始 (ON)")
                }
            }
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
                Text("Mbps", fontSize = 16.sp, color = ratingColor)
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Ping: " + (state.latencyMs?.let { "$it ms" } ?: "--"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colorForLatency(state.latencyMs),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = ratingLabel(state.rating),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ratingColor,
        )
    }
}

private fun colorForRating(rating: SpeedRating): Color = when (rating) {
    SpeedRating.EXCELLENT -> ColorExcellent
    SpeedRating.GOOD -> ColorGood
    SpeedRating.POOR -> ColorPoor
    SpeedRating.UNKNOWN -> ColorUnknown
}

private fun colorForLatency(latencyMs: Long?): Color = when {
    latencyMs == null -> ColorUnknown
    latencyMs <= SpeedThresholds.GOOD_LATENCY_MS -> ColorExcellent
    latencyMs <= SpeedThresholds.FAIR_LATENCY_MS -> ColorGood
    else -> ColorPoor
}

private fun ratingLabel(rating: SpeedRating): String = when (rating) {
    SpeedRating.EXCELLENT -> "快適"
    SpeedRating.GOOD -> "普通"
    SpeedRating.POOR -> "低速"
    SpeedRating.UNKNOWN -> "計測待ち"
}

/** Common stdlib has no String.format; this keeps formatting identical on every target. */
private fun formatOneDecimal(value: Double): String {
    val scaledTenths = kotlin.math.round(value * 10).toLong().coerceAtLeast(0L)
    val whole = scaledTenths / 10
    val fraction = scaledTenths % 10
    return "$whole.$fraction"
}
