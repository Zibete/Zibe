package com.zibete.proyecto1.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.zibete.proyecto1.core.ZibeResult
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.core.constants.ERR_PASSWORD_REQUIRED
import com.zibete.proyecto1.core.constants.ERR_UNDER_AGE
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_BIRTHDAY_REQUIRED
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_EXCEPTION
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_NAME_REQUIRED
import com.zibete.proyecto1.core.constants.SIGNUP_MSG_SUCCESS
import com.zibete.proyecto1.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.utils.getAuthErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val userRepositoryActions: UserRepositoryActions,
    private val userSessionActions: UserSessionActions,
    private val sessionBootstrapper: SessionBootstrapper,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SignUpUiEvent>(extraBufferCapacity = 1)
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

            // 1. Crear Usuario
            val authResult = userSessionActions.createUser(email.trim(), password.trim())

            if (authResult is ZibeResult.Failure) {
                handleRegisterError(authResult.exception)
                return@launch
            }

            val firebaseUser = (authResult as ZibeResult.Success).data?.user
                ?: return@launch handleRegisterError(IllegalStateException(SIGNUP_ERR_EXCEPTION))

            // 2. Perfil de Auth
            val updateAuthProfileResult = userSessionActions.updateAuthProfile(
                userName = name,
                photoUrl = DEFAULT_PROFILE_PHOTO_URL
            )

            if (updateAuthProfileResult is ZibeResult.Failure) {
                handleRegisterError(updateAuthProfileResult.exception)
                return@launch
            }

            // 3. Guardar perfil
            val sessionBootstrapperResult = sessionBootstrapper.bootstrap(
                uid = firebaseUser.uid,
                birthDate = birthDate,
                description = description
            )

            if (sessionBootstrapperResult is ZibeResult.Failure) {
                handleRegisterError(sessionBootstrapperResult.exception)
                return@launch
            }

            // 4. Éxito
            _events.emit(
                SignUpUiEvent.ShowSnack(
                    message = SIGNUP_MSG_SUCCESS,
                    type = ZibeSnackType.SUCCESS
                )
            )
            _uiState.update { it.copy(isLoading = false) }

            // 5. Splash -> Location Permission
            _events.emit(SignUpUiEvent.NavigateToSplash)
        }
    }

    private suspend fun handleRegisterError(e: Throwable) {
        val message = getAuthErrorMessage(e)

        _events.emit(
            SignUpUiEvent.ShowSnack(
                message = message,
                type = ZibeSnackType.ERROR
            )
        )

        _uiState.update { it.copy(isLoading = false) }
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
