/**
 * @file AppDatabase.kt
 * @description Base de données Room de l'application.
 */
package com.scaminal.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.scaminal.data.dao.CommandShortcutDao
import com.scaminal.data.dao.HostDao
import com.scaminal.data.entity.CommandShortcut
import com.scaminal.data.entity.HostEntity

@Database(
    entities = [HostEntity::class, CommandShortcut::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hostDao(): HostDao
    abstract fun commandShortcutDao(): CommandShortcutDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS command_shortcuts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        command TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
    }
}
