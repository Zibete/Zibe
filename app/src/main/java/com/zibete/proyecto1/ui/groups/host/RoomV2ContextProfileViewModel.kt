package com.zibete.proyecto1.ui.groups.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.roomsv2.RoomV2ContextProfile
import com.zibete.proyecto1.domain.roomsv2.RoomV2ErrorCode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Exception
import com.zibete.proyecto1.domain.roomsv2.RoomV2Identity
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomsV2ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoomV2ContextProfileUiState(
    val profile: RoomV2ContextProfile? = null,
    val loadingIdentityId: String? = null,
    val error: UiText? = null,
)

@HiltViewModel
class RoomV2ContextProfileViewModel @Inject constructor(
    private val repository: RoomsV2ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomV2ContextProfileUiState())
    val uiState: StateFlow<RoomV2ContextProfileUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun open(roomId: String, identity: RoomV2Identity) {
        if (identity.mode != RoomV2IdentityMode.REAL) return
        if (_uiState.value.loadingIdentityId == identity.identityId) return
        if (roomId.isBlank()) {
            _uiState.value = RoomV2ContextProfileUiState(
                error = UiText.StringRes(R.string.rooms_v2_error_not_found),
            )
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = RoomV2ContextProfileUiState(
                loadingIdentityId = identity.identityId,
            )
            when (val result = repository.loadContextProfile(roomId, identity.identityId)) {
                is ZibeResult.Success -> {
                    val profile = result.data
                    _uiState.value = if (
                        profile != null && profile.identityId == identity.identityId
                    ) {
                        RoomV2ContextProfileUiState(profile = profile)
                    } else {
                        RoomV2ContextProfileUiState(
                            error = UiText.StringRes(R.string.rooms_v2_error_unexpected),
                        )
                    }
                }

                is ZibeResult.Failure -> {
                    _uiState.value = RoomV2ContextProfileUiState(
                        error = result.exception.toRoomText(),
                    )
                }
            }
        }
    }

    fun dismiss() {
        loadJob?.cancel()
        loadJob = null
        _uiState.value = RoomV2ContextProfileUiState()
    }

    private fun Throwable.toRoomText(): UiText {
        val resource = when ((this as? RoomV2Exception)?.code) {
            RoomV2ErrorCode.UNAUTHENTICATED,
            RoomV2ErrorCode.PERMISSION_DENIED -> R.string.rooms_v2_error_permission
            RoomV2ErrorCode.NOT_FOUND -> R.string.rooms_v2_error_not_found
            RoomV2ErrorCode.INVALID_INPUT -> R.string.rooms_v2_error_invalid_input
            RoomV2ErrorCode.OFFLINE -> R.string.rooms_v2_error_offline
            RoomV2ErrorCode.CONFLICT -> R.string.rooms_v2_error_conflict
            RoomV2ErrorCode.ALIAS_TAKEN,
            RoomV2ErrorCode.ROOM_NAME_TAKEN,
            RoomV2ErrorCode.ROOM_CLOSED,
            RoomV2ErrorCode.IDENTITY_CHANGE_REQUIRED,
            RoomV2ErrorCode.OWNER_ACTION_REQUIRED,
            RoomV2ErrorCode.INTERNAL,
            null -> R.string.rooms_v2_error_unexpected
        }
        return UiText.StringRes(resource)
    }
}
