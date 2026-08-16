package dev.akagiryohei.netspeed.core

import io.ktor.client.HttpClient

/** Each platform supplies an Ktor engine suited to it (OkHttp on Android, CIO on desktop). */
expect fun createHttpClient(): HttpClient
