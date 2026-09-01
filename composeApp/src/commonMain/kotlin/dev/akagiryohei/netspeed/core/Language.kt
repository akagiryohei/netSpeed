package dev.akagiryohei.netspeed.core

/** Two-letter language tag of the platform's current locale, e.g. "ja", "en". */
expect fun systemLanguageTag(): String

/** Minimal ja/en switcher so the UI doesn't need a full resource-bundle setup for two strings sets. */
object Strings {
    private val isJapanese: Boolean by lazy { systemLanguageTag().startsWith("ja") }

    fun of(japanese: String, english: String): String = if (isJapanese) japanese else english
}
