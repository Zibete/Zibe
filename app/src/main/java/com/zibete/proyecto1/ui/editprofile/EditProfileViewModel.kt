package com.zibete.proyecto1.ui.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.core.utils.TimeUtils.isAdult
import com.zibete.proyecto1.core.utils.TimeUtils.isoToUiDate
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.domain.profile.UpdateProfileUseCase
import com.zibete.proyecto1.ui.components.ZibeSnackType
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
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val snackBarManager: SnackBarManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    private val _events = MutableSharedFlow<EditProfileUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditProfileUiEvent> = _events.asSharedFlow()

    fun load() {

        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            runCatching {

                val u = userRepositoryProvider.getMyAccount()

                if (u == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    onError(UiText.StringRes(R.string.msg_profile_load_error))
                    return@runCatching
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = u.name,
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
            }.onFailure {
                onError(
                    UiText.StringRes(
                        resId = R.string.err_zibe_prefix,
                        args = listOf(it.message ?: "")
                    )
                )
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, saveEnabled = true) }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value, saveEnabled = true) }
    }

    fun onBirthDateChanged(birthDate: String) {
        val age = ageCalculator(birthDate)
        _uiState.update { it.copy(birthDate = birthDate, age = age, saveEnabled = true) }
    }

    fun showSkipButton() : Boolean = uiState.value.hasBirthDate

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
            val birthDate = state.birthDate.trim()

            if (!validateInputs(state.name, birthDate)) return@launch
            _uiState.update { it.copy(isLoading = true, isSaving = true) }

            val calculatedAge = ageCalculator(birthDate)

            updateProfileUseCase.execute(
                newName = state.name.trim(),
                newBirthDate = state.birthDate.trim(),
                newDescription = state.description.trim(),
                age = calculatedAge,
                originalPhotoUrl = state.photoUrl,
                photoPreviewUri = state.photoPreviewUri,
                shouldDeletePhoto = state.deletePhoto
            ).onFailure { e ->
                handleError(e)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        photoUrl = it.photoUrl,
                        photoPreviewUri = null,
                        deletePhoto = false,
                        saveEnabled = false
                    )
                }

                val uiTextProfileSaved = UiText.StringRes(resId = R.string.msg_profile_saved)

                snackBarManager.show(
                    uiText = uiTextProfileSaved,
                    type = ZibeSnackType.SUCCESS
                )

                onBackToMain()
            }
        }
    }

    val birthDateUi: String
        get() = _uiState.value.birthDate
            .takeIf { it.isNotBlank() }
            ?.let { iso -> isoToUiDate(iso) }
            .orEmpty()

    fun onBackToMain() {
        viewModelScope.launch {
            _events.emit(EditProfileUiEvent.OnBackToMain)
        }
    }

    private fun handleError(e: Throwable) {
        val uiText = getAuthErrorMessage(e)
        onError(uiText)
    }

    fun onError(uiText: UiText) {
        _events.tryEmit(
            EditProfileUiEvent.ShowSnack(
                uiText = uiText,
                type = ZibeSnackType.ERROR
            )
        )
        _uiState.update {
            it.copy(
                isSaving = false,
                isLoading = false
            )
        }
    }

    private suspend fun validateInputs(
        name: String,
        birthDate: String
    ): Boolean {

        suspend fun warn(uiText: UiText): Boolean {
            onError(uiText)
            return false
        }

        if (name.isBlank()) return warn(UiText.StringRes(R.string.signup_err_name_required))
        if (birthDate.isBlank()) return warn(UiText.StringRes(R.string.signup_err_birthdate_required))
        if (!isAdult(birthDate)) return warn(UiText.StringRes(R.string.err_under_age))

        return true
    }


}
