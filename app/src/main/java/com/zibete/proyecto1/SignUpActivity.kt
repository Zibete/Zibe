package com.zibete.proyecto1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.facebook.login.LoginManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.Splash.SplashActivity
import com.zibete.proyecto1.utils.Constants.REQUEST_LOCATION
import com.zibete.proyecto1.utils.DateUtils
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import com.zibete.proyecto1.utils.UserMessageUtils
import com.zibete.proyecto1.utils.UserMessageUtils.showInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*

class SignUpActivity : ComponentActivity() {

    private lateinit var mAuth: FirebaseAuth
    private var myInstallId: String? = null
    private var myFcmToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
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
        var birthday by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Toolbar (simple)
            Text(
                text = "Tus datos",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // EMAIL
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // PASSWORD
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // NOMBRE
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // FECHA DE NACIMIENTO
            OutlinedTextField(
                value = birthday,
                onValueChange = { birthday = it },
                label = { Text("Fecha de nacimiento") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // DESCRIPCIÓN
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Descripción") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start)
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

        var index by remember { mutableStateOf(0) }

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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    dlg.dismiss()
                    toast("E-mail o contraseña inválidos")
                    return@addOnCompleteListener
                }

                val user = mAuth.currentUser ?: return@addOnCompleteListener
                writeUserProfile(user, dlg, email, name, birthday, desc)
            }
    }

    private fun writeUserProfile(
        user: FirebaseUser,
        dlg: androidx.appcompat.app.AlertDialog,
        email: String,
        name: String,
        birthday: String,
        desc: String
    ) {
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Calendar.getInstance().time)
        val age = DateUtils.calcAge(birthday)

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
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val birth = LocalDate.parse(birthStr, fmt)
        Period.between(birth, LocalDate.now()).years >= 18
    } catch (_: Exception) { false }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
