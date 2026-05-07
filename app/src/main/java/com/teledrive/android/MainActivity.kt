package com.teledrive.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.*
import com.teledrive.android.ui.TeleDriveApp
import com.teledrive.android.ui.auth.AuthViewModelFactory
import com.teledrive.android.ui.drive.DriveViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.teledrive.android.secure.SecureSettings

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val composeContainer = findViewById<android.widget.FrameLayout>(R.id.composeContainer)
        val app = application
        val secureSettings = SecureSettings(app)

        ComposeView(this).also { composeContainer.addView(it) }.setContent {
            var container by remember { mutableStateOf(AppContainer.cachedOrNull()) }
            var darkMode by remember { mutableStateOf(secureSettings.getDarkMode()) }

            LaunchedEffect(Unit) {
                if (container == null) {
                    container = withContext(Dispatchers.IO) { AppContainer.create(app) }
                }
            }

            val ready = container ?: return@setContent

            val authViewModel: com.teledrive.android.ui.auth.AuthViewModel = viewModel(
                factory = AuthViewModelFactory(ready.telegramGateway, ready.secureSettings)
            )
            val driveViewModel: com.teledrive.android.ui.drive.DriveViewModel = viewModel(
                factory = DriveViewModelFactory(ready.database, ready.telegramGateway, ready.backupManager, ready.secureSettings, app)
            )

            TeleDriveApp(
                authViewModel = authViewModel,
                driveViewModel = driveViewModel,
                hasCompletedLogin = ready.secureSettings.hasCompletedLogin(),
                darkMode = darkMode,
                onToggleTheme = {
                    darkMode = !darkMode
                    secureSettings.setDarkMode(darkMode)
                },
            )
        }
    }
}