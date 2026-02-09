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
import kotlinx.coroutines.sync.Semaphore
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
            7 to "Echo",
            20 to "FTP-Data", 21 to "FTP", 22 to "SSH", 23 to "Telnet",
            25 to "SMTP", 26 to "RSFTP",
            53 to "DNS",
            67 to "DHCP-S", 68 to "DHCP-C", 69 to "TFTP",
            80 to "HTTP", 81 to "HTTP-Alt",
            88 to "Kerberos",
            110 to "POP3", 111 to "RPCbind", 113 to "Ident",
            119 to "NNTP", 123 to "NTP", 135 to "MSRPC",
            137 to "NetBIOS-NS", 138 to "NetBIOS-DGM", 139 to "NetBIOS-SSN",
            143 to "IMAP",
            161 to "SNMP", 162 to "SNMP-Trap",
            179 to "BGP",
            194 to "IRC",
            389 to "LDAP",
            443 to "HTTPS", 445 to "SMB",
            464 to "Kpasswd", 465 to "SMTPS",
            500 to "IKE",
            514 to "Syslog", 515 to "LPD",
            520 to "RIP", 523 to "IBM-DB2",
            530 to "RPC",
            543 to "Klogin", 544 to "Kshell",
            548 to "AFP",
            554 to "RTSP",
            587 to "Submission",
            593 to "DCOM",
            623 to "IPMI",
            631 to "IPP",
            636 to "LDAPS",
            873 to "Rsync",
            902 to "VMware",
            993 to "IMAPS", 995 to "POP3S",
            1080 to "SOCKS",
            1433 to "MSSQL", 1434 to "MSSQL-UDP",
            1521 to "Oracle",
            1701 to "L2TP", 1723 to "PPTP",
            1883 to "MQTT",
            2049 to "NFS",
            2082 to "cPanel", 2083 to "cPanel-SSL",
            2181 to "ZooKeeper",
            2222 to "SSH-Alt",
            2375 to "Docker", 2376 to "Docker-TLS",
            3000 to "Dev-Server", 3001 to "Dev-Server",
            3268 to "LDAP-GC", 3269 to "LDAPS-GC",
            3306 to "MySQL",
            3389 to "RDP",
            4443 to "HTTPS-Alt",
            4567 to "Sinatra",
            4711 to "McAfee",
            4848 to "GlassFish",
            5000 to "UPnP", 5001 to "Synology",
            5004 to "RTP", 5005 to "RTP",
            5060 to "SIP", 5061 to "SIPS",
            5222 to "XMPP",
            5432 to "PostgreSQL",
            5555 to "ADB",
            5601 to "Kibana",
            5672 to "AMQP", 5673 to "AMQP",
            5900 to "VNC", 5901 to "VNC",
            5984 to "CouchDB",
            6379 to "Redis",
            6443 to "Kubernetes",
            6660 to "IRC", 6661 to "IRC", 6662 to "IRC",
            6663 to "IRC", 6664 to "IRC", 6665 to "IRC",
            6666 to "IRC", 6667 to "IRC", 6668 to "IRC",
            6669 to "IRC",
            7070 to "RealServer",
            7474 to "Neo4j",
            8000 to "HTTP-Alt", 8008 to "HTTP-Alt",
            8080 to "HTTP-Proxy", 8081 to "HTTP-Alt",
            8443 to "HTTPS-Alt", 8444 to "HTTPS-Alt",
            8888 to "HTTP-Alt",
            8883 to "MQTT-TLS",
            9000 to "SonarQube", 9001 to "Tor",
            9090 to "Prometheus", 9091 to "Prometheus",
            9092 to "Kafka",
            9200 to "Elasticsearch", 9300 to "Elasticsearch",
            9418 to "Git",
            9999 to "Urchin",
            10000 to "Webmin",
            11211 to "Memcached",
            15672 to "RabbitMQ",
            27017 to "MongoDB", 27018 to "MongoDB", 27019 to "MongoDB",
            28017 to "MongoDB-Web",
            50000 to "SAP",
            50070 to "Hadoop"
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
     * Scanne tous les ports (1-65535) d'un hôte avec contrôle de concurrence.
     * Émet la progression et les ports ouverts trouvés au fur et à mesure.
     *
     * @param ip Adresse IPv4 cible
     * @param concurrency Nombre max de connexions simultanées
     * @param timeout Timeout par port en millisecondes
     * @return Flow émettant des paires (progression%, portsOuvertsTrouvés)
     */
    fun scanAllPorts(
        ip: String,
        concurrency: Int = 500,
        timeout: Int = BuildConfig.DEFAULT_PORT_TIMEOUT
    ): Flow<Pair<Int, List<Int>>> = flow {
        val totalPorts = 65535
        val batchSize = 1000
        val semaphore = Semaphore(concurrency)
        val openPorts = mutableListOf<Int>()

        Timber.d("Full port scan starting: %s (1-%d, concurrency=%d, timeout=%dms)",
            ip, totalPorts, concurrency, timeout)
        val startTime = System.currentTimeMillis()

        for (batchStart in 1..totalPorts step batchSize) {
            val batchEnd = (batchStart + batchSize - 1).coerceAtMost(totalPorts)
            val batchResults = coroutineScope {
                (batchStart..batchEnd).map { port ->
                    async(Dispatchers.IO) {
                        semaphore.acquire()
                        try {
                            if (isPortOpen(ip, port, timeout)) port else null
                        } finally {
                            semaphore.release()
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            openPorts.addAll(batchResults)
            val progress = (batchEnd * 100) / totalPorts
            emit(progress to openPorts.toList())
        }

        val elapsed = System.currentTimeMillis() - startTime
        Timber.d("Full port scan complete: %s — %d open ports in %dms %s",
            ip, openPorts.size, elapsed,
            openPorts.joinToString { "$it(${getPortName(it)})" }
        )
    }.flowOn(Dispatchers.IO)

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
