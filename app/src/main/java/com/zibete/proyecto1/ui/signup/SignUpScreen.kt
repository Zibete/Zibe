package com.zibete.proyecto1.ui.signup

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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeAnimatedQuotesCard
import com.zibete.proyecto1.ui.components.ZibeButton
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibeSnackHost
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.constants.stringsSignUpScreen
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme
import com.zibete.proyecto1.utils.Utils.millisToBirthDate
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
    onNavigateToPermission: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var birthday by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val snackHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        signUpEvents.collect { event ->
            when (event) {
                is SignUpUiEvent.ShowSnack -> {
                    scope.launch {
                        snackHostState.showZibeMessage(
                            type = event.type,
                            message = event.message
                        )
                    }
                }
                SignUpUiEvent.RequestLocationPermission -> {
                    // Esto lo maneja la Activity; acá no hacemos nada.
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
                title = "Tus datos",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalZibeExtendedColors.current.gradientZibe)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateTopPadding(),
                        top = innerPadding.calculateTopPadding()
                    )
            ) {
                // EMAIL
                ZibeInputField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(id = R.string.email),
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
                                contentDescription = "Contraseña visible/invisible"
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
                    label = "Nombre/Apodo",
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_person_24),
                            contentDescription = "Nombre"
                        )
                    },
                    enabled = !isLoading
                )

                // FECHA DE NACIMIENTO
                Box {
                    ZibeInputField(
                        value = birthday,
                        onValueChange = { },
                        label = "Fecha de nacimiento",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_24),
                                contentDescription = "Fecha de nacimiento",
                                tint = colorResource(id = R.color.zibe_text_muted)
                            )
                        },
                        enabled = !isLoading,
                        readOnly = true,
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState()

                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val formatted = millisToBirthDate(millis)
                                    birthday = formatted
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text(DIALOG_CANCEL) }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // DESCRIPCIÓN
                ZibeInputField(
                    value = description,
                    onValueChange = { description = it },
                    label = "¿Algo sobre vos?",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    singleLine = false,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info_24),
                            contentDescription = "Descripción"
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
                    strings = stringsSignUpScreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )

                // BOTÓN REGISTRAR
                ZibeButton(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    text = stringResource(R.string.finalizar_registro),
                    onClick = { onRegister(email, password, name, birthday, description) },
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
            onNavigateToPermission = {}
        )
    }
}