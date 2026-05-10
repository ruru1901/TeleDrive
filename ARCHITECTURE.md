# TeleDrive Android Architecture Handoff

This file is for future agents and engineers picking up the Android implementation. It explains what is wired, what works today, and where the real Telegram TDLib integration should attach.

## Project Shape

- `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`: native Android Gradle project using Kotlin, Compose, Material 3, Room, WorkManager dependency, and encrypted preferences.
- `gradlew`, `gradlew.bat`, `gradle/wrapper`: Gradle wrapper generated for reproducible local builds.
- `app/src/main/java/com/teledrive/android/TeleDriveApplication.kt`: creates the app container and wires dependencies.
- `app/src/main/java/com/teledrive/android/MainActivity.kt`: starts Compose and creates the auth + drive view models.
- `app/src/main/java/com/teledrive/android/ui`: Compose screens and theme.
- `app/src/main/java/com/teledrive/android/data`: Room entities, DAO, converters, and database.
- `app/src/main/java/com/teledrive/android/telegram`: Telegram gateway contract, TDLib reflection adapter, and temporary in-memory fallback.
- `app/src/main/java/com/teledrive/android/drive`: mapping helpers and folder marker parsing.
- `app/src/main/java/com/teledrive/android/queue`: transfer state reducer.
- `app/src/main/java/com/teledrive/android/preview`: cache pruning policy.

## Dependency Wiring

Current runtime wiring is:

1. `TeleDriveApplication.onCreate()`
2. `AppContainer.create(application)`
3. Creates Room database: `TeleDriveDatabase`
4. Creates encrypted settings: `SecureSettings`
5. Creates Telegram gateway:
   - `TdLibTelegramGateway` when `org.drinkless.tdlib.*` or `org.drinkless.td.libcore.telegram.*` classes are packaged.
   - `InMemoryTelegramGateway` when TDLib classes are absent.
6. `MainActivity` injects these into:
   - `AuthViewModel`
   - `DriveViewModel`
7. `TeleDriveApp` switches UI based on `AuthState`

The runtime selection point is in `TeleDriveApplication.kt`:

```kotlin
telegramGateway = if (TdLibTelegramGateway.isAvailable()) {
    TdLibTelegramGateway(...)
} else {
    InMemoryTelegramGateway()
}
```

`TdLibTelegramGateway` uses reflection so the app supports both standard TDLib Java bindings and the TDLibX Android package. TDLibX is currently packaged through JitPack.

## Implemented Working Features

These features currently work against `InMemoryTelegramGateway` and Room:

- API credential form with encrypted local storage.
- Phone login, code login, and 2FA screen flow.
- Logout clears encrypted settings and returns to onboarding.
- Root drive named `Saved Messages`.
- Folder list bottom sheet.
- Create folder.
- Delete folder.
- File listing for the active folder.
- Search field with local Room search and gateway-backed refresh for queries longer than two characters.
- Android file picker upload flow.
- Upload progress queue.
- Download progress queue.
- File delete.
- Multi-select by long-pressing a file card.
- Bulk delete for selected files.
- Move selected files between `Saved Messages` and folder channels.
- Folder delete confirmation dialog.
- TDLib-backed gateway implementation for auth, folder scan/create/delete, list/search, upload/download, delete, and move.
- TDLibX AAR packaging through `com.github.tdlibx:td:1.8.56`, including Android `libtdjni.so` binaries.
- Automatic fallback to the fake gateway if TDLib is not packaged.
- Debug APK assembly and install to a connected Android device.
- File metadata preview dialog.
- Stable file cards with type icons for images, videos, PDFs, and generic files.
- Unit tests for:
  - folder marker parsing,
  - file metadata mapping,
  - transfer progress state transitions,
  - preview cache pruning.

## Temporary Fake Telegram Behavior

`InMemoryTelegramGateway` is a development stand-in. It does not connect to Telegram.

It simulates:

- Auth states.
- A couple of starter files in `Saved Messages`.
- Folder creation/deletion.
- Upload progress and adding uploaded files to memory.
- Download progress.
- File delete and move logic.

Special fake-auth note:

- Any non-empty login code signs in.
- Code `2222` forces the 2FA password screen.

## TDLib Gateway Coverage

`TdLibTelegramGateway` currently implements the `TelegramGateway` contract using TDLib Java API names through reflection:

- Authorization states:
  - `AuthorizationStateWaitTdlibParameters`
  - `AuthorizationStateWaitEncryptionKey`
  - `AuthorizationStateWaitPhoneNumber`
  - `AuthorizationStateWaitCode`
  - `AuthorizationStateWaitPassword`
  - `AuthorizationStateReady`
