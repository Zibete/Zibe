package com.zibete.proyecto1.ui.auth

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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeButton
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibeSnackbarHost
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
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
    authEvents: SharedFlow<AuthUiEvent>,
    isLoading: Boolean,
    onNavigateToSplash: () -> Unit,
    onClearDeletePrefs: () -> Unit
) {
    // Inputs
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by rememberSaveable { mutableStateOf("") }

    val zibeColors = LocalZibeExtendedColors.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        authEvents.collect { event ->
            when (event) {
                is AuthUiEvent.ShowSnackbar -> {
                    scope.launch {
                        snackbarHostState.showZibeMessage(
                            type = event.type,
                            message = event.message
                        )
                    }
                }
                AuthUiEvent.ClearDeletePrefs -> onClearDeletePrefs()
                AuthUiEvent.NavigateToSplash -> onNavigateToSplash()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            ZibeSnackbarHost(hostState = snackbarHostState)
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
                    .padding(start = 16.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateTopPadding(),
                        top = innerPadding.calculateTopPadding()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // LOGO
                Image(
                    painter = painterResource(id = R.mipmap.logo_zibe),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 8.dp,
                            top = 0.dp)
                )

                // GOOGLE
                SocialButton(
                    text = "Continuar con Google",
                    iconRes = R.drawable.ic_google,
                    onClick = onGoogleClick
                )

                // FACEBOOK
                SocialButton(
                    text = "Continuar con Facebook",
                    iconRes = R.drawable.ic_facebook,
                    onClick = onFacebookClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.or_use_your_account),
                    style = MaterialTheme.typography.headlineSmall,
                    color = zibeColors.mutedText,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Si no está marcada para eliminación, mostramos login normal
                if (!deleteUser) {
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
                        label = "Contraseña",

                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lock_24),
                                contentDescription = "Contraseña"
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
                                    contentDescription = "Contraseña visible/invisible"
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
                        text = stringResource(id = R.string.forgot_password),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(bottom = 8.dp)
                            .clickable {
                                resetEmail = email   // prellenamos con el mail del formulario
                                showResetDialog = true },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = zibeColors.mutedText
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ZibeButton(
                        text = stringResource(id = R.string.Entrar),
                        onClick = { onLogin(email.trim(), password) },
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                        enabled = !isLoading,
                        isLoading = isLoading
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.dont_have_an_account),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = zibeColors.mutedText
                            )
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = stringResource(id = R.string.registro),
                            modifier = Modifier
                                .clickable { onNavigateToSignUp() },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = zibeColors.mutedText
                            )
                        )
                    }
                } else {
                    // Modo "reactivar cuenta"
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onDoNotDelete) {
                        Text(
                            text = stringResource(id = R.string.dont_delete_account),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (showResetDialog) {
                ZibeDialog(
                    title = "Reestablecimiento de contraseña",
                    textContent = {
                        ZibeInputField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = stringResource(id = R.string.email),
                            singleLine = true,
                            enabled = !isLoading
                        )
                    },
                    confirmText = "Enviar e-mail",
                    onConfirm = {
                        if (resetEmail.isNotBlank()) {
                            onResetPassword(resetEmail.trim())
                            showResetDialog = false
                        }
                    },
                    dismissText = "Cancelar",
                    onDismiss = { if (!isLoading) showResetDialog = false },
                    enabled = !isLoading
                )
            }
        }
    }
}

@Composable
private fun SocialButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .height(52.dp)
            .shadow(8.dp, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
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
            authEvents = MutableSharedFlow(),
            isLoading = false,
            onNavigateToSplash = {},
            onClearDeletePrefs = {},
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
            authEvents = MutableSharedFlow(),
            isLoading = false,
            onNavigateToSplash = {},
            onClearDeletePrefs = {},
        )
    }
}
