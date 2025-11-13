package com.zibete.proyecto1

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.utils.Constants.REQUEST_LOCATION
import com.zibete.proyecto1.utils.DateUtils.calcAge
import com.zibete.proyecto1.utils.FirebaseRefs.auth
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import com.zibete.proyecto1.utils.UserMessageUtils
import com.zibete.proyecto1.utils.UserMessageUtils.showInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SignUpActivity : ComponentActivity() {

    private var myInstallId: String? = null
    private var myFcmToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFirebase()

        setContent {
            MaterialTheme {
                SignUpScreen(
                    onRegister = { email, pass, name, birthday, desc ->
                        doSignUp(email, pass, name, birthday, desc)
                    }
                )
            }
        }
    }

    // --------------------------
    // 🔹 Compose UI principal
    // --------------------------
    @Composable
    fun SignUpScreen(onRegister: (String, String, String, String, String) -> Unit) {
        var email by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var showDatePicker by remember { mutableStateOf(false) }
        var birthday by remember { mutableStateOf("") }   // ya lo tenías
        var desc by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = "Volver",
                    tint = colorResource(id = R.color.white),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { finish() }
                )
                Text(
                    text = "Tus datos",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = colorResource(id = R.color.white)
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // EMAIL
            ZibeInputField(
                value = email,
                onValueChange = { email = it },
                label = "Correo electrónico",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {Icon(
                    painter = painterResource(id = R.drawable.ic_mail_24),
                    contentDescription = "Correo electrónico")
                }
            )

            // PASSWORD
            ZibeInputField(
                value = pass,
                onValueChange = { pass = it },
                label = "Contraseña",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {Icon(
                    painter = painterResource(id = R.drawable.ic_lock_24),
                    contentDescription = "Contraseña")
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
                label = "Nombre",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {Icon(
                    painter = painterResource(id = R.drawable.ic_person_24),
                    contentDescription = "Nombre")
                }
            )

            // FECHA DE NACIMIENTO
            Box(
            ) {
                ZibeInputField(
                    value = birthday,
                    onValueChange = { },
                    label = "Fecha de nacimiento",
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_calendar_24),
                            contentDescription = "Fecha de nacimiento",
                            tint = colorResource(id = R.color.zibe_text_muted)
                        )
                    }
                )
                // Capa clickeable encima del input
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
                                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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
                label = "Descripción",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                leadingIcon = {Icon(
                    painter = painterResource(id = R.drawable.ic_info_24),
                    contentDescription = "Descripción")
                }
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.zibe_surface))
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
                            color = colorResource(id = R.color.zibe_text_muted)
                        )
                    )
                }
            }
        }
    }

    @Composable
    fun ZibeInputField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        modifier: Modifier = Modifier,
        singleLine: Boolean = true,
        leadingIcon: (@Composable (() -> Unit))? = null,
        trailingIcon: (@Composable (() -> Unit))? = null,
        visualTransformation: VisualTransformation = VisualTransformation.None,
        readOnly: Boolean = false
    ) {
        val containerColor = colorResource(id = R.color.zibe_night_end)
        val borderColor = colorResource(id = R.color.accent)
        val hintColor = colorResource(id = R.color.zibe_hint_text)
        val iconTint = colorResource(id = R.color.zibe_text_muted)
        val textColor = colorResource(id = R.color.white)

        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth()
        ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            singleLine = singleLine,
            label = {
                Text(
                    text = label,
                    color = hintColor
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(
                topStart = 8.dp,
                topEnd = 8.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),

            colors = TextFieldDefaults.colors(
                // fondo (equivalente a boxBackgroundColor)
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,

                // borde (equivalente a boxStrokeColor)
                focusedIndicatorColor = borderColor,
                unfocusedIndicatorColor = borderColor,
                disabledIndicatorColor = borderColor,
                errorIndicatorColor = borderColor,

                focusedTextColor = textColor,
                unfocusedTextColor = textColor,

                // hint flotante
                focusedLabelColor = hintColor,
                unfocusedLabelColor = hintColor,

                // íconos start/end (equivalente a startIconTint / endIconTint)
                focusedLeadingIconColor = iconTint,
                unfocusedLeadingIconColor = iconTint,
                focusedTrailingIconColor = iconTint,
                unfocusedTrailingIconColor = iconTint,

                // cursor
                cursorColor = borderColor,

            ),

            visualTransformation = visualTransformation,
            readOnly = readOnly,

            )}
    }

    // --------------------------
    // 🔹 Lógica de registro (idéntica)
    // --------------------------
    private fun setupFirebase() {
        FirebaseInstallations.getInstance().id.addOnCompleteListener { t ->
            if (t.isSuccessful) myInstallId = t.result
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
            if (t.isSuccessful) myFcmToken = t.result
        }
    }

    private fun doSignUp(email: String, password: String, name: String, birthday: String, desc: String) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || birthday.isEmpty()) {
            toast("Por favor, completá todos los campos")
            return
        }

        if (!isAdult(birthday)) {
            showInfo(
                findViewById(android.R.id.content),
                "Lo sentimos, debe ser mayor de 18 años para utilizar la App"
            )
            return
        }

        val dlg = UserMessageUtils.showProgress(this, "Registrando...")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    dlg.dismiss()
                    toast("E-mail o contraseña inválidos")
                    return@addOnCompleteListener
                }

                val user = auth.currentUser ?: return@addOnCompleteListener
                writeUserProfile(user, dlg, email, name, birthday, desc)
            }
    }

    private fun writeUserProfile(
        user: FirebaseUser,
        dlg: AlertDialog,
        email: String,
        name: String,
        birthday: String,
        desc: String
    ) {
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Calendar.getInstance().time)
        val age = calcAge(birthday)

        val data = hashMapOf<String, Any?>(
            "id" to user.uid,
            "nombre" to name,
            "birthDay" to birthday,
            "date" to nowStr,
            "age" to age,
            "mail" to email,
            "foto" to getString(R.string.URL_PHOTO_DEF),
            "estado" to true,
            "installId" to myInstallId,
            "fcmToken" to myFcmToken,
            "token" to myInstallId,
            "distance" to 0,
            "descripcion" to desc.ifEmpty { "" },
            "latitud" to 0,
            "longitud" to 0
        )

        val userRef: DatabaseReference = refCuentas.child(user.uid)
        userRef.setValue(data).addOnCompleteListener { setTask ->
            dlg.dismiss()
            if (!setTask.isSuccessful) {
                toast("Error guardando datos")
                return@addOnCompleteListener
            }

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(getString(R.string.URL_PHOTO_DEF).toUri())
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener {
                    ActivityCompat.requestPermissions(
                        this@SignUpActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION
                    )
                }
        }
    }

    private fun isAdult(birthStr: String): Boolean = try {
        calcAge(birthStr) >= 18
    } catch (_: Exception) { false }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
