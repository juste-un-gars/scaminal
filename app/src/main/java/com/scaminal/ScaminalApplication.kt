/**
 * @file ScaminalApplication.kt
 * @description Point d'entrée de l'application. Initialise Hilt et Timber.
 */
package com.scaminal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ScaminalApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupLogging()
    }

    private fun setupLogging() {
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Scaminal started — debug logging enabled")
        }
    }
}
