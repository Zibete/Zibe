package com.zibete.proyecto1.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_EXCEPTION
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.TimeUtils.isAdult
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onFinally
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authSessionActions: AuthSessionActions,
    private val sessionBootstrapper: SessionBootstrapper,
    private val snackBarManager: SnackBarManager,
    private val appNavigator: AppNavigator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()

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
            }.onFinally {
                setLoading(false)
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

        if (email.isBlank()) return warn(UiText.StringRes(R.string.err_email_required))
        if (password.isBlank()) return warn(UiText.StringRes(R.string.err_password_required))
        if (name.isBlank()) return warn(UiText.StringRes(R.string.signup_err_name_required))
        if (birthDate.isBlank()) return warn(UiText.StringRes(R.string.signup_err_birthdate_required))
        if (!isAdult(birthDate)) return warn(UiText.StringRes(R.string.err_under_age))

        return true
    }
}
