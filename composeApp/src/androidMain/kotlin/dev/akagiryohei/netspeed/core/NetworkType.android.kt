package dev.akagiryohei.netspeed.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.akagiryohei.netspeed.NetSpeedApplication

actual fun currentNetworkType(): NetworkType {
    val connectivityManager = NetSpeedApplication.appContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return NetworkType.UNKNOWN

    val network = connectivityManager.activeNetwork ?: return NetworkType.UNKNOWN
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.UNKNOWN

    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
        else -> NetworkType.UNKNOWN
    }
}
