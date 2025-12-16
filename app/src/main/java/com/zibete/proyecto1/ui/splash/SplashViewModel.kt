package com.zibete.proyecto1.ui.splash

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_SESSION_CONFLICT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    fun handleIntentExtras(hasSessionConflict: Boolean) {
        savedStateHandle[EXTRA_SESSION_CONFLICT] = hasSessionConflict
    }

    private val _events = MutableSharedFlow<SplashUiEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events = _events.asSharedFlow()

    fun start(context: Context, isRetry: Boolean = false) {
        viewModelScope.launch {

            if (isRetry) delay(150L)

            // Delay visual opcional
            delay(1000L)

            // 1) Conflicto de sesión externo
            val hasSessionConflict = savedStateHandle.get<Boolean>(EXTRA_SESSION_CONFLICT) ?: false
            if (hasSessionConflict) {
                _events.emit(SplashUiEvent.ShowSessionConflictDialog)
                return@launch
            }

            // 2) OnBoarding solo la primera vez
            if (!userPreferencesRepository.onboardingDone) {
                userPreferencesRepository.onboardingDone = true
                _events.emit(SplashUiEvent.NavigateOnBoarding)
                return@launch
            }

            // 3) Chequeo de internet
            if (!hasInternetConnection(context)) {
                _events.emit(SplashUiEvent.ShowNoInternetDialog)
                return@launch
            }

            // 5) Sin usuario → navegar a Auth
            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser == null) {
                _events.emit(SplashUiEvent.NavigateAuth)
                return@launch
            }

            // 6) Permisos de ubicación
            if (!hasLocationPermission(context)) {
                _events.emit(SplashUiEvent.RequestLocationPermission)
                return@launch
            }

            // 7) Manejo de sesión
            setActiveSession(firebaseUser)

            // 8) Continuar flujo normal
            updateUserFlow(firebaseUser)

        }
    }

    // ============================================================
    // TOKEN FLOW (nuevo schema: fcmToken + email)
    // ============================================================
    suspend fun onSessionConflictConfirmed() {
        val currentUser = firebaseAuth.currentUser ?: return

        setActiveSession(currentUser)
        updateUserFlow(currentUser)
    }

    fun onSessionConflictCancelled() {
        onLogoutRequested()
    }

    suspend fun setActiveSession(currentUser: FirebaseUser) {
        val installId = sessionRepository.getLocalInstallId()
        val fcmToken = sessionRepository.getLocalFcmToken()

        sessionRepository.setActiveSession(
            uid = currentUser.uid,
            installId = installId,
            fcmToken = fcmToken
        )
    }

    fun onLogoutRequested() {
        viewModelScope.launch {
            val intent = userSessionManager.logOutCleanup()
            _events.emit(SplashUiEvent.Navigate(intent))
        }
    }

    // ============================================================
    // USER FLOW
    // ============================================================

    private suspend fun updateUserFlow(firebaseUser: FirebaseUser) {
        val snapshot = userRepository.getAccountSnapshot(firebaseUser.uid)

        // Usuario nuevo: Google o Facebook
        if (!snapshot.exists()) {
            userRepository.createUserNode(firebaseUser, "", "")
            userPreferencesRepository.firstLoginDone = false
            _events.emit(SplashUiEvent.NavigateMain)
            return
        }

        // Perfil incompleto → Main luego enviará a EditProfile
        userPreferencesRepository.firstLoginDone = userRepository.hasBirthDate(firebaseUser.uid)

        _events.emit(SplashUiEvent.NavigateMain)
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private fun hasInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun onExternalSessionConflict() {
        viewModelScope.launch {
            _events.emit(SplashUiEvent.ShowSessionConflictDialog)
        }
    }
}
