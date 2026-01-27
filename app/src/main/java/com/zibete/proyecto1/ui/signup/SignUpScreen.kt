package com.zibete.proyecto1.ui.signup

import LocalZibeExtendedColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.TestTags
import com.zibete.proyecto1.core.constants.Constants.UiTags.SIGNUP_SCREEN
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.navigation.NavAppEvent
import com.zibete.proyecto1.core.utils.TimeUtils.isoToMillis
import com.zibete.proyecto1.core.utils.TimeUtils.isoToUiDate
import com.zibete.proyecto1.core.utils.TimeUtils.millisToIso
import com.zibete.proyecto1.ui.components.ZibeAnimatedQuotesCard
import com.zibete.proyecto1.ui.components.ZibeButtonPrimary
import com.zibete.proyecto1.ui.components.ZibeInputFieldDark
import com.zibete.proyecto1.ui.components.ZibeInputPasswordFieldDark
import com.zibete.proyecto1.ui.components.ZibeSnackbar
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.theme.ZibeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onRegister: (String, String, String, String, String) -> Unit,
    isLoading: Boolean,
    onNavigateToSplash: () -> Unit,
    appNavigator: AppNavigator
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var birthDate by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val snackHostState = remember { SnackbarHostState() }

    val zibeColors = LocalZibeExtendedColors.current
    val lightText = zibeColors.lightText

    val spacingSm = dimensionResource(R.dimen.element_spacing_small)

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
        snackbarHost = { ZibeSnackbar(hostState = snackHostState) },
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
                    )
            ) {
                // EMAIL
                ZibeInputFieldDark(
                    value = email,
                    onValueChange = { email = it },
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

                // NOMBRE
                ZibeInputFieldDark(
                    value = name,
                    onValueChange = { name = it },
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
                    enabled = !isLoading
                )

                // FECHA DE NACIMIENTO
                Box {
                    ZibeInputFieldDark(
                        value = birthDate.takeIf { it.isNotBlank() }?.let { isoToUiDate(it) }
                            .orEmpty(),
                        onValueChange = { },
                        label = stringResource(R.string.birth_date),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_24),
                                contentDescription = stringResource(R.string.birth_date),
                                tint = lightText.copy(alpha = 0.6f)
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
                            contentDescription = stringResource(R.string.birth_date),
                            tint = lightText.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = spacingSm)
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
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { ms ->
                                        birthDate = millisToIso(ms)
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text(stringResource(R.string.action_accept))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // DESCRIPCIÓN
                ZibeInputFieldDark(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .testTag(TestTags.DESCRIPTION)
                        .height(140.dp),
                    singleLine = false,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_edit_24),
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
                        .padding(vertical = spacingSm)
                )

                // BOTÓN REGISTRAR
                ZibeButtonPrimary(
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.element_spacing_xs))
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignUpScreenPreview() {
    ZibeTheme {
        SignUpScreen(
            onBack = {},
            onRegister = { _, _, _, _, _ -> },
            isLoading = false,
            onNavigateToSplash = {},
            appNavigator = AppNavigator()
        )
    }
}

