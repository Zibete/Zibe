package com.zibete.proyecto1.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.zibete.proyecto1.core.constants.Constants.EXTRA_DELETE_ACCOUNT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SNACK_TYPE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_UI_TEXT
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ZibeTheme {
                    SettingsRoute(
                        onBack = { findNavController().popBackStack() },
                        onNavigateToSplash = { uiText, snackType, deleteAccount, sessionConflict ->
                            startActivity(
                                Intent(requireContext(), SplashActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                    putExtra(EXTRA_UI_TEXT, uiText)
                                    putExtra(EXTRA_SNACK_TYPE, snackType)
                                    putExtra(EXTRA_DELETE_ACCOUNT, deleteAccount)
                                    putExtra(EXTRA_SESSION_CONFLICT, sessionConflict)
                                }
                            )
                            requireActivity().finish()
                        }
                    )
                }
            }
        }
    }
}
