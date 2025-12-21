package com.zibete.proyecto1.data

import android.net.Uri
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatGroup
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.constants.Constants.ChatGroupKeys
import com.zibete.proyecto1.ui.constants.Constants.ChatListKeys
import com.zibete.proyecto1.ui.constants.Constants.ConversationKeys
import com.zibete.proyecto1.ui.constants.Constants.GroupMetaKeys
import com.zibete.proyecto1.ui.constants.Constants.GroupUserKeys
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_CLIENT_DATA
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.ui.constants.Constants.PATH_PHOTOS
import com.zibete.proyecto1.utils.Utils.dateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class GroupRefs(
    val refGroupPhotos: com.google.firebase.storage.StorageReference
)

data class GroupChatItem(
    val id: String,
    val message: ChatGroup
)

sealed interface GroupChatChildEvent {
    data class Added(val item: GroupChatItem) : GroupChatChildEvent
    data class Changed(val item: GroupChatItem) : GroupChatChildEvent
    data class Removed(val item: GroupChatItem) : GroupChatChildEvent
}

@Singleton
class GroupRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository,
    private val appScope: CoroutineScope
) {
    private val myUid: String get() = userRepository.myUid

    private val cacheGroupTotalByName: MutableMap<String, StateFlow<Int>> = mutableMapOf()

    // cache de readCount (1 valor global por usuario, para el "grupo actual")
    private val cacheGroupReadCount: StateFlow<Int> =
        observeGroupReadCount(myUid)
            .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)

    // =========================
    // Refs
    // =========================

    fun buildGroupRefs(groupName: String): GroupRefs {
        // ⚠️ storage moderno para fotos de chat de grupo (según tu decisión):
        // /photos/groups/{groupName}/...
        val refGroupPhotos = firebaseRefsContainer.firebaseStorage.reference
            .child(PATH_PHOTOS)
            .child("groups")
            .child(groupName)

        // (Legacy que tenías antes, lo dejo comentado por si necesitás migrar URLs viejas)
        // val legacy = firebaseRefsContainer.firebaseStorage.reference
        //     .child("$NODE_CHATS_ROOT/$NODE_GROUP_DM/$groupName/")
        //     .child(PATH_PHOTOS)

        return GroupRefs(refGroupPhotos = refGroupPhotos)
    }

    private fun readGroupMessagesRef(uid: String = myUid): DatabaseReference =
        firebaseRefsContainer.refData
            .child(uid)
            .child(NODE_CLIENT_DATA)
            .child(NODE_CHATLIST)
            .child(ChatListKeys.READ_GROUP_MESSAGES)

    // =========================
    // NUEVAS que te faltaban
    // =========================

    /**
     * Helper: toma el groupName actual desde el Flow del DataStore (vos ya lo tenés: groupNameFlow).
     * No acoplo GroupRepository a tu DataStore repo: se lo pasás desde el VM.
     */


    fun observeMyReadGroupMessages(): Flow<Int> =
        observeGroupReadCount(myUid)

    suspend fun setReadGroupMessages(readCount: Int) {
        readGroupMessagesRef()
            .setValue(readCount.coerceAtLeast(0))
            .await()
    }

    fun observeGroupChatEvents(groupName: String): Flow<GroupChatChildEvent> = callbackFlow {
        val ref = firebaseRefsContainer.refGroupChat.child(groupName)

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val msg = snapshot.getValue(ChatGroup::class.java) ?: ChatGroup()
                trySend(GroupChatChildEvent.Added(GroupChatItem(id, msg)))
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val msg = snapshot.getValue(ChatGroup::class.java) ?: ChatGroup()
                trySend(GroupChatChildEvent.Changed(GroupChatItem(id, msg)))
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.key ?: return
                // si ya no está el payload, mandamos empty (pero con id válido)
                trySend(GroupChatChildEvent.Removed(GroupChatItem(id, ChatGroup())))
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    // =========================
    // BADGES / COUNTS (lo tuyo)
    // =========================

    fun groupsBottomNavBadgeCount(
        groupName: String,
        userId: String = myUid
    ): Flow<Int> =
        combine(
            observeGroupUnreadCount(groupName),
            observeUnreadPrivateChatsInsideGroup(userId)
        ) { unreadGroup, unreadPrivateChats ->
            (unreadGroup + unreadPrivateChats).coerceAtLeast(0)
        }

    fun groupChatUnreadCount(groupName: String): StateFlow<Int> =
        observeGroupUnreadCount(groupName)
            .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)

    suspend fun groupTabUnreadCountOnce(groupName: String, myUid: String): Int {
        val totalSnap = firebaseRefsContainer.refGroupChat
            .child(groupName)
            .get()
            .await()
        val total = totalSnap.childrenCount.toInt().coerceAtLeast(0)

        val readSnap = readGroupMessagesRef(myUid).get().await()
        val read = (readSnap.getValue(Int::class.java) ?: 0).coerceAtLeast(0)

        return (total - read).coerceAtLeast(0)
    }

    // ========== CORE FLOWS ==========

    /** unread del grupo = totalGrupo - readCount */
    private fun observeGroupUnreadCount(groupName: String): Flow<Int> =
        combine(
            groupTotalMsgCount(groupName),
            groupReadCount()
        ) { total, read ->
            (total - read).coerceAtLeast(0)
        }

    /** total mensajes del grupo (realtime) */
