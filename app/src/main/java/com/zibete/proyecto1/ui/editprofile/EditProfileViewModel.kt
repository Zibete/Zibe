package com.zibete.proyecto1.ui.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.core.constants.ERR_UNDER_AGE
import com.zibete.proyecto1.core.constants.ERR_ZIBE
import com.zibete.proyecto1.core.constants.MSG_PROFILE_LOAD_ERROR
import com.zibete.proyecto1.core.constants.MSG_PROFILE_SAVED
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_BIRTHDAY_REQUIRED
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.domain.profile.UpdateProfileUseCase
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.core.utils.TimeUtils.isoToUiDate
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
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
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions,
    private val userRepositoryProvider: UserRepositoryProvider,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    private val myUid: String get() = userRepositoryProvider.myUid

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    private val _events = MutableSharedFlow<EditProfileUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditProfileUiEvent> = _events.asSharedFlow()

    fun load() {

        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            runCatching {

                val u = userRepositoryProvider.getAccount(myUid)

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

//    suspend fun isFirstLoginDone(): Boolean {
//        return userPreferencesProvider.isFirstLoginDone()
//    }
//
//    fun markFirstLoginAsDone() {
//        viewModelScope.launch { userPreferencesActions.setFirstLoginDone(true) }
//    }

    suspend fun isEditProfileWelcomeShown(): Boolean {
        return userPreferencesProvider.isEditProfileWelcomeShown()
    }

    fun markEditProfileWelcomeShown() {
        viewModelScope.launch {
            userPreferencesActions.setEditProfileWelcomeShown(true)
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
            val newBirthDate = state.birthDate.trim()

            if (newBirthDate.isBlank()) {
                onError(SIGNUP_ERR_BIRTHDAY_REQUIRED)
                return@launch
            }

            val calculatedAge = ageCalculator(newBirthDate)

            if (calculatedAge < 18) {
                onError(ERR_UNDER_AGE)
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }

            updateProfileUseCase.execute(
                newName = state.displayName.trim(),
                newBirthDate = state.birthDate.trim(),
                newDescription = state.description.trim(),
                age = calculatedAge,
                originalPhotoUrl = state.photoUrl,
                photoPreviewUri = state.photoPreviewUri,
                shouldDeletePhoto = state.deletePhoto
            ).onSuccess {
                _uiState.update { it.copy(
                    isSaving = false,
                    photoUrl = it.photoUrl,
                    photoPreviewUri = null,
                    deletePhoto = false,
                    saveEnabled = false
                ) }
                onBackToMain(MSG_PROFILE_SAVED)
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false) }
                onError(getAuthErrorMessage(e))
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
