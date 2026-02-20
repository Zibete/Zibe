package com.zibete.proyecto1.data

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.core.constants.Constants.ChatGroupKeys
import com.zibete.proyecto1.core.constants.Constants.ChatListKeys
import com.zibete.proyecto1.core.constants.Constants.ConversationKeys
import com.zibete.proyecto1.core.constants.Constants.EXTENSION_IMAGE
import com.zibete.proyecto1.core.constants.Constants.GroupMetaKeys
import com.zibete.proyecto1.core.constants.Constants.GroupUserKeys
import com.zibete.proyecto1.core.constants.Constants.KEY_SEPARATOR
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.core.constants.Constants.NODE_CHAT_LIST
import com.zibete.proyecto1.core.constants.Constants.NODE_CLIENT_DATA
import com.zibete.proyecto1.core.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.core.constants.Constants.PATH_PHOTOS
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.core.utils.TimeUtils.now
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.di.qualifiers.ApplicationScope
import com.zibete.proyecto1.model.ChatGroup
import com.zibete.proyecto1.model.ChatGroupItem
import com.zibete.proyecto1.model.GroupChatChildEvent
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.UserGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface GroupRepositoryProvider {

    suspend fun findUserGroup(userId: String, groupName: String): UserGroup?

    suspend fun isGroupMatch(otherUid: String, groupName: String): ZibeResult<Boolean>
}

