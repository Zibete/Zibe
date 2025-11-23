package com.zibete.proyecto1.ui.signup

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.ZibeTheme
import com.zibete.proyecto1.utils.Constants
import kotlinx.coroutines.launch

class SignUpActivity : ComponentActivity() {

    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeEvents()

        setContent {
            ZibeTheme {

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                SignUpScreen(
                    onBack = { finish() },
                    onRegister = { email, pass, name, birthday, desc ->
                        val defaultPhotoUrl = getString(R.string.URL_PHOTO_DEF)
                        viewModel.onRegister(
                            email = email,
                            password = pass,
                            name = name,
                            birthday = birthday,
                            desc = desc,
                            defaultPhotoUrl = defaultPhotoUrl
                        )
                    },
                    signUpEvents = viewModel.events,
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

                        is SignUpUiEvent.ShowSnackbar -> {
                            // El snackbar lo maneja directamente SignUpScreen (Compose).
                            // No hacemos nada acá para no duplicar.
                        }

                        SignUpUiEvent.RequestLocationPermission -> {
                            ActivityCompat.requestPermissions(
                                this@SignUpActivity,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                Constants.REQUEST_LOCATION
                            )
                        }
                    }
                }
            }
        }
    }
}
