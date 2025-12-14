package com.zibete.proyecto1.adapters

import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_GROUPS_CATEGORY
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_GROUPS_DATA
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_GROUPS_USERS

object GroupsDiffCallback : DiffUtil.ItemCallback<Groups>() {


    override fun areItemsTheSame(oldItem: Groups, newItem: Groups): Boolean {
        // En tu app, el key real del grupo es el name.
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: Groups, newItem: Groups): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: Groups, newItem: Groups): Any? {
        val changed = mutableSetOf<String>()

        if (oldItem.users != newItem.users) changed += PAYLOAD_GROUPS_USERS
        if (oldItem.data != newItem.data) changed += PAYLOAD_GROUPS_DATA
        if (oldItem.groupType != newItem.groupType) changed += PAYLOAD_GROUPS_CATEGORY

        return changed.takeIf { it.isNotEmpty() }
    }
}
