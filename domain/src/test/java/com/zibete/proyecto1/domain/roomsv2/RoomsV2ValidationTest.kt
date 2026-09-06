package com.zibete.proyecto1.domain.roomsv2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RoomsV2ValidationTest {
    @Test
    fun `alias normalization is stable and accent preserving`() {
        assertEquals("álias uno", RoomsV2Validation.normalizeAlias("  ÁlIaS   Uno  "))
        assertNull(RoomsV2Validation.validateAlias("Álias Uno"))
    }

    @Test
    fun `alias rejects firebase path characters`() {
        assertNotNull(RoomsV2Validation.validateAlias("alias/profile"))
    }

    @Test
    fun `room name collapses whitespace and validates limits`() {
        assertEquals("Sala Norte", RoomsV2Validation.collapseSpaces("  Sala   Norte "))
        assertNull(RoomsV2Validation.validateRoomName("Sala Norte"))
        assertNotNull(RoomsV2Validation.validateRoomName("ab"))
    }

    @Test
    fun `client message id must be path safe`() {
        assertNull(RoomsV2Validation.validateClientMessageId("android-001"))
        assertNotNull(RoomsV2Validation.validateClientMessageId("bad/id"))
    }
}
