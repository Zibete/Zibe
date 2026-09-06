package com.zibete.proyecto1.domain.roomsv2

import kotlinx.coroutines.flow.Flow

enum class RoomV2IdentityMode { REAL, ANONYMOUS }
enum class RoomV2Role { OWNER, MODERATOR, MEMBER }
enum class RoomV2Status { OPEN, CLOSED }

data class RoomV2DirectoryItem(
    val roomId: String,
    val name: String,
    val description: String,
    val memberCount: Int,
    val pendingCount: Int,
    val status: RoomV2Status,
    val updatedAt: Long,
)

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
)

data class RoomV2Message(
    val messageId: String,
    val roomId: String,
    val authorIdentityId: String,
    val authorDisplayName: String,
    val authorMode: RoomV2IdentityMode,
    val text: String,
    val sentAt: Long,
)

data class CreateRoomV2Request(val name: String, val description: String)
data class JoinRoomV2Request(
    val roomId: String,
    val mode: RoomV2IdentityMode,
    val alias: String? = null,
)

interface RoomsV2Repository {
    fun observeDirectory(limit: Int = 100): Flow<List<RoomV2DirectoryItem>>
    fun observeMemberships(): Flow<List<RoomV2Membership>>
    fun observeMessages(roomId: String, limit: Int = 60): Flow<List<RoomV2Message>>

    suspend fun refreshDirectory(): Result<Unit>
    suspend fun createRoom(request: CreateRoomV2Request): Result<RoomV2Membership>
    suspend fun joinRoom(request: JoinRoomV2Request): Result<RoomV2Membership>
    suspend fun leaveRoom(roomId: String): Result<Unit>
    suspend fun sendText(
        roomId: String,
        text: String,
        clientMessageId: String,
    ): Result<RoomV2Message>
}
