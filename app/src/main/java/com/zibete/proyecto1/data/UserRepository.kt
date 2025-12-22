package com.zibete.proyecto1.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users

import com.zibete.proyecto1.ui.constants.Constants.StatusKeys
import com.zibete.proyecto1.ui.constants.Constants.AccountsKeys
import com.zibete.proyecto1.ui.constants.Constants.ActiveThreadKeys
import com.zibete.proyecto1.ui.constants.Constants.ActiveViewKeys
import com.zibete.proyecto1.ui.constants.Constants.ChatKeys
import com.zibete.proyecto1.ui.constants.Constants.ChatListKeys
import com.zibete.proyecto1.ui.constants.Constants.ConversationKeys

import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.EMPTY
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.NODE_ACTIVE_VIEW
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_CLIENT_DATA
import com.zibete.proyecto1.ui.constants.Constants.NODE_DM
import com.zibete.proyecto1.ui.constants.Constants.NODE_FAVORITE_LIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.ui.constants.Constants.NODE_STATUS
import com.zibete.proyecto1.ui.constants.Constants.PATH_PROFILE_PHOTOS
import com.zibete.proyecto1.ui.constants.Constants.PROFILE_PHOTO
import com.zibete.proyecto1.utils.Utils
import com.zibete.proyecto1.utils.Utils.now
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

// ------------------------------------------------------------
// Atajo opcional para lecturas one-shot con coroutines
// ------------------------------------------------------------
private suspend fun Query.awaitSnapshot(): DataSnapshot = get().await()

