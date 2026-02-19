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
    fun isVisible_returnsTrue_forDm_whenThreadNotEmpty() {
        val chat = Conversation(
            state = NODE_DM,
            lastMessageAt = 1L,
            lastContent = "hi"
        )
        assertTrue(chat.isVisible())
    }

    @Test
    fun isVisible_returnsTrue_forSilent_whenThreadNotEmpty() {
        val chat = Conversation(
            state = CHAT_STATE_SILENT,
            lastMessageAt = 1L,
            lastContent = "hi"
        )
        assertTrue(chat.isVisible())
    }

    @Test
    fun isVisible_returnsFalse_forBlocked_evenIfThreadNotEmpty() {
        val chat = Conversation(
            state = CHAT_STATE_BLOCKED,
            lastMessageAt = 1L,
            lastContent = "hi"
        )
        assertFalse(chat.isVisible())
    }

    @Test
    fun isVisible_returnsFalse_forHide_evenIfThreadNotEmpty() {
        val chat = Conversation(
            state = Constants.CHAT_STATE_HIDE,
            lastMessageAt = 1L,
            lastContent = "hi"
        )
        assertFalse(chat.isVisible())
    }

    @Test
    fun isVisible_returnsFalse_forEmptyThread_evenIfDmOrSilent() {
        val dmEmpty = Conversation(
            state = NODE_DM,
            lastMessageAt = 0L,
            lastContent = ""
        )
        val silentEmpty = Conversation(
            state = CHAT_STATE_SILENT,
            lastMessageAt = 0L,
            lastContent = ""
        )

        assertFalse(dmEmpty.isVisible())
        assertFalse(silentEmpty.isVisible())
    }

    @Test
    fun isVisible_returnsFalse_forUnknownState() {
        val chat = Conversation(
            state = "unknown_state",
            lastMessageAt = 1L,
            lastContent = "hi"
        )
        assertFalse(chat.isVisible())
    }
}
