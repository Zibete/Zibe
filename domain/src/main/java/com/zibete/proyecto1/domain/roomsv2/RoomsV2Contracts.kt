package com.zibete.proyecto1.domain.roomsv2

import com.zibete.proyecto1.core.utils.ZibeResult
import java.util.UUID
import kotlinx.coroutines.flow.Flow

enum class RoomV2IdentityMode { REAL, ANONYMOUS }
enum class RoomV2Role { OWNER, MODERATOR, MEMBER }
enum class RoomV2Status { OPEN, CLOSED }
enum class RoomV2MessageKind { TEXT, IMAGE, AUDIO, VIDEO, FILE, EVENT }

data class RoomV2DirectoryItem(
    val roomId: String,
    val name: String,
    val description: String,
    val memberCount: Int,
    val pendingCount: Int,
    val status: RoomV2Status,
    val updatedAt: Long,
    val normalizedName: String = name,
)

/**
 * Public contextual identity. It is intentionally UID-free.
 *
 * Real-profile navigation must use a separate server-authorized indirection instead of
 * embedding Firebase Auth UIDs in room-readable projections.
 */
data class RoomV2Identity(
    val identityId: String,
    val displayName: String,
    val mode: RoomV2IdentityMode,
    val role: RoomV2Role,
    val active: Boolean,
)

data class RoomV2Membership(
    val roomId: String,
    val identity: RoomV2Identity,
    val joinedAt: Long,
    val lastReadAt: Long,
    val unreadCount: Long = 0,
    val lastReadSeq: Long = 0,
    val notificationsEnabled: Boolean = true,
)

data class RoomV2Message(
    val messageId: String,
    val roomId: String,
    val authorIdentityId: String,
    val authorDisplayName: String,
    val authorMode: RoomV2IdentityMode,
    val text: String,
    val sentAt: Long,
    val seq: Long = 0,
    val kind: RoomV2MessageKind = RoomV2MessageKind.TEXT,
    val removed: Boolean = false,
    val conversationId: String? = null,
    val replyToMessageId: String? = null,
    val attachment: RoomV2Attachment? = null,
)

data class RoomV2Attachment(
    val attachmentId: String,
    val mimeType: String,
    val sizeBytes: Long,
    val fileName: String,
    val durationMs: Long = 0,
)

data class RoomV2Thread(
    val roomId: String,
    val conversationId: String? = null,
)

data class RoomV2Conversation(
    val conversationId: String,
    val roomId: String,
    val otherIdentity: RoomV2Identity,
    val closed: Boolean,
    val blocked: Boolean,
    val lastText: String,
    val updatedAt: Long,
    val unreadCount: Long,
    val lastReadSeq: Long = 0,
)

data class RoomV2Report(
    val reportId: String,
    val roomId: String,
    val reason: String,
    val status: String,
    val evidence: RoomV2Message,
    val createdAt: Long,
    val resolution: String,
)

data class CreateRoomV2Request(
    val name: String,
    val description: String,
    val operationId: String = UUID.randomUUID().toString(),
)

data class JoinRoomV2Request(
    val roomId: String,
    val mode: RoomV2IdentityMode,
    val alias: String? = null,
)

interface RoomsV2Repository {
    fun observeDirectory(limit: Int = 50, search: String = ""): Flow<List<RoomV2DirectoryItem>>
    fun observeRoom(roomId: String): Flow<RoomV2DirectoryItem?>
    fun observeMemberships(): Flow<List<RoomV2Membership>>
    suspend fun loadDirectoryPage(
        search: String,
        after: RoomV2DirectoryItem?,
    ): ZibeResult<List<RoomV2DirectoryItem>>
    suspend fun refreshDirectory(): ZibeResult<Unit>
    suspend fun createRoom(request: CreateRoomV2Request): ZibeResult<RoomV2Membership>
    suspend fun joinRoom(request: JoinRoomV2Request): ZibeResult<RoomV2Membership>
    suspend fun leaveRoom(roomId: String): ZibeResult<Unit>
    suspend fun setNotifications(roomId: String, enabled: Boolean): ZibeResult<Unit>
}

interface RoomsV2ChatRepository {
    fun observeParticipants(roomId: String): Flow<List<RoomV2Identity>>
    fun observeMessages(thread: RoomV2Thread, limit: Int = 50): Flow<List<RoomV2Message>>
    fun observeConversations(roomId: String): Flow<List<RoomV2Conversation>>
    suspend fun loadEarlierMessages(
        thread: RoomV2Thread,
        beforeSeq: Long,
    ): ZibeResult<List<RoomV2Message>>
    suspend fun openPrivate(roomId: String, targetIdentityId: String): ZibeResult<String>
    suspend fun sendText(
        thread: RoomV2Thread,
        text: String,
        clientMessageId: String,
        replyToMessageId: String? = null,
    ): ZibeResult<RoomV2Message>
    suspend fun markRead(thread: RoomV2Thread, visibleSeq: Long): ZibeResult<Unit>
    suspend fun setVisibleThread(thread: RoomV2Thread, visible: Boolean): ZibeResult<Unit>
    suspend fun blockPrivate(thread: RoomV2Thread, blocked: Boolean): ZibeResult<Unit>
}

interface RoomsV2ModerationRepository {
    fun observeReports(roomId: String): Flow<List<RoomV2Report>>
    suspend fun editRoom(roomId: String, name: String, description: String): ZibeResult<Unit>
    suspend fun closeRoom(roomId: String): ZibeResult<Unit>
    suspend fun transferOwnership(roomId: String, targetIdentityId: String): ZibeResult<Unit>
    suspend fun setModerator(
        roomId: String,
        targetIdentityId: String,
        enabled: Boolean,
    ): ZibeResult<Unit>
    suspend fun removeMember(roomId: String, targetIdentityId: String, ban: Boolean): ZibeResult<Unit>
    suspend fun removeMessage(roomId: String, messageId: String): ZibeResult<Unit>
    suspend fun reportMessage(
        thread: RoomV2Thread,
        messageId: String,
        reason: String,
    ): ZibeResult<Unit>
    suspend fun resolveReport(roomId: String, reportId: String, resolution: String): ZibeResult<Unit>
}
