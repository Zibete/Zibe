package com.zibete.proyecto1.ui.groups.host

import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomV2MessagePagingTest {

    @Test
    fun `merge keeps history sorted and live updates win by message id`() {
        val current = listOf(
            message("m3", seq = 3, text = "old value"),
            message("m4", seq = 4, text = "four"),
        )
        val updates = listOf(
            message("m1", seq = 1, text = "one"),
            message("m2", seq = 2, text = "two"),
            message("m3", seq = 3, text = "removed", removed = true),
        )

        val merged = mergeRoomV2Messages(current, updates)

        assertEquals(listOf(1L, 2L, 3L, 4L), merged.map { it.seq })
        assertEquals("removed", merged.first { it.messageId == "m3" }.text)
        assertTrue(merged.first { it.messageId == "m3" }.removed)
    }

    @Test
    fun `earlier availability is derived from the first visible sequence`() {
        assertFalse(hasEarlierRoomV2Messages(emptyList()))
        assertFalse(hasEarlierRoomV2Messages(listOf(message("m1", seq = 1))))
        assertTrue(hasEarlierRoomV2Messages(listOf(message("m42", seq = 42))))
    }

    private fun message(
        id: String,
        seq: Long,
        text: String = id,
        removed: Boolean = false,
    ) = RoomV2Message(
        messageId = id,
        roomId = "room-1",
        authorIdentityId = "identity-1",
        authorDisplayName = "User",
        authorMode = RoomV2IdentityMode.REAL,
        text = text,
        sentAt = seq * 1_000,
        seq = seq,
        removed = removed,
    )
}
