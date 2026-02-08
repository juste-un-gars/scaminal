/**
 * @file CommandShortcutDao.kt
 * @description DAO Room pour les raccourcis de commandes SSH.
 */
package com.scaminal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.scaminal.data.entity.CommandShortcut
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandShortcutDao {

    @Query("SELECT * FROM command_shortcuts ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<CommandShortcut>>

    @Query("SELECT * FROM command_shortcuts WHERE id = :id")
    suspend fun getById(id: Long): CommandShortcut?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: CommandShortcut): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shortcuts: List<CommandShortcut>)

    @Update
    suspend fun update(shortcut: CommandShortcut)

    @Delete
    suspend fun delete(shortcut: CommandShortcut)

    @Query("SELECT COUNT(*) FROM command_shortcuts")
    suspend fun count(): Int
}
