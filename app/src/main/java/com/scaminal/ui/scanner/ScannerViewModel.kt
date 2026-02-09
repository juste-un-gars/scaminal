/**
 * @file ScannerViewModel.kt
 * @description ViewModel pour l'écran de scan réseau. Orchestre les scans IP et ports.
 */
package com.scaminal.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scaminal.data.repository.HostRepository
import com.scaminal.network.NetworkScanner
import com.scaminal.network.PortScanner
import com.scaminal.network.WifiHelper
import com.scaminal.network.model.Host
import com.scaminal.network.model.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val networkScanner: NetworkScanner,
    private val portScanner: PortScanner,
    private val hostRepository: HostRepository,
    private val wifiHelper: WifiHelper
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _hosts = MutableStateFlow<List<Host>>(emptyList())
    val hosts: StateFlow<List<Host>> = _hosts.asStateFlow()

    private val _portScanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val portScanState: StateFlow<ScanState> = _portScanState.asStateFlow()

    private var scanJob: Job? = null
    private var portScanJob: Job? = null

    private val _subnetPrefix = MutableStateFlow("")
    val subnetPrefix: StateFlow<String> = _subnetPrefix.asStateFlow()

    private val _rangeStart = MutableStateFlow("1")
    val rangeStart: StateFlow<String> = _rangeStart.asStateFlow()

    private val _rangeEnd = MutableStateFlow("254")
    val rangeEnd: StateFlow<String> = _rangeEnd.asStateFlow()

    init {
        refreshSubnet()
    }

    /** Détecte le subnet depuis le réseau actif et pré-remplit le champ. */
    fun refreshSubnet() {
        val info = wifiHelper.getNetworkInfo()
        if (info != null) {
            _subnetPrefix.value = info.subnetPrefix
        }
    }

    fun setSubnetPrefix(value: String) {
        _subnetPrefix.value = value
    }

    fun setRangeStart(value: String) {
        _rangeStart.value = value.filter { it.isDigit() }.take(3)
    }

    fun setRangeEnd(value: String) {
        _rangeEnd.value = value.filter { it.isDigit() }.take(3)
    }

    /** Lance un scan IP du sous-réseau avec le range configuré. */
    fun startIpScan() {
        if (_scanState.value is ScanState.InProgress) return

        if (!wifiHelper.isNetworkAvailable()) {
            _scanState.value = ScanState.Error("Pas de connexion réseau")
            return
        }

        val subnet = _subnetPrefix.value.trim()
        if (subnet.isEmpty()) {
            _scanState.value = ScanState.Error("Subnet non configuré")
            return
        }

        val start = _rangeStart.value.toIntOrNull()?.coerceIn(1, 254) ?: 1
        val end = _rangeEnd.value.toIntOrNull()?.coerceIn(1, 254) ?: 254
        if (start > end) {
            _scanState.value = ScanState.Error("Range invalide : début > fin")
            return
        }
        val rangeSize = end - start + 1

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _scanState.value = ScanState.InProgress(0)
            _hosts.value = emptyList()
            val discovered = mutableListOf<Host>()

            try {
                networkScanner.scanSubnet(
                    subnetOverride = subnet,
                    startHost = start,
                    endHost = end
                ).collect { host ->
                    discovered.add(host)
                    _hosts.value = discovered.toList()
                    val progress = (discovered.size * 100) / rangeSize
                    _scanState.value = ScanState.InProgress(progress.coerceAtMost(99))
                }

                _scanState.value = ScanState.Completed(discovered.size)
                hostRepository.saveAll(discovered)
                Timber.d("IP scan finished: %d hosts saved (range %d-%d)", discovered.size, start, end)
            } catch (e: Exception) {
                Timber.e(e, "IP scan failed")
                _scanState.value = ScanState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /** Lance un scan de ports sur tous les hôtes découverts. */
    fun startPortScanAll() {
        val currentHosts = _hosts.value
        if (currentHosts.isEmpty() || _portScanState.value is ScanState.InProgress) return

        portScanJob?.cancel()
        portScanJob = viewModelScope.launch {
            _portScanState.value = ScanState.InProgress(0)

            try {
                val ips = currentHosts.map { it.ipAddress }
                var scanned = 0

                portScanner.scanMultipleHosts(ips).collect { (ip, openPorts) ->
                    scanned++
                    updateHostPorts(ip, openPorts)
                    val progress = (scanned * 100) / ips.size
                    _portScanState.value = ScanState.InProgress(progress.coerceAtMost(99))
                }

                _portScanState.value = ScanState.Completed(currentHosts.size)
                Timber.d("Port scan all complete: %d hosts scanned", scanned)
            } catch (e: Exception) {
                Timber.e(e, "Port scan all failed")
                _portScanState.value = ScanState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /** Lance un scan complet de ports (1-65535) sur un seul hôte. */
    fun startPortScanSingle(ip: String) {
        if (_portScanState.value is ScanState.InProgress) return

        portScanJob?.cancel()
        portScanJob = viewModelScope.launch {
            _portScanState.value = ScanState.InProgress(0)

            try {
                portScanner.scanAllPorts(ip).collect { (progress, openPorts) ->
                    updateHostPorts(ip, openPorts)
                    _portScanState.value = ScanState.InProgress(progress.coerceAtMost(99))
                }
                _portScanState.value = ScanState.Completed(1)
                Timber.d("Port scan single complete: %s → %d ports",
                    ip, _hosts.value.find { it.ipAddress == ip }?.openPorts?.size ?: 0)
            } catch (e: Exception) {
                Timber.e(e, "Port scan single failed: %s", ip)
                _portScanState.value = ScanState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /** Toggle favori d'un hôte. */
    fun toggleFavorite(ip: String) {
        viewModelScope.launch {
            hostRepository.toggleFavorite(ip)
            _hosts.value = _hosts.value.map { host ->
                if (host.ipAddress == ip) host.copy(isFavorite = !host.isFavorite) else host
            }
        }
    }

    /** Met à jour les ports ouverts d'un hôte dans la liste en mémoire. */
    private fun updateHostPorts(ip: String, openPorts: List<Int>) {
        _hosts.value = _hosts.value.map { host ->
            if (host.ipAddress == ip) host.copy(openPorts = openPorts, isPortScanned = true) else host
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        portScanJob?.cancel()
    }
}
