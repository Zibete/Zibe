package com.zibete.proyecto1.domain.rooms

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.model.RoomSession
import javax.inject.Inject
import kotlinx.coroutines.flow.first

interface CreateRoomUseCase {
    suspend fun execute(command: CreateRoomCommand): ZibeResult<RoomOperationResult>
}

class DefaultCreateRoomUseCase @Inject constructor(
    private val groupRepository: GroupRepositoryProvider,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions
) : CreateRoomUseCase {
    override suspend fun execute(command: CreateRoomCommand): ZibeResult<RoomOperationResult> {
        val issues = mutableListOf<RoomValidationIssue>()
        val roomName = validTextOrIssue(RoomValidator.validateRoomName(command.roomName), issues)
        val description = validTextOrIssue(
            RoomValidator.validateDescription(command.description),
            issues
        )
        val eventContent = validTextOrIssue(
            RoomValidator.validateEventContent(command.eventContent),
            issues
        )
        val currentSession = userPreferencesProvider.groupContextFlow.first()?.toRoomSession()
        val leaveEventContent = if (currentSession != null && command.replaceActiveRoom) {
            validTextOrIssue(
                RoomValidator.validateEventContent(command.leaveEventContent),
                issues
            )
        } else {
            null
        }
        val creatorIdentityIssue = RoomCreationPolicy.validateCreator(command.identity)
        val identity = if (creatorIdentityIssue == null) {
            validateIdentity(command.identity)
        } else {
            ValidatedIdentity(identity = null, issues = listOf(creatorIdentityIssue))
        }
        issues += identity.issues
        if (issues.isNotEmpty()) {
            return ZibeResult.Success(RoomOperationResult.ValidationFailed(issues))
        }

        val validatedCommand = command.copy(
            roomName = checkNotNull(roomName),
            description = checkNotNull(description),
            identity = checkNotNull(identity.identity),
            eventContent = checkNotNull(eventContent),
            previousSession = currentSession,
            leaveEventContent = leaveEventContent.orEmpty()
        )
        if (currentSession != null && !command.replaceActiveRoom) {
            return ZibeResult.Success(
                RoomOperationResult.SwitchRequired(currentSession, validatedCommand.roomName)
            )
        }
        return when (val remote = groupRepository.createRoom(validatedCommand)) {
            is ZibeResult.Failure -> remote.rethrowCancellation()
            is ZibeResult.Success -> when (val outcome = remote.data) {
                is RoomOperationResult.Created -> persistSession(outcome.session, outcome)
                is RoomOperationResult.NameInUse -> ZibeResult.Success(outcome)
                null -> ZibeResult.Failure(RoomContractException("createRoom returned no outcome"))
                else -> ZibeResult.Failure(
                    RoomContractException("Unexpected createRoom outcome: $outcome")
                )
            }
        }
    }

    private suspend fun persistSession(
        session: RoomSession,
        outcome: RoomOperationResult
    ): ZibeResult<RoomOperationResult> = zibeCatching {
        userPreferencesActions.setRoomSession(session)
        outcome
    }
}

interface JoinRoomUseCase {
    suspend fun execute(command: JoinRoomCommand): ZibeResult<RoomOperationResult>
}

class DefaultJoinRoomUseCase @Inject constructor(
    private val groupRepository: GroupRepositoryProvider,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions
) : JoinRoomUseCase {
    override suspend fun execute(command: JoinRoomCommand): ZibeResult<RoomOperationResult> {
        val validated = validateJoinCommand(command)
        if (validated.issues.isNotEmpty()) {
            return ZibeResult.Success(RoomOperationResult.ValidationFailed(validated.issues))
        }
        val validatedCommand = checkNotNull(validated.command)
        val currentSession = userPreferencesProvider.groupContextFlow.first()?.toRoomSession()
        if (currentSession?.roomKey == validatedCommand.roomKey) {
            return ZibeResult.Success(RoomOperationResult.AlreadyActive(currentSession))
        }
        if (currentSession != null) {
            return ZibeResult.Success(
                RoomOperationResult.SwitchRequired(currentSession, validatedCommand.roomKey)
            )
        }

        return joinRemotely(validatedCommand)
    }

    private suspend fun joinRemotely(command: JoinRoomCommand): ZibeResult<RoomOperationResult> =
        when (val remote = groupRepository.joinRoom(command)) {
            is ZibeResult.Failure -> remote.rethrowCancellation()
            is ZibeResult.Success -> when (val outcome = remote.data) {
                is RoomOperationResult.Joined -> zibeCatching {
                    userPreferencesActions.setRoomSession(outcome.session)
                    outcome
                }
                is RoomOperationResult.AliasInUse -> ZibeResult.Success(outcome)
                null -> ZibeResult.Failure(RoomContractException("joinRoom returned no outcome"))
                else -> ZibeResult.Failure(
                    RoomContractException("Unexpected joinRoom outcome: $outcome")
                )
            }
        }
}

interface SwitchRoomUseCase {
    suspend fun execute(command: SwitchRoomCommand): ZibeResult<RoomOperationResult>
}

