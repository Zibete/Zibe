package com.zibete.proyecto1.adapters

import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_GROUPS_DATA
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_GROUPS_USERS
import com.zibete.proyecto1.domain.roomsv2.RoomV2DirectoryItem

object GroupsDiffCallback : DiffUtil.ItemCallback<RoomV2DirectoryItem>() {

    override fun areItemsTheSame(
        oldItem: RoomV2DirectoryItem,
        newItem: RoomV2DirectoryItem,
    ): Boolean = oldItem.roomId == newItem.roomId

    override fun areContentsTheSame(
        oldItem: RoomV2DirectoryItem,
        newItem: RoomV2DirectoryItem,
    ): Boolean = oldItem == newItem

    override fun getChangePayload(
        oldItem: RoomV2DirectoryItem,
        newItem: RoomV2DirectoryItem,
    ): Any? {
        val changed = mutableSetOf<String>()

        if (oldItem.memberCount != newItem.memberCount) changed += PAYLOAD_GROUPS_USERS
        if (oldItem.description != newItem.description) changed += PAYLOAD_GROUPS_DATA

        return changed.takeIf { it.isNotEmpty() }
    }
}
