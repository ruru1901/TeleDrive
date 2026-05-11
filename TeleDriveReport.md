# TeleDrive — Final Audit + Kilocode Fix Prompts

---

## CONFIRMED ISSUES (20 total)

| # | Severity | Issue | File |
|---|---|---|---|
| 1 | 🔴 | `#ghost_enc` never written → decrypt gate always false | DriveViewModel.kt |
| 2 | 🔴 | FileProvider authority `.provider` vs `.fileprovider` → crash on decrypt open | DriveViewModel.kt |
| 3 | 🔴 | ProGuard has no LazySodium/JNA keep rules → release APK crypto broken | proguard-rules.pro |
| 4 | 🔴 | `EditMessageText` sets wrong field `text` → manifest never updates | TdLibTelegramGateway.kt |
| 5 | 🔴 | `MasterPasswordService`/`KeystoreRepository` nullable defaults → always null in ViewModel | DriveViewModel.kt |
| 6 | 🔴 | `keystoreBackupEnabled()` hardcoded false, no setter → keystore never backed up | SecureSettings.kt |
| 7 | 🟠 | `cancelTransfer()` only updates DB, never cancels WorkManager job | DriveViewModel.kt |
| 8 | 🟠 | `sourceLabel()` returns hardcoded fake strings shown to users | TeleDriveApp.kt |
| 9 | 🟠 | TwoWay sync `copyTo()` fails silently — no storage permission on Android 10+ | BackupWorker.kt |
| 10 | 🟠 | Completion notification crashes Worker on Android 13+ — no runtime permission check | BackupWorker.kt |
| 11 | 🟠 | `ManifestManager` Hilt singleton never injected; BackupWorker creates own instance | BackupWorker.kt / AppModule.kt |
| 12 | 🟡 | 9 dead UI composables (CategorySection, BackupStatusCard, etc.) defined, never called | TeleDriveApp.kt |
| 13 | 🟡 | 4 dead private methods in TdLibTelegramGateway | TdLibTelegramGateway.kt |
| 14 | 🟡 | 2 empty stub files | core/crypto |
| 15 | 🟡 | 5 dead DAO/Gateway/Repo methods | Various |
| 16 | 🟡 | 3 test-only classes never wired to production | feature/drive, feature/backup |
| 17 | 🟡 | `syncStatus` and `backupSourcePath` entity fields never written | Entities.kt |
| 18 | 🟡 | `SecureSettings.ghostDb` dead second database reference | SecureSettings.kt |
| 19 | 🟡 | 5 dead XML layout files | res/layout |
| 20 | 🟡 | `MIGRATION_5_6` is an empty no-op migration | TeleDriveDatabase.kt |

---
---

# KILOCODE FIX PROMPTS

> Copy each prompt block and give it to Kilocode as a standalone task.
> Each prompt contains full context, exact file locations, and safe change boundaries.

---

## PROMPT 1 — Fix decrypt gate (`#ghost_enc` never written)

```
You are fixing a bug in the TeleDrive Android app (Kotlin, Hilt, Jetpack Compose).

PROBLEM:
In `feature/drive/src/main/java/com/teledrive/android/ui/drive/DriveViewModel.kt`,
the decrypt trigger has TWO conditions:

    val isEncrypted = file.name.endsWith(".ghost") && file.caption?.contains("#ghost_enc") == true

The second condition (caption contains "#ghost_enc") is ALWAYS FALSE because:
- `FileEntity.caption` is never populated by any mapper or upload flow
- `TelegramFile` has no caption field — captions are not mapped to entities
- "#ghost_enc" is never written anywhere in the codebase during upload

Result: the decrypt block on lines 411–430 is completely dead code. All .ghost files
skip decryption silently.

FIX:
In `DriveViewModel.kt`, find the line:

    val isEncrypted = file.name.endsWith(".ghost") && file.caption?.contains("#ghost_enc") == true

Change it to:

    val isEncrypted = file.name.endsWith(".ghost")

CONSTRAINTS:
- Only change this one line
- Do not touch any other logic in startDownload()
- Do not modify any other file
- Do not add imports
```

---

## PROMPT 2 — Fix FileProvider authority mismatch (crash on decrypt open)

```
You are fixing a crash in the TeleDrive Android app (Kotlin, Hilt, Jetpack Compose).

PROBLEM:
In `feature/drive/src/main/java/com/teledrive/android/ui/drive/DriveViewModel.kt`,
the decrypt path opens the decrypted file using FileProvider:

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", decryptedFile)

The authority string is "${context.packageName}.provider"

But in `app/src/main/AndroidManifest.xml`, the FileProvider is declared with:

    android:authorities="${applicationId}.fileprovider"

The authority is ".fileprovider", not ".provider".
This mismatch causes: IllegalArgumentException: Failed to find configured root that contains...
whenever a decrypted file is opened. The correct authority ".fileprovider" is used correctly
in TeleDriveApp.kt's openFileWithSystem() function.

FIX:
In `DriveViewModel.kt`, find this line inside the startDownload() → decrypt block:

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", decryptedFile)

Change it to:

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", decryptedFile)

CONSTRAINTS:
- Only change this one string in this one line
- Do not modify AndroidManifest.xml
- Do not modify any other file
- Do not change any other logic
```

