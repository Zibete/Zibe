package com.zibete.proyecto1.data

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.model.UserGroup

interface GroupRepositoryProvider {
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
        senderName: String = ""
    ): ZibeResult<Unit>

    suspend fun removeUserFromGroup(groupName: String, userId: String = ""): ZibeResult<Unit>
}
