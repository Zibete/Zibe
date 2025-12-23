package com.zibete.proyecto1.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.ui.constants.ERR_PASSWORD_REQUIRED
import com.zibete.proyecto1.ui.constants.ERR_UNDER_AGE
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_BIRTHDAY_REQUIRED
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_EMAIL_IN_USE
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_EXCEPTION
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_GENERIC_PREFIX
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_INVALID_EMAIL
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_INVALID_PASSWORD
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_NAME_REQUIRED
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_UNEXPECTED_PREFIX
import com.zibete.proyecto1.ui.constants.SIGNUP_MSG_SUCCESS
import com.zibete.proyecto1.utils.TimeUtils.ageCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val userSessionManager: UserSessionManager,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SignUpUiEvent>()
    val events = _events.asSharedFlow()

    // -------------------------- Registro

    fun onRegister(
        email: String,
        password: String,
        name: String,
        birthDate: String,
        description: String
    ) {
        viewModelScope.launch {

            if (!validateInputs(email, password, name, birthDate)) return@launch

            _uiState.update { it.copy(isLoading = true) }

            try {
                // 1) Auth
                val authResult = firebaseAuth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val firebaseUser = authResult.user
                    ?: throw IllegalStateException(SIGNUP_ERR_EXCEPTION)

                // 2) Auth profile (para que createUserNode tome displayName/photoUrl)
                userSessionManager.updateAuthProfile(
                    userName = name,
                    photoUrl = DEFAULT_PROFILE_PHOTO_URL
                )

                // 3) Guardar perfil
                userRepository.createUserNode(firebaseUser, birthDate, description)

                // 4) Sesión /sessions (installId + fcmToken)
                val installId = sessionRepository.getLocalInstallId()
                val fcmToken = sessionRepository.getLocalFcmToken()

                if (installId.isNotBlank()) {
                    sessionRepository.setActiveSession(
                        uid = firebaseUser.uid,
                        installId = installId,
                        fcmToken = fcmToken
                    )
                }

                // 5) Éxito
                _events.emit(
                    SignUpUiEvent.ShowSnack(
                        message = SIGNUP_MSG_SUCCESS,
                        type = ZibeSnackType.SUCCESS
                    )
                )

                // 6) Pedir permisos de ubicación (lo maneja la Activity)
                _events.emit(SignUpUiEvent.RequestLocationPermission)

            } catch (e: Exception) {

                val (userMessage, type) = when (e) {
                    is FirebaseAuthException -> {
                        val msg = when (e.errorCode) {
                            "ERROR_EMAIL_ALREADY_IN_USE" -> SIGNUP_ERR_EMAIL_IN_USE
                            "ERROR_WEAK_PASSWORD"        -> SIGNUP_ERR_INVALID_PASSWORD
                            "ERROR_INVALID_EMAIL"        -> SIGNUP_ERR_INVALID_EMAIL
                            else -> SIGNUP_ERR_GENERIC_PREFIX + e.errorCode
                        }
                        msg to ZibeSnackType.ERROR
                    }

                    else -> (
                            SIGNUP_ERR_UNEXPECTED_PREFIX +
                                    (e.localizedMessage ?: "")
                            ) to ZibeSnackType.ERROR
                }

                _events.emit(
                    SignUpUiEvent.ShowSnack(
                        message = userMessage,
                        type = type
                    )
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun isAdult(birthStr: String): Boolean = try {
        ageCalculator(birthStr) >= 18
    } catch (_: Exception) {
        false
    }

    private suspend fun validateInputs(
        email: String,
        password: String,
        name: String,
        birthDate: String
    ): Boolean {

        suspend fun warn(msg: String): Boolean {
            _uiState.update { it.copy(isLoading = false) }
            _events.emit(
                SignUpUiEvent.ShowSnack(
                    message = msg,
                    type = ZibeSnackType.WARNING
                )
            )
            return false
        }

        if (email.isBlank()) return warn(ERR_EMAIL_REQUIRED)
        if (password.isBlank()) return warn(ERR_PASSWORD_REQUIRED)
        if (name.isBlank()) return warn(SIGNUP_ERR_NAME_REQUIRED)
        if (birthDate.isBlank()) return warn(SIGNUP_ERR_BIRTHDAY_REQUIRED)
        if (!isAdult(birthDate)) return warn(ERR_UNDER_AGE)

        return true
    }

}
