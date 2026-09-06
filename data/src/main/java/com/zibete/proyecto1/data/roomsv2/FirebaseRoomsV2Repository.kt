package com.zibete.proyecto1.data.roomsv2

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.zibete.proyecto1.domain.roomsv2.CreateRoomV2Request
import com.zibete.proyecto1.domain.roomsv2.JoinRoomV2Request
import com.zibete.proyecto1.domain.roomsv2.RoomV2DirectoryItem
import com.zibete.proyecto1.domain.roomsv2.RoomV2Identity
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Membership
import com.zibete.proyecto1.domain.roomsv2.RoomV2Message
import com.zibete.proyecto1.domain.roomsv2.RoomV2Role
import com.zibete.proyecto1.domain.roomsv2.RoomV2Status
import com.zibete.proyecto1.domain.roomsv2.RoomsV2Repository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRoomsV2Repository(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : RoomsV2Repository {

    override fun observeDirectory(limit: Int): Flow<List<RoomV2DirectoryItem>> =
        valueFlow(
            database.reference.child("RoomsV2/publicRooms")
                .orderByChild("updatedAt")
                .limitToLast(limit.coerceIn(1, 200)),
        ) { snapshot ->
            snapshot.children.mapNotNull(::directoryItem).sortedByDescending { it.updatedAt }
        }

    override fun observeMemberships(): Flow<List<RoomV2Membership>> {
        val uid = requireUid()
        return valueFlow(database.reference.child("RoomsV2/membershipIndexByUser/$uid")) { snapshot ->
            snapshot.children.mapNotNull(::membership).filter { it.identity.active }
                .sortedByDescending { it.joinedAt }
        }
    }

    override fun observeMessages(roomId: String, limit: Int): Flow<List<RoomV2Message>> =
        valueFlow(
            database.reference.child("RoomsV2/publicMessages/$roomId")
                .orderByChild("sentAt")
                .limitToLast(limit.coerceIn(1, 100)),
        ) { snapshot -> snapshot.children.mapNotNull { message(roomId, it) }.sortedBy { it.sentAt } }

    override suspend fun refreshDirectory(): Result<Unit> = catching {
        database.reference.child("RoomsV2/publicRooms").limitToFirst(1).get().await()
        Unit
    }

    override suspend fun createRoom(request: CreateRoomV2Request): Result<RoomV2Membership> = catching {
        val data = call(
            "create_room_v2",
            mapOf("name" to request.name, "description" to request.description),
        )
        membershipFromCallable(data)
    }

    override suspend fun joinRoom(request: JoinRoomV2Request): Result<RoomV2Membership> = catching {
        val payload = mutableMapOf<String, Any?>(
            "roomId" to request.roomId,
            "mode" to request.mode.name.lowercase(),
        )
        request.alias?.let { payload["alias"] = it }
        membershipFromCallable(call("join_room_v2", payload))
    }

    override suspend fun leaveRoom(roomId: String): Result<Unit> = catching {
        call("leave_room_v2", mapOf("roomId" to roomId))
        Unit
    }

    override suspend fun closeRoom(roomId: String): Result<Unit> = catching {
        call("close_room_v2", mapOf("roomId" to roomId))
        Unit
    }

    override suspend fun transferOwnership(roomId: String, targetIdentityId: String): Result<Unit> = catching {
        call(
            "transfer_room_owner_v2",
            mapOf("roomId" to roomId, "targetIdentityId" to targetIdentityId),
        )
        Unit
    }

    override suspend fun setModerator(
        roomId: String,
        targetIdentityId: String,
        enabled: Boolean,
    ): Result<Unit> = catching {
        call(
            "set_room_moderator_v2",
            mapOf("roomId" to roomId, "targetIdentityId" to targetIdentityId, "enabled" to enabled),
        )
        Unit
    }

    override suspend fun removeMember(
        roomId: String,
        targetIdentityId: String,
        ban: Boolean,
    ): Result<Unit> = catching {
        val function = if (ban) "ban_room_member_v2" else "kick_room_member_v2"
        call(function, mapOf("roomId" to roomId, "targetIdentityId" to targetIdentityId))
        Unit
    }

    override suspend fun sendText(
        roomId: String,
        text: String,
        clientMessageId: String,
    ): Result<RoomV2Message> = catching {
        val data = call(
            "send_room_v2_text",
            mapOf("roomId" to roomId, "text" to text, "clientMessageId" to clientMessageId),
        )
        messageFromMap(data) ?: error("send_room_v2_text returned an invalid message")
    }

    private suspend fun call(name: String, payload: Map<String, Any?>): Map<String, Any?> {
        requireUid()
        val raw = functions.getHttpsCallable(name).call(payload).await().data
        @Suppress("UNCHECKED_CAST")
        return raw as? Map<String, Any?> ?: error("$name returned an invalid payload")
    }

    private fun requireUid(): String = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Authentication is required for RoomsV2")

    private fun directoryItem(snapshot: DataSnapshot): RoomV2DirectoryItem? {
        val roomId = snapshot.child("roomId").string() ?: snapshot.key ?: return null
        val name = snapshot.child("name").string() ?: return null
        return RoomV2DirectoryItem(
            roomId = roomId,
            name = name,
            description = snapshot.child("description").string().orEmpty(),
            memberCount = snapshot.child("memberCount").int(0),
            pendingCount = snapshot.child("pendingCount").int(0),
            status = enumOrDefault(snapshot.child("status").string(), RoomV2Status.OPEN),
            updatedAt = snapshot.child("updatedAt").long(0L),
        )
    }

    private fun membership(snapshot: DataSnapshot): RoomV2Membership? {
        val roomId = snapshot.child("roomId").string() ?: snapshot.key ?: return null
        val identityId = snapshot.child("identityId").string() ?: return null
        return RoomV2Membership(
            roomId = roomId,
            identity = RoomV2Identity(
                identityId = identityId,
                displayName = snapshot.child("displayName").string() ?: return null,
                mode = enumOrDefault(snapshot.child("mode").string(), RoomV2IdentityMode.REAL),
                role = enumOrDefault(snapshot.child("role").string(), RoomV2Role.MEMBER),
                active = snapshot.child("active").getValue(Boolean::class.java) == true,
            ),
            joinedAt = snapshot.child("joinedAt").long(0L),
            lastReadAt = snapshot.child("lastReadAt").long(0L),
        )
    }

    private fun message(roomId: String, snapshot: DataSnapshot): RoomV2Message? {
        val raw = mapOf<String, Any?>(
            "messageId" to (snapshot.child("messageId").string() ?: snapshot.key),
            "roomId" to roomId,
            "authorIdentityId" to snapshot.child("authorIdentityId").string(),
            "authorDisplayName" to snapshot.child("authorDisplayName").string(),
            "authorMode" to snapshot.child("authorMode").string(),
            "text" to snapshot.child("text").string(),
            "sentAt" to snapshot.child("sentAt").long(0L),
        )
        return messageFromMap(raw)
    }

    private fun membershipFromCallable(raw: Map<String, Any?>): RoomV2Membership {
        val membership = raw["membership"] as? Map<*, *> ?: raw
        val roomId = membership.string("roomId") ?: error("Missing roomId")
        return RoomV2Membership(
            roomId = roomId,
            identity = RoomV2Identity(
                identityId = membership.string("identityId") ?: error("Missing identityId"),
                displayName = membership.string("displayName") ?: error("Missing displayName"),
                mode = enumOrDefault(membership.string("mode"), RoomV2IdentityMode.REAL),
                role = enumOrDefault(membership.string("role"), RoomV2Role.MEMBER),
                active = membership["active"] as? Boolean ?: true,
            ),
            joinedAt = membership.number("joinedAt"),
            lastReadAt = membership.number("lastReadAt"),
        )
    }

    private fun messageFromMap(raw: Map<String, Any?>): RoomV2Message? {
        val messageId = raw.string("messageId") ?: return null
        val roomId = raw.string("roomId") ?: return null
        val authorIdentityId = raw.string("authorIdentityId") ?: return null
        val authorDisplayName = raw.string("authorDisplayName") ?: return null
        val text = raw.string("text") ?: return null
        return RoomV2Message(
            messageId = messageId,
            roomId = roomId,
            authorIdentityId = authorIdentityId,
            authorDisplayName = authorDisplayName,
            authorMode = enumOrDefault(raw.string("authorMode"), RoomV2IdentityMode.REAL),
            text = text,
            sentAt = raw.number("sentAt"),
        )
    }

    private fun <T> valueFlow(query: Query, mapper: (DataSnapshot) -> T): Flow<T> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(mapper(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    private suspend fun <T> catching(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(value: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: fallback

    private fun DataSnapshot.string(): String? = getValue(String::class.java)?.trim()?.takeIf { it.isNotEmpty() }
    private fun DataSnapshot.long(default: Long): Long = (value as? Number)?.toLong() ?: default
    private fun DataSnapshot.int(default: Int): Int = (value as? Number)?.toInt() ?: default
    private fun Map<*, *>.string(key: String): String? = this[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    private fun Map<*, *>.number(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L
}
