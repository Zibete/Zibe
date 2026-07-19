package com.zibete.proyecto1.domain.rooms

import com.zibete.proyecto1.model.RoomIdentity
import com.zibete.proyecto1.model.RoomIdentityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomCreationPolicyTest {
    @Test
    fun `public profile can create a room`() {
        assertNull(
            RoomCreationPolicy.validateCreator(
                RoomIdentity("Public User", RoomIdentityType.PUBLIC)
            )
        )
    }

    @Test
    fun `anonymous identity cannot create a room`() {
        assertEquals(
            RoomValidationIssue(
                field = RoomValidationField.PUBLIC_IDENTITY,
                error = RoomValidationError.INVALID_VALUE
            ),
            RoomCreationPolicy.validateCreator(
                RoomIdentity("Ghost", RoomIdentityType.ANONYMOUS)
            )
        )
    }
}
