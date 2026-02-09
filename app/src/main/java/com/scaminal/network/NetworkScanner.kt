/**
 * @file NetworkScanner.kt
 * @description Scanner IP de sous-réseau /24 via ping parallèle (InetAddress.isReachable).
 */
package com.scaminal.network

import com.scaminal.BuildConfig
import com.scaminal.network.model.Host
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkScanner @Inject constructor(
    private val wifiHelper: WifiHelper
) {

    /**
     * Scanne le sous-réseau /24 pour découvrir les hôtes actifs.
     * Émet chaque hôte découvert au fur et à mesure.
     *
     * @param timeout Timeout par hôte en millisecondes
     * @return Flow émettant chaque [Host] joignable
     */
    fun scanSubnet(
        subnetOverride: String? = null,
        startHost: Int = 1,
        endHost: Int = 254,
        timeout: Int = BuildConfig.DEFAULT_SCAN_TIMEOUT
    ): Flow<Host> = flow {
        val subnet = if (subnetOverride != null) {
            subnetOverride
        } else {
            val networkInfo = wifiHelper.getNetworkInfo()
            if (networkInfo == null) {
                Timber.w("Cannot scan: no network info available")
                return@flow
            }
            networkInfo.subnetPrefix
        }

        val range = startHost.coerceIn(1, 254)..endHost.coerceIn(1, 254)
        Timber.d("Starting subnet scan: %s.%d-%d (timeout=%dms)", subnet, range.first, range.last, timeout)
        val startTime = System.currentTimeMillis()

        val results = kotlinx.coroutines.coroutineScope {
            range.map { i ->
                async {
                    pingHost("$subnet.$i", timeout)
                }
            }.awaitAll()
        }

        var found = 0
        for (host in results) {
            if (host.isReachable) {
                found++
                emit(host)
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        Timber.d("Subnet scan complete: %d hosts found in %dms", found, elapsed)
    }.flowOn(Dispatchers.IO)

    /**
     * Ping un hôte unique via InetAddress.isReachable.
     *
     * @param ip Adresse IPv4 cible
     * @param timeout Timeout en millisecondes
     * @return [Host] avec isReachable et responseTime
     */
    private fun pingHost(ip: String, timeout: Int): Host {
        return try {
            val address = InetAddress.getByName(ip)
            val start = System.currentTimeMillis()
            val reachable = address.isReachable(timeout)
            val elapsed = System.currentTimeMillis() - start

            val hostname = if (reachable) {
                try {
                    val resolved = address.canonicalHostName
                    if (resolved != ip) resolved else null
                } catch (_: Exception) {
                    null
                }
            } else null

            Host(
                ipAddress = ip,
                hostname = hostname,
                isReachable = reachable,
                responseTime = if (reachable) elapsed else -1L
            )
        } catch (e: Exception) {
            Timber.w("Ping failed for %s: %s", ip, e.message)
            Host(ipAddress = ip, isReachable = false)
        }
    }
}
