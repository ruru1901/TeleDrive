package com.teledrive.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teledrive.android.secure.SecureSettings
import com.teledrive.android.telegram.AuthState
import com.teledrive.android.telegram.TelegramGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val gateway: TelegramGateway,
    private val secureSettings: SecureSettings,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AuthUiState> = combine(
        gateway.authState,
        busy,
        message,
    ) { authState, isBusy, msg ->
        AuthUiState(authState = authState, busy = isBusy, message = msg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthUiState(),
    )

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { secureSettings.apiCredentials() }?.let { credentials ->
                runStep {
                    gateway.configure(credentials.apiId, credentials.apiHash)
                }
            }
        }

        viewModelScope.launch {
            gateway.authState.collectLatest { state ->
                if (state == AuthState.Ready) {
                    withContext(Dispatchers.IO) {
                        secureSettings.setHasCompletedLogin(true)
                    }
                }
            }
        }
    }

    fun submitApi(apiIdText: String, apiHash: String) {
        viewModelScope.launch {
            runStep {
                val apiId = apiIdText.toIntOrNull() ?: error("API ID must be a number.")
                withContext(Dispatchers.IO) { secureSettings.saveApiCredentials(apiId, apiHash.trim()) }
                gateway.configure(apiId, apiHash.trim())
            }
        }
    }

    fun submitPhone(phone: String) {
        viewModelScope.launch {
            runStep { gateway.submitPhone(phone.trim()) }
        }
    }

    fun submitCode(code: String) {
        viewModelScope.launch {
            runStep { gateway.submitCode(code.trim()) }
        }
    }

    fun submitPassword(password: String) {
        viewModelScope.launch {
            runStep { gateway.submitPassword(password) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runStep {
                gateway.logout()
                withContext(Dispatchers.IO) { secureSettings.clear() }
            }
        }
    }

    private suspend fun runStep(block: suspend () -> Unit) {
        busy.value = true
        message.value = null
        try {
            block()
        } catch (throwable: Throwable) {
            message.value = throwable.message ?: "Something went wrong."
        } finally {
            busy.value = false
        }
    }
}

data class AuthUiState(
    val authState: AuthState = AuthState.NeedsApiCredentials,
    val busy: Boolean = false,
    val message: String? = null,
)