- Auth calls:
  - `SetTdlibParameters`
  - `CheckDatabaseEncryptionKey`
  - `SetAuthenticationPhoneNumber`
  - `CheckAuthenticationCode`
  - `CheckAuthenticationPassword`
  - `LogOut`
- Drive calls:
  - `GetMe` for `Saved Messages`
  - `GetChats`, `GetChat`, `GetChatFullInfo`
  - `CreateNewSupergroupChat`
  - `SetChatMessageAutoDeleteTime`
  - `DeleteChatHistory`, `LeaveChat`
  - `GetChatHistory`, `GetMessage`
  - `SendMessage`, `DownloadFile`, `DeleteMessages`, `ForwardMessages`

The adapter supports common TDLib API shape differences, including both direct `SetTdlibParameters` fields and older `TdlibParameters` nested payloads.

## Pending Production Work

Highest priority:

- Complete manual TDLib auth validation on the connected Android device: enter API ID/hash, phone, code, and optional 2FA.
- Confirm the exact TDLib build's Java field names and adjust reflection shims if needed.
- Replace simulated progress with update-driven upload/download progress from TDLib file updates.

TDLib gateway responsibilities:

- Replace current cross-folder local filtering with TDLib's dedicated global search API if a broader search surface is desired.
- Decide whether folder deletion should leave/archive channels or fully delete channels for owner accounts, depending on what TDLib exposes for the account type.
- Export downloaded files to user-selected destinations instead of only triggering TDLib download.

UI still pending:

- Real thumbnail decoding/caching for images.
- Video/audio player screen.
- PDF viewer screen using Android PDF renderer or a maintained PDF dependency.
- Cache cleanup settings.
- Background-safe WorkManager upload/download workers.
- Notifications for long-running transfers.

Testing still pending:

- TDLib runtime integration tests on an emulator/device with packaged TDLib.
- Room DAO tests.
- Compose UI tests for onboarding and dashboard flows.
- Android instrumentation tests for file picker, preview, and download/share flows.

## Known Build Notes

- Gradle wrapper files are present. Prefer `.\gradlew.bat` after the wrapper distribution finishes downloading.
- Gradle is not installed on PATH on this machine. A temporary Gradle distribution was also downloaded under `%TEMP%\gradle-8.14.3` for verification.
- Android SDK was found under `%LOCALAPPDATA%\Android\Sdk`; set `ANDROID_HOME` before running Gradle from PowerShell.
- `:app:compileDebugKotlin` passed offline with this command:

```powershell
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
& "$env:TEMP\gradle-8.14.3\bin\gradle.bat" :app:compileDebugKotlin --offline --no-daemon --stacktrace --console=plain --max-workers=1
```

- `:app:testDebugUnitTest` passed with this command:

```powershell
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
& "$env:TEMP\gradle-8.14.3\bin\gradle.bat" :app:testDebugUnitTest --no-daemon --stacktrace --console=plain --max-workers=1
```

- Current unit-test coverage includes the TDLib availability check. TDLibX classes are now packaged, so `TdLibTelegramGateway.isAvailable()` is expected to be true.
- `:app:assembleDebug` passed and produced `app/build/outputs/apk/debug/app-debug.apk`.
- The APK was installed with `adb install -r` on connected device `H6JNSSTOYP8LS8BM`.
- APK inspection confirmed packaged native libraries:
  - `lib/arm64-v8a/libtdjni.so`
  - `lib/armeabi-v7a/libtdjni.so`
  - `lib/x86/libtdjni.so`
  - `lib/x86_64/libtdjni.so`
- First device launch exposed a TDLib `Client.create(...)` signature mismatch; `TdLibReflection.createClient` was updated to adapt to the actual packaged method signature.
- After the fix, the app installed and started as process `com.teledrive.android` without the previous `NoSuchMethodException` crash. Visual screenshot verification was limited because the connected device reported `mAwake=false` / protected capture.

- A wrapper-based `.\gradlew.bat :app:testDebugUnitTest ...` run was attempted once and timed out during wrapper startup/download; no compile/test failure was observed from that wrapper attempt. The temporary Gradle command above is the verified path.
- If Android Studio is used, open `H:\code\drive\TeleDrive` directly as the Gradle project.

## Design Intention

The code is intentionally layered so future agents can replace the fake gateway without touching Compose screens or Room models. UI and persistence depend on `TelegramGateway`, not TDLib directly.
