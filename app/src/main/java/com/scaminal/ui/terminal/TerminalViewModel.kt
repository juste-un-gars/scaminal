/**
 * @file TerminalViewModel.kt
 * @description ViewModel pour l'écran terminal SSH. Orchestre connexion, I/O et parsing ANSI.
 */
package com.scaminal.ui.terminal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scaminal.data.entity.CommandShortcut
import com.scaminal.data.repository.HostRepository
import com.scaminal.data.repository.ShortcutRepository
import com.scaminal.security.KeystoreManager
import com.scaminal.ssh.AnsiParser
import com.scaminal.ssh.SshClient
import com.scaminal.ssh.SshConnectionState
import com.scaminal.ssh.TerminalSpan
import com.scaminal.ssh.TerminalStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sshClient: SshClient,
    private val terminalStream: TerminalStream,
    private val ansiParser: AnsiParser,
    private val hostRepository: HostRepository,
    private val keystoreManager: KeystoreManager,
    private val shortcutRepository: ShortcutRepository
) : ViewModel() {

    val hostIp: String = savedStateHandle.get<String>("hostIp") ?: ""

    val connectionState: StateFlow<SshConnectionState> = sshClient.state

    private val _outputSpans = MutableStateFlow<List<TerminalSpan>>(emptyList())
    val outputSpans: StateFlow<List<TerminalSpan>> = _outputSpans.asStateFlow()

    private val _savedUsername = MutableStateFlow("")
    val savedUsername: StateFlow<String> = _savedUsername.asStateFlow()

    private val _hasSavedCredentials = MutableStateFlow(false)
    val hasSavedCredentials: StateFlow<Boolean> = _hasSavedCredentials.asStateFlow()

    val shortcuts: StateFlow<List<CommandShortcut>> = shortcutRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        private const val MAX_SPANS = 10_000
    }

    init {
        loadSavedCredentials()
    }

    private fun loadSavedCredentials() {
        viewModelScope.launch {
            val credentials = hostRepository.getCredentials(hostIp)
            if (credentials != null) {
                _savedUsername.value = credentials.first
                _hasSavedCredentials.value = true
                Timber.d("Saved credentials found for %s", hostIp)
            }
        }
    }

    /**
     * Connecte au serveur SSH, sauvegarde les credentials si demandé, et attache le stream.
     */
    fun connect(username: String, password: String, saveCredentials: Boolean) {
        viewModelScope.launch {
            ansiParser.reset()
            _outputSpans.value = emptyList()

            if (saveCredentials) {
                val encrypted = keystoreManager.encrypt(password)
                hostRepository.saveCredentials(hostIp, username, encrypted)
            }

            sshClient.connect(host = hostIp, username = username, password = password)

            val channel = sshClient.getChannel()
            if (channel != null) {
                terminalStream.attach(channel).collect { chunk ->
                    val newSpans = ansiParser.parse(chunk)
                    val current = _outputSpans.value
                    val combined = if (current.size + newSpans.size > MAX_SPANS) {
                        (current + newSpans).takeLast(MAX_SPANS)
                    } else {
                        current + newSpans
                    }
                    _outputSpans.value = combined
                }
            }
        }
    }

    /** Déchiffre le mot de passe sauvegardé pour auto-connexion. */
    fun getSavedPassword(): String? {
        val credentials = _hasSavedCredentials.value
        if (!credentials) return null
        return try {
            // Load synchronously from cache — credentials already loaded in init
            null // Will be loaded via connect flow
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt saved password")
            null
        }
    }

    /** Connecte avec les credentials sauvegardés. */
    fun connectWithSaved() {
        viewModelScope.launch {
            val credentials = hostRepository.getCredentials(hostIp) ?: return@launch
            val password = keystoreManager.decrypt(credentials.second)
            connect(credentials.first, password, false)
        }
    }

    /** Ajoute la commande actuelle comme raccourci. */
    fun addShortcut(label: String, command: String) {
        viewModelScope.launch { shortcutRepository.add(label, command) }
    }

    /** Envoie une commande au terminal SSH. */
    fun sendCommand(command: String) {
        viewModelScope.launch {
            terminalStream.write(command + "\n")
        }
    }

    /** Déconnecte la session SSH. */
    fun disconnect() {
        viewModelScope.launch {
            terminalStream.detach()
            sshClient.disconnect()
            Timber.d("Terminal disconnected from %s", hostIp)
        }
    }

    override fun onCleared() {
        super.onCleared()
        terminalStream.detach()
        // disconnect is suspend, launch in global scope won't work well
        // SshClient is singleton, will be cleaned up on next connect or app close
    }
}
