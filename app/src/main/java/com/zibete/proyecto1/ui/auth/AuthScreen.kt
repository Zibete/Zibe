package com.zibete.proyecto1.ui.auth

import LocalZibeExtendedColors
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.navigation.NavAppEvent
import com.zibete.proyecto1.ui.components.ZibeButtonOutlined
import com.zibete.proyecto1.ui.components.ZibeButtonPrimary
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibeInputFieldDark
import com.zibete.proyecto1.ui.components.ZibeInputPasswordFieldDark
import com.zibete.proyecto1.ui.components.ZibeSnackbar
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun AuthScreen(
    deleteUser: Boolean,
    onLogin: (email: String, password: String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onResetPassword: (String) -> Unit,
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onDoNotDelete: () -> Unit,
    onDeleteAccount: () -> Unit,
    authEvents: SharedFlow<AuthUiEvent>,
    isLoading: Boolean,
    onNavigateToSplash: () -> Unit,
    appNavigator: AppNavigator
) {
    // Inputs
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by rememberSaveable { mutableStateOf("") }

    val zibeColors = LocalZibeExtendedColors.current
    val lightText = zibeColors.lightText.copy(alpha = 0.8f)

    // Dimens
    val spacingXs = dimensionResource(R.dimen.element_spacing_xs)
    val spacingSm = dimensionResource(R.dimen.element_spacing_small)

    val snackHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        appNavigator.events.collect { event ->
            when (event) {
                NavAppEvent.FinishFlowNavigateToSplash -> onNavigateToSplash()
            }
        }
    }

    LaunchedEffect(Unit) {
        authEvents.collect { event ->
            when (event) {
                is AuthUiEvent.NavigateToSignUp -> onNavigateToSignUp()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { ZibeSnackbar(hostState = snackHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(zibeColors.gradientZibe)
                .testTag(AUTH_SCREEN)
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(
                        horizontal = dimensionResource(R.dimen.screen_padding),
                        vertical = dimensionResource(R.dimen.screen_padding)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
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

                if (!deleteUser) {
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
                        style = MaterialTheme.typography.headlineSmall,
                        color = lightText,
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.element_spacing_medium)))

                    // EMAIL
                    ZibeInputFieldDark(
                        value = email,
                        onValueChange = { email = it },
                        label = stringResource(id = R.string.email),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mail_24),
                                contentDescription = stringResource(id = R.string.email)
                            )
                        },
                        enabled = !isLoading
                    )

                    // PASSWORD
                    ZibeInputPasswordFieldDark(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(id = R.string.password),
                        enabled = !isLoading,
                        visible = passwordVisible,
                        onToggleVisible = { passwordVisible = !passwordVisible }
                    )

                    Text(
                        text = stringResource(R.string.auth_forgot_password),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(bottom = spacingSm)
                            .clickable {
                                resetEmail = email
                                showResetDialog = true
                            },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = lightText
                        )
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.element_spacing_medium)))

                    ZibeButtonPrimary(
                        text = stringResource(id = R.string.Entrar),
                        onClick = { onLogin(email.trim(), password) },
                        modifier = Modifier.padding(
                            top = spacingXs,
                            bottom = spacingSm
                        ),
                        enabled = !isLoading,
                        isLoading = isLoading
                    )

                    Spacer(modifier = Modifier.height(spacingSm))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.auth_do_not_have_account),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = lightText
                            )
                        )

                        Spacer(modifier = Modifier.width(spacingXs))

                        Text(
                            text = stringResource(R.string.action_register),
                            modifier = Modifier
                                .clickable { onNavigateToSignUp() }
                                .testTag(Constants.TestTags.BTN_REGISTER),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = lightText
                            )
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
                        enabled = !isLoading,
                        isLoading = isLoading
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
                        enabled = !isLoading,
                        isLoading = isLoading
                    )
                }
            }

            if (showResetDialog) {
                ZibeDialog(
                    title = stringResource(R.string.reset_password_title),
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.reset_password_content))
                            ZibeInputField(
                                value = resetEmail,
                                onValueChange = { resetEmail = it },
                                label = stringResource(id = R.string.email),
                                singleLine = true,
                                enabled = !isLoading
                            )
                        }
                    },
                    confirmText = stringResource(R.string.reset_password_accept),
                    onConfirm = {
                        if (resetEmail.isNotBlank()) {
                            onResetPassword(resetEmail.trim())
                            showResetDialog = false
                        }
                    },
                    onCancel = { if (!isLoading) showResetDialog = false },
                    enabled = !isLoading,
                    confirmEnabled = resetEmail.isNotBlank() && !isLoading
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreenPreview() {
    ZibeTheme {
        AuthScreen(
            deleteUser = false,
            onLogin = { _, _ -> },
            onNavigateToSignUp = {},
            onResetPassword = { _ -> },
            onGoogleClick = {},
            onFacebookClick = {},
            onDoNotDelete = {},
            onDeleteAccount = {},
            authEvents = MutableSharedFlow(),
            isLoading = false,
            onNavigateToSplash = {},
            appNavigator = AppNavigator()
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreenDeleteUserPreview() {
    ZibeTheme {
        AuthScreen(
            deleteUser = true,
            onLogin = { _, _ -> },
            onNavigateToSignUp = {},
            onResetPassword = { _ -> },
            onGoogleClick = {},
            onFacebookClick = {},
            onDoNotDelete = {},
            onDeleteAccount = {},
            authEvents = MutableSharedFlow(),
            isLoading = false,
            onNavigateToSplash = {},
            appNavigator = AppNavigator()
        )
    }
}
