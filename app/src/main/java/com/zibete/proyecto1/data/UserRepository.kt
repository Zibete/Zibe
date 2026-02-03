package com.zibete.proyecto1.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.AccountsKeys
import com.zibete.proyecto1.core.constants.Constants.ActiveThreadKeys
import com.zibete.proyecto1.core.constants.Constants.ActiveViewKeys
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.core.constants.Constants.ChatListKeys
import com.zibete.proyecto1.core.constants.Constants.ConversationKeys
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.constants.Constants.EMPTY
import com.zibete.proyecto1.core.constants.Constants.NODE_ACTIVE_VIEW
import com.zibete.proyecto1.core.constants.Constants.NODE_CHAT_LIST
import com.zibete.proyecto1.core.constants.Constants.NODE_CLIENT_DATA
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_FAVORITE_LIST
import com.zibete.proyecto1.core.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_STATUS
import com.zibete.proyecto1.core.constants.Constants.PATH_PROFILE_PHOTOS
import com.zibete.proyecto1.core.constants.Constants.PROFILE_PHOTO
import com.zibete.proyecto1.core.constants.Constants.StatusKeys
import com.zibete.proyecto1.core.constants.USER_NOT_FOUND_EXCEPTION
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface LocalRepositoryProvider {
    val myUserName: String
    val myProfilePhotoUrl: String
    val myEmail: String
}

interface UserRepositoryProvider {
    suspend fun accountExists(uid: String): Boolean
    suspend fun hasBirthDate(uid: String): Boolean
    suspend fun getProfilePhotoUrl(): String?
    suspend fun getAccount(uid: String): Users?
    suspend fun getChatStateWith(otherUid: String, nodeType: String): String
    suspend fun getMyAccount(): ZibeResult<Users>
}

interface UserRepositoryActions {
    suspend fun createUserNode(
        firebaseUser: FirebaseUser,
        name: String,
        birthDate: String,
        description: String
    )

    suspend fun setUserLastSeen()
    suspend fun setUserActivityStatus(status: String)
    suspend fun deleteMyAccountData(): ZibeResult<Unit>
    suspend fun deleteProfilePhoto(): ZibeResult<Unit>
    suspend fun putProfilePhotoInStorage(localUri: Uri): ZibeResult<Unit>
    suspend fun updateUserFields(fields: Map<String, Any?>)
    suspend fun updateLocalProfile(name: String?, photoUrl: String?, email: String?)
    suspend fun sendFeedback(
        feedback: String,
        screen: String,
        model: String,
        appVersion: String
    ): ZibeResult<Unit>
}

private suspend fun Query.awaitSnapshot(): DataSnapshot =
    get().await() // Atajo para lecturas one-shot con coroutines

