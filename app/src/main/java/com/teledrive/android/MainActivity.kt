package com.teledrive.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.teledrive.android.ui.TeleDriveApp
import com.teledrive.android.ui.auth.AuthViewModel
import com.teledrive.android.ui.drive.DriveViewModel
import com.teledrive.android.secure.SecureSettings
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val driveViewModel: DriveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val secureSettings = SecureSettings(application)
        var darkMode by mutableStateOf(secureSettings.getDarkMode())

        setContent {
            val authState by authViewModel.uiState.collectAsState()
            val driveState by driveViewModel.uiState.collectAsState()

            TeleDriveApp(
                authViewModel = authViewModel,
                driveViewModel = driveViewModel,
                hasCompletedLogin = secureSettings.hasCompletedLogin(),
                darkMode = darkMode,
                onToggleTheme = {
                    darkMode = !darkMode
                    secureSettings.setDarkMode(darkMode)
                },
            )
        }
    }
}
