package com.zibete.proyecto1.ui.signup

import LocalZibeExtendedColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.TestTags
import com.zibete.proyecto1.core.constants.Constants.UiTags.SIGNUP_SCREEN
import com.zibete.proyecto1.core.utils.TimeUtils.isoToMillis
import com.zibete.proyecto1.core.utils.TimeUtils.isoToUiDate
import com.zibete.proyecto1.core.utils.TimeUtils.millisToIso
import com.zibete.proyecto1.ui.components.ZibeAnimatedQuotesCard
import com.zibete.proyecto1.ui.components.ZibeButton
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibeSnackHost
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onRegister: (String, String, String, String, String) -> Unit,
    signUpEvents: Flow<SignUpUiEvent>,
    isLoading: Boolean,
    onNavigateToSplash: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var birthDate by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val snackHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val zibeColors = LocalZibeExtendedColors.current
    val lightText = zibeColors.lightText

    val elementSpacing8 = dimensionResource(R.dimen.element_spacing_xs)
    val elementSpacing12 = dimensionResource(R.dimen.element_spacing_small)
    val elementSpacing16 = dimensionResource(R.dimen.element_spacing_medium)
    val elementSpacingXl = dimensionResource(R.dimen.element_spacing_xl)

    LaunchedEffect(Unit) {
        signUpEvents.collect { event ->
            when (event) {
                is SignUpUiEvent.ShowSnack -> {
                    scope.launch {
                        snackHostState.showZibeMessage(
                            type = event.type,
                            message = event.uiText.asString(context)
                        )
                    }
                }
                is SignUpUiEvent.NavigateToSplash -> {
                    onNavigateToSplash()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = {
            ZibeSnackHost(hostState = snackHostState)
        },
        topBar = {
            ZibeToolbar(
                title = stringResource(id = R.string.your_data),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalZibeExtendedColors.current.gradientZibe)
                .testTag(SIGNUP_SCREEN)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(
                        start = elementSpacing16,
                        end = elementSpacing16,
                        bottom = innerPadding.calculateTopPadding(),
                        top = innerPadding.calculateTopPadding()
                    )
            ) {
                // EMAIL
                ZibeInputField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(id = R.string.email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.EMAIL),
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
                    modifier = Modifier.fillMaxWidth()
                        .testTag(TestTags.PASSWORD),
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
                                contentDescription = stringResource(id = R.string.password)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    enabled = !isLoading
                )

                // NOMBRE
                ZibeInputField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.name),
                    modifier = Modifier.fillMaxWidth()
                        .testTag(TestTags.NAME),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_person_24),
                            contentDescription = stringResource(R.string.name)
                        )
                    },
                    enabled = !isLoading
                )

                // FECHA DE NACIMIENTO
                Box {
                    ZibeInputField(
                        value = birthDate.takeIf { it.isNotBlank() }?.let { isoToUiDate(it) }.orEmpty(),
                        onValueChange = { },
                        label = stringResource(R.string.birth_date),
                        modifier = Modifier
                            .fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_24),
                                contentDescription = stringResource(R.string.birth_date),
                                tint = colorResource(id = R.color.zibe_hint_text)
                            )
                        },
                        enabled = !isLoading,
                        readOnly = true
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .testTag(TestTags.BIRTHDATE_PICKER)
                            .clickable { showDatePicker = true }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_keyboard_arrow_down_24),
                            contentDescription = "Abrir selector de fecha",
                            tint = colorResource(id = R.color.zibe_hint_text),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = elementSpacing12)
                        )
                    }
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = birthDate
                            .takeIf { it.isNotBlank() }
                            ?.let { isoToMillis(it) }
                    )

                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { ms ->
                                    birthDate = millisToIso(ms)
                                }
                                showDatePicker = false
                            }) { Text(stringResource(R.string.action_accept)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text(stringResource(R.string.action_cancel)) }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // DESCRIPCIÓN
                ZibeInputField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.DESCRIPTION)
                        .height(140.dp),
                    singleLine = false,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info_24),
                            contentDescription = stringResource(R.string.description)
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Default,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    enabled = !isLoading
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
                        .padding(vertical = elementSpacing12)
                )

                // BOTÓN REGISTRAR
                ZibeButton(
                    modifier = Modifier
                        .padding(top = elementSpacing8)
                        .testTag(TestTags.BTN_REGISTER),
                    text = stringResource(R.string.finish_registration),
                    onClick = { onRegister(email, password, name, birthDate, description) },
                    enabled = !isLoading,
                    isLoading = isLoading
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUpScreen() {
    ZibeTheme {
        val fakeFlow = MutableSharedFlow<SignUpUiEvent>()

        SignUpScreen(
            onBack = {},
            onRegister = { _, _, _, _, _ -> },
            signUpEvents = fakeFlow,
            isLoading = false,
            onNavigateToSplash = {}
        )
    }
}