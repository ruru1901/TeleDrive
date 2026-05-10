# TeleDrive Build Error Report & Fix Plan

## CRITICAL (Hard Compilation Failures)

### #1 Ambiguous `uploadFile` overload
**File:** `TelegramGateway.kt` (lines 23, 40-45)
**Error:** Two declarations overlap when `backupPath` uses its default value. Any call with 3 args (`uri, name, folderId`) is ambiguous.
**Fix:** Remove the standalone 3-param overload on line 23. Keep only the 4-param version but **remove the default value** (`= null`) from `backupPath` on line 44. All existing callers that pass 3 args should be updated to pass `null` explicitly as the 4th argument. Affected callers: `InMemoryTelegramGateway.kt:135`, `TdLibTelegramGateway.kt:215`, `DriveViewModel.kt:158`.

### #2 Unresolved reference `DashboardScaffold`
**File:** `TeleDriveApp.kt` (line 555)
**Error:** `DashboardScaffold(...)` is called but never defined. Only `DashboardScreen` exists (line 529).
**Fix:** See #3 — the orphaned code block starting at line 752 is the missing body. Wrap it with the correct function signature.

### #3 Orphaned code block (body of `DashboardScaffold`)
**File:** `TeleDriveApp.kt` (lines 752–1108)
**Error:** After `BackupSettingsCard` closes at line 751, lines 752–1108 contain floating `val` declarations, `LaunchedEffect`, `Scaffold`, etc. outside any function.
**Fix:** Insert a function declaration before line 752:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScaffold(
    state: DriveUiState,
    darkMode: Boolean,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onSelectFolder: (Long?) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onUpload: (Uri, String) -> Unit,
    onDownload: (FileEntity) -> Unit,
    onDownloadTo: (FileEntity, Uri) -> Unit,
    onDeleteFile: (FileEntity) -> Unit,
    onDeleteFiles: (List<FileEntity>) -> Unit,
    onMoveFiles: (List<FileEntity>, Long?) -> Unit,
    onUpdateBackupSettings: (BackupSettingsEntity) -> Unit,
    onSetDownloadDestination: (Uri) -> Unit,
    onClearDownloadDestination: () -> Unit,
    onRecordBackupRun: () -> Unit,
    onRestoreAll: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
```
And ensure the closing `}` is placed after line 1108.

### #4 Unresolved reference `MIGRATION_5_6`
**File:** `TeleDriveApplication.kt` (line 60)
**Error:** Only `MIGRATION_4_5` and `MIGRATION_6_7` are defined in `TeleDriveDatabase.kt`.
**Fix:** Add an empty migration to `TeleDriveDatabase.kt` companion object (between `MIGRATION_4_5` and `MIGRATION_6_7`):
```kotlin
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No schema changes between v5 and v6
    }
}
```

---

## MISSING DEPENDENCIES IN `libs.versions.toml`

### #5 `kotlinx-coroutines-android`
**Used in:** All 27+ `.kt` files
**Impact:** `kotlinx.coroutines.*` imports unresolved everywhere
**Fix:** Add to `libs.versions.toml`:
```toml
[versions]
coroutines = "1.10.2"

[libraries]
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
```
Add to `app/build.gradle.kts` dependencies:
```kotlin
implementation(libs.kotlinx.coroutines.android)
testImplementation(libs.kotlinx.coroutines.test)
```

### #6 `androidx.appcompat:appcompat`
**Used in:** `MainActivity.kt:4`
**Impact:** `AppCompatActivity` unresolved
**Fix:** Add to `libs.versions.toml`:
```toml
[versions]
appcompat = "1.7.0"

[libraries]
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "appcompat" }
```
Add to `app/build.gradle.kts` (replace the hardcoded string at line 49):
```kotlin
implementation(libs.androidx.appcompat)
```

### #7 `lazysodium-android`
**Used in:** `GhostCrypto.kt:3-6`, `KeystoreRepositoryImpl.kt:5-8`
**Impact:** `LazySodiumAndroid`, `SecretBox`, `Key` unresolved
**Fix:** Add to `libs.versions.toml`:
```toml
[versions]
lazysodium = "5.1.3"

[libraries]
lazysodium-android = { module = "com.goterl:lazysodium-android", version.ref = "lazysodium" }
```
Add to `app/build.gradle.kts`:
```kotlin
implementation(libs.lazysodium.android)
```

### #8 `kotlinx-serialization-json`
**Used in:** `KeystoreRepositoryImpl.kt:16-18`
**Impact:** `kotlinx.serialization.*` unresolved
**Fix:** Add to `libs.versions.toml`:
```toml
[versions]
kotlinxSerialization = "1.8.1"

