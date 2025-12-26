package com.zibete.proyecto1.ui.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.Constants.AccountsKeys
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.ERR_UNDER_AGE
import com.zibete.proyecto1.ui.constants.ERR_ZIBE
import com.zibete.proyecto1.ui.constants.MSG_PROFILE_LOAD_ERROR
import com.zibete.proyecto1.ui.constants.MSG_PROFILE_SAVED
import com.zibete.proyecto1.ui.constants.MSG_PROFILE_SAVE_ERROR
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_BIRTHDAY_REQUIRED
import com.zibete.proyecto1.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.utils.TimeUtils.isoToUiDate
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

    private val myUid: String get() = userRepository.myUid

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
                        birthDate = u.birthDate,
                        age = ageCalculator(u.birthDate),
                        photoUrl = u.photoUrl,
                        photoPreviewUri = null,
                        deletePhoto = false,
                        saveEnabled = false,
                        hasBirthDate = u.birthDate.isNotBlank()
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

    fun onBirthDateChanged(birthDate: String) {
        val age = ageCalculator(birthDate)
        _uiState.update { it.copy(birthDate = birthDate, age = age, saveEnabled = true) }
    }

    suspend fun isFirstLoginDone(): Boolean {
        return userPreferencesRepository.isFirstLoginDone()
    }

    fun markFirstLoginAsDone() {
        viewModelScope.launch {
            userPreferencesRepository.setFirstLoginDone(true)
        }
    }

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

            val age = ageCalculator(newBirthDate)

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
                    AccountsKeys.NAME to newName,
                    AccountsKeys.BIRTHDATE to newBirthDate,
                    AccountsKeys.AGE to age,
                    AccountsKeys.DESCRIPTION to newDescription
                )

                if (finalPhotoUrl != null && finalPhotoUrl != originalPhotoUrl) {
                    updates[AccountsKeys.PHOTO_URL] = finalPhotoUrl
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

    val birthDateUi: String
        get() = _uiState.value.birthDate
            .takeIf { it.isNotBlank() }
            ?.let { iso -> isoToUiDate(iso) }
            .orEmpty()

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
