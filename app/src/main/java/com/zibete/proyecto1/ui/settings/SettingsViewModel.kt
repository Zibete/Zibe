package com.zibete.proyecto1.ui.settings

import android.content.Context
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
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onFinally
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.core.validation.CredentialValidators
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.FirebaseSessionManager.AuthProvider
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val snackBarManager: SnackBarManager,
    private val appNavigator: AppNavigator
) : ViewModel() {

    val currentUser: FirebaseUser
        get() = checkNotNull(authSessionProvider.currentUser) {
            USER_PROVIDER_ERR_EXCEPTION
        }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()


    init {
        refreshStaticState()
        observePreferences()
    }

    private fun refreshStaticState() {
        val providerType = authSessionProvider.authProvider()
        val providerLabel = authSessionProvider.authProviderLabel()
        val canChange = providerType == AuthProvider.PASSWORD

        val currentEmail = userRepository.myEmail
            .takeIf { it.isNotBlank() }
            ?: (currentUser.email ?: "")

        val display = buildString {
            append(currentEmail)
            providerLabel?.let { append("\n($it)") }
        }

        _uiState.update {
            it.copy(
                emailDisplay = display,
                providerLabel = providerLabel,
                canChangeCredentials = canChange,
                requiresPasswordForSensitiveActions = canChange,
                currentEmail = currentEmail
            )
        }
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

    fun onGroupNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesActions.setGroupNotifications(enabled)
        }

        val uiText = if (enabled) UiText.StringRes(R.string.settings_group_notifications_on)
        else UiText.StringRes(R.string.settings_group_notifications_off)

        showSnack(
            uiText = uiText,
            snackType = ZibeSnackType.SUCCESS
        )
    }

    fun onIndividualNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesActions.setIndividualNotifications(enabled)

            val uiText =
                if (enabled) UiText.StringRes(R.string.settings_individual_notifications_on)
                else UiText.StringRes(R.string.settings_individual_notifications_off)

            showSnack(
                uiText = uiText,
                snackType = ZibeSnackType.SUCCESS
            )
        }
    }

    // -------------------------
    // EMAIL / PASSWORD
    // -------------------------

    fun updateEmail(
        newEmail: String,
        currentPassword: String
    ) {
        viewModelScope.launch {
            setLoading(true)

            val state = _uiState.value
            val trimmedNewEmail = newEmail.trim()

            // 1) Verificamos que newEmail y currentPassword no estén vacíos
            if (!validateInputs(trimmedNewEmail, currentPassword)) return@launch


            // 2) Verificamos que pueda cambiar las credenciales
            if (!state.canChangeCredentials) {
                showSnack(
                    state.providerLabel.toUiText(
                        R.string.settings_err_provider,
                        R.string.message_err_not_available_generic
                    )
                )
                return@launch
            }

            // 3) Verificamos que el nuevo correo sea distinto al actual
            if (trimmedNewEmail == state.currentEmail) {
                showSnack(UiText.StringRes(R.string.auth_err_email_no_changes))
                return@launch
            }

            // 4) Verificamos que el correo cumpla con el stándard
            if (!CredentialValidators.isValidEmail(trimmedNewEmail)) {
                showSnack(UiText.StringRes(R.string.auth_err_incorrect_email))
                return@launch
            }

            // 5) Verificamos que la constraseña ingresada sea correcta
            if (!reauthenticate(AuthProvider.PASSWORD, currentPassword)) {
                showSnack(UiText.StringRes(R.string.auth_err_incorrect_password))
                return@launch
            }

            zibeCatching {
                currentUser.updateEmail(trimmedNewEmail).await()
            }.onFailure { e ->
                showSnack(
                    e.message.toUiText(
                        R.string.err_zibe_prefix,
                        R.string.err_zibe
                    )
                )
                return@launch
            }.onSuccess {
                userRepository.updateEmail(trimmedNewEmail)
                userRepository.updateLocalProfile(
                    name = userRepository.myUserName,
                    photoUrl = userRepository.myProfilePhotoUrl,
                    email = trimmedNewEmail
                )

                showSnack(
                    uiText = UiText.StringRes(R.string.settings_success_actualize_email),
                    snackType = ZibeSnackType.SUCCESS
                )

                onNavigateToSplash()
            }.onFinally {
                stopLoading()
            }
        }
    }

    fun onEmailInputChanged(value: String) {
        _uiState.update {
            it.copy(emailInput = value)
        }
    }

    fun onPasswordForEmailChanged(value: String) {
        _uiState.update {
            it.copy(passwordEmailInput = value)
        }
    }

    fun onCurrentPasswordChanged(value: String) {
        _uiState.update {
            it.copy(passwordInput = value)
        }
    }

    fun onNewPasswordInputChanged(value: String) {
        _uiState.update {
            it.copy(newPasswordInput = value)
        }
    }

    fun updatePassword(
        password: String,
        newPassword: String
    ) {
        viewModelScope.launch {
            val state = _uiState.value

            setLoading(true)

            if (!state.canChangeCredentials) {
                showSnack(
                    UiText.StringRes(
                        R.string.settings_err_provider,
                        listOf(state.providerLabel ?: "")
                    )
                )
                return@launch
            }

            if (!CredentialValidators.isValidPassword(newPassword)) {
                showSnack(UiText.StringRes(R.string.signup_err_invalid_format_password))
                return@launch
            }

            if (!reauthenticate(AuthProvider.PASSWORD, password)) {
                showSnack(UiText.StringRes(R.string.auth_err_incorrect_password))
                return@launch
            }

            runCatching { currentUser.updatePassword(newPassword).await() }
                .onFailure { e ->
                    showSnack(
                        e.message.toUiText(
                            R.string.err_zibe_prefix,
                            R.string.err_zibe
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
            setLoading(true)

            val providerType = authSessionProvider.authProvider()
            val requiresPassword = providerType == AuthProvider.PASSWORD

            if (requiresPassword) {
                if (passwordIfNeeded.isNullOrBlank() ||
                    !reauthenticate(AuthProvider.PASSWORD, passwordIfNeeded)
                ) {
                    showSnack(UiText.StringRes(R.string.settings_err_incorrect_password))
                    return@launch
                }
            }

            onLogoutRequested()
        }
    }

    fun onLogoutRequested() {
        viewModelScope.launch {
            setLoading(true)
            logoutUseCase.execute()
                .onFailure { e ->
                    showSnack(
                        e.message.toUiText(
                            R.string.err_zibe_prefix,
                            R.string.err_zibe
                        )
                    )
                }
                .onSuccess {
                    setLoading(false)
                    onNavigateToSplash()
                }
        }
    }

    // -------------------------
    // INTERNAL HELPERS
    // -------------------------


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

            AuthProvider.OTHER -> return false
            AuthProvider.NONE -> return false
        }

        return runCatching {
            currentUser.reauthenticate(credential).await()
            true
        }.getOrDefault(false)
    }

    private fun setLoading(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    private fun setSaving(value: Boolean) {
        _uiState.update { it.copy(isSaving = value) }
    }

    private fun stopLoading() {
        _uiState.update {
            it.copy(
                isLoading = false,
                isSaving = false
            )
        }
    }

    private fun onNavigateToSplash() {
        appNavigator.finishFlowNavigateToSplash()
    }

    // ================= HELPERS =================

    fun showSnack(
        uiText: UiText,
        snackType: ZibeSnackType = ZibeSnackType.ERROR,
        stopLoading: Boolean = true
    ) {
        snackBarManager.show(
            uiText = uiText,
            type = snackType
        )
        if (stopLoading) stopLoading()
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
}

