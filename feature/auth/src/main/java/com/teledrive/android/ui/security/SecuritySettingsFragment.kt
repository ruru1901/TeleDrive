package com.teledrive.android.ui.security

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teledrive.android.MasterPasswordService
import com.teledrive.android.repository.KeystoreRepository
import kotlinx.coroutines.launch

@Composable
fun SecuritySettingsFragment(
    masterPasswordService: MasterPasswordService? = null,
    keystoreRepository: KeystoreRepository? = null,
) {
    var masterPasswordSet by remember { mutableStateOf<Boolean?>(null) }
    var showSetPassword by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showClearKeys by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val biometricPrefs = context.getSharedPreferences("teledrive_biometric", android.content.Context.MODE_PRIVATE)
    var biometricEnabled by remember { mutableStateOf(biometricPrefs.getBoolean("biometric_enabled", false)) }

    LaunchedEffect(masterPasswordService) {
        masterPasswordSet = masterPasswordService?.isMasterPasswordSet()
    }

    if (showSetPassword) {
        SetMasterPasswordFragment(
            masterPasswordService = masterPasswordService,
            onDismiss = {
                showSetPassword = false
                scope.launch { masterPasswordSet = masterPasswordService?.isMasterPasswordSet() }
            },
        )
    }
    if (showChangePassword) {
        ChangeMasterPasswordFragment(
            masterPasswordService = masterPasswordService,
            onDismiss = {
                showChangePassword = false
                scope.launch { masterPasswordSet = masterPasswordService?.isMasterPasswordSet() }
            },
        )
    }
    if (showClearKeys) {
        AlertDialog(
            onDismissRequest = { if (!clearing) showClearKeys = false },
            title = { Text("Clear Encryption Keys") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This will delete all encryption keys from this device. Encrypted files cannot be decrypted without keys or master password. Are you sure?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (clearing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clearing = true
                        scope.launch {
                            try {
                                keystoreRepository?.clearAll()
                                Toast.makeText(context, "All keys cleared", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {
                            } finally {
                                clearing = false
                                showClearKeys = false
                            }
                        }
                    },
                    enabled = !clearing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearKeys = false }, enabled = !clearing) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Security Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Encryption Keys Section
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Encryption Keys", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()

                // Master Password
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Master Password", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    when (masterPasswordSet) {
                        null -> Text("Checking...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        true -> {
                            Text("Active — tap to change", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            OutlinedButton(onClick = { showChangePassword = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Change Master Password")
                            }
                        }
                        false -> {
                            Text("Not set — tap to configure", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { showSetPassword = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Set Master Password")
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Keystore Backup
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Keystore Backup", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Never", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                syncing = true
                                scope.launch {
                                    try {
                                        masterPasswordService?.syncKeystore()
                                        Toast.makeText(context, "Keystore synced", Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {
                                    } finally {
                                        syncing = false
                                    }
                                }
                            },
                            enabled = !syncing && masterPasswordSet == true,
                        ) {
                            if (syncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Sync now")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val b64 = masterPasswordService?.exportKeystoreBase64()
                                if (b64.isNullOrBlank()) {
                                    Toast.makeText(context, "No keystore to export", Toast.LENGTH_SHORT).show()
                                } else {
                                    clipboard.setText(AnnotatedString(b64))
                                    Toast.makeText(context, "Keystore exported to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = masterPasswordSet == true,
                        ) {
                            Text("Export as Base64")
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val ok = masterPasswordService?.restoreKeystoreFromTelegram() ?: false
                                        if (ok) {
                                            masterPasswordSet = true
                                            Toast.makeText(context, "Keystore restored from Telegram", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No backed up keystore found in Telegram", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Failed to restore keystore", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = masterPasswordSet != null,
                        ) {
                            Text("Restore from Telegram")
                        }
                    }
                }

                HorizontalDivider()

                // Biometric Lock
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Biometric Lock", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Require fingerprint before decrypting files", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                biometricPrefs.edit().putBoolean("biometric_enabled", it).apply()
                            },
                        )
                    }
                }

                HorizontalDivider()

                // Danger Zone
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Danger Zone", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { showClearKeys = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Clear all local keys", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}
