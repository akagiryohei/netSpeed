package dev.akagiryohei.netspeed.core

import java.util.prefs.Preferences

private class DesktopSettingsStore(
    private val prefs: Preferences = Preferences.userRoot().node("dev/akagiryohei/netspeed"),
) : SettingsStore {
    override fun getDouble(key: String, default: Double): Double = prefs.getDouble(key, default)
    override fun putDouble(key: String, value: Double) = prefs.putDouble(key, value)
    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)
    override fun putLong(key: String, value: Long) = prefs.putLong(key, value)
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.putBoolean(key, value)
}

actual fun createSettingsStore(): SettingsStore = DesktopSettingsStore()
