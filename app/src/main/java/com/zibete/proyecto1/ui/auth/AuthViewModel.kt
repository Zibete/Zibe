package com.zibete.proyecto1.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.zibete.proyecto1.data.UserPreferencesDSRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.DELETE_ACCOUNT
import com.zibete.proyecto1.ui.constants.DO_NOT_DELETE_ACCOUNT
import com.zibete.proyecto1.ui.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.ui.constants.ERR_PASSWORD_REQUIRED
import com.zibete.proyecto1.ui.constants.ERR_ZIBE
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
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userSessionManager: UserSessionManager,
    private val userPreferencesDSRepository: UserPreferencesDSRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthUiEvent>()
    val events = _events.asSharedFlow()

    fun initAfterDelete() {
        val deleteUser = userPreferencesRepository.deleteUser
        val deleteFirebaseAccount = userPreferencesRepository.deleteFirebaseAccount

        _uiState.update { it.copy(deleteUser = deleteUser) }

        if (deleteFirebaseAccount) {
            showMessage(DELETE_ACCOUNT, ZibeSnackType.INFO, stopLoading = false)
            viewModelScope.launch { _events.emit(AuthUiEvent.ClearDeletePrefs) }
        }
    }

    // ================= EMAIL / PASSWORD =================

    fun onEmailLogin(email: String, password: String) {
        viewModelScope.launch {
            if (!validateInputs(email, password)) return@launch
            _uiState.update { it.copy(isLoading = true) }

            runCatching {
                userSessionManager.signInWithEmail(email, password)
            }.onFailure { e ->
                showMessage(getAuthErrorMessage(e), ZibeSnackType.ERROR)
            }.onSuccess {
                handleAuthSuccess()
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            runCatching {
                userSessionManager.sendPasswordResetEmail(email)
            }.onSuccess {
                showMessage("Instrucciones enviadas a $email", ZibeSnackType.SUCCESS)
            }.onFailure {
                showMessage(
                    "No pudimos enviar el correo a $email. Verificá que esté correcto.",
                    ZibeSnackType.ERROR
                )
            }
        }
    }

    // ================= GOOGLE =================

    fun onGoogleAccountReceived(account: GoogleSignInAccount?) {
        if (account == null) {
            showMessage("No se pudo iniciar con Google (account nulo)", ZibeSnackType.ERROR)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            runCatching {
                userSessionManager.signInWithCredential(credential)
            }.onFailure { e ->
                showMessage(getAuthErrorMessage(e), ZibeSnackType.ERROR)
            }.onSuccess {
                handleAuthSuccess()
            }
        }
    }

    // ================= FACEBOOK =================

    fun onFacebookAccessToken(token: AccessToken) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val credential = FacebookAuthProvider.getCredential(token.token)

            runCatching {
                userSessionManager.signInWithCredential(credential)
            }.onFailure { e ->
                showMessage(getAuthErrorMessage(e), ZibeSnackType.ERROR)
            }.onSuccess {
                handleAuthSuccess()
            }
        }
    }

    // ================= DELETE USER FLOW =================

    private fun handleAuthSuccess() {
        val user = userSessionManager.currentUser
        if (user == null) {
            showMessage(ERR_ZIBE, ZibeSnackType.ERROR)
            return
        }

        viewModelScope.launch {
            if (_uiState.value.deleteUser) {
                deleteCurrentUser(user.uid)
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AuthUiEvent.NavigateToSplash)
            }
        }
    }

    private suspend fun deleteCurrentUser(uid: String) {
        runCatching {
            // 1) RTDB (tus nodos)
            firebaseRefsContainer.refData.child(uid).removeValue()
            firebaseRefsContainer.refAccounts.child(uid).removeValue()

            // 2) Storage (usa tu repo, no FirebaseStorage.getInstance())
            runCatching { userRepository.getProfilePhotoStoragePath().delete() }

            // 3) Auth (centralizado)
            userSessionManager.deleteFirebaseUser()

        }.onFailure {
            showMessage("No se pudo eliminar la cuenta. Intentá nuevamente.", ZibeSnackType.ERROR)
            return
        }

        _uiState.update { it.copy(isLoading = false, deleteUser = false) }

        // Avisar + limpiar prefs
        showMessage(DELETE_ACCOUNT, ZibeSnackType.INFO, stopLoading = false)
        _events.emit(AuthUiEvent.ClearDeletePrefs)
    }

    // ================= DO NOT DELETE =================

    fun onDoNotDeleteClicked() {
        _uiState.update { it.copy(deleteUser = false) }
        showMessage(DO_NOT_DELETE_ACCOUNT, ZibeSnackType.INFO, stopLoading = false)
        viewModelScope.launch { _events.emit(AuthUiEvent.ClearDeletePrefs) }
    }

    // ================= HELPERS =================

    fun showMessage(
        message: String,
        type: ZibeSnackType,
        stopLoading: Boolean = true
    ) {
        if (stopLoading) _uiState.update { it.copy(isLoading = false) }
        viewModelScope.launch { _events.emit(AuthUiEvent.ShowSnackbar(message, type)) }
    }

    private fun validateInputs(email: String, password: String): Boolean {

        fun warn(msg: String): Boolean {
            showMessage(msg, ZibeSnackType.WARNING, stopLoading = false)
            return false
        }

        if (email.isBlank()) return warn(ERR_EMAIL_REQUIRED)
        if (password.isBlank()) return warn(ERR_PASSWORD_REQUIRED)
        return true
    }

    private fun getAuthErrorMessage(e: Throwable?): String {
        if (e == null) return ERR_ZIBE

        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> "Email o contraseña incorrectos."
            is FirebaseAuthInvalidUserException -> "La cuenta no existe o fue deshabilitada."
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta registrada con este email."
            else -> "Ocurrió un error (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Intentá nuevamente."}"
        }
    }
}
