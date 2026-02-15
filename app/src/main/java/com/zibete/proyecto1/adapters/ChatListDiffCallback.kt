package com.zibete.proyecto1.adapters

import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.core.constants.Constants.PayloadRowKeys
import com.zibete.proyecto1.model.Conversation

object ChatListDiffCallback : DiffUtil.ItemCallback<Conversation>() {

    override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem.otherId == newItem.otherId
    }

    override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem.otherName == newItem.otherName &&
                oldItem.otherPhotoUrl == newItem.otherPhotoUrl &&
                oldItem.lastContent == newItem.lastContent &&
                oldItem.lastMessageAt == newItem.lastMessageAt &&
                oldItem.unreadCount == newItem.unreadCount &&
                oldItem.seen == newItem.seen &&
                oldItem.userId == newItem.userId &&
                oldItem.state == newItem.state
    }

    override fun getChangePayload(oldItem: Conversation, newItem: Conversation): Any? {
        val changed = mutableSetOf<String>()

        if (oldItem.otherName != newItem.otherName) changed += PayloadRowKeys.NAME
        if (oldItem.otherPhotoUrl != newItem.otherPhotoUrl) changed += PayloadRowKeys.PHOTO
        if (oldItem.lastContent != newItem.lastContent) changed += PayloadRowKeys.MESSAGE
        if (oldItem.lastMessageAt != newItem.lastMessageAt) changed += PayloadRowKeys.TIME
        if (oldItem.unreadCount != newItem.unreadCount) changed += PayloadRowKeys.UNREAD
        if (oldItem.seen != newItem.seen || oldItem.userId != newItem.userId) changed += PayloadRowKeys.CHECKS
        if (oldItem.state != newItem.state) changed += PayloadRowKeys.STATE

        return changed.takeIf { it.isNotEmpty() }
    }
}

