package com.zibete.proyecto1.data.profile

import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import kotlinx.coroutines.flow.Flow

data class BlockState(
    val isBlockedByMe: Boolean,
    val hasBlockedMe: Boolean
)

data class BlockedUser(
    val id: String,
    val name: String
)

interface ProfileRepositoryActions {
    suspend fun toggleFavoriteUser(otherUid: String): ZibeResult<Boolean>
    suspend fun toggleNotificationsUser(
        otherUid: String,
        otherName: String,
        nodeType: String = NODE_DM
    ): Boolean

    suspend fun toggleBlock(
        otherUid: String,
        otherName: String,
        nodeType: String = NODE_DM
    ): Boolean

    suspend fun updateChatState(
        otherUid: String,
        otherName: String,
        nodeType: String,
        newState: String
    ): ZibeResult<Unit>
}

interface ProfileRepositoryProvider {
    suspend fun isFavorite(otherUid: String): ZibeResult<Boolean>
    suspend fun getMyChatState(otherUid: String): ZibeResult<String>
    suspend fun getOtherAccount(uid: String): ZibeResult<Users>
    suspend fun getBlockStateWith(
        otherUid: String,
        nodeType: String = NODE_DM
    ): ZibeResult<BlockState>

    suspend fun getDmPhotoList(
        otherUid: String,
        nodeType: String = NODE_DM
    ): ZibeResult<List<String>>

    suspend fun getBlockedUsers(nodeType: String = NODE_DM): List<BlockedUser>
    fun observeUserStatus(userId: String, node: String): Flow<UserStatus>
}
