package com.zibete.proyecto1.ui.splash

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    private val firebaseUser = userRepository.firebaseUser


    // replay = 1 para no perder el último evento (ej. sin internet antes de que Compose empiece a colectar)
    private val _events = MutableSharedFlow<SplashUiEvent>(replay = 1)
    val events = _events.asSharedFlow()

    private var userToken: String? = null

    fun start(context: Context, isRetry: Boolean = false) {
        viewModelScope.launch {

            if (isRetry) delay(150L)

            // 1) Delay visual opcional
            delay(1000L)

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

            // 4) Obtener token FCM (espera real para no correr sin token)
            userToken = runCatching {
                FirebaseMessaging.getInstance().token.await()
            }.getOrNull()

            // 5) Sin usuario → navegar a Auth
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                _events.emit(SplashUiEvent.NavigateAuth)
                return@launch
            }

            // 6) Permisos de ubicación
            if (!hasLocationPermission(context)) {
                _events.emit(SplashUiEvent.RequestLocationPermission)
                return@launch
            }

            // 7) Token + ruta
            routeWithTokenIfNeeded(currentUser)
        }
    }

    // ============================================================
    // TOKEN FLOW (nuevo schema: fcmToken + email)
    // ============================================================

    private fun routeWithTokenIfNeeded(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            val token = userToken

            // Token vacío → seguir flujo normal
            if (token.isNullOrEmpty()) {
                updateUserFlow(firebaseUser)
                return@launch
            }

            val snapshot = userRepository.findAccountsByFcmToken(token)

            // Nadie tiene el token → asignar y seguir
            if (!snapshot.exists()) {
                userRepository.setFcmToken(firebaseUser.uid, token)
                updateUserFlow(firebaseUser)
                return@launch
            }

            val accounts = snapshot.children.toList()

            // Solo una cuenta lo tiene
            if (accounts.size == 1) {
                val single = accounts.first()
                if (single.key == firebaseUser.uid) {
                    updateUserFlow(firebaseUser)
                } else {
                    val email = single.child("email").getValue(String::class.java) ?: "otra cuenta"
                    onExternalSessionConflict(email, flag = 1)
                }
                return@launch
            }

            // Varias cuentas: conflicto si hay alguna distinta a la mía
            val other = accounts.firstOrNull { it.key != firebaseUser.uid }
            if (other != null) {
                val email = other.child("email").getValue(String::class.java) ?: "otra cuenta"
                onExternalSessionConflict(email, flag = 2)
            } else {
                updateUserFlow(firebaseUser)
            }
        }
    }

    suspend fun onTokenDialogConfirmed(flag: Int) {
        val currentUser = firebaseAuth.currentUser ?: return
        val token = userToken ?: return

        // asignar token a mi cuenta
        userRepository.setFcmToken(currentUser.uid, token)

        // limpiar token de otras cuentas (si aplica)
        if (flag == 1 || flag == 2) {
            userRepository.clearFcmTokenFromOtherAccounts(
                token = token,
                keepUid = currentUser.uid
            )
        }

        updateUserFlow(currentUser)
    }

    suspend fun onTokenDialogCancelled(flag: Int) {
        firebaseAuth.signOut()
        LoginManager.getInstance().logOut()
        _events.emit(SplashUiEvent.NavigateAuth)
    }

    // ============================================================
    // USER FLOW
    // ============================================================

    private suspend fun updateUserFlow(firebaseUser: FirebaseUser) {
        val snapshot = userRepository.getAccountSnapshot(firebaseUser.uid)

        // Usuario nuevo
        if (!snapshot.exists()) {
            userRepository.createUserNode(firebaseUser, userToken)
            userPreferencesRepository.firstLoginDone = false
            _events.emit(SplashUiEvent.NavigateMain)
            return
        }

        // Usuario existente → verificar perfil
        val birthDay = snapshot.child("birthDay").getValue(String::class.java).orEmpty()

        // Perfil incompleto → Main luego enviará a EditProfile
        userPreferencesRepository.firstLoginDone = birthDay.isNotEmpty()

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

    fun onExternalSessionConflict(email: String, flag: Int) {
        viewModelScope.launch {
            _events.emit(SplashUiEvent.ShowTokenDialog(email, flag))
        }
    }
}
