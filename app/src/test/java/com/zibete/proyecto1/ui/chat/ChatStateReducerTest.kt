package com.zibete.proyecto1.ui.chat

import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_RECEIVER_DLT
import com.zibete.proyecto1.model.ChatChildEvent
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatStateReducerTest {

    @Test
    fun `changed message deleted for me leaves messages and selection coherent`() {
        val item = ChatMessageItem("message", ChatMessage(senderUid = "other"))
        val state = ChatState(messages = listOf(item), selectedIds = setOf(item.id))
        val deleted = item.copy(message = item.message.copy(type = MSG_TEXT_RECEIVER_DLT))

        val reduced = state.reduce(ChatChildEvent.Changed(deleted), myUid = "me")

        assertFalse(reduced.messages.any { it.id == item.id })
        assertFalse(item.id in reduced.selectedIds)
    }

    @Test
    fun `removed event clears the same id without affecting other messages`() {
        val first = ChatMessageItem("first", ChatMessage())
        val second = ChatMessageItem("second", ChatMessage())
        val state = ChatState(messages = listOf(first, second), selectedIds = setOf(first.id))

        val reduced = state.reduce(ChatChildEvent.Removed(first), myUid = "me")

        assertEquals(listOf(second), reduced.messages)
        assertEquals(emptySet<String>(), reduced.selectedIds)
    }
}