---

## PROMPT 3 — Fix ProGuard for LazySodium/JNA (release APK crypto broken)

```
You are fixing a release build bug in the TeleDrive Android app.

PROBLEM:
`app/proguard-rules.pro` has no keep rules for LazySodium or JNA.
The release build uses isMinifyEnabled = true (confirmed in app/build.gradle.kts).
LazySodium uses JNA (Java Native Access) to bridge to native libsodium.
When ProGuard runs, it obfuscates/removes JNA bridge classes.
At runtime in release APK:
- GhostCrypto.encryptFile() → sodium call throws UnsatisfiedLinkError → falls to JVM AES/GCM fallback (12-byte nonce)
- GhostCrypto.decryptFile() → sodium call throws → falls to JVM AES/GCM fallback
- Files encrypted with sodium (24-byte XChaCha20 nonce) cannot be decrypted by the JVM fallback
- The fallback silently truncates the nonce, causing unrecoverable corruption
- No error is shown to the user

FIX:
Open `app/proguard-rules.pro` and add these lines at the end of the file:

    # LazySodium + JNA — required for GhostCrypto native crypto in release builds
    -keep class com.sun.jna.** { *; }
    -keep interface com.sun.jna.** { *; }
    -keep class com.goterl.lazysodium.** { *; }
    -dontwarn com.sun.jna.**
    -dontwarn com.goterl.lazysodium.**

CONSTRAINTS:
- Only add these lines to proguard-rules.pro
- Do not modify any other file
- Do not change build.gradle.kts
- Do not change GhostCrypto.kt
```

---

## PROMPT 4 — Fix EditMessageText wrong field (backup manifest never updates)

```
You are fixing a bug in the TeleDrive Android app (Kotlin, reflection-based TDLib).

PROBLEM:
In `core/telegram/src/main/java/com/teledrive/android/telegram/TdLibTelegramGateway.kt`,
the editMessage() function (around line 1143) calls TDLib's EditMessageText with a wrong field:

    send(reflection.newFunction("EditMessageText").also {
        reflection.setIfPresent(it, "chatId", chatId)
        reflection.setIfPresent(it, "messageId", messageId)
        reflection.setIfPresent(it, "text", formattedText)           // ← WRONG FIELD
        reflection.setIfPresent(it, "disableWebPagePreview", true)   // ← REMOVED in TDLib 1.8+
    })

TDLib's EditMessageText does NOT have a "text" field. It expects "inputMessageContent"
containing an InputMessageText object. The field "disableWebPagePreview" was removed in TDLib 1.8+.
Because setIfPresent() silently ignores unknown fields, TDLib receives an EditMessageText with
no content — the call returns OK but the message is unchanged.
Result: the backup manifest is NEVER updated after creation. Every backup re-uploads everything.

Compare with sendMessage() in the same file (around line 1120) which correctly uses:
    reflection.setIfPresent(it, "inputMessageContent", reflection.newObject("InputMessageText").also { msg ->
        reflection.setIfPresent(msg, "text", formattedText)
        reflection.setIfPresent(msg, "disableWebPagePreview", true)
        reflection.setIfPresent(msg, "clearDraft", false)
    })

FIX:
Replace the entire body of the editMessage() function's send() call with:

    send(reflection.newFunction("EditMessageText").also {
        reflection.setIfPresent(it, "chatId", chatId)
        reflection.setIfPresent(it, "messageId", messageId)
        reflection.setIfPresent(it, "replyMarkup", null)
        reflection.setIfPresent(it, "inputMessageContent",
            reflection.newObject("InputMessageText").also { msg ->
                reflection.setIfPresent(msg, "text", formattedText)
                reflection.setIfPresent(msg, "clearDraft", false)
            }
        )
    })

CONSTRAINTS:
- Only modify the send() call inside editMessage()
- Do not change the function signature or any surrounding code
- Do not modify sendMessage() or any other function
- Do not add imports
- formattedText is already constructed correctly above the send() call — do not change that
```

---

## PROMPT 5 — Fix MasterPasswordService/KeystoreRepository always null

