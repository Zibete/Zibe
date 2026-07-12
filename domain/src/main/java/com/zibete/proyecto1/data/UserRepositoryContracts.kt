package com.zibete.proyecto1.data

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.auth.AuthUser
import com.zibete.proyecto1.model.Users
import kotlinx.coroutines.flow.Flow

data class HiddenChat(
    val id: String,
    val name: String
)

interface LocalRepositoryProvider {
    val myUid: String
    val myUserName: String
    val myProfilePhotoUrl: String
    val myEmail: String
}

interface UserDirectoryProvider {
    suspend fun getAllAccounts(): List<Users>
    suspend fun getFavoriteUserIds(uid: String): Set<String>
    suspend fun removeFavoriteUserIds(uid: String, userIds: Collection<String>)
    suspend fun getConversationStates(uid: String): Map<String, String>
    suspend fun hasBlockedUser(otherUid: String, myUid: String): Boolean
}

interface ConversationOverviewRepository {
    fun observeUnreadChatList(): Flow<Int>
    suspend fun getHiddenChats(): List<HiddenChat>
    suspend fun updateChatState(
        otherUid: String,
        otherName: String,
        nodeType: String,
        newState: String
    ): ZibeResult<Unit>
    suspend fun toggleUnreadBadge(otherUid: String, nodeType: String)
    suspend fun setActiveThread(otherUid: String, nodeType: String): ZibeResult<Unit>
    suspend fun clearActiveThread(): ZibeResult<Unit>
}

interface UserRepositoryProvider {
    suspend fun getMyAccount(): ZibeResult<Users>
    suspend fun accountExists(uid: String): Boolean
    suspend fun hasBirthDate(uid: String): Boolean
    suspend fun getDefaultProfilePhotoUrl(): ZibeResult<String>
    suspend fun getProfilePhotoUrl(): String?
    suspend fun getAccount(uid: String): Users?
}

interface UserRepositoryActions {
    suspend fun createUserNode(
        user: AuthUser,
        name: String,
        birthDate: String,
        description: String
    ): ZibeResult<Unit>

    suspend fun setUserLastSeen()
    suspend fun setUserActivityStatus(status: String)
    suspend fun deleteMyAccountData(): ZibeResult<Unit>
    suspend fun deleteProfilePhoto(): ZibeResult<Unit>
    suspend fun putProfilePhotoInStorage(localUri: String): ZibeResult<Unit>
    suspend fun updateUserFields(fields: Map<String, Any?>)
    suspend fun updateLocalProfile(name: String?, photoUrl: String?, email: String?)
    suspend fun sendFeedback(
        feedback: String,
        screen: String,
        model: String,
        appVersion: String
    ): ZibeResult<Unit>
}
