package com.zibete.proyecto1.ui.signup

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.theme.ZibeTheme
import com.zibete.proyecto1.utils.Constants
import com.zibete.proyecto1.utils.DateUtils
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.UserMessageUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SignUpActivity : ComponentActivity() {

    private var myInstallId: String? = null
    private var myFcmToken: String? = null

    // Evento tipado para los snackbars
    data class SignUpEvent(
        val message: String,
        val type: ZibeSnackType
    )

    // Flow de eventos hacia la UI
    val signUpEvents = MutableSharedFlow<SignUpEvent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFirebase()

        setContent {
            ZibeTheme {
                SignUpScreen(
                    onBack = { finish() },
                    onRegister = { email, pass, name, birthday, desc ->
                        doSignUp(email, pass, name, birthday, desc)
                    },
                    signUpEvents = signUpEvents
                )
            }
        }
    }

    // --------------------------
    // 🔹 Firebase
    // --------------------------
    private fun setupFirebase() {
        FirebaseInstallations.getInstance().id.addOnCompleteListener { t ->
            if (t.isSuccessful) myInstallId = t.result
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
            if (t.isSuccessful) myFcmToken = t.result
        }
    }

    // --------------------------
    // 🔹 Registro
    // --------------------------
    private fun doSignUp(
        email: String,
        password: String,
        name: String,
        birthday: String,
        desc: String
    ) {
        // Validaciones de formulario → WARNING
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || birthday.isEmpty()) {
            lifecycleScope.launch {
                signUpEvents.emit(
                    SignUpEvent(
                        message = "Por favor, completá todos los campos",
                        type = ZibeSnackType.WARNING
                    )
                )
            }
            return
        }

        if (!isAdult(birthday)) {
            lifecycleScope.launch {
                signUpEvents.emit(
                    SignUpEvent(
                        message = "Lo sentimos, debe ser mayor de 18 años para utilizar la App",
                        type = ZibeSnackType.WARNING
                    )
                )
            }
            return
        }

        val dlg = UserMessageUtils.showProgress(this, "Registrando...")

        lifecycleScope.launch {
            try {
                // 1) Autenticación
                val authResult = FirebaseRefs.auth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val user = authResult.user
                    ?: throw IllegalStateException("El usuario no pudo ser creado.")

                // 2) Guardar perfil
                writeUserProfile(user, email, name, birthday, desc)

                // 3) Mensaje de éxito
                signUpEvents.emit(
                    SignUpEvent(
                        message = "Registro completado 🎉",
                        type = ZibeSnackType.SUCCESS
                    )
                )

                // 4) Permisos de ubicación
                ActivityCompat.requestPermissions(
                    this@SignUpActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    Constants.REQUEST_LOCATION
                )

            } catch (e: Exception) {
                val (userMessage, type) = when (e) {
                    is FirebaseAuthException -> {
                        val msg = when (e.errorCode) {
                            "ERROR_EMAIL_ALREADY_IN_USE" ->
                                "🚫 El correo electrónico ya está registrado."
                            "ERROR_WEAK_PASSWORD" ->
                                "Contraseña demasiado débil. Usá 6 caracteres o más."
                            "ERROR_INVALID_EMAIL" ->
                                "El formato de correo es inválido."
                            else ->
                                "Error en el registro: código ${e.errorCode}"
                        }
                        msg to ZibeSnackType.ERROR
                    }

                    else ->
                        ("Ocurrió un error inesperado: ${e.localizedMessage}"
                                to ZibeSnackType.ERROR)
                }

                signUpEvents.emit(
                    SignUpEvent(
                        message = userMessage,
                        type = type
                    )
                )
            } finally {
                dlg.dismiss()
            }
        }
    }

    // --------------------------
    // 🔹 Perfil en Realtime DB
    // --------------------------
    private suspend fun writeUserProfile(
        user: FirebaseUser,
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
            "foto" to getString(com.zibete.proyecto1.R.string.URL_PHOTO_DEF),
            "estado" to true,
            "installId" to myInstallId,
            "fcmToken" to myFcmToken,
            "token" to myInstallId,
            "distance" to 0,
            "descripcion" to desc.ifEmpty { "" },
            "latitud" to 0,
            "longitud" to 0
        )

        val userRef: DatabaseReference = FirebaseRefs.refCuentas.child(user.uid)
        userRef.setValue(data).await()

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .setPhotoUri(getString(com.zibete.proyecto1.R.string.URL_PHOTO_DEF).toUri())
            .build()

        user.updateProfile(profileUpdates).await()
    }

    private fun isAdult(birthStr: String): Boolean = try {
        DateUtils.calcAge(birthStr) >= 18
    } catch (_: Exception) {
        false
    }
}
