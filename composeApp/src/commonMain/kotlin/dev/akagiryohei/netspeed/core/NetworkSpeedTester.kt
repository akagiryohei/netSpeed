package dev.akagiryohei.netspeed.core

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

/**
 * Cloudflare's public speed-test endpoints. No API key required; used by many
 * open-source speed test tools (e.g. librespeed) for the same purpose.
 */
private const val LATENCY_CHECK_URL = "https://speed.cloudflare.com/cdn-cgi/trace"
private const val DOWNLOAD_TEST_BASE_URL = "https://speed.cloudflare.com/__down?bytes="
private const val UPLOAD_TEST_URL = "https://speed.cloudflare.com/__up"
private const val DOWNLOAD_TIMEOUT_MS = 15_000L
private const val UPLOAD_TIMEOUT_MS = 15_000L
private const val LATENCY_TIMEOUT_MS = 5_000L

/** Measures round-trip latency and download/upload throughput against a fixed remote endpoint. */
class NetworkSpeedTester(private val client: HttpClient) {

    suspend fun measureLatencyMillis(): Long? = measureSafely(LATENCY_TIMEOUT_MS) {
        val mark = TimeSource.Monotonic.markNow()
        client.get(LATENCY_CHECK_URL)
        mark.elapsedNow().inWholeMilliseconds
    }

    suspend fun measureDownloadMbps(payloadBytes: Int): Double? = measureSafely(DOWNLOAD_TIMEOUT_MS) {
        val mark = TimeSource.Monotonic.markNow()
        val response: HttpResponse = client.get("$DOWNLOAD_TEST_BASE_URL$payloadBytes")
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(64 * 1024)
        var totalBytes = 0L
        while (true) {
            val read = channel.readAvailable(buffer)
            if (read == -1) break
            totalBytes += read
        }
        val elapsedSeconds = mark.elapsedNow().inWholeMilliseconds / 1000.0
        if (elapsedSeconds <= 0.0 || totalBytes <= 0L) return@measureSafely null
        bitsPerSecondToMbps(totalBytes * 8, elapsedSeconds)
    }

    suspend fun measureUploadMbps(payloadBytes: Int): Double? = measureSafely(UPLOAD_TIMEOUT_MS) {
        val payload = ByteArray(payloadBytes)
        val mark = TimeSource.Monotonic.markNow()
        client.post(UPLOAD_TEST_URL) {
            contentType(ContentType.Application.OctetStream)
            setBody(payload)
        }
        val elapsedSeconds = mark.elapsedNow().inWholeMilliseconds / 1000.0
        if (elapsedSeconds <= 0.0) return@measureSafely null
        bitsPerSecondToMbps(payloadBytes.toLong() * 8, elapsedSeconds)
    }

    /** Treats both a timeout and any network exception (offline, DNS failure, reset...) as "no result". */
    private suspend fun <T> measureSafely(timeoutMs: Long, block: suspend () -> T?): T? =
        try {
            withTimeoutOrNull(timeoutMs) { block() }
        } catch (e: Exception) {
            null
        }

    private fun bitsPerSecondToMbps(bits: Long, seconds: Double): Double = bits / seconds / 1_000_000.0
}
