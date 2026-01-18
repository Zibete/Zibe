package com.zibete.proyecto1.ui.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
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

                val birthDate = u.birthDate.trim()

                _uiState.update { it ->
                    it.copy(
                        isLoading = false,
                        name = u.name,
                        description = u.description,
                        birthDate = birthDate,
                        age = birthDate.takeIf { it.isNotBlank() }?.let { ageCalculator(it) },
                        photoUrl = u.photoUrl,
                        photoPreviewUri = null,
                        deletePhoto = false,
                        saveEnabled = false,
                        hasBirthDate = birthDate.isNotBlank(),
                        originalName = u.name,
                        originalDescription = u.description,
                        originalBirthDate = birthDate,
                        originalPhotoUrl = u.photoUrl
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false) }
                onError(
                    UiText.StringRes(
                        resId = R.string.err_zibe_prefix,
                        args = listOf(e.message.orEmpty())
                    )
                )
            }
        }
    }

    private fun recomputeSaveEnabled(state: EditProfileUiState): Boolean {
        val changedName = state.name.trim() != state.originalName.trim()
        val changedDesc = state.description.trim() != state.originalDescription.trim()
        val changedBirth = state.birthDate.trim() != state.originalBirthDate.trim()

        val pickedNewPhoto = state.photoPreviewUri != null
        val requestedDelete = state.deletePhoto

        val changedPhoto = pickedNewPhoto || requestedDelete

        return changedName || changedDesc || changedBirth || changedPhoto
    }

    fun onNameChanged(value: String) {
        _uiState.update { s ->
            val ns = s.copy(name = value)
            ns.copy(saveEnabled = recomputeSaveEnabled(ns))
        }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { s ->
            val ns = s.copy(description = value)
            ns.copy(saveEnabled = recomputeSaveEnabled(ns))
        }
    }

    fun onBirthDateChanged(birthDate: String) {
        val trimmed = birthDate.trim()
        val age = trimmed.takeIf { it.isNotBlank() }?.let { ageCalculator(it) }
        _uiState.update { s ->
            val ns = s.copy(birthDate = birthDate, age = age)
            ns.copy(saveEnabled = recomputeSaveEnabled(ns))
        }
    }

    fun onPhotoSelected(uri: Uri) {
        _uiState.update { s ->
            val ns = s.copy(photoPreviewUri = uri, deletePhoto = false)
            ns.copy(saveEnabled = recomputeSaveEnabled(ns))
        }
    }

    fun onPhotoDeletedSetDefault() {
        _uiState.update { s ->
            val ns = s.copy(photoPreviewUri = null, deletePhoto = true)
            ns.copy(saveEnabled = recomputeSaveEnabled(ns))
        }
    }

    fun resolveProfilePhotoToLoad(): Any? =
        when {
            _uiState.value.photoPreviewUri != null -> _uiState.value.photoPreviewUri
            _uiState.value.deletePhoto -> DEFAULT_PROFILE_PHOTO_URL
            !_uiState.value.photoUrl.isNullOrBlank() -> _uiState.value.photoUrl
            else -> DEFAULT_PROFILE_PHOTO_URL
        }

    suspend fun isEditProfileWelcomeShown(): Boolean {
        return userPreferencesProvider.isEditProfileWelcomeShown()
    }

    fun markEditProfileWelcomeShown() {
        viewModelScope.launch {
            userPreferencesActions.setEditProfileWelcomeShown(true)
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
                _uiState.update { s ->
                    val newPhotoUrl = when {
                        s.deletePhoto -> null
                        else -> s.photoUrl
                    }

                    s.copy(
                        isLoading = false,
                        isSaving = false,
                        photoPreviewUri = null,
                        deletePhoto = false,
                        saveEnabled = false,
                        originalName = s.name.trim(),
                        originalDescription = s.description.trim(),
                        originalBirthDate = s.birthDate.trim(),
                        originalPhotoUrl = newPhotoUrl
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

    private fun validateInputs(
        name: String,
        birthDate: String
    ): Boolean {

        fun warn(uiText: UiText): Boolean {
            onError(uiText)
            return false
        }

        if (name.isBlank()) return warn(UiText.StringRes(R.string.signup_err_name_required))
        if (birthDate.isBlank()) return warn(UiText.StringRes(R.string.signup_err_birthdate_required))
        if (!isAdult(birthDate)) return warn(UiText.StringRes(R.string.err_under_age))

        return true
    }


}
