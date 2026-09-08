package com.zibete.proyecto1.core.notifications

import com.zibete.proyecto1.core.constants.Constants.PayloadKeys

data class RoomsV2NotificationPayload(
    val type: String,
    val roomId: String,
    val conversationId: String?,
    val messageId: String,
    val roomName: String?,
    val senderName: String?,
    val preview: String?,
)

object RoomsV2NotificationContract {
    const val TYPE_ROOM = "room_v2"
    const val TYPE_PRIVATE = "room_private_v2"

    const val PAYLOAD_ROOM_ID = "roomId"
    const val PAYLOAD_CONVERSATION_ID = "conversationId"
    const val PAYLOAD_ROOM_NAME = "roomName"
    const val PAYLOAD_PREVIEW = "preview"

    fun isRoomsV2Type(type: String?): Boolean =
        type == TYPE_ROOM || type == TYPE_PRIVATE

    fun parse(data: Map<String, String>): RoomsV2NotificationPayload? {
        val type = data[PayloadKeys.TYPE]?.trim().orEmpty()
        if (!isRoomsV2Type(type)) return null

        val roomId = data[PAYLOAD_ROOM_ID]?.trim().orEmpty()
        val messageId = data[PayloadKeys.MESSAGE_ID]?.trim().orEmpty()
        if (roomId.isBlank() || messageId.isBlank()) return null

        val conversationId = data[PAYLOAD_CONVERSATION_ID]
            ?.trim()
            ?.takeIf(String::isNotBlank)
        if (type == TYPE_PRIVATE && conversationId == null) return null

        return RoomsV2NotificationPayload(
            type = type,
            roomId = roomId,
            conversationId = if (type == TYPE_PRIVATE) conversationId else null,
            messageId = messageId,
            roomName = data[PAYLOAD_ROOM_NAME]?.trim()?.takeIf(String::isNotBlank),
            senderName = data[PayloadKeys.SENDER_NAME]?.trim()?.takeIf(String::isNotBlank),
            preview = data[PAYLOAD_PREVIEW]?.trim()?.takeIf(String::isNotBlank),
        )
    }
}
