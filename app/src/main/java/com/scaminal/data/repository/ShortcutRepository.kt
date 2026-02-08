/**
 * @file ShortcutRepository.kt
 * @description Repository pour les raccourcis de commandes SSH.
 *              Pré-remplit les commandes par défaut au premier lancement.
 */
package com.scaminal.data.repository

import com.scaminal.data.dao.CommandShortcutDao
import com.scaminal.data.entity.CommandShortcut
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutRepository @Inject constructor(
    private val dao: CommandShortcutDao
) {

    fun getAll(): Flow<List<CommandShortcut>> = dao.getAll()

    suspend fun add(label: String, command: String) {
        dao.insert(CommandShortcut(label = label, command = command))
        Timber.d("Shortcut added: %s", label)
    }

    suspend fun update(shortcut: CommandShortcut) {
        dao.update(shortcut)
        Timber.d("Shortcut updated: %s", shortcut.label)
    }

    suspend fun delete(shortcut: CommandShortcut) {
        dao.delete(shortcut)
        Timber.d("Shortcut deleted: %s", shortcut.label)
    }

    /** Insère les commandes par défaut si la table est vide. */
    suspend fun ensureDefaults() {
        if (dao.count() > 0) return

        val defaults = listOf(
            CommandShortcut(label = "update", command = "sudo apt update && sudo apt upgrade -y", sortOrder = 0),
            CommandShortcut(label = "reboot", command = "sudo reboot", sortOrder = 1),
            CommandShortcut(label = "shutdown", command = "sudo shutdown now", sortOrder = 2),
            CommandShortcut(label = "disk", command = "df -h", sortOrder = 3),
            CommandShortcut(label = "mem", command = "free -h", sortOrder = 4),
            CommandShortcut(label = "top", command = "htop", sortOrder = 5),
            CommandShortcut(label = "ports", command = "ss -tlnp", sortOrder = 6),
            CommandShortcut(label = "ip", command = "ip a", sortOrder = 7),
        )
        dao.insertAll(defaults)
        Timber.d("Default shortcuts inserted: %d", defaults.size)
    }
}
