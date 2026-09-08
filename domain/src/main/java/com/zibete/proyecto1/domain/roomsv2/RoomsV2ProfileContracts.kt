package com.zibete.proyecto1.domain.roomsv2

import com.zibete.proyecto1.core.utils.ZibeResult

/**
 * Sanitized public profile resolved from a contextual RoomsV2 identity.
 *
 * This contract is intentionally UID-free and does not expose direct Storage URLs.
 */
data class RoomV2ContextProfile(
    val identityId: String,
    val displayName: String,
    val age: Int,
    val description: String,
)

interface RoomsV2ProfileRepository {
    suspend fun loadContextProfile(
        roomId: String,
        identityId: String,
    ): ZibeResult<RoomV2ContextProfile>
}