class DefaultSwitchRoomUseCase @Inject constructor(
    private val groupRepository: GroupRepositoryProvider,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions
) : SwitchRoomUseCase {
    override suspend fun execute(command: SwitchRoomCommand): ZibeResult<RoomOperationResult> {
        val validated = validateJoinCommand(command.target)
        val issues = validated.issues.toMutableList()
        val leaveEvent = validTextOrIssue(
            RoomValidator.validateEventContent(command.leaveEventContent),
            issues
        )
        if (issues.isNotEmpty()) {
            return ZibeResult.Success(RoomOperationResult.ValidationFailed(issues))
        }

        val target = checkNotNull(validated.command)
        val currentSession = userPreferencesProvider.groupContextFlow.first()?.toRoomSession()
        if (currentSession == null) return joinWithoutActiveSession(target)
        if (currentSession.roomKey == target.roomKey) {
            return ZibeResult.Success(RoomOperationResult.AlreadyActive(currentSession))
        }

        val authoritativeCommand = command.copy(
            previousSession = currentSession,
            target = target,
            leaveEventContent = checkNotNull(leaveEvent)
        )
        return when (val remote = groupRepository.switchRoom(authoritativeCommand)) {
            is ZibeResult.Failure -> remote.rethrowCancellation()
            is ZibeResult.Success -> when (val outcome = remote.data) {
                is RoomOperationResult.Switched -> zibeCatching {
                    userPreferencesActions.setRoomSession(outcome.session)
                    outcome
                }
                is RoomOperationResult.AliasInUse -> ZibeResult.Success(outcome)
                null -> ZibeResult.Failure(RoomContractException("switchRoom returned no outcome"))
                else -> ZibeResult.Failure(
                    RoomContractException("Unexpected switchRoom outcome: $outcome")
                )
            }
        }
    }

    private suspend fun joinWithoutActiveSession(
        target: JoinRoomCommand
    ): ZibeResult<RoomOperationResult> = when (val remote = groupRepository.joinRoom(target)) {
        is ZibeResult.Failure -> remote.rethrowCancellation()
        is ZibeResult.Success -> when (val outcome = remote.data) {
            is RoomOperationResult.Joined -> zibeCatching {
                userPreferencesActions.setRoomSession(outcome.session)
                outcome
            }
            is RoomOperationResult.AliasInUse -> ZibeResult.Success(outcome)
            null -> ZibeResult.Failure(RoomContractException("joinRoom returned no outcome"))
            else -> ZibeResult.Failure(
                RoomContractException("Unexpected joinRoom outcome: $outcome")
            )
        }
    }
}

interface MarkRoomReadUseCase {
    suspend fun execute(command: MarkRoomReadCommand): ZibeResult<MarkRoomReadResult>
}

class DefaultMarkRoomReadUseCase @Inject constructor(
    private val groupRepository: GroupRepositoryProvider
) : MarkRoomReadUseCase {
    override suspend fun execute(command: MarkRoomReadCommand): ZibeResult<MarkRoomReadResult> {
        if (!command.isChatVisible) return ZibeResult.Success(MarkRoomReadResult.SkippedNotVisible)

        val issues = mutableListOf<RoomValidationIssue>()
        val roomKey = validTextOrIssue(RoomValidator.validateRoomKey(command.roomKey), issues)
        if (command.lastReadAt <= 0L) {
            issues += RoomValidationIssue(
                RoomValidationField.LAST_READ_AT,
                RoomValidationError.INVALID_VALUE
            )
        }
        if (issues.isNotEmpty()) {
            return ZibeResult.Success(MarkRoomReadResult.ValidationFailed(issues))
        }

        val validatedRoomKey = checkNotNull(roomKey)
        return when (
            val result = groupRepository.markRoomAsRead(validatedRoomKey, command.lastReadAt)
        ) {
            is ZibeResult.Failure -> result.rethrowCancellation()
            is ZibeResult.Success -> ZibeResult.Success(
                MarkRoomReadResult.Marked(validatedRoomKey, command.lastReadAt)
            )
        }
    }
}

interface ResumeRoomSessionUseCase {
    suspend fun execute(roomKey: String): ZibeResult<RoomSession>
}

class DefaultResumeRoomSessionUseCase @Inject constructor(
    private val groupRepository: GroupRepositoryProvider,
    private val userPreferencesActions: UserPreferencesActions
) : ResumeRoomSessionUseCase {
    override suspend fun execute(roomKey: String): ZibeResult<RoomSession> {
        val validation = RoomValidator.validateRoomKey(roomKey)
        if (validation is RoomTextValidation.Invalid) {
            return ZibeResult.Failure(RoomValidationException(listOf(validation.issue)))
        }
        val validatedRoomKey = (validation as RoomTextValidation.Valid).value
        return when (val remote = groupRepository.resolveRoomSession(validatedRoomKey)) {
            is ZibeResult.Failure -> remote.rethrowCancellation()
            is ZibeResult.Success -> {
                val session = remote.data
                    ?: return ZibeResult.Failure(
                        RoomMembershipNotFoundException(validatedRoomKey)
                    )
                if (session.roomKey != validatedRoomKey) {
                    return ZibeResult.Failure(
                        RoomContractException("Resolved session does not match requested room")
                    )
                }
                zibeCatching {
                    userPreferencesActions.setRoomSession(session)
                    session
                }
            }
        }
    }
}

private data class ValidatedJoinCommand(
    val command: JoinRoomCommand?,
    val issues: List<RoomValidationIssue>
)

private fun validateJoinCommand(command: JoinRoomCommand): ValidatedJoinCommand {
    val issues = mutableListOf<RoomValidationIssue>()
    val roomKey = validTextOrIssue(RoomValidator.validateRoomKey(command.roomKey), issues)
    val displayName = validTextOrIssue(
        RoomValidator.validateRoomName(command.displayName),
        issues
    )
    val eventContent = validTextOrIssue(
        RoomValidator.validateEventContent(command.eventContent),
        issues
    )
    val identity = validateIdentity(command.identity)
    issues += identity.issues
    if (issues.isNotEmpty()) return ValidatedJoinCommand(null, issues)

    return ValidatedJoinCommand(
        command.copy(
            roomKey = checkNotNull(roomKey),
            displayName = checkNotNull(displayName),
            identity = checkNotNull(identity.identity),
            eventContent = checkNotNull(eventContent)
        ),
        emptyList()
    )
}
