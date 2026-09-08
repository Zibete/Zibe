package com.zibete.proyecto1.domain.roomsv2

import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class RoomsV2UnreadSummary(
    val publicUnread: Int = 0,
    val privateUnread: Int = 0,
) {
    val totalUnread: Int
        get() = (publicUnread.toLong() + privateUnread.toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
}

class ObserveRoomsV2UnreadSummaryUseCase @Inject constructor(
    private val roomsRepository: RoomsV2Repository,
    private val chatRepository: RoomsV2ChatRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<RoomsV2UnreadSummary> =
        roomsRepository.observeMemberships()
            .flatMapLatest { memberships ->
                if (memberships.isEmpty()) {
                    flowOf(RoomsV2UnreadSummary())
                } else {
                    val publicUnread = memberships
                        .sumOf { it.unreadCount }
                        .toBadgeCount()
                    privateUnreadFlow(memberships)
                        .map { privateUnread ->
                            RoomsV2UnreadSummary(
                                publicUnread = publicUnread,
                                privateUnread = privateUnread.toBadgeCount(),
                            )
                        }
                }
            }
            .distinctUntilChanged()

    private fun privateUnreadFlow(
        memberships: List<RoomV2Membership>,
    ): Flow<Long> = memberships.fold(flowOf(0L)) { totalFlow, membership ->
        val roomUnreadFlow = chatRepository.observeConversations(membership.roomId)
            .map { conversations -> conversations.sumOf { it.unreadCount } }
            .catch { emit(0L) }
        combine(totalFlow, roomUnreadFlow) { total, roomUnread ->
            saturatingAdd(total, roomUnread)
        }
    }

    private fun Long.toBadgeCount(): Int =
        coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    private fun saturatingAdd(left: Long, right: Long): Long {
        val safeLeft = left.coerceAtLeast(0L)
        val safeRight = right.coerceAtLeast(0L)
        return if (Long.MAX_VALUE - safeLeft < safeRight) {
            Long.MAX_VALUE
        } else {
            safeLeft + safeRight
        }
    }
}
