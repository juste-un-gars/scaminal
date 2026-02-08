/**
 * @file ShortcutsScreen.kt
 * @description Écran de gestion des raccourcis de commandes SSH (ajouter, modifier, supprimer).
 */
package com.scaminal.ui.shortcuts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scaminal.data.entity.CommandShortcut

@Composable
fun ShortcutsScreen(viewModel: ShortcutsViewModel = hiltViewModel()) {
    val shortcuts by viewModel.shortcuts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingShortcut by remember { mutableStateOf<CommandShortcut?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Raccourcis (${shortcuts.size})",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (shortcuts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aucun raccourci\nAppuyez sur + pour en ajouter",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shortcuts, key = { it.id }) { shortcut ->
                        ShortcutItem(
                            shortcut = shortcut,
                            onEdit = { editingShortcut = shortcut },
                            onDelete = { viewModel.delete(shortcut) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter")
        }
    }

    if (showAddDialog) {
        ShortcutDialog(
            title = "Nouveau raccourci",
            onConfirm = { label, command ->
                viewModel.add(label, command)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingShortcut?.let { shortcut ->
        ShortcutDialog(
            title = "Modifier le raccourci",
            initialLabel = shortcut.label,
            initialCommand = shortcut.command,
            onConfirm = { label, command ->
                viewModel.update(shortcut.copy(label = label, command = command))
                editingShortcut = null
            },
            onDismiss = { editingShortcut = null }
        )
    }
}

@Composable
private fun ShortcutItem(
    shortcut: CommandShortcut,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shortcut.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = shortcut.command,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Modifier",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ShortcutDialog(
    title: String,
    initialLabel: String = "",
    initialCommand: String = "",
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(initialLabel) }
    var command by remember { mutableStateOf(initialCommand) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nom") },
                    placeholder = { Text("ex: update") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Commande") },
                    placeholder = { Text("ex: sudo apt update") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label.trim(), command.trim()) },
                enabled = label.isNotBlank() && command.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
