package com.teledrive.android.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import kotlin.math.sin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.teledrive.android.data.BackupFolder
import com.teledrive.android.data.BackupMode
import com.teledrive.android.data.BackupScope
import com.teledrive.android.data.BackupSettingsEntity
import com.teledrive.android.data.FileEntity
import com.teledrive.android.data.FolderEntity
import com.teledrive.android.data.TransferEntity
import com.teledrive.android.data.TransferStatus
import com.teledrive.android.data.SyncStatus
import com.teledrive.android.telegram.AuthState
import com.teledrive.android.ui.auth.AuthUiState
import com.teledrive.android.ui.auth.AuthViewModel
import com.teledrive.android.ui.drive.DriveUiState
import com.teledrive.android.ui.drive.DriveViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

@Composable
fun TeleDriveApp(
    authViewModel: AuthViewModel,
    driveViewModel: DriveViewModel,
) {
    var darkMode by remember { mutableStateOf(true) }
    var pendingDarkMode by remember { mutableStateOf<Boolean?>(null) }
    var themeChangeNonce by remember { mutableStateOf(0) }
    var themeWaveOrigin by remember { mutableStateOf<Offset?>(null) }
    val authState by authViewModel.uiState.collectAsState()
    val driveState by driveViewModel.uiState.collectAsState()
    val toggleTheme: () -> Unit = {
        pendingDarkMode = !(pendingDarkMode ?: darkMode)
        themeChangeNonce += 1
    }

    TeleDriveTheme(darkMode = darkMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                when (authState.authState) {
                    AuthState.Ready -> DashboardScreen(
                        state = driveState,
                        darkMode = darkMode,
                        onToggleDark = toggleTheme,
                        onThemeOriginChange = { themeWaveOrigin = it },
                        onRefresh = driveViewModel::refresh,
                        onSelectFolder = driveViewModel::selectFolder,
                        onCreateFolder = driveViewModel::createFolder,
                        onDeleteFolder = driveViewModel::deleteFolder,
                        onQueryChange = driveViewModel::setQuery,
                        onUpload = driveViewModel::upload,
                        onDownload = driveViewModel::download,
                        onDownloadTo = driveViewModel::downloadTo,
                        onDeleteFile = driveViewModel::deleteFile,
                        onDeleteFiles = driveViewModel::deleteFiles,
                        onMoveFiles = driveViewModel::moveFiles,
                        onUpdateBackupSettings = driveViewModel::updateBackupSettings,
                        onSetDownloadDestination = driveViewModel::setDownloadDestination,
                        onClearDownloadDestination = driveViewModel::clearDownloadDestination,
                        onRecordBackupRun = driveViewModel::recordBackupRun,
                        onRestoreAll = driveViewModel::restoreAllFiles,
                        onLogout = authViewModel::logout,
                    )
                    AuthState.Initializing -> InitializingScreen()
                    else -> AuthScreen(
                        state = authState,
                        darkMode = darkMode,
                        onToggleDark = toggleTheme,
                        onThemeOriginChange = { themeWaveOrigin = it },
                        onSubmitApi = authViewModel::submitApi,
                        onSubmitPhone = authViewModel::submitPhone,
                        onSubmitCode = authViewModel::submitCode,
                        onSubmitPassword = authViewModel::submitPassword,
                    )
                }
                ThemeWaveOverlay(
                    nonce = themeChangeNonce,
                    targetDarkMode = pendingDarkMode ?: darkMode,
                    origin = themeWaveOrigin,
                    onComplete = {
                        pendingDarkMode?.let { darkMode = it }
                        pendingDarkMode = null
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeWaveOverlay(
    nonce: Int,
    targetDarkMode: Boolean,
    origin: Offset?,
    onComplete: () -> Unit,
) {
    val waveProgress = remember { Animatable(0f) }
    LaunchedEffect(nonce) {
        if (nonce == 0) return@LaunchedEffect
        waveProgress.snapTo(0f)
        waveProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        )
        onComplete()
        waveProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        )
    }
    if (nonce > 0 && waveProgress.value > 0f) {
        val overlayColor = if (targetDarkMode) Color(0xFF111316) else Color(0xFFFFFFFF)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val origin = origin ?: Offset(size.width - 32.dp.toPx(), 32.dp.toPx())
            fun distanceTo(x: Float, y: Float): Float =
                hypot((x - origin.x).toDouble(), (y - origin.y).toDouble()).toFloat()
            val maxRadius = maxOf(
                distanceTo(0f, 0f),
                distanceTo(size.width, 0f),
                distanceTo(0f, size.height),
                distanceTo(size.width, size.height),
            )
            drawCircle(
                color = overlayColor,
                radius = maxRadius * waveProgress.value,
                center = origin,
                alpha = if (waveProgress.value < 1f) 0.94f else 1f,
            )
        }
    }
}

@Composable
private fun TeleDriveLogo(
    markColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val outline = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.10f)
            lineTo(size.width * 0.90f, size.height * 0.78f)
            lineTo(size.width * 0.18f, size.height * 0.78f)
            close()
        }
        drawPath(outline, color = markColor, style = stroke)
        drawLine(
            color = markColor.copy(alpha = 0.72f),
            start = Offset(size.width * 0.36f, size.height * 0.78f),
            end = Offset(size.width * 0.61f, size.height * 0.34f),
            strokeWidth = size.minDimension * 0.075f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = markColor.copy(alpha = 0.72f),
            start = Offset(size.width * 0.64f, size.height * 0.78f),
            end = Offset(size.width * 0.39f, size.height * 0.34f),
            strokeWidth = size.minDimension * 0.075f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun PhoneNumberField(
    phone: String,
    selectedCountry: CountryDial,
    onCountryChange: (CountryDial) -> Unit,
    onPhoneChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Phone number") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text("${selectedCountry.flag} ${selectedCountry.dialCode}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    CountryDial.options.forEach { country ->
                        DropdownMenuItem(
                            text = { Text("${country.flag} ${country.name} ${country.dialCode}") },
                            onClick = {
                                expanded = false
                                onCountryChange(country)
                            },
                        )
                    }
                }
            }
        },
    )
}

