package com.zibete.proyecto1.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_EXCEPTION
import com.zibete.proyecto1.core.di.SettingsConfig
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.core.utils.TimeUtils.isAdult
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.core.validation.CredentialValidators
import com.zibete.proyecto1.core.validation.EmailValidator
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.domain.profile.UpdateProfileUseCase
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authSessionActions: AuthSessionActions,
    private val sessionBootstrapper: SessionBootstrapper,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val snackBarManager: SnackBarManager,
    private val appNavigator: AppNavigator,
    private val config: SettingsConfig,
    private val emailValidator: EmailValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()

    private var validationJob: Job? = null

    // -------------------------- Inputs Validation

    fun onEmailInputChanged(email: String) {
        validationJob?.cancel()
        _uiState.update { it.copy(emailError = null) }

        if (email.isBlank()) return

        validationJob = viewModelScope.launch {
            delay(config.validationDebounce)
            val error = CredentialValidators.validateEmail(email, emailValidator)
            _uiState.update { it.copy(emailError = error) }
        }
    }

    fun onPasswordInputChanged(password: String) {
        validationJob?.cancel()
        _uiState.update { it.copy(passwordError = null) }

        if (password.isBlank()) return

        validationJob = viewModelScope.launch {
            delay(config.validationDebounce)
            val error = CredentialValidators.validateNewPassword(password = password, compareTo = null)
            _uiState.update { it.copy(passwordError = error) }
        }
    }

    fun onBirthDateChanged(birthDate: String) {
        _uiState.update { it.copy(birthDateError = null, age = null) }

        if (birthDate.isBlank()) {
            _uiState.update { it.copy(birthDateError = UiText.StringRes(R.string.signup_err_birthdate_required)) }
            return
        }

        val age = birthDate.trim().takeIf { it.isNotBlank() }?.let { ageCalculator(it) }
        _uiState.update { it.copy(age = age) }

        if (!isAdult(birthDate)) {
            _uiState.update { it.copy(birthDateError = UiText.StringRes(R.string.err_under_age)) }
        }
    }

    // -------------------------- Registro

    fun onRegister(
        email: String,
        password: String,
        name: String,
        birthDate: String,
        description: String
    ) {
        viewModelScope.launch {

            setLoading(true)

            // 1. Validar campos
            if (!validateInputs(email, password, name, birthDate)) return@launch

            // 2. Crear Usuario
            authSessionActions.createUser(
                email = email.trim(),
                password = password.trim()
            ).onFailure { e ->
                handleRegisterError(e)
                return@launch
            }.onSuccess { authResult ->

                val firebaseUser = authResult?.user
                    ?: return@launch handleRegisterError(
                        IllegalStateException(SIGNUP_ERR_EXCEPTION)
                    )

                // 3. Guardar perfil RTDB
                sessionBootstrapper.bootstrap(
                    uid = firebaseUser.uid,
                    birthDate = birthDate,
                    description = description
                ).onFailure { e ->
                    handleRegisterError(e)
                    return@launch
                }
            }

            // 4. Guardar Perfil de Auth
            authSessionActions.updateAuthProfile(
                userName = name,
                photoUrl = DEFAULT_PROFILE_PHOTO_URL
            ).onFailure { e ->
                handleRegisterError(e)
                return@launch
            }.onSuccess {
                // 5. Éxito
                showSnack(
                    uiText = UiText.StringRes(R.string.signup_msg_success),
                    snackType = ZibeSnackType.SUCCESS
                )
                // 6. Splash -> Location Permission
                appNavigator.finishFlowNavigateToSplash()
            }
        }
    }

    private fun handleRegisterError(e: Throwable) {
        val uiText = getAuthErrorMessage(e)
        showSnack(uiText = uiText)
    }

    private fun setLoading(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    fun showSnack(
        uiText: UiText,
        snackType: ZibeSnackType = ZibeSnackType.ERROR,
        stopLoading: Boolean = true
    ) {
        snackBarManager.show(
            uiText = uiText,
            type = snackType
        )
        if (stopLoading) setLoading(false)
    }

    private fun validateInputs(
        email: String,
        password: String,
        name: String,
        birthDate: String
    ): Boolean {

        fun warn(uiText: UiText): Boolean {
            showSnack(uiText, ZibeSnackType.WARNING)
            return false
        }

        val emailError = CredentialValidators.validateEmail(email, emailValidator)
        val passwordError = CredentialValidators.validateNewPassword(password = password, compareTo = null)

        if (emailError != null) return warn(emailError)
        if (passwordError != null) return warn(passwordError)

        if (name.isBlank()) return warn(UiText.StringRes(R.string.signup_err_name_required))
        if (birthDate.isBlank()) {
            _uiState.update { it.copy(birthDateError = UiText.StringRes(R.string.signup_err_birthdate_required)) }
            return false
        }
        if (!isAdult(birthDate)) {
            _uiState.update { it.copy(birthDateError = UiText.StringRes(R.string.err_under_age)) }
            return false
        }

        return true
    }
}
