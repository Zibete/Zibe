package com.zibete.proyecto1.ui.chatlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
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
import com.zibete.proyecto1.ui.search.SearchHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatListFragment : BaseChatSessionFragment(), SearchHandler {

    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var profileRepositoryProvider: ProfileRepositoryProvider

    private val chatListViewModel: ChatListViewModel by viewModels()

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterChatList: AdapterChatList
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)

        setupOptionMenu()
        setupRecycler()
        setupInitialUi()
        setupAdapterObserver()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeChatSessionEvents(chatListViewModel.events)

        collectUiState()

        chatListViewModel.loadChatList()
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatListViewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect

                    b.progressbar2.isVisible = state.isLoading

                    if (state.showOnboarding) {
                        showOnBoarding()
                    } else {
                        showChatList()
                    }

                    adapterChatList.submitList(state.filteredChats)
                }
            }
        }
    }

    // ---------- UI inicial ----------

    private fun setupInitialUi() = with(binding) {
        binding.rv.isVisible = true
        linearOnBoardingChatList.isVisible = false
        progressbar2.isVisible = true

        lottieChatLeft.cancelAnimation()
        lottieChatRight.cancelAnimation()
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

    private fun openChat(chat: Conversation) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(EXTRA_CHAT_ID, chat.otherId)
            putExtra(EXTRA_CHAT_NODE, NODE_DM)
        }
        startActivity(intent)
    }

    // ---------- Empty state / onboarding ----------

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

    // ---------- Observer para scroll y progress ----------

    private fun setupAdapterObserver() {
        adapterChatList.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                setScrollbar()
                _binding?.progressbar2?.isVisible = false
            }
        })
    }

    private fun setScrollbar() {
        val b = _binding ?: return
        if (::adapterChatList.isInitialized && adapterChatList.itemCount > 0) {
            b.rv.scrollToPosition(adapterChatList.itemCount - 1)
        }
    }

    private fun setupOptionMenu() {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = Unit
                override fun onPrepareMenu(menu: Menu) {
                    menu.findItem(R.id.action_settings)?.isVisible = true
                    menu.findItem(R.id.action_search)?.isVisible = true
                    menu.findItem(R.id.action_favorites)?.isVisible = true
                    menu.findItem(R.id.action_unblock_users)?.isVisible = true
                }
                override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.groupId != FRAGMENT_ID_CHATLIST) return false

        val chat = adapterChatList.currentList.getOrNull(item.order) ?: return false

        when (item.itemId) {
            1 -> chatListViewModel.onMarkAsReadChatListClicked(chat.otherId, NODE_DM)
            2 -> chatListViewModel.onToggleNotificationsClicked(chat.otherId, chat.otherName, NODE_DM)
            3 -> chatListViewModel.onBlockClicked(chat.otherId, chat.otherName, NODE_DM)
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
