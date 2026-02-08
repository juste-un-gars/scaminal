/**
 * @file WifiHelper.kt
 * @description Utilitaire réseau : détection Wi-Fi, IP locale, calcul sous-réseau.
 */
package com.scaminal.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkInfo(
    val localIp: String,
    val subnetPrefix: String,
    val prefixLength: Int
)

@Singleton
class WifiHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Vérifie si l'appareil est connecté à un réseau Wi-Fi.
     *
     * @return true si connecté en Wi-Fi
     */
    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Vérifie si l'appareil a une connectivité réseau (Wi-Fi ou autre).
     *
     * @return true si connecté
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Récupère les informations réseau : IP locale, préfixe sous-réseau, longueur du masque.
     *
     * @return [NetworkInfo] ou null si non connecté
     */
    fun getNetworkInfo(): NetworkInfo? {
        try {
            val linkProperties = connectivityManager.activeNetwork?.let {
                connectivityManager.getLinkProperties(it)
            } ?: return null

            val ipv4Address = linkProperties.linkAddresses.firstOrNull { linkAddress ->
                linkAddress.address is Inet4Address && !linkAddress.address.isLoopbackAddress
            } ?: return null

            val ip = ipv4Address.address.hostAddress ?: return null
            val prefixLength = ipv4Address.prefixLength
            val subnetPrefix = calculateSubnetPrefix(ip, prefixLength)

            Timber.d("Network info: ip=%s, subnet=%s/%d", ip, subnetPrefix, prefixLength)
            return NetworkInfo(
                localIp = ip,
                subnetPrefix = subnetPrefix,
                prefixLength = prefixLength
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get network info")
            return null
        }
    }

    /**
     * Récupère l'IP locale via ConnectivityManager, avec fallback sur NetworkInterface.
     *
     * @return adresse IP locale ou null
     */
    fun getLocalIpAddress(): String? {
        return getNetworkInfo()?.localIp ?: getIpFromNetworkInterface()
    }

    private fun getIpFromNetworkInterface(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                for (address in networkInterface.inetAddresses) {
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        Timber.d("IP from NetworkInterface: %s", address.hostAddress)
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get IP from NetworkInterface")
        }
        return null
    }

    /**
     * Calcule le préfixe du sous-réseau à partir de l'IP et de la longueur du masque.
     *
     * @param ip adresse IPv4 (ex: "192.168.1.42")
     * @param prefixLength longueur du masque CIDR (ex: 24)
     * @return préfixe sous-réseau (ex: "192.168.1")
     */
    private fun calculateSubnetPrefix(ip: String, prefixLength: Int): String {
        val parts = ip.split(".").map { it.toInt() }
        val ipInt = (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
        val mask = if (prefixLength == 0) 0 else (-1 shl (32 - prefixLength))
        val network = ipInt and mask

        return "${(network shr 24) and 0xFF}.${(network shr 16) and 0xFF}.${(network shr 8) and 0xFF}"
    }
}
