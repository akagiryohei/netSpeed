package dev.akagiryohei.netspeed.core

/** Tiny key-value store for user-adjustable settings; each platform backs it with its native prefs API. */
interface SettingsStore {
    fun getDouble(key: String, default: Double): Double
    fun putDouble(key: String, value: Double)
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

expect fun createSettingsStore(): SettingsStore

object SettingsKeys {
    const val INTERVAL_MS = "interval_ms"
    const val GOOD_MBPS = "good_mbps"
    const val FAIR_MBPS = "fair_mbps"
    const val GOOD_LATENCY_MS = "good_latency_ms"
    const val FAIR_LATENCY_MS = "fair_latency_ms"
    const val ALLOW_FULL_SPEED_ON_CELLULAR = "allow_full_speed_on_cellular"
}
