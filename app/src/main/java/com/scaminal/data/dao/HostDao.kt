/**
 * @file HostDao.kt
 * @description DAO Room pour les opérations CRUD sur les hôtes.
 */
package com.scaminal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.scaminal.data.entity.HostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HostDao {

    @Query("SELECT * FROM hosts ORDER BY lastSeen DESC")
    fun getAllHosts(): Flow<List<HostEntity>>

    @Query("SELECT * FROM hosts WHERE isFavorite = 1 ORDER BY lastSeen DESC")
    fun getFavorites(): Flow<List<HostEntity>>

    @Query("SELECT * FROM hosts WHERE id = :id")
    suspend fun getById(id: Long): HostEntity?

    @Query("SELECT * FROM hosts WHERE ipAddress = :ip LIMIT 1")
    suspend fun getByIp(ip: String): HostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(host: HostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hosts: List<HostEntity>)

    @Update
    suspend fun update(host: HostEntity)

    @Delete
    suspend fun delete(host: HostEntity)

    @Query("DELETE FROM hosts WHERE isFavorite = 0")
    suspend fun deleteNonFavorites()
}
