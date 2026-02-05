package com.zibete.proyecto1.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.MAX_ABOUT_CHARS
import com.zibete.proyecto1.core.constants.Constants.TestTags
import com.zibete.proyecto1.core.constants.Constants.UiTags.SIGNUP_SCREEN
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.navigation.NavAppEvent
import com.zibete.proyecto1.core.utils.TimeUtils.isoToUiDate
import com.zibete.proyecto1.ui.components.ZibeAboutField
import com.zibete.proyecto1.ui.components.ZibeAnimatedQuotesCard
import com.zibete.proyecto1.ui.components.ZibeBirthDatePickerDialog
import com.zibete.proyecto1.ui.components.ZibeButtonPrimary
import com.zibete.proyecto1.ui.components.ZibeInputBirthdateDark
import com.zibete.proyecto1.ui.components.ZibeInputFieldDark
import com.zibete.proyecto1.ui.components.ZibeInputPasswordFieldDark
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun SignUpRoute(
    onBack: () -> Unit,
    onNavigateToSplash: () -> Unit,
    appNavigator: AppNavigator,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SignUpScreen(
        state = uiState,
        onBack = onBack,
        onRegister = viewModel::onRegister,
        onEmailInputChanged = viewModel::onEmailInputChanged,
        onPasswordInputChanged = viewModel::onPasswordInputChanged,
        onBirthDateChanged = viewModel::onBirthDateChanged,
        onNameChanged = viewModel::onNameChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onNavigateToSplash = onNavigateToSplash,
        appNavigator = appNavigator
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    state: SignUpUiState,
    onBack: () -> Unit,
    onRegister: (String, String, String, String, String) -> Unit,
    onEmailInputChanged: (String) -> Unit,
    onPasswordInputChanged: (String) -> Unit,
    onBirthDateChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onNavigateToSplash: () -> Unit,
    appNavigator: AppNavigator
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val inputPadding = dimensionResource(R.dimen.zibe_input_padding)

    val registerEnabled by remember(state) {
        derivedStateOf {
            state.email.trim().isNotEmpty() &&
                    state.password.isNotEmpty() &&
                    state.name.trim().isNotEmpty() &&
                    state.birthDate.isNotEmpty() &&
                    !state.isLoading &&
                    state.emailError == null &&
                    state.passwordError == null &&
                    state.nameError == null &&
                    state.birthDateError == null
        }
    }

    LaunchedEffect(Unit) {
        appNavigator.events.collect { event ->
            when (event) {
                is NavAppEvent.FinishFlowNavigateToSplash -> {
                    onNavigateToSplash()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            ZibeToolbar(
                title = stringResource(id = R.string.signup_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(SIGNUP_SCREEN)
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
                verticalArrangement = Arrangement.spacedBy(inputPadding)
            ) {
                // EMAIL
                ZibeInputFieldDark(
                    value = state.email,
                    onValueChange = onEmailInputChanged,
                    label = stringResource(id = R.string.email),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.EMAIL),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mail_24),
                            contentDescription = stringResource(id = R.string.email)
                        )
                    },
                    enabled = !state.isLoading,
                    error = state.emailError?.asString(context)
                )

                // PASSWORD
                ZibeInputPasswordFieldDark(
                    value = state.password,
                    onValueChange = onPasswordInputChanged,
                    label = stringResource(id = R.string.password),
                    enabled = !state.isLoading,
                    visible = passwordVisible,
                    onToggleVisible = { passwordVisible = !passwordVisible },
                    error = state.passwordError?.asString(context)
                )

                // NOMBRE
                ZibeInputFieldDark(
                    value = state.name,
                    onValueChange = onNameChanged,
                    label = stringResource(R.string.name),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.NAME),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_person_24),
                            contentDescription = stringResource(R.string.name)
                        )
                    },
                    enabled = !state.isLoading
                )

                // FECHA DE NACIMIENTO + EDAD
                ZibeInputBirthdateDark(
                    value = state.birthDate.takeIf { it.isNotBlank() }?.let { isoToUiDate(it) }.orEmpty(),
                    age = state.age?.toString().orEmpty(),
                    onClick = { if (!state.isLoading) showDatePicker = true },
                    error = state.birthDateError?.asString(context),
                    enabled = !state.isLoading
                )

                ZibeBirthDatePickerDialog(
                    isOpen = showDatePicker,
                    initialIso = state.birthDate.takeIf { it.isNotBlank() },
                    onDismiss = { showDatePicker = false },
                    onConfirmIso = { iso ->
                        onBirthDateChanged(iso)
                    }
                )

                ZibeAboutField(
                    value = state.description,
                    onValueChange = onDescriptionChanged,
                    label = stringResource(R.string.description),
                    testTag = TestTags.DESCRIPTION,
                    enabled = !state.isLoading,
                    maxChars = MAX_ABOUT_CHARS,
                    resizable = true,
                    bringIntoViewExtraBottom = 10.dp
                )

                // 💡 Tip dinámico
                ZibeAnimatedQuotesCard(
                    strings = listOf(
                        stringResource(R.string.signup_animated_message_1),
                        stringResource(R.string.signup_animated_message_2),
                        stringResource(R.string.signup_animated_message_3)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )

                // BOTÓN REGISTRAR
                ZibeButtonPrimary(
                    modifier = Modifier
                        .testTag(TestTags.BTN_REGISTER),
                    text = stringResource(R.string.finish_registration),
                    onClick = {
                        onRegister(
                            state.email,
                            state.password,
                            state.name,
                            state.birthDate,
                            state.description
                        )
                    },
                    enabled = registerEnabled,
                    isLoading = state.isLoading
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignUpScreenPreview() {
    ZibeTheme {
        SignUpScreen(
            state = SignUpUiState(),
            onBack = {},
            onRegister = { _, _, _, _, _ -> },
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onBirthDateChanged = {},
            onNameChanged = {},
            onDescriptionChanged = {},
            onNavigateToSplash = {},
            appNavigator = AppNavigator()
        )
    }
}
