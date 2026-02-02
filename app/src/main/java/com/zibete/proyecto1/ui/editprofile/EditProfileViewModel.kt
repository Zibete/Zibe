package com.zibete.proyecto1.ui.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.core.utils.TimeUtils.isAdult
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

    val snackBarEvents = snackBarManager.events

//    fun hasPendingChanges(): Boolean = _uiState.value.hasPendingChanges
//    fun hasBirthDate(): Boolean = _uiState.value.hasBirthDate

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
                    showSnack(UiText.StringRes(R.string.msg_profile_load_error))
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
                        hasPendingChanges = false,
                        hasBirthDate = birthDate.isNotBlank(),
                        birthDateError = null,
                        nameError = null,
                        originalName = u.name,
                        originalDescription = u.description,
                        originalBirthDate = birthDate,
                        originalPhotoUrl = u.photoUrl
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false) }
                showSnack(
                    e.message.toUiText(
                        R.string.err_zibe_prefix,
                        R.string.err_zibe
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
            ns.copy(hasPendingChanges = recomputeSaveEnabled(ns))
        }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { s ->
            val ns = s.copy(description = value)
            ns.copy(hasPendingChanges = recomputeSaveEnabled(ns))
        }
    }

    fun onBirthDateChanged(birthDate: String) {
        _uiState.update { it.copy(birthDateError = null, age = null) }

        if (birthDate.isBlank()) {
            _uiState.update { it.copy(birthDateError = UiText.StringRes(R.string.signup_err_birthdate_required)) }
            return
        }

        val age = birthDate.trim().takeIf { it.isNotBlank() }?.let { ageCalculator(it) }
        _uiState.update {
            val newState = it.copy(
                birthDate = birthDate,
                age = age
            )
            newState.copy(hasPendingChanges = recomputeSaveEnabled(newState))

        }

        if (!isAdult(birthDate)) {
            _uiState.update { it.copy(birthDateError = UiText.StringRes(R.string.err_under_age)) }
        }
    }

    fun onPhotoSelected(uri: Uri) {
        _uiState.update { s ->
            val ns = s.copy(photoPreviewUri = uri, deletePhoto = false)
            ns.copy(hasPendingChanges = recomputeSaveEnabled(ns))
        }
    }

    fun onPhotoDeletedSetDefault() {
        _uiState.update { s ->
            val ns = s.copy(photoPreviewUri = null, deletePhoto = true)
            ns.copy(hasPendingChanges = recomputeSaveEnabled(ns))
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
            }.onSuccess { finalPhotoUrl ->
                _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        isSaving = false,
                        photoUrl = finalPhotoUrl,
                        photoPreviewUri = null,
                        deletePhoto = false,
                        hasPendingChanges = false,
                        birthDateError = null,
                        nameError = null,
                        originalName = s.name.trim(),
                        originalDescription = s.description.trim(),
                        originalBirthDate = s.birthDate.trim(),
                        originalPhotoUrl = finalPhotoUrl
                    )
                }

                snackBarManager.show(
                    uiText = UiText.StringRes(resId = R.string.msg_profile_saved),
                    type = ZibeSnackType.SUCCESS
                )

                onBackToMain()
            }
        }
    }

    fun onBackRequest() {
        val state = _uiState.value
        when {
            state.isSaving -> Unit
            state.hasPendingChanges -> _uiState.update { it.copy(showDiscardDialog = true) }
            !state.hasBirthDate -> _uiState.update { it.copy(birthDateError = UiText.StringRes(R.string.signup_err_birthdate_required)) }
            else -> onBackToMain()
        }
    }

    fun onBackToMain() {
        viewModelScope.launch {
            _events.emit(EditProfileUiEvent.NavigateBack)
        }
    }

    private fun handleError(e: Throwable) {
        val uiText = getAuthErrorMessage(e)
        showSnack(uiText)
    }

    fun showSnack(
        uiText: UiText,
        snackType: ZibeSnackType = ZibeSnackType.ERROR
    ) {
        snackBarManager.show(
            uiText = uiText,
            type = snackType
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

        when {
            name.isBlank() -> {
                val nameError = UiText.StringRes(R.string.signup_err_name_required)
                _uiState.update { it.copy(nameError = nameError) }
                return false
            }

            birthDate.isBlank() -> {
                val birthDateError = UiText.StringRes(R.string.signup_err_birthdate_required)
                _uiState.update { it.copy(birthDateError = birthDateError) }
                return false
            }

            !isAdult(birthDate) -> {
                val birthDateError = UiText.StringRes(R.string.err_under_age)
                showSnack(birthDateError, ZibeSnackType.WARNING)
                return false
            }

            else -> return true
        }
    }
}
