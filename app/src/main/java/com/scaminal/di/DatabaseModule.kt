/**
 * @file DatabaseModule.kt
 * @description Module Hilt fournissant la base Room et les DAOs.
 */
package com.scaminal.di

import android.content.Context
import androidx.room.Room
import com.scaminal.data.AppDatabase
import com.scaminal.data.dao.HostDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "scaminal.db"
        ).build()
    }

    @Provides
    fun provideHostDao(database: AppDatabase): HostDao {
        return database.hostDao()
    }
}