@Singleton
class UserRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userSessionManager: UserSessionManager,
    private val chatRepository: ChatRepository,
    private val presenceRepository: PresenceRepository,
    @ApplicationContext private val context: Context
) {

    // ============================================================
    // SESSION (cache local)
    // ============================================================

    val myUid get() = userSessionManager.myUid
    val firebaseUser get() = userSessionManager.firebaseUser

    var myUserName: String = ""
        private set

    var myProfilePhotoUrl: String = ""
        private set

    var myEmail: String = ""
        private set

    fun updateLocalProfile(name: String, photoUrl: String, email: String) {
        myUserName = name
        myProfilePhotoUrl = photoUrl
        myEmail = email
    }

    val latitude: Double get() = userSessionManager.latitude
    val longitude: Double get() = userSessionManager.longitude

    fun updateMyLocation(lat: Double, lon: Double) {
        userSessionManager.latitude = lat
        userSessionManager.longitude = lon
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

    private fun conversationsRootRef(ownerUid: String, nodeType: String) =
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
            .child(NODE_CLIENT_DATA)
            .child(NODE_CHATLIST)

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

    suspend fun putProfilePhotoInStorage(localUri: Uri) {
        getProfilePhotoStoragePath()
            .putFile(localUri)
            .await()
    }

    suspend fun deleteProfilePhoto() {
        getProfilePhotoStoragePath()
            .delete()
            .await()
    }

    suspend fun getProfilePhotoUrl(): String? {
        return try {
            getProfilePhotoStoragePath()
                .downloadUrl
                .await()
                .toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * EditProfile típico:
     * Storage (put) -> URL -> DB (/Users/Accounts/<uid>/photoUrl)
     */
    suspend fun updateProfilePhoto(localUri: Uri): String? {
        putProfilePhotoInStorage(localUri)
        val url = getProfilePhotoUrl()
        if (!url.isNullOrBlank()) {
            updateUserFields(mapOf(AccountsKeys.PHOTO_URL to url))
        }
        return url
    }

    suspend fun deleteProfilePhotoAndResetDefault() {
        runCatching { deleteProfilePhoto() }
        updateUserFields(mapOf(AccountsKeys.PHOTO_URL to DEFAULT_PROFILE_PHOTO_URL))
    }

    // ============================================================
    // SPLASH (routing)
    // ============================================================

    suspend fun getAccountSnapshot(uid: String): DataSnapshot =
        accountRef(uid).awaitSnapshot()

    suspend fun getAccount(uid: String): Users? =
        getAccountSnapshot(uid)
            .takeIf { it.exists() }
            ?.getValue(Users::class.java)

    // ============================================================
    // EDIT PROFILE (updates)
    // ============================================================

    suspend fun updateUserFields(fields: Map<String, Any?>, uid: String = myUid) {
        val clean = fields.filterValues { it != null }
        if (clean.isEmpty()) return
        accountRef(uid).updateChildren(clean).await()
    }

    suspend fun updateUserName(userName: String) =
        updateUserFields(mapOf(AccountsKeys.NAME to userName))

    suspend fun updateBirthDate(birthDate: String) =
        updateUserFields(mapOf(AccountsKeys.BIRTHDATE to birthDate))


    suspend fun updateDescription(description: String) =
        updateUserFields(mapOf(AccountsKeys.DESCRIPTION to description))

    suspend fun updateEmail(email: String) =
        updateUserFields(mapOf(AccountsKeys.EMAIL to email))

    // ============================================================
    // USER NODE (alta)
    // ============================================================

    suspend fun createUserNode(
        firebaseUser: FirebaseUser,
        birthDate: String,
        description: String
    ) {
        val email: String = firebaseUser.email ?: ""
        val photoUrl: String = firebaseUser.photoUrl?.toString() ?: DEFAULT_PROFILE_PHOTO_URL

        val newUser = Users(
            id = firebaseUser.uid,
            name = firebaseUser.displayName.orEmpty(),
            birthDate = birthDate,
            createdAt = now(),
            age = Utils.calcAge(birthDate),
            email = email,
            photoUrl = photoUrl,
            isOnline = true,
            distanceMeters = 0.0,
            description = description,
            latitude = 0.0,
            longitude = 0.0
        )

        accountRef(firebaseUser.uid)
            .setValue(newUser)
            .await()
    }

    suspend fun hasBirthDate(uid: String): Boolean =
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

    suspend fun getChatPhotosWithUser(otherUid: String, nodeType: String): List<String> {
        val chatId = chatRepository.getChatId(otherUid)
        val refChat = firebaseRefsContainer.refChatsRoot
            .child(nodeType)
            .child(chatId)

        val photos = mutableListOf<String>()

        fun collectFrom(snapshot: DataSnapshot) {
            snapshot.children.forEach { msgSnap ->
                val type = msgSnap.child(ChatKeys.TYPE).getValue(Int::class.java)
                val senderUid = msgSnap.child(ChatKeys.SENDER_UID).getValue(String::class.java)
                val content = msgSnap.child(ChatKeys.CONTENT).getValue(String::class.java)

                // mantenemos tu regla: fotos enviadas por el otro
                if (!senderUid.isNullOrBlank() && senderUid != myUid) {
                    if (type == MSG_PHOTO || type == MSG_PHOTO_SENDER_DLT) {
                        if (!content.isNullOrEmpty()) photos.add(content)
                    }
                }
            }
        }

        val snap = refChat.awaitSnapshot()
        if (snap.exists()) collectFrom(snap)

        return photos.distinct()
    }

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

    suspend fun getChatStateWith(otherUid: String, nodeType: String): String {
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
            snapshot.child(ConversationKeys.OTHER_PHOTO).getValue(String::class.java).orEmpty()
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
            lastDate = now(),
            date = null, // no firebase
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

    suspend fun setUserActivityStatus(status: String) {
        presenceRepository.setActivityStatus(status)
    }

    suspend fun setUserLastSeen() {
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

                val base = snapshot.toUserStatus { ms -> Utils.formatLastSeen(ms) }

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

    suspend fun deleteMyAccountData() {
        firebaseRefsContainer.refData.child(myUid).removeValue().await()
        firebaseRefsContainer.refAccounts.child(myUid).removeValue().await()
        runCatching { getProfilePhotoStoragePath().delete().await() }
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
