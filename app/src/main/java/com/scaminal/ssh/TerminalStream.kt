/**
 * @file TerminalStream.kt
 * @description Flux bidirectionnel pour lire/écrire sur un channel SSH shell.
 */
package com.scaminal.ssh

import com.jcraft.jsch.ChannelShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class TerminalStream @Inject constructor() {

    companion object {
        private const val BUFFER_SIZE = 4096
        private const val READ_DELAY_MS = 50L
    }

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    /**
     * Attache un channel shell et émet les données lues en continu.
     *
     * @param channel Le channel shell SSH connecté
     * @return Flow émettant les chunks de texte reçus du serveur
     */
    fun attach(channel: ChannelShell): Flow<String> = flow {
        inputStream = channel.inputStream
        outputStream = channel.outputStream
        Timber.d("TerminalStream attached")

        val buffer = ByteArray(BUFFER_SIZE)
        val stream = inputStream ?: return@flow

        while (coroutineContext.isActive && channel.isConnected) {
            val available = stream.available()
            if (available > 0) {
                val bytesRead = stream.read(buffer, 0, minOf(available, BUFFER_SIZE))
                if (bytesRead > 0) {
                    emit(String(buffer, 0, bytesRead, Charsets.UTF_8))
                }
            } else {
                kotlinx.coroutines.delay(READ_DELAY_MS)
            }
        }
        Timber.d("TerminalStream read loop ended")
    }.flowOn(Dispatchers.IO)

    /**
     * Écrit du texte sur le flux de sortie du terminal SSH.
     *
     * @param text Le texte à envoyer (commande + \n)
     */
    suspend fun write(text: String) = withContext(Dispatchers.IO) {
        try {
            outputStream?.let {
                it.write(text.toByteArray(Charsets.UTF_8))
                it.flush()
            }
        } catch (e: Exception) {
            Timber.e(e, "TerminalStream write error")
        }
    }

    /** Ferme les streams et libère les ressources. */
    fun detach() {
        try {
            inputStream?.close()
            outputStream?.close()
        } catch (e: Exception) {
            Timber.e(e, "TerminalStream detach error")
        } finally {
            inputStream = null
            outputStream = null
            Timber.d("TerminalStream detached")
        }
    }
}
