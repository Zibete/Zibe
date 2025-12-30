package com.zibete.proyecto1.ui.custompermission

import androidx.lifecycle.ViewModel
import com.zibete.proyecto1.ui.custompermission.di.PermissionRequester
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val requester: PermissionRequester
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    private val _event = MutableStateFlow<PermissionUiEvent?>(null)
    val event: StateFlow<PermissionUiEvent?> = _event.asStateFlow()

    fun consumeEvent() {
        _event.value = null
    }

    fun onStartClicked(shouldShowRationale: Boolean) {
        if (shouldShowRationale) {
            _uiState.value = _uiState.value.copy(showRationaleDialog = true)
        } else {
            requestPermission()
        }
    }

    fun onRationaleAccept() {
        _uiState.value = _uiState.value.copy(showRationaleDialog = false)
        requestPermission()
    }

    fun onRationaleDismiss() {
        _uiState.value = _uiState.value.copy(showRationaleDialog = false)
    }

    fun onDeniedOkClicked() {
        _uiState.value = _uiState.value.copy(showDeniedDialog = false)
        _event.value = PermissionUiEvent.PermissionDenied
    }

    private fun requestPermission() {
        requester.requestLocationPermission { granted ->
            if (granted) {
                _event.value = PermissionUiEvent.PermissionGranted
            } else {
                _uiState.value = _uiState.value.copy(showDeniedDialog = true)
            }
        }
    }
}
