package com.zibete.proyecto1.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.zibete.proyecto1.core.constants.Constants.EXTRA_DELETE_ACCOUNT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SNACK_TYPE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_UI_TEXT
import com.zibete.proyecto1.ui.base.BaseEdgeToEdgeActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : BaseEdgeToEdgeActivity() {

    override val enableComposeSnackHost: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZibeTheme {
                SettingsRoute(
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onNavigateToSplash = { uiText, snackType, deleteAccount, sessionConflict ->
                        startActivity(
                            Intent(applicationContext, SplashActivity::class.java).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra(EXTRA_UI_TEXT, uiText)
                                putExtra(EXTRA_SNACK_TYPE, snackType)
                                putExtra(EXTRA_DELETE_ACCOUNT, deleteAccount)
                                putExtra(EXTRA_SESSION_CONFLICT, sessionConflict)
                            }
                        )
                        finish()
                    }
                )
            }
        }
    }
}
