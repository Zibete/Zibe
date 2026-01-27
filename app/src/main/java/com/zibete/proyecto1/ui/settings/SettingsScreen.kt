package com.zibete.proyecto1.ui.settings

import LocalZibeExtendedColors
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.SETTINGS_SCREEN
import com.zibete.proyecto1.core.navigation.NavAppEvent
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.validation.CredentialValidators.MIN_PASSWORD_LEN
import com.zibete.proyecto1.ui.components.ActionRow
import com.zibete.proyecto1.ui.components.SheetActions
import com.zibete.proyecto1.ui.components.SheetHeader
import com.zibete.proyecto1.ui.components.ZibeBottomSheet
import com.zibete.proyecto1.ui.components.ZibeButtonOutlined
import com.zibete.proyecto1.ui.components.ZibeCard
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibeInputPasswordField
import com.zibete.proyecto1.ui.components.ZibeMessageDialog
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.components.ZibeSnackbar
import com.zibete.proyecto1.ui.components.ZibeSwitchRow
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onNavigateToSplash: (UiText?, ZibeSnackType?, Boolean, Boolean) -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val state by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val snackHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        settingsViewModel.appNavigatorEvents.collect { event ->
            when (event) {
                is NavAppEvent.FinishFlowNavigateToSplash -> {
                    onNavigateToSplash(
                        event.snackMessage,
                        event.snackType,
                        event.deleteAccount,
                        event.sessionConflict
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.snackBarEvents.collectLatest { event ->
            snackHostState.showZibeMessage(
                message = event.uiText.asString(context),
                type = event.type
            )
        }
    }

    SettingsScreen(
        state = state,
        snackHostState = snackHostState,
        onBack = onBack,
        onEmailInputChanged = settingsViewModel::onEmailInputChanged,
        onPasswordInputChanged = settingsViewModel::onPasswordInputChanged,
        cleanErrors = settingsViewModel::cleanErrors,
        onChangeEmail = { newEmail, currentPassword ->
            settingsViewModel.updateEmail(
                currentPassword = currentPassword,
                newEmail = newEmail
            )
        },
        onChangePassword = { currentPassword, newPassword ->
            settingsViewModel.updatePassword(
                currentPassword = currentPassword,
                newPassword = newPassword
            )
        },
        onLogout = settingsViewModel::onLogoutRequested,
        onToggleIndividualNotifications = settingsViewModel::onIndividualNotificationsToggled,
        onToggleGroupNotifications = settingsViewModel::onGroupNotificationsToggled,
        onSendFeedback = { feedback ->
            settingsViewModel.sendFeedback(feedback)
        }
    ) { passwordOrNull ->
        settingsViewModel.deleteAccount(passwordIfNeeded = passwordOrNull)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    snackHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEmailInputChanged: (email: String, compareTo: String?) -> Unit,
    onPasswordInputChanged: (password: String, compareTo: String?) -> Unit,
    cleanErrors: () -> Unit,
    onChangeEmail: (newEmail: String, currentPassword: String) -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    onLogout: () -> Unit,
    onToggleIndividualNotifications: (Boolean) -> Unit,
    onToggleGroupNotifications: (Boolean) -> Unit,
    onSendFeedback: (feedback: String) -> Unit,
    onDeleteAccount: (passwordOrNull: String?) -> Unit
) {
    val zibeColors = LocalZibeExtendedColors.current
    val scope = rememberCoroutineScope()

    // ---- Dimens ----
    val spacingXs8 = dimensionResource(R.dimen.element_spacing_xs)
    val spacingMd16 = dimensionResource(R.dimen.element_spacing_medium)

    // ---- Flags ----
    val isBusy = state.loadingAction != null
    val canEditCredentials = state.canChangeCredentials

    // ---- Dialogs + Sheet ----
    var showLogoutDialog by remember { mutableStateOf(false) }
    var deleteStep by remember { mutableIntStateOf(0) }
    var infoProviderDialog by remember { mutableStateOf(false) }

    var settingsAction by remember { mutableStateOf<SettingsAction?>(null) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ---- Inputs ----
    var newEmailInput by rememberSaveable { mutableStateOf("") }
    var currentPasswordForEmail by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var currentPasswordForPassword by rememberSaveable { mutableStateOf("") }
    var currentPasswordForDelete by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf("") }

    var visibleCurrentPasswordForEmail by rememberSaveable { mutableStateOf(false) }
    var visibleCurrentPasswordForPassword by rememberSaveable { mutableStateOf(false) }
    var visibleNewPassword by rememberSaveable { mutableStateOf(false) }
    var visiblePasswordForDelete by rememberSaveable { mutableStateOf(false) }

    val deleteEnabled by remember(currentPasswordForDelete, isBusy) {
        derivedStateOf { !isBusy && currentPasswordForDelete.isNotBlank() }
    }

    val sendFeedbackEnabled by remember(feedback, isBusy) {
        derivedStateOf { !isBusy && feedback.trim().isNotEmpty() }
    }

    val emailSaveEnabled by remember(newEmailInput, currentPasswordForEmail, isBusy) {
        derivedStateOf {
            !isBusy &&
                    currentPasswordForEmail.isNotBlank() &&
                    newEmailInput.trim().isNotEmpty()
        }
    }

    val passSaveEnabled by remember(currentPasswordForPassword, newPassword, isBusy) {
        derivedStateOf {
            !isBusy &&
                    currentPasswordForPassword.isNotBlank() &&
                    newPassword.isNotBlank()
        }
    }

    val handleSheetCancel: () -> Unit = {
        scope.launch {
            bottomSheetState.hide()
            settingsAction = null
            cleanErrors()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = {
            ZibeToolbar(
                title = stringResource(R.string.menu_settings),
                onBack = onBack
            )
        },
        snackbarHost = {
            ZibeSnackbar(
                hostState = snackHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = dimensionResource(R.dimen.element_spacing_medium))
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(zibeColors.gradientZibe)
                .testTag(SETTINGS_SCREEN)
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = dimensionResource(R.dimen.screen_padding),
                        vertical = dimensionResource(R.dimen.screen_padding)
                    )
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(spacingMd16)
            ) {

                // =========================
                // CUENTA
                // =========================
                ZibeCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_person_24),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(spacingXs8))
                        Text(text = stringResource(R.string.account))
                    }

                    Spacer(Modifier.height(dimensionResource(R.dimen.element_spacing_xxs)))

                    Text(
                        text = state.currentEmail.orEmpty(),
                        color = zibeColors.accent,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(spacingXs8))

                    ActionRow(
                        title = stringResource(R.string.change_email_title),
                        enabled = !isBusy,
                        subtitle = if (!canEditCredentials) {
                            state.providerLabel.toUiText(
                                R.string.settings_err_provider,
                                R.string.err_not_available_title
                            ).asString()
                        } else null,
                        onClick = {
                            if (canEditCredentials) {
                                newEmailInput = ""
                                currentPasswordForEmail = ""
                                settingsAction = SettingsAction.UPDATE_EMAIL
                                scope.launch {
                                    bottomSheetState.show()
                                    bottomSheetState.expand()
                                }
                            } else {
                                infoProviderDialog = true
                            }
                        }
                    )

                    Spacer(Modifier.height(spacingXs8))

                    ActionRow(
                        title = stringResource(R.string.change_password_title),
                        enabled = !isBusy,
                        subtitle = if (!canEditCredentials) {
                            state.providerLabel.toUiText(
                                R.string.settings_err_provider,
                                R.string.err_not_available_title
                            ).asString()
                        } else null,
                        onClick = {
                            if (canEditCredentials) {
                                currentPasswordForPassword = ""
                                newPassword = ""
                                settingsAction = SettingsAction.UPDATE_PASSWORD
                                scope.launch {
                                    bottomSheetState.show()
                                    bottomSheetState.expand()
                                }
                            } else {
                                infoProviderDialog = true
                            }
                        }
                    )

                    Spacer(Modifier.height(spacingMd16))

                    ZibeButtonOutlined(
                        text = stringResource(R.string.logout),
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy,
                        isLoading = state.loadingAction == SettingsAction.LOGOUT
                    )
                }

                // =========================
                // NOTIFICACIONES
                // =========================
                ZibeCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notifications_black_24dp),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(spacingXs8))
                        Text(text = stringResource(R.string.notifications))
                    }

                    Spacer(Modifier.height(dimensionResource(R.dimen.element_spacing_small)))

                    ZibeSwitchRow(
                        title = stringResource(R.string.individual_notifications),
                        supportingText = stringResource(R.string.individual_notifications_supporting),
                        checked = state.individualNotificationsEnabled,
                        enabled = !isBusy,
                        onCheckedChange = onToggleIndividualNotifications
                    )

                    Spacer(Modifier.height(spacingXs8))

                    ZibeSwitchRow(
                        title = stringResource(R.string.group_notifications),
                        supportingText = stringResource(R.string.group_notifications_supporting),
                        checked = state.groupNotificationsEnabled,
                        enabled = !isBusy,
                        onCheckedChange = onToggleGroupNotifications
                    )
                }

                // =========================
                // ABOUT / APP
                // =========================
                ZibeCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.mipmap.logo_zibe),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp)
                        )

                        Spacer(Modifier.width(14.dp))

                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.version_string, state.appVersion),
                                style = MaterialTheme.typography.bodySmall,
                                color = zibeColors.hintText
                            )
                            Text(
                                text = stringResource(R.string.company_name),
                                style = MaterialTheme.typography.bodySmall,
                                color = zibeColors.hintText
                            )
                        }
                    }

                    Spacer(Modifier.height(spacingMd16))

                    ZibeButtonOutlined(
                        text = stringResource(R.string.send_feedback),
                        onClick = {
                            feedback = ""
                            settingsAction = SettingsAction.SEND_FEEDBACK
                            scope.launch {
                                bottomSheetState.show()
                                bottomSheetState.expand()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy,
                        isLoading = false
                    )

                    Spacer(Modifier.height(spacingXs8))

                    ZibeButtonOutlined(
                        text = stringResource(R.string.delete_account),
                        onClick = {
                            currentPasswordForDelete = ""
                            deleteStep = 1
                        },
                        modifier = Modifier.fillMaxWidth(),
                        buttonColors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !isBusy,
                        isLoading = false
                    )
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }

    // =========================
    // DIALOGS
    // =========================
    if (showLogoutDialog) {
        ZibeDialog(
            title = stringResource(R.string.logout),
            content = { Text(stringResource(R.string.dialog_logout_message)) },
            confirmText = stringResource(R.string.logout),
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onCancel = { showLogoutDialog = false }
        )
    }

    if (deleteStep == 1) {
        ZibeDialog(
            title = stringResource(R.string.dialog_delete_account_title),
            content = { Text(stringResource(R.string.dialog_delete_account_message)) },
            confirmText = stringResource(R.string.action_yes),
            onConfirm = { deleteStep = 2 },
            onCancel = { deleteStep = 0 }
        )
    }

    if (deleteStep == 2) {
        ZibeDialog(
            title = stringResource(R.string.attention_title),
            content = { Text(stringResource(R.string.dialog_delete_account_final_message)) },
            confirmText = stringResource(R.string.action_delete_account),
            onConfirm = {
                deleteStep = 0
                if (state.requiresReauthForSensitiveActions) {
                    settingsAction = SettingsAction.DELETE_ACCOUNT
                    scope.launch {
                        bottomSheetState.show()
                        bottomSheetState.expand()
                    }
                } else {
                    onDeleteAccount(null)
                }
            },
            onCancel = { deleteStep = 0 }
        )
    }

    if (infoProviderDialog) {
        ZibeMessageDialog(
            title = state.providerLabel.toUiText(
                R.string.settings_err_provider_title,
                R.string.err_not_available_title
            ).asString(),
            textContent = {
                Text(
                    state.providerLabel.toUiText(
                        R.string.settings_err_provider_message,
                        R.string.message_err_not_available_generic
                    ).asString()
                )
            },
            onConfirm = { infoProviderDialog = false }
        )
    }

    // =========================
    // BOTTOM SHEET
    // =========================
    ZibeBottomSheet(
        isOpen = settingsAction != null,
        onCancel = handleSheetCancel,
        sheetState = bottomSheetState,
        content = {
            when (settingsAction) {
                SettingsAction.UPDATE_EMAIL -> {
                    SheetHeader(
                        title = stringResource(R.string.change_email_title),
                        subtitle = stringResource(R.string.change_email_message)
                    )

                    state.generalSheetError?.let {
                        Text(
                            it.asString(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    ZibeInputField(
                        value = newEmailInput,
                        onValueChange = {
                            newEmailInput = it
                            cleanErrors()
                            onEmailInputChanged(it, state.currentEmail)
                        },
                        label = stringResource(R.string.new_email),
                        enabled = !isBusy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mail_24),
                                contentDescription = stringResource(id = R.string.email)
                            )
                        },
                        error = state.newEmailError?.asString()
                    )

                    ZibeInputPasswordField(
                        value = currentPasswordForEmail,
                        onValueChange = {
                            currentPasswordForEmail = it
                            cleanErrors()
                        },
                        label = stringResource(R.string.current_password),
                        enabled = !isBusy,
                        visible = visibleCurrentPasswordForEmail,
                        onToggleVisible = {
                            visibleCurrentPasswordForEmail = !visibleCurrentPasswordForEmail
                        },
                        error = state.currentPasswordError?.asString()
                    )

                    SheetActions(
                        onCancel = handleSheetCancel,
                        confirmEnabled = emailSaveEnabled,
                        confirmText = stringResource(R.string.action_save),
                        onConfirm = {
                            onChangeEmail(
                                newEmailInput.trim(),
                                currentPasswordForEmail
                            )
                        },
                        isConfirmLoading = state.loadingAction == SettingsAction.UPDATE_EMAIL
                    )
                }

                SettingsAction.UPDATE_PASSWORD -> {
                    SheetHeader(
                        title = stringResource(R.string.change_password_title),
                        subtitle = stringResource(
                            R.string.change_password_message,
                            MIN_PASSWORD_LEN
                        )
                    )

                    state.generalSheetError?.let {
                        Text(
                            it.asString(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    ZibeInputPasswordField(
                        value = currentPasswordForPassword,
                        onValueChange = {
                            currentPasswordForPassword = it
                            cleanErrors()
                        },
                        label = stringResource(R.string.current_password),
                        enabled = !isBusy,
                        visible = visibleCurrentPasswordForPassword,
                        onToggleVisible = {
                            visibleCurrentPasswordForPassword = !visibleCurrentPasswordForPassword
                        },
                        error = state.currentPasswordError?.asString()
                    )

                    ZibeInputPasswordField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            cleanErrors()
                            onPasswordInputChanged(it, currentPasswordForPassword)
                        },
                        label = stringResource(R.string.new_password),
                        enabled = !isBusy,
                        visible = visibleNewPassword,
                        onToggleVisible = { visibleNewPassword = !visibleNewPassword },
                        error = state.newPasswordError?.asString()
                    )

                    SheetActions(
                        onCancel = handleSheetCancel,
                        confirmEnabled = passSaveEnabled,
                        confirmText = stringResource(R.string.action_save),
                        onConfirm = { onChangePassword(currentPasswordForPassword, newPassword) },
                        isConfirmLoading = state.loadingAction == SettingsAction.UPDATE_PASSWORD
                    )
                }

                SettingsAction.DELETE_ACCOUNT -> {
                    SheetHeader(
                        title = stringResource(R.string.delete_account),
                        subtitle = stringResource(R.string.password_is_required)
                    )

                    ZibeInputPasswordField(
                        value = currentPasswordForDelete,
                        onValueChange = {
                            currentPasswordForDelete = it
                            cleanErrors()
                        },
                        label = stringResource(R.string.current_password),
                        enabled = !isBusy,
                        visible = visiblePasswordForDelete,
                        onToggleVisible = { visiblePasswordForDelete = !visiblePasswordForDelete },
                        error = state.currentPasswordError?.asString()
                    )

                    SheetActions(
                        onCancel = handleSheetCancel,
                        confirmEnabled = deleteEnabled,
                        confirmText = stringResource(R.string.action_delete_account),
                        onConfirm = { onDeleteAccount(currentPasswordForDelete) },
                        isConfirmLoading = state.loadingAction == SettingsAction.DELETE_ACCOUNT
                    )
                }

                SettingsAction.SEND_FEEDBACK -> {
                    val sendFeedbackTitle = stringResource(
                        R.string.send_feedback_title,
                        stringResource(R.string.app_name)
                    )

                    SheetHeader(
                        title = sendFeedbackTitle,
                        subtitle = stringResource(R.string.send_feedback_subtitle)
                    )

                    state.generalSheetError?.let {
                        Text(
                            it.asString(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    ZibeInputField(
                        value = feedback,
                        onValueChange = {
                            feedback = it
                            cleanErrors()
                        },
                        label = sendFeedbackTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        singleLine = false,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_edit_24),
                                contentDescription = sendFeedbackTitle
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Default,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        enabled = !isBusy,
                        error = state.feedbackError?.asString()
                    )

                    SheetActions(
                        onCancel = handleSheetCancel,
                        confirmEnabled = sendFeedbackEnabled,
                        confirmText = stringResource(R.string.action_send),
                        onConfirm = { onSendFeedback(feedback) },
                        isConfirmLoading = state.loadingAction == SettingsAction.SEND_FEEDBACK
                    )
                }

                else -> Unit
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ZibeTheme {
        SettingsScreen(
            state = SettingsUiState(
                currentEmail = "example@zibe.com",
                appVersion = "1.0.0",
                individualNotificationsEnabled = true,
                groupNotificationsEnabled = false
            ),
            snackHostState = remember { SnackbarHostState() },
            onBack = {},
            onEmailInputChanged = { _, _ -> },
            onPasswordInputChanged = { _, _ -> },
            cleanErrors = {},
            onChangeEmail = { _, _ -> },
            onChangePassword = { _, _ -> },
            onLogout = {},
            onToggleIndividualNotifications = {},
            onToggleGroupNotifications = {},
            onSendFeedback = {},
        ) {}
    }
}