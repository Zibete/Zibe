package com.zibete.proyecto1.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.groups.host.ROOM_V2_ID_ARG
import com.zibete.proyecto1.ui.main.MainUiEvent
import com.zibete.proyecto1.ui.main.MainViewModel
import com.zibete.proyecto1.ui.search.SearchHandler
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupsFragment : BaseChatSessionFragment(), SearchHandler {
    private val roomsViewModel: RoomsV2ViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ZibeTheme {
                RoomsV2Route(viewModel = roomsViewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomsViewModel.events.collect { event ->
                    when (event) {
                        is RoomsV2UiEvent.ShowSnack -> mainViewModel.emit(
                            MainUiEvent.ShowSnack(event.uiText, event.snackType)
                        )

                        is RoomsV2UiEvent.NavigateToRoom -> {
                            findNavController().navigate(
                                R.id.nav_room_v2_host,
                                bundleOf(ROOM_V2_ID_ARG to event.roomId),
                                navOptions { launchSingleTop = true },
                            )
                        }
                    }
                }
            }
        }
        roomsViewModel.loadRooms()
    }

    override fun onSearchQueryChanged(query: String?) {
        roomsViewModel.onSearchQueryChanged(query.orEmpty())
    }
}
