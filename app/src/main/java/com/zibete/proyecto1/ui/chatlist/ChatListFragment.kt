package com.zibete.proyecto1.ui.chatlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.zibete.proyecto1.adapters.AdapterChatList
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.databinding.FragmentChatListBinding
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.FRAGMENT_ID_CHATLIST
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.ui.main.MainUiEvent
import com.zibete.proyecto1.ui.main.MainViewModel
import com.zibete.proyecto1.ui.search.SearchHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatListFragment : BaseChatSessionFragment(), SearchHandler {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var profileRepositoryProvider: ProfileRepositoryProvider

    private val chatListViewModel: ChatListViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterChatList: AdapterChatList
    private lateinit var layoutManager: LinearLayoutManager
    private var didAutoScroll = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)

        setupRecycler()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectUiState()
        collectEvents()
    }

    override fun onStart() {
        super.onStart()
        chatListViewModel.startObserving()
    }

    override fun onStop() {
        chatListViewModel.stopObserving()
        super.onStop()
    }

    private fun collectEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatListViewModel.events.collect { event ->
                    mainViewModel.emit(MainUiEvent.HandleChatSessionEvent(event))
                }
            }
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatListViewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun setupRecycler() {
        layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        adapterChatList = AdapterChatList(
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            userRepository = userRepository,
            profileRepositoryProvider = profileRepositoryProvider,
            onChatClicked = ::openChat
        )

        binding.rv.apply {
            layoutManager = this@ChatListFragment.layoutManager
            adapter = adapterChatList
            setHasFixedSize(true)
        }

        registerForContextMenu(binding.rv)
    }

    private fun render(state: ChatListUiState) {
        val b = _binding ?: return

        b.progressIndicator.isVisible = state.isLoading

        if (state.showOnboarding) showOnBoarding() else showChatList()

        adapterChatList.submitList(state.filteredChats)

        if (state.isLoading) {
            didAutoScroll = false
            return
        }

        if (state.filteredChats.isEmpty()) {
            didAutoScroll = false
            return
        }

        if (!didAutoScroll && !state.showOnboarding) {
            val lastIndex = state.filteredChats.lastIndex
            if (lastIndex >= 0) b.rv.scrollToPosition(lastIndex)
            didAutoScroll = true
        }
    }

    private fun openChat(chat: Conversation) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(EXTRA_CHAT_ID, chat.otherId)
            putExtra(EXTRA_CHAT_NODE, NODE_DM)
        }
        startActivity(intent)
    }

    private fun showOnBoarding() = with(binding) {
        binding.rv.isVisible = false
        linearOnBoardingChatList.isVisible = true

        lottieChatLeft.playAnimation()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)
            lottieChatRight.playAnimation()
        }
    }

    private fun showChatList() = with(binding) {
        binding.rv.isVisible = true
        linearOnBoardingChatList.isVisible = false

        lottieChatLeft.cancelAnimation()
        lottieChatRight.cancelAnimation()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.groupId != FRAGMENT_ID_CHATLIST) return false

        val chat = adapterChatList.currentList.getOrNull(item.order) ?: return false

        when (item.itemId) {
            1 -> chatListViewModel.onMarkAsReadChatListClicked(chat.otherId, NODE_DM)
            2 -> chatListViewModel.onToggleNotificationsClicked(
                chat.otherId,
                chat.otherName,
                NODE_DM
            )

            3 -> chatListViewModel.onConfirmToggleBlockAction(chat.otherId, chat.otherName)
            4 -> chatListViewModel.onHideClicked(chat.otherId, chat.otherName, NODE_DM)
            5 -> chatListViewModel.onDeleteClicked(chat.otherId, chat.otherName, NODE_DM)
        }
        return true
    }

    override fun onSearchQueryChanged(query: String?) {
        chatListViewModel.onSearchQueryChanged(query.orEmpty())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.lottieChatLeft.cancelAnimation()
        binding.lottieChatRight.cancelAnimation()
        _binding = null
    }
}
