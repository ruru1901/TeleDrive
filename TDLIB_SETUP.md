# TDLib Packaging Notes

`TdLibTelegramGateway` is implemented in `app/src/main/java/com/teledrive/android/telegram/TdLibTelegramGateway.kt`, but the repository does not yet include TDLib's generated Java classes or native Android binaries.

## What The App Expects

At runtime the app checks for:

- `org.drinkless.tdlib.Client`
- `org.drinkless.tdlib.TdApi`
- or the tdlibx package:
  - `org.drinkless.td.libcore.telegram.Client`
  - `org.drinkless.td.libcore.telegram.TdApi`

If either package is present, `TeleDriveApplication` creates `TdLibTelegramGateway`. If neither is present, it falls back to `InMemoryTelegramGateway` so development builds still run.

## Current Gradle Attempt

Status: TDLibX is now wired and Gradle resolved the AAR successfully on this machine.

The project is wired to try tdlibx through JitPack:

```kotlin
implementation("com.github.tdlibx:td:1.8.56")
```

`settings.gradle.kts` includes:

```kotlin
maven("https://jitpack.io")
```

This should provide the `org.drinkless.td.libcore.telegram` package if JitPack resolves correctly.

Confirmed AAR contents after Gradle resolution:

- `classes.jar`
- `jni/arm64-v8a/libtdjni.so`
- `jni/armeabi-v7a/libtdjni.so`
- `jni/x86/libtdjni.so`
- `jni/x86_64/libtdjni.so`

Note: JVM unit tests may still print `UnsatisfiedLinkError` when probing the standard `org.drinkless.tdlib` package because the desktop JVM cannot load Android `.so` files. The gateway catches that and selects the TDLibX package instead.

## Device Packaging Result

Verified on connected device `H6JNSSTOYP8LS8BM`:

- `:app:assembleDebug` succeeded.
- `app-debug.apk` installed with `adb install -r`.
- Android package reports `primaryCpuAbi=arm64-v8a`.
- APK contains native TDLib libraries for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.
- Initial launch crash from `Client.create(...)` method mismatch was fixed by adapting `TdLibReflection`.
- After the fix, `com.teledrive.android` starts and remains running.

Remaining manual step: unlock/wake the device screen and complete the Telegram auth flow in the UI.

## Files Needed

Add a TDLib Android package that provides:

- Java/Kotlin-visible classes under `org.drinkless.tdlib`.
- Native libraries for Android ABIs, usually including `libtdjni.so`.

Typical locations:

- Java/AAR dependency through Gradle, or local AAR/JAR under `app/libs`.
- Native `.so` files under `app/src/main/jniLibs/<abi>/`.

Example local layout:

```text
app/
  libs/
    tdlib.aar
  src/main/jniLibs/
    arm64-v8a/libtdjni.so
    armeabi-v7a/libtdjni.so
    x86_64/libtdjni.so
```

Then add the local dependency in `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(files("libs/tdlib.aar"))
}
```

## Validation Checklist

1. Install a debug build on a physical device or emulator.
2. Enter Telegram API ID/hash from `my.telegram.org`.
3. Complete phone, code, and 2FA auth if enabled.
4. Confirm TDLib creates app-private data under `files/tdlib`.
5. Confirm `Saved Messages` lists document/media messages.
6. Create a folder and confirm a Telegram channel is created with `[TD]`.
7. Upload a small file and confirm it appears in Telegram.
8. Download, delete, and move a file.

## Known Adapter Caveats

- The gateway uses reflection to support TDLib API shape differences. If your TDLib build uses renamed fields/classes, adjust `TdLibTelegramGateway`.
- Upload/download progress currently emits start and finish. Rich progress should be wired from TDLib file updates.
- Folder delete currently clears history and leaves the chat where supported. Full channel deletion may need a TDLib/API-specific owner-only call.
