/**
 * @file SshClient.kt
 * @description Client SSH basé sur JSch. Gère connexion, déconnexion et état.
 */
package com.scaminal.ssh

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.scaminal.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** États possibles de la connexion SSH. */
sealed class SshConnectionState {
    data object Disconnected : SshConnectionState()
    data object Connecting : SshConnectionState()
    data class Connected(val host: String) : SshConnectionState()
    data class Error(val message: String) : SshConnectionState()
}

@Singleton
class SshClient @Inject constructor() {

    private val jsch = JSch()
    private var session: Session? = null
    private var channel: ChannelShell? = null

    private val _state = MutableStateFlow<SshConnectionState>(SshConnectionState.Disconnected)
    val state: StateFlow<SshConnectionState> = _state.asStateFlow()

    /**
     * Ouvre une connexion SSH et un shell interactif.
     *
     * @param host Adresse IP ou hostname
     * @param port Port SSH (défaut 22)
     * @param username Nom d'utilisateur
     * @param password Mot de passe en clair
     * @param timeout Timeout de connexion en ms
     */
    suspend fun connect(
        host: String,
        port: Int = 22,
        username: String,
        password: String,
        timeout: Int = BuildConfig.DEFAULT_SSH_TIMEOUT
    ) = withContext(Dispatchers.IO) {
        try {
            _state.value = SshConnectionState.Connecting
            Timber.d("SSH connecting to %s@%s:%d", username, host, port)

            val newSession = jsch.getSession(username, host, port).apply {
                setPassword(password)
                setConfig("StrictHostKeyChecking", "no")
                connect(timeout)
            }
            session = newSession

            val newChannel = (newSession.openChannel("shell") as ChannelShell).apply {
                setPtyType("xterm-256color", 80, 24, 0, 0)
                connect(timeout)
            }
            channel = newChannel

            _state.value = SshConnectionState.Connected(host)
            Timber.d("SSH connected to %s@%s:%d", username, host, port)
        } catch (e: Exception) {
            Timber.e(e, "SSH connection failed to %s:%d", host, port)
            disconnect()
            _state.value = SshConnectionState.Error(e.message ?: "Unknown SSH error")
        }
    }

    /** Ferme le channel et la session SSH. */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            channel?.disconnect()
            session?.disconnect()
            Timber.d("SSH disconnected")
        } catch (e: Exception) {
            Timber.e(e, "Error during SSH disconnect")
        } finally {
            channel = null
            session = null
            _state.value = SshConnectionState.Disconnected
        }
    }

    /** Retourne le channel shell actif, ou null si non connecté. */
    fun getChannel(): ChannelShell? = channel

    /** Vérifie si la session SSH est active. */
    fun isConnected(): Boolean = session?.isConnected == true && channel?.isConnected == true
}
