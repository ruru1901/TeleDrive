package com.teledrive.android.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.teledrive.android.MasterPasswordService
import kotlinx.coroutines.launch

@Composable
fun SetMasterPasswordFragment(
    masterPasswordService: MasterPasswordService?,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Set Master Password") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "This password encrypts your file keys and backs them up to your Telegram Saved Messages. If you forget it, encrypted files cannot be recovered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("New master password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it; error = null },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        password.isBlank() -> error = "Password is required"
                        password.length < 8 -> error = "Password must be at least 8 characters"
                        password != confirm -> error = "Passwords do not match"
                        else -> {
                            busy = true
                            scope.launch {
                                try {
                                    masterPasswordService?.setMasterPassword(password)
                                    onDismiss()
                                } catch (e: Exception) {
                                    error = e.message ?: "Failed to set password"
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    }
                },
                enabled = !busy,
            ) {
                Text("Set password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Cancel")
            }
        },
    )
}
