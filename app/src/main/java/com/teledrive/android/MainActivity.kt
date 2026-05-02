package com.teledrive.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teledrive.android.ui.TeleDriveApp
import com.teledrive.android.ui.TeleDriveTheme
import com.teledrive.android.ui.auth.AuthViewModel
import com.teledrive.android.ui.auth.AuthViewModelFactory
import com.teledrive.android.ui.drive.DriveViewModel
import com.teledrive.android.ui.drive.DriveViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val app = application
            var container by remember { mutableStateOf(AppContainer.cachedOrNull()) }

            LaunchedEffect(Unit) {
                if (container == null) {
                    container = withContext(Dispatchers.IO) {
                        AppContainer.create(app)
                    }
                }
            }

            val readyContainer = container
            if (readyContainer == null) {
                TeleDriveTheme(darkMode = true) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            } else {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(
                        gateway = readyContainer.telegramGateway,
                        secureSettings = readyContainer.secureSettings,
                    ),
                )
                val driveViewModel: DriveViewModel = viewModel(
                    factory = DriveViewModelFactory(
                        database = readyContainer.database,
                        gateway = readyContainer.telegramGateway,
                        backupManager = readyContainer.backupManager,
                    ),
                )
                TeleDriveApp(
                    authViewModel = authViewModel,
                    driveViewModel = driveViewModel,
                )
            }
        }
    }
}
