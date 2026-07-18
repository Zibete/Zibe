package com.zibete.proyecto1.domain.session

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.domain.rooms.LeaveRoomCommand
import com.zibete.proyecto1.domain.rooms.RoomContractException
import com.zibete.proyecto1.domain.rooms.RoomOperationResult
import com.zibete.proyecto1.domain.rooms.RoomSessionNotFoundException
import com.zibete.proyecto1.domain.rooms.RoomTextValidation
import com.zibete.proyecto1.domain.rooms.RoomValidationException
import com.zibete.proyecto1.domain.rooms.RoomValidator
import javax.inject.Inject
import kotlinx.coroutines.flow.first

interface ExitGroupUseCase {
    suspend fun performExitGroupDataCleanup(
        message: String
    ): ZibeResult<Unit>
}

class DefaultExitGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepositoryProvider,
    private val userPreferencesActions: UserPreferencesActions,
    private val userPreferencesProvider: UserPreferencesProvider
) : ExitGroupUseCase {

    override suspend fun performExitGroupDataCleanup(
        message: String
    ): ZibeResult<Unit> = zibeCatching {
        val groupContext = userPreferencesProvider.groupContextFlow.first()
            ?: throw RoomSessionNotFoundException()
        val eventValidation = RoomValidator.validateEventContent(message)
        if (eventValidation is RoomTextValidation.Invalid) {
            throw RoomValidationException(listOf(eventValidation.issue))
        }
        val roomKey = groupContext.roomKey.ifBlank { groupContext.groupName }
        val outcome = groupRepository.leaveRoom(
            LeaveRoomCommand(
                roomKey = roomKey,
                userName = groupContext.userName,
                userType = groupContext.userType,
                eventContent = (eventValidation as RoomTextValidation.Valid).value
            )
        ).getOrThrow()
        if (outcome !is RoomOperationResult.Left || outcome.roomKey != roomKey) {
            throw RoomContractException("Unexpected leaveRoom outcome: $outcome")
        }
        userPreferencesActions.resetRoomSession()
    }
}
