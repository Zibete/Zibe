package com.zibete.proyecto1.domain.roomsv2

import javax.inject.Inject

class CreateRoomV2UseCase @Inject constructor(private val repository: RoomsV2Repository) {
    suspend operator fun invoke(name: String, description: String): Result<RoomV2Membership> {
        RoomsV2Validation.validateRoomName(name)?.let { return Result.failure(IllegalArgumentException(it)) }
        RoomsV2Validation.validateDescription(description)?.let { return Result.failure(IllegalArgumentException(it)) }
        return repository.createRoom(
            CreateRoomV2Request(
                name = RoomsV2Validation.collapseSpaces(name),
                description = description.trim(),
            ),
        )
    }
}

class JoinRoomV2UseCase @Inject constructor(private val repository: RoomsV2Repository) {
    suspend operator fun invoke(request: JoinRoomV2Request): Result<RoomV2Membership> {
        if (request.roomId.isBlank()) return Result.failure(IllegalArgumentException("Sala inválida"))
        if (request.mode == RoomV2IdentityMode.ANONYMOUS) {
            val alias = request.alias.orEmpty()
            RoomsV2Validation.validateAlias(alias)?.let { return Result.failure(IllegalArgumentException(it)) }
            return repository.joinRoom(request.copy(alias = RoomsV2Validation.collapseSpaces(alias)))
        }
        return repository.joinRoom(request.copy(alias = null))
    }
}

class SendRoomV2TextUseCase @Inject constructor(private val repository: RoomsV2Repository) {
    suspend operator fun invoke(roomId: String, text: String, clientMessageId: String): Result<RoomV2Message> {
        if (roomId.isBlank()) return Result.failure(IllegalArgumentException("Sala inválida"))
        RoomsV2Validation.validateMessage(text)?.let { return Result.failure(IllegalArgumentException(it)) }
        RoomsV2Validation.validateClientMessageId(clientMessageId)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        return repository.sendText(roomId, text.trim(), clientMessageId.trim())
    }
}
