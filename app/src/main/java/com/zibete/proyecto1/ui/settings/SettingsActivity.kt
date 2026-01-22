package com.zibete.proyecto1.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.ui.base.BaseEdgeToEdgeActivity
import com.zibete.proyecto1.ui.report.ReportActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BaseEdgeToEdgeActivity() {

    @Inject lateinit var appNavigator: AppNavigator
    @Inject lateinit var snackBarManager: SnackBarManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZibeTheme {
                SettingsRoute(
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onOpenSendFeedback = {
                        startActivity(Intent(this@SettingsActivity, ReportActivity::class.java))
                    },
                    onNavigateToSplash = {
                        startActivity(
                            Intent(applicationContext, SplashActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                        finish()
                    },
                    appNavigator = appNavigator,
                    snackBarManager = snackBarManager
                )
            }
        }
    }
}
