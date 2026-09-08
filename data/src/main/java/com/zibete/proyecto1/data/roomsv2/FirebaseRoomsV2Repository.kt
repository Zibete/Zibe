package com.zibete.proyecto1.data.roomsv2

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.roomsv2.CreateRoomV2Request
import com.zibete.proyecto1.domain.roomsv2.JoinRoomV2Request
import com.zibete.proyecto1.domain.roomsv2.RoomV2Attachment
import com.zibete.proyecto1.domain.roomsv2.RoomV2Conversation
import com.zibete.proyecto1.domain.roomsv2.RoomV2DirectoryItem
import com.zibete.proyecto1.domain.roomsv2.RoomV2ErrorCode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Exception
import com.zibete.proyecto1.domain.roomsv2.RoomV2Identity
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Membership
import com.zibete.proyecto1.domain.roomsv2.RoomV2Message
import com.zibete.proyecto1.domain.roomsv2.RoomV2MessageKind
import com.zibete.proyecto1.domain.roomsv2.RoomV2Report
import com.zibete.proyecto1.domain.roomsv2.RoomV2Role
import com.zibete.proyecto1.domain.roomsv2.RoomV2Status
import com.zibete.proyecto1.domain.roomsv2.RoomV2Thread
import com.zibete.proyecto1.domain.roomsv2.RoomsV2ChatRepository
import com.zibete.proyecto1.domain.roomsv2.RoomsV2ModerationRepository
import com.zibete.proyecto1.domain.roomsv2.RoomsV2Repository
import java.text.Normalizer
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRoomsV2Repository(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : RoomsV2Repository, RoomsV2ChatRepository, RoomsV2ModerationRepository {

    override fun observeDirectory(
        limit: Int,
        search: String,
    ): Flow<List<RoomV2DirectoryItem>> {
        val normalizedSearch = normalizeSearch(search)
        return valueFlow(
            database.reference.child("$ROOT/publicRooms")
                .orderByChild("updatedAt")
                .limitToLast(limit.coerceIn(1, MAX_DIRECTORY_LIMIT)),
        ) { snapshot ->
            snapshot.children
                .mapNotNull(::directoryItem)
                .filter { item ->
                    normalizedSearch.isEmpty() ||
                        normalizeSearch(item.normalizedName).contains(normalizedSearch)
                }
                .sortedByDescending { it.updatedAt }
        }
    }

    override fun observeRoom(roomId: String): Flow<RoomV2DirectoryItem?> =
        valueFlow(
            database.reference.child("$ROOT/publicRooms/${safeId(roomId)}"),
        ) { snapshot -> directoryItem(snapshot) }

    override fun observeMemberships(): Flow<List<RoomV2Membership>> {
        val uid = requireUid()
        return valueFlow(database.reference.child("$ROOT/membershipIndexByUser/$uid")) { snapshot ->
            snapshot.children
                .mapNotNull(::membership)
                .filter { it.identity.active }
                .sortedByDescending { it.joinedAt }
        }
    }

    override suspend fun loadDirectoryPage(
        search: String,
        after: RoomV2DirectoryItem?,
    ): ZibeResult<List<RoomV2DirectoryItem>> = catching {
        val normalizedSearch = normalizeSearch(search)
        val snapshot = database.reference.child("$ROOT/publicRooms")
            .orderByChild("updatedAt")
            .limitToLast(MAX_DIRECTORY_PAGE)
            .get()
            .await()
        snapshot.children
            .mapNotNull(::directoryItem)
            .asSequence()
            .filter { after == null || it.updatedAt < after.updatedAt }
            .filter {
                normalizedSearch.isEmpty() ||
                    normalizeSearch(it.normalizedName).contains(normalizedSearch)
            }
            .sortedByDescending { it.updatedAt }
            .take(DIRECTORY_PAGE_SIZE)
            .toList()
    }

    override suspend fun refreshDirectory(): ZibeResult<Unit> = catching {
        database.reference.child("$ROOT/publicRooms").limitToFirst(1).get().await()
        Unit
    }

    override suspend fun createRoom(request: CreateRoomV2Request): ZibeResult<RoomV2Membership> =
        catching {
            membershipFromCallable(
                call(
                    "create_room_v2",
                    mapOf(
                        "name" to request.name,
                        "description" to request.description,
                        "operationId" to request.operationId,
                    ),
                ),
            )
        }

    override suspend fun joinRoom(request: JoinRoomV2Request): ZibeResult<RoomV2Membership> =
        catching {
            val payload = mutableMapOf<String, Any?>(
                "roomId" to safeId(request.roomId),
                "mode" to request.mode.name.lowercase(),
            )
            request.alias?.let { payload["alias"] = it }
            membershipFromCallable(call("join_room_v2", payload))
        }

    override suspend fun leaveRoom(roomId: String): ZibeResult<Unit> = unitCall(
        "leave_room_v2",
        mapOf("roomId" to safeId(roomId)),
    )

    override suspend fun setNotifications(
        roomId: String,
        enabled: Boolean,
    ): ZibeResult<Unit> = unitCall(
        "set_room_notifications_v2",
        mapOf("roomId" to safeId(roomId), "enabled" to enabled),
    )

    override fun observeParticipants(roomId: String): Flow<List<RoomV2Identity>> =
        valueFlow(database.reference.child("$ROOT/publicMembers/${safeId(roomId)}")) { snapshot ->
            snapshot.children
                .mapNotNull(::identity)
                .filter { it.active }
                .sortedWith(
                    compareBy<RoomV2Identity> { roleOrder(it.role) }
                        .thenBy { it.displayName.lowercase() },
                )
        }

    override fun observeMessages(
        thread: RoomV2Thread,
        limit: Int,
    ): Flow<List<RoomV2Message>> {
        val roomId = safeId(thread.roomId)
        val conversationId = thread.conversationId
        val query = if (conversationId == null) {
            database.reference.child("$ROOT/publicMessages/$roomId")
                .orderByChild("seq")
                .limitToLast(limit.coerceIn(1, MAX_MESSAGE_LIMIT))
        } else {
            database.reference.child(
                "$ROOT/privateMessages/${safeId(conversationId)}",
            ).orderByChild("seq").limitToLast(limit.coerceIn(1, MAX_MESSAGE_LIMIT))
        }
        return valueFlow(query) { snapshot ->
            snapshot.children
                .mapNotNull { message(roomId, it, conversationId) }
                .sortedBy { it.seq }
        }
    }

    override fun observeConversations(roomId: String): Flow<List<RoomV2Conversation>> {
        val uid = requireUid()
        return valueFlow(
            database.reference.child(
                "$ROOT/conversationIndexByUser/$uid/${safeId(roomId)}",
            ),
        ) { snapshot ->
            snapshot.children
                .filter { it.child("visible").getValue(Boolean::class.java) != false }
                .mapNotNull(::conversation)
                .sortedByDescending { it.updatedAt }
        }
    }

    override suspend fun loadEarlierMessages(
        thread: RoomV2Thread,
        beforeSeq: Long,
    ): ZibeResult<List<RoomV2Message>> = catching {
        if (beforeSeq <= 0L) return@catching emptyList()
        val roomId = safeId(thread.roomId)
        val conversationId = thread.conversationId
        val base = if (conversationId == null) {
            database.reference.child("$ROOT/publicMessages/$roomId")
        } else {
            database.reference.child("$ROOT/privateMessages/${safeId(conversationId)}")
        }
        base.orderByChild("seq")
            .endAt((beforeSeq - 1).toDouble())
            .limitToLast(DIRECTORY_PAGE_SIZE)
            .get()
            .await()
            .children
            .mapNotNull { message(roomId, it, conversationId) }
            .sortedBy { it.seq }
    }

    override suspend fun openPrivate(
        roomId: String,
        targetIdentityId: String,
    ): ZibeResult<String> = catching {
        val data = call(
            "open_room_private_v2",
            mapOf(
                "roomId" to safeId(roomId),
                "targetIdentityId" to safeId(targetIdentityId),
            ),
        )
        data.string("conversationId") ?: throw contractFailure("Missing conversationId")
    }

    override suspend fun sendText(
        thread: RoomV2Thread,
        text: String,
        clientMessageId: String,
        replyToMessageId: String?,
    ): ZibeResult<RoomV2Message> = catching {
        val conversationId = thread.conversationId
        val payload = mutableMapOf<String, Any?>(
            "roomId" to safeId(thread.roomId),
            "text" to text,
            "clientMessageId" to safeId(clientMessageId),
        )
        replyToMessageId?.let { payload["replyToMessageId"] = safeId(it) }
        val function = if (conversationId == null) {
            "send_room_v2_text"
        } else {
            payload["conversationId"] = safeId(conversationId)
            "send_room_private_v2_text"
        }
        messageFromMap(call(function, payload), conversationId)
            ?: throw contractFailure("$function returned an invalid message")
    }

    override suspend fun markRead(
        thread: RoomV2Thread,
        visibleSeq: Long,
    ): ZibeResult<Unit> = unitCall(
        "mark_room_thread_read_v2",
        buildMap {
            put("roomId", safeId(thread.roomId))
            put("visibleSeq", visibleSeq.coerceAtLeast(0))
            thread.conversationId?.let { put("conversationId", safeId(it)) }
        },
    )

    override suspend fun setVisibleThread(
        thread: RoomV2Thread,
        visible: Boolean,
    ): ZibeResult<Unit> = unitCall(
        "set_room_visible_thread_v2",
        buildMap {
            put("roomId", safeId(thread.roomId))
            put("visible", visible)
            thread.conversationId?.let { put("conversationId", safeId(it)) }
        },
    )

    override suspend fun blockPrivate(
        thread: RoomV2Thread,
        blocked: Boolean,
    ): ZibeResult<Unit> {
        val conversationId = thread.conversationId
            ?: return ZibeResult.Failure(
                RoomV2Exception(RoomV2ErrorCode.INVALID_INPUT),
            )
        return unitCall(
            "block_room_private_v2",
            mapOf(
                "roomId" to safeId(thread.roomId),
                "conversationId" to safeId(conversationId),
                "blocked" to blocked,
            ),
        )
    }

    override fun observeReports(roomId: String): Flow<List<RoomV2Report>> =
        valueFlow(database.reference.child("$ROOT/reports/${safeId(roomId)}")) { snapshot ->
            snapshot.children
                .mapNotNull(::report)
                .sortedByDescending { it.createdAt }
        }

    override suspend fun editRoom(
        roomId: String,
        name: String,
        description: String,
    ): ZibeResult<Unit> = unitCall(
        "edit_room_v2",
        mapOf("roomId" to safeId(roomId), "name" to name, "description" to description),
    )

    override suspend fun closeRoom(roomId: String): ZibeResult<Unit> = unitCall(
        "close_room_v2",
        mapOf("roomId" to safeId(roomId)),
    )

    override suspend fun transferOwnership(
        roomId: String,
        targetIdentityId: String,
    ): ZibeResult<Unit> = unitCall(
        "transfer_room_owner_v2",
        mapOf(
            "roomId" to safeId(roomId),
            "targetIdentityId" to safeId(targetIdentityId),
        ),
    )

    override suspend fun setModerator(
        roomId: String,
        targetIdentityId: String,
        enabled: Boolean,
    ): ZibeResult<Unit> = unitCall(
        "set_room_moderator_v2",
        mapOf(
            "roomId" to safeId(roomId),
            "targetIdentityId" to safeId(targetIdentityId),
            "enabled" to enabled,
        ),
    )

    override suspend fun removeMember(
        roomId: String,
        targetIdentityId: String,
        ban: Boolean,
    ): ZibeResult<Unit> = unitCall(
        if (ban) "ban_room_member_v2" else "kick_room_member_v2",
        mapOf(
            "roomId" to safeId(roomId),
            "targetIdentityId" to safeId(targetIdentityId),
        ),
    )

    override suspend fun removeMessage(
        roomId: String,
        messageId: String,
    ): ZibeResult<Unit> = unitCall(
        "remove_room_message_v2",
        mapOf("roomId" to safeId(roomId), "messageId" to safeId(messageId)),
    )

    override suspend fun reportMessage(
        thread: RoomV2Thread,
        messageId: String,
        reason: String,
    ): ZibeResult<Unit> = unitCall(
        "report_room_message_v2",
        buildMap {
            put("roomId", safeId(thread.roomId))
            put("messageId", safeId(messageId))
            put("reason", reason)
            thread.conversationId?.let { put("conversationId", safeId(it)) }
        },
    )

    override suspend fun resolveReport(
        roomId: String,
        reportId: String,
        resolution: String,
    ): ZibeResult<Unit> = unitCall(
        "resolve_room_report_v2",
        mapOf(
            "roomId" to safeId(roomId),
            "reportId" to safeId(reportId),
            "resolution" to resolution,
        ),
    )

    private suspend fun unitCall(
        name: String,
        payload: Map<String, Any?>,
    ): ZibeResult<Unit> = catching {
        call(name, payload)
        Unit
    }

    private suspend fun call(
        name: String,
        payload: Map<String, Any?>,
    ): Map<String, Any?> {
        requireUid()
        val raw = functions.getHttpsCallable(name).call(payload).await().data
        @Suppress("UNCHECKED_CAST")
        return raw as? Map<String, Any?>
            ?: throw contractFailure("$name returned an invalid payload")
    }

    private fun requireUid(): String = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        ?: throw RoomV2Exception(RoomV2ErrorCode.UNAUTHENTICATED)

    private fun directoryItem(snapshot: DataSnapshot): RoomV2DirectoryItem? {
        if (!snapshot.exists()) return null
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
            normalizedName = snapshot.child("normalizedName").string() ?: name,
        )
    }

    private fun membership(snapshot: DataSnapshot): RoomV2Membership? {
        if (!snapshot.exists()) return null
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
            unreadCount = snapshot.child("unreadCount").long(0L),
            lastReadSeq = snapshot.child("lastReadSeq").long(0L),
            notificationsEnabled = snapshot.child("notificationsEnabled")
                .getValue(Boolean::class.java) ?: true,
        )
    }

    private fun identity(snapshot: DataSnapshot): RoomV2Identity? {
        if (!snapshot.exists()) return null
        return RoomV2Identity(
            identityId = snapshot.child("identityId").string() ?: snapshot.key ?: return null,
            displayName = snapshot.child("displayName").string() ?: return null,
            mode = enumOrDefault(snapshot.child("mode").string(), RoomV2IdentityMode.REAL),
            role = enumOrDefault(snapshot.child("role").string(), RoomV2Role.MEMBER),
            active = snapshot.child("active").getValue(Boolean::class.java) == true,
        )
    }

    private fun message(
        roomId: String,
        snapshot: DataSnapshot,
        conversationId: String? = null,
    ): RoomV2Message? {
        if (!snapshot.exists()) return null
        val raw = mapOf<String, Any?>(
            "messageId" to (snapshot.child("messageId").string() ?: snapshot.key),
            "roomId" to roomId,
            "authorIdentityId" to snapshot.child("authorIdentityId").string(),
            "authorDisplayName" to snapshot.child("authorDisplayName").string(),
            "authorMode" to snapshot.child("authorMode").string(),
            "text" to snapshot.child("text").getValue(String::class.java).orEmpty(),
            "sentAt" to snapshot.child("sentAt").long(0L),
            "seq" to snapshot.child("seq").long(0L),
            "kind" to snapshot.child("kind").string(),
            "removed" to snapshot.child("removed").getValue(Boolean::class.java),
            "conversationId" to (snapshot.child("conversationId").string() ?: conversationId),
            "replyToMessageId" to snapshot.child("replyToMessageId").string(),
            "attachment" to snapshot.child("attachment").value,
        )
        return messageFromMap(raw, conversationId)
    }

    private fun conversation(snapshot: DataSnapshot): RoomV2Conversation? {
        if (!snapshot.exists()) return null
        val roomId = snapshot.child("roomId").string() ?: return null
        val conversationId = snapshot.child("conversationId").string()
            ?: snapshot.key
            ?: return null
        val other = snapshot.child("otherIdentity")
        return RoomV2Conversation(
            conversationId = conversationId,
            roomId = roomId,
            otherIdentity = identity(other) ?: return null,
            closed = snapshot.child("closed").getValue(Boolean::class.java) == true,
            blocked = snapshot.child("blocked").getValue(Boolean::class.java) == true,
            lastText = snapshot.child("lastText").getValue(String::class.java).orEmpty(),
            updatedAt = snapshot.child("updatedAt").long(0L),
            unreadCount = snapshot.child("unreadCount").long(0L),
            lastReadSeq = snapshot.child("lastReadSeq").long(0L),
        )
    }

    private fun report(snapshot: DataSnapshot): RoomV2Report? {
        if (!snapshot.exists()) return null
        val roomId = snapshot.child("roomId").string() ?: return null
        val evidence = snapshot.child("evidence")
        return RoomV2Report(
            reportId = snapshot.child("reportId").string() ?: snapshot.key ?: return null,
            roomId = roomId,
            reason = snapshot.child("reason").getValue(String::class.java).orEmpty(),
            status = snapshot.child("status").getValue(String::class.java).orEmpty(),
            evidence = message(roomId, evidence, evidence.child("conversationId").string())
                ?: return null,
            createdAt = snapshot.child("createdAt").long(0L),
            resolution = snapshot.child("resolution").getValue(String::class.java).orEmpty(),
        )
    }

    private fun membershipFromCallable(raw: Map<String, Any?>): RoomV2Membership {
        val membership = raw["membership"] as? Map<*, *> ?: raw
        val roomId = membership.string("roomId") ?: throw contractFailure("Missing roomId")
        return RoomV2Membership(
            roomId = roomId,
            identity = RoomV2Identity(
                identityId = membership.string("identityId")
                    ?: throw contractFailure("Missing identityId"),
                displayName = membership.string("displayName")
                    ?: throw contractFailure("Missing displayName"),
                mode = enumOrDefault(membership.string("mode"), RoomV2IdentityMode.REAL),
                role = enumOrDefault(membership.string("role"), RoomV2Role.MEMBER),
                active = membership["active"] as? Boolean ?: true,
            ),
            joinedAt = membership.number("joinedAt"),
            lastReadAt = membership.number("lastReadAt"),
            unreadCount = membership.number("unreadCount"),
            lastReadSeq = membership.number("lastReadSeq"),
            notificationsEnabled = membership["notificationsEnabled"] as? Boolean ?: true,
        )
    }

    private fun messageFromMap(
        raw: Map<String, Any?>,
        fallbackConversationId: String? = null,
    ): RoomV2Message? {
        val messageId = raw.string("messageId") ?: return null
        val roomId = raw.string("roomId") ?: return null
        val authorIdentityId = raw.string("authorIdentityId") ?: return null
        val authorDisplayName = raw.string("authorDisplayName") ?: return null
        val attachment = (raw["attachment"] as? Map<*, *>)?.let(::attachmentFromMap)
        return RoomV2Message(
            messageId = messageId,
            roomId = roomId,
            authorIdentityId = authorIdentityId,
            authorDisplayName = authorDisplayName,
            authorMode = enumOrDefault(raw.string("authorMode"), RoomV2IdentityMode.REAL),
            text = raw["text"]?.toString().orEmpty(),
            sentAt = raw.number("sentAt"),
            seq = raw.number("seq"),
            kind = enumOrDefault(raw.string("kind"), RoomV2MessageKind.TEXT),
            removed = raw["removed"] as? Boolean ?: false,
            conversationId = raw.string("conversationId") ?: fallbackConversationId,
            replyToMessageId = raw.string("replyToMessageId"),
            attachment = attachment,
        )
    }

    private fun attachmentFromMap(raw: Map<*, *>): RoomV2Attachment? {
        val attachmentId = raw.string("attachmentId") ?: return null
        val mimeType = raw.string("mimeType") ?: return null
        return RoomV2Attachment(
            attachmentId = attachmentId,
            mimeType = mimeType,
            sizeBytes = raw.number("sizeBytes"),
            fileName = raw.string("fileName").orEmpty(),
            durationMs = raw.number("durationMs"),
        )
    }

    private fun <T> valueFlow(
        query: Query,
        mapper: (DataSnapshot) -> T,
    ): Flow<T> = callbackFlow {
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

    private suspend fun <T> catching(block: suspend () -> T): ZibeResult<T> = try {
        ZibeResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: RoomV2Exception) {
        ZibeResult.Failure(failure)
    } catch (failure: FirebaseFunctionsException) {
        ZibeResult.Failure(mapFunctionsFailure(failure))
    } catch (failure: Exception) {
        ZibeResult.Failure(RoomV2Exception(RoomV2ErrorCode.INTERNAL, failure))
    }

    private fun mapFunctionsFailure(failure: FirebaseFunctionsException): RoomV2Exception {
        val roomCode = (failure.details as? Map<*, *>)
            ?.get(ROOM_CODE_DETAIL_KEY)
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { raw ->
                runCatching { RoomV2ErrorCode.valueOf(raw) }.getOrNull()
            }

        val code = roomCode ?: when (failure.code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> RoomV2ErrorCode.UNAUTHENTICATED
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> RoomV2ErrorCode.PERMISSION_DENIED
            FirebaseFunctionsException.Code.NOT_FOUND -> RoomV2ErrorCode.NOT_FOUND
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> RoomV2ErrorCode.INVALID_INPUT
            FirebaseFunctionsException.Code.ALREADY_EXISTS,
            FirebaseFunctionsException.Code.ABORTED,
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> RoomV2ErrorCode.CONFLICT
            FirebaseFunctionsException.Code.UNAVAILABLE,
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> RoomV2ErrorCode.OFFLINE
            else -> RoomV2ErrorCode.INTERNAL
        }
        return RoomV2Exception(code, failure)
    }

    private fun safeId(value: String): String {
        val id = value.trim()
        if (
            id.isEmpty() ||
            id.length > MAX_FIREBASE_KEY ||
            id.any { it in ".#$[]/" || it.code < 32 }
        ) {
            throw RoomV2Exception(RoomV2ErrorCode.INVALID_INPUT)
        }
        return id
    }

    private fun contractFailure(message: String): RoomV2Exception =
        RoomV2Exception(RoomV2ErrorCode.INTERNAL, IllegalStateException(message))

    private inline fun <reified E : Enum<E>> enumOrDefault(value: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: fallback

    private fun DataSnapshot.string(): String? =
        getValue(String::class.java)?.trim()?.takeIf { it.isNotEmpty() }

    private fun DataSnapshot.long(default: Long): Long = (value as? Number)?.toLong() ?: default

    private fun DataSnapshot.int(default: Int): Int = (value as? Number)?.toInt() ?: default

    private fun Map<*, *>.string(key: String): String? =
        this[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private fun Map<*, *>.number(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L

    private fun roleOrder(role: RoomV2Role): Int = when (role) {
        RoomV2Role.OWNER -> 0
        RoomV2Role.MODERATOR -> 1
        RoomV2Role.MEMBER -> 2
    }

    private fun normalizeSearch(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFKD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .replace("\\s+".toRegex(), " ")

    companion object {
        private const val ROOT = "RoomsV2"
        private const val ROOM_CODE_DETAIL_KEY = "roomV2Code"
        private const val MAX_FIREBASE_KEY = 120
        private const val MAX_DIRECTORY_LIMIT = 200
        private const val MAX_DIRECTORY_PAGE = 200
        private const val DIRECTORY_PAGE_SIZE = 50
        private const val MAX_MESSAGE_LIMIT = 100
    }
}
