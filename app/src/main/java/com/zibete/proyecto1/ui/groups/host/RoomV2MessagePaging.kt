package com.zibete.proyecto1.ui.groups.host

import com.zibete.proyecto1.domain.roomsv2.RoomV2Message

internal fun mergeRoomV2Messages(
    current: List<RoomV2Message>,
    updates: List<RoomV2Message>,
): List<RoomV2Message> {
    if (current.isEmpty()) return updates.sortedBy(RoomV2Message::seq)
    if (updates.isEmpty()) return current.sortedBy(RoomV2Message::seq)

    val byId = LinkedHashMap<String, RoomV2Message>(current.size + updates.size)
    current.forEach { message -> byId[message.messageId] = message }
    updates.forEach { message -> byId[message.messageId] = message }
    return byId.values.sortedBy(RoomV2Message::seq)
}

internal fun hasEarlierRoomV2Messages(messages: List<RoomV2Message>): Boolean =
    (messages.firstOrNull()?.seq ?: 0L) > 1L