@Singleton
class UserRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val authSessionProvider: AuthSessionProvider,
    private val presenceRepository: PresenceRepository,
    @ApplicationContext private val context: Context
) : LocalRepositoryProvider, UserRepositoryProvider, UserRepositoryActions {

    // ============================================================
    // SESSION (cache local)
    // ============================================================
    val firebaseUser: FirebaseUser
        get() = checkNotNull(authSessionProvider.currentUser) {
            USER_PROVIDER_ERR_EXCEPTION
        }

    val myUid: String
        get() = firebaseUser.uid

    override var myUserName: String = ""
        private set

    override var myProfilePhotoUrl: String = ""
        private set

    override var myEmail: String = ""
        private set

    override suspend fun updateLocalProfile(name: String?, photoUrl: String?, email: String?) {
        name?.let { myUserName = it }
        photoUrl?.let { myProfilePhotoUrl = it }
        email?.let { myEmail = it }
    }

    // ============================================================
    // Refs helpers (RTDB)
    // ============================================================

    private fun accountRef(uid: String = myUid) =
        firebaseRefsContainer.refAccounts.child(uid)

    fun dataRef(uid: String = myUid) =
        firebaseRefsContainer.refData.child(uid)

    private fun favoriteListRef(uid: String = myUid) =
        dataRef(uid).child(NODE_FAVORITE_LIST)

    fun conversationsRootRef(ownerUid: String = myUid, nodeType: String) =
        firebaseRefsContainer.refData
            .child(ownerUid)
            .child(nodeType)

    private fun conversationRef(
        ownerUid: String = myUid,
        nodeType: String,
        otherUid: String
    ) = conversationsRootRef(ownerUid, nodeType).child(otherUid)

    fun accountIsOnlineRef(uid: String = myUid) =
        firebaseRefsContainer.refAccounts.child(uid)
            .child(AccountsKeys.IS_ONLINE)

    fun statusRef(uid: String = myUid) =
        firebaseRefsContainer.refData.child(uid)
            .child(NODE_CLIENT_DATA)
            .child(NODE_STATUS)

    private fun activeViewRef(uid: String = myUid) =
        firebaseRefsContainer.refData.child(uid)
            .child(NODE_CLIENT_DATA)
            .child(NODE_ACTIVE_VIEW)

    private fun chatListRef(uid: String = myUid) =
        firebaseRefsContainer.refData.child(uid)
            .child(NODE_CHAT_LIST)

    private fun readGroupMessagesRef(uid: String = myUid): DatabaseReference =
        chatListRef(uid).child(ChatListKeys.READ_GROUP_MESSAGES)

    private fun activeThreadRef(uid: String = myUid) =
        activeViewRef(uid).child(ActiveViewKeys.ACTIVE_THREAD)

    // ============================================================
    // Refs helpers (STORAGE)
    // ============================================================

    fun getProfilePhotoStoragePath(
        fileName: String = PROFILE_PHOTO
    ) = firebaseRefsContainer.storageUsersRef
        .child(myUid)
        .child(PATH_PROFILE_PHOTOS)
        .child(fileName)

    override suspend fun putProfilePhotoInStorage(localUri: Uri): ZibeResult<Unit> =
        zibeCatching { getProfilePhotoStoragePath().putFile(localUri).await() }

    override suspend fun deleteProfilePhoto(): ZibeResult<Unit> =
        zibeCatching { getProfilePhotoStoragePath().delete().await() }

    override suspend fun getProfilePhotoUrl(): String? {
        return try {
            getProfilePhotoStoragePath()
                .downloadUrl
                .await()
                .toString()
        } catch (_: Exception) {
            null
        }
    }

    // ============================================================
    // SPLASH (routing)
    // ============================================================

    suspend fun getAccountSnapshot(uid: String): DataSnapshot =
        accountRef(uid).awaitSnapshot()

    override suspend fun getAccount(uid: String): Users? =
        getAccountSnapshot(uid)
            .takeIf { it.exists() }
            ?.getValue(Users::class.java)

    override suspend fun getMyAccount(): ZibeResult<Users> = zibeCatching {
        getAccount(myUid) ?: throw Exception(USER_NOT_FOUND_EXCEPTION)
    }

    // ============================================================
    // EDIT PROFILE (updates)
    // ============================================================

    override suspend fun updateUserFields(fields: Map<String, Any?>) {
        val clean = fields.filterValues { it != null }
        if (clean.isEmpty()) return
        accountRef(myUid).updateChildren(clean).await()
    }

    override suspend fun sendFeedback(
        feedback: String,
        screen: String,
        model: String,
        appVersion: String
    ): ZibeResult<Unit> = zibeCatching {
        val ref = firebaseRefsContainer.refAppFeedback
            .child(screen)
            .push()

        ref.apply {
            child("id").setValue(myUid)
            child("name").setValue(myUserName)
            child("email").setValue(myEmail)
            child("feedback").setValue(feedback)
            child("device").setValue(model)
            child("appVersion").setValue(appVersion)
            child("createdAt").setValue(ServerValue.TIMESTAMP)
        }
    }

    // ============================================================
    // USER NODE (alta)
    // ============================================================

    override suspend fun createUserNode(
        firebaseUser: FirebaseUser,
        name: String,
        birthDate: String,
        description: String
    ) {
        val id = firebaseUser.uid
        val userName = name
        val createdAt = now()
        val age = if (birthDate.isBlank()) 0 else ageCalculator(birthDate)
        val email: String = firebaseUser.email ?: ""
        val photoUrl: String = firebaseUser.photoUrl?.toString() ?: DEFAULT_PROFILE_PHOTO_URL

        val newUser = Users(
            id = id,
            name = userName,
            birthDate = birthDate,
            createdAt = createdAt,
            age = age,
            email = email,
            photoUrl = photoUrl,
            isOnline = true,
            distanceMeters = 0.0,
            description = description,
            latitude = 0.0,
            longitude = 0.0
        )

        accountRef(id)
            .setValue(newUser)
            .await()
    }

    override suspend fun accountExists(uid: String): Boolean =
        firebaseRefsContainer.refAccounts.child(uid).get().await().exists()

    override suspend fun hasBirthDate(uid: String): Boolean =
        firebaseRefsContainer.refAccounts
            .child(uid)
            .child(AccountsKeys.BIRTHDATE) // OLD: BIRTHDAY
            .get()
            .await()
            .getValue(String::class.java)
            .orEmpty()
            .isNotBlank()

    // ============================================================
    // FAVORITES
    // ============================================================

    suspend fun isUserFavorite(otherUid: String): Boolean {
        val snap = favoriteListRef()
            .child(otherUid)
            .awaitSnapshot()
        return snap.exists()
    }

    // OLD: toggleFavoriteUser(userId, isFavorite) mantiene firma para no romper llamadas
    suspend fun toggleFavoriteUser(userId: String, isFavorite: Boolean) {
        val otherUid = userId // renombre mental
        val ref = favoriteListRef().child(otherUid)

        if (isFavorite) {
            ref.removeValue().await()
        } else {
            // antes guardabas el uid como valor; ahora guardamos boolean para simpleza
            ref.setValue(true).await()
        }
    }

    // ============================================================
    // BLOCK STATE (ConversationKeys.STATE)
    // ============================================================

    data class BlockState(
        val iBlockedUser: Boolean,
        val userBlockedMe: Boolean
    )

    suspend fun getBlockStateWith(otherUid: String, nodeType: String = NODE_DM): BlockState {
        val meState = conversationRef(ownerUid = myUid, nodeType = nodeType, otherUid = otherUid)
            .child(ConversationKeys.STATE) // OLD: NODE_CHAT_STATE
            .awaitSnapshot()
            .getValue(String::class.java)
            .orEmpty()

        val otherState = conversationRef(ownerUid = otherUid, nodeType = nodeType, otherUid = myUid)
            .child(ConversationKeys.STATE)
            .awaitSnapshot()
            .getValue(String::class.java)
            .orEmpty()

        return BlockState(
            iBlockedUser = meState == CHAT_STATE_BLOQ,
            userBlockedMe = otherState == CHAT_STATE_BLOQ
        )
    }

    // ============================================================
    // UNREAD CHATS (Flow)  - suma unreadCount
    // ============================================================

    fun observeUnreadChatList(nodeType: String = NODE_DM): Flow<Int> = callbackFlow {
        // OLD: orderByChild("noVisto")
        val query = conversationsRootRef(myUid, nodeType)
            .orderByChild(ConversationKeys.UNREAD_COUNT)
            .startAt(1.0)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var total = 0
                snapshot.children.forEach { child ->
                    total += child.child(ConversationKeys.UNREAD_COUNT).getValue(Int::class.java)
                        ?: 0
                }
                trySend(total)
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    // ============================================================
    // CHAT PHOTOS (lee /Chats/{nodeType}/{chatId})
    // ============================================================


    // ============================================================
    // UNREAD COUNTS (legacy helpers)
    // ============================================================

    /**
     * OLD: toggleUnreadBadge(userId, chatType) manipulaba "noVisto" + un contador global.
     * Nuevo: solo alterna ConversationKeys.UNREAD_COUNT (no toca ChatList).
     */
    suspend fun toggleUnreadBadge(otherUid: String, nodeType: String) {
        val ref = conversationRef(
            nodeType = nodeType,
            otherUid = otherUid
        ).child(ConversationKeys.UNREAD_COUNT)
        val current = ref.awaitSnapshot().getValue(Int::class.java) ?: 0
        ref.setValue(if (current > 0) 0 else 1).await()
    }


    suspend fun setChatAsReadChatList(otherUid: String, nodeType: String) {
        val ref = conversationRef(
            nodeType = nodeType,
            otherUid = otherUid
        ).child(ConversationKeys.UNREAD_COUNT)
        val current = ref.awaitSnapshot().getValue(Int::class.java) ?: 0
        if (current > 0) ref.setValue(0).await()
    }

    // ============================================================
    // CHAT STATE (ConversationKeys.STATE)
    // ============================================================

    override suspend fun getChatStateWith(otherUid: String, nodeType: String): String {
        return conversationRef(nodeType = nodeType, otherUid = otherUid)
            .child(ConversationKeys.STATE) // OLD: NODE_CHAT_STATE
            .awaitSnapshot()
            .getValue(String::class.java)
            .orEmpty()
    }

    suspend fun updateStateChatWith(
        otherUid: String,
        otherName: String,
        nodeType: String,
        newState: String
    ) {
        val chatRef = conversationRef(nodeType = nodeType, otherUid = otherUid)
        val snapshot = chatRef.awaitSnapshot()

        if (!snapshot.exists()) {
            val newConversation = createDefaultConversation(otherUid, otherName, newState)
            chatRef.setValue(newConversation).await()
            return
        }

        // OLD: "wUserPhoto" / EMPTY
        val photo =
            snapshot.child(ConversationKeys.OTHER_PHOTO_URL).getValue(String::class.java).orEmpty()
        if (photo == EMPTY) {
            chatRef.removeValue().await()
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

    // ============================================================
    // PRESENCE / STATUS (nuevo)
    // ============================================================

    override suspend fun setUserActivityStatus(status: String) {
        presenceRepository.setActivityStatus(status)
    }

    override suspend fun setUserLastSeen() {
        presenceRepository.setLastSeenNow()
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
        else UserStatus.LastSeen(context.getString(R.string.ultVez) + " $lastSeenText")
    }

    fun observeUserStatus(
        otherUid: String,
        nodeType: String
    ): Flow<UserStatus> = callbackFlow {

        val statusRef = statusRef()

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val base = snapshot.toUserStatus { ms -> formatLastSeen(ms, context) }

                if (base !is UserStatus.TypingOrRecording) {
                    trySend(base)
                    return
                }

                launch {

                    val otherAt = activeThreadRef(otherUid).get().await()

                    val node =
                        otherAt.child(ActiveThreadKeys.NODE_TYPE).getValue(String::class.java)
                            .orEmpty()
                    val other =
                        otherAt.child(ActiveThreadKeys.OTHER_UID).getValue(String::class.java)
                            .orEmpty()

                    val matches = when (nodeType) {
                        NODE_DM ->
                            node == NODE_DM && other == myUid

                        NODE_GROUP_DM ->
                            node == NODE_GROUP_DM && other == myUid

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

    // ============================================================
    // UTILS
    // ============================================================

    override suspend fun deleteMyAccountData(): ZibeResult<Unit> =
        zibeCatching {
            firebaseRefsContainer.refData.child(myUid).removeValue().await()
            firebaseRefsContainer.refAccounts.child(myUid).removeValue().await()
            deleteProfilePhoto()
        }


    suspend fun setActiveThread(otherUid: String, nodeType: String) {
        activeThreadRef().setValue(
            mapOf(
                ActiveThreadKeys.NODE_TYPE to nodeType,
                ActiveThreadKeys.OTHER_UID to otherUid,
            )
        ).await()
    }

    suspend fun clearActiveThread() {
        activeThreadRef().removeValue().await()
    }

    suspend fun setReadGroupMessages(readCount: Int) {
        readGroupMessagesRef()
            .setValue(readCount)
            .await()
    }

    suspend fun getReadGroupMessages(): Int {
        return readGroupMessagesRef()
            .get()
            .await()
            .getValue(Int::class.java) ?: 0
    }


}
