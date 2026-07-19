package com.zibete.proyecto1.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.domain.rooms.CreateRoomCommand
import com.zibete.proyecto1.domain.rooms.CreateRoomUseCase
import com.zibete.proyecto1.domain.rooms.JoinRoomCommand
import com.zibete.proyecto1.domain.rooms.JoinRoomUseCase
import com.zibete.proyecto1.domain.rooms.RoomOperationResult
import com.zibete.proyecto1.domain.rooms.RoomFailureReason
import com.zibete.proyecto1.domain.rooms.RoomOperationException
import com.zibete.proyecto1.domain.rooms.RoomValidationField
import com.zibete.proyecto1.domain.rooms.RoomValidationIssue
import com.zibete.proyecto1.domain.rooms.RoomValidator
import com.zibete.proyecto1.domain.rooms.SwitchRoomCommand
import com.zibete.proyecto1.domain.rooms.SwitchRoomUseCase
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.RoomIdentity
import com.zibete.proyecto1.model.RoomIdentityType
import com.zibete.proyecto1.model.RoomSession
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepositoryProvider,
    private val localRepositoryProvider: LocalRepositoryProvider,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val createRoomUseCase: CreateRoomUseCase,
    private val joinRoomUseCase: JoinRoomUseCase,
    private val switchRoomUseCase: SwitchRoomUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        GroupsUiState(
            publicIdentityName = localRepositoryProvider.myUserName,
            publicIdentityPhotoUrl = localRepositoryProvider.myProfilePhotoUrl
        )
    )
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GroupsUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<GroupsUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            userPreferencesProvider.groupContextFlow.collect { context ->
                _uiState.update { it.copy(activeSession = context?.toRoomSession()) }
            }
        }
    }

    fun loadRooms() = fetchRooms(isRefresh = _uiState.value.rooms.isNotEmpty())

    fun refreshRooms() = fetchRooms(isRefresh = true)

    private fun fetchRooms(isRefresh: Boolean) {
        val current = _uiState.value
        if (current.isLoading || current.isRefreshing) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh && it.rooms.isEmpty(),
                    isRefreshing = isRefresh,
                    error = null
                )
            }
            when (val result = groupRepository.loadRooms()) {
                is ZibeResult.Success -> {
                    val rooms = result.data
                    if (rooms == null) {
                        handleLoadFailure()
                        return@launch
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            rooms = rooms,
                            visibleRooms = filterRooms(rooms, state.searchQuery),
                            error = null
                        )
                    }
                }

                is ZibeResult.Failure -> handleLoadFailure()
            }
        }
    }

    private suspend fun handleLoadFailure() {
        val error = UiText.StringRes(R.string.rooms_error_message)
        val hadContent = _uiState.value.rooms.isNotEmpty()
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                error = if (hadContent) null else error
            )
        }
        if (hadContent) emitSnack(error, ZibeSnackType.ERROR)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                visibleRooms = filterRooms(it.rooms, query)
            )
        }
    }

    fun onCreateRoomRequested() {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(
                sheet = RoomsSheet.Create,
                pendingSwitch = null,
                identityType = RoomIdentityType.PUBLIC,
                publicIdentityName = localRepositoryProvider.myUserName,
                publicIdentityPhotoUrl = localRepositoryProvider.myProfilePhotoUrl,
                alias = "",
                roomName = "",
                roomDescription = "",
                roomNameError = null,
                roomDescriptionError = null,
                identityError = null
            )
        }
    }

    fun onRoomSelected(room: Groups) {
        if (_uiState.value.isSubmitting) return
        val roomKey = room.resolvedRoomKey()
        if (_uiState.value.activeSession?.roomKey == roomKey) {
            viewModelScope.launch { _events.emit(GroupsUiEvent.NavigateToGroupHost) }
            return
        }
        _uiState.update {
            it.copy(
                sheet = RoomsSheet.Join(room),
                pendingSwitch = null,
                identityType = RoomIdentityType.PUBLIC,
                publicIdentityName = localRepositoryProvider.myUserName,
                publicIdentityPhotoUrl = localRepositoryProvider.myProfilePhotoUrl,
                alias = "",
                roomNameError = null,
                roomDescriptionError = null,
                identityError = null
            )
        }
    }

    fun dismissSheet() {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(
                sheet = null,
                pendingSwitch = null,
                roomNameError = null,
                roomDescriptionError = null,
                identityError = null
            )
        }
    }

    fun onIdentitySelected(type: RoomIdentityType) {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(identityType = type, identityError = null) }
    }

    fun onAliasChanged(alias: String) {
        _uiState.update { it.copy(alias = alias, identityError = null) }
    }

    fun onRoomNameChanged(name: String) {
        _uiState.update { it.copy(roomName = name, roomNameError = null) }
    }

    fun onRoomDescriptionChanged(description: String) {
        _uiState.update {
            it.copy(roomDescription = description, roomDescriptionError = null)
        }
    }

    fun submitSheet(joinEventContent: String, leaveEventContent: String) {
        val state = _uiState.value
        if (state.isSubmitting) return
        when (val sheet = state.sheet) {
            RoomsSheet.Create -> submitCreate(joinEventContent, leaveEventContent)
            is RoomsSheet.Join -> submitJoin(sheet.room, joinEventContent)
            null -> Unit
        }
    }

    private fun submitCreate(joinEventContent: String, leaveEventContent: String) {
        val state = _uiState.value
        val command = CreateRoomCommand(
            roomName = state.roomName,
            description = state.roomDescription,
            identity = publicIdentity(state),
            eventContent = joinEventContent,
            leaveEventContent = leaveEventContent
        )
        launchOperation(
            pendingAction = { current -> PendingRoomSwitch.Create(current, command) }
        ) { createRoomUseCase.execute(command) }
    }

    private fun submitJoin(
        room: Groups,
        joinEventContent: String
    ) {
        val state = _uiState.value
        val command = JoinRoomCommand(
            roomKey = room.resolvedRoomKey(),
            displayName = room.resolvedDisplayName(),
            identity = selectedIdentity(state),
            eventContent = joinEventContent
        )
        launchOperation(
            pendingAction = { current -> PendingRoomSwitch.Join(current, command) }
        ) { joinRoomUseCase.execute(command) }
    }

    fun confirmSwitch(leaveEventContent: String) {
        val pending = _uiState.value.pendingSwitch ?: return
        if (_uiState.value.isSubmitting) return
        launchOperation(pendingAction = null) {
            when (pending) {
                is PendingRoomSwitch.Create -> createRoomUseCase.execute(
                    pending.command.copy(
                        replaceActiveRoom = true,
                        previousSession = pending.currentSession,
                        leaveEventContent = leaveEventContent
                    )
                )

                is PendingRoomSwitch.Join -> switchRoomUseCase.execute(
                    SwitchRoomCommand(
                        previousSession = pending.currentSession,
                        target = pending.command,
                        leaveEventContent = leaveEventContent
                    )
                )
            }
        }
    }

    fun dismissSwitch() {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(pendingSwitch = null) }
    }

    private fun launchOperation(
        pendingAction: ((RoomSession) -> PendingRoomSwitch)?,
        operation: suspend () -> ZibeResult<RoomOperationResult>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, pendingSwitch = null) }
            try {
                handleOperation(operation(), pendingAction)
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(isSubmitting = false) }
                throw cancellation
            }
        }
    }

    private suspend fun handleOperation(
        result: ZibeResult<RoomOperationResult>,
        pendingAction: ((RoomSession) -> PendingRoomSwitch)?
    ) {
        when (result) {
            is ZibeResult.Failure -> {
                _uiState.update { it.copy(isSubmitting = false) }
                emitSnack(result.exception.toRoomErrorText(), ZibeSnackType.ERROR)
            }

            is ZibeResult.Success -> when (val outcome = result.data) {
                is RoomOperationResult.Created,
                is RoomOperationResult.Joined,
                is RoomOperationResult.Switched,
                is RoomOperationResult.AlreadyActive -> finishAndNavigate()

                is RoomOperationResult.NameInUse -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            roomNameError = UiText.StringRes(
                                R.string.group_name_in_use,
                                listOf(outcome.roomName)
                            )
                        )
                    }
                }

                is RoomOperationResult.AliasInUse -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            identityError = UiText.StringRes(
                                R.string.group_nick_in_use,
                                listOf(outcome.alias)
                            )
                        )
                    }
                }

                is RoomOperationResult.SwitchRequired -> {
                    val pending = pendingAction?.invoke(outcome.currentSession)
                    if (pending == null) {
                        _uiState.update { it.copy(isSubmitting = false) }
                        emitSnack(
                            UiText.StringRes(R.string.rooms_action_error),
                            ZibeSnackType.ERROR
                        )
                    } else {
                        _uiState.update {
                            it.copy(isSubmitting = false, pendingSwitch = pending)
                        }
                    }
                }

                is RoomOperationResult.ValidationFailed -> {
                    applyValidationIssues(outcome.issues)
                }

                is RoomOperationResult.Left, null -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    emitSnack(
                        UiText.StringRes(R.string.rooms_action_error),
                        ZibeSnackType.ERROR
                    )
                }
            }
        }
    }

    private suspend fun finishAndNavigate() {
        _uiState.update {
            it.copy(
                isSubmitting = false,
                sheet = null,
                pendingSwitch = null,
                roomNameError = null,
                roomDescriptionError = null,
                identityError = null
            )
        }
        _events.emit(GroupsUiEvent.NavigateToGroupHost)
    }

    private fun applyValidationIssues(issues: List<RoomValidationIssue>) {
        var nameError: UiText? = null
        var descriptionError: UiText? = null
        var identityError: UiText? = null
        issues.forEach { issue ->
            when (issue.field) {
                RoomValidationField.ROOM_NAME ->
                    nameError = UiText.StringRes(R.string.rooms_invalid_name)
                RoomValidationField.DESCRIPTION ->
                    descriptionError = UiText.StringRes(R.string.rooms_invalid_description)
                RoomValidationField.ALIAS ->
                    identityError = UiText.StringRes(R.string.rooms_invalid_alias)
                RoomValidationField.PUBLIC_IDENTITY ->
                    identityError = UiText.StringRes(R.string.rooms_public_identity_missing)
                else -> Unit
            }
        }
        _uiState.update {
            it.copy(
                isSubmitting = false,
                roomNameError = nameError,
                roomDescriptionError = descriptionError,
                identityError = identityError
            )
        }
    }

    private fun selectedIdentity(state: GroupsUiState): RoomIdentity =
        when (state.identityType) {
            RoomIdentityType.PUBLIC -> RoomIdentity(
                displayName = state.publicIdentityName,
                type = RoomIdentityType.PUBLIC,
                photoUrl = state.publicIdentityPhotoUrl
            )
            RoomIdentityType.ANONYMOUS -> RoomIdentity(
                displayName = state.alias,
                type = RoomIdentityType.ANONYMOUS
            )
        }

    private fun publicIdentity(state: GroupsUiState): RoomIdentity = RoomIdentity(
        displayName = state.publicIdentityName,
        type = RoomIdentityType.PUBLIC,
        photoUrl = state.publicIdentityPhotoUrl
    )

    private fun Throwable.toRoomErrorText(): UiText {
        val reason = (this as? RoomOperationException)?.reason
        val stringRes = when (reason) {
            RoomFailureReason.INVALID_IDENTITY -> R.string.rooms_public_creator_required
            RoomFailureReason.PERMISSION -> R.string.rooms_error_permission
            RoomFailureReason.CONNECTION -> R.string.rooms_error_connection
            RoomFailureReason.ROOM_NOT_FOUND -> R.string.rooms_error_not_found
            RoomFailureReason.SESSION_INVALID -> R.string.rooms_error_session
            RoomFailureReason.UNEXPECTED, null -> R.string.rooms_action_error
        }
        return UiText.StringRes(stringRes)
    }

    private fun filterRooms(rooms: List<Groups>, query: String): List<Groups> {
        val normalizedQuery = RoomValidator.normalizeIndexKey(query)
        if (normalizedQuery.isBlank()) return rooms
        return rooms.filter { room ->
            RoomValidator.normalizeIndexKey(room.resolvedDisplayName())
                .contains(normalizedQuery) ||
                RoomValidator.normalizeIndexKey(room.description).contains(normalizedQuery)
        }
    }

    private suspend fun emitSnack(uiText: UiText, type: ZibeSnackType) {
        _events.emit(GroupsUiEvent.ShowSnack(uiText, type))
    }
}
