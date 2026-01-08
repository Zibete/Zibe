package com.zibete.proyecto1.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.OnboardingPage
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.ONBOARDING_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.PERMISSION_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.SIGNUP_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.SPLASH_SCREEN
import com.zibete.proyecto1.core.constants.DIALOG_EXIT
import com.zibete.proyecto1.core.constants.SESSION_CONFLICT_KEEP_HERE
import com.zibete.proyecto1.core.constants.SESSION_CONFLICT_LOGOUT
import com.zibete.proyecto1.core.constants.SESSION_CONFLICT_MESSAGE
import com.zibete.proyecto1.core.constants.SESSION_CONFLICT_TITLE
import com.zibete.proyecto1.core.di.SnackBarEntryPoint
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.ui.auth.AuthScreen
import com.zibete.proyecto1.ui.auth.AuthViewModel
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeSnackHost
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.custompermission.CustomPermissionScreen
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.onboarding.OnboardingScreen
import com.zibete.proyecto1.ui.signup.SignUpScreen
import com.zibete.proyecto1.ui.signup.SignUpViewModel
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
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

    // ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        splashViewModel.handleIntentExtras(
            intent.getBooleanExtra(EXTRA_SESSION_CONFLICT, false))

        // Configurar Facebook
        setupFacebookSignIn(authViewModel)

        setContent {
            ZibeTheme {

                val navController = rememberNavController()
                val snackHostState = remember { SnackbarHostState() }
                var showSessionConflictDialog by remember { mutableStateOf(false) }
                var noInternetDialog by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {

                    NavHost(
                        navController = navController,
                        startDestination = SPLASH_SCREEN
                    ) {
                        // ======================================
                        composable(SPLASH_SCREEN) {
                            SplashScreen()
                            LaunchedEffect(Unit) {
                                splashViewModel.start(this@SplashActivity)
                            }
                        }
                        // ======================================
                        composable(ONBOARDING_SCREEN) {
                            OnboardingScreen(
                                onFinished = {
                                    navController.navigate(AUTH_SCREEN) {
                                        popUpTo(ONBOARDING_SCREEN) { inclusive = true }
                                    }
                                }
                            )
                        }
                        // ======================================
                        composable(AUTH_SCREEN) {

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
                                    navController.navigate(SIGNUP_SCREEN)
                                },

                                onResetPassword = { email ->
                                    authViewModel.onResetPassword(email)
                                },

                                onGoogleClick = {
                                    authViewModel.onGoogleClick(this@SplashActivity)
                                },

                                onFacebookClick = {
                                    facebookLauncher.launch(listOf("public_profile", "email"))
                                },

                                onDoNotDelete = {
                                    authViewModel.onDoNotDeleteAccountClicked()
                                },

                                onDeleteAccount = {
                                    authViewModel.onDeleteAccountClicked()
                                },

                                isLoading = uiState.isLoading,
                                authEvents = authViewModel.events,

                                onNavigateToSplash = {
                                    navController.navigate(SPLASH_SCREEN) {
                                        popUpTo(AUTH_SCREEN) { inclusive = true }
                                    }
                                },
                            )
                        }
                        // ======================================
                        composable(SIGNUP_SCREEN) {

                            val signUpViewModel: SignUpViewModel = hiltViewModel()
                            val uiState by signUpViewModel.uiState.collectAsState()

                            SignUpScreen(
                                onBack = { navController.popBackStack() },

                                onRegister = { email, pass, name, birthDate, description ->
                                    signUpViewModel.onRegister(
                                        email = email,
                                        password = pass,
                                        name = name,
                                        birthDate = birthDate,
                                        description = description
                                    )
                                },

                                signUpEvents = signUpViewModel.events,
                                isLoading = uiState.isLoading,

                                onNavigateToSplash = {
                                    navController.navigate(SPLASH_SCREEN)
                                }
                            )
                        }
                        // ======================================
                        composable(PERMISSION_SCREEN) {

                            CustomPermissionScreen(
                                onPermissionGranted = {
                                    navController.navigate(SPLASH_SCREEN) {
                                        popUpTo(PERMISSION_SCREEN) { inclusive = true }
                                    }
                                },

                                onPermissionDenied = {
                                    splashViewModel.onLogoutRequested()
                                }
                            )
                        }
                    }

                    // SNACKBAR HOST
                    ZibeSnackHost(
                        hostState = snackHostState,
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




                val context = LocalContext.current
                val snackBarManager: SnackBarManager = remember {
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        SnackBarEntryPoint::class.java
                    ).snackBarManager()
                }

                LaunchedEffect(Unit) {
                    snackBarManager.events.collect { e ->
                        snackHostState.showZibeMessage(e.type, e.message)
                    }
                }

                // ====== EVENTOS DEL SPLASH VIEWMODEL ======
                LaunchedEffect(Unit) {
                    splashViewModel.events.collect { event ->
                        when (event) {

                            is SplashUiEvent.ShowSnack ->
                                snackHostState.showZibeMessage(event.type, event.message)

                            is SplashUiEvent.ShowNoInternetDialog ->
                                noInternetDialog = true

                            is SplashUiEvent.ShowSessionConflictDialog ->
                                showSessionConflictDialog = true

                            is SplashUiEvent.NavigateOnBoarding ->
                                navController.navigate(ONBOARDING_SCREEN) {
                                    popUpTo(SPLASH_SCREEN) { inclusive = true }
                                }

                            is SplashUiEvent.NavigateAuth ->
                                navController.navigate(AUTH_SCREEN) {
                                    popUpTo(SPLASH_SCREEN) { inclusive = true }
                                }

                            is SplashUiEvent.NavigatePermission ->
                                navController.navigate(PERMISSION_SCREEN)

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
