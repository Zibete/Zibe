package com.zibete.proyecto1.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.GoogleSignInUseCase
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthUiEvent>()
    val events = _events.asSharedFlow()

    fun initAfterDelete() {
        viewModelScope.launch {
            val deleteUser = userPreferencesProvider.isDeleteUser()
            _uiState.update { it.copy(deleteUser = deleteUser) }
        }
    }

    // ================= EMAIL / PASSWORD =================

    fun onEmailLogin(email: String, password: String) {
        viewModelScope.launch {
            if (!validateInputs(email, password)) return@launch

            _uiState.update { it.copy(isLoading = true) }

            authSessionActions.signInWithEmail(
                email = email.trim(),
                password = password.trim()
            ).onSuccess {
                handleAuthSuccess()
            }.onFailure { e ->
                showMessage(
                    message = getAuthErrorMessage(e),
                    type = ZibeSnackType.ERROR
                )
            }
        }
    }

    // ================= RESET PASSWORD =================

    fun onResetPassword(email: String) {
        if (email.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            authSessionActions.sendPasswordResetEmail(
                email = email.trim()
            ).onSuccess {
                showMessage(
                    message = UiText.StringRes(
                        R.string.reset_password_success,
                        args = listOf(email)
                    ),
                    type = ZibeSnackType.SUCCESS
                )
            }.onFailure {
                showMessage(
                    message = UiText.StringRes(R.string.reset_password_error, args = listOf(email)),
                    type = ZibeSnackType.ERROR
                )
            }
        }
    }

    // ================= GOOGLE =================
    fun onGoogleClick(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            googleSignInUseCase(activity)
                .onFailure { e ->
                    showMessage(
                        message = getAuthErrorMessage(e),
                        type = ZibeSnackType.ERROR
                    )
                    return@launch
                }
                .onSuccess { idToken ->
                    val credential = GoogleAuthProvider.getCredential(idToken!!, null)

                    authSessionActions.signInWithCredential(credential)
                        .onSuccess { handleAuthSuccess() }
                        .onFailure { e ->
                            showMessage(
                                message = getAuthErrorMessage(e),
                                type = ZibeSnackType.ERROR
                            )
                        }
                }
        }
    }

    // ================= FACEBOOK =================

    fun onFacebookAccessToken(token: AccessToken) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val facebookCredential = FacebookAuthProvider.getCredential(token.token)

            authSessionActions.signInWithCredential(facebookCredential)
                .onSuccess { handleAuthSuccess() }
                .onFailure { e ->
                    showMessage(
                        message = getAuthErrorMessage(e),
                        type = ZibeSnackType.ERROR
                    )
                }
        }
    }

    // ================= DELETE USER FLOW =================

    fun onDeleteAccountClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            deleteAccountUseCase.execute()
                .onSuccess {
                    showMessage(
                        message = UiText.StringRes(R.string.account_delete_success),
                        type = ZibeSnackType.INFO,
                        stopLoading = false
                    )
                    setDeleteUser(deleteUser = false)
                }
                .onFailure { e ->
                    showMessage(
                        message = getAuthErrorMessage(e),
                        type = ZibeSnackType.ERROR
                    )
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun setDeleteUser(deleteUser: Boolean) {
        viewModelScope.launch {
            userPreferencesActions.setDeleteUser(deleteUser)
            _uiState.update { it.copy(deleteUser = deleteUser, isLoading = false) }
        }
    }

    // ================= DO NOT DELETE =================

    fun onDoNotDeleteAccountClicked() {
        showMessage(
            message = UiText.StringRes(R.string.account_delete_cancelled),
            type = ZibeSnackType.INFO,
            stopLoading = false
        )
        setDeleteUser(false)
    }

    fun onNavigateToSignUpClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = false) }
            _events.emit(AuthUiEvent.NavigateToSignUp)
        }
    }

    // ================= SUCCESS ROUTING =================

    private fun handleAuthSuccess() {
        val user = authSessionProvider.currentUser
        if (user == null) {
            showMessage(message = UiText.StringRes(R.string.err_zibe), ZibeSnackType.ERROR)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = false) }
            _events.emit(AuthUiEvent.NavigateToSplash)
        }
    }

    // ================= HELPERS =================

    fun showMessage(
        message: UiText,
        type: ZibeSnackType,
        stopLoading: Boolean = true
    ) {
        if (stopLoading) _uiState.update { it.copy(isLoading = false) }
        viewModelScope.launch { _events.emit(AuthUiEvent.ShowSnack(message, type)) }
    }

    private fun validateInputs(email: String, password: String): Boolean {

        fun warn(message: UiText): Boolean {
            showMessage(message = message, type = ZibeSnackType.WARNING, stopLoading = false)
            return false
        }

        if (email.isBlank()) return warn(message = UiText.StringRes(R.string.err_email_required))
        if (password.isBlank()) return warn(message = UiText.StringRes(R.string.err_password_required))
        return true
    }
}
