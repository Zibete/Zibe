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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.ui.components.ZibeButton
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibeSnackHost
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

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
    onNavigateToSplash: () -> Unit
) {
    // Inputs
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by rememberSaveable { mutableStateOf("") }

    val zibeColors = LocalZibeExtendedColors.current
    val context = LocalContext.current

    val lightText = zibeColors.lightText

    val elementSpacingXs = dimensionResource(R.dimen.element_spacing_xs)
    val elementSpacingSmall = dimensionResource(R.dimen.element_spacing_small)
    val elementSpacingMedium = dimensionResource(R.dimen.element_spacing_medium)
    val elementSpacingXl = dimensionResource(R.dimen.element_spacing_xl)

    val snackHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.testTag(AUTH_SCREEN)
    ) {
        LaunchedEffect(Unit) {
            authEvents.collect { event ->
                when (event) {
                    is AuthUiEvent.ShowSnack -> {
                        scope.launch {
                            snackHostState.showZibeMessage(
                                message = event.message.asString(context),
                                type = event.type
                            )
                        }
                    }

                    is AuthUiEvent.NavigateToSplash -> {
                        onNavigateToSplash()
                    }

                    is AuthUiEvent.NavigateToSignUp -> {
                        onNavigateToSignUp()
                    }
                }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                ZibeSnackHost(hostState = snackHostState)
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(zibeColors.gradientZibe)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                        .padding(
                            start = elementSpacingMedium,
                            end = elementSpacingMedium,
                            bottom = innerPadding.calculateTopPadding(),
                            top = innerPadding.calculateTopPadding()
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // LOGO
                    Image(
                        painter = painterResource(id = R.mipmap.logo_zibe),
                        contentDescription = stringResource(R.string.logo_content_desc),
                        modifier = Modifier
                            .padding(
                                start = elementSpacingXl,
                                end = elementSpacingXl,
                                bottom = elementSpacingXs,
                                top = dimensionResource(R.dimen.zero_dp)
                            )
                    )

                    if (!deleteUser) {
                        // GOOGLE
                        ZibeButton(
                            text = stringResource(R.string.continue_with_google),
                            iconRes = R.drawable.ic_google,
                            onClick = onGoogleClick
                        )

                        Spacer(modifier = Modifier.height(elementSpacingXs))

                        // FACEBOOK
                        ZibeButton(
                            text = stringResource(R.string.continue_with_facebook),
                            iconRes = R.drawable.ic_facebook,
                            onClick = onFacebookClick
                        )

                        Spacer(modifier = Modifier.height(elementSpacingXs))

                        Text(
                            text = stringResource(R.string.auth_or_use_account),
                            style = MaterialTheme.typography.headlineSmall,
                            color = lightText,
                        )

                        Spacer(modifier = Modifier.height(elementSpacingMedium))

                        // EMAIL
                        ZibeInputField(
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
                        ZibeInputField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(id = R.string.password),

                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_lock_24),
                                    contentDescription = stringResource(id = R.string.password)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (passwordVisible)
                                                R.drawable.ic_baseline_visibility_24
                                            else
                                                R.drawable.ic_baseline_visibility_off_24
                                        ),
                                        contentDescription = stringResource(R.string.password_content_desc)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            enabled = !isLoading
                        )

                        Text(
                            text = stringResource(R.string.auth_forgot_password),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(bottom = elementSpacingSmall)
                                .clickable {
                                    resetEmail = email
                                    showResetDialog = true
                                },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = lightText
                            )
                        )

                        Spacer(modifier = Modifier.height(elementSpacingMedium))

                        ZibeButton(
                            text = stringResource(id = R.string.Entrar),
                            onClick = { onLogin(email.trim(), password) },
                            modifier = Modifier.padding(
                                top = elementSpacingXs,
                                bottom = elementSpacingSmall
                            ),
                            enabled = !isLoading,
                            isLoading = isLoading
                        )

                        Spacer(modifier = Modifier.height(elementSpacingSmall))

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

                            Spacer(modifier = Modifier.width(elementSpacingXs))

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

                        ZibeButton(
                            text = stringResource(R.string.auth_do_not_delete_account),
                            onClick = { onDoNotDelete() },
                            modifier = Modifier.padding(
                                top = elementSpacingXs,
                                bottom = elementSpacingSmall
                            ),
                            enabled = !isLoading,
                            isLoading = isLoading
                        )
                        ZibeButton(
                            text = stringResource(R.string.delete_account),
                            onClick = { onDeleteAccount() },
                            modifier = Modifier.padding(
                                top = elementSpacingXs,
                                bottom = elementSpacingSmall
                            ),
                            enabled = !isLoading,
                            isLoading = isLoading
                        )
                    }
                }

                if (showResetDialog) {
                    ZibeDialog(
                        title = stringResource(R.string.reset_password_title),
                        textContent = {
                            ZibeInputField(
                                value = resetEmail,
                                onValueChange = { resetEmail = it },
                                label = stringResource(id = R.string.email),
                                singleLine = true,
                                enabled = !isLoading
                            )
                        },
                        confirmText = stringResource(R.string.reset_password_accept),
                        confirmEnabled = resetEmail.isNotBlank() && !isLoading,
                        onConfirm = {
                            if (resetEmail.isNotBlank()) {
                                onResetPassword(resetEmail.trim())
                                showResetDialog = false
                            }
                        },
                        onDismiss = { if (!isLoading) showResetDialog = false },
                        enabled = !isLoading
                    )
                }
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
            onNavigateToSplash = {}
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
            onNavigateToSplash = {}
        )
    }
}
