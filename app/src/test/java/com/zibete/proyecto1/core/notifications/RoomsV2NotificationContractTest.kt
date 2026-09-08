package com.zibete.proyecto1.core.notifications

import com.zibete.proyecto1.core.constants.Constants.PayloadKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomsV2NotificationContractTest {

    @Test
    fun `parses public room payload without conversation`() {
        val payload = RoomsV2NotificationContract.parse(
            mapOf(
                PayloadKeys.TYPE to RoomsV2NotificationContract.TYPE_ROOM,
                RoomsV2NotificationContract.PAYLOAD_ROOM_ID to "room_1",
                PayloadKeys.MESSAGE_ID to "message_1",
                PayloadKeys.SENDER_NAME to "Alias",
                RoomsV2NotificationContract.PAYLOAD_PREVIEW to "Hola",
            )
        )

        requireNotNull(payload)
        assertEquals("room_1", payload.roomId)
        assertEquals("message_1", payload.messageId)
        assertNull(payload.conversationId)
        assertEquals("Alias", payload.senderName)
        assertEquals("Hola", payload.preview)
    }

    @Test
    fun `parses private payload only when contextual conversation is present`() {
        val payload = RoomsV2NotificationContract.parse(
            mapOf(
                PayloadKeys.TYPE to RoomsV2NotificationContract.TYPE_PRIVATE,
                RoomsV2NotificationContract.PAYLOAD_ROOM_ID to "room_1",
                RoomsV2NotificationContract.PAYLOAD_CONVERSATION_ID to "conversation_1",
                PayloadKeys.MESSAGE_ID to "message_2",
            )
        )

        requireNotNull(payload)
        assertEquals("conversation_1", payload.conversationId)
    }

    @Test
    fun `rejects private payload without conversation id`() {
        val payload = RoomsV2NotificationContract.parse(
            mapOf(
                PayloadKeys.TYPE to RoomsV2NotificationContract.TYPE_PRIVATE,
                RoomsV2NotificationContract.PAYLOAD_ROOM_ID to "room_1",
                PayloadKeys.MESSAGE_ID to "message_2",
            )
        )

        assertNull(payload)
    }

    @Test
    fun `rejects payload without room or message id`() {
        assertNull(
            RoomsV2NotificationContract.parse(
                mapOf(
                    PayloadKeys.TYPE to RoomsV2NotificationContract.TYPE_ROOM,
                    PayloadKeys.MESSAGE_ID to "message_1",
                )
            )
        )
        assertNull(
            RoomsV2NotificationContract.parse(
                mapOf(
                    PayloadKeys.TYPE to RoomsV2NotificationContract.TYPE_ROOM,
                    RoomsV2NotificationContract.PAYLOAD_ROOM_ID to "room_1",
                )
            )
        )
    }

    @Test
    fun `does not reinterpret legacy dm as RoomsV2`() {
        assertTrue(!RoomsV2NotificationContract.isRoomsV2Type("dm"))
        assertNull(
            RoomsV2NotificationContract.parse(
                mapOf(
                    PayloadKeys.TYPE to "dm",
                    RoomsV2NotificationContract.PAYLOAD_ROOM_ID to "room_1",
                    PayloadKeys.MESSAGE_ID to "message_1",
                )
            )
        )
    }
}
