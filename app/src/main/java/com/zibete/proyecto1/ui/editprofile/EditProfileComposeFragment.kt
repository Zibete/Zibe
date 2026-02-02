package com.zibete.proyecto1.ui.editprofile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditProfileComposeFragment : Fragment() {

    val editProfileViewModel: EditProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ZibeTheme {
                    EditProfileRoute(
                        editProfileViewModel = editProfileViewModel,
                        onNavigateBack = {
                            (activity as? EditProfileExitHandler)?.onExitEditProfile()
                                ?: findNavController().popBackStack()
                        },
                        onOpenSettings = {
                            findNavController().navigate(
                                com.zibete.proyecto1.R.id.settingsFragment
                            )
                        }
                    )
                }
            }
        }
    }
}

