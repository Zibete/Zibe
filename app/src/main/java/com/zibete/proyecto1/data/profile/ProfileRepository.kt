package com.zibete.proyecto1.data.profile

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.chat.ChatIdGenerator.getChatId
import com.zibete.proyecto1.core.constants.Constants.ActiveThreadKeys
import com.zibete.proyecto1.core.constants.Constants.ActiveViewKeys
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOCKED
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_DEFAULT_DM
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.ChatMessageKeys
import com.zibete.proyecto1.core.constants.Constants.ConversationKeys
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.core.constants.Constants.NODE_ACTIVE_VIEW
import com.zibete.proyecto1.core.constants.Constants.NODE_CLIENT_DATA
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_FAVORITE_LIST
import com.zibete.proyecto1.core.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_STATUS
import com.zibete.proyecto1.core.constants.Constants.StatusKeys
import com.zibete.proyecto1.core.constants.USER_NOT_FOUND_EXCEPTION
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.core.utils.TimeUtils.formatLastSeen
import com.zibete.proyecto1.core.utils.TimeUtils.now
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class BlockState(
    val isBlockedByMe: Boolean,
    val hasBlockedMe: Boolean
)

interface ProfileRepositoryActions {
    suspend fun toggleFavoriteUser(otherUid: String): Boolean
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
}

interface ProfileRepositoryProvider {
    suspend fun isFavorite(otherUid: String): Boolean
    suspend fun getChatState(otherUid: String, nodeType: String = NODE_DM): String
    suspend fun getOtherAccount(uid: String): ZibeResult<Users>
    suspend fun getBlockStateWith(otherUid: String, nodeType: String = NODE_DM): BlockState
    suspend fun getDmPhotoList(otherUid: String, nodeType: String = NODE_DM): List<String>
    fun observeUserStatus(userId: String, node: String): Flow<UserStatus>
}

