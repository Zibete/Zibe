package com.zibete.proyecto1.data

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.model.GroupChatChildEvent
import com.zibete.proyecto1.model.Groups
import kotlinx.coroutines.flow.Flow

interface GroupRepositoryProvider {
    val myUid: String
    fun observeGroupChatEvents(groupName: String): Flow<GroupChatChildEvent>
    fun unreadGroupBadgeCount(groupName: String): Flow<Int>
    fun observeUnreadGroupChat(groupName: String): Flow<Int>
    fun observeUnreadPrivateMessages(): Flow<Int>
    fun observeGroupUsers(groupName: String): Flow<List<UserGroup>>
    suspend fun getGroup(groupName: String): Groups?
    suspend fun isNickInUse(groupName: String, nick: String): Boolean
    suspend fun markGroupAsRead(groupName: String)
    suspend fun sendGroupPhotoMessage(
        groupName: String,
        senderName: String,
        userType: Int,
        senderUid: String = "",
        photoUri: String
    )
    suspend fun saveUserInGroup(
        groupName: String,
        userName: String,
        userType: Int,
        userId: String = ""
    )
    suspend fun isGroupNameInUse(groupName: String): Boolean
    suspend fun createGroup(
        groupName: String,
        groupDescription: String,
        groupType: Int,
        creatorUid: String = ""
    )
    suspend fun getAllGroups(): List<Groups>
    suspend fun findUserGroup(userId: String, groupName: String): UserGroup?
    suspend fun isGroupMatch(otherUid: String, groupName: String): ZibeResult<Boolean>
    suspend fun removeMyGroupChatList(): ZibeResult<Unit>
    suspend fun removeMyPrivateGroupChats(userId: String = ""): ZibeResult<Unit>
    suspend fun sendGroupMessage(
        groupName: String,
        userName: String,
        userType: Int,
        chatType: Int,
        content: String,
        senderUid: String = ""
    ): ZibeResult<Unit>

    suspend fun removeUserFromGroup(groupName: String, userId: String = ""): ZibeResult<Unit>
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
