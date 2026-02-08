/**
 * @file PortScanner.kt
 * @description Scanner de ports TCP via Socket connect avec timeout court.
 */
package com.scaminal.network

import com.scaminal.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortScanner @Inject constructor() {

    companion object {
        val COMMON_PORTS = listOf(
            21, 22, 23, 25, 80, 443, 3306, 3389, 5432, 8080
        )

        private val PORT_NAMES = mapOf(
            21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP",
            80 to "HTTP", 443 to "HTTPS", 3306 to "MySQL",
            3389 to "RDP", 5432 to "PostgreSQL", 8080 to "HTTP-Alt"
        )

        fun getPortName(port: Int): String = PORT_NAMES[port] ?: "Unknown"
    }

    /**
     * Scanne les ports communs d'un hôte.
     *
     * @param ip Adresse IPv4 cible
     * @param ports Liste des ports à scanner (défaut: COMMON_PORTS)
     * @param timeout Timeout par port en millisecondes
     * @return Liste des ports ouverts
     */
    suspend fun scanPorts(
        ip: String,
        ports: List<Int> = COMMON_PORTS,
        timeout: Int = BuildConfig.DEFAULT_PORT_TIMEOUT
    ): List<Int> {
        Timber.d("Port scan starting: %s (%d ports, timeout=%dms)", ip, ports.size, timeout)
        val startTime = System.currentTimeMillis()

        val openPorts = coroutineScope {
            ports.map { port ->
                async(Dispatchers.IO) {
                    if (isPortOpen(ip, port, timeout)) port else null
                }
            }.awaitAll().filterNotNull()
        }

        val elapsed = System.currentTimeMillis() - startTime
        Timber.d("Port scan complete: %s — %d open ports in %dms %s",
            ip, openPorts.size, elapsed,
            openPorts.joinToString { "$it(${getPortName(it)})" }
        )
        return openPorts
    }

    /**
     * Scanne les ports de plusieurs hôtes et émet les résultats au fur et à mesure.
     *
     * @param ips Liste des adresses IP à scanner
     * @param ports Liste des ports à scanner
     * @param timeout Timeout par port en millisecondes
     * @return Flow émettant des paires (ip, openPorts)
     */
    fun scanMultipleHosts(
        ips: List<String>,
        ports: List<Int> = COMMON_PORTS,
        timeout: Int = BuildConfig.DEFAULT_PORT_TIMEOUT
    ): Flow<Pair<String, List<Int>>> = flow {
        Timber.d("Multi-host port scan: %d hosts", ips.size)
        for (ip in ips) {
            val openPorts = scanPorts(ip, ports, timeout)
            emit(ip to openPorts)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Teste si un port TCP est ouvert via Socket connect.
     *
     * @param ip Adresse IPv4 cible
     * @param port Numéro du port
     * @param timeout Timeout en millisecondes
     * @return true si le port est ouvert
     */
    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
