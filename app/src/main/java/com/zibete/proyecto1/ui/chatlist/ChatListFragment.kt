package com.zibete.proyecto1.ui.chatlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zibete.proyecto1.ChatListGroupsFragment
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterChatList
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.databinding.FragmentChatListBinding
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.constants.Constants.FRAGMENT_ID_CHATGROUPLIST
import com.zibete.proyecto1.ui.constants.Constants.FRAGMENT_ID_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATWITH
import com.zibete.proyecto1.ui.constants.Constants.NODE_UNKNOWN
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatListFragment : BaseChatSessionFragment(), SearchView.OnQueryTextListener {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var userSessionManager: UserSessionManager

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

        observeChatSessionEvents(chatListViewModel.events)

        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        setupRecycler()
        setupInitialUi()
        setupAdapterObserver()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // UI state
                launch {
                    chatListViewModel.uiState.collect { state ->
                        binding.progressbar2.isVisible = state.isLoading

                        if (state.showOnboarding) {
                            showOnBoarding()
                        } else {
                            showChatList()
                        }

                        adapterChatList.submitList(state.filteredChats)
                        setScrollbar()

                    }
                }
            }
        }
    }

    // ---------- UI inicial ----------

    private fun setupInitialUi() = with(binding) {
        rv.isVisible = true
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
            context = requireContext(),
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            userRepository = userRepository,
            userSessionManager = userSessionManager
        )

        binding.rv.apply {
            layoutManager = this@ChatListFragment.layoutManager
            adapter = adapterChatList
        }

        registerForContextMenu(binding.rv)
    }

    // ---------- Empty state / onboarding ----------

    private fun showOnBoarding() = with(binding) {
        rv.isVisible = false
        linearOnBoardingChatList.isVisible = true

        lottieChatLeft.playAnimation()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)
            lottieChatRight.playAnimation()
        }
    }

    private fun showChatList() = with(binding) {
        rv.isVisible = true
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

    // ---------- Context menu acciones ----------

    override fun onContextItemSelected(item: MenuItem): Boolean {
        // Chat individual
        if (item.groupId == FRAGMENT_ID_CHATLIST) {
            val wChat = adapterChatList.currentList[item.order]
            runItemSelected(item, NODE_CHATWITH, wChat.userId, wChat.userName)
        }
        // Chat unknown / grupos
        if (item.groupId == FRAGMENT_ID_CHATGROUPLIST) {
            val wChat = ChatListGroupsFragment.chatsGroupArrayList[item.order]
            runItemSelected(item, NODE_UNKNOWN, wChat.userId, wChat.userName)
        }

        return true
    }

    private fun runItemSelected(
        item: MenuItem,
        userId: String,
        userName: String,
        nodeType: String
    ) {
        when (item.itemId) {
            1 -> chatListViewModel.onMarkAsReadChatListClicked(userId, nodeType)
            2 -> chatListViewModel.onToggleNotificationsClicked(userId, userName, nodeType)
            3 -> chatListViewModel.onBlockClicked(userId, userName, nodeType) //No hay unblock
            4 -> chatListViewModel.onHideClicked(userId, userName, nodeType)
            5 -> chatListViewModel.onDeleteClicked(userId, userName, nodeType)
        }
    }


    // ---------- SearchView ----------

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val actionSearch = menu.findItem(R.id.action_search)
        val actionUnlock = menu.findItem(R.id.action_unblock)
        val actionExit = menu.findItem(R.id.action_exit_group)

        actionExit.isVisible = false
        actionSearch.isVisible = true
        actionUnlock.isVisible = true

        val searchView = actionSearch.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        chatListViewModel.onSearchQueryChanged(newText.orEmpty())
        return true
    }


    // ---------- Ciclo de vida ----------

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
