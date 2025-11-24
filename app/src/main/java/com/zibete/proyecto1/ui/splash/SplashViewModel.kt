package com.zibete.proyecto1.ui.splash

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
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
import com.zibete.proyecto1.utils.FirebaseRefs.currentUser
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

    // replay = 1 para no perder el último evento
    private val _events = MutableSharedFlow<SplashUiEvent>(replay = 1)
    val events = _events.asSharedFlow()

    private var appContext: Context? = null
    private var userToken: String? = null

    private lateinit var prefs: SharedPreferences

    fun initPrefs(p: SharedPreferences) {
        prefs = p
    }


    fun start(context: Context, isRetry: Boolean = false) {
        // guardamos SIEMPRE applicationContext para evitar leaks
        appContext = context.applicationContext

        viewModelScope.launch {

            if (isRetry) {
                delay(150L)   // parpadeo suave en reintentos
            }
            // 1) Delay visual opcional
            delay(1000L)

            // 2) OnBoarding
            val onBoarding = prefs.getBoolean("onBoarding", false)

            if (!onBoarding) {
                prefs.edit { putBoolean("onBoarding", true) }
                _events.emit(SplashUiEvent.NavigateOnBoarding)
                return@launch
            }

            val ctx = appContext ?: return@launch

            // 3) Chequeo de internet
            if (!hasInternetConnection(ctx)) {
                _events.emit(SplashUiEvent.ShowNoInternetDialog)
                return@launch
            }

            // 4) Obtener token FCM (asíncrono, sin bloquear)
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { userToken = it }



            // 5) Usuario actual desde FirebaseRefs (getter dinámico)
            val user = currentUser

            // Sin usuario → navegar a Auth
            if (user == null) {
                auth.signOut()
                LoginManager.getInstance().logOut()
                _events.emit(SplashUiEvent.NavigateAuth)
                return@launch
            }

            // 6) Permisos de ubicación
            if (!hasLocationPermission(ctx)) {
                _events.emit(SplashUiEvent.RequestLocationPermission)
                return@launch
            }

            // 7) Token + ruta
            queryTokenAndRoute(user, ctx)
        }
    }

    // ============================================================
    // TOKEN FLOW
    // ============================================================

    private fun queryTokenAndRoute(user: FirebaseUser, context: Context) {
        viewModelScope.launch {
            val token = userToken

            // Token vacío → seguir flujo normal
            if (token.isNullOrEmpty()) {
                updateUserFlow(user, context)
                return@launch
            }

            // Buscar cuentas con mismo token
            val snapshot = suspendFirebaseQuery {
                FirebaseRefs.refCuentas.orderByChild("token").equalTo(token)
            }

            if (!snapshot.exists()) {
                assignTokenToUser(user, token)
                updateUserFlow(user, context)
                return@launch
            }

            val accounts = snapshot.children.toList()
            val count = accounts.size

            // Caso 1: solo una cuenta lo tiene
            if (count == 1) {
                val single = accounts.first()
                if (single.key == user.uid) {
                    updateUserFlow(user, context)
                } else {
                    val mail = single.child("mail").getValue(String::class.java) ?: "otra cuenta"
                    _events.emit(SplashUiEvent.ShowTokenDialog(mail, flag = 1))
                }
                return@launch
            }

            // Caso 2: varias cuentas
            val other = accounts.firstOrNull { it.key != user.uid }
            if (other != null) {
                val mail = other.child("mail").getValue(String::class.java) ?: "otra cuenta"
                _events.emit(SplashUiEvent.ShowTokenDialog(mail, flag = 2))
            } else {
                // Caso extremo: todas son mi UID
                updateUserFlow(user, context)
            }
        }
    }

    suspend fun onTokenDialogConfirmed(flag: Int) {
        val currentUser = auth.currentUser ?: return
        val token = userToken ?: return
        val ctx = appContext ?: return

        assignTokenToUser(currentUser, token)

        if (flag == 1 || flag == 2) {
            // limpiar token de otras cuentas
            val snapshot = suspendFirebaseQuery {
                FirebaseRefs.refCuentas.orderByChild("token").equalTo(token)
            }
            snapshot.children.forEach {
                if (it.key != currentUser.uid) {
                    it.ref.child("token").setValue("")
                }
            }
        }

        updateUserFlow(currentUser, ctx)
    }

    suspend fun onTokenDialogCancelled(flag: Int) {
        val ctx = appContext ?: return

        auth.signOut()
        LoginManager.getInstance().logOut()

        _events.emit(SplashUiEvent.NavigateAuth)
    }

    private fun assignTokenToUser(user: FirebaseUser, token: String) {
        FirebaseRefs.refCuentas.child(user.uid).child("token").setValue(token)
    }

    // ============================================================
    // USER FLOW
    // ============================================================

    private suspend fun updateUserFlow(user: FirebaseUser, context: Context) {
        val snapshot = suspendFirebaseQuery {
            FirebaseRefs.refCuentas.child(user.uid)
        }

        val prefs = context.getSharedPreferences("flag_Splash", Context.MODE_PRIVATE)
        val firstTime = !prefs.getBoolean("flag_Splash", false)

        // Usuario nuevo
        if (!snapshot.exists()) {
            createUserNode(user)
            prefs.edit { putBoolean("flag_Splash", true) }
            _events.emit(SplashUiEvent.NavigateEditProfile)
            return
        }

        // Primer inicio (post registro)
        if (firstTime) {
            prefs.edit { putBoolean("flag_Splash", true) }
            _events.emit(SplashUiEvent.NavigateEditProfile)
            return
        }

        // Usuario ya existente → verificar perfil
        val birthDay = snapshot.child("birthDay").getValue(String::class.java) ?: ""

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
