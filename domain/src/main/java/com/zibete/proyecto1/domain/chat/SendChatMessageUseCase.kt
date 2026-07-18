package com.zibete.proyecto1.domain.chat

import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOCKED
import com.zibete.proyecto1.core.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.ChatRepositoryContract
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.Conversation
import javax.inject.Inject

data class SendChatMessageCommand(
    val senderUid: String,
    val receiverUid: String,
    val nodeType: String,
    val messageType: Int,
    val content: String,
    val audioDurationMs: Long,
    val createdAt: Long,
    val senderConversationContent: String,
    val receiverConversationContent: String,
    val receiverName: String,
    val receiverPhotoUrl: String,
    val senderName: String,
    val senderPhotoUrl: String,
    val roomKey: String = ""
)

sealed interface SendChatMessageOutcome {
    data object Sent : SendChatMessageOutcome
    data object BlockedByRecipient : SendChatMessageOutcome
}

interface SendChatMessageUseCase {
    suspend fun execute(command: SendChatMessageCommand): ZibeResult<SendChatMessageOutcome>
}

class DefaultSendChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepositoryContract
) : SendChatMessageUseCase {

    override suspend fun execute(
        command: SendChatMessageCommand
    ): ZibeResult<SendChatMessageOutcome> = zibeCatching {
        val senderConversation = chatRepository.getConversation(
            command.senderUid,
            command.receiverUid,
            command.nodeType
        )
        val receiverConversation = chatRepository.getConversation(
            command.receiverUid,
            command.senderUid,
            command.nodeType
        )
        if (receiverConversation?.state == CHAT_STATE_BLOCKED) {
            return@zibeCatching SendChatMessageOutcome.BlockedByRecipient
        }

        val message = ChatMessage(
            content = command.content,
            createdAt = command.createdAt,
            audioDurationMs = command.audioDurationMs,
            senderUid = command.senderUid,
            type = command.messageType,
            seen = MSG_DELIVERED,
            roomKey = command.roomKey,
            senderName = command.senderName
        )
        val newSenderConversation = Conversation(
            lastContent = command.senderConversationContent,
            lastMessageAt = command.createdAt,
            userId = command.senderUid,
            otherId = command.receiverUid,
            otherName = command.receiverName,
            otherPhotoUrl = command.receiverPhotoUrl,
            state = senderConversation?.state ?: command.nodeType,
            unreadCount = 0,
            seen = MSG_DELIVERED,
            roomKey = command.roomKey
        )
        val newReceiverConversation = Conversation(
            lastContent = command.receiverConversationContent,
            lastMessageAt = command.createdAt,
            userId = command.senderUid,
            otherId = command.senderUid,
            otherName = command.senderName,
            otherPhotoUrl = command.senderPhotoUrl,
            state = receiverConversation?.state ?: command.nodeType,
            unreadCount = (receiverConversation?.unreadCount ?: 0) + 1,
            roomKey = command.roomKey
        )

        if (command.nodeType == NODE_DM) {
            chatRepository.sendDmMessageWithConversations(
                command.senderUid,
                command.receiverUid,
                message,
                newSenderConversation,
                newReceiverConversation
            ).getOrThrow()
        } else if (command.nodeType == NODE_GROUP_DM) {
            require(command.roomKey.isNotBlank()) { "group_dm requires a roomKey" }
            chatRepository.sendGroupDmMessageWithConversations(
                command.senderUid,
                command.receiverUid,
                command.roomKey,
                message,
                newSenderConversation,
                newReceiverConversation
            ).getOrThrow()
        } else {
            val thread = chatRepository.chatThread(command.receiverUid, command.nodeType)
            chatRepository.pushMessageToChat(thread, message)
            chatRepository.saveConversation(
                command.senderUid,
                command.nodeType,
                command.receiverUid,
                newSenderConversation
            )
            chatRepository.saveConversation(
                command.receiverUid,
                command.nodeType,
                command.senderUid,
                newReceiverConversation
            )
        }
        SendChatMessageOutcome.Sent
    }
}
