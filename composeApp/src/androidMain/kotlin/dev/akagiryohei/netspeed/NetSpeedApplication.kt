package dev.akagiryohei.netspeed

import android.app.Application
import android.content.Context

/**
 * Holds a process-wide application Context so common `expect fun` platform hooks (settings
 * storage, network-type detection, locale lookup) can reach Android APIs without every call site
 * having to thread a Context through commonMain.
 */
class NetSpeedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
