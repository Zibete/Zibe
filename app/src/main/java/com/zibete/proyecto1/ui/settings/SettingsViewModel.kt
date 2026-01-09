package com.zibete.proyecto1.ui.settings

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.FirebaseSessionManager.AuthProvider
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions,
    private val userRepository: UserRepository,
    private val logoutUseCase: LogoutUseCase,
    private val authSessionProvider: AuthSessionProvider,
    private val snackBarManager: SnackBarManager
) : ViewModel() {

    val currentUser: FirebaseUser
        get() = checkNotNull(authSessionProvider.currentUser) {
            USER_PROVIDER_ERR_EXCEPTION
        }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsUiEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    init {
        refreshStaticState()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesProvider.groupNotificationsFlow.collect { enabled ->
                _uiState.update { it.copy(groupNotificationsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesProvider.individualNotificationsFlow.collect { enabled ->
                _uiState.update { it.copy(individualNotificationsEnabled = enabled) }
            }
        }
    }

    fun onChangeEmailHeaderClicked() {
        val state = _uiState.value
        if (!state.canChangeCredentials) {
            onError(
                UiText.StringRes(
                    R.string.settings_err_change_email_provider,
                    listOf(state.providerLabel ?: "")
                )
            )
            return
        }

        _uiState.update {
            it.copy(
                isEmailSectionExpanded = !it.isEmailSectionExpanded,
                isPasswordSectionExpanded = false
            )
        }
    }

    fun onChangePasswordHeaderClicked() {
        val state = _uiState.value
        if (!state.canChangeCredentials) {
            onError(
                UiText.StringRes(
                    R.string.settings_err_change_password_provider,
                    listOf(state.providerLabel ?: "")
                )
            )
            return
        }

        _uiState.update {
            it.copy(
                isPasswordSectionExpanded = !it.isPasswordSectionExpanded,
                isEmailSectionExpanded = false
            )
        }
    }

    fun onGroupNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesActions.setGroupNotifications(enabled)
            showSnack(
                if (enabled) UiText.StringRes(R.string.settings_group_notifications_on)
                else UiText.StringRes(R.string.settings_group_notifications_off)
            )
        }
    }

    fun onIndividualNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesActions.setIndividualNotifications(enabled)
            showSnack(
                uiText =
                    if (enabled) UiText.StringRes(R.string.settings_individual_notifications_on)
                    else UiText.StringRes(R.string.settings_individual_notifications_off)
            )
        }
    }

    // -------------------------
    // EMAIL / PASSWORD
    // -------------------------

    fun updateEmail(password: String, email: String) {
        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            val state = _uiState.value

            if (!state.canChangeCredentials) {
                onError(
                    UiText.StringRes(
                        R.string.settings_err_change_email_provider,
                        listOf(state.providerLabel ?: "")
                    )
                )
                return@launch
            }

            if (!validateInputs(email, password)) return@launch

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                onError(UiText.StringRes(R.string.auth_err_incorrect_email))
                return@launch
            }

            if (!reauthenticate(AuthProvider.PASSWORD, password)) {
                onError(UiText.StringRes(R.string.auth_err_incorrect_password))
                return@launch
            }

            runCatching { currentUser.updateEmail(email).await() }
                .onFailure { e ->
                    onError(
                        UiText.StringRes(
                            R.string.err_zibe_prefix,
                            args = listOf(e.message ?: "")
                        )
                    )
                    return@launch
                }.onSuccess {
                    userRepository.updateEmail(email)
                    userRepository.updateLocalProfile(
                        name = userRepository.myUserName,
                        photoUrl = userRepository.myProfilePhotoUrl,
                        email = email
                    )

                    snackBarManager.show(
                        uiText = UiText.StringRes(R.string.settings_success_actualize_email),
                        type = ZibeSnackType.SUCCESS
                    )

                    onNavigateToSplash()
                }
        }
    }

    fun updatePassword(password: String, newPassword: String) {
        viewModelScope.launch {
            val state = _uiState.value

            if (!state.canChangeCredentials) {
                onError(
                    UiText.StringRes(
                        R.string.settings_err_change_password_provider,
                        listOf(state.providerLabel ?: "")
                    )
                )
                return@launch
            }

            if (password.isBlank() || newPassword.length < 6) {
                onError(UiText.StringRes(R.string.signup_err_invalid_format_password))
                return@launch
            }

            if (!reauthenticate(AuthProvider.PASSWORD, password)) {
                onError(UiText.StringRes(R.string.auth_err_incorrect_password))
                return@launch
            }

            runCatching { currentUser.updatePassword(newPassword).await() }
                .onFailure { e ->
                    onError(
                        UiText.StringRes(
                            R.string.err_zibe_prefix,
                            args = listOf(e.message ?: "")
                        )
                    )
                    return@launch
                }.onSuccess {
                    snackBarManager.show(
                        uiText = UiText.StringRes(R.string.settings_success_actualize_password),
                        type = ZibeSnackType.SUCCESS
                    )
                    onNavigateToSplash()
                }

        }
    }

    // -------------------------
    // DELETE / LOGOUT
    // -------------------------

    fun deleteAccount(passwordIfNeeded: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val providerType = authSessionProvider.authProvider()
            val requiresPassword = providerType == AuthProvider.PASSWORD

            if (requiresPassword) {
                if (passwordIfNeeded.isNullOrBlank() ||
                    !reauthenticate(AuthProvider.PASSWORD, passwordIfNeeded)
                ) {
                    showSnack(
                        UiText.StringRes(R.string.settings_err_incorrect_password),
                        ZibeSnackType.ERROR
                    )
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
            }

            // Eliminación: Se decide en Splash
            _uiState.update { it.copy(isLoading = false) }
            onLogoutRequested()
        }
    }

    fun onLogoutRequested() {
        viewModelScope.launch {
            logoutUseCase.execute()
            onNavigateToSplash()
        }
    }

    // -------------------------
    // INTERNAL HELPERS
    // -------------------------

    private fun refreshStaticState() {
        val providerType = authSessionProvider.authProvider()
        val providerLabel = authSessionProvider.authProviderLabel()
        val canChange = providerType == AuthProvider.PASSWORD

        val emailBase = userRepository.myEmail
            .takeIf { it.isNotBlank() }
            ?: (currentUser.email ?: "")

        val display = buildString {
            append(emailBase)
            providerLabel?.let { append("\n($it)") }
        }

        _uiState.update {
            it.copy(
                emailDisplay = display,
                providerLabel = providerLabel,
                canChangeCredentials = canChange,
                requiresPasswordForSensitiveActions = canChange
            )
        }
    }

    private suspend fun reauthenticate(
        authProvider: AuthProvider,
        password: String?
    ): Boolean {
        val credential = when (authProvider) {
            AuthProvider.PASSWORD -> {
                val email = currentUser.email.orEmpty()
                if (email.isBlank() || password.isNullOrBlank()) return false
                EmailAuthProvider.getCredential(email, password)
            }

            AuthProvider.GOOGLE -> {
                val acct = GoogleSignIn.getLastSignedInAccount(appContext) ?: return false
                val token = acct.idToken ?: return false
                GoogleAuthProvider.getCredential(token, null)
            }

            AuthProvider.FACEBOOK -> {
                val token = AccessToken.getCurrentAccessToken()?.token ?: return false
                FacebookAuthProvider.getCredential(token)
            }

            else -> return false
        }

        return runCatching {
            currentUser.reauthenticate(credential).await()
            true
        }.getOrDefault(false)
    }

    fun onError(uiText: UiText) {
        _uiState.update { it.copy(isLoading = false) }
        showSnack(uiText, ZibeSnackType.ERROR)
    }

    private fun showSnack(
        uiText: UiText,
        type: ZibeSnackType = ZibeSnackType.INFO
    ) {
        _events.tryEmit(
            SettingsUiEvent.ShowSnack(
                uiText = uiText,
                type = type,
            )
        )
    }

    private fun validateInputs(email: String, password: String): Boolean {

        fun warn(uiText: UiText): Boolean {
            onError(uiText)
            return false
        }

        if (email.isBlank()) return warn(uiText = UiText.StringRes(R.string.err_email_required))
        if (password.isBlank()) return warn(uiText = UiText.StringRes(R.string.err_password_required))
        return true
    }

    private fun onNavigateToSplash() {
        _events.tryEmit(SettingsUiEvent.NavigateToSplash)
    }
}