//    private fun groupTotalMsgCount(groupName: String): Int = totalMessagesRef(groupName)

    private fun groupTotalMsgCount(groupName: String): StateFlow<Int> =
        cacheGroupTotalByName.getOrPut(groupName) {
            observeGroupTotalMessages(groupName)
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)
        }

    private fun groupReadCount(): StateFlow<Int> = cacheGroupReadCount

    /** suma unreadCount de chats privados dentro del nodo /Users/Data/<uid>/group_dm */
    private fun observeUnreadPrivateChatsInsideGroup(uid: String): Flow<Int> = callbackFlow {
        val ref = groupPrivateConversationsRef(uid)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var total = 0
                for (child in snapshot.children) {
                    total += child.child(ConversationKeys.UNREAD_COUNT).getValue(Int::class.java) ?: 0
                }
                trySend(total.coerceAtLeast(0))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun observeGroupReadCount(uid: String): Flow<Int> = callbackFlow {
        val ref = readGroupMessagesRef(uid)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val read = snapshot.getValue(Int::class.java) ?: 0
                trySend(read.coerceAtLeast(0))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * ⚠️ Hoy esto “cuenta children” del chat => puede ser pesado.
     * Próximo paso pro: reemplazar por /Groups/Meta/{groupName}/totalMessages (contador).
     */
    private fun observeGroupTotalMsgCount(groupName: String): Flow<Int> = callbackFlow {
        val ref = firebaseRefsContainer.refGroupChat.child(groupName)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.childrenCount.toInt().coerceAtLeast(0))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // =========================
    // LO QUE YA TENÍAS (sin borrar)
    // =========================

    fun observeGroupUserAvailability(
        groupName: String,
        userId: String
    ): Flow<Boolean> = callbackFlow {

        val ref = firebaseRefsContainer.refGroupUsers
            .child(groupName)
            .child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(ds: DataSnapshot) {
                trySend(ds.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun getUserGroup(userId: String = myUid, groupName: String): UserGroup? =
        firebaseRefsContainer.refGroupUsers
            .child(groupName)
            .child(userId)
            .get()
            .await()
            .takeIf { it.exists() }
            ?.getValue(UserGroup::class.java)

    suspend fun getGroup(groupName: String): Groups? =
        firebaseRefsContainer.refGroupMeta
            .child(groupName)
            .get()
            .await()
            .takeIf { it.exists() }
            ?.getValue(Groups::class.java)

    suspend fun isNickInUse(groupName: String, nick: String): Boolean {
        val snapshot = firebaseRefsContainer.refGroupUsers
            .child(groupName)
            .get()
            .await()

        return snapshot.children.any { child ->
            val name = child.child(GroupUserKeys.USER_NAME).getValue(String::class.java)
            name == nick
        }
    }

//    suspend fun sendJoinMessage(
//        groupName: String,
//        nick: String,
//        type: Int
//    ) {
//        val chatMsg = ChatGroup(
//            MSG_USER_JOINED,
//            now(),
//            nick,
//            myUid,
//            MSG_TYPE_MID,
//            type
//        )
//
//        firebaseRefsContainer.refGroupChat
//            .child(groupName)
//            .push()
//            .setValue(chatMsg)
//            .await()
//    }
//
//    suspend fun saveUserInGroup(
//        groupName: String,
//        nick: String,
//        type: Int
//    ) {
//        val userGroup = UserGroup(
//            userId = myUid,
//            userName = nick,
//            type = type,
//            joinedAtMs = System.currentTimeMillis()
//        )
//
//        firebaseRefsContainer.refGroupUsers
//            .child(groupName)
//            .child(myUid)
//            .setValue(userGroup)
//            .await()
//    }

//    suspend fun isGroupNameInUse(name: String): Boolean {
//        val snapshot = firebaseRefsContainer.refGroupMeta
//            .child(name)
//            .get()
//            .await()
//        return snapshot.exists()
//    }
//
//    suspend fun createGroup(
//        groupName: String,
//        groupData: String,
//        groupType: Int
//    ) {
//        val group = Groups(
//            name = groupName,
//            description = groupData,
//            creatorUid = myUid,
//            type = groupType,
//            users = 0,
//            createdAt = dateTime()
//        )
//
//        firebaseRefsContainer.refGroupMeta
//            .child(groupName)
//            .setValue(group)
//            .await()
//    }

    suspend fun getAllGroups(): List<Groups> {
        val snapshot = firebaseRefsContainer.refGroupMeta
            .get()
            .await()

        if (!snapshot.exists()) return emptyList()

        val groupsList = mutableListOf<Groups>()

        for (groupSnap in snapshot.children) {
            val group = groupSnap.getValue(Groups::class.java) ?: continue

            // nombre del grupo (alineado a GroupMetaKeys.NAME si existe)
            val name =
                groupSnap.child(GroupMetaKeys.NAME).getValue(String::class.java)
                    ?: groupSnap.child("name").getValue(String::class.java)
                    ?: group.name

            val usersSnap = firebaseRefsContainer.refGroupUsers
                .child(name)
                .get()
                .await()

            group.users = usersSnap.childrenCount.toInt()
            groupsList.add(group)
        }

        return groupsList
    }

//    suspend fun removeMyGroupChatList(userId: String) {
//        firebaseRefsContainer.refData
//            .child(userId)
//            .child(NODE_GROUP_DM)
//            .removeValue()
//            .await()
//    }
//
//    suspend fun removeMyPrivateGroupChats(userId: String) {
//        val snapshot = firebaseRefsContainer.refChatsGroupDm
//            .get()
//            .await()
//
//        for (child in snapshot.children) {
//            val key = child.key ?: continue
//            if (key.contains(userId)) {
//                child.ref.removeValue().await()
//            }
//        }
//    }

//    suspend fun sendLeaveGroupMessage(
//        groupName: String,
//        userName: String,
//        userType: Int,
//        userId: String
//    ) {
//        val chatMsg = ChatGroup(
//            MSG_USER_JOINED, // TODO: si tenés MSG_USER_LEFT, reemplazar acá
//            now(),
//            userName,
//            userId,
//            MSG_TYPE_MID,
//            userType
//        )
//
//        firebaseRefsContainer.refGroupChat
//            .child(groupName)
//            .push()
//            .setValue(chatMsg)
//            .await()
//    }

//    suspend fun removeUserFromGroup(
//        groupName: String,
//        userId: String
//    ) {
//        firebaseRefsContainer.refGroupUsers
//            .child(groupName)
//            .child(userId)
//            .removeValue()
//            .await()
//    }



















        // =========================
        // Refs (según tu schema)
        // =========================

        private fun groupUsersRef(groupName: String): DatabaseReference =
            firebaseRefsContainer.refGroupUsers.child(groupName)

        private fun groupChatRef(groupName: String): DatabaseReference =
            firebaseRefsContainer.refGroupChat.child(groupName)

        /**
         * /Groups/Meta/{groupName}/totalMessages
         */
        private fun totalMessagesRef(groupName: String): DatabaseReference =
            firebaseRefsContainer.refGroupMeta
                .child(groupName)
                .child(GroupMetaKeys.TOTAL_MESSAGES)



        /**
         * /Users/Data/{uid}/{NODE_GROUP_DM}  (conversations privadas dentro de grupo)
         */
        private fun groupPrivateConversationsRef(uid: String): DatabaseReference =
            firebaseRefsContainer.refData.child(uid).child(NODE_GROUP_DM)

        // =========================
        // Models auxiliares (para listas incrementales)
        // =========================

        data class GroupChatItem(
            val id: String,
            val message: ChatGroup
        )

        sealed interface GroupChatChildEvent {
            data class Added(val item: GroupChatItem) : GroupChatChildEvent
            data class Changed(val item: GroupChatItem) : GroupChatChildEvent
            data class Removed(val id: GroupChatItem) : GroupChatChildEvent
        }

        // =========================
        // Meta: totalMessages
        // =========================

        suspend fun ensureGroupMeta(groupName: String) {
            val snap = totalMessagesRef(groupName).get().await()
            if (!snap.exists()) {
                // Creamos SOLO el hijo totalMessages, no pisamos nada más.
                totalMessagesRef(groupName).setValue(0).await()
            }
        }

        private suspend fun incrementTotalMessages(groupName: String) {
            ensureGroupMeta(groupName)

            // Firebase puede devolverte Number como Long/Int/Double según historial de datos.
            // Por eso parseamos como Number y lo pasamos a Long.
            totalMessagesRef(groupName).runTransactionAwait { curAny ->
                val cur = (curAny as? Number)?.toLong() ?: 0L
                cur + 1L
            }
        }

        fun observeGroupTotalMessages(groupName: String): Flow<Int> = callbackFlow {
            val ref = totalMessagesRef(groupName)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = (snapshot.getValue(Long::class.java) ?: 0L).toInt()
                    trySend(value)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }.flowOn(Dispatchers.IO)

        // =========================
        // Read / Unread (grupo)
        // =========================

        suspend fun markGroupAsRead(groupName: String) {
            val total = (totalMessagesRef(groupName).get().await().getValue(Long::class.java) ?: 0L).toInt()
            readGroupMessagesRef(myUid).setValue(total).await()
        }

        suspend fun groupTabUnreadCountOnce(groupName: String): Int {
            val total = (totalMessagesRef(groupName).get().await().getValue(Long::class.java) ?: 0L).toInt()
            val read = readGroupMessagesRef(myUid).get().await().getValue(Int::class.java) ?: 0
            return (total - read).coerceAtLeast(0)
        }

        fun observeGroupTabUnreadCount(groupName: String): Flow<Int> = callbackFlow {
            val totalRef = totalMessagesRef(groupName)
            val readRef = readGroupMessagesRef(myUid)

            var total = 0
            var read = 0

            fun emit() {
                trySend((total - read).coerceAtLeast(0))
            }

            val totalListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    total = ((snapshot.getValue(Long::class.java) ?: 0L).toInt())
                    emit()
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            val readListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    read = snapshot.getValue(Int::class.java) ?: 0
                    emit()
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            totalRef.addValueEventListener(totalListener)
            readRef.addValueEventListener(readListener)

            awaitClose {
                totalRef.removeEventListener(totalListener)
                readRef.removeEventListener(readListener)
            }
        }.flowOn(Dispatchers.IO)

        // =========================
        // Unread privados (group_dm) -> suma unreadCount de conversations
        // =========================

        fun observePrivateGroupChatsUnreadCount(): Flow<Int> = callbackFlow {
            val ref = groupPrivateConversationsRef(myUid)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalUnread = 0
                    for (child in snapshot.children) {
                        val unread = child.child("unreadCount").getValue(Int::class.java) ?: 0
                        totalUnread += unread
                    }
                    trySend(totalUnread)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }.flowOn(Dispatchers.IO)

        // =========================
        // Users del grupo
        // =========================

        fun observeGroupUsers(groupName: String): Flow<List<UserGroup>> = callbackFlow {
            val ref = groupUsersRef(groupName)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children.mapNotNull { it.getValue(UserGroup::class.java) }
                        .sortedBy { it.userName.lowercase() }
                    trySend(list)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }.flowOn(Dispatchers.IO)



        // =========================
        // Chat del grupo (mensajes)
        // =========================

        /**
         * Lista completa (simple). Útil para Compose si preferís render por lista.
         * Si querés NO traer historial, usá el overload con startAtKey y generá un cursor al entrar.
         */
        fun observeGroupMessages(groupName: String, limitLast: Int = 200): Flow<List<GroupChatItem>> =
            callbackFlow {
                val query: Query = groupChatRef(groupName).limitToLast(limitLast)

                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = snapshot.children.mapNotNull { child ->
                            val id = child.key ?: return@mapNotNull null
                            val msg = child.getValue(ChatGroup::class.java) ?: return@mapNotNull null
                            GroupChatItem(id, msg)
                        }
                        trySend(list)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }

                query.addValueEventListener(listener)
                awaitClose { query.removeEventListener(listener) }
            }.flowOn(Dispatchers.IO)

        /**
         * Versión “sin historial”: startAtKey.
         * - Cuando el user entra al grupo: val cursor = newGroupMessageKey(groupName)
         * - Luego escuchás con startAtKey = cursor
         */
        fun observeGroupMessages(
            groupName: String,
            startAtKey: String,
            limitLast: Int = 200
        ): Flow<List<GroupChatItem>> = callbackFlow {
            val query: Query = groupChatRef(groupName)
                .orderByKey()
                .startAt(startAtKey)
                .limitToLast(limitLast)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children.mapNotNull { child ->
                        val id = child.key ?: return@mapNotNull null
                        val msg = child.getValue(ChatGroup::class.java) ?: return@mapNotNull null
                        GroupChatItem(id, msg)
                    }
                    trySend(list)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            query.addValueEventListener(listener)
            awaitClose { query.removeEventListener(listener) }
        }.flowOn(Dispatchers.IO)

        /**
         * Eventos incrementales (Added/Changed/Removed) para listas grandes o updates puntuales.
         */
        fun observeGroupMessageEvents(groupName: String): Flow<GroupChatChildEvent> = callbackFlow {
            val ref = groupChatRef(groupName)

            val listener = object : com.google.firebase.database.ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val id = snapshot.key ?: return
                    val msg = snapshot.getValue(ChatGroup::class.java) ?: return
                    trySend(GroupChatChildEvent.Added(GroupChatItem(id, msg)))
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val id = snapshot.key ?: return
                    val msg = snapshot.getValue(ChatGroup::class.java) ?: return
                    trySend(GroupChatChildEvent.Changed(GroupChatItem(id, msg)))
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    trySend(GroupChatChildEvent.Removed(id))
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            ref.addChildEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }.flowOn(Dispatchers.IO)

        fun newGroupMessageKey(groupName: String): String =
            groupChatRef(groupName).push().key ?: System.currentTimeMillis().toString()

        private suspend fun pushGroupMessage(groupName: String, chatMsg: Map<String, Any>) {
            groupChatRef(groupName).push().setValue(chatMsg).await()
            incrementTotalMessages(groupName)
        }

        suspend fun sendGroupPhotoMessage(
            groupName: String,
            senderName: String,
            userType: Int,
            senderUid: String = myUid,
            photoUri: Uri
        ) {
            val url = uploadGroupPhoto(groupName, photoUri)

            val chatMap = mutableMapOf(
                ChatGroupKeys.CONTENT to url,
                ChatGroupKeys.TIMESTAMP to ServerValue.TIMESTAMP,
                ChatGroupKeys.USER_NAME to senderName,
                ChatGroupKeys.SENDER_UID to senderUid,
                ChatGroupKeys.CHAT_TYPE to MSG_PHOTO,
                ChatGroupKeys.USER_TYPE to userType,
            )

            pushGroupMessage(groupName, chatMap)
        }

        private suspend fun uploadGroupPhoto(groupName: String, photoUri: Uri): String {
            val fileName = "${myUid}_${System.currentTimeMillis()}.jpg"
            val ref = firebaseRefsContainer.firebaseStorage.reference
                .child("photos")
                .child("groups")
                .child(groupName)
                .child(fileName)

            ref.putFile(photoUri).await()
            return ref.downloadUrl.await().toString()
        }

        // =========================
        // Join / Leave / Membership
        // =========================

        suspend fun sendGroupMessage(
            groupName: String,
            userName: String,
            userType: Int,
            chatType: Int,
            content: String
        ) {
            val chatMap = mutableMapOf(
                ChatGroupKeys.CONTENT to content,
                ChatGroupKeys.TIMESTAMP to ServerValue.TIMESTAMP,
                ChatGroupKeys.USER_NAME to userName,
                ChatGroupKeys.SENDER_UID to myUid,
                ChatGroupKeys.CHAT_TYPE to chatType,
                ChatGroupKeys.USER_TYPE to userType,
            )

            pushGroupMessage(groupName, chatMap)
        }

        suspend fun saveUserInGroup(
            groupName: String,
            userName: String,
            userType: Int
        ) {
            val groupUserRef = groupUsersRef(groupName).child(myUid)

            val userMap = mutableMapOf(
                GroupUserKeys.USER_ID to myUid,
                GroupUserKeys.USER_NAME to userName,
                GroupUserKeys.USER_TYPE to userType,
                GroupUserKeys.JOINED_AT_MS to ServerValue.TIMESTAMP
            )

            groupUserRef.setValue(userMap).await()
        }

        suspend fun removeUserFromGroup(groupName: String, userId: String = myUid) {
            groupUsersRef(groupName)
                .child(userId)
                .removeValue()
                .await()
        }

        // =========================
        // Groups list / create (mantenemos tu Data actual)
        // =========================

        suspend fun isGroupNameInUse(name: String): Boolean {
            val snapshot = firebaseRefsContainer.refGroupMeta.child(name).get().await()
            return snapshot.exists()
        }

        suspend fun createGroup(
            groupName: String,
            groupDescription: String,
            groupType: Int
        ) {
            val group = Groups(
                name = groupName,
                description = groupDescription,
                creatorUid = myUid,
                type = groupType,
                users = 0,
                createdAt = dateTime()
            )

            firebaseRefsContainer.refGroupMeta
                .child(groupName)
                .setValue(group)
                .await()

            // inicializamos meta totalMessages
            ensureGroupMeta(groupName)
        }

        suspend fun getGroups(): List<Groups> {
            val snapshot = firebaseRefsContainer.refGroupMeta.get().await()
            if (!snapshot.exists()) return emptyList()

            val groupsList = mutableListOf<Groups>()

            for (groupSnap in snapshot.children) {
                val group = groupSnap.getValue(Groups::class.java) ?: continue
                val name = groupSnap.child(GroupMetaKeys.NAME).getValue(String::class.java) ?: group.name

                val usersSnap = groupUsersRef(name).get().await()
                group.users = usersSnap.childrenCount.toInt()

                groupsList.add(group)
            }

            return groupsList
        }

        // =========================
        // Cleanup al salir del grupo
        // =========================

        /**
         * Borra la lista local de conversaciones privadas (group_dm) del user en Users/Data.
         */
        suspend fun removeMyGroupChatList(userId: String = myUid) {
            firebaseRefsContainer.refData
                .child(userId)
                .child(NODE_GROUP_DM)
                .removeValue()
                .await()
        }

        /**
         * Borra chats privados dentro de /Chats/group_dm cuyo key contenga userId.
         * (Como venías haciendo; es “global”, no por groupName)
         */
        suspend fun removeMyPrivateGroupChats(userId: String = myUid) {
            val snapshot = firebaseRefsContainer.refChatsGroupDm.get().await()
            for (child in snapshot.children) {
                val key = child.key ?: continue
                if (key.contains(userId)) {
                    child.ref.removeValue().await()
                }
            }
        }

        // =========================
        // Helpers
        // =========================

        private suspend fun DatabaseReference.runTransactionAwait(
            computeNewValue: (Any?) -> Any
        ) {
            suspendCancellableCoroutine<Unit> { cont ->
                runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val newValue = computeNewValue(currentData.value)
                        currentData.value = newValue
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error != null) {
                            cont.resumeWithException(error.toException())
                        } else {
                            cont.resume(Unit)
                        }
                    }
                })
            }
        }


}
