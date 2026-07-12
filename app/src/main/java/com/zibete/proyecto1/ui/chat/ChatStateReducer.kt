package com.zibete.proyecto1.ui.chat

import com.zibete.proyecto1.core.constants.Constants.MAX_CHAT_SIZE
import com.zibete.proyecto1.model.ChatChildEvent
import com.zibete.proyecto1.model.isDeletedFor

fun ChatState.reduce(event: ChatChildEvent, myUid: String): ChatState = when (event) {
    is ChatChildEvent.Added -> {
        if (event.item.message.isDeletedFor(myUid)) {
            copy(selectedIds = selectedIds - event.item.id)
        } else {
            copy(messages = (messages + event.item).takeLast(MAX_CHAT_SIZE))
        }
    }

    is ChatChildEvent.Changed -> {
        if (event.item.message.isDeletedFor(myUid)) {
            copy(
                messages = messages.filterNot { it.id == event.item.id },
                selectedIds = selectedIds - event.item.id
            )
        } else {
            copy(
                messages = messages.map { current ->
                    if (current.id == event.item.id) event.item else current
                }
            )
        }
    }

    is ChatChildEvent.Removed -> copy(
        messages = messages.filterNot { it.id == event.item.id },
        selectedIds = selectedIds - event.item.id
    )
}
