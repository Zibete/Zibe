package com.zibete.proyecto1.ui.splash

import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
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
import com.google.android.gms.common.api.ApiException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zibete.proyecto1.MainActivity
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.OnboardingPage
import com.zibete.proyecto1.ui.auth.AuthScreen
import com.zibete.proyecto1.ui.auth.AuthViewModel
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.components.ZibeSnackbarHost
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.constants.DIALOG_CONTINUE
import com.zibete.proyecto1.ui.constants.TOKEN_DIALOG_MESSAGE
import com.zibete.proyecto1.ui.constants.TOKEN_DIALOG_TITLE
import com.zibete.proyecto1.ui.custompermission.CustomPermissionScreen
import com.zibete.proyecto1.ui.onboarding.OnboardingScreen
import com.zibete.proyecto1.ui.signup.SignUpScreen
import com.zibete.proyecto1.ui.signup.SignUpViewModel
import com.zibete.proyecto1.ui.theme.ZibeTheme
import com.zibete.proyecto1.utils.FirebaseRefs.auth
import kotlinx.coroutines.launch

@Suppress("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val splashVM: SplashViewModel by viewModels()
    private val authVM: AuthViewModel by viewModels()

    private var tokenDialogState by mutableStateOf<TokenDialogState?>(null)
    private var noInternetDialog by mutableStateOf(false)

    private data class TokenDialogState(
        val mail: String,
        val flag: Int
    )

    // ===============================
    // SharedPrefs
    // ===============================
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("ZibeAppPrefs", MODE_PRIVATE)
    }

    // ===============================
    // Google & Facebook
    // ===============================

    private lateinit var callbackManager: CallbackManager
    private lateinit var loginManager: LoginManager
    private lateinit var facebookLauncher: ActivityResultLauncher<Collection<String>>
    private var googleSignInClient: GoogleSignInClient? = null
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGoogleResult(result, authVM)
        }
    // ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar Google/Facebook
        setupGoogleSignIn()
        setupFacebookSignIn(authVM)

        // Inicializar prefs en el VM
        splashVM.initPrefs(prefs)

        setContent {
            ZibeTheme {

                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                Box(modifier = Modifier.fillMaxSize()) {

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {

                        // ======================================
                        // SPLASH
                        // ======================================
                        composable("splash") {
                            SplashScreen()
                            // Cada vez que navegás a "splash", corre el flujo del ViewModel
                            LaunchedEffect(Unit) {
                                splashVM.start(this@SplashActivity)
                            }
                        }

                        // ======================================
                        // ONBOARDING
                        // ======================================
                        composable("onboarding") {

                            val pages = listOf(
                                OnboardingPage(
                                    animationRes = R.raw.chat_right,
                                    title = "Chatea",
                                    description = "Chatea con familiares y amigos en tiempo real."
                                ),
                                OnboardingPage(
                                    animationRes = R.raw.lf30_editor_miibzys8,
                                    title = "Descubre",
                                    description = "Encuentra personas cercanas y hace nuevos contactos."
                                ),
                                OnboardingPage(
                                    animationRes = R.raw.onboarding_persons,
                                    title = "Socializa",
                                    description = "Crea salas o unite a salas de chat."
                                )
                            )

                            OnboardingScreen(
                                pages = pages,
                                onFinished = {
                                    prefs.edit { putBoolean("onBoarding", true) }
                                    navController.navigate("auth") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ======================================
                        // AUTH
                        // ======================================
                        composable("auth") {

                            val uiState by authVM.uiState.collectAsStateWithLifecycle()

                            val deleteUser = prefs.getBoolean("deleteUser", false)
                            val deleteFirebaseAccount = prefs.getBoolean("deleteFirebaseAccount", false)
                            authVM.initFromPrefs(deleteUser, deleteFirebaseAccount)

                            AuthScreen(
                                deleteUser = uiState.deleteUser,

                                onLogin = { email, password ->
                                    authVM.onEmailLogin(email, password)
                                },

                                onNavigateToSignUp = {
                                    navController.navigate("signup")
                                },

                                onResetPassword = { email ->
                                    authVM.onResetPassword(email)
                                },

                                onGoogleClick = {
                                    googleSignInClient?.signOut()
                                        ?.addOnCompleteListener {
                                            val intent = googleSignInClient?.signInIntent
                                            if (intent != null) {
                                                googleSignInLauncher.launch(intent)
                                            } else {
                                                authVM.showMessage(
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
                                    authVM.onDoNotDeleteClicked()
                                },

                                isLoading = uiState.isLoading,
                                authEvents = authVM.events,

                                onNavigateToSplash = {
                                    navController.navigate("splash") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },

                                onClearDeletePrefs = {
                                    prefs.edit {
                                        putBoolean("deleteUser", false)
                                        putBoolean("deleteFirebaseAccount", false)
                                    }
                                }
                            )
                        }

                        // ======================================
                        // SIGN UP
                        // ======================================
                        composable("signup") {

                            val vm: SignUpViewModel = viewModel()
                            val uiState by vm.uiState.collectAsStateWithLifecycle()

                            SignUpScreen(
                                onBack = { navController.popBackStack() },

                                onRegister = { email, pass, name, birthday, desc ->
                                    val defaultPhotoUrl = getString(R.string.URL_PHOTO_DEF)
                                    vm.onRegister(
                                        email = email,
                                        password = pass,
                                        name = name,
                                        birthday = birthday,
                                        desc = desc,
                                        defaultPhotoUrl = defaultPhotoUrl
                                    )
                                },

                                signUpEvents = vm.events,
                                isLoading = uiState.isLoading,

                                onNavigateToPermission = {
                                    navController.navigate("permission")
                                }
                            )
                        }

                        // ======================================
                        // CUSTOM PERMISSION
                        // ======================================
                        composable("permission") {

                            CustomPermissionScreen(
                                onPermissionGranted = {
                                    navController.navigate("splash") {
                                        popUpTo("permission") { inclusive = true }
                                    }
                                },

                                onForceLogout = {
                                    auth.signOut()
                                    LoginManager.getInstance().logOut()
                                    navController.navigate("auth") {
                                        popUpTo("permission") { inclusive = true }
                                    }
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
                    val dialogState = tokenDialogState
                    if (dialogState != null) {
                        ZibeDialog(
                            title = TOKEN_DIALOG_TITLE,
                            textContent = {
                                Text(
                                    text = TOKEN_DIALOG_MESSAGE.format(dialogState.mail)
                                )
                            },
                            confirmText = DIALOG_CONTINUE,
                            onConfirm = {
                                lifecycleScope.launch {
                                    splashVM.onTokenDialogConfirmed(dialogState.flag)
                                    tokenDialogState = null
                                }
                            },
                            dismissText = DIALOG_CANCEL,
                            onDismiss = {
                                lifecycleScope.launch {
                                    splashVM.onTokenDialogCancelled(dialogState.flag)
                                    tokenDialogState = null
                                }
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
                                splashVM.start(this@SplashActivity, isRetry = true)
                            },
                            dismissText = "Salir",
                            onDismiss = {
                                noInternetDialog = false
                                finish()
                            }
                        )
                    }
                }

                // ====== EVENTOS DEL SPLASH VIEWMODEL ======
                LaunchedEffect(Unit) {
                    splashVM.events.collect { event ->
                        when (event) {

                            is SplashUiEvent.ShowSnackbar ->
                                snackbarHostState.showZibeMessage(event.type, event.message)

                            is SplashUiEvent.ShowNoInternetDialog ->
                                noInternetDialog = true

                            is SplashUiEvent.ShowTokenDialog ->
                                tokenDialogState = TokenDialogState(event.mail, event.flag)

                            SplashUiEvent.NavigateOnBoarding ->
                                navController.navigate("onboarding") {
                                    popUpTo("splash") { inclusive = true }
                                }

                            SplashUiEvent.NavigateAuth ->
                                navController.navigate("auth") {
                                    popUpTo("splash") { inclusive = true }
                                }

                            SplashUiEvent.RequestLocationPermission ->
                                navController.navigate("permission")

                            SplashUiEvent.NavigateEditProfile -> {
                                startActivity(
                                    Intent(this@SplashActivity, MainActivity::class.java)
                                        .apply { putExtra("flagIntent", 0) }
                                )
                                finish()
                            }

                            SplashUiEvent.NavigateMain -> {
                                startActivity(
                                    Intent(this@SplashActivity, MainActivity::class.java)
                                        .apply { putExtra("flagIntent", 1) }
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
                vm.showMessage("Error de Google: ${e.statusCode}", ZibeSnackType.ERROR)
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
                vm.showMessage("Error con Facebook: ${error.localizedMessage}", ZibeSnackType.ERROR)
            }
        })
    }
}
