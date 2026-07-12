package com.zibete.proyecto1.ui.chat

import com.zibete.proyecto1.core.constants.Constants.MSG_INFO
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_RECEIVER_DLT
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTimelineTest {

    @Test
    fun buildChatTimeline_sortsMessagesAndAddsOneSeparatorPerDate() {
        val messages = listOf(
            item("second", createdAt = 2_000L),
            item("first", createdAt = 1_000L)
        )

        val timeline = buildChatTimeline(messages, CURRENT_UID)

        assertEquals(3, timeline.size)
        assertTrue(timeline[0] is ChatTimelineItem.DateSeparator)
        assertEquals("first", (timeline[1] as ChatTimelineItem.Message).item.id)
        assertEquals("second", (timeline[2] as ChatTimelineItem.Message).item.id)
    }

    @Test
    fun buildChatTimeline_mapsInfoMessagesToDedicatedItem() {
        val timeline = buildChatTimeline(
            messages = listOf(item("info", createdAt = 1_000L, type = MSG_INFO)),
            currentUid = CURRENT_UID
        )

        assertTrue(timeline.last() is ChatTimelineItem.InfoMessage)
    }

    @Test
    fun buildChatTimeline_excludesMessagesDeletedForCurrentParticipant() {
        val timeline = buildChatTimeline(
            messages = listOf(
                item(
                    id = "deleted",
                    createdAt = 1_000L,
                    type = MSG_TEXT_RECEIVER_DLT,
                    senderUid = OTHER_UID
                )
            ),
            currentUid = CURRENT_UID
        )

        assertTrue(timeline.isEmpty())
    }

    private fun item(
        id: String,
        createdAt: Long,
        type: Int = MSG_TEXT,
        senderUid: String = CURRENT_UID
    ) = ChatMessageItem(
        id = id,
        message = ChatMessage(
            content = id,
            createdAt = createdAt,
            senderUid = senderUid,
            type = type
        )
    )

    private companion object {
        const val CURRENT_UID = "current"
        const val OTHER_UID = "other"
    }
}
