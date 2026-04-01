package com.zibete.proyecto1.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.MSG_INFO
import com.zibete.proyecto1.core.constants.Constants.UiTags.CHAT_SCREEN
import com.zibete.proyecto1.core.utils.TimeUtils
import com.zibete.proyecto1.ui.chat.components.ArrowOverlay
import com.zibete.proyecto1.ui.chat.components.ChatInfoRow
import com.zibete.proyecto1.ui.chat.components.ChatInput
import com.zibete.proyecto1.ui.chat.components.ChatPhotoSourceSheet
import com.zibete.proyecto1.ui.chat.components.ChatTopBar
import com.zibete.proyecto1.ui.chat.components.DateOverlay
import com.zibete.proyecto1.ui.chat.components.MicRecordOverlay
import com.zibete.proyecto1.ui.chat.message.LegacyMessageRow
import com.zibete.proyecto1.ui.chat.message.isPhoto
import com.zibete.proyecto1.ui.chat.preview.sampleBlockedHeader
import com.zibete.proyecto1.ui.chat.preview.sampleChatStateEmpty
import com.zibete.proyecto1.ui.chat.preview.sampleChatStateMixedMessages
import com.zibete.proyecto1.ui.chat.preview.sampleHeaderLoaded
import com.zibete.proyecto1.ui.chat.preview.sampleMediaUiStateIdle
import com.zibete.proyecto1.ui.chat.preview.sampleMediaUiStatePendingAudio
import com.zibete.proyecto1.ui.chat.preview.samplePhotoUploadingState
import com.zibete.proyecto1.ui.chat.preview.sampleRecordingStates
import com.zibete.proyecto1.ui.chat.preview.sampleSelectionState
import com.zibete.proyecto1.ui.components.ZibeButtonSecondary
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    mediaUiState: ChatMediaUiState,
    callbacks: ChatCallbacks
) {
    val headerState by viewModel.headerState.collectAsStateWithLifecycle()
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()

    var showPhotoSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(chatState.showPhotoPicker) {
        if (chatState.showPhotoPicker) {
            showPhotoSheet = true
            viewModel.onPhotoPickerShown()
        }
    }

    val photoList = remember(chatState.messages) {
        chatState.messages.mapNotNull { item ->
            item.message.content.takeIf { item.message.type.isPhoto() }
        }
    }

    val myAudioAvatarUrl = (headerState as? ChatHeaderState.Loaded)?.let {
        viewModel.myIdentity.userPhotoUrl
    }

    ChatScreen(
        headerState = headerState,
        chatState = chatState,
        myUid = viewModel.myUid,
        myAudioAvatarUrl = myAudioAvatarUrl,
        otherAudioAvatarUrl = (headerState as? ChatHeaderState.Loaded)?.photoUrl,
        photoList = photoList,
        mediaUiState = mediaUiState,
        showPhotoSheet = showPhotoSheet,
        onDismissPhotoSheet = { showPhotoSheet = false },
        callbacks = callbacks
    )
}