```
You are fixing a dependency injection bug in the TeleDrive Android app (Kotlin, Hilt).

PROBLEM:
In `feature/drive/src/main/java/com/teledrive/android/ui/drive/DriveViewModel.kt`,
the constructor declares:

    @HiltViewModel
    class DriveViewModel @Inject constructor(
        private val context: Context,
        database: TeleDriveDatabase,
        private val gateway: TelegramGateway,
        private val backupManager: com.teledrive.android.backup.BackupManager,
        val keystoreRepository: KeystoreRepository? = null,       // ← always null
        val masterPasswordService: MasterPasswordService? = null, // ← always null
    )

Both are nullable with null defaults. Hilt injects @HiltViewModel via @Inject constructor,
but nullable params with defaults are not injected by Hilt — they keep their default (null).
Result: SecuritySettingsFragment always receives null, shows "Security service is not available".
KeystoreRepository.saveKey() is never called during encrypt, so keys are never stored.

Both classes ARE provided by Hilt:
- KeystoreRepository: @Singleton @Inject constructor in KeystoreRepository.kt, with @Provides in AppModule.kt
- MasterPasswordService: @Singleton @Inject constructor in MasterPasswordService.kt

FIX:
In `DriveViewModel.kt`, change the constructor to make both params mandatory (non-nullable):

BEFORE:
    @HiltViewModel
    class DriveViewModel @Inject constructor(
        private val context: Context,
        database: TeleDriveDatabase,
        private val gateway: TelegramGateway,
        private val backupManager: com.teledrive.android.backup.BackupManager,
        val keystoreRepository: KeystoreRepository? = null,
        val masterPasswordService: MasterPasswordService? = null,
    )

AFTER:
    @HiltViewModel
    class DriveViewModel @Inject constructor(
        private val context: Context,
        database: TeleDriveDatabase,
        private val gateway: TelegramGateway,
        private val backupManager: com.teledrive.android.backup.BackupManager,
        val keystoreRepository: KeystoreRepository,
        val masterPasswordService: MasterPasswordService,
    )

Then find all call sites in DriveViewModel.kt that null-check these:
    keystoreRepository?.saveKey(...)       → change to keystoreRepository.saveKey(...)
    masterPasswordService?.syncKeystore()  → change to masterPasswordService.syncKeystore()

CONSTRAINTS:
- Only modify DriveViewModel.kt
- Do not modify AppModule.kt, SecuritySettingsFragment.kt, or any other file
- Do not change any logic other than removing the null checks on these two fields
- The callers in SecuritySettingsFragment.kt already handle nullable params — those stay as-is
  (SecuritySettingsFragment receives them as nullable; DriveViewModel now holds them non-null)
```

---

## PROMPT 6 — Fix keystoreBackupEnabled always false (keystore never backed up)

```
You are fixing a feature that is permanently disabled in the TeleDrive Android app (Kotlin).

PROBLEM:
In `core/crypto/src/main/java/com/teledrive/android/secure/SecureSettings.kt`,
there is a method:

    fun keystoreBackupEnabled(): Boolean = ghostPrefs.getBoolean(PREF_KEYSTORE_BACKUP_ENABLED, false)

Where PREF_KEYSTORE_BACKUP_ENABLED = "ghost_keystore_backup_enabled"

This defaults to false. There is NO setter method and NO code anywhere that writes
true to this preference. So in MasterPasswordService.kt, these two gates:

    if (prefs.keystoreBackupEnabled()) { uploadKeystoreBackup(blobB64) }  // line 49 — never runs
    if (!prefs.keystoreBackupEnabled()) return                              // line 85 — always returns early

...permanently block keystore backup and sync. The master password feature stores keys
locally but they are NEVER backed up to Telegram. On reinstall, all encrypted files are lost.

FIX:
In `SecureSettings.kt`, add a setter method directly after the existing getter:

    fun keystoreBackupEnabled(): Boolean = ghostPrefs.getBoolean(PREF_KEYSTORE_BACKUP_ENABLED, false)

    fun setKeystoreBackupEnabled(enabled: Boolean) {
        ghostPrefs.edit { putBoolean(PREF_KEYSTORE_BACKUP_ENABLED, enabled) }
    }

Then in `MasterPasswordService.kt`, change setMasterPassword() so that setting a master
password automatically enables keystore backup. Find the line at the end of setMasterPassword():

    prefs.ghostPrefs.edit().putBoolean("ghost_master_password_set", true).apply()

Add this line immediately before it:

    prefs.setKeystoreBackupEnabled(true)

CONSTRAINTS:
- Only modify SecureSettings.kt and MasterPasswordService.kt
- Do not change any other files
- Do not change the existing keystoreBackupEnabled() getter
- Do not add a UI toggle — enabling it programmatically when a password is set is correct behavior
- Use the existing `edit { }` extension pattern already used in SecureSettings.kt
```

---

## PROMPT 7 — Fix cancelTransfer() not stopping WorkManager job

