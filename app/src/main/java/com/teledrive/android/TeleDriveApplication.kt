package com.teledrive.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.teledrive.android.data.TeleDriveDatabase
import com.teledrive.android.secure.SecureSettings
import com.teledrive.android.telegram.InMemoryTelegramGateway
import com.teledrive.android.telegram.TdLibTelegramGateway
import com.teledrive.android.telegram.TelegramGateway
import net.sqlcipher.database.SupportFactory

class TeleDriveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.create(this)
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

data class AppContainer(
    val database: TeleDriveDatabase,
    val secureSettings: SecureSettings,
    val telegramGateway: TelegramGateway,
    val backupManager: com.teledrive.android.backup.BackupManager,
    val manifestManager: com.teledrive.android.backup.ManifestManager,
) {
    companion object {
        @Volatile
        private var cached: AppContainer? = null

        fun cachedOrNull(): AppContainer? = cached

        fun create(app: Application): AppContainer {
            cached?.let { return it }

            // Create encrypted database using SQLCipher
            // If the existing DB is unencrypted (legacy), delete it so SQLCipher can create a fresh one.
            val dbFile = app.getDatabasePath("teledrive.db")
            if (dbFile.exists()) {
                runCatching {
                    val header = ByteArray(16)
                    dbFile.inputStream().use { it.read(header) }
                    val isSQLite = header.take(6).map { it.toInt().toChar() }.joinToString("") == "SQLite"
                    if (isSQLite) dbFile.delete()
                }
            }
            val passphrase = SupportFactory(SecureSettings(app).tdlibDatabaseKey())
            val database = Room.databaseBuilder(
                app,
                TeleDriveDatabase::class.java,
                "teledrive.db",
            )
                .openHelperFactory(passphrase)
                .addMigrations(TeleDriveDatabase.MIGRATION_4_5)
                .addMigrations(TeleDriveDatabase.MIGRATION_5_6)
                .addMigrations(TeleDriveDatabase.MIGRATION_6_7)
                .fallbackToDestructiveMigration(true)
                .build()

            val settings = SecureSettings(app)

            val gateway = if (TdLibTelegramGateway.isAvailable()) {
                TdLibTelegramGateway(
                    context = app,
                    databaseEncryptionKey = settings.tdlibDatabaseKey(),
                )
            } else {
                InMemoryTelegramGateway(app)
            }

            return AppContainer(
                database = database,
                secureSettings = settings,
                telegramGateway = gateway,
                backupManager = com.teledrive.android.backup.BackupManager(app),
                manifestManager = com.teledrive.android.backup.ManifestManager(gateway),
            ).also { cached = it }
        }
    }
}
