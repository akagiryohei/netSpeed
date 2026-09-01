package dev.akagiryohei.netspeed.core

import java.util.Locale

actual fun systemLanguageTag(): String = Locale.getDefault().language
