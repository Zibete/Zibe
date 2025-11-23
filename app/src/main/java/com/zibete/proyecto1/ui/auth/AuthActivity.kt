package com.zibete.proyecto1.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.zibete.proyecto1.ui.onboarding.OnBoardingActivity
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.signup.SignUpActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.launch
import androidx.core.content.edit

class AuthActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var prefs: SharedPreferences
    private lateinit var callbackManager: CallbackManager
    private lateinit var loginManager: LoginManager
    private lateinit var facebookLauncher: ActivityResultLauncher<Collection<String>>
    private var googleSignInClient: GoogleSignInClient? = null

    // Launcher de Google
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGoogleResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preferencias
        prefs = getSharedPreferences("AuthPreferences", MODE_PRIVATE)
        val editor = prefs.edit()
        val onBoarding = prefs.getBoolean("onBoarding", false)
        val deleteUser = prefs.getBoolean("deleteUser", false)
        val deleteFirebaseAccount = prefs.getBoolean("deleteFirebaseAccount", false)

        // OnBoarding una sola vez
        if (!onBoarding) {
            startActivity(Intent(this, OnBoardingActivity::class.java))
            editor.putBoolean("onBoarding", true).apply()
        }

        // Inicializar ViewModel con flags
        viewModel.initFromPrefs(deleteUser, deleteFirebaseAccount)

        // Google
        setupGoogleSignIn()
        // Facebook
        setupFacebookSignIn()
        // Observar eventos del ViewModel
        observeEvents()

        // UI Compose
        setContent {
            ZibeTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                AuthScreen(
                    deleteUser = uiState.deleteUser,
                    onLogin = { email, password ->
                        viewModel.onEmailLogin(email, password)
                    },
                    onNavigateToSignUp = {
                        startActivity(Intent(this, SignUpActivity::class.java))
                    },
                    onResetPassword = { email ->
                        viewModel.onResetPassword(email)
                    },
                    onGoogleClick = {
                        googleSignInClient?.signOut()
                            ?.addOnCompleteListener { _: Task<Void?>? ->
                                val intent = googleSignInClient?.signInIntent
                                if (intent != null) {
                                    googleSignInLauncher.launch(intent)
                                } else {
                                    viewModel.showMessage(
                                        "No se pudo abrir Google Sign-In",
                                        ZibeSnackType.ERROR
                                    )
                                }
                            }
                    },

                    onFacebookClick = {
                        facebookLauncher.launch(listOf("public_profile", "email"))
                    },
                    onDoNotDelete = {
                        viewModel.onDoNotDeleteClicked()
                    },
                    isLoading = uiState.isLoading,
                    authEvents = viewModel.events
                )
            }
        }
    }

    private fun setupFacebookSignIn() {
        callbackManager = CallbackManager.Factory.create()
        loginManager = LoginManager.getInstance()

        facebookLauncher = registerForActivityResult(
            loginManager.createLogInActivityResultContract(callbackManager, null)
        ) { /* el resultado lo maneja callbackManager */ }

        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                viewModel.onFacebookAccessToken(result.accessToken)
            }

            override fun onCancel() {
                viewModel.showMessage(
                    "Inicio con Facebook cancelado",
                    ZibeSnackType.INFO
                )
            }

            override fun onError(error: FacebookException) {
                viewModel.showMessage(
                    "Error con Facebook: ${error.localizedMessage}",
                    ZibeSnackType.ERROR
                )
            }
        })
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

                viewModel.onGoogleAccountReceived(account)

            } catch (e: ApiException) {
                viewModel.showMessage(
                    "Error de Google: ${e.statusCode}",
                    ZibeSnackType.ERROR
                )
            }
        } else {
            viewModel.showMessage(
                "Inicio cancelado",
                ZibeSnackType.INFO
            )
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is AuthUiEvent.ShowSnackbar -> {
                        // La UI (AuthScreen) ya muestra el snackbar. No hacemos nada acá.
                    }

                    is AuthUiEvent.NavigateToSplash -> {
                        val intent = Intent(this@AuthActivity, SplashActivity::class.java)
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                        startActivity(intent)
                        finish()
                    }

                    is AuthUiEvent.ClearDeletePrefs -> {
                        prefs.edit {
                            putBoolean("deleteUser", false)
                                .putBoolean("deleteFirebaseAccount", false)
                        }
                    }
                }
            }
        }
    }
}
