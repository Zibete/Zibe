package com.zibete.proyecto1.ui.groups.host

import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.model.ChatGroup
import com.zibete.proyecto1.model.ChatGroupItem
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.GroupChatChildEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupHostUiStateTest {

    @Test
    fun `added messages stay ordered deduplicated and bounded`() {
        val initial = activeState().copy(
            maxChatSize = 2,
            messages = listOf(message("second", 20L))
        )

        val withFirst = initial.reduce(GroupChatChildEvent.Added(message("first", 10L)))
        val withThird = withFirst.reduce(GroupChatChildEvent.Added(message("third", 30L)))
        val duplicate = withThird.reduce(
            GroupChatChildEvent.Added(message("third", 30L, content = "updated"))
        )

        assertEquals(listOf("second", "third"), duplicate.messages.map { it.id })
        assertEquals("updated", duplicate.messages.last().message.content)
    }

    @Test
    fun `changed and removed events target only the matching message`() {
        val first = message("first", 10L)
        val second = message("second", 20L)
        val state = activeState().copy(messages = listOf(first, second))

        val changed = state.reduce(
            GroupChatChildEvent.Changed(second.copy(message = second.message.copy(content = "new")))
        )
        val removed = changed.reduce(GroupChatChildEvent.Removed(first))

        assertEquals(listOf("second"), removed.messages.map { it.id })
        assertEquals("new", removed.messages.single().message.content)
    }

    @Test
    fun `active room lease requires loaded visible public chat`() {
        val visibleChat = activeState()

        assertTrue(shouldExposeActiveRoom(visibleChat))
        assertFalse(shouldExposeActiveRoom(visibleChat.copy(isLoading = true)))
        assertFalse(shouldExposeActiveRoom(visibleChat.copy(loadError = true)))
        assertFalse(shouldExposeActiveRoom(visibleChat.copy(isScreenVisible = false)))
        assertFalse(
            shouldExposeActiveRoom(visibleChat.copy(selectedTab = GroupHostTab.USERS))
        )
        assertFalse(
            shouldExposeActiveRoom(visibleChat.copy(selectedTab = GroupHostTab.PRIVATE_CHATS))
        )
    }

    @Test
    fun `private unread is independent and never negative`() {
        val state = activeState().copy(
            publicUnread = 7,
            privateConversations = listOf(
                Conversation(otherId = "one", unreadCount = 3),
                Conversation(otherId = "two", unreadCount = -4)
            )
        )

        assertEquals(7, state.publicUnread)
        assertEquals(3, state.privateUnread)
    }

    private fun activeState() = GroupHostUiState(
        isLoading = false,
        loadError = false,
        groupContext = GroupContext(
            inGroup = true,
            groupName = "Legacy room",
            userName = "Ada",
            userType = 1,
            roomKey = "room-key",
            displayName = "Sala Kotlin"
        ),
        currentUid = "me",
        selectedTab = GroupHostTab.GROUP_CHAT,
        isScreenVisible = true
    )

    private fun message(
        id: String,
        timestamp: Long,
        content: String = id
    ) = ChatGroupItem(
        id = id,
        message = ChatGroup(
            content = content,
            timestamp = timestamp,
            nameUser = "Ada",
            senderUid = "other",
            chatType = MSG_TEXT,
            userType = 1
        )
    )
}
