/**
 * @file CommandShortcut.kt
 * @description Entité Room pour les raccourcis de commandes SSH globaux.
 */
package com.scaminal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_shortcuts")
data class CommandShortcut(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val command: String,
    val sortOrder: Int = 0
)
