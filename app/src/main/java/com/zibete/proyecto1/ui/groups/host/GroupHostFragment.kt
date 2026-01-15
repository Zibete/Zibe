package com.zibete.proyecto1.ui.groups.host

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.zibete.proyecto1.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupHostFragment : Fragment() {

    private val groupHostViewModel: GroupHostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Back: si no estás en el chat del grupo, vuelve al tab del chat.
        // Si ya estás en el chat, deja que el back haga lo normal (según tu navegación en Main).
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (groupHostViewModel.tryHandleBack()) return
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                GroupHostRoute(groupHostViewModel = groupHostViewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupOptionMenu()
    }

    private fun setupOptionMenu() {
        val menuHost = requireActivity() as MenuHost

        menuHost.addMenuProvider(
            object : MenuProvider {

                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // No-op: el menú lo infla BaseEdgeToEdgeActivity
                }

                override fun onPrepareMenu(menu: Menu) {
                    menu.findItem(R.id.action_settings)?.isVisible = true
                    menu.findItem(R.id.action_search)?.isVisible = true
                    menu.findItem(R.id.action_exit_group)?.isVisible = true
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }
}
