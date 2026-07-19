package com.zibete.proyecto1.data

import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.core.constants.Constants.ChatGroupKeys
import com.zibete.proyecto1.core.constants.Constants.ChatListKeys
import com.zibete.proyecto1.core.constants.Constants.ConversationKeys
import com.zibete.proyecto1.core.constants.Constants.EXTENSION_IMAGE
import com.zibete.proyecto1.core.constants.Constants.GroupMetaKeys
import com.zibete.proyecto1.core.constants.Constants.GroupChatKeys
import com.zibete.proyecto1.core.constants.Constants.GroupUserKeys
import com.zibete.proyecto1.core.constants.Constants.KEY_SEPARATOR
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.core.constants.Constants.NODE_ACTIVE_VIEW
import com.zibete.proyecto1.core.constants.Constants.NODE_CHAT_LIST
import com.zibete.proyecto1.core.constants.Constants.NODE_CLIENT_DATA
import com.zibete.proyecto1.core.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_ROOM
import com.zibete.proyecto1.core.constants.Constants.NODE_ROOMS
import com.zibete.proyecto1.core.constants.Constants.PATH_PHOTOS
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.core.constants.Constants.ActiveThreadKeys
import com.zibete.proyecto1.core.constants.Constants.ActiveViewKeys
import com.zibete.proyecto1.core.constants.Constants.RoomReadKeys
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.AuthUser
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.di.qualifiers.ApplicationScope
import com.zibete.proyecto1.domain.rooms.CreateRoomCommand
import com.zibete.proyecto1.domain.rooms.JoinRoomCommand
import com.zibete.proyecto1.domain.rooms.LeaveRoomCommand
import com.zibete.proyecto1.domain.rooms.RoomCreationPolicy
import com.zibete.proyecto1.domain.rooms.RoomFailureReason
import com.zibete.proyecto1.domain.rooms.RoomMembershipNotFoundException
import com.zibete.proyecto1.domain.rooms.RoomOperationException
import com.zibete.proyecto1.domain.rooms.RoomOperationResult
import com.zibete.proyecto1.domain.rooms.RoomValidationException
import com.zibete.proyecto1.domain.rooms.RoomValidator
import com.zibete.proyecto1.domain.rooms.SwitchRoomCommand
import com.zibete.proyecto1.model.ChatGroup
import com.zibete.proyecto1.model.ChatGroupItem
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.GroupChatChildEvent
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.RoomIdentity
import com.zibete.proyecto1.model.RoomIdentityType
import com.zibete.proyecto1.model.RoomSession
import com.zibete.proyecto1.model.UserGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton
import java.util.Locale
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
@ApplicationScope
@Singleton
class GroupRepository constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val authSessionProvider: AuthSessionProvider,
) : GroupRepositoryProvider {
    val firebaseUser: AuthUser
        get() = checkNotNull(authSessionProvider.currentUser) {
            USER_PROVIDER_ERR_EXCEPTION
        }

    override val myUid: String
        get() = firebaseUser.uid

    override suspend fun loadRooms(): ZibeResult<List<Groups>> = zibeCatching {
        val metadata = firebaseRefsContainer.refGroupMeta.get().await()
        if (!metadata.exists()) return@zibeCatching emptyList()

        val readStates = roomsReadRootRef().get().await()
        metadata.children.mapNotNull { roomSnapshot ->
            val roomKey = roomSnapshot.key ?: return@mapNotNull null
            val room = runCatching { roomSnapshot.getValue(Groups::class.java) }
                .getOrNull()
                ?.withLegacyFallback(roomKey)
                ?: return@mapNotNull null
            room.apply {
                users = if (isLegacyRoom(roomSnapshot, roomKey)) {
                    groupUsersRef(roomKey).get().await().childrenCount.toInt()
                } else {
                    roomSnapshot.child(GroupMetaKeys.USERS).numberAsInt()
                }
                unreadCount = readStates.child(roomKey)
                    .child(RoomReadKeys.UNREAD_COUNT)
                    .numberAsInt()
                lastActivityAt = roomSnapshot.child(GroupMetaKeys.LAST_MESSAGE_AT)
                    .numberAsLong()
            }
        }.sortedWith(
            compareByDescending<Groups> { it.lastActivityAt }
                .thenBy { it.resolvedDisplayName().lowercase(Locale.ROOT) }
        )
    }

    override fun observeTotalRoomUnread(): Flow<Int> = callbackFlow {
        val ref = roomsReadRootRef()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    snapshot.children.sumOf {
                        it.child(RoomReadKeys.UNREAD_COUNT).numberAsInt()
                    }.coerceAtLeast(0)
                )
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.distinctUntilChanged()

    override fun observeRoomPrivateConversations(roomKey: String): Flow<List<Conversation>> =
        callbackFlow {
            val ref = groupPrivateConversationsRef()
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val conversations = snapshot.children
                        .mapNotNull { it.getValue(Conversation::class.java) }
                        .filter { conversation ->
                            conversation.roomKey.isBlank() || conversation.roomKey == roomKey
                        }
                        .filter { it.isVisibleFor(NODE_GROUP_DM) }
                        .sorted()
                    trySend(conversations)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }.flowOn(Dispatchers.IO)

    override suspend fun resolveRoomSession(roomKey: String): ZibeResult<RoomSession> =
        zibeCatching {
            val member = findUserGroup(myUid, roomKey)
                ?: throw IllegalStateException("Authenticated user is not a room member")
            val room = getGroup(roomKey)
                ?: throw IllegalStateException("Room does not exist")
            RoomSession(
                roomKey = roomKey,
                displayName = room.resolvedDisplayName(roomKey),
                userName = member.resolvedDisplayName(),
                userType = member.type
            )
        }

    override suspend fun isRoomAliasInUse(
        roomKey: String,
        normalizedAlias: String
    ): ZibeResult<Boolean> = zibeCatching {
        firebaseRefsContainer.refGroupAliases
            .child(roomKey)
            .child(normalizedAlias)
            .get()
            .await()
            .exists()
    }

    override suspend fun markRoomAsRead(
        roomKey: String,
        lastReadAt: Long
    ): ZibeResult<Unit> = zibeCatching {
        val stateRef = roomReadRef(roomKey)
        val latest = groupMetaRef(roomKey).get().await()
        val latestMessageAt = latest.child(GroupMetaKeys.LAST_MESSAGE_AT).numberAsLong()
        val latestMessageId = latest.child(GroupMetaKeys.LAST_MESSAGE_ID)
            .getValue(String::class.java)
            .orEmpty()
        if (latestMessageAt > lastReadAt) return@zibeCatching

        if (!stateRef.get().await().exists()) {
            readGroupMessagesRef().setValue(
                latest.child(GroupMetaKeys.TOTAL_MESSAGES).numberAsInt()
            ).await()
            return@zibeCatching
        }

        stateRef.runTransactionAwaitNullable { current ->
            val state = (current as? Map<*, *>)?.toMutableMap()
                ?: return@runTransactionAwaitNullable current
            val currentReadAt = (state[RoomReadKeys.LAST_READ_AT] as? Number)?.toLong() ?: 0L
            val unreadCount = (state[RoomReadKeys.UNREAD_COUNT] as? Number)?.toInt() ?: 0
            val lastUnreadMessageId = state[RoomReadKeys.LAST_UNREAD_MESSAGE_ID]
                ?.toString()
                .orEmpty()
            if (
                unreadCount > 0 &&
                lastUnreadMessageId.isNotBlank() &&
                lastUnreadMessageId != latestMessageId
            ) {
                return@runTransactionAwaitNullable current
            }
            state[RoomReadKeys.UNREAD_COUNT] = 0
            state[RoomReadKeys.LAST_READ_AT] = maxOf(currentReadAt, lastReadAt)
            state[RoomReadKeys.LAST_READ_MESSAGE_ID] = latestMessageId
            state
        }
    }

    override suspend fun setActiveRoom(roomKey: String): ZibeResult<Unit> = zibeCatching {
        activeThreadRef().setValue(
            mapOf(
                ActiveThreadKeys.NODE_TYPE to NODE_ROOM,
                ActiveThreadKeys.ROOM_KEY to roomKey,
                ActiveThreadKeys.UPDATED_AT to ServerValue.TIMESTAMP
            )
        ).await()
    }

    override suspend fun clearActiveRoom(roomKey: String): ZibeResult<Unit> = zibeCatching {
        activeThreadRef().runTransactionAwaitNullable { current ->
            val value = current as? Map<*, *> ?: return@runTransactionAwaitNullable current
            if (
                value[ActiveThreadKeys.NODE_TYPE] == NODE_ROOM &&
                value[ActiveThreadKeys.ROOM_KEY] == roomKey
            ) null else current
        }
    }

    override suspend fun createRoom(
        command: CreateRoomCommand
    ): ZibeResult<RoomOperationResult> {
        val trace = RoomOperationTrace(
            operation = "create",
            identityType = command.identity.type
        )
        return executeRoomOperation(trace) {
            RoomCreationPolicy.validateCreator(command.identity)?.let { issue ->
                throw RoomValidationException(listOf(issue))
            }
            trace.phase = "name_lookup"
            val roomName = command.roomName.trim()
            val nameKey = roomName.lowercase(Locale.ROOT)
            val existingRoomKey = firebaseRefsContainer.refGroupNames.child(nameKey)
                .get()
                .await()
                .getValue(String::class.java)
            if (!existingRoomKey.isNullOrBlank()) {
                val existingMember = findUserGroup(myUid, existingRoomKey)
                val existingRoom = getGroup(existingRoomKey)
                if (existingMember != null && existingRoom?.creatorUid == myUid) {
                    return@executeRoomOperation RoomOperationResult.Created(
                        RoomSession(
                            roomKey = existingRoomKey,
                            displayName = existingRoom.resolvedDisplayName(existingRoomKey),
                            userName = existingMember.resolvedDisplayName(),
                            userType = existingMember.type
                        )
                    )
                }
                return@executeRoomOperation RoomOperationResult.NameInUse(roomName)
            }

            val roomKey = checkNotNull(firebaseRefsContainer.refGroupMeta.push().key) {
                "Could not generate room id"
            }
            trace.roomKey = roomKey
            val messageId = newRequiredGroupMessageKey(roomKey)
            val identityName = command.identity.displayName.trim()
            val identityAliasKey = aliasKey(command.identity)
            val updates = mutableMapOf<String, Any?>()
            updates[groupNamePath(nameKey)] = roomKey
            updates[groupMetaPath(roomKey)] = roomMetaMap(
                roomKey = roomKey,
                roomName = roomName,
                description = command.description.trim(),
                creatorUid = myUid,
                users = 1,
                totalMessages = 1,
                lastMessageId = messageId,
                createdAt = ServerValue.TIMESTAMP,
                lastMessageAt = ServerValue.TIMESTAMP
            )
            updates[groupAliasPath(roomKey, identityAliasKey)] = myUid
            updates[groupMemberPath(roomKey, myUid)] = roomMemberMap(
                command.identity,
                identityAliasKey
            )
            updates[groupMessagePath(roomKey, messageId)] = roomMessageMap(
                roomId = roomKey,
                roomName = roomName,
                messageId = messageId,
                identity = command.identity,
                chatType = com.zibete.proyecto1.core.constants.Constants.MSG_INFO,
                content = command.eventContent
            )
            updates[roomReadPath(myUid, roomKey)] = roomReadStateMap(
                unreadCount = 0,
                lastReadAt = ServerValue.TIMESTAMP,
                lastReadMessageId = messageId,
                lastUnreadMessageId = ""
            )
            command.previousSession?.let { previous ->
                updates.putAll(
                    buildLeaveUpdates(
                        LeaveRoomCommand(
                            roomKey = previous.roomKey,
                            userName = previous.userName,
                            userType = previous.userType,
                            eventContent = command.leaveEventContent
                        )
                    )
                )
            }
            trace.phase = "atomic_create_fanout"
            rootRef().updateChildren(updates).await()

            RoomOperationResult.Created(
                RoomSession(
                    roomKey = roomKey,
                    displayName = roomName,
                    userName = identityName,
                    userType = command.identity.type.legacyValue
                )
            )
        }
    }

    override suspend fun joinRoom(
        command: JoinRoomCommand
    ): ZibeResult<RoomOperationResult> {
        val trace = RoomOperationTrace(
            operation = "join",
            identityType = command.identity.type,
            roomKey = command.roomKey,
            phase = "build_join_fanout"
        )
        return executeRoomOperation(trace) {
            val result = buildJoinUpdates(command)
            if (result.aliasInUse) {
                return@executeRoomOperation RoomOperationResult.AliasInUse(
                    command.identity.displayName
                )
            }
            if (!result.existingMembership) {
                trace.phase = "atomic_join_fanout"
                rootRef().updateChildren(result.updates).await()
            }
            RoomOperationResult.Joined(result.session)
        }
    }

    override suspend fun switchRoom(
        command: SwitchRoomCommand
    ): ZibeResult<RoomOperationResult> {
        val trace = RoomOperationTrace(
            operation = "switch",
            identityType = command.target.identity.type,
            roomKey = command.target.roomKey,
            phase = "build_switch_fanout"
        )
        return executeRoomOperation(trace) {
            val join = buildJoinUpdates(command.target)
            if (join.aliasInUse) {
                return@executeRoomOperation RoomOperationResult.AliasInUse(
                    command.target.identity.displayName
                )
            }
            val leave = buildLeaveUpdates(
                LeaveRoomCommand(
                    roomKey = command.previousSession.roomKey,
                    userName = command.previousSession.userName,
                    userType = command.previousSession.userType,
                    eventContent = command.leaveEventContent
                )
            )
            val updates = leave.toMutableMap().apply {
                if (!join.existingMembership) putAll(join.updates)
            }
            if (updates.isNotEmpty()) {
                trace.phase = "atomic_switch_fanout"
                rootRef().updateChildren(updates).await()
            }
            RoomOperationResult.Switched(
                previousRoomKey = command.previousSession.roomKey,
                session = join.session
            )
        }
    }

    override suspend fun leaveRoom(
        command: LeaveRoomCommand
    ): ZibeResult<RoomOperationResult> {
        val trace = RoomOperationTrace(
            operation = "leave",
            identityType = RoomIdentityType.fromLegacy(command.userType),
            roomKey = command.roomKey,
            phase = "build_leave_fanout"
        )
        return executeRoomOperation(trace) {
            if (findUserGroup(myUid, command.roomKey) == null) {
                clearActiveRoom(command.roomKey).getOrThrow()
                return@executeRoomOperation RoomOperationResult.Left(command.roomKey)
            }
            trace.phase = "atomic_leave_fanout"
            rootRef().updateChildren(buildLeaveUpdates(command)).await()
            RoomOperationResult.Left(command.roomKey)
        }
    }

    override suspend fun sendRoomMessage(
        roomKey: String,
        userName: String,
        userType: Int,
        chatType: Int,
        content: String
    ): ZibeResult<Unit> = zibeCatching {
        appendRoomMessage(
            roomKey = roomKey,
            identity = roomIdentity(userName, userType),
            chatType = chatType,
            content = content.trim()
        )
    }

    override suspend fun sendRoomPhotoMessage(
        roomKey: String,
        userName: String,
        userType: Int,
        photoUri: String
    ): ZibeResult<Unit> = zibeCatching {
        checkNotNull(findUserGroup(myUid, roomKey)) {
            "Authenticated user is not a room member"
        }
        val url = uploadGroupPhoto(roomKey, Uri.parse(photoUri))
        appendRoomMessage(
            roomKey = roomKey,
            identity = roomIdentity(userName, userType),
            chatType = MSG_PHOTO,
            content = url
        )
    }

    private data class JoinUpdates(
        val updates: Map<String, Any?>,
        val session: RoomSession,
        val aliasInUse: Boolean = false,
        val existingMembership: Boolean = false
    )

    private suspend fun buildJoinUpdates(command: JoinRoomCommand): JoinUpdates {
        val roomSnapshot = groupMetaRef(command.roomKey).get().await()
        val room = roomSnapshot.getValue(Groups::class.java)
            ?.withLegacyFallback(command.roomKey)
            ?: throw RoomOperationException(RoomFailureReason.ROOM_NOT_FOUND)
        val existingMember = findUserGroup(myUid, command.roomKey)
        if (existingMember != null) {
            return JoinUpdates(
                updates = emptyMap(),
                session = RoomSession(
                    roomKey = command.roomKey,
                    displayName = room.resolvedDisplayName(command.roomKey),
                    userName = existingMember.resolvedDisplayName(),
                    userType = existingMember.type
                ),
                existingMembership = true
            )
        }
        val identityName = command.identity.displayName.trim()
        val aliasKey = aliasKey(command.identity)
        val aliasOwner = firebaseRefsContainer.refGroupAliases
            .child(command.roomKey)
            .child(aliasKey)
            .get()
            .await()
            .getValue(String::class.java)
        if (!aliasOwner.isNullOrBlank() && aliasOwner != myUid) {
            return JoinUpdates(
                updates = emptyMap(),
                session = RoomSession(
                    command.roomKey,
                    command.displayName,
                    identityName,
                    command.identity.type.legacyValue
                ),
                aliasInUse = true
            )
        }

        val isLegacy = isLegacyRoom(roomSnapshot, command.roomKey)
        val members = groupUsersRef(command.roomKey).get().await()
        val messageId = newRequiredGroupMessageKey(command.roomKey)
        val updates = mutableMapOf<String, Any?>()
        updates[groupMetaPath(command.roomKey)] = roomMetaMap(
            roomKey = command.roomKey,
            roomName = room.resolvedDisplayName(command.roomKey),
            description = room.description,
            creatorUid = room.creatorUid,
            users = ServerValue.increment(1),
            totalMessages = ServerValue.increment(1),
            lastMessageId = messageId,
            createdAt = roomSnapshot.child(GroupMetaKeys.CREATED_AT).value ?: 0L,
            lastMessageAt = ServerValue.TIMESTAMP
        )
        updates[groupAliasPath(command.roomKey, aliasKey)] = myUid
        updates[groupMemberPath(command.roomKey, myUid)] = roomMemberMap(
            command.identity,
            aliasKey
        )
        updates[groupMessagePath(command.roomKey, messageId)] = roomMessageMap(
            roomId = room.resolvedRoomKey(command.roomKey),
            roomName = room.resolvedDisplayName(command.roomKey),
            messageId = messageId,
            identity = command.identity,
            chatType = com.zibete.proyecto1.core.constants.Constants.MSG_INFO,
            content = command.eventContent
        )
        updates[roomReadPath(myUid, command.roomKey)] = roomReadStateMap(
            unreadCount = 0,
            lastReadAt = ServerValue.TIMESTAMP,
            lastReadMessageId = messageId,
            lastUnreadMessageId = ""
        )
        addUnreadFanOutCompatible(
            updates = updates,
            members = members,
            senderUid = myUid,
            roomKey = command.roomKey,
            messageId = messageId,
            isLegacy = isLegacy
        )
        return JoinUpdates(
            updates = updates,
            session = RoomSession(
                roomKey = command.roomKey,
                displayName = room.resolvedDisplayName(command.roomKey),
                userName = identityName,
                userType = command.identity.type.legacyValue
            )
        )
    }

    private suspend fun buildLeaveUpdates(command: LeaveRoomCommand): Map<String, Any?> {
        val roomSnapshot = groupMetaRef(command.roomKey).get().await()
        val room = roomSnapshot.getValue(Groups::class.java)
            ?.withLegacyFallback(command.roomKey)
            ?: throw IllegalStateException("Room does not exist")
        val member = findUserGroup(myUid, command.roomKey)
            ?: return mapOf(activeThreadPath(myUid) to null)
        val members = groupUsersRef(command.roomKey).get().await()
        val isLegacy = isLegacyRoom(roomSnapshot, command.roomKey)
        val messageId = newRequiredGroupMessageKey(command.roomKey)
        val updates = mutableMapOf<String, Any?>()
        updates[groupMetaPath(command.roomKey)] = roomMetaMap(
            roomKey = command.roomKey,
            roomName = room.resolvedDisplayName(command.roomKey),
            description = room.description,
            creatorUid = room.creatorUid,
            users = ServerValue.increment(-1),
            totalMessages = ServerValue.increment(1),
            lastMessageId = messageId,
            createdAt = roomSnapshot.child(GroupMetaKeys.CREATED_AT).value ?: 0L,
            lastMessageAt = ServerValue.TIMESTAMP
        )
        updates[groupMessagePath(command.roomKey, messageId)] = roomMessageMap(
            roomId = room.resolvedRoomKey(command.roomKey),
            roomName = room.resolvedDisplayName(command.roomKey),
            messageId = messageId,
            identity = roomIdentity(command.userName, command.userType),
            chatType = com.zibete.proyecto1.core.constants.Constants.MSG_INFO,
            content = command.eventContent
        )
        val persistedAliasKey = member.aliasKey.ifBlank {
            aliasKey(member.resolvedDisplayName())
        }
        updates[groupAliasPath(command.roomKey, persistedAliasKey)] = null
        updates[groupMemberPath(command.roomKey, myUid)] = null
        updates[roomReadPath(myUid, command.roomKey)] = null
        updates[activeThreadPath(myUid)] = null
        addUnreadFanOutCompatible(
            updates = updates,
            members = members,
            senderUid = myUid,
            roomKey = command.roomKey,
            messageId = messageId,
            isLegacy = isLegacy
        )
        return updates
    }

    private suspend fun appendRoomMessage(
        roomKey: String,
        identity: RoomIdentity,
        chatType: Int,
        content: String
    ) {
        require(content.isNotBlank()) { "Room message cannot be blank" }
        val roomSnapshot = groupMetaRef(roomKey).get().await()
        val room = roomSnapshot.getValue(Groups::class.java)
            ?.withLegacyFallback(roomKey)
            ?: throw IllegalStateException("Room does not exist")
        checkNotNull(findUserGroup(myUid, roomKey)) { "Authenticated user is not a room member" }
        val members = groupUsersRef(roomKey).get().await()
        val isLegacy = isLegacyRoom(roomSnapshot, roomKey)
        val currentRead = roomReadRef(roomKey).get().await()
        val messageId = newRequiredGroupMessageKey(roomKey)
        val updates = mutableMapOf<String, Any?>()
        updates[groupMessagePath(roomKey, messageId)] = roomMessageMap(
            roomId = room.resolvedRoomKey(roomKey),
            roomName = room.resolvedDisplayName(roomKey),
            messageId = messageId,
            identity = identity,
            chatType = chatType,
            content = content
        )
        updates[groupMetaPath(roomKey)] = roomMetaMap(
            roomKey = roomKey,
            roomName = room.resolvedDisplayName(roomKey),
            description = room.description,
            creatorUid = room.creatorUid,
            users = roomSnapshot.child(GroupMetaKeys.USERS).numberAsInt(),
            totalMessages = ServerValue.increment(1),
            lastMessageId = messageId,
            createdAt = roomSnapshot.child(GroupMetaKeys.CREATED_AT).value ?: 0L,
            lastMessageAt = ServerValue.TIMESTAMP
        )
        updates[roomReadPath(myUid, roomKey)] = roomReadStateMap(
            unreadCount = 0,
            lastReadAt = ServerValue.TIMESTAMP,
            lastReadMessageId = messageId,
            lastUnreadMessageId = currentRead.child(RoomReadKeys.LAST_UNREAD_MESSAGE_ID)
                .getValue(String::class.java)
                .orEmpty()
        )
        addUnreadFanOutCompatible(
            updates = updates,
            members = members,
            senderUid = myUid,
            roomKey = roomKey,
            messageId = messageId,
            isLegacy = isLegacy
        )
        rootRef().updateChildren(updates).await()
    }

    private suspend fun addUnreadFanOutCompatible(
        updates: MutableMap<String, Any?>,
        members: DataSnapshot,
        senderUid: String,
        roomKey: String,
        messageId: String,
        isLegacy: Boolean
    ) {
        if (!isLegacy) {
            addUnreadFanOut(updates, members, senderUid, roomKey, messageId)
            return
        }
        val modernRecipients = coroutineScope {
            members.children.mapNotNull(DataSnapshot::getKey)
                .filter { it != senderUid }
                .map { uid ->
                    async {
                        uid.takeIf { roomReadRef(roomKey, uid).get().await().exists() }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
        modernRecipients.forEach { uid ->
            updates["${roomReadPath(uid, roomKey)}/${RoomReadKeys.UNREAD_COUNT}"] =
                ServerValue.increment(1)
            updates["${roomReadPath(uid, roomKey)}/${RoomReadKeys.LAST_UNREAD_MESSAGE_ID}"] =
                messageId
        }
    }

    private fun addUnreadFanOut(
        updates: MutableMap<String, Any?>,
        members: DataSnapshot,
        senderUid: String,
        roomKey: String,
        messageId: String
    ) {
        members.children.mapNotNull(DataSnapshot::getKey)
            .filter { it != senderUid }
            .forEach { uid ->
                updates["${roomReadPath(uid, roomKey)}/${RoomReadKeys.UNREAD_COUNT}"] =
                    ServerValue.increment(1)
                updates["${roomReadPath(uid, roomKey)}/${RoomReadKeys.LAST_UNREAD_MESSAGE_ID}"] =
                    messageId
            }
    }

    private fun roomMetaMap(
        roomKey: String,
        roomName: String,
        description: String,
        creatorUid: String,
        users: Any,
        totalMessages: Any,
        lastMessageId: String,
        createdAt: Any,
        lastMessageAt: Any
    ): Map<String, Any> = mapOf(
        GroupMetaKeys.ROOM_ID to roomKey,
        GroupMetaKeys.NAME to roomName,
        GroupMetaKeys.DESCRIPTION to description,
        GroupMetaKeys.CREATOR_UID to creatorUid,
        GroupMetaKeys.TYPE to com.zibete.proyecto1.core.constants.Constants.PUBLIC_GROUP,
        GroupMetaKeys.USERS to users,
        GroupMetaKeys.CREATED_AT to createdAt,
        GroupMetaKeys.TOTAL_MESSAGES to totalMessages,
        GroupMetaKeys.LAST_MESSAGE_AT to lastMessageAt,
        GroupMetaKeys.LAST_MESSAGE_ID to lastMessageId
    )

    private fun isLegacyRoom(snapshot: DataSnapshot, roomKey: String): Boolean =
        !snapshot.hasChild(GroupMetaKeys.ROOM_ID) ||
            snapshot.child(GroupMetaKeys.NAME).getValue(String::class.java) == roomKey

    private fun roomMemberMap(
        identity: RoomIdentity,
        aliasKey: String
    ): Map<String, Any> = mapOf(
        GroupUserKeys.USER_ID to myUid,
        GroupUserKeys.USER_NAME to identity.displayName.trim(),
        GroupUserKeys.USER_TYPE to identity.type.legacyValue,
        GroupUserKeys.JOINED_AT_MS to ServerValue.TIMESTAMP,
        GroupUserKeys.ALIAS_KEY to aliasKey,
        GroupUserKeys.PHOTO_URL to identity.photoUrl.takeIf {
            identity.type == RoomIdentityType.PUBLIC
        }.orEmpty()
    )

    private fun roomMessageMap(
        roomId: String,
        roomName: String,
        messageId: String,
        identity: RoomIdentity,
        chatType: Int,
        content: String
    ): Map<String, Any> = mapOf(
        ChatGroupKeys.CONTENT to content,
        ChatGroupKeys.TIMESTAMP to ServerValue.TIMESTAMP,
        "nameUser" to identity.displayName.trim(),
        ChatGroupKeys.SENDER_UID to myUid,
        ChatGroupKeys.CHAT_TYPE to chatType,
        ChatGroupKeys.USER_TYPE to identity.type.legacyValue,
        ChatGroupKeys.CLIENT_MESSAGE_ID to messageId,
        "roomId" to roomId,
        "roomName" to roomName
    )

    private fun roomReadStateMap(
        unreadCount: Int,
        lastReadAt: Any,
        lastReadMessageId: String,
        lastUnreadMessageId: String
    ): Map<String, Any> = mapOf(
        RoomReadKeys.UNREAD_COUNT to unreadCount,
        RoomReadKeys.LAST_READ_AT to lastReadAt,
        RoomReadKeys.LAST_READ_MESSAGE_ID to lastReadMessageId,
        RoomReadKeys.LAST_UNREAD_MESSAGE_ID to lastUnreadMessageId
    )

    private fun roomIdentity(userName: String, userType: Int): RoomIdentity = RoomIdentity(
        displayName = userName.trim(),
        type = RoomIdentityType.fromLegacy(userType)
    )

    private fun aliasKey(alias: String): String = alias.trim().lowercase(Locale.ROOT)

    private fun aliasKey(identity: RoomIdentity): String =
        if (identity.type == RoomIdentityType.ANONYMOUS) {
            aliasKey(identity.displayName)
        } else {
            "public-${stableUidHash(myUid)}"
        }

    private fun stableUidHash(uid: String): String = MessageDigest.getInstance("SHA-256")
        .digest(uid.toByteArray())
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        .take(32)

    private fun newRequiredGroupMessageKey(roomKey: String): String =
        checkNotNull(groupChatRef(roomKey).push().key) { "Could not generate room message id" }

    // EVENTS
    override fun observeGroupChatEvents(groupName: String): Flow<GroupChatChildEvent> = callbackFlow {
        val groupChatQuery = groupChatRef(groupName).limitToLast(100)

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val msg = snapshot.toCompatibleChatGroup() ?: return
                trySend(GroupChatChildEvent.Added(ChatGroupItem(id, msg)))
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val msg = snapshot.toCompatibleChatGroup() ?: return
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

        groupChatQuery.addChildEventListener(listener)
        awaitClose { groupChatQuery.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    // BADGES / COUNTS
    override fun unreadGroupBadgeCount(groupName: String): Flow<Int> =
        combine(
            observeTotalRoomUnread(),
            observeUnreadPrivateMessages()
        ) { unreadGroup, unreadPrivateChats ->
            (unreadGroup + unreadPrivateChats).coerceAtLeast(0)
        }

    // ========== CORE FLOWS ==========

    override fun observeUnreadGroupChat(groupName: String): Flow<Int> =
        observeRoomUnreadOrNull(groupName).flatMapLatest { unread ->
            if (unread != null) flowOf(unread)
            else combine(
                observeGroupTotalMessages(groupName),
                observeReadGroupMessages()
            ) { total, read ->
                (total - read).coerceAtLeast(0)
            }
        }

    /** suma unreadCount de chats privados dentro del nodo /Users/Data/<uid>/group_dm */
    override fun observeUnreadPrivateMessages(): Flow<Int> = callbackFlow {
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

    private fun observeRoomUnreadOrNull(roomKey: String): Flow<Int?> = callbackFlow {
        val ref = roomReadRef(roomKey)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    if (snapshot.exists()) {
                        snapshot.child(RoomReadKeys.UNREAD_COUNT).numberAsInt().coerceAtLeast(0)
                    } else null
                )
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.distinctUntilChanged()

    override fun observeGroupUsers(groupName: String): Flow<List<UserGroup>> = callbackFlow {
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

    override fun observeIsUserInGroup(groupName: String, userId: String): Flow<Boolean> = callbackFlow {

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

    override suspend fun getGroup(groupName: String): Groups? =
        groupMetaRef(groupName)
            .get()
            .await()
            .takeIf { it.exists() }
            ?.getValue(Groups::class.java)
            ?.withLegacyFallback(groupName)

    private fun groupMetaRef(groupName: String): DatabaseReference =
        firebaseRefsContainer.refGroupMeta.child(groupName)

    private fun groupUsersRef(groupName: String): DatabaseReference =
        firebaseRefsContainer.refGroupUsers.child(groupName)

    private fun groupChatRef(groupName: String): DatabaseReference =
        firebaseRefsContainer.refGroupChat.child(groupName)

    private fun rootRef(): DatabaseReference = firebaseRefsContainer.firebaseDatabase.reference

    private fun roomsReadRootRef(uid: String = myUid): DatabaseReference =
        firebaseRefsContainer.refData.child(uid).child(NODE_ROOMS)

    private fun roomReadRef(roomKey: String, uid: String = myUid): DatabaseReference =
        roomsReadRootRef(uid).child(roomKey)

    private fun activeThreadRef(uid: String = myUid): DatabaseReference =
        firebaseRefsContainer.refData.child(uid)
            .child(NODE_CLIENT_DATA)
            .child(NODE_ACTIVE_VIEW)
            .child(ActiveViewKeys.ACTIVE_THREAD)

    private fun groupMetaPath(roomKey: String) = "Groups/Meta/$roomKey"
    private fun groupMemberPath(roomKey: String, uid: String) = "Groups/Users/$roomKey/$uid"
    private fun groupMessagePath(roomKey: String, messageId: String) =
        "Groups/Chat/$roomKey/$messageId"
    private fun groupNamePath(nameKey: String) = "Groups/Names/$nameKey"
    private fun groupAliasPath(roomKey: String, aliasKey: String) =
        "Groups/Aliases/$roomKey/$aliasKey"
    private fun roomReadPath(uid: String, roomKey: String) =
        "Users/Data/$uid/Rooms/$roomKey"
    private fun activeThreadPath(uid: String) =
        "Users/Data/$uid/ClientData/ActiveView/activeThread"

    private fun totalMessagesRef(groupName: String): DatabaseReference =
        groupMetaRef(groupName)
            .child(GroupMetaKeys.TOTAL_MESSAGES)

    private fun groupPrivateConversationsRef(uid: String = myUid): DatabaseReference =
        firebaseRefsContainer.refData.child(uid).child(NODE_GROUP_DM)


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

    private data class RoomOperationTrace(
        val operation: String,
        val identityType: RoomIdentityType,
        var roomKey: String = "",
        var phase: String = "validate"
    )

    private inline fun <T> executeRoomOperation(
        trace: RoomOperationTrace,
        block: () -> T
    ): ZibeResult<T> = when (val result = zibeCatching(block)) {
        is ZibeResult.Success -> result
        is ZibeResult.Failure -> {
            val mapped = result.exception.toRoomOperationException()
            Log.d(
                ROOM_OPERATIONS_TAG,
                "room_operation_failed operation=${trace.operation} " +
                    "identity=${trace.identityType.name.lowercase(Locale.ROOT)} " +
                    "roomKey=${safeRoomKey(trace.roomKey)} phase=${trace.phase} " +
                    "exception=${result.exception::class.java.simpleName} " +
                    "firebaseCode=${mapped.reason.firebaseCode}"
            )
            ZibeResult.Failure(mapped)
        }
    }

    private fun Throwable.toRoomOperationException(): RoomOperationException = when {
        this is RoomOperationException -> this
        this is RoomValidationException ->
            RoomOperationException(RoomFailureReason.INVALID_IDENTITY, this)
        this is FirebaseNetworkException ->
            RoomOperationException(RoomFailureReason.CONNECTION, this)
        this is DatabaseException && message.orEmpty().contains("permission", ignoreCase = true) ->
            RoomOperationException(RoomFailureReason.PERMISSION, this)
        this is RoomMembershipNotFoundException ->
            RoomOperationException(RoomFailureReason.SESSION_INVALID, this)
        this is IllegalStateException && message.orEmpty().contains(
            "authenticated user",
            ignoreCase = true
        ) -> RoomOperationException(RoomFailureReason.SESSION_INVALID, this)
        else -> RoomOperationException(RoomFailureReason.UNEXPECTED, this)
    }

    private val RoomFailureReason.firebaseCode: String
        get() = when (this) {
            RoomFailureReason.INVALID_IDENTITY -> "invalid_identity"
            RoomFailureReason.PERMISSION -> "permission_denied"
            RoomFailureReason.CONNECTION -> "network_error"
            RoomFailureReason.ROOM_NOT_FOUND -> "room_not_found"
            RoomFailureReason.SESSION_INVALID -> "session_invalid"
            RoomFailureReason.UNEXPECTED -> "unknown"
        }

    private fun safeRoomKey(roomKey: String): String = when {
        roomKey.isBlank() -> "pending"
        roomKey.length <= 7 -> "${roomKey.take(2)}..."
        else -> "${roomKey.take(4)}...${roomKey.takeLast(3)}"
    }

    private fun chatListRef(uid: String = myUid) =
        firebaseRefsContainer.refData.child(uid)
            .child(NODE_CHAT_LIST)

    private suspend fun uploadGroupPhoto(groupName: String, photoUri: Uri): String {
        val fileName = "$myUid$KEY_SEPARATOR${System.currentTimeMillis()}$EXTENSION_IMAGE"
        val ref = firebaseRefsContainer.storageGroupChatRef
            .child(groupName)
            .child(PATH_PHOTOS)
            .child(fileName)

        ref.putFile(photoUri).await()
        return ref.downloadUrl.await().toString()
    }

    private companion object {
        const val ROOM_OPERATIONS_TAG = "ZibeRooms"
    }

    private fun DataSnapshot.numberAsInt(): Int = (value as? Number)?.toInt() ?: 0

    private fun DataSnapshot.numberAsLong(): Long = (value as? Number)?.toLong() ?: 0L

    private fun DataSnapshot.toCompatibleChatGroup(): ChatGroup? {
        val current = runCatching { getValue(ChatGroup::class.java) }.getOrNull() ?: return null
        return current.copy(
            timestamp = current.timestamp.takeIf { it > 0L }
                ?: child(GroupChatKeys.DATE).numberAsLong(),
            chatType = current.chatType.takeIf { it > 0 }
                ?: child(GroupChatKeys.TYPE).numberAsInt(),
            nameUser = current.nameUser.ifBlank {
                child(GroupChatKeys.NAME_USER).getValue(String::class.java).orEmpty()
            }
        )
    }

    private suspend fun DatabaseReference.runTransactionAwaitNullable(
        computeNewValue: (Any?) -> Any?
    ) {
        suspendCancellableCoroutine { continuation ->
            runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    currentData.value = computeNewValue(currentData.value)
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (!continuation.isActive) return
                    if (error != null) continuation.resumeWithException(error.toException())
                    else continuation.resume(Unit)
                }
            })
        }
    }

}
