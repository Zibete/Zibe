package com.zibete.proyecto1.ui.splash

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
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
import com.zibete.proyecto1.core.constants.Constants.EXTRA_DELETE_ACCOUNT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_UI_TEXT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SNACK_TYPE
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.ONBOARDING_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.PERMISSION_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.SIGNUP_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.SPLASH_SCREEN
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.getAuthErrorMessage
import com.zibete.proyecto1.ui.auth.AuthScreen
import com.zibete.proyecto1.ui.auth.AuthViewModel
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeSnackbar
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.custompermission.CustomPermissionScreen
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.onboarding.OnboardingScreen
import com.zibete.proyecto1.ui.signup.SignUpScreen
import com.zibete.proyecto1.ui.signup.SignUpViewModel
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
@Suppress("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    @Inject
    lateinit var appNavigator: AppNavigator
    @Inject
    lateinit var snackBarManager: SnackBarManager
    private val splashViewModel: SplashViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    // ===============================
    // Google & Facebook
    // ===============================
    private lateinit var callbackManager: CallbackManager
    private lateinit var loginManager: LoginManager
    private lateinit var facebookLauncher: ActivityResultLauncher<Collection<String>>

    // ===============================

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deleteAccount = intent.getBooleanExtra(EXTRA_DELETE_ACCOUNT, false)

        splashViewModel.handleIntentExtras(
            uiText = intent.getParcelableExtra(EXTRA_UI_TEXT, UiText::class.java),
            snackType = intent.getParcelableExtra(EXTRA_SNACK_TYPE, ZibeSnackType::class.java),
            hasSessionConflict = intent.getBooleanExtra(EXTRA_SESSION_CONFLICT, false),
            deleteAccount = deleteAccount
        )

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
                                authViewModel.initAfterDelete(deleteAccount)
                            }

                            AuthScreen(
                                state = uiState,
                                onLogin = { email, password ->
                                    authViewModel.onEmailLogin(email, password)
                                },
                                onEmailInputChanged = { authViewModel.onEmailInputChanged(it) },
                                onResetEmailInputChanged = { authViewModel.onResetEmailInputChanged(it) },
                                onPasswordInputChanged = { authViewModel.onPasswordInputChanged(it) },
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
                                authEvents = authViewModel.events,
                                onNavigateToSplash = { uiText, snackType ->
                                    splashViewModel.handleIntentExtras(
                                        uiText = uiText,
                                        snackType = snackType,
                                        hasSessionConflict = false,
                                        deleteAccount = false
                                    )
                                    navController.navigate(SPLASH_SCREEN) {
                                        popUpTo(AUTH_SCREEN) { inclusive = true }
                                    }
                                },
                                appNavigator = appNavigator
                            )
                        }
                        // ======================================
                        composable(SIGNUP_SCREEN) {

                            val signUpViewModel: SignUpViewModel = hiltViewModel()
                            val uiState by signUpViewModel.uiState.collectAsState()

                            SignUpScreen(
                                state = uiState,
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
                                onEmailInputChanged = { signUpViewModel.onEmailInputChanged(it) },
                                onPasswordInputChanged = { signUpViewModel.onPasswordInputChanged(it) },
                                onNavigateToSplash = {
                                    navController.navigate(SPLASH_SCREEN)
                                },
                                appNavigator = appNavigator
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

                    // TOKEN DIALOG
                    val coroutineScope = rememberCoroutineScope()

                    if (showSessionConflictDialog) {
                        ZibeDialog(
                            title = getString(R.string.attention_title),
                            content = { Text(getString(R.string.session_conflict_message)) },
                            confirmText = getString(R.string.session_conflict_keep_here),
                            cancelText = getString(R.string.logout),
                            onConfirm = {
                                coroutineScope.launch { splashViewModel.onSessionConflictConfirmed() }
                                showSessionConflictDialog = false
                            },
                            onCancel = {
                                coroutineScope.launch { splashViewModel.onSessionConflictCancelled() }
                                showSessionConflictDialog = false
                            }
                        )
                    }

                    // NO INTERNET DIALOG
                    if (noInternetDialog) {
                        ZibeDialog(
                            title = getString(R.string.error_no_connection_title),
                            content = { Text(getString(R.string.error_no_connection_message)) },
                            confirmText = getString(R.string.action_retry),
                            cancelText = getString(R.string.action_exit),
                            onConfirm = {
                                noInternetDialog = false
                                coroutineScope.launch {
                                    splashViewModel.start(
                                        this@SplashActivity,
                                        isRetry = true
                                    )
                                }
                            },
                            onCancel = {
                                noInternetDialog = false
                                finish()
                            }
                        )
                    }

                    // SNACKBAR HOST
                    ZibeSnackbar(
                        hostState = snackHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                // ====== EVENTOS DEL SNACKBARMANAGER ======
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    snackBarManager.events.collectLatest { event ->
                        snackHostState.showZibeMessage(
                            message = event.uiText.asString(context),
                            type = event.type
                        )
                    }
                }

                // ====== EVENTOS DEL SPLASH VIEWMODEL ======
                LaunchedEffect(Unit) {
                    splashViewModel.events.collect { event ->
                        when (event) {

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
                                val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                                    putExtra(EXTRA_UI_TEXT, event.uiText)
                                    putExtra(EXTRA_SNACK_TYPE, event.snackType)
                                }
                                startActivity(intent)
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
    private fun setupFacebookSignIn(authViewModel: AuthViewModel) {
        callbackManager = CallbackManager.Factory.create()
        loginManager = LoginManager.getInstance()

        facebookLauncher = registerForActivityResult(
            loginManager.createLogInActivityResultContract(callbackManager, null)
        ) {}

        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                authViewModel.onFacebookAccessToken(result.accessToken)
            }

            override fun onCancel() {
                authViewModel.showSnack(
                    UiText.StringRes(R.string.signup_facebook_cancelled),
                    ZibeSnackType.INFO
                )
            }

            override fun onError(error: FacebookException) {
                authViewModel.showSnack(getAuthErrorMessage(error), ZibeSnackType.ERROR)
            }

        })
    }
}
