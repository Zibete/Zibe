package com.zibete.proyecto1.adapters

import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.model.Conversation

object ChatListDiffCallback : DiffUtil.ItemCallback<Conversation>() {

    override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem.otherId == newItem.otherId
    }

    override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: Conversation, newItem: Conversation): Any? {
        val changed = mutableSetOf<String>()

        if (oldItem.otherName != newItem.otherName) changed += PayloadConversation.USER_NAME
        if (oldItem.otherPhotoUrl != newItem.otherPhotoUrl) changed += PayloadConversation.PHOTO_URL
        if (oldItem.lastContent != newItem.lastContent) changed += PayloadConversation.MESSAGE
        if (oldItem.lastMessageAt != newItem.lastMessageAt) changed += PayloadConversation.CREATED_AT
        if (oldItem.unreadCount != newItem.unreadCount) changed += PayloadConversation.UNREAD
        if (oldItem.seen != newItem.seen || oldItem.userId != newItem.userId) changed += PayloadConversation.CHECKS
        if (oldItem.state != newItem.state) changed += PayloadConversation.STATE

        return changed.takeIf { it.isNotEmpty() }
    }

    object PayloadConversation {
        const val STATE = "payload_conversation_state"
        const val USER_NAME = "payload_conversation_user_name"
        const val PHOTO_URL = "payload_conversation_photo_url"
        const val MESSAGE = "payload_conversation_message"
        const val CREATED_AT = "payload_conversation_created_at"
        const val UNREAD = "payload_conversation_unread"
        const val CHECKS = "payload_conversation_checks"
    }
}

