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
import androidx.recyclerview.widget.RecyclerView
import com.zibete.proyecto1.R
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
    private var scrollListener: RecyclerView.OnScrollListener? = null
    private val scrollTopThreshold = 3

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)

        setupRecycler()
        setupScrollTopFab()

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
            reverseLayout = false
            stackFromEnd = false
        }

        adapterChatList = AdapterChatList(
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            myUid = userRepository.myUid,
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

        if (state.showOnboarding) showOnBoarding(b) else showChatList(b)

        adapterChatList.submitList(state.filteredChats)
        updateScrollTopFab()
    }

    private fun openChat(chat: Conversation) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(EXTRA_CHAT_ID, chat.otherId)
            putExtra(EXTRA_CHAT_NODE, NODE_DM)
        }
        startActivity(intent)
    }

    private fun showOnBoarding(b: FragmentChatListBinding) {
        b.rv.isVisible = false
        b.linearOnBoardingChatList.isVisible = true

        b.lottieChatLeft.playAnimation()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)
            b.lottieChatRight.playAnimation()
        }
    }

    private fun showChatList(b: FragmentChatListBinding) {
        b.rv.isVisible = true
        b.linearOnBoardingChatList.isVisible = false

        b.lottieChatLeft.cancelAnimation()
        b.lottieChatRight.cancelAnimation()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.groupId != FRAGMENT_ID_CHATLIST) return false

        val otherId = adapterChatList.consumeContextMenuChatId() ?: return false
        val chat = adapterChatList.currentList.firstOrNull { it.otherId == otherId }

        val otherName =
            chat?.otherName.orEmpty().ifBlank { getString(R.string.deleted_profile_fallback) }

        when (item.itemId) {
            1 -> chatListViewModel.onMarkAsReadChatListClicked(otherId, NODE_DM)
            2 -> chatListViewModel.onToggleNotificationsClicked(otherId, otherName, NODE_DM)
            3 -> chatListViewModel.onConfirmToggleBlockAction(otherId, otherName)
            4 -> chatListViewModel.onHideClicked(otherId, otherName, NODE_DM)
            5 -> chatListViewModel.onDeleteClicked(otherId, otherName, NODE_DM)
        }
        return true
    }


    override fun onSearchQueryChanged(query: String?) {
        chatListViewModel.onSearchQueryChanged(query.orEmpty())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.let { b ->
            scrollListener?.let { b.rv.removeOnScrollListener(it) }
            b.lottieChatLeft.cancelAnimation()
            b.lottieChatRight.cancelAnimation()
        }
        scrollListener = null
        _binding = null
    }

    private fun setupScrollTopFab() {
        binding.fabScrollTop.setOnClickListener {
            binding.rv.smoothScrollToPosition(0)
        }

        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollTopFab()
            }
        }
        scrollListener?.let { binding.rv.addOnScrollListener(it) }
        updateScrollTopFab()
    }

    private fun updateScrollTopFab() {
        val b = _binding ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        b.fabScrollTop.isVisible = firstVisible > scrollTopThreshold
    }
}