```
You are fixing a bug in the TeleDrive Android app (Kotlin, WorkManager).

PROBLEM:
In `feature/drive/src/main/java/com/teledrive/android/ui/drive/DriveViewModel.kt`,
the cancelTransfer() function only writes Cancelled to the database:

    fun cancelTransfer(id: String) {
        viewModelScope.launch {
            val allTransfers = dao.observeTransfers().first()
            val existing = allTransfers.firstOrNull { it.id == id }
            dao.upsertTransfer(TransferEntity(
                id = id,
                ...
                status = TransferStatus.Cancelled,
                ...
            ))
        }
    }

It never calls workManager.cancelUniqueWork(). The UI progress strip disappears (DB shows
Cancelled) but the actual WorkManager Worker keeps running, consuming bandwidth, and
completing the transfer invisibly in the background.

Uploads are enqueued as: workManager.enqueueUniqueWork("upload_$transferId", ...)
Downloads are enqueued as: workManager.enqueueUniqueWork("download_$transferId", ...)

The workManager instance is available as: private val workManager by lazy { WorkManager.getInstance(context) }

FIX:
In `DriveViewModel.kt`, replace the entire cancelTransfer() function with:

    fun cancelTransfer(id: String) {
        workManager.cancelUniqueWork("upload_$id")
        workManager.cancelUniqueWork("download_$id")
        viewModelScope.launch {
            val allTransfers = dao.observeTransfers().first()
            val existing = allTransfers.firstOrNull { it.id == id }
            dao.upsertTransfer(TransferEntity(
                id = id,
                type = existing?.type ?: TransferType.Upload,
                fileName = existing?.fileName ?: "",
                folderId = existing?.folderId,
                messageId = existing?.messageId,
                status = TransferStatus.Cancelled,
                progress = 0,
                error = null,
            ))
        }
    }

CONSTRAINTS:
- Only modify the cancelTransfer() function in DriveViewModel.kt
- Do not modify UploadWorker.kt, DownloadWorker.kt, or any other file
- Do not change any other functions in DriveViewModel.kt
- The workManager field is already declared in the class — do not redeclare it
```

---

## PROMPT 8 — Remove hardcoded fake sourceLabel strings

```
You are fixing misleading UI text in the TeleDrive Android app (Kotlin, Jetpack Compose).

PROBLEM:
In `app/src/main/java/com/teledrive/android/ui/TeleDriveApp.kt`, there is a function:

    private fun sourceLabel(file: FileEntity): String = when {
        file.folderId == null -> "Desktop Sync"
        isAudio(file) -> "Synced from Mac"
        fileTypeGroup(file) == FileTab.Videos -> "Camera Uploads"
        fileTypeGroup(file) == FileTab.Photos -> "Mobile Upload"
        else -> "Shared Folder"
    }

These strings are completely fabricated. There is no Desktop Sync, Mac, or Camera Uploads
feature. Every file shows a fake source label. This function's return value is used in the
file list row as part of sizeAndDate:

    val sizeAndDate = "${formatBytes(file.size)} • ${formatDate(file.createdAt)} • ${sourceLabel(file)}"

FIX:
Option A — remove sourceLabel from the displayed string entirely.
Find the sizeAndDate line in DashboardScaffold() (inside the items { } block):

    val sizeAndDate = "${formatBytes(file.size)} \u2022 ${formatDate(file.createdAt)} \u2022 ${sourceLabel(file)}"

Change it to:

    val sizeAndDate = "${formatBytes(file.size)} \u2022 ${formatDate(file.createdAt)}"

Then delete the entire sourceLabel() function from the file.

CONSTRAINTS:
- Only change the sizeAndDate assignment line and delete the sourceLabel() function
- Do not modify any other display logic
- Do not modify FileEntity or any data classes
- Do not add any new fields
- The \u2022 is a bullet character — keep it as-is in the parts you keep
```

---

## PROMPT 9 — Fix TwoWay sync storage permission crash (Android 10+)

```
You are fixing a silent failure in the TeleDrive Android app (Kotlin, WorkManager).

PROBLEM:
In `feature/backup/src/main/java/com/teledrive/android/backup/BackupWorker.kt`,
the TwoWay sync restore block (around line 130) calls:

    downloaded.copyTo(localFile, overwrite = true)

Where localFile is resolved from resolveLocalPath() which returns paths under
Environment.getExternalStorageDirectory() (e.g. /storage/emulated/0/DCIM/...).

On Android 10+ (API 29+, scoped storage), writing to arbitrary external storage paths
requires MANAGE_EXTERNAL_STORAGE permission. The app's AndroidManifest.xml only declares:
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="29" />

So on Android 10+, copyTo() throws SecurityException. This is caught by the outer
try/catch in BackupWorker which just increments skippedCount. Every TwoWay restore
silently fails on all modern Android devices.

FIX:
In `BackupWorker.kt`, find the TwoWay sync block that calls resolveLocalPath() and downloaded.copyTo().
Wrap the copyTo() call with an Android version check to skip it gracefully on API 29+ and
show a clear reason why it was skipped:

Replace:
    if (localFile != null && (!localFile.exists() || localFile.lastModified() < entry.modifiedEpoch)) {
        gateway.downloadFile(entry.messageId, null).collect { progress ->
            if (progress.done && progress.localPath != null) {
                val downloaded = File(progress.localPath)
                if (downloaded.exists()) {
                    localFile.parentFile?.mkdirs()
                    downloaded.copyTo(localFile, overwrite = true)
                    localFile.setLastModified(entry.modifiedEpoch)
                    restored++
                }
            }
        }
    }

With:
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        // TwoWay restore to arbitrary external paths requires MANAGE_EXTERNAL_STORAGE
        // which is not declared. Skip restore on Android 10+ to avoid SecurityException.
        skippedCount++
    } else if (localFile != null && (!localFile.exists() || localFile.lastModified() < entry.modifiedEpoch)) {
        gateway.downloadFile(entry.messageId, null).collect { progress ->
            if (progress.done && progress.localPath != null) {
                val downloaded = File(progress.localPath)
                if (downloaded.exists()) {
                    localFile.parentFile?.mkdirs()
                    downloaded.copyTo(localFile, overwrite = true)
                    localFile.setLastModified(entry.modifiedEpoch)
                    restored++
                }
            }
        }
    }

CONSTRAINTS:
- Only modify the TwoWay sync block in BackupWorker.kt
- Do not modify AndroidManifest.xml
- Do not add new permissions
- Do not modify resolveLocalPath() or BackupPathResolver
- skippedCount variable is already declared in scope — use it
```

