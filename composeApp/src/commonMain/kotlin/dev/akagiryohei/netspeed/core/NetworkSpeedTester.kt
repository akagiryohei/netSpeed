package dev.akagiryohei.netspeed.core

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

/**
 * Cloudflare's public speed-test endpoints. No API key required; used by many
 * open-source speed test tools (e.g. librespeed) for the same purpose.
 */
private const val LATENCY_CHECK_URL = "https://speed.cloudflare.com/cdn-cgi/trace"
private const val DOWNLOAD_TEST_URL = "https://speed.cloudflare.com/__down?bytes=4000000"
private const val DOWNLOAD_TIMEOUT_MS = 15_000L
private const val LATENCY_TIMEOUT_MS = 5_000L

/** Measures round-trip latency and download throughput against a fixed remote endpoint. */
class NetworkSpeedTester(private val client: HttpClient) {

    suspend fun measureLatencyMillis(): Long? = withTimeoutOrNull(LATENCY_TIMEOUT_MS) {
        val mark = TimeSource.Monotonic.markNow()
        client.get(LATENCY_CHECK_URL)
        mark.elapsedNow().inWholeMilliseconds
    }

    suspend fun measureDownloadMbps(): Double? = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
        val mark = TimeSource.Monotonic.markNow()
        val response: HttpResponse = client.get(DOWNLOAD_TEST_URL)
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(64 * 1024)
        var totalBytes = 0L
        while (true) {
            val read = channel.readAvailable(buffer)
            if (read == -1) break
            totalBytes += read
        }
        val elapsedSeconds = mark.elapsedNow().inWholeMilliseconds / 1000.0
        if (elapsedSeconds <= 0.0 || totalBytes <= 0L) return@withTimeoutOrNull null
        (totalBytes * 8) / elapsedSeconds / 1_000_000.0
    }
}
