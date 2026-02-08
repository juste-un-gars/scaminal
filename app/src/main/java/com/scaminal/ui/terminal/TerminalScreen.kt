/**
 * @file TerminalScreen.kt
 * @description Écran terminal SSH avec dialog de connexion, sortie colorée et barre d'input.
 */
package com.scaminal.ui.terminal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scaminal.data.entity.CommandShortcut
import com.scaminal.ssh.AnsiParser
import com.scaminal.ssh.SshConnectionState
import com.scaminal.ssh.TerminalSpan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val outputSpans by viewModel.outputSpans.collectAsState()
    val savedUsername by viewModel.savedUsername.collectAsState()
    val savedPassword by viewModel.savedPassword.collectAsState()
    val hasSavedCredentials by viewModel.hasSavedCredentials.collectAsState()
    val shortcuts by viewModel.shortcuts.collectAsState()

    var showLoginDialog by remember { mutableStateOf(true) }
    var commandText by remember { mutableStateOf("") }

    // Bouton retour système = même action que la flèche
    BackHandler {
        viewModel.disconnect()
        onNavigateBack()
    }

    // Masquer le dialog une fois connecté
    LaunchedEffect(connectionState) {
        if (connectionState is SshConnectionState.Connected) {
            showLoginDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.hostIp) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1E1E1E))
                .imePadding()
        ) {
            // Terminal output
            TerminalOutput(
                spans = outputSpans,
                modifier = Modifier.weight(1f)
            )

            // Status / error bar
            ConnectionStatus(connectionState)

            // Input bar + raccourcis (visible seulement si connecté)
            if (connectionState is SshConnectionState.Connected) {
                TerminalInput(
                    command = commandText,
                    onCommandChange = { commandText = it },
                    onSend = {
                        if (commandText.isNotBlank()) {
                            viewModel.sendCommand(commandText)
                            commandText = ""
                        }
                    }
                )
                ShortcutBar(
                    shortcuts = shortcuts,
                    currentCommand = commandText,
                    onShortcutTap = { command -> commandText = command },
                    onAddShortcut = { label, command -> viewModel.addShortcut(label, command) }
                )
            }
        }
    }

    // Login dialog
    if (showLoginDialog && connectionState !is SshConnectionState.Connected) {
        LoginDialog(
            initialUsername = savedUsername,
            initialPassword = savedPassword,
            hasSavedCredentials = hasSavedCredentials,
            isConnecting = connectionState is SshConnectionState.Connecting,
            error = (connectionState as? SshConnectionState.Error)?.message,
            onConnect = { username, password, save ->
                viewModel.connect(username, password, save)
            },
            onConnectWithSaved = viewModel::connectWithSaved,
            onDismiss = {
                showLoginDialog = false
                onNavigateBack()
            }
        )
    }
}

@Composable
private fun TerminalOutput(spans: List<TerminalSpan>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val ansiParser = remember { AnsiParser() }

    // Auto-scroll vers le bas quand nouveau contenu
    LaunchedEffect(spans.size) {
        if (spans.isNotEmpty()) {
            listState.animateScrollToItem(0.coerceAtLeast(spans.size - 1))
        }
    }

    // Grouper les spans en lignes pour le LazyColumn
    val lines = remember(spans) {
        val result = mutableListOf<List<TerminalSpan>>()
        var currentLine = mutableListOf<TerminalSpan>()

        for (span in spans) {
            val parts = span.text.split('\n')
            for ((i, part) in parts.withIndex()) {
                if (part.isNotEmpty()) {
                    currentLine.add(span.copy(text = part))
                }
                if (i < parts.size - 1) {
                    result.add(currentLine.toList())
                    currentLine = mutableListOf()
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toList())
        }
        result
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        items(lines.size) { index ->
            Text(
                text = ansiParser.toAnnotatedString(lines[index]),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFFCCCCCC),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ConnectionStatus(state: SshConnectionState) {
    when (state) {
        is SshConnectionState.Connecting -> {
            Text(
                text = "Connexion en cours...",
                color = Color(0xFFFFCC00),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        is SshConnectionState.Error -> {
            Text(
                text = "Erreur : ${state.message}",
                color = Color(0xFFFF5555),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        else -> {}
    }
}

@Composable
private fun TerminalInput(
    command: String,
    onCommandChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D2D))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = command,
            onValueChange = onCommandChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Commande...", color = Color(0xFF888888)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = Color.White
            )
        )

        IconButton(onClick = onSend) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Envoyer",
                tint = Color(0xFF00CC00)
            )
        }
    }
}

@Composable
private fun ShortcutBar(
    shortcuts: List<CommandShortcut>,
    currentCommand: String,
    onShortcutTap: (String) -> Unit,
    onAddShortcut: (String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF252525))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Ajouter raccourci",
                tint = Color(0xFF888888),
                modifier = Modifier.size(20.dp)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(shortcuts, key = { it.id }) { shortcut ->
                AssistChip(
                    onClick = { onShortcutTap(shortcut.command) },
                    label = {
                        Text(
                            text = shortcut.label,
                            fontSize = 12.sp
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF3A3A3A),
                        labelColor = Color(0xFFCCCCCC)
                    )
                )
            }
        }
    }

    if (showAddDialog) {
        AddShortcutDialog(
            initialCommand = currentCommand,
            onConfirm = { label, command ->
                onAddShortcut(label, command)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun AddShortcutDialog(
    initialCommand: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var command by remember { mutableStateOf(initialCommand) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau raccourci") },
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label.trim(), command.trim()) },
                enabled = label.isNotBlank() && command.isNotBlank()
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun LoginDialog(
    initialUsername: String,
    initialPassword: String,
    hasSavedCredentials: Boolean,
    isConnecting: Boolean,
    error: String?,
    onConnect: (String, String, Boolean) -> Unit,
    onConnectWithSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var saveCredentials by remember { mutableStateOf(false) }

    // Mettre à jour les champs quand les credentials chargent
    LaunchedEffect(initialUsername) {
        if (initialUsername.isNotEmpty()) username = initialUsername
    }
    LaunchedEffect(initialPassword) {
        if (initialPassword.isNotEmpty()) password = initialPassword
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connexion SSH") },
        text = {
            Column {
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Utilisateur") },
                    singleLine = true,
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
                    singleLine = true,
                    enabled = !isConnecting,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Masquer" else "Afficher"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = saveCredentials,
                        onCheckedChange = { saveCredentials = it },
                        enabled = !isConnecting
                    )
                    Text("Sauvegarder les identifiants")
                }

                if (hasSavedCredentials) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onConnectWithSaved,
                        enabled = !isConnecting
                    ) {
                        Text("Connexion rapide (identifiants sauvegardés)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConnect(username, password, saveCredentials) },
                enabled = !isConnecting && username.isNotBlank() && password.isNotBlank()
            ) {
                Text(if (isConnecting) "Connexion..." else "Connecter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
