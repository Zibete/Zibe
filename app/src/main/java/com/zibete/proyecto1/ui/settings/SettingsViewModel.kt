package com.zibete.proyecto1.ui.settings

import android.content.Context
import android.content.Intent
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.data.UserSessionManager.AuthProvider
import com.zibete.proyecto1.ui.splash.SplashActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userSessionManager: UserSessionManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val firebaseUser = userRepository.firebaseUser
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsUiEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()
    
    init {
        refreshState()
    }

    fun setUserOnline() = viewModelScope.launch { userRepository.setUserOnline() }
    fun setUserOffline() = viewModelScope.launch { userRepository.setUserLastSeen() }

    fun onChangeEmailHeaderClicked() {
        val state = _uiState.value
        if (!state.canChangeCredentials) {
            emitSnack("No puedes cambiar el email porque iniciaste sesión con ${state.providerLabel}.")
            return
        }
        _uiState.update {
            it.copy(
                isEmailSectionExpanded = !it.isEmailSectionExpanded,
                isPasswordSectionExpanded = false
            )
        }
    }

    fun onChangePasswordHeaderClicked() {
        val state = _uiState.value
        if (!state.canChangeCredentials) {
            emitSnack("No puedes cambiar la contraseña porque iniciaste sesión con ${state.providerLabel}.")
            return
        }
        _uiState.update {
            it.copy(
                isPasswordSectionExpanded = !it.isPasswordSectionExpanded,
                isEmailSectionExpanded = false
            )
        }
    }

    fun onGroupNotificationsToggled(enabled: Boolean) {
        userPreferencesRepository.groupNotifications = enabled
        _uiState.update { it.copy(groupNotificationsEnabled = enabled) }
        emitSnack(if (enabled) "Notificaciones grupales encendidas" else "Notificaciones grupales apagadas")
    }

    fun onIndividualNotificationsToggled(enabled: Boolean) {
        userPreferencesRepository.individualNotifications = enabled
        _uiState.update { it.copy(individualNotificationsEnabled = enabled) }
        emitSnack(if (enabled) "Notificaciones individuales encendidas" else "Notificaciones individuales apagadas")
    }

    fun updateEmail(password: String, newEmail: String) {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.canChangeCredentials) {
                emitSnack("No puedes cambiar el email porque iniciaste sesión con ${state.providerLabel}.")
                return@launch
            }

            val email = newEmail.trim()
            if (password.isBlank() || email.isBlank()) {
                emitSnack("No deje campos vacíos")
                return@launch
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emitSnack("Introduzca un e-mail válido")
                return@launch
            }

            emitProgress("Cambiando email...")

            val okReauth = reauthenticate(AuthProvider.PASSWORD, password)
            if (!okReauth) {
                emitHideProgress()
                emitSnack("La contraseña es incorrecta")
                return@launch
            }

            runCatching { firebaseUser.updateEmail(email).await() }
                .onFailure {
                    emitHideProgress()
                    emitSnack("Error al actualizar email")
                    return@launch
                }

            // DB + cache local
            runCatching { userRepository.updateEmail(email) }
            userRepository.updateLocalProfile(
                name = userRepository.myUserName,
                photoUrl = userRepository.myProfilePhotoUrl,
                email = email
            )

            emitHideProgress()
            emitSnack("Email actualizado correctamente")
            emitNavigateToSplash()
        }
    }

    fun updatePassword(password: String, newPassword: String) {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.canChangeCredentials) {
                emitSnack("No puedes cambiar la contraseña porque iniciaste sesión con ${state.providerLabel}.")
                return@launch
            }

            if (password.isBlank() || newPassword.isBlank()) {
                emitSnack("No deje campos vacíos")
                return@launch
            }
            if (newPassword.length < 6) {
                emitSnack("La contraseña debe tener al menos seis caracteres")
                return@launch
            }

            emitProgress("Cambiando contraseña...")

            val okReauth = reauthenticate(AuthProvider.PASSWORD, password)
            if (!okReauth) {
                emitHideProgress()
                emitSnack("La contraseña es incorrecta")
                return@launch
            }

            runCatching { firebaseUser.updatePassword(newPassword).await() }
                .onFailure {
                    emitHideProgress()
                    emitSnack("Error al actualizar contraseña")
                    return@launch
                }

            emitHideProgress()
            emitSnack("Contraseña actualizada correctamente")
            emitNavigateToSplash()
        }
    }

    fun deleteAccount(passwordIfNeeded: String?) {
        viewModelScope.launch {
            emitProgress("Eliminando cuenta...")

            val providerType = userSessionManager.authProvider()
            val requiresPassword = providerType == AuthProvider.PASSWORD

            if (requiresPassword) {
                val pass = passwordIfNeeded.orEmpty()
                if (pass.isBlank()) {
                    emitHideProgress()
                    emitSnack("Introduzca su contraseña")
                    return@launch
                }
                val okReauth = reauthenticate(AuthProvider.PASSWORD, pass)
                if (!okReauth) {
                    emitHideProgress()
                    emitSnack("La contraseña es incorrecta")
                    return@launch
                }
            } else {
                // Best-effort para Google/Facebook: si falla, seguimos (comportamiento cercano al actual).
                val okReauth = reauthenticate(providerType, null)
                if (!okReauth) {
                    emitSnack("No se pudo revalidar la sesión. Intentaré continuar…")
                }
            }

            // 1) Intentar borrar Auth (si falla, seguimos borrando datos igualmente)
            runCatching { userSessionManager.deleteFirebaseUser() }
                .onFailure {
                    emitSnack("No se pudo eliminar la cuenta. Por seguridad, volvé a iniciar sesión y reintentá")
                    return@launch
                }

            // 2) Borrar DB + Storage
            runCatching { userRepository.deleteMyAccountData() }
                .onFailure {
                    emitHideProgress()
                    emitSnack("Error eliminando datos de la cuenta")
                    return@launch
                }

            emitHideProgress()
            // 3) Logout
            logOut()

        }
    }

    fun logOut() {
        viewModelScope.launch {
            val intent = userSessionManager.logOutCleanup()
            _events.tryEmit(SettingsUiEvent.Navigate(intent, finish = true))
        }
    }

    // -------------------------
    // Private
    // -------------------------

    private fun refreshState() {
        val providerType = userSessionManager.authProvider()
        
        val providerLabel = userSessionManager.authProviderLabel()
        
        val canChangeCredentials = providerType == AuthProvider.PASSWORD
        
        val emailBase = userRepository.myEmail
            .takeIf { it.isNotBlank() }
            ?: (firebaseUser.email ?: "")

        val display = buildString {
            append(emailBase)
            providerLabel?.let { append("\n($it)") }
        }

        _uiState.update {
            it.copy(
                emailDisplay = display,
                providerLabel = providerLabel,
                canChangeCredentials = canChangeCredentials,
                requiresPasswordForSensitiveActions = canChangeCredentials,
                groupNotificationsEnabled = userPreferencesRepository.groupNotifications,
                individualNotificationsEnabled = userPreferencesRepository.individualNotifications
            )
        }
    }


    private suspend fun reauthenticate(authProvider: AuthProvider, password: String?): Boolean {
        val credential = when (authProvider) {
            AuthProvider.PASSWORD -> {
                val email = firebaseUser.email.orEmpty()
                if (email.isBlank() || password.isNullOrBlank()) return false
                EmailAuthProvider.getCredential(email, password)
            }

            AuthProvider.GOOGLE -> {
                val acct = GoogleSignIn.getLastSignedInAccount(appContext) ?: return false
                val token = acct.idToken ?: return false
                GoogleAuthProvider.getCredential(token, null)
            }

            AuthProvider.FACEBOOK -> {
                val token = AccessToken.getCurrentAccessToken()?.token ?: return false
                FacebookAuthProvider.getCredential(token)
            }

            AuthProvider.OTHER -> return false
        }

        return runCatching {
            firebaseUser.reauthenticate(credential).await()
            true
        }.getOrDefault(false)
    }

    private fun emitSnack(message: String) {
        _events.tryEmit(SettingsUiEvent.ShowSnack(message))
    }

    private fun emitProgress(message: String) {
        _events.tryEmit(SettingsUiEvent.ShowProgress(message))
    }

    private fun emitHideProgress() {
        _events.tryEmit(SettingsUiEvent.HideProgress)
    }

    private fun emitNavigateToSplash() {
        val intent = Intent(appContext, SplashActivity::class.java)
        _events.tryEmit(SettingsUiEvent.Navigate(intent, finish = true))
    }
}
