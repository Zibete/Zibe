package com.zibete.proyecto1.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.zibete.proyecto1.core.constants.DELETE_ACCOUNT_SUCCESS
import com.zibete.proyecto1.core.constants.DO_NOT_DELETE_ACCOUNT
import com.zibete.proyecto1.core.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.core.constants.ERR_PASSWORD_REQUIRED
import com.zibete.proyecto1.core.constants.ERR_ZIBE
import com.zibete.proyecto1.core.constants.resetPasswordError
import com.zibete.proyecto1.core.constants.resetPasswordSuccess
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.data.auth.DefaultGoogleSignInUseCase
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
    private val userSessionProvider: UserSessionProvider,
    private val userSessionActions: UserSessionActions,
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

            userSessionActions.signInWithEmail(
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

            userSessionActions.sendPasswordResetEmail(
                email = email.trim()
            ).onSuccess {
                showMessage(
                    message = resetPasswordSuccess(email),
                    type = ZibeSnackType.SUCCESS
                )
            }.onFailure {
                showMessage(
                    message = resetPasswordError(email),
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

                    userSessionActions.signInWithCredential(credential)
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

            userSessionActions.signInWithCredential(facebookCredential)
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
                        message = DELETE_ACCOUNT_SUCCESS,
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
            message = DO_NOT_DELETE_ACCOUNT,
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
        val user = userSessionProvider.currentUser
        if (user == null) {
            showMessage(ERR_ZIBE, ZibeSnackType.ERROR)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = false) }
            _events.emit(AuthUiEvent.NavigateToSplash)
        }
    }

    // ================= HELPERS =================

    fun showMessage(
        message: String,
        type: ZibeSnackType,
        stopLoading: Boolean = true
    ) {
        if (stopLoading) _uiState.update { it.copy(isLoading = false) }
        viewModelScope.launch { _events.emit(AuthUiEvent.ShowSnack(message, type)) }
    }

    private fun validateInputs(email: String, password: String): Boolean {

        fun warn(msg: String): Boolean {
            showMessage(msg, ZibeSnackType.WARNING, stopLoading = false)
            return false
        }

        if (email.isBlank()) return warn(ERR_EMAIL_REQUIRED)
        if (password.isBlank()) return warn(ERR_PASSWORD_REQUIRED)
        return true
    }
}
