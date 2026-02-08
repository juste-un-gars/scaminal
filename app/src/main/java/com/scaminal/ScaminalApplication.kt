/**
 * @file ScaminalApplication.kt
 * @description Point d'entrée de l'application. Initialise Hilt et Timber.
 */
package com.scaminal

import android.app.Application
import com.scaminal.data.repository.ShortcutRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ScaminalApplication : Application() {

    @Inject lateinit var shortcutRepository: ShortcutRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        setupLogging()
        appScope.launch { shortcutRepository.ensureDefaults() }
    }

    private fun setupLogging() {
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Scaminal started — debug logging enabled")
        }
    }
}