---

## PROMPT 10 — Fix BackupWorker completion notification crash on Android 13+

```
You are fixing a Worker crash in the TeleDrive Android app (Kotlin, WorkManager).

PROBLEM:
In `feature/backup/src/main/java/com/teledrive/android/backup/BackupWorker.kt`,
the completion notification is posted after all work is done:

    NotificationManagerCompat.from(applicationContext).notify(COMPLETION_NOTIF_ID, finalNotif)

On Android 13+ (API 33+, targetSdk=36), posting notifications requires the
POST_NOTIFICATIONS runtime permission. This permission IS declared in AndroidManifest.xml
but is NOT checked at runtime before posting. If the user has not granted it,
this call throws SecurityException, crashing the Worker at the very end.
WorkManager then marks the Work as FAILED even though the backup completed successfully.

FIX:
In `BackupWorker.kt`, wrap the NotificationManagerCompat.notify() call with a
runtime permission check:

Replace:
    NotificationManagerCompat.from(applicationContext).notify(COMPLETION_NOTIF_ID, finalNotif)

With:
    val notifManager = NotificationManagerCompat.from(applicationContext)
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        notifManager.notify(COMPLETION_NOTIF_ID, finalNotif)
    }

CONSTRAINTS:
- Only change this one notify() call in BackupWorker.kt
- Do not modify AndroidManifest.xml
- Do not modify any other file
- Do not add any imports manually — let the IDE/compiler resolve them
  (ContextCompat is from androidx.core, already a dependency)
```

---

## PROMPT 11 — Fix ManifestManager — use Hilt singleton instead of new instance

```
You are fixing a dependency injection issue in the TeleDrive Android app (Kotlin, Hilt).

PROBLEM:
In `feature/backup/src/main/java/com/teledrive/android/backup/BackupWorker.kt`,
ManifestManager is instantiated directly:

    val manifestManager = ManifestManager(gateway)

But in `app/src/main/java/com/teledrive/android/di/AppModule.kt`, a Hilt singleton
is already provided:

    @Provides @Singleton
    fun provideManifestManager(gateway: TelegramGateway): ManifestManager = ManifestManager(gateway)

BackupWorker uses the @EntryPoint pattern (BackupWorkerEntryPoint) to retrieve dependencies.
The ManifestManager singleton is never exposed through this entry point — so it's never used.

FIX:
Step 1 — In `BackupWorker.kt`, add ManifestManager to the BackupWorkerEntryPoint interface:

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupWorkerEntryPoint {
        fun database(): TeleDriveDatabase
        fun telegramGateway(): TelegramGateway
        fun manifestManager(): ManifestManager    // ← add this
    }

Step 2 — In `BackupWorker.kt` doWork(), replace the direct instantiation:

    val manifestManager = ManifestManager(gateway)   // ← remove this line

With retrieval from the entry point:

    val manifestManager = dependencies.manifestManager()   // ← add this

(dependencies is already retrieved as:
    val dependencies = EntryPointAccessors.fromApplication(applicationContext, BackupWorkerEntryPoint::class.java))

CONSTRAINTS:
- Only modify BackupWorker.kt
- Do not modify AppModule.kt (the @Provides is already correct)
- Do not modify ManifestManager.kt
- The import for ManifestManager is already present in BackupWorker.kt
```

---

## PROMPT 12 — Remove dead UI composables

