package com.zibete.proyecto1.ui.chat.preview

import android.net.Uri
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.core.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.core.constants.Constants.MSG_INFO
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.core.constants.Constants.MSG_RECEIVED
import com.zibete.proyecto1.core.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem
import com.zibete.proyecto1.ui.chat.ChatHeaderState
import com.zibete.proyecto1.ui.chat.ChatMediaUiState
import com.zibete.proyecto1.ui.chat.ChatState

data class RecordingPreviewStates(
    val active: ChatMediaUiState,
    val canceled: ChatMediaUiState
)

fun sampleHeaderLoaded(
    name: String? = "Sofia",
    status: String = "online",
    photoUrl: String? = null,
    isBlocked: Boolean = false,
    notificationsEnabled: Boolean = true
): ChatHeaderState.Loaded = ChatHeaderState.Loaded(
    name = name,
    status = status,
    photoUrl = photoUrl,
    isBlocked = isBlocked,
    notificationsEnabled = notificationsEnabled
)

fun sampleBlockedHeader(): ChatHeaderState.Loaded =
    sampleHeaderLoaded(isBlocked = true)

fun sampleChatStateEmpty(): ChatState = ChatState()

fun sampleChatStateMixedMessages(): ChatState =
    ChatState(messages = sampleMessages())

fun sampleSelectionState(): ChatState {
    val base = sampleChatStateMixedMessages()
    val selected = base.messages.take(2).map { it.id }.toSet()
    return base.copy(selectedIds = selected)
}

fun samplePhotoUploadingState(): ChatState =
    sampleChatStateMixedMessages().copy(
        photoReady = true,
        pendingPhotoUri = Uri.parse("content://preview/photo"),
        pendingFileUrl = null
    )

fun sampleMediaUiStatePendingAudio(): ChatMediaUiState =
    ChatMediaUiState(
        recordingElapsedMs = 0L,
        isRecordingCanceled = false,
        isAudioUploading = false
    )

fun sampleRecordingStates(): RecordingPreviewStates =
    RecordingPreviewStates(
        active = ChatMediaUiState(
            recordingElapsedMs = 15000L,
            isRecordingCanceled = false,
            isAudioUploading = false
        ),
        canceled = ChatMediaUiState(
            recordingElapsedMs = 8000L,
            isRecordingCanceled = true,
            isAudioUploading = false
        )
    )

fun sampleMediaUiStateIdle(): ChatMediaUiState =
    ChatMediaUiState(
        recordingElapsedMs = 0L,
        isRecordingCanceled = false,
        isAudioUploading = false
    )

private const val PREVIEW_BASE_TIME = 1_700_000_000_000L

private fun sampleMessages(): List<ChatMessageItem> = listOf(
    messageItem(
        id = "m1",
        senderUid = "other",
        type = MSG_TEXT,
        content = "Hello",
        seen = MSG_RECEIVED,
        createdAt = PREVIEW_BASE_TIME - 600_000L
    ),
    messageItem(
        id = "m2",
        senderUid = "me",
        type = MSG_TEXT,
        content = "Hey",
        seen = MSG_SEEN,
        createdAt = PREVIEW_BASE_TIME - 540_000L
    ),
    messageItem(
        id = "m3",
        senderUid = "me",
        type = MSG_PHOTO,
        content = "https://example.com/photo.jpg",
        seen = MSG_DELIVERED,
        createdAt = PREVIEW_BASE_TIME - 480_000L
    ),
    messageItem(
        id = "m4",
        senderUid = "other",
        type = MSG_AUDIO,
        content = "https://example.com/audio.mp3",
        seen = MSG_RECEIVED,
        createdAt = PREVIEW_BASE_TIME - 420_000L,
        audioDurationMs = 12_000L
    ),
    messageItem(
        id = "m5",
        senderUid = "me",
        type = MSG_INFO,
        content = "Today",
        seen = MSG_DELIVERED,
        createdAt = PREVIEW_BASE_TIME - 360_000L
    )
)

private fun messageItem(
    id: String,
    senderUid: String,
    type: Int,
    content: String,
    seen: Int,
    createdAt: Long,
    audioDurationMs: Long = 0L
): ChatMessageItem = ChatMessageItem(
    id = id,
    message = ChatMessage(
        content = content,
        createdAt = createdAt,
        audioDurationMs = audioDurationMs,
        senderUid = senderUid,
        type = type,
        seen = seen
    )
)