@Composable
fun ChatScreen(
    headerState: ChatHeaderState,
    chatState: ChatState,
    myUid: String,
    myAudioAvatarUrl: String?,
    otherAudioAvatarUrl: String?,
    photoList: List<String>,
    mediaUiState: ChatMediaUiState,
    showPhotoSheet: Boolean,
    onDismissPhotoSheet: () -> Unit,
    callbacks: ChatCallbacks
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val selectionCount = chatState.selectedIds.size
    val headerLoaded = headerState as? ChatHeaderState.Loaded
    val isBlocked = headerLoaded?.isBlocked == true
    val notificationsEnabled = headerLoaded?.notificationsEnabled ?: true
    val title = headerLoaded?.name ?: stringResource(R.string.loading)
    val status = headerLoaded?.status ?: stringResource(R.string.loading)
    val photoUrl = headerLoaded?.photoUrl

    var inputText by rememberSaveable { mutableStateOf("") }
    var micCenterInWindow by remember { mutableStateOf<Offset?>(null) }
    var micPointerInWindow by remember { mutableStateOf<Offset?>(null) }
    var micPressed by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val sysBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val insetLeftPx = with(density) { sysBarsPadding.calculateLeftPadding(layoutDirection).toPx() }
    val insetTopPx = with(density) { sysBarsPadding.calculateTopPadding().toPx() }

    var rootPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    var playTrashDrop by remember { mutableStateOf(false) }
    val showSend = chatState.textReady || chatState.photoReady || mediaUiState.isAudioUploading

    val isPhotoUploading =
        chatState.photoReady && chatState.pendingFileUrl == null && chatState.pendingPhotoUri != null
    val sendEnabled = when {
        mediaUiState.isAudioUploading -> false
        chatState.photoReady -> chatState.pendingFileUrl != null
        chatState.textReady -> true
        else -> false
    }
    val showMicOverlay = micPressed || chatState.isRecording

    var dateOverlayText by remember { mutableStateOf("") }
    var showDateOverlay by remember { mutableStateOf(false) }
    var dateOverlayJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.scrollToItem(chatState.messages.lastIndex)
        }
    }

    LaunchedEffect(listState, chatState.messages) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val timestamp =
                    chatState.messages.getOrNull(index)?.message?.createdAt ?: return@collect
                dateOverlayText = TimeUtils.formatHeaderDate(timestamp)
                showDateOverlay = true
                dateOverlayJob?.cancel()
                dateOverlayJob = scope.launch {
                    delay(2000)
                    showDateOverlay = false
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalZibeExtendedColors.current.gradientZibe)
            .testTag(CHAT_SCREEN)
            .onGloballyPositioned { coordinates ->
                rootPositionInWindow =
                    coordinates.positionInWindow() + Offset(insetLeftPx, insetTopPx)
            }
            .windowInsetsPadding(WindowInsets.systemBars)

    ) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    name = title,
                    status = status,
                    photoUrl = photoUrl,
                    notificationsEnabled = notificationsEnabled,
                    selectionCount = selectionCount,
                    onBackClick = callbacks.onBackClick,
                    onProfileClick = callbacks.onProfileClick,
                    onToggleNotifications = callbacks.onToggleNotifications,
                    onDeleteChat = callbacks.onDeleteChoiceMode,
                    onHideChat = callbacks.onConfirmHide,
                    onDeleteSelected = callbacks.onDeleteSelected,
                    onClearSelection = callbacks.onClearSelection
                )
            },
            containerColor = Color.Transparent,
            content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (isBlocked) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            ZibeButtonSecondary(
                                text = stringResource(R.string.menu_user_unblock),
                                onClick = { /* */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(all = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            items(chatState.messages, key = { it.id }) { item ->
                                if (item.message.type == MSG_INFO) {
                                    ChatInfoRow(text = item.message.content)
                                } else {
                                    val isSelected = chatState.selectedIds.contains(item.id)
                                    val hasSelection = chatState.selectedIds.isNotEmpty()
                                    val isMe = item.message.senderUid == myUid
                                    LegacyMessageRow(
                                        item = item,
                                        isMe = isMe,
                                        isSelected = isSelected,
                                        hasSelection = hasSelection,
                                        myAudioAvatarUrl = myAudioAvatarUrl,
                                        otherAudioAvatarUrl = otherAudioAvatarUrl,
                                        photoList = photoList,
                                        onSelectionChanged = callbacks.onSelectionChanged
                                    )
                                }
                            }
                        }
                    }

                    if (showDateOverlay) {
                        DateOverlay(text = dateOverlayText)
                    }

                    val showScrollToBottom by remember {
                        derivedStateOf { listState.canScrollForward }
                    }

                    if (showScrollToBottom && !isBlocked) {
                        ArrowOverlay(
                            chatState = chatState,
                            listState = listState,
                            scope = scope
                        )
                    }
                }
            },
            bottomBar = {
                if (!isBlocked) {
                    ChatInput(
                        inputText = inputText,
                        onInputChange = {
                            inputText = it
                            callbacks.onTextChanged(it)
                        },
                        pendingPhotoUri = chatState.pendingPhotoUri,
                        isPhotoUploading = isPhotoUploading,
                        onRemovePendingPhoto = callbacks.onRemovePendingPhoto,
                        isRecording = chatState.isRecording,
                        recordingElapsedMs = mediaUiState.recordingElapsedMs,
                        isRecordingCanceled = mediaUiState.isRecordingCanceled,
                        showSend = showSend,
                        sendEnabled = sendEnabled,
                        onCameraClick = callbacks.onPhotoSourceClick,
                        onSendClick = {
                            when {
                                chatState.photoReady && chatState.pendingFileUrl != null -> {
                                    callbacks.onSendPhoto(chatState.pendingFileUrl)
                                    callbacks.onRemovePendingPhoto()
                                }

                                chatState.textReady -> {
                                    callbacks.onSendText(inputText)
                                    inputText = ""
                                    callbacks.onTextChanged("")
                                }
                            }
                        },
                        onMicPressed = callbacks.onMicPressed,
                        onMicMoved = callbacks.onMicMoved,
                        onMicReleased = callbacks.onMicReleased,
                        onMicPressStateChange = { pressed ->
                            micPressed = pressed
                            if (pressed) {
                                playTrashDrop = false
                            } else if (mediaUiState.isRecordingCanceled) {
                                playTrashDrop = true
                            }
                        },
                        onMicButtonPositioned = { micCenterInWindow = it },
                        onMicPointerInWindowChanged = { micPointerInWindow = it },
                        showTrashDrop = playTrashDrop,
                        onTrashDropFinished = { playTrashDrop = false },
                        isAudioUploading = mediaUiState.isAudioUploading
                    )
                }
            }
        )

        MicRecordOverlay(
            isVisible = showMicOverlay,
            pointerInWindow = micPointerInWindow ?: micCenterInWindow,
            rootPositionInWindow = rootPositionInWindow
        )

        ChatPhotoSourceSheet(
            isOpen = showPhotoSheet,
            onDismiss = onDismissPhotoSheet,
            onCameraClick = callbacks.onLaunchCamera,
            onGalleryClick = callbacks.onLaunchGallery
        )
    }

}

