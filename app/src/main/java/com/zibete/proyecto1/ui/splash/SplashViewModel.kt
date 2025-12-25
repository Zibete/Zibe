package com.zibete.proyecto1.ui.splash

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.utils.AppChecksProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val appChecksProvider: AppChecksProvider,
    private val sessionProvider: UserSessionProvider,
    private val sessionActions: UserSessionActions,
    private val preferencesProvider: UserPreferencesProvider,
    private val preferencesActions: UserPreferencesActions,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
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
            delay(1000L) // delay visual

            // 1) Conflicto de sesión externo
            val hasSessionConflict =
                savedStateHandle.get<Boolean>(EXTRA_SESSION_CONFLICT) ?: false
            if (hasSessionConflict) {
                _events.emit(SplashUiEvent.ShowSessionConflictDialog)
                return@launch
            }

            // 2) Onboarding (solo una vez)
            val onboardingDone =
                preferencesProvider.isOnboardingDone()
            if (!onboardingDone) {
                preferencesActions.setOnboardingDone(true)
                _events.emit(SplashUiEvent.NavigateOnBoarding)
                return@launch
            }

            // 3) Internet
            if (!appChecksProvider.hasInternetConnection(context)) {
                _events.emit(SplashUiEvent.ShowNoInternetDialog)
                return@launch
            }

            // 4) Sin usuario → Auth
            val currentUser = sessionProvider.currentUser
            if (currentUser == null) {
                _events.emit(SplashUiEvent.NavigateAuth)
                return@launch
            }

            // 5) Permisos de ubicación
            if (!appChecksProvider.hasLocationPermission(context)) {
                _events.emit(SplashUiEvent.RequestLocationPermission)
                return@launch
            }

            // 6) Sesión activa (installId + fcmToken)
            setActiveSession(currentUser.uid)

            // 7) Continuar flujo
            updateUserFlow(currentUser.uid)
        }
    }

    // ============================================================
    // SESSION CONFLICT
    // ============================================================

    suspend fun onSessionConflictConfirmed() {
        val currentUser = sessionProvider.currentUser ?: return
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
            userRepository.setUserLastSeen()
            val intent = sessionActions.logOutCleanup()
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

        // Usuario nuevo
        if (!snapshot.exists()) {
            sessionProvider.currentUser?.let {
                userRepository.createUserNode(it, "", "")
            }
            preferencesActions.setFirstLoginDone(false)
            _events.emit(SplashUiEvent.NavigateMain)
            return
        }

        // Perfil incompleto / completo
        val hasBirthDate = userRepository.hasBirthDate(uid)
        preferencesActions.setFirstLoginDone(hasBirthDate)

        _events.emit(SplashUiEvent.NavigateMain)
    }
}
