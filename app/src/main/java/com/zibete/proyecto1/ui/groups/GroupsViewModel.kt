package com.zibete.proyecto1.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_GROUP
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.domain.roomsv2.CreateRoomV2UseCase
import com.zibete.proyecto1.domain.roomsv2.JoinRoomV2Request
import com.zibete.proyecto1.domain.roomsv2.JoinRoomV2UseCase
import com.zibete.proyecto1.domain.roomsv2.RoomV2DirectoryItem
import com.zibete.proyecto1.domain.roomsv2.RoomV2ErrorCode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Exception
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Membership
import com.zibete.proyecto1.domain.roomsv2.RoomV2Status
import com.zibete.proyecto1.domain.roomsv2.RoomsV2Repository
import com.zibete.proyecto1.model.Groups
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
class GroupsViewModel @Inject constructor(
    private val roomsRepository: RoomsV2Repository,
    private val localRepositoryProvider: LocalRepositoryProvider,
    private val createRoomUseCase: CreateRoomV2UseCase,
    private val joinRoomUseCase: JoinRoomV2UseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState(isLoading = true))
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GroupsUiEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<GroupsUiEvent> = _events.asSharedFlow()

    private var allGroups: List<Groups> = emptyList()
    private var roomsByName: Map<String, RoomV2DirectoryItem> = emptyMap()
    private var membershipsByRoomId: Map<String, RoomV2Membership> = emptyMap()
    private var searchQuery: String = ""
    private var observingRooms = false
    private var membershipRequestInFlight = false

    init {
        observeRooms()
    }

    fun loadGroups() {
        observeRooms()
    }

    fun refreshGroups() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val result = roomsRepository.refreshDirectory()) {
                is ZibeResult.Success -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                    if (!observingRooms) observeRooms()
                }

                is ZibeResult.Failure -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                    emitEvent(
                        GroupsUiEvent.ShowSnack(
                            result.exception.toRoomText(),
                            ZibeSnackType.ERROR,
                        )
                    )
                }
            }
        }
    }

    private fun observeRooms() {
        if (observingRooms) return
        observingRooms = true
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            combine(
                roomsRepository.observeDirectory(limit = 200),
                roomsRepository.observeMemberships(),
            ) { rooms, memberships -> rooms to memberships }
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    observingRooms = false
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false)
                    }
                    emitEvent(
                        GroupsUiEvent.ShowSnack(
                            failure.toRoomText(),
                            ZibeSnackType.ERROR,
                        )
                    )
                }
                .collect { (rooms, memberships) ->
                    membershipsByRoomId = memberships
                        .filter { it.identity.active }
                        .associateBy { it.roomId }
                    roomsByName = rooms.associateBy { it.name }

                    allGroups = rooms
                        .asSequence()
                        .filter { room ->
                            room.status == RoomV2Status.OPEN ||
                                membershipsByRoomId.containsKey(room.roomId)
                        }
                        .map(::toLegacyPresentationGroup)
                        .sortedBy { it.name.lowercase() }
                        .toList()

                    updateVisibleGroups(isLoading = false, isRefreshing = false)
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        updateVisibleGroups()
    }

    fun onGroupSelected(groupName: String) {
        val room = roomsByName[groupName]
        if (room == null) {
            emitEvent(
                GroupsUiEvent.ShowSnack(
                    UiText.StringRes(R.string.rooms_v2_error_not_found),
                    ZibeSnackType.WARNING,
                )
            )
            return
        }

        if (membershipsByRoomId[room.roomId]?.identity?.active == true) {
            emitEvent(GroupsUiEvent.NavigateToRoom(room.roomId))
            return
        }

        if (room.status == RoomV2Status.CLOSED) {
            emitEvent(
                GroupsUiEvent.ShowSnack(
                    UiText.StringRes(R.string.rooms_v2_error_closed),
                    ZibeSnackType.WARNING,
                )
            )
            return
        }

        emitEvent(GroupsUiEvent.PromptJoinRoom(room.name))
    }

    fun onJoinWithProfileRequested(roomName: String) {
        if (!hasPublicProfile()) return
        performJoin(
            roomName = roomName,
            mode = RoomV2IdentityMode.REAL,
            alias = null,
        )
    }

    fun onJoinAnonymouslyRequested(roomName: String, alias: String) {
        performJoin(
            roomName = roomName,
            mode = RoomV2IdentityMode.ANONYMOUS,
            alias = alias,
        )
    }

    private fun performJoin(
        roomName: String,
        mode: RoomV2IdentityMode,
        alias: String?,
    ) {
        val room = roomsByName[roomName]
        if (room == null) {
            emitEvent(
                GroupsUiEvent.ShowSnack(
                    UiText.StringRes(R.string.rooms_v2_error_not_found),
                    ZibeSnackType.WARNING,
                )
            )
            return
        }

        launchMembershipOperation(alias = alias) {
            joinRoomUseCase(
                JoinRoomV2Request(
                    roomId = room.roomId,
                    mode = mode,
                    alias = alias,
                )
            )
        }
    }

    fun onCreateNewGroupClicked(groupName: String, groupData: String) {
        if (!hasPublicProfile()) return
        launchMembershipOperation(roomName = groupName) {
            createRoomUseCase(groupName, groupData)
        }
    }

    private fun launchMembershipOperation(
        roomName: String? = null,
        alias: String? = null,
        operation: suspend () -> ZibeResult<RoomV2Membership>,
    ) {
        if (membershipRequestInFlight) return
        membershipRequestInFlight = true

        viewModelScope.launch {
            try {
                when (val result = operation()) {
                    is ZibeResult.Success -> {
                        val membership = result.data
                        if (membership == null) {
                            handleMembershipFailure(RoomV2Exception(RoomV2ErrorCode.INTERNAL))
                        } else {
                            emitEvent(GroupsUiEvent.NavigateToRoom(membership.roomId))
                        }
                    }

                    is ZibeResult.Failure -> {
                        handleMembershipFailure(
                            failure = result.exception,
                            roomName = roomName,
                            alias = alias,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                membershipRequestInFlight = false
            }
        }
    }

    private fun handleMembershipFailure(
        failure: Throwable,
        roomName: String? = null,
        alias: String? = null,
    ) {
        when ((failure as? RoomV2Exception)?.code) {
            RoomV2ErrorCode.ROOM_NAME_TAKEN -> {
                emitEvent(GroupsUiEvent.GroupNameInUse(roomName.orEmpty()))
            }

            RoomV2ErrorCode.ALIAS_TAKEN -> {
                emitEvent(GroupsUiEvent.NickInUse(alias.orEmpty()))
            }

            else -> {
                emitEvent(
                    GroupsUiEvent.ShowSnack(
                        failure.toRoomText(),
                        ZibeSnackType.ERROR,
                    )
                )
            }
        }
    }

    private fun hasPublicProfile(): Boolean {
        if (localRepositoryProvider.myUserName.isNotBlank()) return true
        emitEvent(
            GroupsUiEvent.ShowSnack(
                UiText.StringRes(R.string.rooms_public_identity_missing),
                ZibeSnackType.WARNING,
            )
        )
        return false
    }

    fun onError(uiText: UiText) {
        _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        emitEvent(GroupsUiEvent.ShowSnack(uiText, ZibeSnackType.ERROR))
    }

    private fun updateVisibleGroups(
        isLoading: Boolean? = null,
        isRefreshing: Boolean? = null,
    ) {
        val query = searchQuery.trim().lowercase()
        val filtered = if (query.isBlank()) {
            allGroups
        } else {
            allGroups.filter { group ->
                group.name.lowercase().contains(query) ||
                    group.description.lowercase().contains(query)
            }
        }

        _uiState.update { state ->
            state.copy(
                isLoading = isLoading ?: state.isLoading,
                isRefreshing = isRefreshing ?: state.isRefreshing,
                groups = allGroups,
                filteredGroups = filtered,
                searchQuery = searchQuery,
            )
        }
    }

    private fun toLegacyPresentationGroup(room: RoomV2DirectoryItem): Groups = Groups(
        name = room.name,
        description = room.description,
        type = PUBLIC_GROUP,
        users = room.memberCount,
        createdAt = room.updatedAt,
    )

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

    private fun emitEvent(event: GroupsUiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    fun myDisplayName(): String = localRepositoryProvider.myUserName
    fun myPhotoUrl(): String = localRepositoryProvider.myProfilePhotoUrl
}
