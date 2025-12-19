package com.zibete.proyecto1.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.common.api.ApiException
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.OnboardingPage
import com.zibete.proyecto1.ui.auth.AuthScreen
import com.zibete.proyecto1.ui.auth.AuthViewModel
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.components.ZibeSnackbarHost
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.ui.constants.DIALOG_EXIT
import com.zibete.proyecto1.ui.constants.ONBOARDING_DESC_1
import com.zibete.proyecto1.ui.constants.ONBOARDING_DESC_2
import com.zibete.proyecto1.ui.constants.ONBOARDING_DESC_3
import com.zibete.proyecto1.ui.constants.ONBOARDING_TITLE_1
import com.zibete.proyecto1.ui.constants.ONBOARDING_TITLE_2
import com.zibete.proyecto1.ui.constants.ONBOARDING_TITLE_3
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_KEEP_HERE
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_LOGOUT
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_MESSAGE
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_TITLE
import com.zibete.proyecto1.ui.custompermission.CustomPermissionScreen
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.onboarding.OnboardingScreen
import com.zibete.proyecto1.ui.signup.SignUpScreen
import com.zibete.proyecto1.ui.signup.SignUpViewModel
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
@Suppress("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val splashViewModel: SplashViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    // ===============================
    // Google & Facebook
    // ===============================
    private lateinit var callbackManager: CallbackManager
    private lateinit var loginManager: LoginManager
    private lateinit var facebookLauncher: ActivityResultLauncher<Collection<String>>
    private var googleSignInClient: GoogleSignInClient? = null
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGoogleResult(result, authViewModel)
        }
    // ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        splashViewModel.handleIntentExtras(
            intent.getBooleanExtra(EXTRA_SESSION_CONFLICT, false))

        // Configurar Google/Facebook
        setupGoogleSignIn()
        setupFacebookSignIn(authViewModel)

        setContent {
            ZibeTheme {

                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                var showSessionConflictDialog by remember { mutableStateOf(false) }
                var noInternetDialog by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        // ======================================
                        composable("splash") {
                            SplashScreen()
                            LaunchedEffect(Unit) {
                                splashViewModel.start(this@SplashActivity)
                            }
                        }
                        // ======================================
                        composable("onboarding") {
                            val pages = listOf(
                                OnboardingPage(R.raw.onboarding1,ONBOARDING_TITLE_1,ONBOARDING_DESC_1),
                                OnboardingPage(R.raw.onboarding2,ONBOARDING_TITLE_2,ONBOARDING_DESC_2),
                                OnboardingPage(R.raw.onboarding3,ONBOARDING_TITLE_3,ONBOARDING_DESC_3)
                            )

                            OnboardingScreen(
                                pages = pages,
                                onFinished = {
                                    navController.navigate("auth") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        // ======================================
                        composable("auth") {

                            val uiState by authViewModel.uiState.collectAsState()

                            LaunchedEffect(Unit) {
                                authViewModel.initAfterDelete()
                            }

                            AuthScreen(
                                deleteUser = uiState.deleteUser,

                                onLogin = { email, password ->
                                    authViewModel.onEmailLogin(email, password)
                                },

                                onNavigateToSignUp = {
                                    navController.navigate("signup")
                                },

                                onResetPassword = { email ->
                                    authViewModel.onResetPassword(email)
                                },

                                onGoogleClick = {
                                    googleSignInClient?.signOut()
                                        ?.addOnCompleteListener {
                                            val intent = googleSignInClient?.signInIntent
                                            if (intent != null) {
                                                googleSignInLauncher.launch(intent)
                                            } else {
                                                authViewModel.showMessage(
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
                                    authViewModel.onDoNotDeleteClicked()
                                },

                                isLoading = uiState.isLoading,
                                authEvents = authViewModel.events,

                                onNavigateToSplash = {
                                    navController.navigate("splash") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },

                                onClearDeletePrefs = {
                                    userPreferencesRepository.deleteUser = false
                                    userPreferencesRepository.deleteFirebaseAccount = false
                                }
                            )
                        }
                        // ======================================
                        composable("signup") {

                            val signUpViewModel: SignUpViewModel = hiltViewModel()
                            val uiState by signUpViewModel.uiState.collectAsState()

                            SignUpScreen(
                                onBack = { navController.popBackStack() },

                                onRegister = { email, pass, name, birthday, description ->
                                    signUpViewModel.onRegister(
                                        email = email,
                                        password = pass,
                                        name = name,
                                        birthDate = birthday,
                                        description = description
                                    )
                                },

                                signUpEvents = signUpViewModel.events,
                                isLoading = uiState.isLoading,

                                onNavigateToPermission = {
                                    navController.navigate("permission")
                                }
                            )
                        }
                        // ======================================
                        composable("permission") {

                            CustomPermissionScreen(
                                onPermissionGranted = {
                                    navController.navigate("splash") {
                                        popUpTo("permission") { inclusive = true }
                                    }
                                },

                                onForceLogout = {
                                    splashViewModel.onLogoutRequested()
                                }
                            )
                        }
                    }

                    // SNACKBAR HOST
                    ZibeSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // TOKEN DIALOG

                    val coroutineScope = rememberCoroutineScope()

                    if (showSessionConflictDialog) {
                        ZibeDialog(
                            title = SESSION_CONFLICT_TITLE,
                            textContent = { Text(SESSION_CONFLICT_MESSAGE) },
                            confirmText = SESSION_CONFLICT_KEEP_HERE,
                            onConfirm = {
                                coroutineScope.launch { splashViewModel.onSessionConflictConfirmed() }
                                showSessionConflictDialog = false
                            },
                            dismissText = SESSION_CONFLICT_LOGOUT,
                            onDismiss = {
                                coroutineScope.launch { splashViewModel.onSessionConflictCancelled() }
                                showSessionConflictDialog = false
                            }
                        )
                    }

                    // NO INTERNET DIALOG
                    if (noInternetDialog) {
                        ZibeDialog(
                            title = "Sin conexión",
                            textContent = { Text("Revisá tu conexión e intentá nuevamente.") },
                            confirmText = "Reintentar",
                            onConfirm = {
                                noInternetDialog = false
                                coroutineScope.launch { splashViewModel.start(this@SplashActivity, isRetry = true) }
                            },
                            dismissText = DIALOG_EXIT,
                            onDismiss = {
                                noInternetDialog = false
                                finish()
                            }
                        )
                    }
                }

                // ====== EVENTOS DEL SPLASH VIEWMODEL ======
                LaunchedEffect(Unit) {
                    splashViewModel.events.collect { event ->
                        when (event) {

                            is SplashUiEvent.ShowSnackbar ->
                                snackbarHostState.showZibeMessage(event.type, event.message)

                            is SplashUiEvent.ShowNoInternetDialog ->
                                noInternetDialog = true

                            is SplashUiEvent.ShowSessionConflictDialog ->
                                showSessionConflictDialog = true

                            is SplashUiEvent.NavigateOnBoarding ->
                                navController.navigate("onboarding") {
                                    popUpTo("splash") { inclusive = true }
                                }

                            is SplashUiEvent.NavigateAuth ->
                                navController.navigate("auth") {
                                    popUpTo("splash") { inclusive = true }
                                }

                            is SplashUiEvent.Navigate -> {
                                this@SplashActivity.startActivity(event.intent)
                            }

                            is SplashUiEvent.RequestLocationPermission ->
                                navController.navigate("permission")

                            is SplashUiEvent.NavigateMain -> {
                                startActivity(
                                    Intent(this@SplashActivity, MainActivity::class.java)
                                )
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    // ======================================
    // GOOGLE
    // ======================================
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun handleGoogleResult(result: ActivityResult, vm: AuthViewModel) {
        if (result.resultCode == RESULT_OK && result.data != null) {
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)

                vm.onGoogleAccountReceived(account)

            } catch (e: ApiException) {
                vm.showMessage("ShowErrorDialog de Google: ${e.statusCode}", ZibeSnackType.ERROR)
            }
        } else {
            vm.showMessage("Inicio con Google cancelado", ZibeSnackType.INFO)
        }
    }

    // ======================================
    // FACEBOOK
    // ======================================
    private fun setupFacebookSignIn(vm: AuthViewModel) {
        callbackManager = CallbackManager.Factory.create()
        loginManager = LoginManager.getInstance()

        facebookLauncher = registerForActivityResult(
            loginManager.createLogInActivityResultContract(callbackManager, null)
        ) {}

        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                vm.onFacebookAccessToken(result.accessToken)
            }

            override fun onCancel() {
                vm.showMessage("Inicio con Facebook cancelado", ZibeSnackType.INFO)
            }

            override fun onError(error: FacebookException) {
                vm.showMessage("ShowErrorDialog con Facebook: ${error.localizedMessage}", ZibeSnackType.ERROR)
            }
        })
    }
}
