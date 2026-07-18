package com.zibete.proyecto1.data

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.rooms.CreateRoomCommand
import com.zibete.proyecto1.domain.rooms.JoinRoomCommand
import com.zibete.proyecto1.domain.rooms.LeaveRoomCommand
import com.zibete.proyecto1.domain.rooms.RoomOperationResult
import com.zibete.proyecto1.domain.rooms.SwitchRoomCommand
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.GroupChatChildEvent
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.RoomSession
import com.zibete.proyecto1.model.UserGroup
import kotlinx.coroutines.flow.Flow

interface GroupRepositoryProvider {
    val myUid: String
    suspend fun loadRooms(): ZibeResult<List<Groups>>
    fun observeTotalRoomUnread(): Flow<Int>
    fun observeRoomPrivateConversations(roomKey: String): Flow<List<Conversation>>
    suspend fun createRoom(command: CreateRoomCommand): ZibeResult<RoomOperationResult>
    suspend fun joinRoom(command: JoinRoomCommand): ZibeResult<RoomOperationResult>
    suspend fun switchRoom(command: SwitchRoomCommand): ZibeResult<RoomOperationResult>
    suspend fun leaveRoom(command: LeaveRoomCommand): ZibeResult<RoomOperationResult>
    suspend fun resolveRoomSession(roomKey: String): ZibeResult<RoomSession>
    suspend fun isRoomAliasInUse(
        roomKey: String,
        normalizedAlias: String
    ): ZibeResult<Boolean>
    suspend fun markRoomAsRead(roomKey: String, lastReadAt: Long): ZibeResult<Unit>
    suspend fun sendRoomMessage(
        roomKey: String,
        userName: String,
        userType: Int,
        chatType: Int,
        content: String
    ): ZibeResult<Unit>
    suspend fun sendRoomPhotoMessage(
        roomKey: String,
        userName: String,
        userType: Int,
        photoUri: String
    ): ZibeResult<Unit>
    suspend fun setActiveRoom(roomKey: String): ZibeResult<Unit>
    suspend fun clearActiveRoom(roomKey: String): ZibeResult<Unit>
    fun observeGroupChatEvents(groupName: String): Flow<GroupChatChildEvent>
    fun unreadGroupBadgeCount(groupName: String): Flow<Int>
    fun observeUnreadGroupChat(groupName: String): Flow<Int>
    fun observeUnreadPrivateMessages(): Flow<Int>
    fun observeGroupUsers(groupName: String): Flow<List<UserGroup>>
    fun observeIsUserInGroup(groupName: String, userId: String): Flow<Boolean>
    suspend fun getGroup(groupName: String): Groups?
    suspend fun findUserGroup(userId: String, groupName: String): UserGroup?
    suspend fun isGroupMatch(otherUid: String, groupName: String): ZibeResult<Boolean>
}

interface LocationRepositoryProvider {
    val latitude: Double
    val longitude: Double
    suspend fun getDistanceToUser(otherUid: String): ZibeResult<String>
    fun getDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
    fun formatDistance(distanceMeters: Double): String
}

interface LocationRepositoryActions {
    suspend fun updateLocation(latitude: Double, longitude: Double)
}

interface PresenceRepositoryActions {
    suspend fun startPresence()
    fun stopPresence()
}
