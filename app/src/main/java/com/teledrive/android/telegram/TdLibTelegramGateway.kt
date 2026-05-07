package com.teledrive.android.telegram

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Real Telegram gateway backed by TDLib's Java API.
 *
 * This class intentionally uses reflection so the app can still compile before the generated
 * TDLib Java classes and native `tdjni` binaries are added to the Android project. At runtime it
 * expects the standard package `org.drinkless.tdlib` with `Client` and `TdApi`.
 */
class TdLibTelegramGateway(
    private val context: Context,
    private val databaseEncryptionKey: ByteArray,
    private val reflection: TdLibReflection = TdLibReflection.available(),
) : TelegramGateway {
    private val _authState = MutableStateFlow<AuthState>(AuthState.NeedsApiCredentials)
    override val authState: StateFlow<AuthState> = _authState

    private val tdlibRoot = File(context.filesDir, "tdlib")
    private val uploadCacheDir = File(context.cacheDir, "tdlib_uploads")
    private val downloadPrefs = context.getSharedPreferences("download_destination", Context.MODE_PRIVATE)
    private val _downloadDestinationLabel = MutableStateFlow(readDownloadDestinationLabel())
    override val downloadDestinationLabel: StateFlow<String> = _downloadDestinationLabel
    private val chatTitleCache = mutableMapOf<Long, String>()
    private var client: Any? = null
    private var apiId: Int? = null
    private var apiHash: String? = null
    private var savedMessagesChatId: Long? = null
    // Guard against sending SetTdlibParameters twice (update callback + explicit GetAuthorizationState race)
    @Volatile private var parametersSent = false

    private var isAuthorized = false
    private var isConnectionReady = false

    private data class FileState(
        val fileId: Int,
        val localPath: String? = null,
        val downloadedSize: Long = 0L,
        val expectedSize: Long = 0L,
        val isCompleted: Boolean = false,
    )

    private val fileStates = mutableMapOf<Int, MutableStateFlow<FileState>>()
    private val fileStatesLock = Any()

    override suspend fun configure(apiId: Int, apiHash: String) {
        require(apiId > 0) { "API ID must be positive." }
        require(apiHash.isNotBlank()) { "API hash is required." }
        this.apiId = apiId
        this.apiHash = apiHash
        _authState.value = AuthState.Initializing
        withContext(Dispatchers.IO) {
            runCatching {
                reflection.execute(reflection.newFunction("SetLogVerbosityLevel").also {
                    reflection.setIfPresent(it, "newVerbosityLevel", 1)
                })
            }
        }
        if (client == null) {
            withContext(Dispatchers.IO) { ensureClient() }
        }
        pumpAuthorizationState()
    }

    override suspend fun submitPhone(phone: String) {
        require(phone.isNotBlank()) { "Phone number is required." }
        val function = reflection.newFunction("SetAuthenticationPhoneNumber")
        reflection.setIfPresent(function, "phoneNumber", phone)
        reflection.setIfPresent(function, "settings", reflection.newObjectOrNull("PhoneNumberAuthenticationSettings"))
        send(function)
    }

    override suspend fun submitCode(code: String) {
        require(code.isNotBlank()) { "Login code is required." }
        send(reflection.newFunction("CheckAuthenticationCode").also {
            reflection.setIfPresent(it, "code", code)
        })
    }

    override suspend fun submitPassword(password: String) {
        require(password.isNotBlank()) { "Password is required." }
        send(reflection.newFunction("CheckAuthenticationPassword").also {
            reflection.setIfPresent(it, "password", password)
        })
    }

    override suspend fun logout() {
        send(reflection.newFunction("LogOut"))
        _authState.value = AuthState.NeedsApiCredentials
    }

    override suspend fun scanFolders(): List<TelegramFolder> {
        ensureReady()
        val chats = getChats(limit = 200)
        return chats.mapNotNull { chat ->
            val id = reflection.longField(chat, "id") ?: return@mapNotNull null
            val title = reflection.stringField(chat, "title").orEmpty()
            val about = chatDescription(id)
            chatTitleCache[id] = title
            if (isTeleDriveFolderTitle(title) || isTeleDriveFolderAbout(about)) {
                TelegramFolder(
                    id = id,
                    name = cleanFolderTitle(title),
                    markerSource = if (isTeleDriveFolderTitle(title)) "title" else "about",
                )
            } else {
                null
            }
        }
    }

    override suspend fun createFolder(name: String): TelegramFolder {
        ensureReady()
        require(name.isNotBlank()) { "Folder name is required." }
        val title = "${name.trim()} ${TdLibReadiness.folderTitleMarker}"
        val function = reflection.newFunction("CreateNewSupergroupChat")
        reflection.setIfPresent(function, "title", title)
        reflection.setIfPresent(function, "isForum", false)
        reflection.setIfPresent(function, "isChannel", true)
        reflection.setIfPresent(
            function,
            "description",
            "Telegram Drive Storage Folder\n${TdLibReadiness.folderAboutMarker}",
        )
        reflection.setIfPresent(function, "location", null)
        reflection.setIfPresent(function, "messageAutoDeleteTime", 0)
        reflection.setIfPresent(function, "forImport", false)

        val chat = send(function)
        val chatId = reflection.longField(chat, "id") ?: error("TDLib did not return a folder chat id.")
        chatTitleCache[chatId] = title
        setChatAutoDeleteTime(chatId, 0)
        return TelegramFolder(chatId, name.trim(), "created")
    }

    override suspend fun deleteFolder(folderId: Long) {
        ensureReady()
        runCatching {
            send(reflection.newFunction("DeleteChatHistory").also {
                reflection.setIfPresent(it, "chatId", folderId)
                reflection.setIfPresent(it, "removeFromChatList", true)
                reflection.setIfPresent(it, "revoke", true)
            })
        }
        runCatching {
            send(reflection.newFunction("LeaveChat").also {
                reflection.setIfPresent(it, "chatId", folderId)
            })
        }
        chatTitleCache.remove(folderId)
    }

    override suspend fun listFiles(folderId: Long?): List<TelegramFile> {
        ensureReady()
        val chatId = resolveChatId(folderId)
        val function = reflection.newFunction("GetChatHistory")
        reflection.setIfPresent(function, "chatId", chatId)
        reflection.setIfPresent(function, "fromMessageId", 0L)
        reflection.setIfPresent(function, "offset", 0)
        reflection.setIfPresent(function, "limit", 100)
        reflection.setIfPresent(function, "onlyLocal", false)

        val messages = reflection.field(send(function), "messages") as? Array<*> ?: return emptyList()
        val mapped = messages.mapNotNull { message ->
            if (message == null) null else messageToFile(message, folderId)
        }
        // Thumbnail downloads are now handled asynchronously by the ViewModel.
        return mapped
    }

    override suspend fun searchFiles(query: String): List<TelegramFile> {
        ensureReady()
        if (query.isBlank()) return emptyList()
        val rootFiles = listFiles(null)
        val folderFiles = scanFolders().flatMap { folder -> listFiles(folder.id) }
        return (rootFiles + folderFiles).filter { it.name.contains(query, ignoreCase = true) }
    }

    override fun uploadFile(
        source: Uri,
        displayName: String,
        folderId: Long?,
        backupPath: String?,
    ): Flow<TransferProgress> = flow {
        emit(TransferProgress(0))
        ensureReady()
        val file = copyUriToUploadCache(source, displayName)
        val chatId = resolveChatId(folderId)

        val localFile = reflection.newObject("InputFileLocal").also {
            reflection.setIfPresent(it, "path", file.absolutePath)
        }
        val captionText = if (backupPath != null) {
            "backup/$backupPath"
        } else {
            metadataCaption(displayName, file.length(), context.contentResolver.getType(source))
        }
        val caption = reflection.newObject("FormattedText").also {
            reflection.setIfPresent(it, "text", captionText)
            reflection.setIfPresent(it, "entities", emptyArray<Any>())
        }
        val content = reflection.newObject("InputMessageDocument").also {
            reflection.setIfPresent(it, "document", localFile)
            reflection.setIfPresent(it, "thumbnail", null)
            reflection.setIfPresent(it, "disableContentTypeDetection", false)
            reflection.setIfPresent(it, "caption", caption)
        }
        val function = reflection.newFunction("SendMessage").also {
            reflection.setIfPresent(it, "chatId", chatId)
            reflection.setIfPresent(it, "messageThreadId", 0L)
            reflection.setIfPresent(it, "replyTo", null)
            reflection.setIfPresent(it, "options", null)
            reflection.setIfPresent(it, "replyMarkup", null)
            reflection.setIfPresent(it, "inputMessageContent", content)
        }

        val sentMessage = send(function)
        val messageId = reflection.longField(sentMessage, "id") ?: 0L
        emit(TransferProgress(progress = 10, messageId = messageId))
        waitForUploadCompletion(sentMessage, file.length()).collect { progress ->
            emit(progress.copy(messageId = messageId))
        }
        emit(TransferProgress(progress = 100, done = true, messageId = messageId))
    }

    override fun downloadFile(messageId: Long, folderId: Long?): Flow<TransferProgress> = flow {
        downloadFileInternal(messageId, folderId, destination = null).collect { emit(it) }
    }

    override fun downloadFileTo(messageId: Long, folderId: Long?, destination: Uri): Flow<TransferProgress> = flow {
        downloadFileInternal(messageId, folderId, destination = destination).collect { emit(it) }
    }

    private fun downloadFileInternal(messageId: Long, folderId: Long?, destination: Uri?): Flow<TransferProgress> = flow {
        emit(TransferProgress(0))
        ensureReady()
        val chatId = resolveChatId(folderId)
        val message = getMessage(chatId, messageId)
        val fileInfo = message?.let { messageToFile(it, folderId) }
        val rawFile = message?.let(::messageRawFile)
        val fileId = rawFile?.let { reflection.intField(it, "id") }
        if (message == null || fileInfo == null || fileId == null) {
            emit(TransferProgress(progress = 0, done = false, error = "No downloadable file found."))
            return@flow
        }
        val initialPath = localPath(rawFile)
        if (isLocalDownloadComplete(rawFile) && initialPath != null && !File(initialPath).exists()) {
            resetLocalFile(fileId)
        }
        val localBefore = localPath(rawFile)?.takeIf { File(it).exists() }
        val sourcePath = localBefore ?: run {
            emit(TransferProgress(progress = 10))
            requestDownload(fileId)
            val deadline = System.currentTimeMillis() + 120_000
            var lastProgress = 10
            var resetStaleLocalFile = false
            var lastPollAt = 0L

            while (System.currentTimeMillis() < deadline) {
                val state = observeFileState(fileId).value
                val path = state.localPath
                val expectedSize = state.expectedSize.takeIf { it > 0 } ?: fileInfo.size
                val downloadedSize = state.downloadedSize

                if (!resetStaleLocalFile && state.isCompleted && path != null && !File(path).exists()) {
                    resetStaleLocalFile = true
                    resetLocalFile(fileId)
                    requestDownload(fileId)
                    emit(TransferProgress(progress = 10))
                    delay(500)
                    continue
                }

                val nextProgress = if (expectedSize > 0) {
                    (10 + ((downloadedSize.coerceAtMost(expectedSize) * 70) / expectedSize)).toInt()
                        .coerceIn(10, 80)
                } else {
                    (lastProgress + 5).coerceAtMost(75)
                }
                if (nextProgress > lastProgress) {
                    lastProgress = nextProgress
                    emit(
                        TransferProgress(
                            progress = lastProgress,
                            localPath = path,
                            downloadedBytes = downloadedSize,
                            totalBytes = expectedSize.takeIf { it > 0 },
                        ),
                    )
                }

                if (state.isCompleted && path != null && File(path).exists()) {
                    emit(
                        TransferProgress(
                            progress = 80,
                            localPath = path,
                            downloadedBytes = downloadedSize,
                            totalBytes = expectedSize.takeIf { it > 0 },
                        ),
                    )
                    break
                }

                // Fallback: if UpdateFile updates are not arriving, poll periodically.
                val now = System.currentTimeMillis()
                if (now - lastPollAt > 2_000) {
                    lastPollAt = now
                    runCatching { getFile(fileId)?.let(::handleFileUpdate) }
                }

                delay(500)
            }

            awaitDownloadPath(fileId, timeoutMs = 2_000)
                ?: getMessage(chatId, messageId)
                    ?.let(::messageRawFile)
                    ?.let(::localPath)
                    ?.takeIf { File(it).exists() }
        }

        if (sourcePath == null) {
            emit(TransferProgress(progress = 0, done = false, error = "Downloaded file was not written by TDLib."))
            return@flow
        }

        withContext(Dispatchers.IO) {
            copyToDownloads(File(sourcePath), fileInfo.name, fileInfo.mimeType, destination)
        }
        emit(TransferProgress(progress = 100, done = true))
    }

    override fun setDownloadDestination(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        downloadPrefs.edit()
            .putString(KEY_DOWNLOAD_TREE_URI, uri.toString())
            .putString(KEY_DOWNLOAD_TREE_LABEL, labelForTreeUri(uri))
            .apply()
        _downloadDestinationLabel.value = readDownloadDestinationLabel()
    }

    override fun clearDownloadDestination() {
        downloadPrefs.edit().clear().apply()
        _downloadDestinationLabel.value = readDownloadDestinationLabel()
    }

    override suspend fun deleteFiles(messageIds: List<Long>, folderId: Long?) {
        ensureReady()
        if (messageIds.isEmpty()) return
        val function = reflection.newFunction("DeleteMessages")
        reflection.setIfPresent(function, "chatId", resolveChatId(folderId))
        reflection.setIfPresent(function, "messageIds", messageIds.toLongArray())
        reflection.setIfPresent(function, "revoke", true)
        send(function)
    }

    override suspend fun moveFiles(messageIds: List<Long>, sourceFolderId: Long?, targetFolderId: Long?) {
        ensureReady()
        if (messageIds.isEmpty() || sourceFolderId == targetFolderId) return
        val sourceChatId = resolveChatId(sourceFolderId)
        val targetChatId = resolveChatId(targetFolderId)
        val function = reflection.newFunction("ForwardMessages")
        reflection.setIfPresent(function, "chatId", targetChatId)
        reflection.setIfPresent(function, "fromChatId", sourceChatId)
        reflection.setIfPresent(function, "messageIds", messageIds.toLongArray())
        reflection.setIfPresent(function, "options", null)
        reflection.setIfPresent(function, "sendCopy", false)
        reflection.setIfPresent(function, "removeCaption", false)
        send(function)
        deleteFiles(messageIds, sourceFolderId)
    }

    private fun ensureClient(): Any {
        client?.let { return it }
        return reflection.createClient(
            onUpdate = ::handleUpdate,
            onDefaultException = { throwable ->
                _authState.value = AuthState.Error(throwable.message ?: "TDLib runtime error.")
            },
        ).also { client = it }
    }

    private fun handleUpdate(update: Any) {
        val name = reflection.simpleName(update)
        if (name == "UpdateAuthorizationState") {
            reflection.field(update, "authorizationState")?.let(::handleAuthorizationState)
        } else if (name == "UpdateConnectionState") {
            reflection.field(update, "state")?.let(::handleConnectionState)
        } else if (name == "UpdateFile") {
            reflection.field(update, "file")?.let(::handleFileUpdate)
        }
    }

    private fun handleFileUpdate(file: Any) {
        val fileId = reflection.intField(file, "id") ?: return
        val expectedSize = fileSize(file)
        val localPath = localPath(file)
        val downloadedSize = localDownloadedSize(file)
        val isCompleted = isLocalDownloadComplete(file)

        val state = FileState(
            fileId = fileId,
            localPath = localPath,
            downloadedSize = downloadedSize,
            expectedSize = expectedSize,
            isCompleted = isCompleted,
        )
        flowForFile(fileId).value = state
    }

    private fun flowForFile(fileId: Int): MutableStateFlow<FileState> {
        synchronized(fileStatesLock) {
            return fileStates.getOrPut(fileId) { MutableStateFlow(FileState(fileId = fileId)) }
        }
    }

    private fun observeFileState(fileId: Int): StateFlow<FileState> =
        flowForFile(fileId).asStateFlow()

    private suspend fun awaitDownloadPath(
        fileId: Int,
        timeoutMs: Long = 120_000,
    ): String? {
        // Prime state once in case no updates arrive promptly.
        runCatching { getFile(fileId)?.let(::handleFileUpdate) }
        return withTimeoutOrNull(timeoutMs) {
            observeFileState(fileId)
                .map { it.localPath }
                .filterNotNull()
                .filter { it.isNotBlank() && File(it).exists() }
                .first()
        }
    }

    private suspend fun awaitDownloadedBytes(
        fileId: Int,
        minBytes: Long,
        timeoutMs: Long = 120_000,
    ): FileState? {
        runCatching { getFile(fileId)?.let(::handleFileUpdate) }
        return withTimeoutOrNull(timeoutMs) {
            observeFileState(fileId)
                .filter { it.downloadedSize >= minBytes || it.isCompleted }
                .first()
        }
    }

    private suspend fun awaitDownloadCompletion(
        fileId: Int,
        timeoutMs: Long = 6 * 60 * 60 * 1000L,
    ): FileState? {
        runCatching { getFile(fileId)?.let(::handleFileUpdate) }
        return withTimeoutOrNull(timeoutMs) {
            observeFileState(fileId)
                .filter { it.isCompleted }
                .first()
        }
    }

    private fun handleConnectionState(state: Any) {
        isConnectionReady = reflection.simpleName(state) == "ConnectionStateReady"
        checkReadyState()
    }

    private fun checkReadyState() {
        if (isAuthorized && isConnectionReady) {
            _authState.value = AuthState.Ready
        } else if (!isAuthorized && _authState.value == AuthState.Ready) {
            _authState.value = AuthState.Initializing
        }
    }

    private fun handleAuthorizationState(state: Any) {
        when (reflection.simpleName(state)) {
            "AuthorizationStateWaitTdlibParameters" -> {
                if (apiId != null && apiHash != null) {
                    // parametersSent guard prevents the double-send race:
                    // update callback fires first, then GetAuthorizationState response
                    // also sees waitTdlibParameters — only the first one should send.
                    if (!parametersSent) {
                        parametersSent = true
                        sendAsync(buildTdlibParameters())
                    }
                } else {
                    _authState.value = AuthState.NeedsApiCredentials
                }
            }

            "AuthorizationStateWaitEncryptionKey" -> {
                sendAsync(reflection.newFunction("CheckDatabaseEncryptionKey").also {
                    reflection.setIfPresent(it, "encryptionKey", databaseEncryptionKey)
                })
            }

            "AuthorizationStateWaitPhoneNumber" -> _authState.value = AuthState.NeedsPhone
            "AuthorizationStateWaitCode" -> _authState.value = AuthState.NeedsCode
            "AuthorizationStateWaitPassword" -> _authState.value = AuthState.NeedsPassword
            "AuthorizationStateReady" -> {
                isAuthorized = true
                checkReadyState()
            }
            "AuthorizationStateClosed" -> {
                isAuthorized = false
                // Reset so the next configure() cycle can send parameters again
                parametersSent = false
                _authState.value = AuthState.NeedsApiCredentials
            }
            "AuthorizationStateLoggingOut", "AuthorizationStateClosing" -> {
                isAuthorized = false
            }
            else -> Unit
        }
    }

    private fun buildTdlibParameters(): Any {
        tdlibRoot.mkdirs()
        File(tdlibRoot, "database").mkdirs()
        File(tdlibRoot, "files").mkdirs()

        val function = reflection.newFunction("SetTdlibParameters")
        val target = if (reflection.hasField(function, "parameters")) {
            reflection.newObject("TdlibParameters").also {
                reflection.setIfPresent(function, "parameters", it)
            }
        } else {
            function
        }

        reflection.setIfPresent(target, "useTestDc", false)
        reflection.setIfPresent(target, "databaseDirectory", File(tdlibRoot, "database").absolutePath)
        reflection.setIfPresent(target, "filesDirectory", File(tdlibRoot, "files").absolutePath)
        reflection.setIfPresent(target, "databaseEncryptionKey", databaseEncryptionKey)
        reflection.setIfPresent(target, "useFileDatabase", true)
        reflection.setIfPresent(target, "useChatInfoDatabase", true)
        reflection.setIfPresent(target, "useMessageDatabase", true)
        reflection.setIfPresent(target, "useSecretChats", false)
        reflection.setIfPresent(target, "apiId", apiId ?: 0)
        reflection.setIfPresent(target, "apiHash", apiHash.orEmpty())
        reflection.setIfPresent(target, "systemLanguageCode", Locale.getDefault().toLanguageTag())
        reflection.setIfPresent(target, "deviceModel", Build.MODEL.ifBlank { "Android" })
        reflection.setIfPresent(target, "systemVersion", Build.VERSION.RELEASE.ifBlank { Build.VERSION.SDK_INT.toString() })
        reflection.setIfPresent(target, "applicationVersion", "0.1.0")
        reflection.setIfPresent(target, "enableStorageOptimizer", true)
        reflection.setIfPresent(target, "ignoreFileNames", false)
        return function
    }

    private fun sendAsync(function: Any) {
        runCatching {
            reflection.send(ensureClient(), function) { result ->
                if (reflection.simpleName(result) == "Error") {
                    _authState.value = AuthState.Error(tdErrorMessage(result))
                }
            }
        }.onFailure {
            _authState.value = AuthState.Error(it.message ?: "TDLib send failed.")
        }
    }

    private suspend fun send(function: Any): Any =
        suspendCancellableCoroutine { continuation ->
            reflection.send(ensureClient(), function) { result ->
                if (!continuation.isActive) return@send
                if (reflection.simpleName(result) == "Error") {
                    continuation.resumeWithException(IllegalStateException(tdErrorMessage(result)))
                } else {
                    continuation.resume(result)
                }
            }
        }

    private suspend fun pumpAuthorizationState() {
        repeat(4) {
            val state = withContext(Dispatchers.IO) {
                send(reflection.newFunction("GetAuthorizationState"))
            }
            handleAuthorizationState(state)
            if (_authState.value != AuthState.Initializing) return
            delay(250)
        }
    }

    private fun tdErrorMessage(error: Any): String {
        val code = reflection.intField(error, "code")
        val message = reflection.stringField(error, "message").orEmpty()
        return if (code != null) "TDLib error $code: $message" else message.ifBlank { "TDLib error." }
    }

    private fun ensureReady() {
        check(authState.value == AuthState.Ready) { "Telegram is not authorized yet." }
    }

    private suspend fun resolveChatId(folderId: Long?): Long =
        folderId ?: savedMessagesChatId ?: loadSavedMessagesChatId().also { savedMessagesChatId = it }

    private suspend fun loadSavedMessagesChatId(): Long {
        val me = send(reflection.newFunction("GetMe"))
        val userId = reflection.longField(me, "id")
            ?: error("TDLib did not return the current user id.")
        // CreatePrivateChat forces TDLib to load the Saved Messages chat into memory.
        // Without this, GetChatHistory returns error 400 "chat not found" because
        // TDLib only loads chats lazily — they must be "opened" first.
        val chat = runCatching {
            send(reflection.newFunction("CreatePrivateChat").also {
                reflection.setIfPresent(it, "userId", userId)
                reflection.setIfPresent(it, "force", true)
            })
        }.getOrNull()
        return reflection.longField(chat ?: return userId, "id") ?: userId
    }

    private suspend fun getChats(limit: Int): List<Any> {
        val function = reflection.newFunction("GetChats")
        reflection.setIfPresent(function, "chatList", reflection.newObjectOrNull("ChatListMain"))
        reflection.setIfPresent(function, "offsetOrder", Long.MAX_VALUE)
        reflection.setIfPresent(function, "offsetChatId", 0L)
        reflection.setIfPresent(function, "limit", limit)
        val result = send(function)
        val chatIds = reflection.field(result, "chatIds") as? LongArray ?: return emptyList()
        val chats = mutableListOf<Any>()
        for (id in chatIds) {
            getChat(id)?.let(chats::add)
        }
        return chats
    }

    private suspend fun getChat(chatId: Long): Any? =
        runCatching {
            send(reflection.newFunction("GetChat").also {
                reflection.setIfPresent(it, "chatId", chatId)
            })
        }.getOrNull()

    private suspend fun chatDescription(chatId: Long): String? =
        runCatching {
            val fullInfo = send(reflection.newFunction("GetChatFullInfo").also {
                reflection.setIfPresent(it, "chatId", chatId)
            })
            reflection.stringField(fullInfo, "description")
        }.getOrNull()

    private suspend fun setChatAutoDeleteTime(chatId: Long, seconds: Int) {
        runCatching {
            send(reflection.newFunction("SetChatMessageAutoDeleteTime").also {
                reflection.setIfPresent(it, "chatId", chatId)
                reflection.setIfPresent(it, "messageAutoDeleteTime", seconds)
            })
        }
    }

    private suspend fun getMessage(chatId: Long, messageId: Long): Any? =
        runCatching {
            send(reflection.newFunction("GetMessage").also {
                reflection.setIfPresent(it, "chatId", chatId)
                reflection.setIfPresent(it, "messageId", messageId)
            })
        }.getOrNull()

    private suspend fun getFile(fileId: Int): Any? =
        runCatching {
            send(reflection.newFunction("GetFile").also {
                reflection.setIfPresent(it, "fileId", fileId)
            })
        }.getOrNull()

    internal suspend fun requestDownload(fileId: Int) {
        send(reflection.newFunction("DownloadFile").also {
            reflection.setIfPresent(it, "fileId", fileId)
            reflection.setIfPresent(it, "priority", 32)
            reflection.setIfPresent(it, "offset", 0L)
            reflection.setIfPresent(it, "limit", 0L)
            reflection.setIfPresent(it, "synchronous", false)
        })
    }

    override suspend fun downloadThumbnail(thumbFileId: Int): String? {
        return runCatching {
            requestDownload(thumbFileId)
            awaitDownloadPath(thumbFileId, timeoutMs = 15000)
        }.getOrNull()
    }

    private suspend fun resetLocalFile(fileId: Int) {
        runCatching {
            send(reflection.newFunction("DeleteFile").also {
                reflection.setIfPresent(it, "fileId", fileId)
            })
        }
    }

    private fun waitForUploadCompletion(message: Any, sourceSize: Long): Flow<TransferProgress> = flow {
        val initialFile = messageRawFile(message) ?: return@flow
        val fileId = reflection.intField(initialFile, "id")
        val deadline = System.currentTimeMillis() + 6 * 60 * 60 * 1000L
        var lastProgress = 10
        var currentFile = initialFile

        while (System.currentTimeMillis() < deadline) {
            if (fileId != null) {
                currentFile = getFile(fileId) ?: currentFile
            }
            val remote = reflection.field(currentFile, "remote")
            val uploadedSize = remote?.let { reflection.longField(it, "uploadedSize") } ?: 0L
            val isComplete = remote?.let { reflection.booleanField(it, "isUploadingCompleted") } == true
            val isActive = remote?.let { reflection.booleanField(it, "isUploadingActive") } == true
            val nextProgress = if (sourceSize > 0 && uploadedSize > 0) {
                (10 + ((uploadedSize.coerceAtMost(sourceSize) * 85) / sourceSize)).toInt().coerceIn(10, 95)
            } else if (isActive) {
                (lastProgress + 1).coerceAtMost(95)
            } else {
                lastProgress
            }

            if (nextProgress > lastProgress) {
                lastProgress = nextProgress
                emit(TransferProgress(progress = lastProgress))
            }
            if (isComplete || (sourceSize > 0 && uploadedSize >= sourceSize)) {
                emit(TransferProgress(progress = 98))
                return@flow
            }
            delay(500)
        }

        emit(TransferProgress(progress = lastProgress.coerceAtLeast(95)))
    }

    private fun messageToFile(message: Any, folderId: Long?): TelegramFile? {
        val messageId = reflection.longField(message, "id") ?: return null
        val dateSeconds = reflection.intField(message, "date") ?: 0
        val content = reflection.field(message, "content") ?: return null
        val media = extractMedia(content) ?: return null
        val captionMetadata = captionMetadata(content)
        val resolvedName = captionMetadata?.name ?: media.name.ifBlank { defaultName(content, messageId) }
        val rawFile = messageRawFile(message)
        val tdFileId = rawFile?.let { reflection.intField(it, "id") }
        val remote = rawFile?.let { reflection.field(it, "remote") }
        val tdRemoteUniqueId = remote?.let { reflection.stringField(it, "uniqueId") }?.takeIf { it.isNotBlank() }
        return TelegramFile(
            messageId = messageId,
            folderId = folderId,
            name = resolvedName,
            size = captionMetadata?.size?.takeIf { it > 0 } ?: media.size,
            mimeType = captionMetadata?.mimeType ?: media.mimeType,
            extension = resolvedName.substringAfterLast('.', missingDelimiterValue = "").ifBlank { null },
            createdAt = dateSeconds * 1000L,
            localCachePath = media.localPath,
            thumbnailBase64 = media.thumbnailBase64,
            tdFileId = tdFileId,
            tdRemoteUniqueId = tdRemoteUniqueId,
            tdThumbnailFileId = media.thumbnailFileId,
            tdThumbnailLocalPath = media.thumbnailLocalPath,
        )
    }

    private fun metadataCaption(displayName: String, size: Long, mimeType: String?): String =
        JSONObject()
            .put(CAPTION_FILE_NAME_KEY, displayName.ifBlank { "Uploaded file" })
            .put(CAPTION_SIZE_KEY, size)
            .put(CAPTION_MIME_TYPE_KEY, mimeType.orEmpty())
            .toString()

    private fun captionMetadata(content: Any): StoredMetadata? {
        val caption = reflection.field(content, "caption") ?: return null
        val text = reflection.stringField(caption, "text")?.trim().orEmpty()
        if (text.isBlank()) return null
        return runCatching {
            val json = JSONObject(text)
            StoredMetadata(
                name = json.optString(CAPTION_FILE_NAME_KEY).takeIf { it.isNotBlank() },
                size = json.optLong(CAPTION_SIZE_KEY).takeIf { it > 0 },
                mimeType = json.optString(CAPTION_MIME_TYPE_KEY).takeIf { it.isNotBlank() },
            ).takeIf { it.name != null || it.size != null || it.mimeType != null }
        }.getOrNull()
    }

    private fun extractMedia(content: Any): MediaInfo? {
        val contentName = reflection.simpleName(content)
        val mediaFieldName = when (contentName) {
            "MessageDocument" -> "document"
            "MessagePhoto" -> "photo"
            "MessageVideo" -> "video"
            "MessageAudio" -> "audio"
            "MessageVoiceNote" -> "voiceNote"
            "MessageAnimation" -> "animation"
            else -> return null
        }
        val media = reflection.field(content, mediaFieldName) ?: return null
        val minithumbnail = reflection.field(media, "minithumbnail")
        val thumbnailData = minithumbnail?.let { reflection.field(it, "data") as? ByteArray }
        val thumbnailBase64 = thumbnailData?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
        val (thumbnailFileId, thumbnailLocalPath) = extractThumbnailFile(media)

        if (contentName == "MessagePhoto") {
            val sizes = reflection.field(media, "sizes") as? Array<*> ?: emptyArray<Any>()
            val largest = sizes.filterNotNull().maxByOrNull { reflection.intField(it, "width") ?: 0 }
            val file = largest?.let { reflection.field(it, "photo") }
            return MediaInfo(
                name = "Photo.jpg",
                size = fileSize(file),
                mimeType = "image/jpeg",
                localPath = localPath(file),
                thumbnailBase64 = thumbnailBase64,
                thumbnailFileId = thumbnailFileId,
                thumbnailLocalPath = thumbnailLocalPath,
            )
        }
        val file = when (contentName) {
            "MessageVideo" -> reflection.field(media, "video")
            "MessageAudio" -> reflection.field(media, "audio")
            "MessageVoiceNote" -> reflection.field(media, "voice")
            "MessageAnimation" -> reflection.field(media, "animation")
            else -> reflection.field(media, "document")
        }
        return MediaInfo(
            name = reflection.stringField(media, "fileName").orEmpty(),
            size = fileSize(file),
            mimeType = reflection.stringField(media, "mimeType"),
            localPath = localPath(file),
            thumbnailBase64 = thumbnailBase64,
            thumbnailFileId = thumbnailFileId,
            thumbnailLocalPath = thumbnailLocalPath,
        )
    }

    private fun extractThumbnailFile(media: Any): Pair<Int?, String?> {
        // Many TDLib media objects contain a `thumbnail` object with a nested `file`.
        // We keep this best-effort (reflection-based) and fall back to minithumbnail.
        val thumb = reflection.field(media, "thumbnail") ?: return null to null
        val file = reflection.field(thumb, "file") ?: return null to null
        val id = reflection.intField(file, "id")
        val path = localPath(file)?.takeIf { File(it).exists() }
        return id to path
    }

    private fun messageFileId(message: Any): Int? {
        val content = reflection.field(message, "content") ?: return null
        val media = extractRawFile(content) ?: return null
        return reflection.intField(media, "id")
    }

    private fun messageRawFile(message: Any): Any? {
        val content = reflection.field(message, "content") ?: return null
        return extractRawFile(content)
    }

    private fun extractRawFile(content: Any): Any? {
        val media = when (reflection.simpleName(content)) {
            "MessageDocument" -> reflection.field(reflection.field(content, "document") ?: return null, "document")
            "MessageVideo" -> reflection.field(reflection.field(content, "video") ?: return null, "video")
            "MessageAudio" -> reflection.field(reflection.field(content, "audio") ?: return null, "audio")
            "MessageVoiceNote" -> reflection.field(reflection.field(content, "voiceNote") ?: return null, "voice")
            "MessageAnimation" -> reflection.field(reflection.field(content, "animation") ?: return null, "animation")
            "MessagePhoto" -> {
                val photo = reflection.field(content, "photo") ?: return null
                val sizes = reflection.field(photo, "sizes") as? Array<*> ?: return null
                val largest = sizes.filterNotNull().maxByOrNull { reflection.intField(it, "width") ?: 0 }
                reflection.field(largest ?: return null, "photo")
            }
            else -> null
        }
        return media
    }

    private fun fileSize(file: Any?): Long {
        if (file == null) return 0
        return reflection.longField(file, "size")
            ?: reflection.longField(file, "expectedSize")
            ?: 0L
    }

    private fun localPath(file: Any?): String? {
        val local = reflection.field(file ?: return null, "local") ?: return null
        return reflection.stringField(local, "path")?.takeIf { it.isNotBlank() }
    }

    private fun localDownloadedSize(file: Any?): Long {
        val local = reflection.field(file ?: return 0L, "local") ?: return 0L
        return reflection.longField(local, "downloadedSize")
            ?: reflection.longField(local, "downloadedPrefixSize")
            ?: 0L
    }

    private fun isLocalDownloadComplete(file: Any?): Boolean {
        val local = reflection.field(file ?: return false, "local") ?: return false
        return reflection.booleanField(local, "isDownloadingCompleted") == true
    }

    private fun defaultName(content: Any, messageId: Long): String =
        when (reflection.simpleName(content)) {
            "MessagePhoto" -> "Photo_$messageId.jpg"
            "MessageVideo" -> "Video_$messageId.mp4"
            "MessageAudio" -> "Audio_$messageId.mp3"
            "MessageVoiceNote" -> "Voice_$messageId.ogg"
            "MessageAnimation" -> "Animation_$messageId.gif"
            else -> "File_$messageId"
        }

    private fun copyUriToUploadCache(uri: Uri, displayName: String): File {
        val uploadDir = File(uploadCacheDir, System.currentTimeMillis().toString()).also { it.mkdirs() }
        val safeName = displayName.ifBlank { uri.lastPathSegment ?: "upload.bin" }
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
        val target = File(uploadDir, safeName)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open selected file." }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target
    }

    private fun copyToDownloads(source: File, displayName: String, mimeType: String?, destination: Uri? = null) {
        val safeName = displayName.ifBlank { source.name }
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
        val customTreeUri = destination ?: downloadPrefs.getString(KEY_DOWNLOAD_TREE_URI, null)?.let(Uri::parse)
        if (customTreeUri != null) {
            copyToSelectedFolder(customTreeUri, source, safeName, mimeType)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType ?: "application/octet-stream")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TeleDrive")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create Downloads file.")
            try {
                resolver.openOutputStream(uri).use { output ->
                    requireNotNull(output) { "Unable to open Downloads file." }
                    source.inputStream().use { input -> input.copyTo(output) }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (throwable: Throwable) {
                resolver.delete(uri, null, null)
                throw throwable
            }
        } else {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "downloads")
            downloadsDir.mkdirs()
            source.copyTo(File(downloadsDir, safeName), overwrite = true)
        }
    }

    private fun copyToSelectedFolder(treeUri: Uri, source: File, displayName: String, mimeType: String?) {
        val resolver = context.contentResolver
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        val directoryUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val targetUri = DocumentsContract.createDocument(
            resolver,
            directoryUri,
            mimeType ?: "application/octet-stream",
            displayName,
        ) ?: error("Unable to create file in selected folder.")
        try {
            resolver.openOutputStream(targetUri).use { output ->
                requireNotNull(output) { "Unable to open selected folder file." }
                source.inputStream().use { input -> input.copyTo(output) }
            }
        } catch (throwable: Throwable) {
            resolver.delete(targetUri, null, null)
            throw throwable
        }
    }

    private fun readDownloadDestinationLabel(): String =
        downloadPrefs.getString(KEY_DOWNLOAD_TREE_LABEL, null) ?: DEFAULT_DOWNLOAD_LABEL

    private fun labelForTreeUri(uri: Uri): String =
        DocumentsContract.getTreeDocumentId(uri)
            .substringAfter(':', missingDelimiterValue = "")
            .ifBlank { uri.lastPathSegment?.substringAfter(':') }
            ?.ifBlank { null }
            ?: "Selected folder"

    private data class MediaInfo(
        val name: String,
        val size: Long,
        val mimeType: String?,
        val localPath: String?,
        val thumbnailBase64: String?,
        val thumbnailFileId: Int?,
        val thumbnailLocalPath: String?,
    )

    private data class StoredMetadata(
        val name: String?,
        val size: Long?,
        val mimeType: String?,
    )

    // Backup-specific operations
    override suspend fun sendMessage(text: String, folderId: Long?): Long {
        ensureReady()
        val chatId = resolveChatId(folderId)
        val formattedText = reflection.newObject("FormattedText").also {
            reflection.setIfPresent(it, "text", text)
            reflection.setIfPresent(it, "entities", emptyArray<Any>())
        }
        val function = reflection.newFunction("SendMessage").also {
            reflection.setIfPresent(it, "chatId", chatId)
            reflection.setIfPresent(it, "messageThreadId", 0L)
            reflection.setIfPresent(it, "replyTo", null)
            reflection.setIfPresent(it, "options", null)
            reflection.setIfPresent(it, "replyMarkup", null)
            reflection.setIfPresent(it, "inputMessageContent", reflection.newObject("InputMessageText").also {
                reflection.setIfPresent(it, "text", formattedText)
                reflection.setIfPresent(it, "disableWebPagePreview", true)
                reflection.setIfPresent(it, "clearDraft", false)
            })
        }
        val result = send(function)
        return reflection.longField(result, "id") ?: error("Failed to send message")
    }

    override suspend fun editMessage(messageId: Long, text: String, folderId: Long?) {
        ensureReady()
        val chatId = resolveChatId(folderId)
        val formattedText = reflection.newObject("FormattedText").also {
            reflection.setIfPresent(it, "text", text)
            reflection.setIfPresent(it, "entities", emptyArray<Any>())
        }
        send(reflection.newFunction("EditMessageText").also {
            reflection.setIfPresent(it, "chatId", chatId)
            reflection.setIfPresent(it, "messageId", messageId)
            reflection.setIfPresent(it, "text", formattedText)
            reflection.setIfPresent(it, "disableWebPagePreview", true)
        })
    }

    override suspend fun pinMessage(messageId: Long, folderId: Long?) {
        ensureReady()
        val chatId = resolveChatId(folderId)
        send(reflection.newFunction("PinChatMessage").also {
            reflection.setIfPresent(it, "chatId", chatId)
            reflection.setIfPresent(it, "messageId", messageId)
            reflection.setIfPresent(it, "disableNotification", false)
        })
    }

    override suspend fun unpinMessage(messageId: Long, folderId: Long?) {
        ensureReady()
        val chatId = resolveChatId(folderId)
        send(reflection.newFunction("UnpinChatMessage").also {
            reflection.setIfPresent(it, "chatId", chatId)
            reflection.setIfPresent(it, "messageId", messageId)
        })
    }

    override suspend fun getPinnedMessages(folderId: Long?): List<Long> {
        ensureReady()
        val chatId = resolveChatId(folderId)
        val result = send(reflection.newFunction("GetChatPinnedMessages").also {
            reflection.setIfPresent(it, "chatId", chatId)
        })
        val messages = reflection.field(result, "messages") as? Array<*> ?: return emptyList()
        return messages.mapNotNull { msg ->
            reflection.longField(msg as? Any ?: return@mapNotNull null, "id")
        }
    }

    override suspend fun getMessage(messageId: Long, folderId: Long?): Any? {
        ensureReady()
        val chatId = resolveChatId(folderId)
        return runCatching {
            send(reflection.newFunction("GetMessage").also {
                reflection.setIfPresent(it, "chatId", chatId)
                reflection.setIfPresent(it, "messageId", messageId)
            })
        }.getOrNull()
    }

    companion object {
        private const val KEY_DOWNLOAD_TREE_URI = "tree_uri"
        private const val KEY_DOWNLOAD_TREE_LABEL = "tree_label"
        private const val DEFAULT_DOWNLOAD_LABEL = "Downloads/TeleDrive"
        private const val CAPTION_FILE_NAME_KEY = "td_name"
        private const val CAPTION_SIZE_KEY = "td_size"
        private const val CAPTION_MIME_TYPE_KEY = "td_mime"

        fun isAvailable(): Boolean =
            TdLibReflection.availableOrNull() != null
    }
}

private fun isTeleDriveFolderTitle(title: String): Boolean =
    title.contains(TdLibReadiness.folderTitleMarker, ignoreCase = true)

private fun isTeleDriveFolderAbout(about: String?): Boolean =
    about?.contains(TdLibReadiness.folderAboutMarker, ignoreCase = true) == true

private fun cleanFolderTitle(title: String): String =
    title
        .replace(" ${TdLibReadiness.folderTitleMarker}", "", ignoreCase = true)
        .replace(TdLibReadiness.folderTitleMarker, "", ignoreCase = true)
        .trim()
