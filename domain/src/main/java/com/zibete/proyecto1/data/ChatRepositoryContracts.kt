package com.zibete.proyecto1.data

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.model.ChatChildEvent
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.Conversation
import kotlinx.coroutines.flow.Flow

data class ChatThread(
    val otherUid: String,
    val nodeType: String
)

data class DeleteResult(
    val deletedCount: Int,
    val chatRemoved: Boolean
)

data class UnreadSummary(
    val totalChats: Int,
    val totalUnread: Int
)

interface ChatRepositoryContract {
    fun chatThread(otherUid: String, nodeType: String): ChatThread
    fun observeChatMessages(thread: ChatThread): Flow<ChatChildEvent>
    fun observeConversations(nodeType: String): Flow<List<Conversation>>
    suspend fun getConversation(
        firstUid: String,
        secondUid: String,
        nodeType: String
    ): Conversation?
    suspend fun hasConversation(otherUid: String, nodeType: String): ZibeResult<Boolean>
    suspend fun saveConversation(
        ownerUid: String,
        nodeType: String,
        otherUid: String,
        conversation: Conversation
    )
    suspend fun pushMessageToChat(thread: ChatThread, message: ChatMessage)
    suspend fun sendDmMessageWithConversations(
        senderUid: String,
        receiverUid: String,
        message: ChatMessage,
        senderConversation: Conversation,
        receiverConversation: Conversation
    ): ZibeResult<Unit>
    suspend fun sendGroupDmMessageWithConversations(
        senderUid: String,
        receiverUid: String,
        roomKey: String,
        message: ChatMessage,
        senderConversation: Conversation,
        receiverConversation: Conversation
    ): ZibeResult<Unit>
    suspend fun uploadMedia(
        localUri: String,
        fileName: String,
        thread: ChatThread,
        storagePath: String
    ): ZibeResult<String>
    suspend fun markChatAsSeen(thread: ChatThread): ZibeResult<Unit>
    suspend fun markMessageAsSeenIfNeeded(
        thread: ChatThread,
        messageId: String,
        message: ChatMessage
    ): ZibeResult<Unit>
    suspend fun deleteMessages(
        thread: ChatThread,
        selectedIds: List<String>?
    ): ZibeResult<DeleteResult>
    suspend fun deleteConversationForMe(thread: ChatThread): ZibeResult<DeleteResult>
    suspend fun getMessageCount(thread: ChatThread): Int
    suspend fun getUnreadSummaryForChats(myUid: String, nodeType: String): UnreadSummary
}

interface DirectMessageReceiptAcknowledger {
    suspend fun acknowledgeReceived(
        myUid: String,
        otherUid: String,
        messageId: String
    ): ZibeResult<Unit>
}