```
You are removing dead code from the TeleDrive Android app (Kotlin, Jetpack Compose).

PROBLEM:
In `app/src/main/java/com/teledrive/android/ui/TeleDriveApp.kt`, the following
functions and classes are defined but have ZERO call sites anywhere in the file
or the entire codebase:

FUNCTIONS TO DELETE:
1. `ThemeWaveRevealLayer()` — Composable function for animated wave reveal
2. `CategorySection()` — Composable for category grid
3. `CategoryCard()` — Composable for single category card (only called by CategorySection)
4. `PreviewThumbnailStrip()` — Composable for thumbnail strip (only called by CategoryCard)
5. `BackupStatusCard()` — Composable for storage stats card
6. `rememberStorageStats()` — @Composable function that reads StatFs (only used by BackupStatusCard)
7. `buildCategories()` — Regular function that groups files into CategoryCardModel list
8. `loadTextSnippet()` — Regular function that reads text file content
9. `currentFolderName()` — Regular function that returns folder display name

DATA CLASSES TO DELETE (only used by the dead functions above):
10. `StorageStats` — data class (only used by BackupStatusCard and rememberStorageStats)
11. `CategoryCardModel` — data class (only used by CategorySection and buildCategories)

FIX:
Delete all 11 items listed above from TeleDriveApp.kt.

Before deleting each item, verify it has no callers by searching for its name in the file.
None of these are called from outside this file.

CONSTRAINTS:
- Only modify TeleDriveApp.kt
- Do not delete any other functions
- Do not delete FileTab, StorageStats (if used elsewhere), or any enum
- After deletion, the file must still compile — check that no remaining code references
  any of the deleted items
- CategoryCard and PreviewThumbnailStrip are called only from CategorySection (also deleted) — safe to remove
```

---

## PROMPT 13 — Remove dead private methods from TdLibTelegramGateway

```
You are removing dead code from the TeleDrive Android app (Kotlin).

PROBLEM:
In `core/telegram/src/main/java/com/teledrive/android/telegram/TdLibTelegramGateway.kt`,
the following private methods are defined but never called anywhere in the file
or the rest of the codebase (confirmed: 1 occurrence each = definition only):

1. `awaitDownloadedBytes()` — private suspend fun (lines ~473–485)
   Was intended for streaming partial download progress. The production code
   uses a polling loop with observeFileState() instead.

2. `awaitDownloadCompletion()` — private suspend fun (lines ~486–494)
   Was intended as a blocking wait for download completion. Never called.

3. `messageFileId()` — private fun (lines ~883–887)
   Was intended to get fileId from a message object. messageRawFile() is used instead.

4. `localDownloadedSize()` — private fun (lines ~924–929)
   Was intended to read downloaded byte count from a TDLib file object. Never called.

FIX:
Delete all four functions from TdLibTelegramGateway.kt.

CONSTRAINTS:
- Only modify TdLibTelegramGateway.kt
- Do not delete awaitDownloadPath() — it IS called (by downloadFileInternal)
- Do not delete awaitUploadCompletion() / waitForUploadCompletion() — they ARE called
- Do not delete observeFileState() — it IS called
- Do not delete flowForFile() — it IS called
- After deletion, verify the file compiles with no missing references
```

---

## PROMPT 14 — Delete empty stub files

```
You are removing dead code from the TeleDrive Android app.

PROBLEM:
Two Kotlin files exist that contain only a comment and no real code:

1. `core/crypto/src/main/java/com/teledrive/android/crypto/MasterPasswordService.kt`
   Content: A comment saying "Intentionally empty"
   Note: The REAL MasterPasswordService is at:
   core/crypto/src/main/java/com/teledrive/android/MasterPasswordService.kt (different package)

2. `core/crypto/src/main/java/com/teledrive/android/repository/KeystoreRepositoryImpl.kt`
   Content: A comment saying "KeystoreRepositoryImpl is not used"
   Note: The REAL implementation is KeystoreRepository.kt in the same package.

FIX:
Delete both files:
- core/crypto/src/main/java/com/teledrive/android/crypto/MasterPasswordService.kt
- core/crypto/src/main/java/com/teledrive/android/repository/KeystoreRepositoryImpl.kt

CONSTRAINTS:
- Only delete these two files
- Do NOT delete:
  - core/crypto/src/main/java/com/teledrive/android/MasterPasswordService.kt (the real one)
  - core/crypto/src/main/java/com/teledrive/android/repository/KeystoreRepository.kt (the real impl)
- Verify no file imports from the deleted paths before deleting
```

---

## PROMPT 15 — Remove dead DAO/Gateway/Repository methods

