package com.zibete.proyecto1.adapters

import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.model.Conversation

object ChatListDiffCallback : DiffUtil.ItemCallback<Conversation>() {

    override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem.otherId == newItem.otherId
    }

    override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem.otherName == newItem.otherName &&
                oldItem.otherPhotoUrl == newItem.otherPhotoUrl &&
                oldItem.lastContent == newItem.lastContent &&
                oldItem.lastDate == newItem.lastDate &&
                oldItem.unreadCount == newItem.unreadCount &&
                oldItem.seen == newItem.seen &&
                oldItem.userId == newItem.userId &&
                oldItem.state == newItem.state
    }

    override fun getChangePayload(oldItem: Conversation, newItem: Conversation): Any? {
        val changed = mutableSetOf<String>()

        if (oldItem.otherName != newItem.otherName) changed += "name"
        if (oldItem.otherPhotoUrl != newItem.otherPhotoUrl) changed += "photo"
        if (oldItem.lastContent != newItem.lastContent) changed += "msg"
        if (oldItem.lastDate != newItem.lastDate) changed += "time"
        if (oldItem.unreadCount != newItem.unreadCount) changed += "unread"
        if (oldItem.seen != newItem.seen || oldItem.userId != newItem.userId) changed += "checks"
        if (oldItem.state != newItem.state) changed += "state"

        return changed.takeIf { it.isNotEmpty() }
    }
}

