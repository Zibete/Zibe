package com.zibete.proyecto1.ui.splash

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zibete.proyecto1.CustomPermission
import com.zibete.proyecto1.MainActivity
import com.zibete.proyecto1.ui.auth.AuthActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Iniciar la lógica del Splash
        viewModel.start(this)

        // 2) OBSERVAR eventos del ViewModel
        observeEvents()

        // 3) Dibujar la UI
        setContent {
            ZibeTheme {
                val uiState by viewModel.uiState.collectAsState()
                SplashScreen(
                    isLoading = uiState.isLoading
                )
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
                            startActivity(Intent(this@SplashActivity, MainActivity::class.java).apply {
                                putExtra("flagIntent", 0)
                            })
                            finish()
                        }

                        is SplashUiEvent.NavigateMain -> {
                            startActivity(Intent(this@SplashActivity, MainActivity::class.java).apply {
                                putExtra("flagIntent", 1)
                            })
                            finish()
                        }

                        is SplashUiEvent.ShowNoInternet -> {
                            Toast.makeText(this@SplashActivity, "Sin internet", Toast.LENGTH_LONG).show()
                        }

                        is SplashUiEvent.ShowTokenDialog -> {
                            showTokenDialog(event.mail, event.flag)
                        }

                        is SplashUiEvent.RequestLocationPermission -> {
                            startActivity(Intent(this@SplashActivity, CustomPermission::class.java))
                            finish()
                        }

                    }
                }
            }
        }
    }

    private fun showTokenDialog(mail: String, flag: Int) {
        AlertDialog.Builder(this)
            .setTitle("Un momento…")
            .setMessage("Ya hay una cuenta asociada a este dispositivo. Si continúa, se desvinculará a $mail. ¿Desea continuar?")
            .setPositiveButton("Continuar") { _, _ ->
                // emitimos evento interno al ViewModel
                // paso 8 lo implementamos luego
            }
            .setNegativeButton("Cancelar") { _, _ ->
                // emitimos evento interno al ViewModel
            }
            .show()
    }
}