```
You are removing dead code from the TeleDrive Android app (Kotlin, Room).

PROBLEM:
The following methods are declared but have zero callers in production code
(only referenced in unit/integration tests if at all):

1. In `core/data/src/main/java/com/teledrive/android/data/TeleDriveDao.kt`:
   - `getFileByMessageId(messageId: Long): FileEntity?`
   No production code calls this. Files are looked up via observeFiles() or by messageId
   from the in-memory state.

2. In `core/telegram/src/main/java/com/teledrive/android/telegram/TelegramGateway.kt`
   and both implementations (TdLibTelegramGateway.kt, InMemoryTelegramGateway.kt):
   - `searchFiles(query: String): List<TelegramFile>`
   The UI uses dao.searchFiles() (local DB search) not gateway.searchFiles().
   DriveViewModel never calls gateway.searchFiles().

3. In `core/telegram/src/main/java/com/teledrive/android/telegram/ChunkDownloader.kt`:
   - `fun deletePartialOutput(cacheDir: File, manifest: ChunkManifest)` (single-manifest version)
   Only `deleteAllPartialOutputs()` is called from DownloadWorker. The single-manifest
   version has zero callers.

4. In `core/data/src/main/java/com/teledrive/android/data/KeyEntryDao.kt`:
   - `count(): Int`
   Never called from any production code.

5. In `core/crypto/src/main/java/com/teledrive/android/repository/KeystoreRepository.kt`:
   - `clearAll()`
   Wait — this IS called from SecuritySettingsFragment (keystoreRepository?.clearAll()).
   DO NOT DELETE THIS ONE.

FIX:
Delete the following:
- `getFileByMessageId()` from TeleDriveDao.kt
- `searchFiles()` from TelegramGateway.kt (interface), TdLibTelegramGateway.kt (impl), and InMemoryTelegramGateway.kt (impl)
- `deletePartialOutput()` (single-manifest version only) from ChunkDownloader.kt
- `count()` from KeyEntryDao.kt

CONSTRAINTS:
- Do NOT delete clearAll() from KeystoreRepository — it IS used
- Do NOT delete deleteAllPartialOutputs() from ChunkDownloader — it IS used
- Do NOT delete searchFiles() from TeleDriveDao — dao.searchFiles() IS used by DriveViewModel (local DB search)
- After each deletion, verify the interface and both implementations stay in sync
```

---

## PROMPT 16 — Remove dead test-only classes from production code

```
You are removing dead production code from the TeleDrive Android app (Kotlin).

PROBLEM:
Three classes exist in production source sets (not in test/) but are only used
from unit/integration tests, never wired into any production flow:

1. `feature/drive/src/main/java/com/teledrive/android/queue/TransferReducer.kt`
   Class: TransferReducer with applyProgress() method
   Production use: Zero. DriveViewModel applies progress inline in awaitTransferWork().
   Test use: Used in unit tests.

2. `feature/drive/src/main/java/com/teledrive/android/preview/CachePolicy.kt`
   Classes: CachePolicy, CacheEntry
   Production use: Zero. DAO prunes by age via prunePreviewCache(olderThan). Count/size policy never used.
   Test use: Used in unit tests.

3. `feature/backup/src/main/java/com/teledrive/android/backup/BackupPathResolver.kt`
   Method: resolveUris() specifically (BackupPathResolver itself IS used via resolveBackupFiles())
   Production use of resolveUris(): Zero. BackupWorker calls resolveBackupFiles() directly.
   Test use: Used in integration test.

FIX:
- Delete the entire file: `feature/drive/src/main/java/com/teledrive/android/queue/TransferReducer.kt`
- Delete the entire file: `feature/drive/src/main/java/com/teledrive/android/preview/CachePolicy.kt`
- In `BackupPathResolver.kt`, delete only the resolveUris() method. Keep resolveBackupFiles(),
  getStorageRoot(), and all other methods in the file.

CONSTRAINTS:
- Do NOT delete BackupPathResolver.kt — resolveBackupFiles() and getStorageRoot() ARE used in production
- Do NOT modify any test files
- After deletion, check that no production code imports TransferReducer or CachePolicy
- The unit tests that use these will fail to compile after deletion — that is acceptable
  (they were testing dead production code). If Kilocode asks about test files, say to leave them.
```

---

## PROMPT 17 — Remove dead entity fields (syncStatus, backupSourcePath)

```
You are removing dead database columns from the TeleDrive Android app (Kotlin, Room).

PROBLEM:
In `core/data/src/main/java/com/teledrive/android/data/Entities.kt`, FileEntity has two fields
that are declared but never written by any mapper, worker, or ViewModel in production:

1. `val syncStatus: SyncStatus = SyncStatus.Synced`
   Always stays at its default value Synced. No code ever sets it to LocalOnly or CloudOnly.
   SyncStatusIcon in TeleDriveApp.kt always shows a green checkmark.

2. `val backupSourcePath: String? = null`
   Always stays null. No mapper or BackupWorker sets this field.

These columns exist in the DB schema (added in MIGRATION_6_7: ALTER TABLE files ADD COLUMN caption TEXT).
Wait — caption was added. Let me re-check: backupSourcePath and syncStatus were in the original schema.

FIX:
In `Entities.kt`, remove both fields from FileEntity:
- Remove `val syncStatus: SyncStatus = SyncStatus.Synced`
- Remove `val backupSourcePath: String? = null`

Also delete the SyncStatus enum class from Entities.kt since it will no longer be used.

Then in `TeleDriveApp.kt`:
- Delete the `SyncStatusIcon()` composable function
- Remove the `SyncStatusIcon(file.syncStatus)` call from FileListRow()
- Remove the import for SyncStatus if it becomes unused

Then add a Room migration in `TeleDriveDatabase.kt`:
Add a new MIGRATION_7_8 that does nothing schema-wise (the columns stay in SQLite — 
SQLite doesn't support DROP COLUMN before version 3.35, and removing them from the 
entity just means Room stops reading/writing them):

    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // syncStatus and backupSourcePath columns remain in SQLite
            // but are no longer mapped to FileEntity
        }
    }

Update the @Database annotation version from 7 to 8.
Add .addMigrations(TeleDriveDatabase.MIGRATION_7_8) in AppModule.kt's provideDatabase().

CONSTRAINTS:
- Coordinate changes across: Entities.kt, TeleDriveApp.kt, TeleDriveDatabase.kt, AppModule.kt
- Do NOT remove the caption field — it IS used (file.caption in DriveViewModel)
- Do NOT remove SyncStatus from imports in TeleDriveApp.kt until after removing SyncStatusIcon
- Make sure the @Database version annotation and migration are both updated
```

