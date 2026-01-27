package com.zibete.proyecto1.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import com.facebook.AccessToken
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onFinally
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
    private val googleSignInUseCase: GoogleSignInUseCase,
    private val snackBarManager: SnackBarManager,
    private val appNavigator: AppNavigator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthUiEvent>()
    val events = _events.asSharedFlow()

    fun initAfterDelete(deleteUser: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(deleteAccount = deleteUser) }
        }
    }

    // ================= EMAIL / PASSWORD =================

    fun onEmailLogin(email: String, password: String) {
        viewModelScope.launch {
            if (!validateInputs(email, password)) return@launch

            setLoading(true)

            authSessionActions.signInWithEmail(
                email = email.trim(),
                password = password.trim()
            ).onSuccess {
                handleAuthSuccess()
            }.onFailure { e ->
                showSnack(
                    uiText = getAuthErrorMessage(e)
                )
            }.onFinally {
                setLoading(false)
            }
        }
    }

    // ================= RESET PASSWORD =================

    fun onResetPassword(email: String) {
        if (email.isBlank()) return

        viewModelScope.launch {
            setLoading(true)

            authSessionActions.sendPasswordResetEmail(
                email = email.trim()
            ).onFailure {
                showSnack(
                    UiText.StringRes(
                        R.string.reset_password_error,
                        args = listOf(email)
                    )
                )
            }.onSuccess {
                showSnack(
                    UiText.StringRes(
                        R.string.reset_password_success,
                        args = listOf(email)
                    ),
                    ZibeSnackType.SUCCESS
                )
            }.onFinally {
                setLoading(false)
            }
        }
    }

    // ================= GOOGLE =================
    fun onGoogleClick(activity: Activity) {
        viewModelScope.launch {
            setLoading(true)

            googleSignInUseCase(activity)
                .onFailure { e ->
                    showSnack(getAuthErrorMessage(e))
                    return@launch
                }
                .onSuccess { idToken ->
                    val credential = GoogleAuthProvider.getCredential(idToken!!, null)

                    authSessionActions.signInWithCredential(credential)
                        .onFailure { e ->
                            showSnack(getAuthErrorMessage(e))
                        }
                        .onSuccess {
                            handleAuthSuccess()
                        }
                }.onFinally {
                    setLoading(false)
                }
        }
    }

    // ================= FACEBOOK =================

    fun onFacebookAccessToken(token: AccessToken) {
        viewModelScope.launch {
            setLoading(true)

            val facebookCredential = FacebookAuthProvider.getCredential(token.token)

            authSessionActions.signInWithCredential(facebookCredential)
                .onFailure { e ->
                    showSnack(getAuthErrorMessage(e))
                }.onSuccess {
                    handleAuthSuccess()
                }.onFinally {
                    setLoading(false)
                }
        }
    }

    // ================= DELETE USER FLOW =================

    fun onDeleteAccountClicked() {
        viewModelScope.launch {
            setLoading(true)

            deleteAccountUseCase.execute()
                .onFailure { e ->
                    showSnack(getAuthErrorMessage(e))
                }
                .onSuccess {
                    showSnack(
                        uiText = UiText.StringRes(R.string.account_delete_success),
                        snackType = ZibeSnackType.INFO,
                    )
                    _uiState.update { it.copy(deleteAccount = false) }
                }
                .onFailure { e ->
                    showSnack(getAuthErrorMessage(e))
                }.onFinally {
                    setLoading(isLoading = false)
                }
        }
    }

    // ================= DO NOT DELETE =================

    fun onDoNotDeleteAccountClicked() {
        viewModelScope.launch {
            appNavigator.finishFlowNavigateToSplash(
                uiText = UiText.StringRes(R.string.account_delete_cancelled),
                snackType = ZibeSnackType.INFO
            )
        }
    }

    fun onNavigateToSignUp() {
        viewModelScope.launch {
            setLoading(false)
            _events.emit(AuthUiEvent.NavigateToSignUp)
        }
    }

    // ================= SUCCESS ROUTING =================

    private fun handleAuthSuccess() {
        val user = authSessionProvider.currentUser
        if (user == null) {
            showSnack(UiText.StringRes(R.string.err_zibe))
            return
        }

        navigateToSplash()
    }

    // ================= HELPERS =================

    private fun navigateToSplash() {
        appNavigator.finishFlowNavigateToSplash()
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
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

    private fun validateInputs(email: String, password: String): Boolean {

        fun warn(uiText: UiText): Boolean {
            showSnack(uiText, ZibeSnackType.WARNING)
            return false
        }

        if (email.isBlank()) return warn(UiText.StringRes(R.string.err_email_required))
        if (password.isBlank()) return warn(UiText.StringRes(R.string.err_password_required))

        return true
    }

    companion object {
        private const val NAVIGATION_DELAY = 450L
    }
}
