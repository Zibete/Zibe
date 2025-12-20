// GroupRepository.kt (REPLACE FULL)  - Meta/totalMessages + readGroupMessages + unread privados

package com.zibete.proyecto1.data

import com.google.firebase.database.*
import com.google.firebase.database.Transaction.Handler
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.di.qualifiers.ApplicationScope
import com.zibete.proyecto1.model.ChatGroup
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.constants.Constants.ChatListKeys
import com.zibete.proyecto1.ui.constants.Constants.ConversationKeys
import com.zibete.proyecto1.ui.constants.Constants.GroupMetaKeys
import com.zibete.proyecto1.ui.constants.Constants.GroupUserKeys
import com.zibete.proyecto1.ui.constants.Constants.MSG_INFO
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_MID
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATS_ROOT
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.ui.constants.Constants.PATH_PHOTOS
import com.zibete.proyecto1.ui.constants.MSG_USER_JOINED
import com.zibete.proyecto1.ui.constants.MSG_USER_LEAVED
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    private fun groupChatRef(groupName: String) =
        firebaseRefsContainer.refGroupChat
            .child(groupName)

    private fun readGroupMessagesRef() =
        userRepository.chatListRef()
            .child(ChatListKeys.READ_GROUP_MESSAGES)

    fun buildGroupRefs(groupName: String): GroupRefs {
        val refGroupChat = groupChatRef(groupName)

        val refGroupPhotos =
            firebaseRefsContainer.firebaseStorage.reference
                .child(NODE_CHATS_ROOT)
                .child(NODE_GROUP_DM)
                .child(groupName)
                .child(PATH_PHOTOS)

        return GroupRefs(
            refGroupChat = refGroupChat,
            refGroupPhotos = refGroupPhotos
        )
    }

    private fun groupMetaRef(groupName: String): DatabaseReference =
        firebaseRefsContainer.refGroupMeta.child(groupName)

    private fun totalMessagesRef(groupName: String): DatabaseReference =
        groupMetaRef(groupName).child(GroupMetaKeys.TOTAL_MESSAGES)

    suspend fun getTotalMessagesOnce(groupName: String): Int {
        return totalMessagesRef(groupName).get().await().getValue(Int::class.java) ?: 0
    }

    fun observeTotalMessages(groupName: String): Flow<Int> = callbackFlow {
        val totalMessagesRef = totalMessagesRef(groupName)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Int::class.java) ?: 0)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        totalMessagesRef.addValueEventListener(listener)
        awaitClose { totalMessagesRef.removeEventListener(listener) }
    }

    private suspend fun DatabaseReference.incrementInt(delta: Int = 1): Int =
        suspendCancellableCoroutine { cont ->
            runTransaction(object : Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val curAny = currentData.value

                    val cur = (curAny as? Number)?.toLong() ?: 0L

                    val next = cur + delta
                    currentData.value = next
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    snapshot: DataSnapshot?
                ) {
                    if (error != null) {
                        cont.resumeWithException(error.toException())
                        return
                    }
                    val value = snapshot?.getValue(Long::class.java)?.toInt()
                        ?: snapshot?.getValue(Int::class.java)
                        ?: 0
                    cont.resume(value)
                }
            })
        }

    suspend fun incrementTotalMessages(groupName: String): Int =
        totalMessagesRef(groupName).incrementInt(1)

    suspend fun ensureGroupMeta(groupName: String) {
        val snap = groupMetaRef(groupName).get().await()
        if (!snap.exists()) {
            groupMetaRef(groupName).setValue(
                mapOf(GroupMetaKeys.TOTAL_MESSAGES to 0)
            ).await()
        } else {
            val totalSnap = snap.child(GroupMetaKeys.TOTAL_MESSAGES)
            if (!totalSnap.exists()) {
                totalMessagesRef(groupName).setValue(0).await()
            }
        }
    }

    suspend fun pushGroupMessage(groupName: String, chatMsg: ChatGroup) {
        ensureGroupMeta(groupName)
        groupChatRef(groupName)
            .push()
            .setValue(chatMsg)
            .await()

        incrementTotalMessages(groupName)
    }

    // ---------- caches ----------
    private val cacheGroupTotalByName = mutableMapOf<String, StateFlow<Int>>()

    private val cacheGroupReadCount: StateFlow<Int> by lazy {
        observeGroupReadCount()
            .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)
    }

    private val cacheUnreadPrivateChatsInsideGroup: StateFlow<Int> by lazy {
        observeUnreadPrivateChatsInsideGroup()
            .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)
    }

    fun groupTabUnreadCount(groupName: String): StateFlow<Int> =
        observeGroupUnreadCount(groupName)
            .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)

    suspend fun groupTabUnreadCountOnce(groupName: String): Int {
        val total = getTotalMessagesOnce(groupName)
        val read = readGroupMessagesRef().get().await()
            .getValue(Int::class.java) ?: 0

        return (total - read).coerceAtLeast(0)
    }

    fun groupsBottomNavBadgeCount(groupName: String): StateFlow<Int> =
        combine(
            observeGroupUnreadCount(groupName),
            unreadPrivateChatsInsideGroup()
        ) { unreadGroup, unreadPrivate ->
            (unreadGroup + unreadPrivate).coerceAtLeast(0)
        }.stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun unreadPrivateChatsInsideGroup(): StateFlow<Int> = cacheUnreadPrivateChatsInsideGroup

    suspend fun setReadGroupMessages(readCount: Int) {
        readGroupMessagesRef()
            .setValue(readCount)
            .await()
    }

    suspend fun syncReadGroupMessagesToTotal(groupName: String) {
        val total = getTotalMessagesOnce(groupName)
        setReadGroupMessages(total)
    }

    private fun observeGroupUnreadCount(groupName: String): Flow<Int> =
        combine(
            groupTotalMsgCount(groupName),
            groupReadCount()
        ) { total, read ->
            (total - read).coerceAtLeast(0)
        }

    private fun groupTotalMsgCount(groupName: String): StateFlow<Int> =
        cacheGroupTotalByName.getOrPut(groupName) {
            observeTotalMessages(groupName)
                .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), 0)
        }

    private fun groupReadCount(): StateFlow<Int> = cacheGroupReadCount

    private fun observeUnreadPrivateChatsInsideGroup(): Flow<Int> = callbackFlow {
        val ref = userRepository.dataRef()
            .child(NODE_GROUP_DM)

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

    private fun observeGroupReadCount(): Flow<Int> = callbackFlow {
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

    fun groupUsersRef(groupName: String) =
        firebaseRefsContainer.refGroupUsers
            .child(groupName)
    fun observeGroupUserAvailability(groupName: String, userId: String): Flow<Boolean> = callbackFlow {
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

    suspend fun getUserGroup(userId: String, groupName: String): UserGroup? =
        groupUsersRef(groupName)
            .child(userId)
            .get()
            .await()
            .takeIf { it.exists() }
            ?.getValue(UserGroup::class.java)

    suspend fun isNickInUse(groupName: String, nick: String): Boolean {
        val snapshot = groupUsersRef(groupName).get().await()

        return snapshot.children.any { child ->
            val name = child.child(GroupUserKeys.USER_NAME).getValue(String::class.java)
            name == nick
        }
    }

    suspend fun sendJoinMessage(groupName: String, nick: String, type: Int) {
        val chatMsg = ChatGroup(
            MSG_USER_JOINED,
            now(),
            nick,
            myUid,
            MSG_INFO,
            type
        )
        pushGroupMessage(groupName, chatMsg)
    }

    suspend fun saveUserInGroup(groupName: String, nick: String, type: Int) {
        val userGroup = UserGroup(
            userId = myUid,
            userName = nick,
            type = type
        )

        groupUsersRef(groupName)
            .child(myUid)
            .setValue(userGroup)
            .await()
    }

    suspend fun isGroupNameInUse(groupName: String): Boolean {
        val snapshot = groupMetaRef(groupName).get().await()
        return snapshot.exists()
    }

    suspend fun createGroup(groupName: String, groupData: String, groupType: Int) {
        val group = Groups(
            name = groupName,
            description = groupData,
            myUid,
            groupType,
            users = 0,
            dateTime()
        )

        groupMetaRef(groupName)
            .setValue(group)
            .await()

        ensureGroupMeta(groupName)
    }

    suspend fun getGroups(): List<Groups> {
        val snapshot = firebaseRefsContainer.refGroupMeta.get().await()
        if (!snapshot.exists()) return emptyList()

        val groupsList = mutableListOf<Groups>()

        for (groupSnap in snapshot.children) {
            val group = groupSnap.getValue(Groups::class.java) ?: continue
            val groupName = groupSnap.child(GroupMetaKeys.NAME).getValue(String::class.java) ?: group.name

            val usersSnap = groupUsersRef(groupName)
                .get()
                .await()

            group.users = usersSnap.childrenCount.toInt()
            groupsList.add(group)
        }

        return groupsList
    }

    suspend fun removeMyGroupChatList() {
        userRepository.dataRef()
            .child(NODE_GROUP_DM)
            .removeValue()
            .await()
    }

    suspend fun removeMyPrivateGroupChats(userId: String = myUid) {
        val snapshot = firebaseRefsContainer.refChatsGroupDm.get().await()
        for (child in snapshot.children) {
            val key = child.key ?: continue
            if (key.contains(userId)) {
                child.ref.removeValue().await()
            }
        }
    }

    suspend fun sendLeaveGroupMessage(groupName: String, userName: String, userType: Int, userId: String = myUid) {
        val chatMsg = ChatGroup(
            MSG_USER_LEAVED,
            now(),
            userName,
            userId,
            MSG_TYPE_MID,
            userType
        )
        pushGroupMessage(groupName, chatMsg)
    }

    suspend fun removeUserFromGroup(groupName: String, userId: String = myUid) {
        groupUsersRef(groupName)
            .child(userId)
            .removeValue()
            .await()
    }
}
