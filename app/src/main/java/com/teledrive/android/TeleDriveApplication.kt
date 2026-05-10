package com.teledrive.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import android.os.Build
import androidx.work.Configuration
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TeleDriveApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        configureCoil()
        createNotificationChannel()
    }

    private fun configureCoil() {
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("coil_cache"))
                        .maxSizePercent(0.02)
                        .build()
                }
                .crossfade(true)
                .respectCacheHeaders(false)
                .build()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "backup_channel",
                "Backup",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing backup progress"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
