package com.zibete.proyecto1.ui.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserRepository.AccountKeys
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.Constants.MSG_PROFILE_LOAD_ERROR
import com.zibete.proyecto1.ui.constants.Constants.MSG_PROFILE_SAVED
import com.zibete.proyecto1.ui.constants.Constants.MSG_PROFILE_SAVE_ERROR
import com.zibete.proyecto1.ui.constants.ERR_UNDER_AGE
import com.zibete.proyecto1.ui.constants.ERR_ZIBE
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_BIRTHDAY_REQUIRED
import com.zibete.proyecto1.utils.Utils.calcAge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userSessionManager: UserSessionManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val myUid = userRepository.myUid

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    private val _events = MutableSharedFlow<EditProfileUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditProfileUiEvent> = _events.asSharedFlow()

    fun load() {

        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            runCatching {

                val u = userRepository.getAccount(myUid)

                if (u == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    onError(MSG_PROFILE_LOAD_ERROR)
                    return@runCatching
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        displayName = u.name,
                        description = u.description,
                        birthDate = u.birthDay,
                        age = calcAge(u.birthDay),
                        photoUrl = u.photoUrl,
                        photoPreviewUri = null,
                        deletePhoto = false,
                        saveEnabled = false,
                        hasBirthDate = u.birthDay.isNotBlank()
                    )
                }
            }.onFailure { it ->
                _uiState.update { it.copy(isLoading = false) }
                onError(it.message ?: ERR_ZIBE)
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(displayName = value, saveEnabled = true) }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value, saveEnabled = true) }
    }

    fun onBirthDateChanged(birthDay: String) {
        val age = computeAgeFromString(birthDay)
        _uiState.update { it.copy(birthDate = birthDay, age = age, saveEnabled = true) }
    }

    private fun computeAgeFromString(birthDay: String): Int {
        return calcAge(birthDay)
    }

    fun isFirstLoginDone () : Boolean { return userPreferencesRepository.firstLoginDone }

    fun onFirstLoginDone () { userPreferencesRepository.firstLoginDone = true }

    fun onPhotoSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                photoPreviewUri = uri,
                deletePhoto = false,
                saveEnabled = true
            )
        }
    }

    fun onPhotoDeletedSetDefault() {
        _uiState.update {
            it.copy(
                photoPreviewUri = null,
                deletePhoto = true,
                saveEnabled = true
            )
        }
    }



    fun onSaveClicked() {
        viewModelScope.launch {
            val state = _uiState.value
            val newName = state.displayName.trim()
            val newDescription = state.description.trim()
            val newBirthDate = state.birthDate.trim()

            if (newBirthDate.isBlank()) {
                onError(SIGNUP_ERR_BIRTHDAY_REQUIRED)
                return@launch
            }

            val age = computeAgeFromString(newBirthDate)

            if (age < 18) {
                onError(ERR_UNDER_AGE)
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }

            runCatching {
                // Resolver foto final
                val originalPhotoUrl = state.photoUrl
                var finalPhotoUrl = originalPhotoUrl

                // 1) Subir/borrar foto
                when {
                    state.deletePhoto -> {
                        userRepository.deleteProfilePhoto()
                        finalPhotoUrl = DEFAULT_PROFILE_PHOTO_URL
                    }

                    state.photoPreviewUri != null -> {
                        userRepository.putProfilePhotoInStorage(state.photoPreviewUri)
                        finalPhotoUrl = userRepository.getProfilePhotoUrl()
                    }
                }

                // 2) Update Realtime DB (/Cuentas)
                val updates = mutableMapOf<String, Any?>(
                    AccountKeys.NAME to newName,
                    AccountKeys.BIRTHDAY to newBirthDate,
                    AccountKeys.AGE to age,
                    AccountKeys.DESCRIPTION to newDescription
                )

                if (finalPhotoUrl != null && finalPhotoUrl != originalPhotoUrl) {
                    updates[AccountKeys.PHOTO_URL] = finalPhotoUrl
                }

                userRepository.updateUserFields(updates)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        photoUrl = finalPhotoUrl,
                        photoPreviewUri = null,
                        deletePhoto = false,
                        saveEnabled = false
                    )
                }

                // 3) Update FirebaseAuth user profile
                userSessionManager.updateAuthProfile(
                    newName,
                    finalPhotoUrl
                )

                onBackToMain(MSG_PROFILE_SAVED)

            }.onFailure {
                _uiState.update { it.copy(isSaving = false) }
                onError(MSG_PROFILE_SAVE_ERROR)
            }
        }
    }

    fun onBackToMain(message: String = "") {
        viewModelScope.launch {
            _events.emit(EditProfileUiEvent.OnBackToMain(message))
        }
    }

    fun onError(message: String) {
        _events.tryEmit(EditProfileUiEvent.ShowMessage(
            message = message,
            type = ZibeSnackType.ERROR))
    }
}
