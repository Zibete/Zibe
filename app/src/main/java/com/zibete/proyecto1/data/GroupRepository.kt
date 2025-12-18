package com.zibete.proyecto1.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.di.qualifiers.ApplicationScope
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_MID
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATS
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT
import com.zibete.proyecto1.ui.constants.Constants.PATH_PHOTOS
import com.zibete.proyecto1.ui.constants.MSG_USER_JOINED
import com.zibete.proyecto1.utils.Utils.dateTime
import com.zibete.proyecto1.utils.Utils.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class GroupRefs(
    val refGroupChat: DatabaseReference,
    val refGroupPhotos: StorageReference
)

class GroupRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository,
    @ApplicationScope private val appScope: CoroutineScope
) {

    private val myUid: String get() = userRepository.myUid

    fun buildGroupRefs(
        groupName: String
    ): GroupRefs {

        val refGroupChat =
            firebaseRefsContainer.refGroupChat
                .child(groupName)

        val refGroupPhotos =
            firebaseRefsContainer.firebaseStorage.reference
                .child("$NODE_CHATS/$NODE_GROUP_CHAT/$groupName/")
                .child("$PATH_PHOTOS/")

        return GroupRefs(
            refGroupChat = refGroupChat,
            refGroupPhotos = refGroupPhotos
        )
    }
    // ---------- caches (1 listener por fuente) ----------
    private val cacheGroupTotalByName = mutableMapOf<String, StateFlow<Int>>()
    private val cacheGroupReadCount: StateFlow<Int> by lazy {
        observeGroupReadCount(myUid)
            .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)
    }
    private val cachePrivateUnreadCount: StateFlow<Int> by lazy {
        observePrivateUnreadCount(myUid)
            .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)
    }

    // ========== PUBLIC API ==========

    /** Badge Tab interno (NombreGrupo (X)) -> SOLO grupo */
    fun groupTabUnreadCount(groupName: String): StateFlow<Int> =
        observeGroupUnreadCount(groupName)
            .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Badge BottomNav "Grupos" -> grupo + privados (realtime exacto) */
    fun groupsBottomNavBadgeCount(groupName: String): StateFlow<Int> =
        combine(
            observeGroupUnreadCount(groupName),
            privateUnreadCount()
        ) { unreadGroup, unreadPrivate ->
            (unreadGroup + unreadPrivate).coerceAtLeast(0)
        }.stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Realtime no vistos privados (por si lo necesitás directo en otra pantalla) */
    fun privateUnreadCount(): StateFlow<Int> = cachePrivateUnreadCount

    /** Actualizar readCount (lo llama ChatGroupFragment cuando está visible) */
    suspend fun setReadGroupMessages(uid: String, readCount: Int) {
        firebaseRefsContainer.refData
            .child(uid)
            .child(NODE_CHATLIST)
            .child("msgReadGroup")
            .setValue(readCount)
            .await()
    }

    // ========== CORE FLOWS (reutilizables) ==========

    /** unread del grupo = totalGrupo - readCount */
    private fun observeGroupUnreadCount(groupName: String): Flow<Int> =
        combine(
            groupTotalMsgCount(groupName),
            groupReadCount()
        ) { total, read ->
            (total - read).coerceAtLeast(0)
        }

    /** total mensajes del grupo (realtime) */
    private fun groupTotalMsgCount(groupName: String): StateFlow<Int> =
        cacheGroupTotalByName.getOrPut(groupName) {
            observeGroupTotalMsgCount(groupName)
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)
        }

    /** readCount del grupo (realtime) */
    private fun groupReadCount(): StateFlow<Int> = cacheGroupReadCount

    /** suma noVisto de chats privados (realtime) */
    private fun observePrivateUnreadCount(uid: String): Flow<Int> = callbackFlow {
        val ref = firebaseRefsContainer.refData
            .child(uid)
            .child(NODE_GROUP_CHAT)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var total = 0
                for (child in snapshot.children) {
                    total += child.child("noVisto").getValue(Int::class.java) ?: 0
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
        val ref = firebaseRefsContainer.refData
            .child(uid)
            .child(NODE_CHATLIST)
            .child("msgReadGroup")

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

    private fun observeGroupTotalMsgCount(groupName: String): Flow<Int> = callbackFlow {
        val groupRefs = buildGroupRefs(groupName)
        val ref = groupRefs.refGroupChat // /GroupChat/{groupName}

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













    // User de grupo deja de estar disponible
    fun observeGroupUserAvailability(
        groupName: String,
        userId: String
    ): Flow<Boolean> = callbackFlow {

        val ref = firebaseRefsContainer.refGroupUsers
            .child(groupName)
            .child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(ds: DataSnapshot) {

                val isAvailable = ds.exists()

                trySend(isAvailable)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun getUserGroup(userId: String, groupName: String): UserGroup? =
        firebaseRefsContainer.refGroupUsers
            .child(groupName)
            .child(userId)
            .get()
            .await()
            .takeIf { it.exists() }
            ?.getValue(UserGroup::class.java)

    suspend fun isNickInUse(groupName: String, nick: String): Boolean {
        val snapshot = firebaseRefsContainer.refGroupUsers
            .child(groupName)
            .get()
            .await()

        return snapshot.children.any { child ->
            val name = child.child("user_name").getValue(String::class.java)
            name == nick
        }
    }

    suspend fun sendJoinMessage(
        groupName: String,
        nick: String,
        type: Int
    ) {
        val chatMsg = ChatsGroup(
            MSG_USER_JOINED,
            now(),
            nick,
            myUid,
            MSG_TYPE_MID,
            type
        )

        firebaseRefsContainer.refGroupChat
            .child(groupName)
            .push()
            .setValue(chatMsg)
            .await()
    }

    suspend fun saveUserInGroup(
        groupName: String,
        nick: String,
        type: Int
    ) {
        val userGroup = UserGroup(
            userId = myUid,
            userName = nick,
            type = type
        )

        firebaseRefsContainer.refGroupUsers
            .child(groupName)
            .child(myUid)
            .setValue(userGroup)
            .await()
    }

    suspend fun isGroupNameInUse(name: String): Boolean {
        val snapshot = firebaseRefsContainer.refGroupData
            .child(name)
            .get()
            .await()
        return snapshot.exists()
    }

    suspend fun createGroup(
        groupName: String,
        groupData: String,
        groupType: Int
    ) {

        val group = Groups(
            name = groupName,
            data = groupData,
            myUid,
            groupType,
            users = 0,
            dateTime()
        )

        firebaseRefsContainer.refGroupData
            .child(groupName)
            .setValue(group)
            .await()
    }

    suspend fun getGroups(): List<Groups> {
        val snapshot = firebaseRefsContainer.refGroupData
            .get()
            .await()

        if (!snapshot.exists()) return emptyList()

        val groupsList = mutableListOf<Groups>()

        for (groupSnap in snapshot.children) {
            val group = groupSnap.getValue(Groups::class.java) ?: continue

            // nombre del grupo
            val name = groupSnap.child("name").getValue(String::class.java) ?: group.name

            // contar usuarios del grupo
            val usersSnap = firebaseRefsContainer.refGroupUsers
                .child(name)
                .get()
                .await()

            group.users = usersSnap.childrenCount.toInt()
            groupsList.add(group)
        }

        return groupsList
    }

    // Eliminar chat de grupo al salir
    suspend fun removeMyGroupChatList(userId: String) {
        firebaseRefsContainer.refData
            .child(userId)
            .child(NODE_GROUP_CHAT)
            .removeValue()
            .await()
    }

    suspend fun removeMyPrivateGroupChats(userId: String) {
        val snapshot = firebaseRefsContainer.refChatMessageGroupsRoot
            .get()
            .await()

        for (child in snapshot.children) {
            val key = child.key ?: continue
            if (key.contains(userId)) {
                child.ref.removeValue().await()
            }
        }
    }

    suspend fun sendLeaveGroupMessage(
        groupName: String,
        userName: String,
        userType: Int,
        userId: String
    ) {
        val chatMsg = ChatsGroup(
            MSG_USER_JOINED,
            now(),
            userName,
            userId,
            MSG_TYPE_MID,
            userType
        )

        firebaseRefsContainer.refGroupChat
            .child(groupName)
            .push()
            .setValue(chatMsg)
            .await()
    }

    suspend fun removeUserFromGroup(
        groupName: String,
        userId: String
    ) {
        firebaseRefsContainer.refGroupUsers
            .child(groupName)
            .child(userId)
            .removeValue()
            .await()
    }

}
