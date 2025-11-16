package com.zibete.proyecto1.ui.signup

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SignUpActivity : ComponentActivity() {

    private var myInstallId: String? = null
    private var myFcmToken: String? = null

    sealed class SignUpEvent {
        data class ShowSnackbar(
            val message: String,
            val type: ZibeSnackType
        ) : SignUpEvent()
        // Más adelante podés sumar:
        // object NavigateToHome : AuthEvent()
    }

    // Flow de eventos hacia la UI
    private val signUpEvents = MutableSharedFlow<SignUpEvent>()
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFirebase()

        setContent {
            ZibeTheme {

                val isLoading by isLoading.collectAsStateWithLifecycle()

                SignUpScreen(
                    onBack = { finish() },
                    onRegister = { email, pass, name, birthday, desc ->
                        doSignUp(email, pass, name, birthday, desc)
                    },
                    signUpEvents = signUpEvents,
                    isLoading = isLoading,
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


    // ------------- Registro

    private fun doSignUp(
        email: String,
        password: String,
        name: String,
        birthday: String,
        desc: String
    ) {

        lifecycleScope.launch {

            if (!validateInputs(
                    email,
                    password,
                    name,
                    birthday,
                    signUpEvents,
                    setLoading = { value -> _isLoading.value = value }
                )
            ) return@launch

            _isLoading.value = true

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
                    SignUpEvent.ShowSnackbar(
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
                    SignUpEvent.ShowSnackbar(
                        message = userMessage,
                        type = type
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    // -------------------------- Perfil en Realtime DB
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
    private suspend fun validateInputs(
        email: String,
        password: String,
        name: String,
        birthday: String,
        signUpEvents: MutableSharedFlow<SignUpEvent>,
        setLoading: (Boolean) -> Unit
    ): Boolean {

        suspend fun warn(msg: String): Boolean {
            setLoading(false)
            signUpEvents.emit(SignUpEvent.ShowSnackbar(msg, ZibeSnackType.WARNING))
            return false
        }

        if (email.isBlank())
            return warn("Por favor, ingresá tu email")

        if (password.isBlank())
            return warn("Por favor, ingresá una contraseña")

        if (name.isBlank())
            return warn("Por favor, ingresá tu nombre")

        if (birthday.isBlank())
            return warn("Por favor, ingresá tu fecha de nacimiento")

        // ⬇️ Validación de +18 años
        if (!isAdult(birthday))
            return warn("Lo sentimos, debés ser mayor de 18 años para utilizar la App")

        return true
    }


}
