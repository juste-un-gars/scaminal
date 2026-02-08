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

    /** Lance un scan IP du sous-réseau /24. */
    fun startIpScan() {
        if (_scanState.value is ScanState.InProgress) return

        if (!wifiHelper.isNetworkAvailable()) {
            _scanState.value = ScanState.Error("Pas de connexion réseau")
            return
        }

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _scanState.value = ScanState.InProgress(0)
            _hosts.value = emptyList()
            val discovered = mutableListOf<Host>()

            try {
                networkScanner.scanSubnet().collect { host ->
                    discovered.add(host)
                    _hosts.value = discovered.toList()
                    val progress = (discovered.size * 100) / 254
                    _scanState.value = ScanState.InProgress(progress.coerceAtMost(99))
                }

                _scanState.value = ScanState.Completed(discovered.size)
                hostRepository.saveAll(discovered)
                Timber.d("IP scan finished: %d hosts saved", discovered.size)
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

    /** Lance un scan de ports sur un seul hôte (appui long). */
    fun startPortScanSingle(ip: String) {
        if (_portScanState.value is ScanState.InProgress) return

        portScanJob?.cancel()
        portScanJob = viewModelScope.launch {
            _portScanState.value = ScanState.InProgress(0)

            try {
                val openPorts = portScanner.scanPorts(ip)
                updateHostPorts(ip, openPorts)
                _portScanState.value = ScanState.Completed(1)
                Timber.d("Port scan single complete: %s → %d ports", ip, openPorts.size)
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
        }
    }

    /** Met à jour les ports ouverts d'un hôte dans la liste en mémoire. */
    private fun updateHostPorts(ip: String, openPorts: List<Int>) {
        _hosts.value = _hosts.value.map { host ->
            if (host.ipAddress == ip) host.copy(openPorts = openPorts) else host
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        portScanJob?.cancel()
    }
}
