package dev.akagiryohei.netspeed.core

/** What kind of link the device is currently on. VPN takes priority over the transport it rides on. */
enum class NetworkType { WIFI, CELLULAR, ETHERNET, VPN, UNKNOWN }

expect fun currentNetworkType(): NetworkType
