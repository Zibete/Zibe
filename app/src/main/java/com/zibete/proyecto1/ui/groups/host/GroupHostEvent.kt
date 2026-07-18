package com.zibete.proyecto1.ui.groups.host

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed interface GroupHostEvent {
    data class ShowSnack(
        val message: UiText,
        val type: ZibeSnackType
    ) : GroupHostEvent

    data class OpenPrivateChat(val otherUid: String) : GroupHostEvent

    data class OpenProfile(val userId: String) : GroupHostEvent
}
