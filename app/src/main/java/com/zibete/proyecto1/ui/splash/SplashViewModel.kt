package com.zibete.proyecto1.ui.splash

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    private val _events = MutableSharedFlow<SplashUiEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events = _events.asSharedFlow()

    fun handleIntentExtras(hasSessionConflict: Boolean) {
        savedStateHandle[EXTRA_SESSION_CONFLICT] = hasSessionConflict
    }

    fun start(context: Context, isRetry: Boolean = false) {
        viewModelScope.launch {

            if (isRetry) delay(150L)

            // Delay visual opcional
            delay(1000L)

            // 1) Conflicto de sesión externo (viene por Intent Extra)
            val hasSessionConflict = savedStateHandle.get<Boolean>(EXTRA_SESSION_CONFLICT) ?: false
            if (hasSessionConflict) {
                _events.emit(SplashUiEvent.ShowSessionConflictDialog)
                return@launch
            }

            // 2) Onboarding solo la primera vez
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

            // 4) Sin usuario → Auth (fuente de verdad: UserSessionManager)
            val currentUser = userSessionManager.currentUser
            if (currentUser == null) {
                _events.emit(SplashUiEvent.NavigateAuth)
                return@launch
            }

            // 5) Permisos de ubicación
            if (!hasLocationPermission(context)) {
                _events.emit(SplashUiEvent.RequestLocationPermission)
                return@launch
            }

            // 6) Manejo de sesión (installId + fcmToken)
            setActiveSession(currentUser.uid)

            // 7) Continuar flujo normal
            updateUserFlow(currentUser.uid)
        }
    }

    // ============================================================
    // SESSION CONFLICT
    // ============================================================

    suspend fun onSessionConflictConfirmed() {
        val currentUser = userSessionManager.currentUser ?: return
        setActiveSession(currentUser.uid)
        updateUserFlow(currentUser.uid)
    }

    fun onSessionConflictCancelled() {
        onLogoutRequested()
    }

    private suspend fun setActiveSession(uid: String) {
        val installId = sessionRepository.getLocalInstallId()
        val fcmToken = sessionRepository.getLocalFcmToken()

        sessionRepository.setActiveSession(
            uid = uid,
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

    fun onExternalSessionConflict() {
        viewModelScope.launch {
            _events.emit(SplashUiEvent.ShowSessionConflictDialog)
        }
    }

    // ============================================================
    // USER FLOW
    // ============================================================

    private suspend fun updateUserFlow(uid: String) {
        val snapshot = userRepository.getAccountSnapshot(uid)

        // Usuario nuevo: crear nodo y marcar que falta completar perfil
        if (!snapshot.exists()) {
            val firebaseUser = userSessionManager.currentUser
            if (firebaseUser != null) {
                userRepository.createUserNode(firebaseUser, "", "")
            }
            userPreferencesRepository.firstLoginDone = false
            _events.emit(SplashUiEvent.NavigateMain)
            return
        }

        // Perfil incompleto → Main luego enviará a EditProfile
        userPreferencesRepository.firstLoginDone = userRepository.hasBirthDate(uid)
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
}
