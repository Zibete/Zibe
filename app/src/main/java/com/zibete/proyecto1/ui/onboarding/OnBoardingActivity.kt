package com.zibete.proyecto1.ui.onboarding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.OnboardingPage
import com.zibete.proyecto1.ui.theme.ZibeTheme

class OnBoardingActivity : ComponentActivity() {

    private val pages = listOf(
        OnboardingPage(
            animationRes = R.raw.chat_right,
            title = "Chatea",
            description = "Chatea con familiares y amigos, cuando quieras, en tiempo real!"
        ),
        OnboardingPage(
            animationRes = R.raw.lf30_editor_miibzys8,
            title = "Descubre",
            description = "Encuentra personas cercanas a tu ubicación. Haz nuevos amigos!"
        ),
        OnboardingPage(
            animationRes = R.raw.onboarding_persons,
            title = "Socializa",
            description = "Únete a las salas de chat existentes, o crea una a tu medida!"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ZibeTheme {
                OnboardingScreen(
                    pages = pages,
                    onFinished = {
                        finish()
                    }
                )
            }
        }
    }
}
