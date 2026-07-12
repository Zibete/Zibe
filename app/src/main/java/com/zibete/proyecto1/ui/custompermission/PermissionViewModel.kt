package com.zibete.proyecto1.ui.custompermission

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.core.utils.AppChecksProvider
import com.zibete.proyecto1.notifications.NotificationPermissionStateProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val appChecksProvider: AppChecksProvider,
    private val notificationPermissionStateProvider: NotificationPermissionStateProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PermissionUiState(
            mode = savedStateHandle[KEY_MODE] ?: PermissionEducationMode.COMBINED,
            requestInFlight = savedStateHandle[KEY_REQUEST_IN_FLIGHT]
        )
    )
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PermissionUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PermissionUiEvent> = _events.asSharedFlow()

    fun configure(mode: PermissionEducationMode) {
        if (savedStateHandle.contains(KEY_MODE)) return
        savedStateHandle[KEY_MODE] = mode
        _uiState.update { it.copy(mode = mode) }
    }

    fun onContinueClicked(shouldShowLocationRationale: Boolean) {
        if (_uiState.value.requestInFlight != null) return
        if (_uiState.value.mode == PermissionEducationMode.NOTIFICATION_ONLY ||
            appChecksProvider.hasLocationPermission()
        ) {
            continueAfterLocation()
        } else if (shouldShowLocationRationale) {
            _uiState.update { it.copy(showLocationRationaleDialog = true) }
        } else {
            emitRequest(PermissionRequest.LOCATION)
        }
    }

    fun onLocationRationaleAccepted() {
        _uiState.update { it.copy(showLocationRationaleDialog = false) }
        emitRequest(PermissionRequest.LOCATION)
    }

    fun onLocationRationaleDismissed() {
        _uiState.update { it.copy(showLocationRationaleDialog = false) }
    }

    fun onLocationResult(granted: Boolean) {
        clearInFlight()
        if (granted) continueAfterLocation()
        else _uiState.update { it.copy(showLocationDeniedDialog = true) }
    }

    fun onNotificationResult(_granted: Boolean) {
        clearInFlight()
        emit(PermissionUiEvent.Completed)
    }

    fun onLocationDeniedAcknowledged() {
        _uiState.update { it.copy(showLocationDeniedDialog = false) }
        emit(PermissionUiEvent.LocationDenied)
    }

    private fun continueAfterLocation() {
        val status = notificationPermissionStateProvider.snapshot()
        if (status.shouldRequestDuringOnboarding) {
            notificationPermissionStateProvider.markRequested()
            emitRequest(PermissionRequest.NOTIFICATIONS)
        } else {
            emit(PermissionUiEvent.Completed)
        }
    }

    private fun emitRequest(request: PermissionRequest) {
        savedStateHandle[KEY_REQUEST_IN_FLIGHT] = request
        _uiState.update { it.copy(requestInFlight = request) }
        emit(PermissionUiEvent.RequestPermission(request))
    }

    private fun clearInFlight() {
        savedStateHandle[KEY_REQUEST_IN_FLIGHT] = null
        _uiState.update { it.copy(requestInFlight = null) }
    }

    private fun emit(event: PermissionUiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    private companion object {
        const val KEY_MODE = "permission_mode"
        const val KEY_REQUEST_IN_FLIGHT = "permission_request_in_flight"
    }
}
