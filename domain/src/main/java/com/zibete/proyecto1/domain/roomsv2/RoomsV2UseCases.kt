package com.zibete.proyecto1.domain.roomsv2

import com.zibete.proyecto1.core.utils.ZibeResult
import java.util.UUID
import javax.inject.Inject

class CreateRoomV2UseCase @Inject constructor(private val repository: RoomsV2Repository) {
    suspend operator fun invoke(
        name: String,
        description: String,
        operationId: String = UUID.randomUUID().toString(),
    ): ZibeResult<RoomV2Membership> {
        RoomsV2Validation.validateRoomName(name)?.let { return invalidInput() }
        RoomsV2Validation.validateDescription(description)?.let { return invalidInput() }
        return repository.createRoom(
            CreateRoomV2Request(
                name = RoomsV2Validation.collapseSpaces(name),
                description = description.trim(),
                operationId = operationId,
            ),
        )
    }
}

class JoinRoomV2UseCase @Inject constructor(private val repository: RoomsV2Repository) {
    suspend operator fun invoke(request: JoinRoomV2Request): ZibeResult<RoomV2Membership> {
        if (request.roomId.isBlank()) return invalidInput()
        if (request.mode == RoomV2IdentityMode.ANONYMOUS) {
            val alias = request.alias.orEmpty()
            RoomsV2Validation.validateAlias(alias)?.let { return invalidInput() }
            return repository.joinRoom(request.copy(alias = RoomsV2Validation.collapseSpaces(alias)))
        }
        return repository.joinRoom(request.copy(alias = null))
    }
}

class SendRoomV2TextUseCase @Inject constructor(private val repository: RoomsV2ChatRepository) {
    suspend operator fun invoke(thread: RoomV2Thread, text: String, clientMessageId: String): ZibeResult<RoomV2Message> {
        if (thread.roomId.isBlank()) return invalidInput()
        RoomsV2Validation.validateMessage(text)?.let { return invalidInput() }
        RoomsV2Validation.validateClientMessageId(clientMessageId)?.let {
            return invalidInput()
        }
        return repository.sendText(thread, text.trim(), clientMessageId.trim())
    }
}

private fun invalidInput() = ZibeResult.Failure(RoomV2Exception(RoomV2ErrorCode.INVALID_INPUT))
