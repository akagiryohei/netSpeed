package dev.akagiryohei.netspeed.core

import android.content.Context
import android.content.SharedPreferences
import dev.akagiryohei.netspeed.NetSpeedApplication

private class AndroidSettingsStore(
    private val prefs: SharedPreferences =
        NetSpeedApplication.appContext.getSharedPreferences("netspeed_settings", Context.MODE_PRIVATE),
) : SettingsStore {
    // SharedPreferences has no native double type; store as its bit pattern in a Long.
    override fun getDouble(key: String, default: Double): Double =
        Double.fromBits(prefs.getLong(doubleKey(key), default.toRawBits()))

    override fun putDouble(key: String, value: Double) {
        prefs.edit().putLong(doubleKey(key), value.toRawBits()).apply()
    }

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)
    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    private fun doubleKey(key: String) = "${key}_double_bits"
}

actual fun createSettingsStore(): SettingsStore = AndroidSettingsStore()