---

## PROMPT 18 — Remove SecureSettings.ghostDb dead property

```
You are removing dead code from the TeleDrive Android app (Kotlin).

PROBLEM:
In `core/crypto/src/main/java/com/teledrive/android/secure/SecureSettings.kt`,
there is a property:

    private var _ghostDb: TeleDriveDatabase? = null
    val ghostDb: TeleDriveDatabase?
        get() {
            if (_ghostDb == null) {
                _ghostDb = try {
                    Room.databaseBuilder(
                        appContext,
                        TeleDriveDatabase::class.java,
                        "teledrive_ghost.db",
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                } catch (e: Exception) {
                    null
                }
            }
            return _ghostDb
        }

This creates a second TeleDriveDatabase with the name "teledrive_ghost.db".
There are zero callers of `.ghostDb` anywhere in production code outside SecureSettings.kt.
It was referenced in an earlier design (BUILD_ERRORS.md) but was never integrated.

FIX:
In `SecureSettings.kt`:
1. Delete the `private var _ghostDb: TeleDriveDatabase? = null` field
2. Delete the entire `val ghostDb: TeleDriveDatabase?` property with its getter body

Then remove the unused import:
- `import androidx.room.Room` (if no longer used after deletion)
- `import com.teledrive.android.data.TeleDriveDatabase` (if no longer used)

CONSTRAINTS:
- Only modify SecureSettings.kt
- Do not modify any other file
- After deletion, verify no remaining code in SecureSettings.kt references _ghostDb or ghostDb
- Check that removing the Room import doesn't break anything else in the file
```

---

## PROMPT 19 — Delete dead XML layout files

```
You are deleting dead resource files from the TeleDrive Android app.

PROBLEM:
The app is built entirely with Jetpack Compose. The following XML layout files
exist in the resources directory but are NEVER inflated by any Fragment, Activity,
or View in the codebase. They are orphan files from an earlier View-based design:

Files to delete:
1. app/src/main/res/layout/dialog_clear_keys_confirmation.xml
2. app/src/main/res/layout/fragment_change_master_password.xml
3. app/src/main/res/layout/fragment_security_settings.xml
4. app/src/main/res/layout/fragment_set_master_password.xml
5. app/src/main/res/layout/theme_handle.xml

Verification: Search for R.layout.dialog_clear_keys_confirmation,
R.layout.fragment_change_master_password, R.layout.fragment_security_settings,
R.layout.fragment_set_master_password, R.layout.theme_handle in all .kt and .xml files.
None of these R.layout references exist in any source file.

FIX:
Delete all 5 files listed above.

CONSTRAINTS:
- Only delete these 5 XML files
- Do not delete any other resource files
- Do not modify any Kotlin files
- Do not modify AndroidManifest.xml
```

---

## PROMPT 20 — Clean up empty MIGRATION_5_6

```
You are removing a no-op database migration from the TeleDrive Android app (Kotlin, Room).

PROBLEM:
In `core/data/src/main/java/com/teledrive/android/data/TeleDriveDatabase.kt`,
there is an empty migration:

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema changes between v5 and v6
        }
    }

This migration is registered in AppModule.kt:
    .addMigrations(TeleDriveDatabase.MIGRATION_5_6)

The migration does nothing. It exists purely because a version number was bumped
without any schema change. However, this migration MUST remain registered in the
Room builder to support users who installed the app at DB version 5 — removing it
from addMigrations() would cause Room to try destructive migration for v5 users.

FIX:
This is a documentation-only fix. Add a clear comment explaining WHY this exists
so future developers don't question or accidentally remove it.

In `TeleDriveDatabase.kt`, replace:

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema changes between v5 and v6
        }
    }

With:

    // MIGRATION_5_6: intentionally empty — version 6 was a metadata-only bump with no schema changes.
    // This migration MUST stay registered in AppModule so users on v5 can upgrade without data loss.
    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) { /* no-op: schema unchanged */ }
    }

CONSTRAINTS:
- Only add the comment — do not change any code
- Do not remove this migration from AppModule.kt
- Do not change the version numbers
```
