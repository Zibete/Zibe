package com.zibete.proyecto1.ui.chat

import com.zibete.proyecto1.model.ChatMessageItem
import java.time.LocalDate

data class ChatMediaUiState(
    val recordingElapsedMs: Long,
    val isRecordingCanceled: Boolean,
    val isAudioUploading: Boolean
)

data class ChatCallbacks(
    val onBackClick: () -> Unit,
    val onProfileClick: () -> Unit,
    val onToggleNotifications: () -> Unit,
    val onToggleBlock: () -> Unit,
    val onDeleteChoiceMode: () -> Unit,
    val onConfirmHide: () -> Unit,
    val onDeleteSelected: () -> Unit,
    val onClearSelection: () -> Unit,
    val onPhotoSourceClick: () -> Unit,
    val onLaunchCamera: () -> Unit,
    val onLaunchGallery: () -> Unit,
    val onRemovePendingPhoto: () -> Unit,
    val onTextChanged: (String) -> Unit,
    val onSendText: (String) -> Unit,
    val onSendPhoto: (String) -> Unit,
    val onSendAudio: (String, Long) -> Unit,
    val onSelectionChanged: (ChatMessageItem, Boolean) -> Unit,
    val onMicPressed: () -> Unit,
    val onMicMoved: (Float, Float, Float) -> Unit,
    val onMicReleased: () -> Unit
)

sealed interface ChatTimelineItem {
    val key: String
    val createdAt: Long
    val contentType: String

    data class DateSeparator(
        val date: LocalDate,
        val text: String,
        override val createdAt: Long
    ) : ChatTimelineItem {
        override val key: String = "date-$date"
        override val contentType: String = "date_separator"
    }

    data class InfoMessage(
        val item: ChatMessageItem
    ) : ChatTimelineItem {
        override val key: String = "info-${item.id}"
        override val createdAt: Long = item.message.createdAt
        override val contentType: String = "info_message"
    }

    data class Message(
        val item: ChatMessageItem
    ) : ChatTimelineItem {
        override val key: String = "message-${item.id}"
        override val createdAt: Long = item.message.createdAt
        override val contentType: String = "message"
    }
}
