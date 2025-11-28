package com.zibete.proyecto1.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.storage.FirebaseStorage
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.DELETE_ACCOUNT
import com.zibete.proyecto1.ui.constants.DO_NOT_DELETE_ACCOUNT
import com.zibete.proyecto1.ui.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.ui.constants.ERR_PASSWORD_REQUIRED
import com.zibete.proyecto1.ui.constants.ERR_ZIBE
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class AuthViewModel @Inject constructor(
        private val userPreferencesRepository: UserPreferencesRepository, // ← Inyectado
        private val firebaseAuth: FirebaseAuth,
        private val firebaseRefsContainer: FirebaseRefsContainer
    ) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AuthUiEvent>()
    val events = _events.asSharedFlow()

    fun initFromPrefs(deleteUser: Boolean, deleteFirebaseAccount: Boolean) {
        _uiState.update { it.copy(deleteUser = deleteUser) }

        if (deleteFirebaseAccount) {
            // Mostrar mensaje y pedir limpiar flags
            showMessage(DELETE_ACCOUNT, ZibeSnackType.INFO)
            viewModelScope.launch {
                _events.emit(AuthUiEvent.ClearDeletePrefs)
            }
        }
    }

    // ================= EMAIL / PASSWORD =================

    fun onEmailLogin(email: String, password: String) {
        viewModelScope.launch {
            if (!validateInputs(email, password)) return@launch

            _uiState.update { it.copy(isLoading = true) }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        val msg = getAuthErrorMessage(task.exception)
                        showMessage(msg, ZibeSnackType.ERROR)
                    } else {
                        handleAuthSuccess()
                    }
                }
        }
    }

    // ================= RESET PASSWORD =================

    fun onResetPassword(email: String) {
        if (email.isBlank()) {
            showMessage(
                "Por favor, ingresá tu email para reestablecer la contraseña",
                ZibeSnackType.WARNING,
                stopLoading = false
            )
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                val msg: String
                val type: ZibeSnackType

                if (task.isSuccessful) {
                    msg = "Instrucciones enviadas a $email"
                    type = ZibeSnackType.SUCCESS
                } else {
                    msg = "No pudimos enviar el correo a $email. Verificá que esté correcto."
                    type = ZibeSnackType.ERROR
                }

                showMessage(msg, type)
            }
    }

    // ================= GOOGLE =================

    fun onGoogleAccountReceived(account: GoogleSignInAccount?) {
        if (account == null) {
            showMessage("Error de Google (account nulo)", ZibeSnackType.ERROR)
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val msg = getAuthErrorMessage(task.exception)
                    showMessage(msg, ZibeSnackType.ERROR)
                } else {
                    handleAuthSuccess()
                }
            }
    }

    // ================= FACEBOOK =================

    fun onFacebookAccessToken(token: AccessToken) {
        _uiState.update { it.copy(isLoading = true) }

        val credential = FacebookAuthProvider.getCredential(token.token)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val msg = getAuthErrorMessage(task.exception)
                    showMessage(msg, ZibeSnackType.ERROR)
                } else {
                    handleAuthSuccess()
                }
            }
    }

    // ================= DELETE USER FLOW =================

    private fun handleAuthSuccess() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            showMessage(
                ERR_ZIBE,
                ZibeSnackType.ERROR
            )
            return
        }

        viewModelScope.launch {
            if (_uiState.value.deleteUser) {
                deleteCurrentUser(user)
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AuthUiEvent.NavigateToSplash)
            }
        }
    }

    private suspend fun deleteCurrentUser(user: FirebaseUser) {
        // Eliminar datos del usuario en RTDB
        firebaseRefsContainer.refDatos.child(user.uid).removeValue()
        firebaseRefsContainer.refCuentas.child(user.uid).removeValue()

        // Eliminar foto de perfil en Storage
        FirebaseStorage.getInstance()
            .getReference("Users/imgPerfil/${user.uid}.jpg")
            .delete()

        // Eliminar usuario de Auth
        user.delete()

        _uiState.update { it.copy(isLoading = false, deleteUser = false) }

        // Avisar a la UI y pedir limpiar prefs
        showMessage(DELETE_ACCOUNT, ZibeSnackType.INFO, stopLoading = false)
        _events.emit(AuthUiEvent.ClearDeletePrefs)
    }

    // ================= DO NOT DELETE =================

    fun onDoNotDeleteClicked() {
        _uiState.update { it.copy(deleteUser = false) }
        showMessage(
            DO_NOT_DELETE_ACCOUNT,
            ZibeSnackType.INFO,
            stopLoading = false
        )
        viewModelScope.launch {
            _events.emit(AuthUiEvent.ClearDeletePrefs)
        }
    }

    // ================= HELPERS =================

    fun showMessage(
        message: String,
        type: ZibeSnackType,
        stopLoading: Boolean = true
    ) {
        if (stopLoading) {
            _uiState.update { it.copy(isLoading = false) }
        }
        viewModelScope.launch {
            _events.emit(AuthUiEvent.ShowSnackbar(message, type))
        }
    }

    private fun validateInputs(
        email: String,
        password: String
    ): Boolean {

        fun warn(msg: String): Boolean {
            showMessage(msg, ZibeSnackType.WARNING, stopLoading = false)
            return false
        }

        if (email.isBlank()) return warn(ERR_EMAIL_REQUIRED)
        if (password.isBlank()) return warn(ERR_PASSWORD_REQUIRED)

        return true
    }

    private fun getAuthErrorMessage(e: Exception?): String {
        if (e == null) {
            return ERR_ZIBE
        }

        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> {
                "Email o contraseña incorrectos."
            }
            is FirebaseAuthInvalidUserException -> {
                "La cuenta no existe o fue deshabilitada."
            }
            is FirebaseAuthUserCollisionException -> {
                "Ya existe una cuenta registrada con este email."
            }
            else -> {
                "Ocurrió un error (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Intentá nuevamente."}"
            }
        }
    }
}
