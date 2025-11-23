package com.zibete.proyecto1.ui.signup

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.*
import com.zibete.proyecto1.utils.DateUtils
import com.zibete.proyecto1.utils.FirebaseRefs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SignUpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SignUpUiEvent>()
    val events = _events.asSharedFlow()

    private var myInstallId: String? = null
    private var myFcmToken: String? = null

    init {
        setupFirebase()
    }

    // -------------------------- Firebase IDs / Tokens

    private fun setupFirebase() {
        FirebaseInstallations.getInstance().id.addOnCompleteListener { t ->
            if (t.isSuccessful) myInstallId = t.result
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
            if (t.isSuccessful) myFcmToken = t.result
        }
    }

    // -------------------------- Registro

    fun onRegister(
        email: String,
        password: String,
        name: String,
        birthday: String,
        desc: String,
        defaultPhotoUrl: String
    ) {
        viewModelScope.launch {

            if (!validateInputs(email, password, name, birthday)) return@launch

            setLoading(true)

            try {
                // 1) Autenticación
                val authResult = FirebaseRefs.auth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val user = authResult.user
                    ?: throw IllegalStateException(SIGNUP_ERR_EXCEPTION)

                // 2) Guardar perfil
                writeUserProfile(user, email, name, birthday, desc, defaultPhotoUrl)

                // 3) Mensaje de éxito
                _events.emit(
                    SignUpUiEvent.ShowSnackbar(
                        message = SIGNUP_MSG_SUCCESS,
                        type = ZibeSnackType.SUCCESS
                    )
                )

                // 4) Pedir permisos de ubicación (lo maneja la Activity)
                _events.emit(SignUpUiEvent.RequestLocationPermission)

            } catch (e: Exception) {

                val (userMessage, type) = when (e) {
                    is FirebaseAuthException -> {
                        val msg = when (e.errorCode) {
                            "ERROR_EMAIL_ALREADY_IN_USE" -> SIGNUP_ERR_EMAIL_IN_USE
                            "ERROR_WEAK_PASSWORD"        -> SIGNUP_ERR_INVALID_PASSWORD
                            "ERROR_INVALID_EMAIL"        -> SIGNUP_ERR_INVALID_EMAIL
                            else -> SIGNUP_ERR_GENERIC_PREFIX + e.errorCode
                        }
                        msg to ZibeSnackType.ERROR
                    }

                    else -> (
                            SIGNUP_ERR_UNEXPECTED_PREFIX +
                                    (e.localizedMessage ?: "")
                            ) to ZibeSnackType.ERROR
                }

                _events.emit(
                    SignUpUiEvent.ShowSnackbar(
                        message = userMessage,
                        type = type
                    )
                )
            } finally {
                setLoading(false)
            }
        }
    }

    // -------------------------- Perfil en Realtime DB

    private suspend fun writeUserProfile(
        user: FirebaseUser,
        email: String,
        name: String,
        birthday: String,
        desc: String,
        defaultPhotoUrl: String
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
            "foto" to defaultPhotoUrl,
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
            .setPhotoUri(defaultPhotoUrl.toUri())
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
        birthday: String
    ): Boolean {

        suspend fun warn(msg: String): Boolean {
            setLoading(false)
            _events.emit(
                SignUpUiEvent.ShowSnackbar(
                    message = msg,
                    type = ZibeSnackType.WARNING
                )
            )
            return false
        }

        if (email.isBlank())
            return warn(ERR_EMAIL_REQUIRED)

        if (password.isBlank())
            return warn(ERR_PASSWORD_REQUIRED)

        if (name.isBlank())
            return warn(SIGNUP_ERR_NAME_REQUIRED)

        if (birthday.isBlank())
            return warn(SIGNUP_ERR_BIRTHDAY_REQUIRED)

        if (!isAdult(birthday))
            return warn(ERR_UNDER_AGE)

        return true
    }

    private fun setLoading(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }
}
