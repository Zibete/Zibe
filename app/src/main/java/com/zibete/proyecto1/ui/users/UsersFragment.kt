package com.zibete.proyecto1.ui.users

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_START_INDEX
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_IDS
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.main.MainUiEvent
import com.zibete.proyecto1.ui.main.MainViewModel
import com.zibete.proyecto1.ui.profile.ProfileActivity
import com.zibete.proyecto1.ui.search.SearchHandler
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class UsersFragment : BaseChatSessionFragment(), SearchHandler, DiscoverToolbarHandler {
    private val usersViewModel: UsersViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    override val hasActiveFilters: Boolean
        get() = usersViewModel.uiState.value.hasActiveFilters

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ZibeTheme {
                DiscoverRoute(viewModel = usersViewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        usersViewModel.loadUsers()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usersViewModel.events.collect { event ->
                    when (event) {
                        is UsersUiEvent.NavigateToChat -> openChat(event.userId)
                        is UsersUiEvent.NavigateToProfile -> openProfile(
                            event.userIds,
                            event.startIndex
                        )
                        is UsersUiEvent.ShowSnack -> mainViewModel.emit(
                            MainUiEvent.ShowSnack(event.uiText, event.snackType)
                        )
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usersViewModel.uiState
                    .map { it.hasActiveFilters }
                    .distinctUntilChanged()
                    .collect { requireActivity().invalidateOptionsMenu() }
            }
        }
    }

    override fun onSearchQueryChanged(query: String?) {
        usersViewModel.onSearchQueryChanged(query.orEmpty())
    }

    override fun onFilterRequested() {
        usersViewModel.onFilterRequested()
    }

    private fun openChat(userId: String) {
        startActivity(
            Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra(EXTRA_CHAT_ID, userId)
                putExtra(EXTRA_CHAT_NODE, NODE_DM)
            }
        )
    }

    private fun openProfile(userIds: ArrayList<String>, startIndex: Int) {
        startActivity(
            Intent(requireContext(), ProfileActivity::class.java)
                .putStringArrayListExtra(EXTRA_USER_IDS, userIds)
                .putExtra(EXTRA_START_INDEX, startIndex)
        )
    }
}
