/**
 * @file HostRepository.kt
 * @description Couche d'abstraction entre le DAO Room et les ViewModels.
 *              Gère la conversion Host ↔ HostEntity et les opérations CRUD.
 */
package com.scaminal.data.repository

import com.scaminal.data.dao.HostDao
import com.scaminal.data.entity.HostEntity
import com.scaminal.network.model.Host
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HostRepository @Inject constructor(
    private val hostDao: HostDao
) {

    /** Flow de tous les hôtes convertis en modèle réseau. */
    fun getAllHosts(): Flow<List<Host>> =
        hostDao.getAllHosts().map { entities -> entities.map { it.toHost() } }

    /** Flow des favoris uniquement. */
    fun getFavorites(): Flow<List<Host>> =
        hostDao.getFavorites().map { entities -> entities.map { it.toHost() } }

    /**
     * Sauvegarde ou met à jour un hôte découvert.
     * Si l'IP existe déjà, on met à jour lastSeen et les ports.
     */
    suspend fun saveHost(host: Host) {
        val existing = hostDao.getByIp(host.ipAddress)
        if (existing != null) {
            hostDao.update(existing.copy(
                hostname = host.hostname ?: existing.hostname,
                lastSeen = System.currentTimeMillis()
            ))
            Timber.d("Host updated: %s", host.ipAddress)
        } else {
            hostDao.insert(host.toEntity())
            Timber.d("Host inserted: %s", host.ipAddress)
        }
    }

    /** Sauvegarde un lot d'hôtes découverts. */
    suspend fun saveAll(hosts: List<Host>) {
        hosts.forEach { saveHost(it) }
    }

    /** Toggle le statut favori d'un hôte par son IP. */
    suspend fun toggleFavorite(ipAddress: String) {
        val entity = hostDao.getByIp(ipAddress) ?: return
        hostDao.update(entity.copy(isFavorite = !entity.isFavorite))
        Timber.d("Favorite toggled: %s → %s", ipAddress, !entity.isFavorite)
    }

    /** Supprime un hôte par son IP. */
    suspend fun deleteByIp(ipAddress: String) {
        val entity = hostDao.getByIp(ipAddress) ?: return
        hostDao.delete(entity)
        Timber.d("Host deleted: %s", ipAddress)
    }

    /** Supprime tous les hôtes non favoris. */
    suspend fun clearNonFavorites() {
        hostDao.deleteNonFavorites()
        Timber.d("Non-favorite hosts cleared")
    }
}

/** Convertit un [HostEntity] Room en modèle réseau [Host]. */
private fun HostEntity.toHost(): Host = Host(
    ipAddress = ipAddress,
    hostname = hostname,
    isReachable = true
)

/** Convertit un modèle réseau [Host] en [HostEntity] Room. */
private fun Host.toEntity(): HostEntity = HostEntity(
    ipAddress = ipAddress,
    hostname = hostname
)
