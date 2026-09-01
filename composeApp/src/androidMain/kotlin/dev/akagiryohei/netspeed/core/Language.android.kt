package dev.akagiryohei.netspeed.core

import dev.akagiryohei.netspeed.NetSpeedApplication

actual fun systemLanguageTag(): String =
    NetSpeedApplication.appContext.resources.configuration.locales[0].language