@Singleton
class ProfileRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val authSessionProvider: AuthSessionProvider,
    @ApplicationContext private val context: Context
) : ProfileRepositoryProvider, ProfileRepositoryActions {

    private val firebaseUser: FirebaseUser
        get() = checkNotNull(authSessionProvider.currentUser) {
            USER_PROVIDER_ERR_EXCEPTION
        }

    private val myUid: String
        get() = firebaseUser.uid

    private fun accountRef(uid: String = myUid) =
        firebaseRefsContainer.refAccounts.child(uid)

    private fun dataRef(uid: String = myUid) =
        firebaseRefsContainer.refData.child(uid)

    private fun favoriteListRef(uid: String = myUid) =
        dataRef(uid).child(NODE_FAVORITE_LIST)

    private fun conversationsRootRef(ownerUid: String = myUid, nodeType: String) =
        firebaseRefsContainer.refData
            .child(ownerUid)
            .child(nodeType)

    private fun conversationRef(
        ownerUid: String = myUid,
        nodeType: String,
        otherUid: String
    ) = conversationsRootRef(ownerUid, nodeType).child(otherUid)

    private fun statusRef(uid: String = myUid) =
        firebaseRefsContainer.refData.child(uid)
            .child(NODE_CLIENT_DATA)
            .child(NODE_STATUS)

    private fun activeViewRef(uid: String = myUid) =
        firebaseRefsContainer.refData.child(uid)
            .child(NODE_CLIENT_DATA)
            .child(NODE_ACTIVE_VIEW)

    private fun activeThreadRef(uid: String = myUid) =
        activeViewRef(uid).child(ActiveViewKeys.ACTIVE_THREAD)

    private suspend fun getAccountSnapshot(uid: String): DataSnapshot =
        accountRef(uid).get().await()

    private suspend fun getAccount(uid: String): Users? =
        getAccountSnapshot(uid)
            .takeIf { it.exists() }
            ?.getValue(Users::class.java)

    override suspend fun getOtherAccount(uid: String): ZibeResult<Users> = zibeCatching {
        getAccount(uid) ?: throw Exception(USER_NOT_FOUND_EXCEPTION)
    }

    override suspend fun isFavorite(otherUid: String): Boolean =
        favoriteListRef()
            .child(otherUid)
            .get()
            .await()
            .exists()

    override suspend fun toggleFavoriteUser(otherUid: String): Boolean {
        val favoriteListRef = favoriteListRef().child(otherUid)
        val isFavorite = isFavorite(otherUid)
        if (isFavorite) {
            favoriteListRef.removeValue().await()
            return false
        } else {
            favoriteListRef.setValue(true).await()
            return true
        }
    }

    override suspend fun toggleNotificationsUser(
        otherUid: String,
        otherName: String,
        nodeType: String
    ): Boolean {
        val currentState = getChatState(otherUid)

        val newState =
            if (currentState == CHAT_STATE_SILENT) CHAT_STATE_DEFAULT_DM else CHAT_STATE_SILENT

        updateChatState(otherUid, otherName, nodeType, newState)

        return newState == CHAT_STATE_SILENT
    }

    override suspend fun toggleBlock(
        otherUid: String,
        otherName: String,
        nodeType: String
    ): Boolean {
        val currentState = getChatState(otherUid)

        val newState =
            if (currentState == CHAT_STATE_SILENT) CHAT_STATE_DEFAULT_DM else CHAT_STATE_SILENT

        updateChatState(otherUid, otherName, nodeType, newState)

        return newState == CHAT_STATE_SILENT
    }

    override suspend fun getBlockStateWith(otherUid: String, nodeType: String): BlockState {
        val meState = conversationRef(ownerUid = myUid, nodeType = nodeType, otherUid = otherUid)
            .child(ConversationKeys.STATE)
            .get()
            .await()
            .getValue(String::class.java)
            .orEmpty()

        val otherState = conversationRef(ownerUid = otherUid, nodeType = nodeType, otherUid = myUid)
            .child(ConversationKeys.STATE)
            .get()
            .await()
            .getValue(String::class.java)
            .orEmpty()

        return BlockState(
            isBlockedByMe = meState == CHAT_STATE_BLOCKED,
            hasBlockedMe = otherState == CHAT_STATE_BLOCKED
        )
    }

    override suspend fun getDmPhotoList(otherUid: String, nodeType: String): List<String> {
        val chatId = getChatId(myUid, otherUid)
        val refChat = firebaseRefsContainer.refChatsRoot
            .child(nodeType)
            .child(chatId)
        val photos = mutableListOf<String>()

        fun collectFrom(snapshot: DataSnapshot) {
            snapshot.children.forEach { msgSnap ->
                val type = msgSnap.child(ChatMessageKeys.TYPE).getValue(Int::class.java)
                val content = msgSnap.child(ChatMessageKeys.CONTENT).getValue(String::class.java)

                if (type == MSG_PHOTO)
                    if (!content.isNullOrEmpty()) photos.add(content)
            }
        }

        val snap = refChat.get().await()
        if (snap.exists()) collectFrom(snap)

        return photos.distinct()
    }

    override suspend fun getChatState(otherUid: String, nodeType: String): String =
        conversationRef(nodeType = nodeType, otherUid = otherUid)
            .child(ConversationKeys.STATE)
            .get()
            .await()
            .getValue(String::class.java)
            .orEmpty()

    private suspend fun updateChatState(
        otherUid: String,
        otherName: String,
        nodeType: String,
        newState: String
    ) {
        val chatRef = conversationRef(nodeType = nodeType, otherUid = otherUid)
        val snapshot = chatRef.get().await()

        if (!snapshot.exists()) {
            val newConversation = createDefaultConversation(otherUid, otherName, newState)
            chatRef.setValue(newConversation).await()
            return
        }

        chatRef.child(ConversationKeys.STATE).setValue(newState).await()
    }

    private fun createDefaultConversation(
        otherUid: String,
        otherName: String,
        state: String
    ): Conversation {
        return Conversation(
            lastContent = "Chat vacío",
            lastMessageAt = now(),
            userId = myUid,
            otherId = otherUid,
            otherName = otherName,
            otherPhotoUrl = DEFAULT_PROFILE_PHOTO_URL,
            state = state,
            unreadCount = 0,
            seen = 1
        )
    }

    private fun DataSnapshot.toUserStatus(
        lastSeenFormatter: (Long) -> String
    ): UserStatus {
        if (!exists()) return UserStatus.Offline

        val status = child(StatusKeys.STATUS).getValue(String::class.java).orEmpty()
        val lastSeenMs = child(StatusKeys.LAST_SEEN_MS).getValue(Long::class.java) ?: 0L

        if (status == context.getString(R.string.online)) return UserStatus.Online

        if (status == context.getString(R.string.typing) ||
            status == context.getString(R.string.recording)
        ) {
            return UserStatus.TypingOrRecording(status)
        }

        val lastSeenText = lastSeenFormatter(lastSeenMs)
        return if (lastSeenText.isBlank()) UserStatus.Offline
        else UserStatus.LastSeen(lastSeenText)
    }

    override fun observeUserStatus(
        userId: String,
        node: String
    ): Flow<UserStatus> {
        if (userId.isBlank()) return flowOf(UserStatus.Offline)
        return callbackFlow {
            val statusRef = statusRef(userId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val base = snapshot.toUserStatus { ms -> formatLastSeen(ms, context) }
                    if (base !is UserStatus.TypingOrRecording) {
                        trySend(base)
                        return
                    }
                    launch {
                        val otherAt = activeThreadRef(userId).get().await()
                        val otherNode =
                            otherAt.child(ActiveThreadKeys.NODE_TYPE).getValue(String::class.java)
                                .orEmpty()
                        val other =
                            otherAt.child(ActiveThreadKeys.OTHER_UID).getValue(String::class.java)
                                .orEmpty()
                        val matches = when (node) {
                            NODE_DM ->
                                otherNode == NODE_DM && other == myUid

                            NODE_GROUP_DM ->
                                otherNode == NODE_GROUP_DM && other == myUid

                            else -> false
                        }
                        if (matches) trySend(base) else trySend(UserStatus.Online)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            statusRef.addValueEventListener(listener)
            awaitClose { statusRef.removeEventListener(listener) }
        }.flowOn(Dispatchers.IO)
    }
}