private fun previewCallbacks(): ChatCallbacks = ChatCallbacks(
    onBackClick = {},
    onProfileClick = {},
    onToggleNotifications = {},
    onToggleBlock = {},
    onDeleteChoiceMode = {},
    onConfirmHide = {},
    onDeleteSelected = {},
    onClearSelection = {},
    onPhotoSourceClick = {},
    onLaunchCamera = {},
    onLaunchGallery = {},
    onRemovePendingPhoto = {},
    onTextChanged = {},
    onSendText = {},
    onSendPhoto = {},
    onSendAudio = { _, _ -> },
    onSelectionChanged = { _, _ -> },
    onMicPressed = {},
    onMicMoved = { _, _, _ -> },
    onMicReleased = {}
)

@Preview(name = "ChatScreenScenario_LoadingEmpty", showBackground = true)
@Composable
private fun ChatScreenScenarioLoadingEmpty() {
    ZibeTheme {
        ChatScreen(
            headerState = ChatHeaderState.Loading,
            chatState = sampleChatStateEmpty(),
            myUid = "me",
            myAudioAvatarUrl = null,
            otherAudioAvatarUrl = null,
            photoList = emptyList(),
            mediaUiState = sampleMediaUiStateIdle(),
            showPhotoSheet = false,
            onDismissPhotoSheet = {},
            callbacks = previewCallbacks()
        )
    }
}

@Preview(name = "ChatScreenScenario_LoadedEmpty", showBackground = true)
@Composable
private fun ChatScreenScenarioLoadedEmpty() {
    ZibeTheme {
        ChatScreen(
            headerState = sampleHeaderLoaded(),
            chatState = sampleChatStateEmpty(),
            myUid = "me",
            myAudioAvatarUrl = null,
            otherAudioAvatarUrl = null,
            photoList = emptyList(),
            mediaUiState = sampleMediaUiStateIdle(),
            showPhotoSheet = false,
            onDismissPhotoSheet = {},
            callbacks = previewCallbacks()
        )
    }
}

@Preview(name = "ChatScreenScenario_MixedMessages", showBackground = true)
@Composable
private fun ChatScreenScenarioMixedMessages() {
    val chatState = sampleChatStateMixedMessages()
    val photoList = chatState.messages.mapNotNull { item ->
        item.message.content.takeIf { item.message.type.isPhoto() }
    }

    ZibeTheme {
        ChatScreen(
            headerState = sampleHeaderLoaded(),
            chatState = chatState,
            myUid = "me",
            myAudioAvatarUrl = null,
            otherAudioAvatarUrl = null,
            photoList = photoList,
            mediaUiState = sampleMediaUiStateIdle(),
            showPhotoSheet = false,
            onDismissPhotoSheet = {},
            callbacks = previewCallbacks()
        )
    }
}

