package com.zibete.proyecto1.model

import com.zibete.proyecto1.core.constants.Constants.ANONYMOUS_USER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomLegacyCompatibilityTest {
    @Test
    fun `legacy group uses persisted key and legacy name as display fallback`() {
        val room = Groups(name = "Legacy Room")

        val compatible = room.withLegacyFallback("legacy-path-key")

        assertEquals("legacy-path-key", compatible.roomKey)
        assertEquals("Legacy Room", compatible.displayName)
        assertEquals("legacy-path-key", compatible.resolvedRoomKey())
    }

    @Test
    fun `new room keeps stable room id independent from display name`() {
        val room = Groups(
            name = "Visible name",
            roomId = "room-stable-id",
            displayName = "Renamed room"
        )

        assertEquals("room-stable-id", room.resolvedRoomKey())
        assertEquals("Renamed room", room.resolvedDisplayName())
    }

    @Test
    fun `legacy group message resolves old author field`() {
        val message = ChatGroup(nameUser = "Legacy alias")

        assertEquals("Legacy alias", message.resolvedUserName())
        assertEquals(message, ChatGroup(userName = "Legacy alias"))
    }

    @Test
    fun `anonymous member exposes alias without uid fallback`() {
        val member = UserGroup(
            userId = "secret-uid",
            userName = "old-alias",
            type = ANONYMOUS_USER,
            alias = "Public alias"
        )

        assertTrue(member.isAnonymous)
        assertEquals("Public alias", member.resolvedDisplayName())
    }
}
