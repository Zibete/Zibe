package com.zibete.proyecto1.ui.splash

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.core.utils.AppChecksProvider
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
    private val userSessionProvider: UserSessionProvider,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions,
    private val sessionBootstrapper: SessionBootstrapper,
    private val logoutUseCase: LogoutUseCase
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
            if (!userPreferencesProvider.isOnboardingDone()) {
                userPreferencesActions.setOnboardingDone(true)
                _events.emit(SplashUiEvent.NavigateOnBoarding)
                return@launch
            }

            // 3) Internet
            if (!appChecksProvider.hasInternetConnection(context)) {
                _events.emit(SplashUiEvent.ShowNoInternetDialog)
                return@launch
            }

            // 4) Sin usuario → Auth
            val currentUser = userSessionProvider.currentUser
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
            continueToMain(currentUser)
        }
    }

    // ============================================================
    // SESSION
    // ============================================================

    fun onSessionConflictConfirmed() {
        val currentUser = userSessionProvider.currentUser ?: return
        viewModelScope.launch { continueToMain(currentUser) }
    }

    suspend fun continueToMain(currentUser: FirebaseUser){
        sessionBootstrapper.bootstrap(currentUser.uid)
        _events.emit(SplashUiEvent.NavigateMain)
    }

    fun onSessionConflictCancelled() {
        onLogoutRequested()
    }

    fun onLogoutRequested() {
        viewModelScope.launch {
            logoutUseCase.execute()
            _events.emit(SplashUiEvent.NavigateAuth)
        }
    }

    fun onExternalSessionConflict() {
        viewModelScope.launch {
            _events.emit(SplashUiEvent.ShowSessionConflictDialog)
        }
    }
}
