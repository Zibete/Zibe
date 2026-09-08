package com.zibete.proyecto1.data.roomsv2

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.roomsv2.RoomV2ContextProfile
import com.zibete.proyecto1.domain.roomsv2.RoomV2ErrorCode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Exception
import com.zibete.proyecto1.domain.roomsv2.RoomsV2ProfileRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseRoomsV2ProfileRepository(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : RoomsV2ProfileRepository {

    override suspend fun loadContextProfile(
        roomId: String,
        identityId: String,
    ): ZibeResult<RoomV2ContextProfile> = catching {
        requireUid()
        val safeRoomId = safeId(roomId)
        val safeIdentityId = safeId(identityId)
        val raw = functions.getHttpsCallable(CALLABLE)
            .call(
                mapOf(
                    "roomId" to safeRoomId,
                    "identityId" to safeIdentityId,
                )
            )
            .await()
            .data as? Map<*, *>
            ?: throw contractFailure("$CALLABLE returned an invalid payload")

        val profile = raw["profile"] as? Map<*, *>
            ?: throw contractFailure("$CALLABLE returned no profile")
        val returnedIdentityId = profile.string("identityId")
            ?: throw contractFailure("Profile is missing identityId")
        if (returnedIdentityId != safeIdentityId) {
            throw contractFailure("Profile identity does not match request")
        }

        val displayName = profile.string("displayName")
            ?.take(MAX_DISPLAY_NAME)
            ?: throw contractFailure("Profile is missing displayName")
        val age = (profile["age"] as? Number)
            ?.toInt()
            ?.takeIf { it in MIN_PUBLIC_AGE..MAX_PUBLIC_AGE }
            ?: 0
        val description = profile["description"]
            ?.toString()
            ?.trim()
            ?.take(MAX_DESCRIPTION)
            .orEmpty()

        RoomV2ContextProfile(
            identityId = returnedIdentityId,
            displayName = displayName,
            age = age,
            description = description,
        )
    }

    private fun requireUid(): String = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        ?: throw RoomV2Exception(RoomV2ErrorCode.UNAUTHENTICATED)

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
            ?.uppercase()
            ?.takeIf { it.isNotEmpty() }
            ?.let { raw -> runCatching { RoomV2ErrorCode.valueOf(raw) }.getOrNull() }

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

    private fun contractFailure(message: String): RoomV2Exception =
        RoomV2Exception(RoomV2ErrorCode.INTERNAL, IllegalStateException(message))

    private fun Map<*, *>.string(key: String): String? =
        this[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val CALLABLE = "resolve_room_identity_profile_v2"
        const val ROOM_CODE_DETAIL_KEY = "roomV2Code"
        const val MAX_FIREBASE_KEY = 120
        const val MAX_DISPLAY_NAME = 80
        const val MAX_DESCRIPTION = 500
        const val MIN_PUBLIC_AGE = 18
        const val MAX_PUBLIC_AGE = 120
    }
}
