package com.zibete.proyecto1.model

import com.zibete.proyecto1.core.constants.Constants
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOCKED
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationVisibilityTest {

    @Test
    fun isVisible_returnsTrue_forDmWithPhoto() {
        val chat = Conversation(
            otherPhotoUrl = "photo",
            state = NODE_DM
        )

        assertTrue(chat.isVisible())
    }

    @Test
    fun isVisible_returnsTrue_forSilentWithPhoto() {
        val chat = Conversation(
            otherPhotoUrl = "photo",
            state = CHAT_STATE_SILENT
        )

        assertTrue(chat.isVisible())
    }

    @Test
    fun isVisible_returnsFalse_forEmptyPhoto_orNonVisibleState() {
        val noPhoto = Conversation(
            otherPhotoUrl = Constants.EMPTY,
            state = NODE_DM
        )
        val blocked = Conversation(
            otherPhotoUrl = "photo",
            state = CHAT_STATE_BLOCKED
        )

        assertFalse(noPhoto.isVisible())
        assertFalse(blocked.isVisible())
    }
}
