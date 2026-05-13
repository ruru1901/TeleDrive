package com.teledrive.android.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.teledrive.android.backup.BackupManager
import com.teledrive.android.backup.ManifestManager
import com.teledrive.android.data.KeyEntryDao
import com.teledrive.android.data.TeleDriveDatabase
import com.teledrive.android.secure.SecureSettings
import com.teledrive.android.telegram.InMemoryTelegramGateway
import com.teledrive.android.telegram.TdLibTelegramGateway
import com.teledrive.android.telegram.TelegramGateway
import com.teledrive.android.repository.KeystoreRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSecureSettings(app: Application): SecureSettings = SecureSettings(app)

    @Provides
    @Singleton
    fun provideDatabase(app: Application, settings: SecureSettings): TeleDriveDatabase {
        val dbFile = app.getDatabasePath("teledrive.db")
        if (dbFile.exists()) {
            runCatching {
                val header = ByteArray(16)
                dbFile.inputStream().use { it.read(header) }
                val isSQLite = header.take(6).map { it.toInt().toChar() }.joinToString("") == "SQLite"
                if (isSQLite) dbFile.delete()
            }
        }
        val passphrase = SupportFactory(settings.tdlibDatabaseKey())
        return Room.databaseBuilder(
            app,
            TeleDriveDatabase::class.java,
            "teledrive.db",
        )
            .openHelperFactory(passphrase)
            .addMigrations(TeleDriveDatabase.MIGRATION_4_5)
            .addMigrations(TeleDriveDatabase.MIGRATION_5_6)
            .addMigrations(TeleDriveDatabase.MIGRATION_6_7)
            .addMigrations(TeleDriveDatabase.MIGRATION_7_8)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideTelegramGateway(app: Application, settings: SecureSettings): TelegramGateway {
        return if (TdLibTelegramGateway.isAvailable()) {
            TdLibTelegramGateway(
                context = app,
                databaseEncryptionKey = settings.tdlibDatabaseKey(),
            )
        } else {
            InMemoryTelegramGateway(app)
        }
    }

    @Provides
    @Singleton
    fun provideKeyEntryDao(database: TeleDriveDatabase): KeyEntryDao = database.keyEntryDao()

    @Provides
    @Singleton
    fun provideKeystoreRepository(keyEntryDao: KeyEntryDao): KeystoreRepository = KeystoreRepository(keyEntryDao)

    @Provides
    @Singleton
    fun provideBackupManager(app: Application): BackupManager = BackupManager(app)

    @Provides
    @Singleton
    fun provideManifestManager(gateway: TelegramGateway): ManifestManager = ManifestManager(gateway)

    @Provides
    fun provideContext(app: Application): Context = app.applicationContext
}