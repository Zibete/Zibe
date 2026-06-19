package com.zibete.proyecto1.ui.chat

import com.zibete.proyecto1.core.constants.Constants.MSG_INFO
import com.zibete.proyecto1.core.utils.TimeUtils
import com.zibete.proyecto1.model.ChatMessageItem
import java.time.LocalDate

fun buildChatTimeline(messages: List<ChatMessageItem>): List<ChatTimelineItem> {
    val sortedMessages = messages.sortedBy { item -> item.message.createdAt }
    val timeline = mutableListOf<ChatTimelineItem>()
    var currentDate: LocalDate? = null

    sortedMessages.forEach { item ->
        val createdAt = item.message.createdAt
        if (createdAt > 0L) {
            val itemDate = TimeUtils.zonedDateTime(createdAt).toLocalDate()
            if (itemDate != currentDate) {
                timeline += ChatTimelineItem.DateSeparator(
                    date = itemDate,
                    text = TimeUtils.formatHeaderDate(createdAt),
                    createdAt = createdAt
                )
                currentDate = itemDate
            }
        }

        timeline += if (item.message.type == MSG_INFO) {
            ChatTimelineItem.InfoMessage(item)
        } else {
            ChatTimelineItem.Message(item)
        }
    }

    return timeline
}