private data class CountryDial(
    val name: String,
    val flag: String,
    val dialCode: String,
) {
    companion object {
        val options = listOf(
            CountryDial("India", "IN", "+91"),
            CountryDial("United States", "US", "+1"),
            CountryDial("United Kingdom", "GB", "+44"),
            CountryDial("United Arab Emirates", "AE", "+971"),
            CountryDial("Singapore", "SG", "+65"),
            CountryDial("Australia", "AU", "+61"),
            CountryDial("Canada", "CA", "+1"),
            CountryDial("Germany", "DE", "+49"),
        )
        val default = options.first()

        fun match(phone: String): CountryDial? =
            options
                .filter { phone.startsWith(it.dialCode) }
                .maxByOrNull { it.dialCode.length }
    }
}

@Composable
private fun InitializingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Text("Connecting to Telegram...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun AuthScreen(
    state: AuthUiState,
    darkMode: Boolean,
    onToggleDark: () -> Unit,
    onThemeOriginChange: (Offset) -> Unit,
    onSubmitApi: (String, String) -> Unit,
    onSubmitPhone: (String) -> Unit,
    onSubmitCode: (String) -> Unit,
    onSubmitPassword: (String) -> Unit,
) {
    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(CountryDial.default) }
    var phone by remember { mutableStateOf(selectedCountry.dialCode) }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        IconButton(
            onClick = onToggleDark,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    onThemeOriginChange(
                        Offset(
                            position.x + coordinates.size.width / 2f,
                            position.y + coordinates.size.height / 2f,
                        ),
                    )
                },
        ) {
            Icon(
                if (darkMode) Icons.Default.WbSunny else Icons.Default.DarkMode,
                contentDescription = "Toggle dark mode",
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                TeleDriveLogo(markColor = Color.White, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("TeleDrive", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(
                "Your Telegram account as a private drive",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (val st = state.authState) {
                        AuthState.NeedsApiCredentials -> {
                            Text("Connect your account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Get your API ID and hash from my.telegram.org/apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            OutlinedTextField(value = apiId, onValueChange = { apiId = it }, label = { Text("API ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = apiHash, onValueChange = { apiHash = it }, label = { Text("API Hash") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { onSubmitApi(apiId, apiHash) }, enabled = !state.busy && apiId.isNotBlank() && apiHash.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                Text("Continue", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        AuthState.NeedsPhone -> {
                            Text("Enter your phone number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Pick a country or type the dial code; both stay in sync.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            PhoneNumberField(
                                phone = phone,
                                selectedCountry = selectedCountry,
                                onCountryChange = { country ->
                                    selectedCountry = country
                                    phone = country.dialCode
                                },
                                onPhoneChange = { value ->
                                    phone = value
                                    CountryDial.match(value)?.let { selectedCountry = it }
                                },
                            )
                            Button(onClick = { onSubmitPhone(phone) }, enabled = !state.busy && phone.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                Text("Send code", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        AuthState.NeedsCode -> {
                            Text("Check your Telegram app", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Enter the login code Telegram sent you", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Login code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { onSubmitCode(code) }, enabled = !state.busy && code.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                Text("Sign in", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        AuthState.NeedsPassword -> {
                            Text("Two-step verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Enter your 2FA password", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                            Button(onClick = { onSubmitPassword(password) }, enabled = !state.busy && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                Text("Unlock", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        is AuthState.Error -> {
                            Text("Something went wrong", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            Text(st.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        else -> Unit
                    }

                    if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    state: DriveUiState,
    darkMode: Boolean,
    onToggleDark: () -> Unit,
    onThemeOriginChange: (Offset) -> Unit,
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
) {
    var showFolders by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showUploadMenu by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<FileEntity?>(null) }
    var downloadOptionsFile by remember { mutableStateOf<FileEntity?>(null) }
    var folderPendingDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var fileTab by remember { mutableStateOf(FileTab.AllFiles) }
    var sortOption by remember { mutableStateOf(FileSort.NameAsc) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val name = it.lastPathSegment?.substringAfterLast('/') ?: "file"
            onUpload(it, name)
        }
    }
    val downloadFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onSetDownloadDestination)
    }
    val oneTimeDownloadFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val file = downloadOptionsFile
        if (uri != null && file != null) {
            onDownloadTo(file, uri)
            downloadOptionsFile = null
        }
    }

    val displayedFiles by remember(state.files, fileTab, sortOption) {
        derivedStateOf {
            state.files
                .filter { fileTab.matches(it) }
                .sortedWith(sortOption.comparator)
        }
    }
    val context = LocalContext.current
    val categories = remember(state.files) { buildCategories(state.files) }
    val storageStats = rememberStorageStats()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationFooter(
                selected = fileTab,
                onSelect = { fileTab = it },
            )
        },
        floatingActionButton = {
            Box(contentAlignment = Alignment.BottomEnd) {
                if (showUploadMenu) {
                    UploadActionMenu(
                        onUploadFiles = {
                            showUploadMenu = false
                            picker.launch(arrayOf("*/*"))
                        },
                        onSyncAll = {
                            showUploadMenu = false
                            showBackupSheet = true
                        },
                    )
                }
                IconButton(
                    onClick = { showUploadMenu = !showUploadMenu },
                    modifier = Modifier
                        .padding(bottom = 72.dp)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                DashboardHeader(
                    onLogout = onLogout,
                    onToggleDark = onToggleDark,
                    darkMode = darkMode,
                    onThemeOriginChange = onThemeOriginChange,
                )

                if (state.busy && state.files.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            // Search and Folder context
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(22.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { showFolders = true }) {
                                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                currentFolderName(state.activeFolderId, state.folders),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = onRefresh) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    OutlinedTextField(
                                        value = state.query,
                                        onValueChange = onQueryChange,
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Search in TeleDrive") },
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = TextFieldDefaults.colors(
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }
                        item {
                            CategorySection(
                                categories = categories,
                                onOpenTab = { fileTab = it },
                            )
                        }
                        item {
                            BackupStatusCard(
                                settings = state.backupSettings,
                                storageStats = storageStats,
                                onOpen = { showBackupSheet = true },
                            )
                        }
                        item {
                            FilesSectionHeader(
                                sortOption = sortOption,
                                onSortOptionChange = { sortOption = it },
                            )
                        }
                        if (state.message != null) {
                            item {
                                Text(
                                    state.message ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            }
                        }
                        if (displayedFiles.isEmpty()) {
                            item {
                                EmptyDrive(onUpload = { picker.launch(arrayOf("*/*")) })
                            }
                        } else {
                            itemsIndexed(
                                items = displayedFiles,
                                key = { _, file -> "${fileTab.name}_${file.messageId}" }
                            ) { index, file ->
                                val selected = file.messageId in selectedIds
                                FileListRow(
                                    file = file,
                                    selected = selected,
                                    showDivider = index != displayedFiles.lastIndex,
                                    onPreview = {
                                        val localFile = file.localCachePath?.let(::File)
                                        if (localFile != null && localFile.exists()) {
                                            if (isInAppMedia(file)) {
                                                previewFile = file
                                            } else if (isTextFile(file)) {
                                                previewFile = file
                                            } else {
                                                openFileWithSystem(context, file, localFile)
                                            }
                                        } else {
                                            previewFile = file
                                            onDownload(file)
                                        }
                                    },
                                    onToggleSelection = {
                                        selectedIds = if (file.messageId in selectedIds) {
                                            selectedIds - file.messageId
                                        } else {
                                            selectedIds + file.messageId
                                        }
                                    },
                                    onDownload = { downloadOptionsFile = file },
                                    onDelete = { onDeleteFile(file) },
                                )
                            }
                        }
                        if (state.transfers.isNotEmpty()) {
                            item {
                                TransferStrip(state.transfers)
                            }
                        }
                        item {
                            Spacer(Modifier.height(84.dp))
                        }
                    }
                }
            }

            if (selectedIds.isNotEmpty()) {
                BulkSelectionBar(
                    count = selectedIds.size,
                    onMove = { showMoveSheet = true },
                    onDelete = { showBulkDeleteConfirm = true },
                    onClear = { selectedIds = emptySet() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 92.dp),
                )
            }

            AnimatedVisibility(
                visible = state.notificationMessage != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 100.dp)
            ) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            state.notificationMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    if (showFolders) {
        FolderSheet(
            folders = state.folders,
            activeFolderId = state.activeFolderId,
            onDismiss = { showFolders = false },
            onSelect = {
                onSelectFolder(it)
                showFolders = false
            },
            onCreate = { showCreateFolder = true },
            onDelete = {
                folderPendingDelete = it
                showFolders = false
            },
        )
    }
    if (showCreateFolder) {
        CreateFolderDialog(
            onDismiss = { showCreateFolder = false },
            onCreate = {
                onCreateFolder(it)
                showCreateFolder = false
            },
        )
    }
    if (showMoveSheet) {
        MoveToFolderSheet(
            folders = state.folders,
            activeFolderId = state.activeFolderId,
            onDismiss = { showMoveSheet = false },
            onMove = { targetId ->
                val sel = state.files.filter { it.messageId in selectedIds }
                onMoveFiles(sel, targetId)
                selectedIds = emptySet()
                showMoveSheet = false
            },
        )
    }
    if (showBulkDeleteConfirm) {
        val sel = state.files.filter { it.messageId in selectedIds }
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete ${sel.size} files?") },
            text = { Text("This removes the selected files from Telegram permanently.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteFiles(sel)
                    selectedIds = emptySet()
                    showBulkDeleteConfirm = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
    folderPendingDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderPendingDelete = null },
            title = { Text("Delete '${folder.name}'?") },
            text = { Text("This deletes the Telegram channel used for this folder.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteFolder(folder.id)
                    folderPendingDelete = null
                }) { Text("Delete folder") }
            },
            dismissButton = { TextButton(onClick = { folderPendingDelete = null }) { Text("Cancel") } },
        )
    }
    if (showBackupSheet) {
        BackupSettingsSheet(
            settings = state.backupSettings,
            onDismiss = { showBackupSheet = false },
            onUpdate = onUpdateBackupSettings,
            downloadDestinationLabel = state.downloadDestinationLabel,
            onChooseDownloadFolder = { downloadFolderPicker.launch(null) },
            onClearDownloadFolder = onClearDownloadDestination,
            onRunBackupNow = {
                onRecordBackupRun()
                showBackupSheet = false
            },
            onRestoreAll = {
                onRestoreAll()
                showBackupSheet = false
            },
        )
    }
    downloadOptionsFile?.let { file ->
        DownloadOptionsSheet(
            file = file,
            destinationLabel = state.downloadDestinationLabel,
            onDismiss = { downloadOptionsFile = null },
            onDownloadDefault = {
                onDownload(file)
                downloadOptionsFile = null
            },
            onPickFolder = { oneTimeDownloadFolderPicker.launch(null) },
        )
    }
    previewFile?.let { file ->
        val freshFile = state.files.firstOrNull { it.messageId == file.messageId } ?: file
        val transfer = state.transfers.find { it.messageId == freshFile.messageId && (it.status == TransferStatus.Running || it.status == TransferStatus.Pending) }
        PreviewDialog(file = freshFile, transfer = transfer, onDismiss = { previewFile = null })
    }

    LaunchedEffect(state.activeFolderId) { selectedIds = emptySet() }
}

@Composable
private fun DashboardHeader(
    darkMode: Boolean,
    state: DriveUiState,
    onToggleDark: () -> Unit,
    onThemeOriginChange: (Offset) -> Unit,
    onOpenFolders: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconButton(
                        onClick = onOpenFolders,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                    ) {
                        TeleDriveLogo(markColor = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text(
                            currentFolderName(state.activeFolderId, state.folders),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${state.files.size} files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = onToggleDark,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInRoot()
                        onThemeOriginChange(
                            Offset(
                                position.x + coordinates.size.width / 2f,
                                position.y + coordinates.size.height / 2f,
                            ),
                        )
                    },
                ) {
                    Icon(if (darkMode) Icons.Default.WbSunny else Icons.Default.DarkMode, contentDescription = "Toggle theme")
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout")
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search in TeleDrive") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(22.dp),
            )
        }
    }
}

@Composable
private fun CategorySection(
    categories: List<CategoryCardModel>,
    onOpenTab: (FileTab) -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            categories.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { category ->
                        CategoryCard(
                            model = category,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenTab(category.tab) },
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    model: CategoryCardModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(model.tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(model.icon, contentDescription = null, tint = model.tint, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(model.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    Text(model.countLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PreviewThumbnailStrip(files = model.previewFiles, tint = model.tint)
        }
    }
}

@Composable
private fun PreviewThumbnailStrip(
    files: List<FileEntity>,
    tint: Color,
) {
    if (files.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tint.copy(alpha = 0.10f)),
                        )
                    }
                }
            }
        }
        return
    }
    val previewItems = files.take(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        previewItems.chunked(2).forEach { rowFiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowFiles.forEach { file ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        FileContentThumbnail(
                            file = file,
                            modifier = Modifier.fillMaxSize(),
                            tint = tint,
                            compact = true,
                        )
                    }
                }
                if (rowFiles.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BackupStatusCard(
    settings: BackupSettingsEntity,
    storageStats: StorageStats,
    onOpen: () -> Unit,
) {
    ElevatedCard(
        onClick = onOpen,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.SdStorage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Device Storage Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Total: ${formatBytes(storageStats.totalBytes)}, Used: ${formatBytes(storageStats.usedBytes)}, Free: ${formatBytes(storageStats.freeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val backupModeLabel = when (settings.mode) {
                    BackupMode.OneWay -> "One-way backup"
                    BackupMode.TwoWay -> "Two-way sync"
                }
                Text(
                    "Last backup: ${formatRelativeTime(settings.lastBackupAt)} • $backupModeLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilesSectionHeader(
    sortOption: FileSort,
    onSortOptionChange: (FileSort) -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("All Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FileSort.entries.forEach { option ->
                    FilterChip(
                        selected = sortOption == option,
                        onClick = { onSortOptionChange(option) },
                        label = { Text(option.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FileContentThumbnail(
    file: FileEntity,
    modifier: Modifier = Modifier,
    tint: Color = fileTint(file),
    compact: Boolean = false,
) {
    val localFile = remember(file.localCachePath) { file.localCachePath?.let(::File) }
    val imageBitmap = remember(file.thumbnailBase64) {
        file.thumbnailBase64?.let { encoded ->
            runCatching {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 10.dp else 14.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            imageBitmap != null -> {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            localFile != null && localFile.exists() && file.mimeType?.startsWith("image/") == true -> {
                AsyncImage(
                    model = localFile,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            isTextFile(file) && localFile != null && localFile.exists() -> {
                val snippet by produceState<String?>(initialValue = null, localFile.absolutePath) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            localFile.readText()
                                .lineSequence()
                                .filter { it.isNotBlank() }
                                .take(if (compact) 3 else 5)
                                .joinToString("\n")
                                .take(if (compact) 90 else 180)
                        }.getOrNull()
                    }
                }
                TextPreviewThumb(snippet = snippet, tint = tint, compact = compact)
            }
            else -> TypePreviewThumb(file = file, tint = tint, compact = compact)
        }
    }
}

@Composable
private fun TextPreviewThumb(snippet: String?, tint: Color, compact: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(if (compact) 4.dp else 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            snippet?.takeIf { it.isNotBlank() } ?: "TXT",
            maxLines = if (compact) 3 else 4,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 6.sp else 7.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.56f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tint.copy(alpha = 0.7f)),
        )
    }
}

@Composable
private fun TypePreviewThumb(file: FileEntity, tint: Color, compact: Boolean) {
    val ext = file.extension.orEmpty().uppercase(Locale.getDefault()).take(4).ifBlank {
        displayFileType(file).substringBefore(' ').uppercase(Locale.getDefault()).take(4)
    }
    val lineCount = when {
        file.extension.orEmpty().lowercase() in setOf("xls", "xlsx", "csv") -> 4
        file.extension.orEmpty().lowercase() in setOf("zip", "rar", "7z") -> 2
        file.mimeType?.startsWith("audio/") == true || isAudio(file) -> 3
        file.mimeType?.startsWith("video/") == true -> 1
        else -> 3
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(if (compact) 5.dp else 7.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            fileIcon(file),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(if (compact) 16.dp else 20.dp),
        )
        if (!compact) Spacer(Modifier.height(3.dp))
        repeat(lineCount) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index % 2 == 0) 0.74f else 0.52f)
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(tint.copy(alpha = 0.32f)),
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            ext,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 7.sp else 8.sp),
            color = tint,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FileListRow(
    file: FileEntity,
    selected: Boolean,
    showDivider: Boolean,
    onPreview: () -> Unit,
    onToggleSelection: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .combinedClickable(
                onClick = onPreview,
                onLongClick = onToggleSelection,
            )
            .padding(horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(fileTint(file).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                FileContentThumbnail(
                    file = file,
                    modifier = Modifier.fillMaxSize(),
                    tint = fileTint(file),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(6.dp))
                    SyncStatusIcon(file.syncStatus)
                }
                Text(
                    "${formatBytes(file.size)} • ${formatDate(file.createdAt)} • ${sourceLabel(file)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.CloudDownload, contentDescription = "Download")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
        if (showDivider) {
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun SyncStatusIcon(status: SyncStatus) {
    val icon = when (status) {
        SyncStatus.LocalOnly -> Icons.Default.CloudOff
        SyncStatus.CloudOnly -> Icons.Default.Cloud
        SyncStatus.Synced -> Icons.Outlined.CheckCircle
    }
    val tint = when (status) {
        SyncStatus.LocalOnly -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        SyncStatus.CloudOnly -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        SyncStatus.Synced -> MaterialTheme.colorScheme.secondary
    }
    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
}

@Composable
private fun BulkSelectionBar(
    count: Int,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$count selected", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onMove) {
                Icon(Icons.Default.DriveFileMove, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Move")
            }
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Delete")
            }
            TextButton(onClick = onClear) { Text("Clear") }
        }
    }
}

@Composable
private fun NavigationFooter(
    selected: FileTab,
    onSelect: (FileTab) -> Unit,
) {
    Surface(shadowElevation = 8.dp, tonalElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            FileTab.entries.forEach { tab ->
                val active = selected == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(tab.label, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun UploadActionMenu(
    onUploadFiles: () -> Unit,
    onSyncAll: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .padding(bottom = 144.dp, end = 8.dp)
            .width(190.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 14.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            ActionMenuRow(icon = Icons.Default.UploadFile, label = "Upload files", onClick = onUploadFiles)
            ActionMenuRow(icon = Icons.Default.Sync, label = "Sync all", onClick = onSyncAll)
        }
    }
}

@Composable
private fun ActionMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null)
            Text(label)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BackupSettingsSheet(
    settings: BackupSettingsEntity,
    onDismiss: () -> Unit,
    onUpdate: (BackupSettingsEntity) -> Unit,
    downloadDestinationLabel: String,
    onChooseDownloadFolder: () -> Unit,
    onClearDownloadFolder: () -> Unit,
    onRunBackupNow: () -> Unit,
    onRestoreAll: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Device Storage Backup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            SettingSwitchRow(
                title = "Enable backup",
                subtitle = "Allow TeleDrive to manage automatic backups",
                checked = settings.enabled,
                onCheckedChange = { onUpdate(settings.copy(enabled = it)) },
            )
            SectionTitle("Backup scope")
            ChoiceChips(
                options = BackupScope.entries.map { it to it.label },
                selected = settings.scope,
                onSelect = { onUpdate(settings.copy(scope = it)) },
            )

            SectionTitle("Backup mode")
            ChoiceChips(
                options = BackupMode.entries.map { it to it.label },
                selected = settings.mode,
                onSelect = { onUpdate(settings.copy(mode = it)) },
            )

            SectionTitle("Folders to back up")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BackupFolder.entries.forEach { folder ->
                    FilterChip(
                        selected = folder in settings.folders,
                        onClick = {
                            val next = settings.folders.toMutableSet().apply {
                                if (contains(folder)) remove(folder) else add(folder)
                            }
                            onUpdate(settings.copy(folders = next))
                        },
                        label = { Text(folder.label) },
                        leadingIcon = {
                            Icon(backupFolderIcon(folder), contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                    )
                }
            }

            SectionTitle("Schedule")
            SettingSwitchRow(
                title = "Wi-Fi only",
                subtitle = "Avoid mobile data for automatic backup",
                checked = settings.wifiOnly,
                onCheckedChange = { onUpdate(settings.copy(wifiOnly = it)) },
                icon = Icons.Outlined.Wifi,
            )
            SettingSwitchRow(
                title = "Only while charging",
                subtitle = "Keep large backup jobs off battery",
                checked = settings.chargingOnly,
                onCheckedChange = { onUpdate(settings.copy(chargingOnly = it)) },
                icon = Icons.Outlined.Bolt,
            )
            SettingSwitchRow(
                title = "Instant when new files appear",
                subtitle = "Try to back up newly detected files as they arrive",
                checked = settings.instantBackup,
                onCheckedChange = { onUpdate(settings.copy(instantBackup = it)) },
                icon = Icons.Outlined.CheckCircle,
            )
            SettingSwitchRow(
                title = "Daily backup sweep",
                subtitle = "Run a broader periodic scan for missed files",
                checked = settings.dailyBackup,
                onCheckedChange = { onUpdate(settings.copy(dailyBackup = it)) },
                icon = Icons.Outlined.Collections,
            )

            SectionTitle("Downloads")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Save downloads to", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(downloadDestinationLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onClearDownloadFolder, modifier = Modifier.weight(1f)) {
                    Text("Use default")
                }
                Button(onClick = onChooseDownloadFolder, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose")
                }
            }

            SectionTitle("Restore")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onRestoreAll, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restore all")
                }
                Button(onClick = onRunBackupNow, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sync now")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector = Icons.Default.Settings,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderSheet(
    folders: List<FolderEntity>,
    activeFolderId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit,
    onCreate: () -> Unit,
    onDelete: (FolderEntity) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text("Folders", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ListItem(headlineContent = { Text("TeleDrive") }, supportingContent = { Text("Root drive location") }, leadingContent = { Icon(Icons.Default.Cloud, null) }, trailingContent = { if (activeFolderId == null) Text("Active", style = MaterialTheme.typography.labelSmall) })
            TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) { Text("Open TeleDrive") }
            folders.forEach { folder ->
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(folder.name) },
                    supportingContent = { Text("Telegram channel") },
                    leadingContent = { Icon(Icons.Default.Folder, null) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (activeFolderId == folder.id) Text("Active", style = MaterialTheme.typography.labelSmall)
                            IconButton(onClick = { onDelete(folder) }) { Icon(Icons.Default.Delete, "Delete folder") }
                        }
                    },
                )
                TextButton(onClick = { onSelect(folder.id) }, modifier = Modifier.fillMaxWidth()) { Text("Open ${folder.name}") }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("New folder", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToFolderSheet(
    folders: List<FolderEntity>,
    activeFolderId: Long?,
    onDismiss: () -> Unit,
    onMove: (Long?) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text("Move to...", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { onMove(null) }, enabled = activeFolderId != null, modifier = Modifier.fillMaxWidth()) { Text("TeleDrive (root)") }
            folders.forEach { folder ->
                TextButton(onClick = { onMove(folder.id) }, enabled = activeFolderId != folder.id, modifier = Modifier.fillMaxWidth()) { Text(folder.name) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadOptionsSheet(
    file: FileEntity,
    destinationLabel: String,
    onDismiss: () -> Unit,
    onDownloadDefault: () -> Unit,
    onPickFolder: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Current destination: $destinationLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onDownloadDefault, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Download here")
            }
            OutlinedButton(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose folder")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CreateFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Folder name") }, singleLine = true) },
        confirmButton = { Button(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PreviewDialog(file: FileEntity, transfer: TransferEntity?, onDismiss: () -> Unit) {
    val isMedia = file.mimeType?.startsWith("image/") == true || file.mimeType?.startsWith("video/") == true
    val isText = isTextFile(file)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !isMedia),
    ) {
        if (isMedia) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (file.localCachePath != null && File(file.localCachePath).exists()) {
                    if (file.mimeType?.startsWith("video/") == true) {
                        val context = LocalContext.current
                        val exoPlayer = remember {
                            ExoPlayer.Builder(context).build().apply {
                                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(file.localCachePath))))
                                prepare()
                                playWhenReady = true
                            }
                        }
                        DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
                        AndroidView(
                            factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer } },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        AsyncImage(
                            model = File(file.localCachePath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                } else if (file.thumbnailBase64 != null) {
                    val bitmap = remember(file.thumbnailBase64) {
                        try {
                            val bytes = Base64.decode(file.thumbnailBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (transfer != null) {
                                CircularProgressIndicator(color = Color.White)
                                Text("Downloading ${transfer.progress}%", color = Color.White)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                                Text("Waiting to download...", color = Color.White)
                            }
                        }
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        } else if (isText && file.localCachePath != null && File(file.localCachePath).exists()) {
            Card {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val text = remember(file.localCachePath) {
                        runCatching { File(file.localCachePath).readText().take(12_000) }.getOrElse { "Unable to read text file." }
                    }
                    Text(
                        text,
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
                }
            }
        } else {
            Card {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(file.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FileContentThumbnail(
                        file = file,
                        modifier = Modifier
                            .size(112.dp)
                            .align(Alignment.CenterHorizontally),
                        tint = fileTint(file),
                    )
                    Text("Type: ${displayFileType(file)}", style = MaterialTheme.typography.bodySmall)
                    Text("Size: ${formatBytes(file.size)}", style = MaterialTheme.typography.bodySmall)
                    Text("Created: ${formatDate(file.createdAt)}", style = MaterialTheme.typography.bodySmall)
                    if (transfer != null) {
                        LinearProgressIndicator(progress = { transfer.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    }
                    Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun EmptyDrive(onUpload: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                Text("No files yet", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text("Upload your first file to get started", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                Button(onClick = onUpload) {
                    Icon(Icons.Default.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload file")
                }
            }
        }
    }
}

@Composable
private fun TransferStrip(transfers: List<TransferEntity>) {
    val running = transfers.filter { it.status == TransferStatus.Running || it.status == TransferStatus.Pending }
    if (running.isEmpty()) return
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Transfers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            running.forEach { transfer ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(transfer.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    LinearProgressIndicator(progress = { transfer.progress / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun rememberStorageStats(): StorageStats {
    return remember {
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val total = stat.totalBytes
        val free = stat.availableBytes
        StorageStats(totalBytes = total, usedBytes = total - free, freeBytes = free)
    }
}

private fun buildCategories(files: List<FileEntity>): List<CategoryCardModel> {
    val photoFiles = mutableListOf<FileEntity>()
    val videoFiles = mutableListOf<FileEntity>()
    val genericFiles = mutableListOf<FileEntity>()
    val audioFiles = mutableListOf<FileEntity>()

    files.forEach { file ->
        when {
            fileTypeGroup(file) == FileTab.Photos -> photoFiles.add(file)
            fileTypeGroup(file) == FileTab.Videos -> videoFiles.add(file)
            isAudio(file) -> audioFiles.add(file)
            else -> genericFiles.add(file)
        }
    }

    return listOf(
        CategoryCardModel("Photos", "${photoFiles.size} files", Icons.Outlined.Image, Color(0xFF5A9BFF), photoFiles, FileTab.Photos),
        CategoryCardModel("Videos", "${videoFiles.size} videos", Icons.Outlined.VideoLibrary, Color(0xFFB367FF), videoFiles, FileTab.Videos),
        CategoryCardModel("Files", "${genericFiles.size} files", Icons.Outlined.InsertDriveFile, Color(0xFF6CC28A), genericFiles, FileTab.Files),
        CategoryCardModel("Audio", "${audioFiles.size} files", Icons.Outlined.MusicNote, Color(0xFFF0A24C), audioFiles, FileTab.AllFiles),
    )
}

private fun currentFolderName(activeFolderId: Long?, folders: List<FolderEntity>) =
    if (activeFolderId == null) "TeleDrive" else folders.firstOrNull { it.id == activeFolderId }?.name ?: "Folder"

private fun fileIcon(file: FileEntity): ImageVector {
    val mime = file.mimeType.orEmpty()
    val ext = file.extension.orEmpty().lowercase()
    return when {
        mime.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "gif", "webp") -> Icons.Default.Image
        mime.startsWith("video/") || ext in setOf("mp4", "mkv", "mov") -> Icons.Default.Movie
        ext == "pdf" -> Icons.Default.Description
        mime.startsWith("audio/") || ext in setOf("mp3", "wav", "m4a", "flac", "aac") -> Icons.Default.AudioFile
        else -> Icons.Default.InsertDriveFile
    }
}

private fun fileTypeGroup(file: FileEntity): FileTab = when {
    file.mimeType?.startsWith("image/") == true -> FileTab.Photos
    file.extension.orEmpty().lowercase() in setOf("jpg", "jpeg", "png", "gif", "webp") -> FileTab.Photos
    file.mimeType?.startsWith("video/") == true -> FileTab.Videos
    file.extension.orEmpty().lowercase() in setOf("mp4", "mkv", "mov") -> FileTab.Videos
    else -> FileTab.Files
}

private fun isAudio(file: FileEntity): Boolean {
    val ext = file.extension.orEmpty().lowercase()
    return file.mimeType?.startsWith("audio/") == true || ext in setOf("mp3", "wav", "m4a", "flac", "aac")
}

private fun isTextFile(file: FileEntity): Boolean {
    val ext = file.extension.orEmpty().lowercase()
    val mime = file.mimeType.orEmpty()
    return mime.startsWith("text/") || ext in setOf("txt", "md", "json", "csv", "log", "xml", "kt", "java", "js", "ts")
}

private fun isInAppMedia(file: FileEntity): Boolean =
    file.mimeType?.startsWith("image/") == true || file.mimeType?.startsWith("video/") == true

private fun openFileWithSystem(context: android.content.Context, file: FileEntity, localFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, file.mimeType ?: "application/octet-stream")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Open ${file.name}"))
    }.onFailure {
        Toast.makeText(context, "No app found to open this file.", Toast.LENGTH_SHORT).show()
    }
}

private fun fileTint(file: FileEntity): Color {
    val ext = file.extension.orEmpty().lowercase()
    val mime = file.mimeType.orEmpty()
    return when {
        ext == "pdf" -> Color(0xFFE05252)
        ext in setOf("xls", "xlsx", "csv") -> Color(0xFF2FA96B)
        ext in setOf("doc", "docx", "rtf") -> Color(0xFF4C86E8)
        ext in setOf("zip", "rar", "7z") -> Color(0xFFF0A24C)
        mime.startsWith("image/") -> Color(0xFF5A9BFF)
        mime.startsWith("video/") -> Color(0xFFB367FF)
        mime.startsWith("audio/") || isAudio(file) -> Color(0xFFF0A24C)
        isTextFile(file) -> Color(0xFF6CC28A)
        else -> Color(0xFF8AB4F8)
    }
}

private fun sourceLabel(file: FileEntity): String = when {
    file.folderId == null -> "Desktop Sync"
    isAudio(file) -> "Synced from Mac"
    fileTypeGroup(file) == FileTab.Videos -> "Camera Uploads"
    fileTypeGroup(file) == FileTab.Photos -> "Mobile Upload"
    else -> "Shared Folder"
}

private fun displayFileType(file: FileEntity): String {
    val mime = file.mimeType?.takeIf { it.isNotBlank() }
    if (mime != null) return mime
    val ext = file.extension?.takeIf { it.isNotBlank() }?.uppercase(Locale.getDefault())
    if (ext != null) return "$ext file"
    return "Unknown file type"
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes / 1024.0
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return "%.1f %s".format(Locale.US, value, units[index])
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d", Locale.US)
    return formatter.format(Date(timestamp))
}

private fun formatRelativeTime(timestamp: Long?): String {
    if (timestamp == null) return "Never"
    val minutes = ((System.currentTimeMillis() - timestamp) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} hr ago"
        else -> formatDate(timestamp)
    }
}

private fun backupFolderIcon(folder: BackupFolder): ImageVector = when (folder) {
    BackupFolder.Camera -> Icons.Outlined.CameraAlt
    BackupFolder.Screenshots -> Icons.Outlined.Screenshot
    BackupFolder.Downloads -> Icons.Outlined.Download
    BackupFolder.WhatsAppImages -> Icons.Outlined.PhotoLibrary
    BackupFolder.WhatsAppVideo -> Icons.Outlined.VideoLibrary
    BackupFolder.Documents -> Icons.Outlined.Description
    BackupFolder.CustomFolder -> Icons.Outlined.FolderOpen
}

private data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
)

private data class CategoryCardModel(
    val label: String,
    val countLabel: String,
    val icon: ImageVector,
    val tint: Color,
    val previewFiles: List<FileEntity>,
    val tab: FileTab,
)

private enum class FileTab(val label: String, val icon: ImageVector) {
    AllFiles("All Files", Icons.Outlined.Collections),
    Videos("Videos", Icons.Outlined.VideoLibrary),
    Photos("Photos", Icons.Outlined.PhotoLibrary),
    Files("Files", Icons.Outlined.FolderOpen),
    ;

    fun matches(file: FileEntity): Boolean = when (this) {
        AllFiles -> true
        Videos -> fileTypeGroup(file) == Videos
        Photos -> fileTypeGroup(file) == Photos
        Files -> fileTypeGroup(file) == Files || isAudio(file)
    }
}

private enum class FileSort(val label: String, val comparator: Comparator<FileEntity>) {
    NameAsc("Name A-Z", compareBy { it.name.lowercase(Locale.getDefault()) }),
    NameDesc("Name Z-A", compareByDescending { it.name.lowercase(Locale.getDefault()) }),
    Size("Size", compareByDescending<FileEntity> { it.size }.thenBy { it.name.lowercase(Locale.getDefault()) }),
    Created("Created date", compareByDescending<FileEntity> { it.createdAt }.thenBy { it.name.lowercase(Locale.getDefault()) }),
}

@Composable
private fun DashboardHeader(
    onLogout: () -> Unit,
    onToggleDark: () -> Unit,
    darkMode: Boolean,
    onThemeOriginChange: (Offset) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                TeleDriveLogo(
                    markColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "TeleDrive",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onToggleDark,
                modifier = Modifier.onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    onThemeOriginChange(Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height / 2f))
                }
            ) {
                Icon(
                    if (darkMode) Icons.Default.WbSunny else Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
