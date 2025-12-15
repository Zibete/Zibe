package com.zibete.proyecto1.ui.editprofile

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.ERR_UNDER_AGE
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_BIRTHDAY_REQUIRED
import com.zibete.proyecto1.utils.Utils
import com.zibete.proyecto1.utils.Utils.dateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val firebaseUser = userRepository.firebaseUser
    private val myUid = userRepository.myUid

    private var myInstallId: String? = null
    private var myFcmToken: String? = null

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    private val _events = MutableSharedFlow<EditProfileUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditProfileUiEvent> = _events.asSharedFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            launch {
                runCatching { myInstallId = FirebaseInstallations.getInstance().id.await() }
                runCatching { myFcmToken = FirebaseMessaging.getInstance().token.await() }
            }

            runCatching {

                val u = userRepository.getUserProfile(myUid) ?: return@launch

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        displayName = u.name,
                        description = u.description,
                        birthDate = u.birthDay,
                        age = Utils.calcAge(u.birthDay),
                        photoUrl = u.photoUrl,
                        photoPreviewUri = null,
                        deletePhoto = false,
                        saveEnabled = false
                    )
                }
            }.onFailure { it ->
                _uiState.update { it.copy(isLoading = false) }
                _events.tryEmit(EditProfileUiEvent.ShowMessage(it.message ?: "Error cargando perfil"))
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
        return Utils.calcAge(birthDay)
    }

    fun isOnboardingProfileDone () : Boolean { return userPreferencesRepository.onboardingProfileDone }

    fun onboardingProfileDone () { userPreferencesRepository.onboardingProfileDone = true }

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

    fun onError(message: String) {
        _events.tryEmit(EditProfileUiEvent.ShowMessage(message))
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            val state = _uiState.value
            val name = state.displayName.trim()
            val description = state.description.trim()
            val birthDate = state.birthDate.trim()

            if (birthDate.isBlank()) {
                onError(SIGNUP_ERR_BIRTHDAY_REQUIRED)
                return@launch
            }

            val age = computeAgeFromString(birthDate)

            if (age < 18) {
                onError(ERR_UNDER_AGE)
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }

            runCatching {

                // 1) Resolver foto final
                var finalPhotoUrl: String? = state.photoUrl

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

                // 2) Update FirebaseAuth user profile
                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .apply {
                        finalPhotoUrl?.let { photoUri = it.toUri() }
                    }
                    .build()

                firebaseUser.updateProfile(req).await()

                // 3) Update Realtime DB (/Cuentas)


                val updates = mutableMapOf<String, Any>(
                    "nombre" to name,
                    "birthDay" to birthDate,
                    "age" to age,
                    "descripcion" to description,
                    "date" to dateTime()
                )

                finalPhotoUrl?.let { updates["foto"] = it }

                myInstallId?.takeIf { it.isNotBlank() }?.let {
                    updates["installId"] = it
                    updates["token"] = it // mantengo tu comportamiento actual
                }

                myFcmToken?.takeIf { it.isNotBlank() }?.let {
                    updates["fcmToken"] = it
                }

                firebaseRefsContainer.refAccounts.child(myUid).updateChildren(updates).await()

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        photoUrl = finalPhotoUrl,
                        photoPreviewUri = null,
                        deletePhoto = false,
                        saveEnabled = false
                    )
                }

                _events.tryEmit(EditProfileUiEvent.NavigateToSplash)
            }.onFailure {
                _uiState.update { it.copy(isSaving = false) }
                onError("Error guardando perfil")
            }
        }
    }
}
