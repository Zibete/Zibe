package com.zibete.proyecto1.domain.chat

import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOCKED
import com.zibete.proyecto1.core.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.data.ChatRepositoryContract
import com.zibete.proyecto1.model.Conversation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendChatMessageUseCaseTest {

    private val repository = mockk<ChatRepositoryContract>(relaxed = true)
    private val useCase = DefaultSendChatMessageUseCase(repository)

    @Test
    fun `DM uses atomic fan-out and preserves delivered plus unread contract`() = runTest {
        coEvery { repository.getConversation("sender", "receiver", NODE_DM) } returns
            Conversation(state = NODE_DM)
        coEvery { repository.getConversation("receiver", "sender", NODE_DM) } returns
            Conversation(state = NODE_DM, unreadCount = 4)
        coEvery {
            repository.sendDmMessageWithConversations(any(), any(), any(), any(), any())
        } returns ZibeResult.Success(Unit)

        val outcome = useCase.execute(command()).getOrThrow()

        assertEquals(SendChatMessageOutcome.Sent, outcome)
        coVerify(exactly = 1) {
            repository.sendDmMessageWithConversations(
                "sender",
                "receiver",
                match { it.seen == MSG_DELIVERED && it.senderUid == "sender" },
                match { it.unreadCount == 0 && it.seen == MSG_DELIVERED },
                match { it.unreadCount == 5 }
            )
        }
        coVerify(exactly = 0) { repository.pushMessageToChat(any(), any()) }
    }

    @Test
    fun `blocked recipient prevents every write`() = runTest {
        coEvery { repository.getConversation("sender", "receiver", NODE_DM) } returns null
        coEvery { repository.getConversation("receiver", "sender", NODE_DM) } returns
            Conversation(state = CHAT_STATE_BLOCKED)

        val outcome = useCase.execute(command()).getOrThrow()

        assertEquals(SendChatMessageOutcome.BlockedByRecipient, outcome)
        coVerify(exactly = 0) {
            repository.sendDmMessageWithConversations(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) { repository.saveConversation(any(), any(), any(), any()) }
    }

    @Test
    fun `group dm preserves node and room context in atomic fan-out`() = runTest {
        coEvery { repository.getConversation("sender", "receiver", NODE_GROUP_DM) } returns
            Conversation(state = NODE_GROUP_DM, roomKey = "room-1")
        coEvery { repository.getConversation("receiver", "sender", NODE_GROUP_DM) } returns
            Conversation(state = NODE_GROUP_DM, unreadCount = 2, roomKey = "room-1")
        coEvery {
            repository.sendGroupDmMessageWithConversations(any(), any(), any(), any(), any(), any())
        } returns ZibeResult.Success(Unit)

        val outcome = useCase.execute(
            command().copy(nodeType = NODE_GROUP_DM, roomKey = "room-1")
        ).getOrThrow()

        assertEquals(SendChatMessageOutcome.Sent, outcome)
        coVerify(exactly = 1) {
            repository.sendGroupDmMessageWithConversations(
                "sender",
                "receiver",
                "room-1",
                match { it.roomKey == "room-1" && it.senderName == "Sender" },
                match { it.state == NODE_GROUP_DM && it.roomKey == "room-1" },
                match {
                    it.state == NODE_GROUP_DM &&
                        it.roomKey == "room-1" &&
                        it.unreadCount == 3
                }
            )
        }
        coVerify(exactly = 0) {
            repository.sendDmMessageWithConversations(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `group dm without room key fails before any write`() = runTest {
        coEvery { repository.getConversation(any(), any(), NODE_GROUP_DM) } returns null

        val result = useCase.execute(command().copy(nodeType = NODE_GROUP_DM))

        assertTrue(result is ZibeResult.Failure)
        coVerify(exactly = 0) {
            repository.sendGroupDmMessageWithConversations(any(), any(), any(), any(), any(), any())
        }
    }

    private fun command() = SendChatMessageCommand(
        senderUid = "sender",
        receiverUid = "receiver",
        nodeType = NODE_DM,
        messageType = MSG_TEXT,
        content = "hello",
        audioDurationMs = 0,
        createdAt = 123,
        senderConversationContent = "hello",
        receiverConversationContent = "hello",
        receiverName = "Receiver",
        receiverPhotoUrl = "receiver-photo",
        senderName = "Sender",
        senderPhotoUrl = "sender-photo"
    )
}
