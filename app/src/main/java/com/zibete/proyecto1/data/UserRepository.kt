package com.zibete.proyecto1.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.data.UserRepository.AccountKeys.BIRTHDAY
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.Constants.EMPTY
import com.zibete.proyecto1.ui.constants.Constants.KEY_ACTIVE_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHAT_STATE
import com.zibete.proyecto1.ui.constants.Constants.NODE_DM
import com.zibete.proyecto1.ui.constants.Constants.NODE_FAVORITE_LIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_STATUS
import com.zibete.proyecto1.ui.constants.Constants.NODE_UNREAD_COUNT
import com.zibete.proyecto1.ui.constants.Constants.NODE_USERS_ROOT
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
    // Keys del schema (Firebase)
    // ============================================================

    object AccountKeys {
        const val NAME = "name"
        const val BIRTHDAY = "birthDay"
        const val CREATED_AT = "createdAt"
        const val AGE = "age"
        const val EMAIL = "email"
        const val PHOTO_URL = "photoUrl"
        const val IS_ONLINE = "isOnline"
//        const val FCM_TOKEN = "fcmToken"
        const val DESCRIPTION = "description"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
    }

    // ============================================================
    // Refs helpers (DB)
    // ============================================================

    private fun myAccountRef(uid: String = myUid) =
        firebaseRefsContainer.refAccounts.child(uid)

    private fun myDataRef(uid: String = myUid) =
        firebaseRefsContainer.refData.child(uid)

    private fun myStatusRef(uid: String = myUid) =
        myDataRef(uid).child(NODE_STATUS)

    private fun myFavoriteListRef(uid: String = myUid) =
        myDataRef(uid).child(NODE_FAVORITE_LIST)

    private fun myCurrentChatRef(uid: String = myUid) =
        myDataRef(uid).child(NODE_DM)

    private fun myChatListUnreadRef(uid: String = myUid) =
        myDataRef(uid).child(NODE_CHATLIST).child(NODE_UNREAD_COUNT)

    private fun getChatWithRef(
        ownerUid: String = myUid,
        chatType: String,
        otherUid: String
    ) = firebaseRefsContainer.refData
        .child(ownerUid)
        .child(chatType)
        .child(otherUid)

    // ============================================================
    // Refs helpers (STORAGE)
    // ============================================================

    fun getProfilePhotoStoragePath(
        fileName: String = PROFILE_PHOTO
    ) = firebaseRefsContainer.firebaseStorage.reference
        .child(NODE_USERS_ROOT)
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
     * Storage (put) -> URL -> DB (/Cuentas/<uid>/photoUrl)
     */
    suspend fun updateProfilePhoto(localUri: Uri): String? {
        putProfilePhotoInStorage(localUri)
        val url = getProfilePhotoUrl()
        if (!url.isNullOrBlank()) {
            updateUserFields(mapOf(AccountKeys.PHOTO_URL to url))
        }
        return url
    }

    suspend fun deleteProfilePhotoAndResetDefault() {
        runCatching { deleteProfilePhoto() }
        updateUserFields(mapOf(AccountKeys.PHOTO_URL to DEFAULT_PROFILE_PHOTO_URL))
    }

    // ============================================================
    // SPLASH (token / routing)
    // ============================================================

    suspend fun getAccountSnapshot(uid: String): DataSnapshot =
        myAccountRef(uid).awaitSnapshot()

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
        myAccountRef(uid).updateChildren(clean).await()
    }

    suspend fun updateUserName(userName: String) =
        updateUserFields(mapOf(AccountKeys.NAME to userName))

    suspend fun updateBirthDay(birthDay: String) =
        updateUserFields(mapOf(AccountKeys.BIRTHDAY to birthDay))

    suspend fun updateDescription(description: String) =
        updateUserFields(mapOf(AccountKeys.DESCRIPTION to description))

    suspend fun updateEmail(email: String) =
        updateUserFields(mapOf(AccountKeys.EMAIL to email))

    // ============================================================
    // USER NODE (alta)
    // ============================================================

    suspend fun createUserNode(
        firebaseUser: FirebaseUser,
        birthDate: String,
        description: String)
    {

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

        myAccountRef(firebaseUser.uid)
            .setValue(newUser)
            .await()
    }

    suspend fun hasBirthDate(uid: String): Boolean =
        firebaseRefsContainer.refAccounts
            .child(uid)
            .child(BIRTHDAY)
            .get()
            .await()
            .getValue(String::class.java)
            .orEmpty()
            .isNotBlank()

    // ============================================================
    // FAVORITES
    // ============================================================

    suspend fun isUserFavorite(otherUid: String): Boolean {
        val snap = myFavoriteListRef()
            .child(otherUid)
            .awaitSnapshot()
        return snap.exists()
    }

    suspend fun toggleFavoriteUser(userId: String, isFavorite: Boolean) {
        val ref = myFavoriteListRef().child(userId)
        if (isFavorite) {
            ref.removeValue().await()
        } else {
            ref.setValue(userId).await()
        }
    }

    // ============================================================
    // BLOCK STATE
    // ============================================================

    data class BlockState(
        val iBlockedUser: Boolean,
        val userBlockedMe: Boolean
    )

    suspend fun getBlockStateWith(userId: String): BlockState {
        val meSnap = myCurrentChatRef()
            .child(userId)
            .child(NODE_CHAT_STATE)
            .awaitSnapshot()

        val otherSnap = myCurrentChatRef(userId)
            .child(myUid)
            .child(NODE_CHAT_STATE)
            .awaitSnapshot()

        val isBlocked = meSnap.getValue(String::class.java) == CHAT_STATE_BLOQ
        val blockedMe = otherSnap.getValue(String::class.java) == CHAT_STATE_BLOQ

        return BlockState(
            iBlockedUser = isBlocked,
            userBlockedMe = blockedMe
        )
    }

    // ============================================================
    // UNREAD CHATS (Flow)
    // ============================================================

    fun observeUnreadChats(): Flow<Int> = callbackFlow {
        val query = myCurrentChatRef()
            .orderByChild("noVisto")
            .startAt(1.0)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                snapshot.children.forEach { child ->
                    count += child.child("noVisto").getValue(Int::class.java) ?: 0
                }
                trySend(count)
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)


    // ============================================================
    // CHAT PHOTOS
    // ============================================================

    suspend fun getChatPhotosWithUser(userId: String, nodeType: String): List<String> {
        val chatId = chatRepository.getChatId(userId)
        val refChat = firebaseRefsContainer.refChatsRoot
            .child(nodeType)
            .child(chatId)

        val photos = mutableListOf<String>()

        fun collectFrom(snapshot: DataSnapshot) {
            snapshot.children.forEach { msgSnap ->
                val type = msgSnap.child("type").getValue(Int::class.java)
                val sender = msgSnap.child("envia").getValue(String::class.java)
                val message = msgSnap.child("mensaje").getValue(String::class.java)

                if (sender != null && sender != myUid) {
                    if (type == Constants.MSG_PHOTO || type == Constants.MSG_PHOTO_SENDER_DLT) {
                        if (!message.isNullOrEmpty()) photos.add(message)
                    }
                }
            }
        }

        val snap = refChat.awaitSnapshot()
        if (snap.exists()) collectFrom(snap)

        return photos.distinct()
    }

    // ============================================================
    // SEEN / UNREAD COUNTS
    // ============================================================

    suspend fun toggleUnreadBadge(userId: String, chatType: String) {
        val chatWithRef = getChatWithRef(chatType = chatType, otherUid = userId)
        val noSeenRef = chatWithRef.child("noVisto")
        val noSeenChatList = myChatListUnreadRef()

        val noSeenSnap = noSeenRef.awaitSnapshot()
        val noSeenListSnap = noSeenChatList.awaitSnapshot()

        val currentNoSeenChatWith = noSeenSnap.getValue(Int::class.java) ?: 0
        val currentNoSeenChatList = noSeenListSnap.getValue(Int::class.java) ?: 0

        if (currentNoSeenChatWith > 0) {
            noSeenRef.setValue(0).await()
            noSeenChatList.setValue(currentNoSeenChatList - currentNoSeenChatWith).await()
        } else {
            noSeenRef.setValue(1).await()
            noSeenChatList.setValue(currentNoSeenChatList + 1).await()
        }
    }

    /**
     * (Nuevo) “set a leído” sin toggle.
     * Útil cuando abrís un chat y querés SIEMPRE dejarlo en 0.
     */
    suspend fun setChatAsReadChatList(userId: String, chatType: String) {
        val chatWithRef = getChatWithRef(chatType = chatType, otherUid = userId)
        val noSeenRef = chatWithRef.child("noVisto")
        val noSeenChatList = myChatListUnreadRef()

        val noSeenSnap = noSeenRef.awaitSnapshot()
        val noSeenListSnap = noSeenChatList.awaitSnapshot()

        val currentNoSeenChatWith = noSeenSnap.getValue(Int::class.java) ?: 0
        val currentNoSeenChatList = noSeenListSnap.getValue(Int::class.java) ?: 0

        if (currentNoSeenChatWith > 0) {
            noSeenRef.setValue(0).await()
            noSeenChatList.setValue(currentNoSeenChatList - currentNoSeenChatWith).await()
        }
    }

    // ============================================================
    // CHAT STATE
    // ============================================================

    suspend fun getChatStateWith(userId: String, chatType: String): String {
        return getChatWithRef(chatType = chatType, otherUid = userId)
            .child(NODE_CHAT_STATE)
            .awaitSnapshot()
            .getValue(String::class.java)
            ?: NODE_DM
    }

    suspend fun updateStateChatWith(
        userId: String,
        userName: String,
        nodeType: String,
        newState: String
    ) {
        val chatRef = getChatWithRef(chatType = nodeType, otherUid = userId)
        val snapshot = chatRef.awaitSnapshot()

        if (!snapshot.exists()) {
            val newChat = createDefaultChatWith(userId, userName, newState)
            chatRef.setValue(newChat).await()
            return
        }

        val photo = snapshot.child("wUserPhoto").getValue(String::class.java).orEmpty()
        if (photo == EMPTY) {
            chatRef.removeValue().await()
            return
        }

        chatRef.child(NODE_CHAT_STATE).setValue(newState).await()
    }

    private fun createDefaultChatWith(
        chatWithId: String,
        userName: String,
        newState: String
    ): Conversation {
        return Conversation(
            "Chat vacío",
            now(),
            null,
            "",
            chatWithId,
            userName,
            DEFAULT_PROFILE_PHOTO_URL,
            newState,
            0,
            1
        )
    }

    // ============================================================
    // PRESENCE / STATUS (nuevo)
    // ============================================================

    suspend fun setUserActivityStatus(status: String) {
        presenceRepository.setActivityStatus(myUid, status)
    }

    // Opcional: si querés forzar lastSeen en logout (además de onDisconnect)
    suspend fun setUserLastSeen() {
        presenceRepository.setLastSeenNow(myUid)
    }

    private fun DataSnapshot.toUserStatus(
        chatType: String,
        lastSeenFormatter: (Long) -> String
    ): UserStatus {
        if (!exists()) return UserStatus.Offline

        val status = child("status").getValue(String::class.java).orEmpty()
        val lastSeenMs = child("lastSeenMs").getValue(Long::class.java) ?: 0L

        if (status == context.getString(R.string.online)) return UserStatus.Online

        if (status == context.getString(R.string.typing) ||
            status == context.getString(R.string.recording)
        ) {
            // La decisión final depende de si está chateando conmigo
            // (esto se resuelve fuera con una lectura de ACTIVE_CHAT)
            return UserStatus.TypingOrRecording(status)
        }

        val lastSeenText = lastSeenFormatter(lastSeenMs)
        return if (lastSeenText.isBlank()) UserStatus.Offline
        else UserStatus.LastSeen("Últ. vez $lastSeenText")
    }

    fun observeUserStatus(userId: String, chatType: String): kotlinx.coroutines.flow.Flow<UserStatus> =
        kotlinx.coroutines.flow.callbackFlow {

            val statusRef = firebaseRefsContainer.refData
                .child(userId)
                .child(NODE_STATUS)

            val activeChatRef = firebaseRefsContainer.refData
                .child(userId)
                .child(NODE_CHATLIST)
                .child(KEY_ACTIVE_CHAT)

            val lastSeenFormatter: (Long) -> String = { ms ->
                // usa tu formatter correcto con Date(ms)
                Utils.formatLastSeen(ms)
            }

            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {

                    val base = snapshot.toUserStatus(chatType, lastSeenFormatter)

                    // Si no es typing/recording, enviamos directo
                    if (base !is UserStatus.TypingOrRecording) {
                        trySend(base)
                        return
                    }

                    // typing/recording: ver si está en el chat conmigo
                    launch {
                        val currentChat = activeChatRef.get().await()
                            .getValue(String::class.java)
                            .orEmpty()

                        val expected = chatRepository.buildActiveChatKey(userId, node)

                        if (currentChat == expected) {
                            trySend(base) // typing/recording real
                        } else {
                            trySend(UserStatus.Online) // no está en mi chat: “online”
                        }
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            }

            statusRef.addValueEventListener(listener)
            awaitClose { statusRef.removeEventListener(listener) }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)


    // ============================================================
    // UTILS
    // ============================================================


    fun deleteMyAccountData() {
        firebaseRefsContainer.refData.child(myUid).removeValue()
        firebaseRefsContainer.refAccounts.child(myUid).removeValue()
        getProfilePhotoStoragePath().delete()
    }

}
