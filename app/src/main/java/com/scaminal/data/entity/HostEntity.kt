/**
 * @file HostEntity.kt
 * @description Entité Room représentant un hôte réseau découvert ou sauvegardé en favori.
 */
package com.scaminal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hosts")
data class HostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ipAddress: String,
    val hostname: String? = null,
    val isFavorite: Boolean = false,
    val sshPort: Int? = null,
    val username: String? = null,
    /** Credentials SSH chiffrés via Android Keystore — jamais en clair */
    val encryptedPassword: ByteArray? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HostEntity) return false
        return id == other.id && ipAddress == other.ipAddress
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + ipAddress.hashCode()
        return result
    }
}
