// AuthActivity.kt
package com.zibete.proyecto1.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.storage.FirebaseStorage
import com.zibete.proyecto1.OnBoardingActivity
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.signup.SignUpActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.FirebaseRefs.auth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var deleteUser: Boolean = false
    private var deleteFirebaseAccount: Boolean = false
    private var googleSignInClient: GoogleSignInClient? = null
    sealed class AuthEvent {
        data class ShowSnackbar(
            val message: String,
            val type: ZibeSnackType
        ) : AuthEvent()
        // Más adelante podés sumar:
        // object NavigateToHome : AuthEvent()
    }

    private val authEvents = MutableSharedFlow<AuthEvent>()
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    // Launcher de Google
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGoogleResult(result)
        }
    // Launcher de Facebook
    private lateinit var callbackManager: CallbackManager
    private lateinit var loginManager: LoginManager
    private lateinit var facebookLauncher:
            ActivityResultLauncher<Collection<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preferencias
        val prefs = getSharedPreferences("AuthPreferences", MODE_PRIVATE)
        val editor = prefs.edit()
        val onBoarding = prefs.getBoolean("onBoarding", false)
        deleteUser = prefs.getBoolean("deleteUser", false)
        deleteFirebaseAccount = prefs.getBoolean("deleteFirebaseAccount", false)

        // OnBoarding una sola vez
        if (!onBoarding) {
            startActivity(Intent(this, OnBoardingActivity::class.java))
            editor.putBoolean("onBoarding", true).apply()
        }

        // Google
        setupGoogleSignIn()
        // Facebook
        setupFacebookSignIn()
        // Listener de auth
        setupAuthListener()

        // Mensaje de cuenta eliminada
        if (deleteFirebaseAccount) {
            emitEvent("La cuenta ha sido eliminada", ZibeSnackType.INFO, stopLoading = true)
            editor.putBoolean("deleteFirebaseAccount", false).apply()
        }

        // UI Compose
        setContent {
            ZibeTheme {
                // Estado Compose para deleteUser (para mostrar/ocultar secciones)
                var deleteUserState by remember { mutableStateOf(deleteUser) }
                val isLoading by isLoading.collectAsStateWithLifecycle()

                AuthScreen(
                    deleteUser = deleteUserState,
                    onLogin = { email, password ->
                        _isLoading.value = true
                        doEmailLogin(email, password)
                    },
                    onNavigateToSignUp = {
                        startActivity(Intent(this, SignUpActivity::class.java))
                    },
                    onResetPassword = { email ->
                        _isLoading.value = true
                        resetPassword(email)
                    },

                    onGoogleClick = {
                        _isLoading.value = true
                        googleSignInClient?.signOut()
                            ?.addOnCompleteListener { _: Task<Void?>? ->
                                val intent = googleSignInClient?.signInIntent
                                if (intent != null) {
                                    googleSignInLauncher.launch(intent)
                                } else {
                                    emitEvent("No se pudo abrir Google Sign-In", ZibeSnackType.ERROR, stopLoading = true)
                                }
                            }
                    },

                    onFacebookClick = {
                        _isLoading.value = true
                        facebookLauncher.launch(listOf("public_profile", "email"))

                    },
                    onDoNotDelete = {
                        doNotDelete()
                        deleteUserState = false
                    },
                    isLoading = isLoading,
                    authEvents = authEvents
                )
            }
        }
    }

    private fun setupFacebookSignIn() {

        callbackManager = CallbackManager.Factory.create()
        loginManager = LoginManager.getInstance()

        facebookLauncher = registerForActivityResult(
            loginManager.createLogInActivityResultContract(callbackManager, null)
        ) { /* vacío: el resultado lo maneja callbackManager */ }

        // Callback clásico: éxito / cancel / error
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                handleFacebookAccessToken(result.accessToken)
            }

            override fun onCancel() {
                emitEvent("Inicio con Facebook cancelado", ZibeSnackType.INFO, stopLoading = true)
                updateUI(null)
            }

            override fun onError(error: FacebookException) {
                emitEvent("Error con Facebook: ${error.localizedMessage}", ZibeSnackType.ERROR, stopLoading = true)
                updateUI(null)
            }
        })
    }

    private fun setupAuthListener() {
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth  ->
            val user = firebaseAuth.currentUser

            if (user != null) {
                if (deleteUser) {
                    // Eliminar datos del usuario
                    FirebaseRefs.refDatos.child(user.uid).removeValue()
                    FirebaseRefs.refCuentas.child(user.uid).removeValue()
                    FirebaseStorage.getInstance()
                        .getReference("Users/imgPerfil/${user.uid}.jpg")
                        .delete()

                    user.delete()
                    doNotDelete() // limpia flags

                    emitEvent("La cuenta ha sido eliminada", ZibeSnackType.INFO, stopLoading = true)

                } else {
                    updateUI(user)
                }
            }
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun handleGoogleResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK && result.data != null) {
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)

                if (account != null) firebaseAuthWithGoogle(account)
                else {
                    emitEvent("Error de Google (account nulo)", ZibeSnackType.ERROR, stopLoading = true)
                    updateUI(null)
                }
            } catch (e: ApiException) {
                emitEvent("Error de Google: ${e.statusCode}", ZibeSnackType.ERROR, stopLoading = true)
                updateUI(null)
            }
        } else {
            emitEvent("Inicio cancelado", ZibeSnackType.INFO, stopLoading = true)
            updateUI(null)
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    val msg = getAuthErrorMessage(task.exception)
                    emitEvent(msg, ZibeSnackType.ERROR, stopLoading = true)
                    updateUI(null)
                } // Si es OK, el AuthStateListener llama a updateUI(user)
            }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    val msg = getAuthErrorMessage(task.exception)
                    emitEvent(msg, ZibeSnackType.ERROR, stopLoading = true)
                    updateUI(null)
                }
                // Si es OK, AuthStateListener llama a updateUI(user)
            }
    }

    private fun doEmailLogin(email: String, password: String) {

        lifecycleScope.launch {

            val isValid = validateInputs(email, password)
            if (!isValid) return@launch
            _isLoading.value = true

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this@AuthActivity) { task ->
                    if (!task.isSuccessful) {
                        val msg = getAuthErrorMessage(task.exception)
                        emitEvent(msg, ZibeSnackType.ERROR, stopLoading = true)
                        updateUI(null)
                    } // Si es OK, AuthStateListener llama a updateUI(user)
                }
        }
    }

    private fun resetPassword(email: String) {

        if (email.isBlank()) {
            emitEvent(
                "Por favor, ingresá tu email para reestablecer la contraseña",
                ZibeSnackType.WARNING
            )
            return
        }

        _isLoading.value = true

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) {
                    "Instrucciones enviadas a $email"
                } else {
                    "No pudimos enviar el correo a $email. Verificá que esté correcto."
                }

                emitEvent(
                    msg,
                    if (task.isSuccessful) ZibeSnackType.SUCCESS else ZibeSnackType.ERROR,
                    stopLoading = true
                )
            }
    }


    private fun doNotDelete() {
        val prefs = getSharedPreferences("AuthPreferences", MODE_PRIVATE)
        prefs.edit { putBoolean("deleteUser", false) }
        emitEvent("La cuenta se mantendrá activa", ZibeSnackType.INFO, stopLoading = true)
    }

    private fun emitEvent(
        message: String,
        type: ZibeSnackType,
        stopLoading: Boolean = true
    ) {
        if (stopLoading) {
            _isLoading.value = false
        }
        lifecycleScope.launch {
            authEvents.emit(AuthEvent.ShowSnackbar(message, type))
        }
    }

    fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, SplashActivity::class.java)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            )
            startActivity(intent)
            finish()
        } else {
            auth.signOut()
            loginManager.logOut()
        }
    }

    private suspend fun validateInputs(
        email: String,
        password: String
    ): Boolean {

        fun warn(msg: String): Boolean {
            emitEvent(msg, ZibeSnackType.WARNING)
            return false
        }

        if (email.isBlank()) return warn("Por favor, ingresá tu email")
        if (password.isBlank()) return warn("Por favor, ingresá una contraseña")

        return true
    }

    private fun getAuthErrorMessage(e: Exception?): String {
        if (e == null) {
            return "Ocurrió un error inesperado. Intentá nuevamente."
        }

        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> {
                "Email o contraseña incorrectos."
            }
            is FirebaseAuthInvalidUserException -> {
                "La cuenta no existe o fue deshabilitada."
            }
            is FirebaseAuthUserCollisionException -> {
                "Ya existe una cuenta registrada con este email."
            }
            else -> {
                // Mensaje genérico pero con algo de info técnica
                "Ocurrió un error (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Intentá nuevamente."}"
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mAuthListener?.let { auth.addAuthStateListener(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        mAuthListener?.let { auth.removeAuthStateListener(it) }
    }

}
