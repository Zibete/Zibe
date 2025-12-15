package com.zibete.proyecto1.adapters

import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.model.ChatWith

object ChatListDiffCallback : DiffUtil.ItemCallback<ChatWith>() {

    override fun areItemsTheSame(oldItem: ChatWith, newItem: ChatWith): Boolean {
        return oldItem.userId == newItem.userId
    }

    override fun areContentsTheSame(oldItem: ChatWith, newItem: ChatWith): Boolean {
        return oldItem.userName == newItem.userName &&
                oldItem.userPhoto == newItem.userPhoto &&
                oldItem.msg == newItem.msg &&
                oldItem.dateTime == newItem.dateTime &&
                oldItem.msgReceivedUnread == newItem.msgReceivedUnread &&
                oldItem.seen == newItem.seen &&
                oldItem.senderId == newItem.senderId &&
                oldItem.state == newItem.state
    }

    override fun getChangePayload(oldItem: ChatWith, newItem: ChatWith): Any? {
        val changed = mutableSetOf<String>()

        if (oldItem.userName != newItem.userName) changed += "name"
        if (oldItem.userPhoto != newItem.userPhoto) changed += "photo"
        if (oldItem.msg != newItem.msg) changed += "msg"
        if (oldItem.dateTime != newItem.dateTime) changed += "time"
        if (oldItem.msgReceivedUnread != newItem.msgReceivedUnread) changed += "unread"
        if (oldItem.seen != newItem.seen || oldItem.senderId != newItem.senderId) changed += "checks"
        if (oldItem.state != newItem.state) changed += "state"

        return changed.takeIf { it.isNotEmpty() }
    }
}

