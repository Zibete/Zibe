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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.SETTINGS_SCREEN
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.navigation.NavAppEvent
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.validation.CredentialValidators
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
import com.zibete.proyecto1.ui.components.ZibeSnackbar
import com.zibete.proyecto1.ui.components.ZibeSwitchRow
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.launch

private enum class CredentialSheet { CHANGE_EMAIL, CHANGE_PASSWORD, DELETE_PASSWORD }

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenSendFeedback: () -> Unit,
    onNavigateToSplash: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    appNavigator: AppNavigator,
    snackBarManager: SnackBarManager
    ) {
    val state by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val snackHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        appNavigator.events.collect { event ->
            when (event) {
                NavAppEvent.FinishFlowNavigateToSplash -> onNavigateToSplash()
            }
        }
    }

    // ====== EVENTOS DEL SNACKBARMANAGER ======
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        snackBarManager.events.collect { event ->
            snackHostState.currentSnackbarData?.dismiss()
            snackHostState.showZibeMessage(
                message = event.uiText.asString(context),
                type = event.type
            )
        }
    }

    // SNACKBAR HOST
    Box(modifier = Modifier.fillMaxSize()) {

        SettingsScreen(
            state = state,
            onBack = onBack,
            onOpenSendFeedback = onOpenSendFeedback,
            onToggleIndividualNotifications = settingsViewModel::onIndividualNotificationsToggled,
            onToggleGroupNotifications = settingsViewModel::onGroupNotificationsToggled,
            onChangeEmail = { newEmail, currentPassword ->
                settingsViewModel.updateEmail(currentPassword = currentPassword, newEmail = newEmail)
            },
            onChangePassword = { currentPassword, newPassword ->
                settingsViewModel.updatePassword(password = currentPassword, newPassword = newPassword)
            },
            onLogout = settingsViewModel::onLogoutRequested,
            onDeleteAccount = { passwordOrNull ->
                settingsViewModel.deleteAccount(passwordIfNeeded = passwordOrNull)
            }
        )

        ZibeSnackbar(
            hostState = snackHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onOpenSendFeedback: () -> Unit,
    onToggleIndividualNotifications: (Boolean) -> Unit,
    onToggleGroupNotifications: (Boolean) -> Unit,
    onChangeEmail: (newEmail: String, currentPassword: String) -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: (passwordOrNull: String?) -> Unit
) {
    val zibeColors = LocalZibeExtendedColors.current
    val scope = rememberCoroutineScope()

    // ---- Resources ----
    val titleSettings = stringResource(R.string.menu_settings)
    val titleAccount = stringResource(R.string.account)
    val titleNotifications = stringResource(R.string.notifications)
    val actionChangeEmail = stringResource(R.string.change_email_title)
    val actionChangePassword = stringResource(R.string.change_password_title)
    val actionLogout = stringResource(R.string.logout)
    val actionSendFeedback = stringResource(R.string.send_feedback)
    val actionDeleteAccount = stringResource(R.string.delete_account)
    val actionSave = stringResource(R.string.action_save)

    val labelCurrentPassword = stringResource(R.string.current_password)
    val labelNewPassword = stringResource(R.string.new_password)
    val labelNewEmail = stringResource(R.string.new_email)

    val dialogLogoutTitle = stringResource(R.string.logout)
    val dialogLogoutMessage = stringResource(R.string.dialog_logout_message)
    val dialogDeleteTitle = stringResource(R.string.dialog_delete_account_title)
    val dialogDeleteMessage = stringResource(R.string.dialog_delete_account_message)
    val dialogDeleteFinalMessage = stringResource(R.string.dialog_delete_account_final_message)
    val attentionTitle = stringResource(R.string.attention_title)

    val screenPadding20 = dimensionResource(R.dimen.screen_padding)
    val elementSpacingXs8 = dimensionResource(R.dimen.element_spacing_xs)
    val elementSpacingXxs6 = dimensionResource(R.dimen.element_spacing_xxs)
    val elementSpacingSmall12 = dimensionResource(R.dimen.element_spacing_small)
    val elementSpacingMedium16 = dimensionResource(R.dimen.element_spacing_medium)

    // ---- Derived flags ----
    val isBusy = state.isLoading || state.isSaving
    val canEditCredentials = state.canChangeCredentials

    // ---- Dialogs + Sheets ----
    var showLogoutDialog by remember { mutableStateOf(false) }
    var deleteStep by remember { mutableIntStateOf(0) } // 0 none, 1 first confirm, 2 final confirm
    var infoProviderDialog by remember { mutableStateOf(false) }

    var credentialSheet by remember { mutableStateOf<CredentialSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ---- Inputs ----
    var emailInput by rememberSaveable { mutableStateOf("") }
    var passForEmail by rememberSaveable { mutableStateOf("") }

    var currentPass by rememberSaveable { mutableStateOf("") }
    var newPass by rememberSaveable { mutableStateOf("") }

    var deletePassword by rememberSaveable { mutableStateOf("") }

    val currentEmailOnly by remember(state.emailDisplay) {
        derivedStateOf { state.emailDisplay }
    }

    val deleteEnabled by remember(deletePassword, isBusy) {
        derivedStateOf { !isBusy && deletePassword.isNotBlank() }
    }

    val emailSaveEnabled by remember(
        emailInput,
        passForEmail,
        isBusy,
        canEditCredentials,
        currentEmailOnly
    ) {
        derivedStateOf {
            val newEmail = emailInput.trim()
            !isBusy &&
                    canEditCredentials &&
                    passForEmail.isNotBlank() &&
                    CredentialValidators.isValidEmail(newEmail) &&
                    newEmail != currentEmailOnly
        }
    }

    val passSaveEnabled by remember(currentPass, newPass, isBusy, canEditCredentials) {
        derivedStateOf {
            !isBusy &&
                    canEditCredentials &&
                    currentPass.isNotBlank() &&
                    CredentialValidators.isValidPassword(newPass)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = {
            ZibeToolbar(
                title = titleSettings,
                onBack = onBack
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalZibeExtendedColors.current.gradientZibe)
                .testTag(SETTINGS_SCREEN)
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = screenPadding20,
                        vertical = screenPadding20
                    )
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(elementSpacingMedium16)
            )
            {

                // =========================
                // CUENTA
                // =========================
                ZibeCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_person_24),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(elementSpacingXs8))
                        Text(text = titleAccount)
                    }

                    Spacer(Modifier.height(elementSpacingXxs6))

                    Text(
                        text = state.emailDisplay,
                        color = zibeColors.accent,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(elementSpacingXs8))

                    ActionRow(
                        title = actionChangeEmail,
                        enabled = !isBusy,
                        subtitle = if (!canEditCredentials) {
                            state.providerLabel.toUiText(
                                R.string.settings_err_provider,
                                R.string.err_not_available_title
                            ).asString()
                        } else null,
                        onClick = {
                            if (canEditCredentials) {
                                emailInput = ""
                                passForEmail = ""
                                credentialSheet = CredentialSheet.CHANGE_EMAIL
                                scope.launch { sheetState.show() }
                            } else {
                                infoProviderDialog = true
                            }
                        }
                    )

                    Spacer(Modifier.height(elementSpacingXs8))

                    ActionRow(
                        title = actionChangePassword,
                        enabled = !isBusy,
                        subtitle = if (!canEditCredentials) {
                            state.providerLabel.toUiText(
                                R.string.settings_err_provider,
                                R.string.err_not_available_title
                            ).asString()
                        } else null,
                        onClick = {
                            if (canEditCredentials) {
                                currentPass = ""
                                newPass = ""
                                credentialSheet = CredentialSheet.CHANGE_PASSWORD
                                scope.launch { sheetState.show() }
                            } else {
                                infoProviderDialog = true
                            }
                        }
                    )

                    Spacer(Modifier.height(elementSpacingMedium16))

                    // Logout
                    ZibeButtonOutlined(
                        text = actionLogout,
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy,
                        isLoading = isBusy
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
                        Spacer(Modifier.width(elementSpacingXs8))
                        Text(
                            text = titleNotifications
                        )
                    }

                    Spacer(Modifier.height(elementSpacingSmall12))

                    ZibeSwitchRow(
                        title = stringResource(R.string.settings_individual_notifications_on), // asumido
                        checked = state.individualNotificationsEnabled,
                        enabled = !isBusy,
                        onCheckedChange = onToggleIndividualNotifications
                    )

                    Spacer(Modifier.height(elementSpacingXs8))

                    ZibeSwitchRow(
                        title = stringResource(R.string.settings_group_notifications_on), // asumido
                        checked = state.groupNotificationsEnabled,
                        enabled = !isBusy,
                        onCheckedChange = onToggleGroupNotifications
                    )
                }

                // ABOUT / APP
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

                        Column(
                            horizontalAlignment = Alignment.Start
                        )
                        {
                            Text(
                                text = stringResource(R.string.app_name), // asumido (o "Zibe App")
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.version_string),
                                style = MaterialTheme.typography.bodySmall,
                                color = zibeColors.hintText
                            )
                            Text(
                                text = stringResource(R.string.version),
                                style = MaterialTheme.typography.bodySmall,
                                color = zibeColors.hintText
                            )
                            Text(
                                text = stringResource(R.string.company_name), // asumido (o "ZibeteProjects")
                                style = MaterialTheme.typography.bodySmall,
                                color = zibeColors.hintText
                            )
                        }


                    }

                    Spacer(Modifier.height(elementSpacingMedium16))

                    ZibeButtonOutlined(
                        text = actionSendFeedback,
                        onClick = onOpenSendFeedback,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy,
                        isLoading = isBusy
                    )

                    Spacer(Modifier.height(elementSpacingXs8))

                    ZibeButtonOutlined(
                        text = actionDeleteAccount,
                        onClick = { deleteStep = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy,
                        isLoading = isBusy,
                        buttonColors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }

    // Logout confirm dialog
    if (showLogoutDialog) {
        ZibeDialog(
            title = dialogLogoutTitle,
            content = { Text(dialogLogoutMessage) },
            confirmText = actionLogout,
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onCancel = { showLogoutDialog = false }
        )
    }

    // Delete flow (2 pasos + password opcional)
    if (deleteStep == 1) {
        ZibeDialog(
            title = dialogDeleteTitle,
            content = { Text(dialogDeleteMessage) },
            confirmText = stringResource(R.string.action_yes),
            onConfirm = { deleteStep = 2 },
            onCancel = { deleteStep = 0 }
        )
    }

    if (deleteStep == 2) {
        ZibeDialog(
            title = attentionTitle,
            content = { Text(dialogDeleteFinalMessage) },
            confirmText = stringResource(R.string.action_delete_account),
            onConfirm = {
                deleteStep = 0
                if (state.requiresPasswordForSensitiveActions) {
                    deletePassword = ""
                    credentialSheet = CredentialSheet.DELETE_PASSWORD
                    scope.launch { sheetState.show() }
                } else {
                    onDeleteAccount(null)
                }
            },
            onCancel = { deleteStep = 0 }
        )
    }

    // Provider info dialog (cuando no puede cambiar credenciales)
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

    // Bottom Sheets (Change email / Change password / Delete account with password)
    ZibeBottomSheet(
        isOpen = credentialSheet != null,
        onCancel = { credentialSheet = null },
        sheetState = sheetState,
        content = {

            when (credentialSheet) {
                CredentialSheet.CHANGE_EMAIL -> {
                    SheetHeader(
                        title = actionChangeEmail,
                        subtitle = stringResource(R.string.change_email_message)
                    )

                    Spacer(Modifier.height(elementSpacingSmall12))

                    ZibeInputField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = labelNewEmail,
                        enabled = !isBusy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mail_24),
                                contentDescription = stringResource(id = R.string.email)
                            )
                        },
                    )

                    Spacer(Modifier.height(10.dp))

                    var visiblePassword by rememberSaveable { mutableStateOf(false) }

                    ZibeInputPasswordField(
                        value = passForEmail,
                        onValueChange = { passForEmail = it },
                        label = labelCurrentPassword,
                        enabled = !isBusy,
                        visible = visiblePassword,
                        onToggleVisible = { visiblePassword = !visiblePassword }
                    )

                    Spacer(Modifier.height(16.dp))

                    SheetActions(
                        onCancel = {
                            credentialSheet = null
                            scope.launch { sheetState.hide() }
                        },
                        confirmEnabled = emailSaveEnabled,
                        confirmText = actionSave,
                        onConfirm = {
                            onChangeEmail(emailInput.trim(), passForEmail)
                        }
                    )
                }

                CredentialSheet.CHANGE_PASSWORD -> {
                    SheetHeader(
                        title = actionChangePassword,
                        subtitle = stringResource(
                            R.string.change_password_message,
                            MIN_PASSWORD_LEN
                        )
                    )

                    Spacer(Modifier.height(elementSpacingSmall12))

                    var visibleCurrentPassword by rememberSaveable { mutableStateOf(false) }
                    var visibleNewPassword by rememberSaveable { mutableStateOf(false) }

                    ZibeInputPasswordField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = labelCurrentPassword,
                        enabled = !isBusy,
                        visible = visibleCurrentPassword,
                        onToggleVisible = { visibleCurrentPassword = !visibleCurrentPassword }
                    )

                    Spacer(Modifier.height(10.dp))

                    ZibeInputPasswordField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = labelNewPassword,
                        enabled = !isBusy,
                        visible = visibleNewPassword,
                        onToggleVisible = { visibleNewPassword = !visibleNewPassword }
                    )

                    Spacer(Modifier.height(16.dp))

                    SheetActions(
                        onCancel = {
                            credentialSheet = null
                            scope.launch { sheetState.hide() }
                        },
                        confirmEnabled = passSaveEnabled,
                        confirmText = actionSave,
                        onConfirm = {
                            onChangePassword(currentPass, newPass)
                        }
                    )
                }

                CredentialSheet.DELETE_PASSWORD -> {
                    SheetHeader(
                        title = actionDeleteAccount,
                        subtitle = stringResource(R.string.password_is_required)
                    )

                    Spacer(Modifier.height(elementSpacingSmall12))

                    var visiblePassword by rememberSaveable { mutableStateOf(false) }

                    ZibeInputPasswordField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = labelCurrentPassword,
                        enabled = !isBusy,
                        visible = visiblePassword,
                        onToggleVisible = { visiblePassword = !visiblePassword }
                    )

                    Spacer(Modifier.height(16.dp))

                    SheetActions(
                        onCancel = {
                            credentialSheet = null
                            scope.launch { sheetState.hide() }
                        },
                        confirmEnabled = deleteEnabled,
                        confirmText = stringResource(R.string.action_delete_account),
                        onConfirm = {
                            onDeleteAccount(deletePassword)
                        }
                    )
                }

                else -> {}
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.bottom_sheet_bottom_padding)))
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ZibeTheme {
        SettingsScreen(
            state = SettingsUiState(
                emailDisplay = "user@example.com",
                providerLabel = "Google",
                canChangeCredentials = false,
                individualNotificationsEnabled = true,
                groupNotificationsEnabled = false
            ),
            onBack = {},
            onOpenSendFeedback = {},
            onToggleIndividualNotifications = {},
            onToggleGroupNotifications = {},
            onChangeEmail = { _, _ -> },
            onChangePassword = { _, _ -> },
            onLogout = {},
            onDeleteAccount = {}
        )
    }
}
