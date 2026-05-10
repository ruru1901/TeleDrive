package com.teledrive.android.telegram

/**
 * Production Telegram access belongs behind [TelegramGateway].
 *
 * The app is wired to [InMemoryTelegramGateway] while the Android project is bootstrapped so
 * Compose, Room, queues, and tests can run without a Telegram account. The production swap is to
 * implement this contract with Telegram TDLib's Android Java bindings and instantiate that class
 * from AppContainer. Keeping the seam here prevents TDLib authorization state churn from leaking
 * into UI code.
 */
object TdLibReadiness {
    const val folderTitleMarker = "[TD]"
    const val folderAboutMarker = "[telegram-drive-folder]"
}
