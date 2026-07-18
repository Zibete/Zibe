package com.zibete.proyecto1.model

import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO_SENDER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO_BOTH_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO_SENDER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO_BOTH_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_SENDER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_BOTH_DLT
import java.io.Serializable

sealed class ChatChildEvent {
    data class Added(val item: ChatMessageItem) : ChatChildEvent()
    data class Changed(val item: ChatMessageItem) : ChatChildEvent()
    data class Removed(val item: ChatMessageItem) : ChatChildEvent()
}
data class ChatMessageItem(
    val id: String,
    val message: ChatMessage
)
data class ChatMessage(
    var content: String = "",
    var createdAt: Long = 0L,
    var audioDurationMs: Long = 0L,
    var senderUid: String = "",
    var type: Int = 0,
    var seen: Int = 0,
    var roomKey: String = "",
    var senderName: String = ""
) : Serializable

fun ChatMessage.isDeletedFor(currentUid: String): Boolean =
    if (currentUid.isBlank()) {
        false
    } else if (senderUid == currentUid) {
        type in senderDeletedTypes
    } else {
        type in receiverDeletedTypes
    }

fun ChatMessage.isVisibleFor(currentUid: String): Boolean = !isDeletedFor(currentUid)

private val senderDeletedTypes = setOf(
    MSG_TEXT_SENDER_DLT,
    MSG_PHOTO_SENDER_DLT,
    MSG_AUDIO_SENDER_DLT,
    MSG_TEXT_BOTH_DLT,
    MSG_PHOTO_BOTH_DLT,
    MSG_AUDIO_BOTH_DLT
)

private val receiverDeletedTypes = setOf(
    MSG_TEXT_RECEIVER_DLT,
    MSG_PHOTO_RECEIVER_DLT,
    MSG_AUDIO_RECEIVER_DLT,
    MSG_TEXT_BOTH_DLT,
    MSG_PHOTO_BOTH_DLT,
    MSG_AUDIO_BOTH_DLT
)
