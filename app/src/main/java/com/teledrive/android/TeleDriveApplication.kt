package com.teledrive.android

import android.app.Application
import androidx.room.Room
import com.teledrive.android.data.TeleDriveDatabase
import com.teledrive.android.secure.SecureSettings
import com.teledrive.android.telegram.InMemoryTelegramGateway
import com.teledrive.android.telegram.TdLibTelegramGateway
import com.teledrive.android.telegram.TelegramGateway

data class AppContainer(
    val database: TeleDriveDatabase,
    val secureSettings: SecureSettings,
    val telegramGateway: TelegramGateway,
    val backupManager: com.teledrive.android.backup.BackupManager,
) {
    companion object {
        @Volatile
        private var cached: AppContainer? = null

        fun cachedOrNull(): AppContainer? = cached

        fun create(app: Application): AppContainer {
            cached?.let { return it }

            val database = Room.databaseBuilder(
                app,
                TeleDriveDatabase::class.java,
                "teledrive.db",
            ).fallbackToDestructiveMigration().build()

            val settings = SecureSettings(app)

            return AppContainer(
                database = database,
                secureSettings = settings,
                telegramGateway = if (TdLibTelegramGateway.isAvailable()) {
                    TdLibTelegramGateway(
                        context = app,
                        databaseEncryptionKey = settings.tdlibDatabaseKey(),
                    )
                } else {
                    InMemoryTelegramGateway(app)
                },
                backupManager = com.teledrive.android.backup.BackupManager(app),
            ).also { cached = it }
        }
    }
}