@ApplicationScope
@Singleton
class GroupRepository constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val authSessionProvider: AuthSessionProvider,
) : GroupRepositoryProvider {
    val firebaseUser: FirebaseUser
        get() = checkNotNull(authSessionProvider.currentUser) {
            USER_PROVIDER_ERR_EXCEPTION
        }

    val myUid: String
        get() = firebaseUser.uid

    // EVENTS
    fun observeGroupChatEvents(groupName: String): Flow<GroupChatChildEvent> = callbackFlow {
        val groupChatRef = groupChatRef(groupName)

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val msg = snapshot.getValue(ChatGroup::class.java) ?: ChatGroup()
                trySend(GroupChatChildEvent.Added(ChatGroupItem(id, msg)))
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val msg = snapshot.getValue(ChatGroup::class.java) ?: ChatGroup()
                trySend(GroupChatChildEvent.Changed(ChatGroupItem(id, msg)))
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.key ?: return
                // si ya no está el payload, mandamos empty (pero con id válido)
                trySend(GroupChatChildEvent.Removed(ChatGroupItem(id, ChatGroup())))
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        groupChatRef.addChildEventListener(listener)
        awaitClose { groupChatRef.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    // BADGES / COUNTS
    fun unreadGroupBadgeCount(groupName: String): Flow<Int> =
        combine(
            observeUnreadGroupChat(groupName),
            observeUnreadPrivateMessages()
        ) { unreadGroup, unreadPrivateChats ->
            (unreadGroup + unreadPrivateChats).coerceAtLeast(0)
        }

    // ========== CORE FLOWS ==========

    /** unread del grupo = totalGrupo - readCount */
    fun observeUnreadGroupChat(groupName: String): Flow<Int> =
        combine(
            observeGroupTotalMessages(groupName),
            observeReadGroupMessages()
        ) { total, read ->
            (total - read).coerceAtLeast(0)
        }

    /** suma unreadCount de chats privados dentro del nodo /Users/Data/<uid>/group_dm */
    fun observeUnreadPrivateMessages(): Flow<Int> = callbackFlow {
        val ref = groupPrivateConversationsRef()

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var total = 0
                for (child in snapshot.children) {
                    total += child.child(ConversationKeys.UNREAD_COUNT).getValue(Int::class.java)
                        ?: 0
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

    private fun observeReadGroupMessages(): Flow<Int> = callbackFlow {
        val ref = readGroupMessagesRef()

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

    fun observeIsUserInGroup(groupName: String, userId: String): Flow<Boolean> = callbackFlow {

        val ref = groupUsersRef(groupName)
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

    override suspend fun findUserGroup(userId: String, groupName: String): UserGroup? =
        groupUsersRef(groupName)
            .child(userId)
            .get()
            .await()
            .takeIf { it.exists() }
            ?.getValue(UserGroup::class.java)

    override suspend fun isGroupMatch(otherUid: String, groupName: String): ZibeResult<Boolean> =
        zibeCatching {
            val userGroup = findUserGroup(otherUid, groupName)
            userGroup != null && userGroup.type == PUBLIC_USER
        }

    suspend fun getGroup(groupName: String): Groups? =
        groupMetaRef(groupName)
            .get()
            .await()
            .takeIf { it.exists() }
            ?.getValue(Groups::class.java)

    suspend fun isNickInUse(groupName: String, nick: String): Boolean {
        val snapshot = groupUsersRef(groupName)
            .get()
            .await()

        return snapshot.children.any { child ->
            val name = child.child(GroupUserKeys.USER_NAME).getValue(String::class.java)
            name == nick
        }
    }

    private fun groupMetaRef(groupName: String): DatabaseReference =
        firebaseRefsContainer.refGroupMeta.child(groupName)

    private fun groupUsersRef(groupName: String): DatabaseReference =
        firebaseRefsContainer.refGroupUsers.child(groupName)

    private fun groupChatRef(groupName: String): DatabaseReference =
        firebaseRefsContainer.refGroupChat.child(groupName)

    private fun totalMessagesRef(groupName: String): DatabaseReference =
        groupMetaRef(groupName)
            .child(GroupMetaKeys.TOTAL_MESSAGES)

    private fun groupPrivateConversationsRef(uid: String = myUid): DatabaseReference =
        firebaseRefsContainer.refData.child(uid).child(NODE_GROUP_DM)

    // Meta: totalMessages
    suspend fun ensureGroupMeta(groupName: String) {
        val snap = totalMessagesRef(groupName).get().await()
        if (!snap.exists()) {
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

    private fun readGroupMessagesRef(uid: String = myUid): DatabaseReference =
        chatListRef(uid).child(ChatListKeys.READ_GROUP_MESSAGES)

    private fun chatListRef(uid: String = myUid) =
        firebaseRefsContainer.refData.child(uid)
            .child(NODE_CLIENT_DATA)
            .child(NODE_CHAT_LIST)

    // Read / Unread
    suspend fun markGroupAsRead(groupName: String) {
        val total = (totalMessagesRef(groupName)
            .get().await().getValue(Long::class.java) ?: 0L).toInt()
        readGroupMessagesRef().setValue(total).await()
    }


    /**
     * Lista completa (simple). Útil para Compose si preferís render por lista.
     * Si querés NO traer historial, usá el overload con startAtKey y generá un cursor al entrar.
     */
    fun observeGroupMessages(groupName: String, limitLast: Int = 200): Flow<List<ChatGroupItem>> =
        callbackFlow {
            val query: Query = groupChatRef(groupName).limitToLast(limitLast)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children.mapNotNull { child ->
                        val id = child.key ?: return@mapNotNull null
                        val msg = child.getValue(ChatGroup::class.java) ?: return@mapNotNull null
                        ChatGroupItem(id, msg)
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
    ): Flow<List<ChatGroupItem>> = callbackFlow {
        val query: Query = groupChatRef(groupName)
            .orderByKey()
            .startAt(startAtKey)
            .limitToLast(limitLast)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    val msg = child.getValue(ChatGroup::class.java) ?: return@mapNotNull null
                    ChatGroupItem(id, msg)
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
        val fileName = "$myUid$KEY_SEPARATOR${System.currentTimeMillis()}$EXTENSION_IMAGE"
        val ref = firebaseRefsContainer.storageGroupChatRef
            .child(groupName)
            .child(PATH_PHOTOS)
            .child(fileName)

        ref.putFile(photoUri).await()
        return ref.downloadUrl.await().toString()
    }


    // Join / Leave / Membership
    suspend fun sendGroupMessage(
        groupName: String,
        userName: String,
        userType: Int,
        chatType: Int,
        content: String,
        senderName: String = myUid
    ): ZibeResult<Unit> = zibeCatching {
        val chatMap = mutableMapOf(
            ChatGroupKeys.CONTENT to content,
            ChatGroupKeys.TIMESTAMP to ServerValue.TIMESTAMP,
            ChatGroupKeys.USER_NAME to userName,
            ChatGroupKeys.SENDER_UID to senderName,
            ChatGroupKeys.CHAT_TYPE to chatType,
            ChatGroupKeys.USER_TYPE to userType,
        )
        pushGroupMessage(groupName, chatMap)
    }

    suspend fun saveUserInGroup(
        groupName: String,
        userName: String,
        userType: Int,
        userId: String = myUid
    ) {
        val groupUserRef = groupUsersRef(groupName)
            .child(userId)

        val userMap = mutableMapOf(
            GroupUserKeys.USER_ID to userId,
            GroupUserKeys.USER_NAME to userName,
            GroupUserKeys.USER_TYPE to userType,
            GroupUserKeys.JOINED_AT_MS to ServerValue.TIMESTAMP
        )

        groupUserRef.setValue(userMap).await()
    }

    suspend fun removeUserFromGroup(
        groupName: String,
        userId: String = myUid
    ): ZibeResult<Unit> = zibeCatching {
        groupUsersRef(groupName)
            .child(userId)
            .removeValue()
            .await()
    }


    // Groups list / create (mantenemos tu Data actual)
    suspend fun isGroupNameInUse(groupName: String): Boolean {
        return groupMetaRef(groupName).get().await().exists()
    }

    suspend fun createGroup(
        groupName: String,
        groupDescription: String,
        groupType: Int,
        creatorUid: String = myUid
    ) {
        val group = Groups(
            name = groupName,
            description = groupDescription,
            creatorUid = creatorUid,
            type = groupType,
            users = 0,
            createdAt = now()
        )

        groupMetaRef(groupName)
            .setValue(group)
            .await()

        ensureGroupMeta(groupName)
    }

    suspend fun getAllGroups(): List<Groups> {
        val snapshot = firebaseRefsContainer.refGroupMeta.get().await()
        if (!snapshot.exists()) return emptyList()

        val groupsList = mutableListOf<Groups>()

        for (groupSnap in snapshot.children) {
            val group = groupSnap.getValue(Groups::class.java) ?: continue
            val usersSnap = groupUsersRef(group.name).get().await()
            group.users = usersSnap.childrenCount.toInt()
            groupsList.add(group)
        }

        return groupsList
    }


    // Cleanup al salir del grupo


    /**
     * Borra la lista local de conversaciones privadas (group_dm) del user en Users/Data.
     */
    suspend fun removeMyGroupChatList(): ZibeResult<Unit> = zibeCatching {
        groupPrivateConversationsRef().removeValue().await()
    }

    /**
     * Borra chats privados dentro de /Chats/group_dm cuyo key contenga userId.
     * (Como venías haciendo; es “global”, no por groupName)
     */
    suspend fun removeMyPrivateGroupChats(userId: String = myUid): ZibeResult<Unit> = zibeCatching {
        val snapshot = firebaseRefsContainer.refChatsGroupDm.get().await()
        for (child in snapshot.children) {
            val key = child.key ?: continue
            if (key.contains(userId)) {
                child.ref.removeValue().await()
            }
        }
    }

    // Helpers
    private suspend fun DatabaseReference.runTransactionAwait(
        computeNewValue: (Any?) -> Any
    ) {
        suspendCancellableCoroutine { cont ->
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
