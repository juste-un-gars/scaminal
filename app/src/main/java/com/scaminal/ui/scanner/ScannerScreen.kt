/**
 * @file ScannerScreen.kt
 * @description Écran principal de scan réseau : découverte IP et scan de ports.
 */
package com.scaminal.ui.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scaminal.network.PortScanner
import com.scaminal.network.model.Host
import com.scaminal.network.model.ScanState

@Composable
fun ScannerScreen(
    onNavigateToTerminal: (String) -> Unit = {},
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val scanState by viewModel.scanState.collectAsState()
    val portScanState by viewModel.portScanState.collectAsState()
    val hosts by viewModel.hosts.collectAsState()
    val subnetPrefix by viewModel.subnetPrefix.collectAsState()
    val rangeStart by viewModel.rangeStart.collectAsState()
    val rangeEnd by viewModel.rangeEnd.collectAsState()

    var selectedHost by remember { mutableStateOf<Host?>(null) }

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

        Spacer(modifier = Modifier.height(8.dp))

        IpRangeSelector(
            subnetPrefix = subnetPrefix,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            onSubnetChange = viewModel::setSubnetPrefix,
            onStartChange = viewModel::setRangeStart,
            onEndChange = viewModel::setRangeEnd,
            enabled = scanState !is ScanState.InProgress
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
                onToggleFavorite = viewModel::toggleFavorite,
                onTap = { ip -> selectedHost = hosts.find { it.ipAddress == ip } }
            )
        }
    }

    selectedHost?.let { host ->
        HostDetailDialog(
            host = host,
            onDismiss = { selectedHost = null },
            onScanPorts = {
                viewModel.startPortScanSingle(host.ipAddress)
                selectedHost = null
            },
            onConnectSsh = {
                selectedHost = null
                onNavigateToTerminal(host.ipAddress)
            }
        )
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
private fun IpRangeSelector(
    subnetPrefix: String,
    rangeStart: String,
    rangeEnd: String,
    onSubnetChange: (String) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = subnetPrefix,
            onValueChange = onSubnetChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Subnet (ex: 192.168.1)") },
            singleLine = true,
            enabled = enabled
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Range :",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = rangeStart,
                onValueChange = onStartChange,
                modifier = Modifier.weight(1f),
                label = { Text("Début") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = enabled
            )
            Text("—", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = rangeEnd,
                onValueChange = onEndChange,
                modifier = Modifier.weight(1f),
                label = { Text("Fin") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = enabled
            )
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
private fun HostDetailDialog(
    host: Host,
    onDismiss: () -> Unit,
    onScanPorts: () -> Unit,
    onConnectSsh: () -> Unit
) {
    val context = LocalContext.current
    val hasSsh = host.openPorts.contains(22)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(host.ipAddress) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                host.hostname?.let {
                    Text("Hostname : $it", style = MaterialTheme.typography.bodyMedium)
                }
                if (host.responseTime > 0) {
                    Text("Ping : ${host.responseTime}ms", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()

                if (!host.isPortScanned) {
                    Text(
                        "Ports non scannés",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (host.openPorts.isEmpty()) {
                    Text(
                        "Aucun port ouvert",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Ports ouverts :", style = MaterialTheme.typography.bodyMedium)
                    host.openPorts.forEach { port ->
                        val portName = PortScanner.getPortName(port)
                        val action = getPortAction(port)
                        Text(
                            text = "$port ($portName) — ${action.label}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.Underline
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                handlePortAction(context, host.ipAddress, port, action, onConnectSsh)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (hasSsh) {
                Button(onClick = onConnectSsh) {
                    Text("Connexion SSH")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Fermer")
                }
                if (!host.isPortScanned) {
                    OutlinedButton(onClick = onScanPorts) {
                        Text("Scanner ports")
                    }
                }
            }
        }
    )
}

private enum class PortActionType { BROWSER_HTTP, BROWSER_HTTPS, SSH, COPY }

private data class PortAction(val type: PortActionType, val label: String)

private fun getPortAction(port: Int): PortAction = when (port) {
    80, 81, 3000, 3001, 4567, 4848, 5601, 7474,
    8000, 8008, 8080, 8081, 8888, 9000, 9090, 10000
        -> PortAction(PortActionType.BROWSER_HTTP, "Ouvrir HTTP")
    443, 4443, 8443, 8444
        -> PortAction(PortActionType.BROWSER_HTTPS, "Ouvrir HTTPS")
    22, 2222 -> PortAction(PortActionType.SSH, "Connexion SSH")
    else -> PortAction(PortActionType.COPY, "Copier")
}

private fun handlePortAction(
    context: Context,
    ip: String,
    port: Int,
    action: PortAction,
    onConnectSsh: () -> Unit
) {
    when (action.type) {
        PortActionType.BROWSER_HTTP -> {
            val url = if (port == 80) "http://$ip" else "http://$ip:$port"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        PortActionType.BROWSER_HTTPS -> {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://$ip")))
        }
        PortActionType.SSH -> onConnectSsh()
        PortActionType.COPY -> {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("address", "$ip:$port"))
            Toast.makeText(context, "$ip:$port copié", Toast.LENGTH_SHORT).show()
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
    onToggleFavorite: (String) -> Unit,
    onTap: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(hosts, key = { it.ipAddress }) { host ->
            HostItem(
                host = host,
                onLongPress = { onLongPress(host.ipAddress) },
                onToggleFavorite = { onToggleFavorite(host.ipAddress) },
                onTap = { onTap(host.ipAddress) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HostItem(
    host: Host,
    onLongPress: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongPress, onClick = onTap)
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
                when {
                    host.openPorts.isNotEmpty() -> Text(
                        text = host.openPorts.joinToString(", ") {
                            "$it (${PortScanner.getPortName(it)})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    host.isPortScanned -> Text(
                        text = "Aucun port ouvert",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    imageVector = if (host.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favori",
                    tint = if (host.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