@Preview(name = "ChatScreenScenario_SelectionMode", showBackground = true)
@Composable
private fun ChatScreenScenarioSelectionMode() {
    val chatState = sampleSelectionState()
    val photoList = chatState.messages.mapNotNull { item ->
        item.message.content.takeIf { item.message.type.isPhoto() }
    }

    ZibeTheme {
        ChatScreen(
            headerState = sampleHeaderLoaded(),
            chatState = chatState,
            myUid = "me",
            myAudioAvatarUrl = null,
            otherAudioAvatarUrl = null,
            photoList = photoList,
            mediaUiState = sampleMediaUiStateIdle(),
            showPhotoSheet = false,
            onDismissPhotoSheet = {},
            callbacks = previewCallbacks()
        )
    }
}

@Preview(name = "ChatScreenScenario_Blocked", showBackground = true)
@Composable
private fun ChatScreenScenarioBlocked() {
    ZibeTheme {
        ChatScreen(
            headerState = sampleBlockedHeader(),
            chatState = sampleChatStateMixedMessages(),
            myUid = "me",
            myAudioAvatarUrl = null,
            otherAudioAvatarUrl = null,
            photoList = emptyList(),
            mediaUiState = sampleMediaUiStateIdle(),
            showPhotoSheet = false,
            onDismissPhotoSheet = {},
            callbacks = previewCallbacks()
        )
    }
}

@Preview(name = "ChatScreenScenario_PendingPhotoUploading", showBackground = true)
@Composable
private fun ChatScreenScenarioPendingPhotoUploading() {
    val chatState = samplePhotoUploadingState()
    val photoList = chatState.messages.mapNotNull { item ->
        item.message.content.takeIf { item.message.type.isPhoto() }
    }

    ZibeTheme {
        ChatScreen(
            headerState = sampleHeaderLoaded(),
            chatState = chatState,
            myUid = "me",
            myAudioAvatarUrl = null,
            otherAudioAvatarUrl = null,
            photoList = photoList,
            mediaUiState = sampleMediaUiStateIdle(),
            showPhotoSheet = false,
            onDismissPhotoSheet = {},
            callbacks = previewCallbacks()
        )
    }
}

@Preview(name = "ChatScreenScenario_PendingAudio", showBackground = true)
@Composable
private fun ChatScreenScenarioPendingAudio() {
    val chatState = sampleChatStateMixedMessages()
    val photoList = chatState.messages.mapNotNull { item ->
        item.message.content.takeIf { item.message.type.isPhoto() }
    }

    ZibeTheme {
        ChatScreen(
            headerState = sampleHeaderLoaded(),
            chatState = chatState,
            myUid = "me",
            myAudioAvatarUrl = null,
            otherAudioAvatarUrl = null,
            photoList = photoList,
            mediaUiState = sampleMediaUiStatePendingAudio(),
            showPhotoSheet = false,
            onDismissPhotoSheet = {},
            callbacks = previewCallbacks()
        )
    }
}

@Preview(name = "ChatScreenScenario_RecordingActive", showBackground = true)
@Composable
private fun ChatScreenScenarioRecordingActive() {
    val recordingStates = sampleRecordingStates()
    val chatState = sampleChatStateMixedMessages().copy(isRecording = true)
    val photoList = chatState.messages.mapNotNull { item ->
        item.message.content.takeIf { item.message.type.isPhoto() }
    }

    ZibeTheme {
        ChatScreen(
            headerState = sampleHeaderLoaded(),
            chatState = chatState,
            myUid = "me",
            myAudioAvatarUrl = null,
            otherAudioAvatarUrl = null,
            photoList = photoList,
            mediaUiState = recordingStates.active,
            showPhotoSheet = false,
            onDismissPhotoSheet = {},
            callbacks = previewCallbacks()
        )
    }
}

@Preview(name = "ChatScreenScenario_RecordingCanceled", showBackground = true)
@Composable
private fun ChatScreenScenarioRecordingCanceled() {
    val recordingStates = sampleRecordingStates()
    val chatState = sampleChatStateMixedMessages().copy(isRecording = true)
    val photoList = chatState.messages.mapNotNull { item ->
        item.message.content.takeIf { item.message.type.isPhoto() }
    }

    ZibeTheme {
        ChatScreen(
            headerState = sampleHeaderLoaded(),
            chatState = chatState,
            myUid = "me",
            myAudioAvatarUrl = null,
            otherAudioAvatarUrl = null,
            photoList = photoList,
            mediaUiState = recordingStates.canceled,
            showPhotoSheet = false,
            onDismissPhotoSheet = {},
            callbacks = previewCallbacks()
        )
    }
}
