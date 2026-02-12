package com.zibete.proyecto1.ui.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.di.SettingsConfig
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.core.utils.TimeUtils.isAdult
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onFinally
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.domain.profile.UpdateProfileUseCase
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val snackBarManager: SnackBarManager,
    private val config: SettingsConfig,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    private val _events = MutableSharedFlow<EditProfileUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditProfileUiEvent> = _events.asSharedFlow()

    private var validationJob: Job? = null

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            userRepositoryProvider.getMyAccount()
                .onFailure { e ->
                    showSnack(
                        e.message.toUiText(
                            R.string.err_zibe_prefix,
                            R.string.err_zibe
                        )
                    )
                }.onSuccess { u ->

                    if (u == null) {
                        showSnack(UiText.StringRes(R.string.msg_profile_load_error))
                        return@launch
                    }

                    val birthDate = u.birthDate.trim()
                    val isFirstLoginDone = userPreferencesProvider.isFirstLoginDone()
                    val showWelcomeSheet = !userPreferencesProvider.isEditProfileWelcomeShown()

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
                            isValidating = false,
                            birthDateError = null,
                            nameError = null,
                            originalName = u.name,
                            originalDescription = u.description,
                            originalBirthDate = birthDate,
                            originalPhotoUrl = u.photoUrl,
                            showWelcomeSheet = showWelcomeSheet,
                            showSkipButton = !isFirstLoginDone && birthDate.isNotBlank()
                        )
                    }
                }.onFinally {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun onNameChanged(input: String) {
        validationJob?.cancel()
        val trimmed = input.trim()
        _uiState.update { it.copy(name = trimmed, nameError = null, isValidating = true) }

        val job = viewModelScope.launch {
            delay(config.validationDebounce)
            val error = when {
                trimmed.isBlank() -> UiText.StringRes(R.string.signup_err_name_required)
                else -> null
            }
            _uiState.update { s ->
                val ns = s.copy(
                    nameError = error,
                    isValidating = false
                )
                ns.copy(hasPendingChanges = recomputeSaveEnabled(ns))
            }
        }
        validationJob = job
    }

    fun onBirthDateChanged(input: String) {
        validationJob?.cancel()
        val trimmed = input.trim()
        _uiState.update { s ->
            val ns = s.copy(
                birthDate = trimmed,
                birthDateError = null,
                isValidating = trimmed.isNotBlank()
            )
            ns.copy(hasPendingChanges = recomputeSaveEnabled(ns))
        }

        if (trimmed.isBlank()) return

        val job = viewModelScope.launch {
            val age = trimmed.takeIf { it.isNotBlank() }?.let { ageCalculator(it) }
            val error = when {
                !isAdult(trimmed) -> UiText.StringRes(R.string.err_under_age)
                else -> null
            }
            _uiState.update { s ->
                val ns = s.copy(
                    birthDate = input,
                    age = age,
                    birthDateError = error,
                    isValidating = false
                )
                ns.copy(hasPendingChanges = recomputeSaveEnabled(ns))
            }
        }
        validationJob = job
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { s ->
            val ns = s.copy(description = value)
            ns.copy(hasPendingChanges = recomputeSaveEnabled(ns))
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
            val hasPreview = s.photoPreviewUri != null
            val hasRemotePhoto = !s.photoUrl.isNullOrBlank()
            val ns = when {
                hasPreview -> s.copy(photoPreviewUri = null, deletePhoto = false)
                hasRemotePhoto -> s.copy(photoPreviewUri = null, deletePhoto = true)
                else -> s.copy(photoPreviewUri = null, deletePhoto = false)
            }
            ns.copy(hasPendingChanges = recomputeSaveEnabled(ns))
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

    fun resolveProfilePhotoToLoad(): Any? =
        when {
            _uiState.value.photoPreviewUri != null -> _uiState.value.photoPreviewUri
            _uiState.value.deletePhoto -> DEFAULT_PROFILE_PHOTO_URL
            !_uiState.value.photoUrl.isNullOrBlank() -> _uiState.value.photoUrl
            else -> DEFAULT_PROFILE_PHOTO_URL
        }

    fun onWelcomeSheetDismissed() {
        _uiState.update { it.copy(showWelcomeSheet = false) }
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
                showSnack(
                    uiText = getAuthErrorMessage(e)
                )
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
        val s = _uiState.value
        if (s.isLoading || s.isSaving) return

        val name = s.name.trim()
        val birth = s.birthDate.trim()
        val originalName = s.originalName.trim()
        val originalBirth = s.originalBirthDate.trim()

        val originalIncomplete = originalName.isBlank() || originalBirth.isBlank()
        val originalValid = !originalIncomplete && isAdult(originalBirth)

        // 1) Si hubo cambios y el estado original estaba bien, SIEMPRE permitir salir con discard,
        if (s.hasPendingChanges && originalValid) {
            _uiState.update { it.copy(showDiscardDialog = true, pendingNav = PendingNav.BACK) }
            return
        }

        // 2) Si el perfil original estaba incompleto, forzar guardado (no permitir salir).
        if (!originalValid) {
            _uiState.update {
                it.copy(
                    nameError = if (name.isBlank())
                        UiText.StringRes(R.string.signup_err_name_required)
                    else it.nameError,
                    birthDateError = if (birth.isBlank())
                        UiText.StringRes(R.string.signup_err_birthdate_required)
                    else it.birthDateError
                )
            }
            showSnack(
                uiText = UiText.StringRes(R.string.err_save_required),
                snackType = ZibeSnackType.WARNING
            )
            return
        }

        onBackToMain()
    }

    fun onSettingsRequest() {
        val state = _uiState.value
        when {
            state.isSaving -> Unit
            state.hasPendingChanges -> _uiState.update {
                it.copy(showDiscardDialog = true, pendingNav = PendingNav.SETTINGS)
            }

            else -> viewModelScope.launch {
                _events.emit(EditProfileUiEvent.NavigateToSettings)
            }
        }
    }

    fun onDiscardDialogDismiss() {
        _uiState.update { it.copy(showDiscardDialog = false, pendingNav = null) }
    }

    fun onDiscardDialogConfirmExit() {
        val pending = _uiState.value.pendingNav
        _uiState.update { it.copy(showDiscardDialog = false, pendingNav = null) }
        when (pending) {
            PendingNav.SETTINGS -> viewModelScope.launch {
                _events.emit(EditProfileUiEvent.NavigateToSettings)
            }

            PendingNav.BACK, null -> onBackToMain()
        }
    }

    fun onBackToMain() {
        viewModelScope.launch {
            _events.emit(EditProfileUiEvent.NavigateBack)
        }
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
                _uiState.update { it.copy(birthDateError = birthDateError) }
                showSnack(birthDateError, ZibeSnackType.WARNING)
                return false
            }

            else -> return true
        }
    }
}
