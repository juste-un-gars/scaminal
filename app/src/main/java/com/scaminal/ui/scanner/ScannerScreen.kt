/**
 * @file ScannerScreen.kt
 * @description Écran principal de scan réseau : découverte IP et scan de ports.
 */
package com.scaminal.ui.scanner

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scaminal.network.PortScanner
import com.scaminal.network.model.Host
import com.scaminal.network.model.ScanState

@Composable
fun ScannerScreen(viewModel: ScannerViewModel = hiltViewModel()) {
    val scanState by viewModel.scanState.collectAsState()
    val portScanState by viewModel.portScanState.collectAsState()
    val hosts by viewModel.hosts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScanControls(
            scanState = scanState,
            portScanState = portScanState,
            hostsCount = hosts.size,
            onIpScan = viewModel::startIpScan,
            onPortScanAll = viewModel::startPortScanAll
        )

        Spacer(modifier = Modifier.height(12.dp))

        ScanProgress(scanState = scanState, label = "Scan IP")
        ScanProgress(scanState = portScanState, label = "Scan Ports")

        Spacer(modifier = Modifier.height(12.dp))

        when {
            scanState is ScanState.Idle && hosts.isEmpty() -> EmptyState()
            scanState is ScanState.Error -> ErrorState((scanState as ScanState.Error).message)
            else -> HostList(
                hosts = hosts,
                onLongPress = viewModel::startPortScanSingle,
                onToggleFavorite = viewModel::toggleFavorite
            )
        }
    }
}

@Composable
private fun ScanControls(
    scanState: ScanState,
    portScanState: ScanState,
    hostsCount: Int,
    onIpScan: () -> Unit,
    onPortScanAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onIpScan,
            enabled = scanState !is ScanState.InProgress,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Scan IP")
        }

        OutlinedButton(
            onClick = onPortScanAll,
            enabled = hostsCount > 0 && portScanState !is ScanState.InProgress,
            modifier = Modifier.weight(1f)
        ) {
            Text("Scan Ports ($hostsCount)")
        }
    }
}

@Composable
private fun ScanProgress(scanState: ScanState, label: String) {
    if (scanState is ScanState.InProgress) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "$label : ${scanState.progress}%",
                style = MaterialTheme.typography.bodySmall
            )
            LinearProgressIndicator(
                progress = { scanState.progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Appuyez sur Scan IP pour scanner le réseau",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun HostList(
    hosts: List<Host>,
    onLongPress: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(hosts, key = { it.ipAddress }) { host ->
            HostItem(
                host = host,
                onLongPress = { onLongPress(host.ipAddress) },
                onToggleFavorite = { onToggleFavorite(host.ipAddress) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HostItem(
    host: Host,
    onLongPress: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongPress, onClick = {})
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.ipAddress,
                    style = MaterialTheme.typography.titleMedium
                )
                host.hostname?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (host.openPorts.isNotEmpty()) {
                    Text(
                        text = host.openPorts.joinToString(", ") {
                            "$it (${PortScanner.getPortName(it)})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (host.responseTime > 0) {
                    Text(
                        text = "${host.responseTime}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Favori",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
