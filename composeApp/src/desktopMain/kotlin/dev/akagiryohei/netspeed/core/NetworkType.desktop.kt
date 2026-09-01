package dev.akagiryohei.netspeed.core

import java.net.NetworkInterface
import java.util.Collections

private val VPN_HINTS = listOf("tun", "tap", "ppp", "wg", "utun", "ipsec", "vpn")
private val WIFI_HINTS = listOf("wlan", "wifi", "wi-fi", "wlp", "airport")
private val ETHERNET_HINTS = listOf("eth", "enp", "ens")

/**
 * Desktop OSes expose no simple "active network transport" API like Android does, so this is a
 * best-effort guess based on which interface is up and its (driver-assigned) name.
 */
actual fun currentNetworkType(): NetworkType {
    val activeInterfaces = try {
        Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback }
    } catch (e: Exception) {
        emptyList()
    }

    val names = activeInterfaces.mapNotNull { it.displayName?.lowercase() ?: it.name?.lowercase() }

    return when {
        names.any { name -> VPN_HINTS.any { name.contains(it) } } -> NetworkType.VPN
        names.any { name -> WIFI_HINTS.any { name.contains(it) } } -> NetworkType.WIFI
        names.any { name -> ETHERNET_HINTS.any { name.contains(it) } } -> NetworkType.ETHERNET
        activeInterfaces.isNotEmpty() -> NetworkType.UNKNOWN
        else -> NetworkType.UNKNOWN
    }
}
