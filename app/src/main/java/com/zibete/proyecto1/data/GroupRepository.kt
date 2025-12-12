package com.zibete.proyecto1.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_MID
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT
import com.zibete.proyecto1.ui.constants.MSG_USER_JOINED
import com.zibete.proyecto1.utils.Utils.dateTime
import com.zibete.proyecto1.utils.Utils.now
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository
) {

    private val myUid = userRepository.user.uid


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

    fun observeUnreadGroup(): Flow<Int> = callbackFlow {
        val query = firebaseRefsContainer.refGroupChat.parent
        if (query == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(0)
                    return
                }

                launch {
                    val totalMsgCount = getTotalMsgCount(snapshot)
                    val seen = getSeenGroupCount(myUid)
                    val unread = getUnreadGroupCount(myUid)

                    val totalBadge = (totalMsgCount - seen) + unread
                    trySend(if (totalBadge > 0) totalBadge else 0)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }


    private fun getTotalMsgCount(snapshot: DataSnapshot): Int =
        snapshot.children.sumOf { it.childrenCount }.toInt()

    private suspend fun getSeenGroupCount(uid: String): Int {
        val ds = firebaseRefsContainer.refDatos
            .child(uid)
            .child(NODE_CHATLIST)
            .child("msgReadGroup")
            .get()
            .await()

        return ds.getValue(Int::class.java) ?: 0
    }

    private suspend fun getUnreadGroupCount(uid: String): Int {
        val ds = firebaseRefsContainer.refDatos
            .child(uid)
            .child(NODE_GROUP_CHAT)
            .orderByChild("noVisto")
            .startAt(1.0)
            .get()
            .await()

        var count = 0
        if (ds.exists()) {
            for (child in ds.children) {
                count += child.child("noVisto").getValue(Int::class.java) ?: 0
            }
        }
        return count
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




}
