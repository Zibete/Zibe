package com.zibete.proyecto1.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.di.SettingsConfig
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onFinally
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.core.validation.CredentialValidators
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.GoogleSignInUseCase
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authSessionProvider: AuthSessionProvider,
    private val authSessionActions: AuthSessionActions,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase,
    private val snackBarManager: SnackBarManager,
    private val appNavigator: AppNavigator,
    private val config: SettingsConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthUiEvent>()
    val events = _events.asSharedFlow()

    private var validationJob: Job? = null

    fun initAfterDelete(deleteUser: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(deleteAccount = deleteUser) }
        }
    }

    // ================= EMAIL / PASSWORD =================

    fun onEmailInputChanged(email: String) {
        validationJob?.cancel()
        _uiState.update { it.copy(emailError = null) }

        if (email.isBlank()) return

        validationJob = viewModelScope.launch {
            delay(config.validationDebounce)
            val error = CredentialValidators.validateEmail(email)
            _uiState.update { it.copy(emailError = error) }
        }
    }

    fun onResetEmailInputChanged(email: String) {
        validationJob?.cancel()
        _uiState.update { it.copy(resetPasswordEmailError = null) }

        if (email.isBlank()) return

        validationJob = viewModelScope.launch {
            delay(config.validationDebounce)
            val error = CredentialValidators.validateEmail(email)
            _uiState.update { it.copy(resetPasswordEmailError = error) }
        }
    }

    fun onPasswordInputChanged(password: String) {
        validationJob?.cancel()
        _uiState.update { it.copy(passwordError = null) }

        if (password.isBlank()) return

        validationJob = viewModelScope.launch {
            delay(config.validationDebounce)

            val error: UiText? = when {
                password != password.trim() ->
                    UiText.StringRes(R.string.err_password_trim_spaces)

                else -> null
            }

            _uiState.update { it.copy(passwordError = error) }
        }
    }

    fun onEmailLogin(email: String, password: String) {
        viewModelScope.launch {
            if (!validateInputs(email, password)) return@launch

            setLoadingLogin(true)

            authSessionActions.signInWithEmail(
                email = email.trim(),
                password = password.trim()
            ).onSuccess {
                handleAuthSuccess()
            }.onFailure { e ->
                showSnack(
                    uiText = getAuthErrorMessage(e),
                    snackType = ZibeSnackType.ERROR
                )
            }.onFinally {
                setLoadingLogin(false)
            }
        }
    }

    // ================= RESET PASSWORD =================

    fun onResetPassword(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) return

        viewModelScope.launch {

            setLoadingResetPassword(true)

            delay(config.navigationDelay)

            authSessionActions.sendPasswordResetEmail(
                email = trimmedEmail
            ).onFailure {
                _events.emit(AuthUiEvent.CloseResetPasswordSheet)

                showSnack(
                    UiText.StringRes(
                        R.string.reset_password_error,
                        args = listOf(trimmedEmail)
                    ),
                    ZibeSnackType.ERROR
                )
            }.onSuccess {
                _events.emit(AuthUiEvent.CloseResetPasswordSheet)
                
                showSnack(
                    UiText.StringRes(
                        R.string.reset_password_success,
                        args = listOf(trimmedEmail)
                    ),
                    ZibeSnackType.SUCCESS
                )
            }.onFinally {
                setLoadingResetPassword(false)
                _uiState.update { it.copy(resetPasswordEmailError = null) }
            }
        }
    }

    // ================= GOOGLE =================
    fun onGoogleClick(activity: Activity) {
        viewModelScope.launch {
            setLoadingLogin(true)

            googleSignInUseCase(activity)
                .onFailure { e ->
                    showSnack(getAuthErrorMessage(e), ZibeSnackType.ERROR)
                    return@launch
                }
                .onSuccess { idToken ->
                    val credential = GoogleAuthProvider.getCredential(idToken!!, null)

                    authSessionActions.signInWithCredential(credential)
                        .onFailure { e ->
                            showSnack(getAuthErrorMessage(e), ZibeSnackType.ERROR)
                        }
                        .onSuccess {
                            handleAuthSuccess()
                        }
                }.onFinally {
                    setLoadingLogin(false)
                }
        }
    }

    // ================= FACEBOOK =================

    fun onFacebookAccessToken(token: AccessToken) {
        viewModelScope.launch {
            setLoadingLogin(true)

            val facebookCredential = FacebookAuthProvider.getCredential(token.token)

            authSessionActions.signInWithCredential(facebookCredential)
                .onFailure { e ->
                    showSnack(getAuthErrorMessage(e), ZibeSnackType.ERROR)
                }.onSuccess {
                    handleAuthSuccess()
                }.onFinally {
                    setLoadingLogin(false)
                }
        }
    }

    // ================= DELETE USER FLOW =================

    fun onDeleteAccountClicked() {
        viewModelScope.launch {
            setLoadingDeleteAccount(true)

            delay(config.navigationDelay)

            deleteAccountUseCase.execute()
                .onFailure { e ->
                    showSnack(getAuthErrorMessage(e), ZibeSnackType.ERROR)
                }
                .onSuccess {
                    showSnack(
                        uiText = UiText.StringRes(R.string.account_delete_success),
                        snackType = ZibeSnackType.INFO,
                    )
                    _uiState.update { it.copy(deleteAccount = false) }
                }
                .onFailure { e ->
                    showSnack(getAuthErrorMessage(e), ZibeSnackType.ERROR)
                }.onFinally {
                    setLoadingDeleteAccount(false)
                }
        }
    }

    // ================= DO NOT DELETE =================

    fun onDoNotDeleteAccountClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDoNotDelete = true) }
            appNavigator.finishFlowNavigateToSplash(
                uiText = UiText.StringRes(R.string.account_delete_cancelled),
                snackType = ZibeSnackType.INFO
            )
        }
    }

    fun onNavigateToSignUp() {
        viewModelScope.launch {
            setLoadingLogin(false)
            _events.emit(AuthUiEvent.NavigateToSignUp)
        }
    }

    // ================= SUCCESS ROUTING =================

    private fun handleAuthSuccess() {
        val user = authSessionProvider.currentUser
        if (user == null) {
            showSnack(UiText.StringRes(R.string.err_zibe), ZibeSnackType.ERROR)
            return
        }

        navigateToSplash()
    }

    // ================= HELPERS =================

    private fun navigateToSplash() {
        appNavigator.finishFlowNavigateToSplash()
    }

    private fun setLoadingLogin(isLoading: Boolean) {
        _uiState.update { it.copy(isLoadingLogin = isLoading) }
    }

    private fun setLoadingDeleteAccount(isLoading: Boolean) {
        _uiState.update { it.copy(isLoadingDeleteAccount = isLoading) }
    }

    private fun setLoadingResetPassword(isLoading: Boolean) {
        _uiState.update { it.copy(isLoadingResetPassword = isLoading) }
    }

    fun showSnack(
        uiText: UiText,
        snackType: ZibeSnackType
    ) {
        snackBarManager.show(
            uiText = uiText,
            type = snackType
        )
    }

    private fun validateInputs(email: String, password: String): Boolean {

        val emailError = CredentialValidators.validateEmail(email)
        val passwordError =
            CredentialValidators.validateNewPassword(password = password, compareTo = null)

        if (emailError != null) {
            showSnack(emailError, ZibeSnackType.WARNING)
            return false
        }
        if (passwordError != null) {
            showSnack(passwordError, ZibeSnackType.WARNING)
            return false
        }

        return true
    }

    companion object {
        private const val NAVIGATION_DELAY = 450L
    }
}
