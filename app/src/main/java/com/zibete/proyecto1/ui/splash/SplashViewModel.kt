package com.zibete.proyecto1.ui.splash

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.FirebaseRefs.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume

class SplashViewModel : ViewModel() {

    // ===================== STATE =====================

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState = _uiState.asStateFlow()

    // ===================== EVENTS (one-shot) =====================

    private val _events = MutableSharedFlow<SplashUiEvent>()
    val events = _events.asSharedFlow()

    private var userToken: String? = null
    private lateinit var appContext: Context

    // ===================== START LOGIC =====================

    fun start(context: Context) {
        appContext = context.applicationContext

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, needsPermission = false)

            // Obtener token FCM (no bloqueante)
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        userToken = task.result
                    }
                }

            // Pequeño delay de splash
            delay(1000)

            val currentUser = auth.currentUser

            if (!hasInternetConnection(appContext)) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.emit(SplashUiEvent.ShowNoInternet)
                return@launch
            }

            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                auth.signOut()
                LoginManager.getInstance().logOut()
                _events.emit(SplashUiEvent.NavigateAuth)
                return@launch
            }

            if (!hasLocationPermission(appContext)) {
                _uiState.value = _uiState.value.copy(isLoading = false, needsPermission = true)
                _events.emit(SplashUiEvent.RequestLocationPermission)
                return@launch
            }

            queryTokenCoroutine(currentUser)
        }
    }

    // ===================== TOKEN LOGIC =====================

    private fun queryTokenCoroutine(user: FirebaseUser) {
        viewModelScope.launch {

            val token = userToken

            if (token.isNullOrEmpty()) {
                updateUserFlow(user)
                return@launch
            }

            val snapshot = suspendFirebaseQuery {
                FirebaseRefs.refCuentas.orderByChild("token").equalTo(token)
            }

            if (!snapshot.exists()) {
                assignTokenIfNeeded(user, token)
                updateUserFlow(user)
                return@launch
            }

            val count = snapshot.childrenCount

            if (count == 1L) {
                val child = snapshot.children.first()

                if (child.key == user.uid) {
                    updateUserFlow(user)
                } else {
                    val mail = child.child("mail").getValue(String::class.java) ?: ""
                    _events.emit(SplashUiEvent.ShowTokenDialog(mail, 1))
                }

            } else {
                val other = snapshot.children.firstOrNull { it.key != user.uid }
                if (other != null) {
                    val mail = other.child("mail").getValue(String::class.java) ?: ""
                    _events.emit(SplashUiEvent.ShowTokenDialog(mail, 2))
                }
            }
        }
    }

    private suspend fun assignTokenIfNeeded(user: FirebaseUser, token: String) {
        suspendFirebaseQuery {
            FirebaseRefs.refCuentas.child(user.uid).child("token")
        }.ref.setValue(token)
    }

    // ===================== USER FLOW =====================

    private suspend fun updateUserFlow(user: FirebaseUser) {

        val snapshot = suspendFirebaseQuery {
            FirebaseRefs.refCuentas.child(user.uid)
        }

        val prefs = appContext.getSharedPreferences("flag_Splash", Context.MODE_PRIVATE)
        val firstTime = !prefs.getBoolean("flag_Splash", false)

        if (!snapshot.exists()) {
            createUserNode(user)
            _uiState.value = _uiState.value.copy(isLoading = false)
            _events.emit(SplashUiEvent.NavigateEditProfile)
            return
        }

        if (firstTime) {
            setSplashFlag(appContext)
            _uiState.value = _uiState.value.copy(isLoading = false)
            _events.emit(SplashUiEvent.NavigateEditProfile)
            return
        }

        val birthDay = snapshot.child("birthDay").getValue(String::class.java) ?: ""

        _uiState.value = _uiState.value.copy(isLoading = false)

        if (birthDay.isEmpty()) {
            _events.emit(SplashUiEvent.NavigateEditProfile)
        } else {
            _events.emit(SplashUiEvent.NavigateMain)
        }
    }

    private suspend fun createUserNode(user: FirebaseUser) {
        val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Calendar.getInstance().time)

        val newUser = Users(
            user.uid,
            user.displayName ?: "",
            "",
            now,
            0,
            user.email ?: "",
            user.photoUrl?.toString() ?: "",
            true,
            userToken ?: "",
            0.0,
            "",
            0.0,
            0.0
        )

        suspendFirebaseQuery {
            FirebaseRefs.refCuentas.child(user.uid)
        }.ref.setValue(newUser)
    }

    private fun setSplashFlag(context: Context) {
        context.getSharedPreferences("flag_Splash", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("flag_Splash", true)
            .apply()
    }

    // ===================== TOKEN DIALOG HANDLERS =====================

    fun onTokenDialogConfirmed(flag: Int) {
        viewModelScope.launch {
            val user = auth.currentUser ?: run {
                _events.emit(SplashUiEvent.NavigateAuth)
                return@launch
            }

            val token = userToken
            if (token.isNullOrEmpty()) {
                updateUserFlow(user)
                return@launch
            }

            val snapshot = suspendFirebaseQuery {
                FirebaseRefs.refCuentas.orderByChild("token").equalTo(token)
            }

            for (child in snapshot.children) {
                if (child.key == user.uid) {
                    child.child("token").ref.setValue(token)
                } else {
                    child.child("token").ref.setValue("")
                }
            }

            updateUserFlow(user)
        }
    }

    fun onTokenDialogCancelled(flag: Int) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (flag == 2 && user != null) {
                FirebaseRefs.refCuentas.child(user.uid).child("token").setValue("")
            }
            auth.signOut()
            LoginManager.getInstance().logOut()
            _events.emit(SplashUiEvent.NavigateAuth)
        }
    }

    // ===================== HELPERS =====================

    private fun hasInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        @Suppress("DEPRECATION")
        return cm.activeNetworkInfo?.isConnected == true
    }

    private fun hasLocationPermission(context: Context): Boolean {
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        val granted = ContextCompat.checkSelfPermission(context, permission)
        return granted == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private suspend fun suspendFirebaseQuery(
        block: () -> Query
    ): DataSnapshot = suspendCancellableCoroutine { cont ->
        block().addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resume(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.cancel()
            }
        })
    }
}
