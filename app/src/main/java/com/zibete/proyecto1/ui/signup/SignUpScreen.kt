package com.zibete.proyecto1.ui.signup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// --------------------------
// 🔹 Pantalla principal de registro
// --------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onRegister: (String, String, String, String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var birthday by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val gradientZibe = LocalZibeExtendedColors.current.gradientZibe

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
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
                .background(gradientZibe)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                        top = innerPadding.calculateTopPadding())
            )
            {
                // EMAIL
                ZibeInputField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",

                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mail_24),
                            contentDescription = "Correo electrónico"
                        )
                    },
                )

                // PASSWORD
                ZibeInputField(
                    value = pass,
                    onValueChange = { pass = it },
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
                            TextButton(
                                onClick = {
                                    val millis = datePickerState.selectedDateMillis
                                    if (millis != null) {
                                        val sdf =
                                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        birthday = sdf.format(Date(millis))
                                    }
                                    showDatePicker = false
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Cancelar")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // DESCRIPCIÓN
                ZibeInputField(
                    value = desc,
                    onValueChange = { desc = it },
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
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default)

                )

                // 💡 Tip dinámico
                AnimatedQuotesCard()

                // BOTÓN REGISTRAR
                Button(
                    onClick = { onRegister(email, pass, name, birthday, desc) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Finalizar registro")
                }
            }
        }
    }
}

// --------------------------
// 💬 Tarjeta animada Compose
// --------------------------
@Composable
fun AnimatedQuotesCard() {
    val frases = listOf(
        "💬 Contá algo que te haga único — así otros pueden conectar con vos 😉",
        "🌟 Compartí quién sos, qué te gusta o qué te inspira",
        "✨ Un toque personal hace que tu perfil destaque entre los demás",
        "⚡ Mostrate auténtico: tus intereses dicen más que mil palabras."
    )

    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            index = (index + 1) % frases.size
        }
    }

    val cardColor = LocalZibeExtendedColors.current.surfaceBright

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = frases[index],
                transitionSpec = {
                    fadeIn(tween(600)) togetherWith fadeOut(tween(600))
                },
                label = "FrasesLoop"
            ) { texto ->
                Text(
                    text = texto,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// --------------------------
// 🔹 Previews
// --------------------------
@Preview(showBackground = true)
@Composable
fun PreviewSignUpScreen() {
    ZibeTheme {
        SignUpScreen(
            onBack = {},
            onRegister = { _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAnimatedQuotesCard() {
    ZibeTheme {
        AnimatedQuotesCard()
    }
}
