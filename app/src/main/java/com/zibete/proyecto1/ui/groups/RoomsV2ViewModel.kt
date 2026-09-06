package com.zibete.proyecto1.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.domain.roomsv2.CreateRoomV2UseCase
import com.zibete.proyecto1.domain.roomsv2.JoinRoomV2Request
import com.zibete.proyecto1.domain.roomsv2.JoinRoomV2UseCase
import com.zibete.proyecto1.domain.roomsv2.RoomV2ErrorCode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Exception
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Membership
import com.zibete.proyecto1.domain.roomsv2.RoomV2Status
import com.zibete.proyecto1.domain.roomsv2.RoomsV2Repository
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RoomsV2ViewModel @Inject constructor(
    private val repository: RoomsV2Repository,
    private val localRepositoryProvider: LocalRepositoryProvider,
    private val createRoom: CreateRoomV2UseCase,
    private val joinRoom: JoinRoomV2UseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RoomsV2UiState(publicIdentityName = localRepositoryProvider.myUserName),
    )
    val uiState: StateFlow<RoomsV2UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RoomsV2UiEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<RoomsV2UiEvent> = _events.asSharedFlow()

    init {
        observeRooms()
    }

    private fun observeRooms() {
        viewModelScope.launch {
            combine(
                repository.observeDirectory(limit = 200),
                repository.observeMemberships(),
            ) { directory, memberships ->
                val byRoom = memberships.associateBy { it.roomId }
                directory.map { room -> RoomV2ListItem(room, byRoom[room.roomId]) }
            }
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    onLoadFailure(failure)
                }
                .collect { rooms ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            rooms = rooms,
                            visibleRooms = filterRooms(rooms, state.searchQuery),
                            error = null,
                        )
                    }
                }
        }
    }

    fun loadRooms() {
        if (_uiState.value.isRefreshing) return
        refreshRooms()
    }

    fun refreshRooms() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val result = repository.refreshDirectory()) {
                is ZibeResult.Success -> _uiState.update {
                    it.copy(isRefreshing = false, error = null)
                }
                is ZibeResult.Failure -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                    emitSnack(result.exception.toRoomText(), ZibeSnackType.ERROR)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                visibleRooms = filterRooms(state.rooms, query),
            )
        }
    }

    fun onCreateRoomRequested() {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(
                sheet = RoomsV2Sheet.Create,
                roomName = "",
                roomDescription = "",
                alias = "",
                identityMode = RoomV2IdentityMode.REAL,
                roomNameError = null,
                roomDescriptionError = null,
                identityError = null,
                publicIdentityName = localRepositoryProvider.myUserName,
            )
        }
    }

    fun onRoomSelected(item: RoomV2ListItem) {
        if (_uiState.value.isSubmitting) return
        if (item.isMember) {
            emit(RoomsV2UiEvent.NavigateToRoom(item.room.roomId))
            return
        }
        if (item.room.status == RoomV2Status.CLOSED) {
            emitSnack(UiText.StringRes(R.string.rooms_v2_error_closed), ZibeSnackType.WARNING)
            return
        }
        _uiState.update {
            it.copy(
                sheet = RoomsV2Sheet.Join(item.room),
                identityMode = RoomV2IdentityMode.REAL,
                alias = "",
                identityError = null,
                publicIdentityName = localRepositoryProvider.myUserName,
            )
        }
    }

    fun dismissSheet() {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(
                sheet = null,
                roomNameError = null,
                roomDescriptionError = null,
                identityError = null,
            )
        }
    }

    fun onIdentityModeSelected(mode: RoomV2IdentityMode) {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(
                identityMode = mode,
                identityError = null,
                alias = if (mode == RoomV2IdentityMode.REAL) "" else it.alias,
            )
        }
    }

    fun onAliasChanged(alias: String) {
        _uiState.update { it.copy(alias = alias, identityError = null) }
    }

    fun onRoomNameChanged(name: String) {
        _uiState.update { it.copy(roomName = name, roomNameError = null) }
    }

    fun onRoomDescriptionChanged(description: String) {
        _uiState.update { it.copy(roomDescription = description, roomDescriptionError = null) }
    }

    fun submitSheet() {
        if (_uiState.value.isSubmitting) return
        when (val sheet = _uiState.value.sheet) {
            RoomsV2Sheet.Create -> submitCreate()
            is RoomsV2Sheet.Join -> submitJoin(sheet.room.roomId)
            null -> Unit
        }
    }

    private fun submitCreate() {
        val state = _uiState.value
        launchMembershipOperation {
            createRoom(state.roomName, state.roomDescription)
        }
    }

    private fun submitJoin(roomId: String) {
        val state = _uiState.value
        launchMembershipOperation {
            joinRoom(
                JoinRoomV2Request(
                    roomId = roomId,
                    mode = state.identityMode,
                    alias = state.alias.takeIf {
                        state.identityMode == RoomV2IdentityMode.ANONYMOUS
                    },
                ),
            )
        }
    }

    private fun launchMembershipOperation(
        operation: suspend () -> ZibeResult<RoomV2Membership>,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    roomNameError = null,
                    roomDescriptionError = null,
                    identityError = null,
                )
            }
            val result = try {
                operation()
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isSubmitting = false) }
                throw cancelled
            }
            when (result) {
                is ZibeResult.Success -> {
                    val membership = result.data
                    if (membership == null) {
                        finishFailure(RoomV2Exception(RoomV2ErrorCode.INTERNAL))
                    } else {
                        _uiState.update {
                            it.copy(isSubmitting = false, sheet = null)
                        }
                        emit(RoomsV2UiEvent.NavigateToRoom(membership.roomId))
                    }
                }
                is ZibeResult.Failure -> finishFailure(result.exception)
            }
        }
    }

    private fun finishFailure(failure: Throwable) {
        val code = (failure as? RoomV2Exception)?.code
        _uiState.update { state ->
            when (code) {
                RoomV2ErrorCode.ROOM_NAME_TAKEN -> state.copy(
                    isSubmitting = false,
                    roomNameError = UiText.StringRes(R.string.rooms_v2_error_room_name_taken),
                )
                RoomV2ErrorCode.ALIAS_TAKEN -> state.copy(
                    isSubmitting = false,
                    identityError = UiText.StringRes(R.string.rooms_v2_error_alias_taken),
                )
                RoomV2ErrorCode.INVALID_INPUT -> when (state.sheet) {
                    RoomsV2Sheet.Create -> state.copy(
                        isSubmitting = false,
                        roomNameError = UiText.StringRes(R.string.rooms_v2_error_invalid_input),
                    )
                    is RoomsV2Sheet.Join -> state.copy(
                        isSubmitting = false,
                        identityError = UiText.StringRes(R.string.rooms_v2_error_invalid_input),
                    )
                    null -> state.copy(isSubmitting = false)
                }
                else -> state.copy(isSubmitting = false)
            }
        }
        if (
            code != RoomV2ErrorCode.ROOM_NAME_TAKEN &&
            code != RoomV2ErrorCode.ALIAS_TAKEN &&
            code != RoomV2ErrorCode.INVALID_INPUT
        ) {
            emitSnack(failure.toRoomText(), ZibeSnackType.ERROR)
        }
    }

    private fun onLoadFailure(failure: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                error = UiText.StringRes(R.string.rooms_v2_error_message),
            )
        }
        if (_uiState.value.rooms.isNotEmpty()) {
            emitSnack(failure.toRoomText(), ZibeSnackType.ERROR)
        }
    }

    private fun filterRooms(
        rooms: List<RoomV2ListItem>,
        query: String,
    ): List<RoomV2ListItem> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return rooms
        return rooms.filter { item ->
            item.room.name.lowercase().contains(normalized) ||
                item.room.description.lowercase().contains(normalized) ||
                item.room.normalizedName.lowercase().contains(normalized)
        }
    }

    private fun Throwable.toRoomText(): UiText {
        val resource = when ((this as? RoomV2Exception)?.code) {
            RoomV2ErrorCode.UNAUTHENTICATED,
            RoomV2ErrorCode.PERMISSION_DENIED -> R.string.rooms_v2_error_permission
            RoomV2ErrorCode.ALIAS_TAKEN -> R.string.rooms_v2_error_alias_taken
            RoomV2ErrorCode.ROOM_NAME_TAKEN -> R.string.rooms_v2_error_room_name_taken
            RoomV2ErrorCode.ROOM_CLOSED -> R.string.rooms_v2_error_closed
            RoomV2ErrorCode.IDENTITY_CHANGE_REQUIRED -> R.string.rooms_v2_error_identity_change
            RoomV2ErrorCode.OWNER_ACTION_REQUIRED -> R.string.rooms_v2_error_owner_action
            RoomV2ErrorCode.NOT_FOUND -> R.string.rooms_v2_error_not_found
            RoomV2ErrorCode.INVALID_INPUT -> R.string.rooms_v2_error_invalid_input
            RoomV2ErrorCode.OFFLINE -> R.string.rooms_v2_error_offline
            RoomV2ErrorCode.CONFLICT -> R.string.rooms_v2_error_conflict
            RoomV2ErrorCode.INTERNAL,
            null -> R.string.rooms_v2_error_unexpected
        }
        return UiText.StringRes(resource)
    }

    private fun emit(event: RoomsV2UiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    private fun emitSnack(text: UiText, type: ZibeSnackType) {
        emit(RoomsV2UiEvent.ShowSnack(text, type))
    }
}
