/**
 * @file AppDatabase.kt
 * @description Base de données Room de l'application.
 */
package com.scaminal.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scaminal.data.dao.HostDao
import com.scaminal.data.entity.HostEntity

@Database(
    entities = [HostEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hostDao(): HostDao
}
