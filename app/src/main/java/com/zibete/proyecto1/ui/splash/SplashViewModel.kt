package com.zibete.proyecto1.ui.splash

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.constants.Constants.EXTRA_DELETE_ACCOUNT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_UI_TEXT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SNACK_TYPE
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.AppChecksProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authSessionProvider: AuthSessionProvider,
    private val savedStateHandle: SavedStateHandle,
    private val appChecksProvider: AppChecksProvider,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions,
    private val sessionBootstrapper: SessionBootstrapper,
    private val logoutUseCase: LogoutUseCase,
    private val snackBarManager: SnackBarManager
) : ViewModel() {

    private val _events = MutableSharedFlow<SplashUiEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events = _events.asSharedFlow()

    fun handleIntentExtras(
        uiText: UiText?,
        snackType: ZibeSnackType?,
        hasSessionConflict: Boolean,
        deleteAccount: Boolean
    ) {
        savedStateHandle[EXTRA_UI_TEXT] = uiText
        savedStateHandle[EXTRA_SNACK_TYPE] = snackType
        savedStateHandle[EXTRA_SESSION_CONFLICT] = hasSessionConflict
        savedStateHandle[EXTRA_DELETE_ACCOUNT] = deleteAccount
    }

    fun start(context: Context, isRetry: Boolean = false) {
        viewModelScope.launch {
            if (isRetry) delay(150L)
            delay(1000L) // delay visual

            // 1) Capturamos datos del snack si existen
            val uiText = savedStateHandle.get<UiText>(EXTRA_UI_TEXT)
            val snackType = savedStateHandle.get<ZibeSnackType>(EXTRA_SNACK_TYPE)

            // 2) Delete account
            val shouldDelete = savedStateHandle.get<Boolean>(EXTRA_DELETE_ACCOUNT) ?: false
            if (shouldDelete) {
                _events.emit(SplashUiEvent.NavigateAuth)
                return@launch
            }

            // 3) Conflicto de sesión externo
            val hasSessionConflict =
                savedStateHandle.get<Boolean>(EXTRA_SESSION_CONFLICT) ?: false
            if (hasSessionConflict) {
                _events.emit(SplashUiEvent.ShowSessionConflictDialog)
                return@launch
            }

            // 4) Onboarding (solo una vez)
            if (!userPreferencesProvider.isOnboardingDone()) {
                userPreferencesActions.setOnboardingDone(true)
                _events.emit(SplashUiEvent.NavigateOnBoarding)
                return@launch
            }

            // 5) Internet
            if (!appChecksProvider.hasInternetConnection(context)) {
                _events.emit(SplashUiEvent.ShowNoInternetDialog)
                return@launch
            }

            // 6) Sin usuario → Auth
            val currentUser = authSessionProvider.currentUser
            if (currentUser == null) {
                _events.emit(SplashUiEvent.NavigateAuth)
                return@launch
            }

            // 7) Permisos de ubicación
            if (!appChecksProvider.hasLocationPermission(context)) {
                _events.emit(SplashUiEvent.NavigatePermission)
                return@launch
            }

            // 8) Sesión activa (bootstrap + navegar con data del snack si aplica)
            continueToMain(
                uiText = uiText,
                snackType = snackType,
                currentUser = currentUser
            )
        }
    }

    // ============================================================
    // SESSION
    // ============================================================

    fun onSessionConflictConfirmed() {
        val currentUser = authSessionProvider.currentUser ?: return
        viewModelScope.launch { continueToMain(null, null, currentUser) }
    }

    suspend fun continueToMain(
        uiText: UiText?,
        snackType: ZibeSnackType?,
        currentUser: FirebaseUser
    ) {
        sessionBootstrapper.bootstrap(currentUser.uid)
        _events.emit(SplashUiEvent.NavigateMain(uiText, snackType))
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
}
