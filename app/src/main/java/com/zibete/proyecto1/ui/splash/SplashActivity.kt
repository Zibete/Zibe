package com.zibete.proyecto1.ui.splash

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.MainActivity
import com.zibete.proyecto1.ui.auth.AuthActivity
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.theme.ZibeTheme
import com.zibete.proyecto1.CustomPermission
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import com.zibete.proyecto1.ui.constants.NO_INTERNET

class SplashActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    // Estado efímero para el diálogo de token
    private data class TokenDialogState(
        val mail: String,
        val flag: Int
    )

    private var tokenDialogState by mutableStateOf<TokenDialogState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Iniciar lógica del splash
        viewModel.start(this)

        observeEvents()

        setContent {
            ZibeTheme {

                // Pantalla de splash (logo + progress)
                SplashScreen()

                // Diálogo de token (si corresponde)
                val dialogState = tokenDialogState
                if (dialogState != null) {
                    ZibeDialog(
                        title = "Un momento…",
                        textContent = {
                            Text(
                                text = "Este dispositivo ya tiene una cuenta vinculada. " +
                                        "Si continuás, se desvinculará la cuenta asociada a ${dialogState.mail}. ¿Querés seguir?"
                            )
                        },
                        confirmText = "Continuar",
                        onConfirm = {
                            // Confirmar en ViewModel y cerrar diálogo
                            lifecycleScope.launch {
                                viewModel.onTokenDialogConfirmed(dialogState.flag)
                                tokenDialogState = null
                            }
                        },
                        dismissText = "Cancelar",
                        onDismiss = {
                            lifecycleScope.launch {
                                viewModel.onTokenDialogCancelled(dialogState.flag)
                                tokenDialogState = null
                            }
                        },
                        enabled = true
                    )
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is SplashUiEvent.NavigateAuth -> {
                            startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
                            finish()
                        }

                        is SplashUiEvent.NavigateEditProfile -> {
                            startActivity(
                                Intent(this@SplashActivity, MainActivity::class.java).apply {
                                    putExtra("flagIntent", 0)
                                }
                            )
                            finish()
                        }

                        is SplashUiEvent.NavigateMain -> {
                            startActivity(
                                Intent(this@SplashActivity, MainActivity::class.java).apply {
                                    putExtra("flagIntent", 1)
                                }
                            )
                            finish()
                        }

                        is SplashUiEvent.ShowNoInternet -> {
                            Toast.makeText(
                                this@SplashActivity,
                                NO_INTERNET,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        is SplashUiEvent.ShowTokenDialog -> {
                            tokenDialogState = TokenDialogState(
                                mail = event.mail,
                                flag = event.flag
                            )
                        }

                        is SplashUiEvent.RequestLocationPermission -> {
                            startActivity(
                                Intent(
                                    this@SplashActivity,
                                    CustomPermission::class.java
                                )
                            )
                            finish()
                        }
                    }
                }
            }
        }
    }
}
