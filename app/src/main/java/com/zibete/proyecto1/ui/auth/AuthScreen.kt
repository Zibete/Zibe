package com.zibete.proyecto1.ui.auth

import LocalZibeExtendedColors
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.navigation.NavAppEvent
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.components.SheetActions
import com.zibete.proyecto1.ui.components.SheetHeader
import com.zibete.proyecto1.ui.components.ZibeBottomSheet
import com.zibete.proyecto1.ui.components.ZibeButtonOutlined
import com.zibete.proyecto1.ui.components.ZibeButtonPrimary
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibeInputFieldDark
import com.zibete.proyecto1.ui.components.ZibeInputPasswordFieldDark
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    state: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onEmailInputChanged: (String) -> Unit,
    onResetEmailInputChanged: (String) -> Unit,
    onPasswordInputChanged: (String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onResetPassword: (String) -> Unit,
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onDoNotDelete: () -> Unit,
    onDeleteAccount: () -> Unit,
    authEvents: SharedFlow<AuthUiEvent>,
    onNavigateToSplash: (uiText: UiText?, snackType: ZibeSnackType?) -> Unit,
    appNavigator: AppNavigator
) {
    // Inputs
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by rememberSaveable { mutableStateOf("") }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val zibeColors = LocalZibeExtendedColors.current
    val lightText = zibeColors.lightText.copy(alpha = 0.8f)

    // Dimens
    val spacingXs = dimensionResource(R.dimen.element_spacing_xs)
    val spacingSm = dimensionResource(R.dimen.element_spacing_small)
    val inputPadding = dimensionResource(R.dimen.zibe_input_padding)

    val loginEnabled by remember(email, password, state.isLoadingLogin) {
        derivedStateOf {
            email.trim().isNotEmpty() && password.isNotEmpty() && !state.isLoadingLogin
        }
    }

    val resetEmailEnabled by remember(resetEmail, state.isLoadingResetPassword) {
        derivedStateOf {
            resetEmail.trim().isNotEmpty() && !state.isLoadingResetPassword
        }
    }

    val hideResetPasswordSheet: () -> Unit = {
        if (!state.isLoadingResetPassword) {
            scope.launch {
                bottomSheetState.hide()
                showResetDialog = false
            }
        }
    }

    LaunchedEffect(Unit) {
        appNavigator.events.collect { event ->
            when (event) {
                is NavAppEvent.FinishFlowNavigateToSplash -> {
                    onNavigateToSplash(
                        event.snackMessage,
                        event.snackType
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        authEvents.collect { event ->
            when (event) {
                is AuthUiEvent.NavigateToSignUp -> onNavigateToSignUp()
                is AuthUiEvent.CloseResetPasswordSheet -> {
                    bottomSheetState.hide()
                    showResetDialog = false
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(AUTH_SCREEN)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = dimensionResource(R.dimen.screen_padding),
                        vertical = dimensionResource(R.dimen.screen_padding)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // LOGO
                Image(
                    painter = painterResource(id = R.mipmap.logo_zibe),
                    contentDescription = stringResource(R.string.logo_content_desc),
                    modifier = Modifier.padding(
                        start = dimensionResource(R.dimen.element_spacing_xl),
                        end = dimensionResource(R.dimen.element_spacing_xl),
                        bottom = spacingXs,
                        top = 0.dp
                    )
                )

                if (!state.deleteAccount) {
                    // GOOGLE
                    ZibeButtonPrimary(
                        text = stringResource(R.string.continue_with_google),
                        iconRes = R.drawable.ic_google,
                        iconTint = Color.Unspecified,
                        onClick = onGoogleClick
                    )

                    Spacer(modifier = Modifier.height(spacingXs))

                    // FACEBOOK
                    ZibeButtonPrimary(
                        text = stringResource(R.string.continue_with_facebook),
                        iconRes = R.drawable.ic_facebook,
                        iconTint = Color.Unspecified,
                        onClick = onFacebookClick
                    )

                    Spacer(modifier = Modifier.height(spacingXs))

                    Text(
                        text = stringResource(R.string.auth_or_use_account),
                        style = LocalZibeTypography.current.label,
                        color = lightText
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.element_spacing_medium)))

                    // EMAIL
                    ZibeInputFieldDark(
                        value = email,
                        onValueChange = {
                            email = it
                            onEmailInputChanged(it)
                        },
                        label = stringResource(id = R.string.email),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mail_24),
                                contentDescription = stringResource(id = R.string.email)
                            )
                        },
                        enabled = !state.isLoadingLogin,
                        error = state.emailError?.asString(context)
                    )

                    Spacer(modifier = Modifier.height(inputPadding))

                    // PASSWORD
                    ZibeInputPasswordFieldDark(
                        value = password,
                        onValueChange = {
                            password = it
                            onPasswordInputChanged(it)
                        },
                        label = stringResource(id = R.string.password),
                        enabled = !state.isLoadingLogin,
                        visible = passwordVisible,
                        onToggleVisible = { passwordVisible = !passwordVisible },
                        error = state.passwordError?.asString(context)
                    )

                    Text(
                        text = stringResource(R.string.auth_forgot_password),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp, bottom = spacingSm)
                            .clickable {
                                showResetDialog = true
                            },
                        style = LocalZibeTypography.current.actionLabel,
                        color = lightText
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.element_spacing_medium)))

                    ZibeButtonPrimary(
                        text = stringResource(id = R.string.action_login),
                        onClick = { onLogin(email.trim(), password) },
                        modifier = Modifier.padding(
                            top = spacingXs,
                            bottom = spacingSm
                        ),
                        enabled = loginEnabled,
                        isLoading = state.isLoadingLogin
                    )

                    Spacer(modifier = Modifier.height(spacingSm))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.auth_do_not_have_account),
                            style = LocalZibeTypography.current.label,
                            color = lightText
                        )

                        Spacer(modifier = Modifier.width(spacingXs))

                        Text(
                            text = stringResource(R.string.action_register),
                            modifier = Modifier
                                .clickable { onNavigateToSignUp() }
                                .testTag(Constants.TestTags.BTN_REGISTER),
                            style = LocalZibeTypography.current.actionLabel,
                            color = lightText
                        )
                    }
                } else {
                    // Modo "reactivar cuenta"
                    ZibeButtonPrimary(
                        text = stringResource(R.string.auth_do_not_delete_account),
                        onClick = { onDoNotDelete() },
                        modifier = Modifier.padding(
                            top = spacingXs,
                            bottom = spacingSm
                        ),
                        enabled = !state.isLoadingDoNotDelete,
                        isLoading = state.isLoadingDoNotDelete
                    )

                    ZibeButtonOutlined(
                        text = stringResource(R.string.delete_account),
                        onClick = { onDeleteAccount() },
                        modifier = Modifier.padding(
                            top = spacingXs,
                            bottom = spacingSm
                        ),
                        buttonColors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !state.isLoadingDeleteAccount,
                        isLoading = state.isLoadingDeleteAccount
                    )
                }
            }

            ZibeBottomSheet(
                isOpen = showResetDialog,
                onCancel = hideResetPasswordSheet,
                sheetState = bottomSheetState,
                content = {
                    SheetHeader(
                        title = stringResource(R.string.reset_password_title),
                        subtitle = stringResource(R.string.reset_password_content)
                    )

                    ZibeInputField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            onResetEmailInputChanged(it)
                        },
                        label = stringResource(id = R.string.email),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mail_24),
                                contentDescription = stringResource(id = R.string.email)
                            )
                        },
                        enabled = !state.isLoadingResetPassword,
                        error = state.resetPasswordEmailError?.asString(context)
                    )

                    SheetActions(
                        onCancel = hideResetPasswordSheet,
                        confirmEnabled = resetEmailEnabled,
                        confirmText = stringResource(R.string.action_send_email),
                        onConfirm = {
                            onResetPassword(resetEmail)
                        },
                        isConfirmLoading = state.isLoadingResetPassword
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreenPreview() {
    ZibeTheme {
        AuthScreen(
            state = AuthUiState(),
            onLogin = { _, _ -> },
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onNavigateToSignUp = {},
            onResetPassword = { _ -> },
            onGoogleClick = {},
            onFacebookClick = {},
            onDoNotDelete = {},
            onDeleteAccount = {},
            authEvents = MutableSharedFlow(),
            onNavigateToSplash = { _, _ -> Unit },
            appNavigator = AppNavigator(),
            onResetEmailInputChanged = { _ -> }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreenDeleteAccountPreview() {
    ZibeTheme {
        AuthScreen(
            state = AuthUiState(deleteAccount = true),
            onLogin = { _, _ -> },
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onNavigateToSignUp = {},
            onResetPassword = { _ -> },
            onGoogleClick = {},
            onFacebookClick = {},
            onDoNotDelete = {},
            onDeleteAccount = {},
            authEvents = MutableSharedFlow(),
            onNavigateToSplash = { _, _ -> Unit },
            appNavigator = AppNavigator(),
            onResetEmailInputChanged = { _ -> }
        )
    }
}
