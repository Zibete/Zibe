package com.zibete.proyecto1.domain.rooms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomValidatorTest {
    @Test
    fun `room name trims and creates accent independent index key`() {
        val result = RoomValidator.validateRoomName("  Cámara Norte  ")

        assertEquals(
            RoomTextValidation.Valid("Cámara Norte", "camara-norte"),
            result
        )
    }

    @Test
    fun `room name rejects firebase path characters`() {
        val result = RoomValidator.validateRoomName("Room#unsafe")

        assertEquals(
            RoomTextValidation.Invalid(
                RoomValidationIssue(
                    RoomValidationField.ROOM_NAME,
                    RoomValidationError.INVALID_CHARACTERS
                )
            ),
            result
        )
    }

    @Test
    fun `alias enforces minimum length and safe characters`() {
        assertEquals(
            RoomTextValidation.Invalid(
                RoomValidationIssue(RoomValidationField.ALIAS, RoomValidationError.TOO_SHORT)
            ),
            RoomValidator.validateAlias("ab")
        )
        assertTrue(RoomValidator.validateAlias("Alias válido") is RoomTextValidation.Valid)
        assertEquals(
            RoomValidationError.INVALID_CHARACTERS,
            (RoomValidator.validateAlias("alias/profile") as RoomTextValidation.Invalid)
                .issue.error
        )
    }

    @Test
    fun `description rejects control characters but allows line breaks`() {
        assertTrue(
            RoomValidator.validateDescription("Line one\nLine two") is RoomTextValidation.Valid
        )
        assertEquals(
            RoomValidationError.INVALID_CHARACTERS,
            (RoomValidator.validateDescription("unsafe\u0001value") as RoomTextValidation.Invalid)
                .issue.error
        )
    }
}
