package com.zibete.proyecto1.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.SETTINGS_SCREEN
import com.zibete.proyecto1.core.device.DeviceInfoProvider
import com.zibete.proyecto1.core.di.SettingsConfig
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.core.validation.CredentialValidators
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.FirebaseSessionManager.AuthProvider
import com.zibete.proyecto1.domain.profile.SendFeedbackUseCase
import com.zibete.proyecto1.domain.profile.UpdateEmailUseCase
import com.zibete.proyecto1.domain.profile.UpdatePasswordUseCase
import com.zibete.proyecto1.domain.session.LogoutUseCase
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
class SettingsViewModel @Inject constructor(
    private val userPreferencesProvider: UserPreferencesProvider,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val userPreferencesActions: UserPreferencesActions,
    private val updateEmailUseCase: UpdateEmailUseCase,
    private val updatePasswordUseCase: UpdatePasswordUseCase,
    private val sendFeedbackUseCase: SendFeedbackUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val localRepositoryProvider: LocalRepositoryProvider,
    private val authSessionProvider: AuthSessionProvider,
    private val authSessionActions: AuthSessionActions,
    private val snackBarManager: SnackBarManager,
    private val appNavigator: AppNavigator,
    private val config: SettingsConfig
) : ViewModel() {

    private var validationJob: Job? = null

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    val appNavigatorEvents = appNavigator.events
    val snackBarEvents = snackBarManager.events

    init {
        load()
        observePreferences()
    }

    private fun load() {
        val providerType = authSessionProvider.authProvider()
        val providerLabel = authSessionProvider.authProviderLabel()
        val canChange = providerType == AuthProvider.PASSWORD
        val currentEmail = localRepositoryProvider.myEmail
        val appVersion = deviceInfoProvider.getAppVersion()

        _uiState.update {
            it.copy(
                currentEmail = currentEmail,
                providerLabel = providerLabel,
                appVersion = appVersion,
                canChangeCredentials = canChange,
                requiresReauthForSensitiveActions = canChange
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

    // -------------------------
    // ACTIONS
    // -------------------------

    fun updateEmail(
        newEmail: String,
        currentPassword: String
    ) {
        viewModelScope.launch {

            setAction(SettingsAction.UPDATE_EMAIL)

            val state = _uiState.value
            val trimmedNewEmail = newEmail.trim()

            // 1) Verificamos que pueda cambiar las credenciales
            if (!state.canChangeCredentials) {
                val uiText = state.providerLabel.toUiText(
                    R.string.settings_err_provider,
                    R.string.message_err_not_available_generic
                )
                setErrors(generalSheetError = uiText)
                return@launch
            }

            // 2) Verificamos que trimmedNewEmail no esté vacío
            if (trimmedNewEmail.isBlank()) {
                setErrors(newEmailError = UiText.StringRes(R.string.err_email_required))
                return@launch
            }

            // 3) Verificamos que currentPassword no esté vacío
            if (currentPassword.isBlank()) {
                setErrors(currentPasswordError = UiText.StringRes(R.string.err_current_password_required))
                return@launch
            }

            // 4) Verificamos que el nuevo correo sea distinto al actual
            // 5) Verificamos que el correo cumpla con el stándard
            CredentialValidators.validateEmail(trimmedNewEmail, state.currentEmail)?.let { error ->
                setErrors(newEmailError = error)
                return@launch
            }

            // 6) Verificamos que la constraseña ingresada sea correcta
            if (!authSessionActions.reauthenticate(currentPassword)) {
                setErrors(currentPasswordError = UiText.StringRes(R.string.err_incorrect_password))
                return@launch
            }

            updateEmailUseCase.execute(trimmedNewEmail)
                .onFailure { e ->
                    setErrors(newEmailError = getAuthErrorMessage(e))
                    return@launch
                }.onSuccess {
                    delay(config.navigationDelay)

                    appNavigator.finishFlowNavigateToSplash(
                        uiText = UiText.StringRes(R.string.settings_success_actualize_email),
                        snackType = ZibeSnackType.SUCCESS,
                    )

                }
        }
    }

    fun updatePassword(
        currentPassword: String,
        newPassword: String
    ) {
        viewModelScope.launch {

            setAction(SettingsAction.UPDATE_PASSWORD)

            val state = _uiState.value

            // 1) Verificamos que pueda cambiar las credenciales
            if (!state.canChangeCredentials) {
                val uiText = state.providerLabel.toUiText(
                    R.string.settings_err_provider,
                    R.string.message_err_not_available_generic
                )
                setErrors(generalSheetError = uiText)
                return@launch
            }

            // 2) Verificamos que currentPassword no esté vacío
            if (currentPassword.isBlank()) {
                setErrors(currentPasswordError = UiText.StringRes(R.string.err_current_password_required))
                return@launch
            }

            // 3) Verificamos que newPassword no esté vacío
            if (newPassword.isBlank()) {
                setErrors(newPasswordError = UiText.StringRes(R.string.err_new_password_required))
                return@launch
            }

            // 4) Verificamos que el email cumpla con el stándard
            CredentialValidators.validateNewPassword(newPassword, currentPassword)?.let { error ->
                setErrors(newPasswordError = error)
                return@launch
            }

            // 5) Verificamos que la constraseña ingresada sea correcta
            if (!authSessionActions.reauthenticate(currentPassword)) {
                setErrors(currentPasswordError = UiText.StringRes(R.string.err_incorrect_password))
                return@launch
            }

            updatePasswordUseCase.execute(newPassword)
                .onFailure { e ->
                    setErrors(newPasswordError = getAuthErrorMessage(e))
                    return@launch
                }.onSuccess {
                    delay(config.navigationDelay)

                    appNavigator.finishFlowNavigateToSplash(
                        uiText = UiText.StringRes(R.string.settings_success_actualize_password),
                        snackType = ZibeSnackType.SUCCESS,
                    )
                }
        }
    }

    fun onLogoutRequested(
    ) {
        viewModelScope.launch {

            setAction(SettingsAction.LOGOUT)

            logoutUseCase.execute()
                .onFailure { e ->
                    showSnack(
                        uiText = getAuthErrorMessage(e),
                        snackType = ZibeSnackType.ERROR
                    )
                }
                .onSuccess {
                    delay(config.navigationDelay)

                    appNavigator.finishFlowNavigateToSplash()
                }
        }
    }

    fun onIndividualNotificationsToggled(
        enabled: Boolean
    ) {
        viewModelScope.launch {
            userPreferencesActions.setIndividualNotifications(enabled)

            val statusRes = if (enabled) R.string.state_enabled else R.string.state_disabled

            val uiText = UiText.StringRes(
                resId = R.string.individual_notifications_status,
                args = listOf(statusRes)
            )

            showSnack(
                uiText = uiText,
                snackType = ZibeSnackType.SUCCESS
            )
        }
    }

    fun onGroupNotificationsToggled(
        enabled: Boolean
    ) {
        viewModelScope.launch { userPreferencesActions.setGroupNotifications(enabled) }

        val statusRes = if (enabled) R.string.state_enabled else R.string.state_disabled

        val uiText = UiText.StringRes(
            resId = R.string.group_notifications_status,
            args = listOf(statusRes)
        )

        showSnack(
            uiText = uiText,
            snackType = ZibeSnackType.SUCCESS
        )
    }

    fun sendFeedback(
        feedback: String
    ) {
        viewModelScope.launch {

            setAction(SettingsAction.SEND_FEEDBACK)

            val trimmedFeedback = feedback.trim()

            // 1) Verificamos que feedback no esté vacío
            if (trimmedFeedback.isBlank()) {
                setErrors(feedbackError = UiText.StringRes(R.string.err_feedback_required))
                return@launch
            }

            sendFeedbackUseCase.execute(
                feedback = trimmedFeedback,
                screen = SETTINGS_SCREEN
            ).onFailure { e ->
                setErrors(feedbackError = getAuthErrorMessage(e))
                return@launch
            }.onSuccess {
                delay(config.navigationDelay)
                appNavigator.finishFlowNavigateToSplash(
                    uiText = UiText.StringRes(R.string.feedback_sent),
                    snackType = ZibeSnackType.SUCCESS,
                )
            }
        }
    }

    fun deleteAccount(
        passwordIfNeeded: String?
    ) {
        viewModelScope.launch {

            setAction(SettingsAction.DELETE_ACCOUNT)

            if (_uiState.value.requiresReauthForSensitiveActions) {

                // 1) Verificamos que currentPassword no esté vacío
                if (passwordIfNeeded.orEmpty().isBlank()) {
                    setErrors(currentPasswordError = UiText.StringRes(R.string.err_current_password_required))
                    return@launch
                }

                // 2) Verificamos que la constraseña ingresada sea correcta
                if (!authSessionActions.reauthenticate(passwordIfNeeded)) {
                    setErrors(currentPasswordError = UiText.StringRes(R.string.err_incorrect_password))
                    return@launch
                }
            }

            delay(config.navigationDelay)

            appNavigator.finishFlowNavigateToSplash(
                deleteAccount = true
            )
        }
    }

    // ================= HELPERS =================

    fun setErrors(
        generalSheetError: UiText? = null,
        currentEmailError: UiText? = null,
        newEmailError: UiText? = null,
        currentPasswordError: UiText? = null,
        newPasswordError: UiText? = null,
        feedbackError: UiText? = null
    ) {
        _uiState.update {
            it.copy(
                generalSheetError = generalSheetError,
                newEmailError = newEmailError,
                currentPasswordError = currentPasswordError,
                newPasswordError = newPasswordError,
                feedbackError = feedbackError,
                loadingAction = null
            )
        }
    }

    fun cleanErrors(
    ) {
        _uiState.update {
            it.copy(
                loadingAction = null,
                generalSheetError = null,
                newEmailError = null,
                currentPasswordError = null,
                newPasswordError = null,
                feedbackError = null
            )
        }
    }

    fun setAction(
        loadingAction: SettingsAction
    ) {
        _uiState.update { it.copy(loadingAction = loadingAction) }
    }


    fun onEmailInputChanged(
        email: String,
        compareTo: String?
    ) {
        validationJob?.cancel()
        _uiState.update { it.copy(newEmailError = null) }

        if (email.isBlank()) return

        validationJob = viewModelScope.launch {
            delay(config.validationDebounce)
            val uiTextOrNull =
                CredentialValidators.validateEmail(email = email, compareTo = compareTo)
            _uiState.update { it.copy(newEmailError = uiTextOrNull) }
        }
    }

    fun onPasswordInputChanged(
        newValue: String,
        compareTo: String?
    ) {
        validationJob?.cancel()
        _uiState.update { it.copy(newPasswordError = null) }

        if (newValue.isBlank()) return

        validationJob = viewModelScope.launch {
            delay(config.validationDebounce)
            val uiTextOrNull =
                CredentialValidators.validateNewPassword(password = newValue, compareTo = compareTo)
            _uiState.update { it.copy(newPasswordError = uiTextOrNull) }
        }
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
}
