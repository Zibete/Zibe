package com.zibete.proyecto1.ui.splash

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.FirebaseRefs.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume

class SplashViewModel : ViewModel() {

    private val _events = MutableSharedFlow<SplashUiEvent>()
    val events = _events.asSharedFlow()

    private var appContext: Context? = null
    private var userToken: String? = null

    fun start(context: Context) {
        // Evitar relanzar lógica si ya se inició
        if (appContext != null) return

        appContext = context.applicationContext

        viewModelScope.launch {
            // Obtener token FCM (no bloqueante)
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        userToken = task.result
                    }
                }

            // Pequeño delay visual (podrías bajarlo o quitarlo si querés)
            delay(1000L)

            val ctx = appContext ?: return@launch
            val currentUser = auth.currentUser

            if (!hasInternetConnection(ctx)) {
                _events.emit(SplashUiEvent.ShowNoInternet)
                return@launch
            }

            if (currentUser == null) {
                auth.signOut()
                LoginManager.getInstance().logOut()
                _events.emit(SplashUiEvent.NavigateAuth)
                return@launch
            }

            if (!hasLocationPermission(ctx)) {
                _events.emit(SplashUiEvent.RequestLocationPermission)
                return@launch
            }

            queryTokenAndRoute(currentUser, ctx)
        }
    }

    // ===================== TOKEN / SESIÓN =====================

    private fun queryTokenAndRoute(user: FirebaseUser, context: Context) {
        viewModelScope.launch {
            val token = userToken

            if (token.isNullOrEmpty()) {
                updateUserFlow(user, context)
                return@launch
            }

            val snapshot = suspendFirebaseQuery {
                FirebaseRefs.refCuentas.orderByChild("token").equalTo(token)
            }

            if (!snapshot.exists()) {
                // Nadie tiene este token → verificar si debo setearlo en mi cuenta
                assignTokenIfNeeded(user, token)
                updateUserFlow(user, context)
                return@launch
            }

            val count = snapshot.childrenCount

            if (count == 1L) {
                val child = snapshot.children.first()
                if (child.key == user.uid) {
                    // Es mi propia cuenta
                    updateUserFlow(user, context)
                } else {
                    val mail = child.child("mail").getValue(String::class.java) ?: "otra cuenta"
                    _events.emit(SplashUiEvent.ShowTokenDialog(mail = mail, flag = 1))
                }
            } else {
                // Más de uno con el mismo token
                val other = snapshot.children.firstOrNull { it.key != user.uid }
                if (other != null) {
                    val mail = other.child("mail").getValue(String::class.java) ?: "otra cuenta"
                    _events.emit(SplashUiEvent.ShowTokenDialog(mail = mail, flag = 2))
                } else {
                    // Caso raro: todos son mi uid
                    updateUserFlow(user, context)
                }
            }
        }
    }

    suspend fun onTokenDialogConfirmed(flag: Int) {
        val user = auth.currentUser ?: return
        val token = userToken ?: return
        val ctx = appContext ?: return

        // Asigno token a mi cuenta
        FirebaseRefs.refCuentas.child(user.uid).child("token").setValue(token)

        if (flag == 1 || flag == 2) {
            // Limpio token de otras cuentas que tenían este token
            val snapshot = suspendFirebaseQuery {
                FirebaseRefs.refCuentas.orderByChild("token").equalTo(token)
            }
            snapshot.children.forEach { child ->
                if (child.key != user.uid) {
                    child.ref.child("token").setValue("")
                }
            }
        }

        updateUserFlow(user, ctx)
    }

    suspend fun onTokenDialogCancelled(flag: Int) {
        val user = auth.currentUser ?: return
        val ctx = appContext ?: return

        if (flag == 2) {
            // Si había varios con el mismo token, limpio mi token
            FirebaseRefs.refCuentas.child(user.uid).child("token").setValue("")
        }

        // Volver al flujo como si no hubiera usuario válido
        auth.signOut()
        LoginManager.getInstance().logOut()
        _events.emit(SplashUiEvent.NavigateAuth)
    }

    private suspend fun assignTokenIfNeeded(user: FirebaseUser, token: String) {
        suspendFirebaseQuery {
            FirebaseRefs.refCuentas.child(user.uid).child("token")
        }.ref.setValue(token)
    }

    // ===================== USER FLOW =====================

    private suspend fun updateUserFlow(user: FirebaseUser, context: Context) {
        val snapshot = suspendFirebaseQuery {
            FirebaseRefs.refCuentas.child(user.uid)
        }

        val prefs = context.getSharedPreferences("flag_Splash", Context.MODE_PRIVATE)
        val firstTime = !prefs.getBoolean("flag_Splash", false)

        if (!snapshot.exists()) {
            createUserNode(user)
            prefs.edit { putBoolean("flag_Splash", true) }
            _events.emit(SplashUiEvent.NavigateEditProfile)
            return
        }

        val birthDay = snapshot.child("birthDay").getValue(String::class.java) ?: ""

        if (firstTime) {
            prefs.edit { putBoolean("flag_Splash", true) }
            _events.emit(SplashUiEvent.NavigateEditProfile)
            return
        }

        if (birthDay.isEmpty()) {
            _events.emit(SplashUiEvent.NavigateEditProfile)
        } else {
            _events.emit(SplashUiEvent.NavigateMain)
        }
    }

    private fun createUserNode(user: FirebaseUser) {
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

        FirebaseRefs.refCuentas.child(user.uid).setValue(newUser)
    }

    // ===================== HELPERS =====================

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

    private suspend fun suspendFirebaseQuery(
        block: () -> com.google.firebase.database.Query
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
