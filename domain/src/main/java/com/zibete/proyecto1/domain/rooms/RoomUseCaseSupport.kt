package com.zibete.proyecto1.domain.rooms

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.model.RoomIdentity
import com.zibete.proyecto1.model.RoomIdentityType
import kotlin.coroutines.cancellation.CancellationException

class RoomContractException(message: String) : IllegalStateException(message)
class RoomMembershipNotFoundException(roomKey: String) :
    IllegalStateException("Authenticated user is not a member of room $roomKey")

class RoomSessionNotFoundException : IllegalStateException("No active room session")
class RoomValidationException(val issues: List<RoomValidationIssue>) :
    IllegalArgumentException("Invalid room command: $issues")

enum class RoomFailureReason {
    INVALID_IDENTITY,
    PERMISSION,
    CONNECTION,
    ROOM_NOT_FOUND,
    SESSION_INVALID,
    UNEXPECTED
}

class RoomOperationException(
    val reason: RoomFailureReason,
    cause: Throwable? = null
) : IllegalStateException("Room operation failed: $reason", cause)

object RoomCreationPolicy {
    fun validateCreator(identity: RoomIdentity): RoomValidationIssue? =
        if (identity.type == RoomIdentityType.PUBLIC) null
        else RoomValidationIssue(
            field = RoomValidationField.PUBLIC_IDENTITY,
            error = RoomValidationError.INVALID_VALUE
        )
}

internal fun ZibeResult.Failure.rethrowCancellation(): ZibeResult.Failure {
    if (exception is CancellationException) throw exception
    return this
}

internal data class ValidatedIdentity(
    val identity: RoomIdentity?,
    val issues: List<RoomValidationIssue>
)

internal fun validateIdentity(identity: RoomIdentity): ValidatedIdentity {
    val validation = when (identity.type) {
        RoomIdentityType.ANONYMOUS -> RoomValidator.validateAlias(identity.displayName)
        RoomIdentityType.PUBLIC -> RoomValidator.validatePublicIdentity(identity.displayName)
    }
    return when (validation) {
        is RoomTextValidation.Invalid -> ValidatedIdentity(null, listOf(validation.issue))
        is RoomTextValidation.Valid -> ValidatedIdentity(
            identity.copy(displayName = validation.value),
            emptyList()
        )
    }
}

internal fun validTextOrIssue(
    validation: RoomTextValidation,
    issues: MutableList<RoomValidationIssue>
): String? = when (validation) {
    is RoomTextValidation.Invalid -> {
        issues += validation.issue
        null
    }
    is RoomTextValidation.Valid -> validation.value
}