[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```
Add to `app/build.gradle.kts`:
```kotlin
// In plugins block:
alias(libs.plugins.kotlin.serialization)

// In dependencies block:
implementation(libs.kotlinx.serialization.json)
```

### #9 `sqlcipher-android`
**Used in:** `KeystoreRepositoryImpl.kt:19`
**Impact:** `net.zetetic.database.sqlcipher.SupportFactory` unresolved
**Fix:** Add to `libs.versions.toml`:
```toml
[versions]
sqlcipher = "4.6.1"

[libraries]
sqlcipher-android = { module = "net.zetetic:sqlcipher-android", version.ref = "sqlcipher" }
```
Add to `app/build.gradle.kts`:
```kotlin
implementation(libs.sqlcipher.android)
```

### #10 `argon2kt`
**Used in:** `KeystoreRepositoryImpl.kt:27,78,163,225`
**Impact:** `Argon2kt`, `ArgonType` unresolved
**Fix:** Add to `libs.versions.toml`:
```toml
[versions]
argon2kt = "1.4.0"

[libraries]
argon2kt = { module = "com.lambdapioneer.argon2kt:argon2kt", version.ref = "argon2kt" }
```
Add to `app/build.gradle.kts`:
```kotlin
implementation(libs.argon2kt)
```

### #11 `javax.inject`
**Used in:** `KeystoreRepository.kt:8-9`, `KeystoreRepositoryImpl.kt:21-22`
**Impact:** `@Inject`, `@Singleton` unresolved
**Fix:** Add to `libs.versions.toml`:
```toml
[libraries]
javax-inject = { module = "javax.inject:javax.inject", version = "1" }
```
Add to `app/build.gradle.kts`:
```kotlin
implementation(libs.javax.inject)
```

---

## MISSING PROPERTIES / UNRESOLVED REFERENCES

### #12-14 `ghostPrefs` and `ghostDb` missing from `SecureSettings`
**File:** `KeystoreRepositoryImpl.kt` (lines 31, 36, 42, 48, 54, 60, 113, 114, 145, 195, 252, 256, 260, 264)
**Error:** `secureSettings.ghostPrefs` and `secureSettings.ghostDb` do not exist on `SecureSettings` class
**Fix:** Add these properties to `SecureSettings.kt`:
```kotlin
// Add imports:
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportFactory

// Add to SecureSettings class:
val ghostPrefs: SharedPreferences by lazy {
    EncryptedSharedPreferences.create(
        context,
        "teledrive_ghost_secure",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}

private var _ghostDb: TeleDriveDatabase? = null
val ghostDb: TeleDriveDatabase?
    get() {
        if (_ghostDb == null) {
            _ghostDb = try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val sqlCipherKey = masterKey.getOrGenerateSecretKey().encoded
                val factory = SupportFactory(sqlCipherKey)
                Room.databaseBuilder(
                    context,
                    TeleDriveDatabase::class.java,
                    "teledrive_ghost.db"
                ).openHelperFactory(factory).build()
            } catch (e: Exception) {
                null
            }
        }
        return _ghostDb
    }
```

### #15 Unused import
**File:** `KeystoreRepository.kt` (line 3)
**Error:** `import com.teledrive.android.crypto.GhostCrypto` is never used
**Fix:** Delete line 3.

---

## GRADLE VERSION COMPATIBILITY

### #16 KSP version alignment
**File:** `libs.versions.toml:42`
**Issue:** KSP `2.2.21-2.0.4` against Kotlin `2.2.21`. This version may not exist yet.
**Fix:** Check Maven for the actual latest KSP version compatible with Kotlin 2.2.21. If unavailable, downgrade Kotlin to `2.1.0` and use KSP `2.1.0-1.0.29`, or use the latest confirmed KSP version.

### #17 AGP 8.11.1 + Gradle 8.14.3
**Issue:** AGP 8.11.1 may not be published yet.
**Fix:** Verify AGP version exists on `google()`. If not, use `8.9.0` (latest stable confirmed).

### #18 Compose BOM `2025.10.01`
**Issue:** Future-dated BOM may not exist.
**Fix:** Verify on Maven. If not found, use `2025.05.00` or the latest available BOM version.

---

## TEST ISSUES

### #19 TDLib availability test always fails
**File:** `TdLibTelegramGatewayTest.kt` (~line 10)
**Error:** `assertTrue(TdLibTelegramGateway.isAvailable())` fails on JVM — no native TDLib libs
**Fix:** Change to `assertFalse(TdLibTelegramGateway.isAvailable())` since native libs are never present in a headless JVM unit test environment. Alternatively, move this test to `src/androidTest/` for instrumented testing on a real device.

---

## SUMMARY OF ACTIONS

| Priority | Category | Count | Effort |
|----------|----------|-------|--------|
| P0 | Code fixes (#1-#4, #15, #19) | 6 | ~30 min |
| P0 | Missing dependencies (#5-#11) | 7 | ~20 min |
| P1 | Missing properties (#12-#14) | 1 (multi-line) | ~15 min |
| P2 | Version verification (#16-#18) | 3 | ~10 min |

**Estimated total fix time: ~75 minutes**
