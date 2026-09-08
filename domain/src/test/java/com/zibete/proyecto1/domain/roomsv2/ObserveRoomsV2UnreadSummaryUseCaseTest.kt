package com.zibete.proyecto1.domain.roomsv2

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveRoomsV2UnreadSummaryUseCaseTest {

    private val roomsRepository = mockk<RoomsV2Repository>()
    private val chatRepository = mockk<RoomsV2ChatRepository>()
    private val useCase = ObserveRoomsV2UnreadSummaryUseCase(
        roomsRepository = roomsRepository,
        chatRepository = chatRepository,
    )

    @Test
    fun `aggregates public and contextual private unread across active memberships`() = runTest {
        every { roomsRepository.observeMemberships() } returns flowOf(
            listOf(
                membership("room_1", unread = 2),
                membership("room_2", unread = 3),
            )
        )
        every { chatRepository.observeConversations("room_1") } returns flowOf(
            listOf(conversation("room_1", "conversation_1", unread = 4))
        )
        every { chatRepository.observeConversations("room_2") } returns flowOf(
            listOf(
                conversation("room_2", "conversation_2", unread = 1),
                conversation("room_2", "conversation_3", unread = 2),
            )
        )

        val summary = useCase().first()

        assertEquals(5, summary.publicUnread)
        assertEquals(7, summary.privateUnread)
        assertEquals(12, summary.totalUnread)
    }

    @Test
    fun `empty membership set clears RoomsV2 badge`() = runTest {
        every { roomsRepository.observeMemberships() } returns flowOf(emptyList())

        assertEquals(RoomsV2UnreadSummary(), useCase().first())
    }

    @Test
    fun `one inaccessible private index does not cancel badges for other rooms`() = runTest {
        every { roomsRepository.observeMemberships() } returns flowOf(
            listOf(
                membership("room_1", unread = 2),
                membership("room_2", unread = 1),
            )
        )
        every { chatRepository.observeConversations("room_1") } returns flow {
            throw IllegalStateException("permission transition")
        }
        every { chatRepository.observeConversations("room_2") } returns flowOf(
            listOf(conversation("room_2", "conversation_2", unread = 5))
        )

        val summary = useCase().first()

        assertEquals(3, summary.publicUnread)
        assertEquals(5, summary.privateUnread)
        assertEquals(8, summary.totalUnread)
    }

    private fun membership(roomId: String, unread: Long): RoomV2Membership =
        RoomV2Membership(
            roomId = roomId,
            identity = identity("me_$roomId"),
            joinedAt = 1L,
            lastReadAt = 0L,
            unreadCount = unread,
        )

    private fun conversation(
        roomId: String,
        conversationId: String,
        unread: Long,
    ): RoomV2Conversation = RoomV2Conversation(
        conversationId = conversationId,
        roomId = roomId,
        otherIdentity = identity("other_$conversationId"),
        closed = false,
        blocked = false,
        lastText = "",
        updatedAt = 1L,
        unreadCount = unread,
    )

    private fun identity(identityId: String): RoomV2Identity = RoomV2Identity(
        identityId = identityId,
        displayName = identityId,
        mode = RoomV2IdentityMode.REAL,
        role = RoomV2Role.MEMBER,
        active = true,
    )
}
