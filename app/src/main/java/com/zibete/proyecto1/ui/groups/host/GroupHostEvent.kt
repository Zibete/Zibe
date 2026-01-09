package com.zibete.proyecto1.ui.groups.host

import com.zibete.proyecto1.ui.components.ZibeSnackType

sealed class GroupHostEvent {
    data class OpenPrivateChat(val otherUid: String, val nodeType: String) : GroupHostEvent()

    data class ShowSnack(
        val message: String,
        val type: ZibeSnackType
    ) : GroupHostEvent()

}
