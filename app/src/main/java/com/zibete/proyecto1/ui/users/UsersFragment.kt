package com.zibete.proyecto1.ui.users

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterUsers
import com.zibete.proyecto1.databinding.FilterLayoutBinding
import com.zibete.proyecto1.databinding.FragmentUsersBinding
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_START_INDEX
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_USER_IDS
import com.zibete.proyecto1.ui.constants.Constants.NODE_DM
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.constants.DIALOG_FILTER_OFF
import com.zibete.proyecto1.ui.constants.DIALOG_FILTER_ON
import com.zibete.proyecto1.ui.profile.ProfileActivity
import com.zibete.proyecto1.ui.search.SearchHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsersFragment : BaseChatSessionFragment(), SearchHandler {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    private val usersViewModel: UsersViewModel by viewModels()

    private lateinit var layoutManager: LinearLayoutManager
    private var adapterUsers: AdapterUsers? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupOptionMenu()
        setupRecyclerView()
        setupSwipeRefresh()

        usersViewModel.loadUsers()

        collectUiState()
        collectEvents()

        // Para que el menú se “reprepare” y muestre lo que corresponde al entrar
        requireActivity().invalidateOptionsMenu()
    }

    private fun collectEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usersViewModel.events.collect { event ->
                    when (event) {
                        UsersUiEvent.ShowFilterDialog -> showFilterDialog()
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usersViewModel.uiState.collect { state ->

                    binding.progressbar.isVisible = state.isLoading
                    binding.swipeRefresh.isRefreshing = state.isLoading

                    val shouldStickToBottom = isUserAtBottom()

                    adapterUsers?.submitUsers(state.users)

                    if (shouldStickToBottom) scrollToBottom()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        binding.rv.apply {
            this.layoutManager = this@UsersFragment.layoutManager
            setHasFixedSize(true)
        }

        adapterUsers = AdapterUsers(
            onChatClicked = { userId ->
                startActivity(
                    Intent(requireContext(), ChatActivity::class.java).apply {
                        putExtra(EXTRA_CHAT_ID, userId)
                        putExtra(EXTRA_CHAT_NODE, NODE_DM)
                    }
                ) },
            onProfileClicked = { selectedUser ->
                val list = ArrayList(usersViewModel.uiState.value.users)
                list.reverse()

                val position = list.indexOf(selectedUser).coerceAtLeast(0)
                val ids = ArrayList(list.map { it.id })

                startActivity(
                    Intent(requireContext(), ProfileActivity::class.java)
                        .putStringArrayListExtra(EXTRA_USER_IDS, ids)
                        .putExtra(EXTRA_START_INDEX, position)
                ) },
            formatDistance = { meters -> usersViewModel.locationRepository.formatDistance(meters) }
        )

        binding.rv.adapter = adapterUsers
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setRecyclerView(binding.rv)
            setOnRefreshListener {
                usersViewModel.loadUsers()
            }
        }
    }

    private fun showFilterDialog() {
        val ctx = requireContext()
        val dialogBinding = FilterLayoutBinding.inflate(layoutInflater)

        val ages = (18..99).toList()
        val spinnerAdapter = ArrayAdapter(ctx, R.layout.tv_spinner_selected, ages).apply {
            setDropDownViewResource(R.layout.tv_spinner_lista)
        }

        dialogBinding.spinnerMinAge.adapter = spinnerAdapter
        dialogBinding.spinerMaxAge.adapter = spinnerAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            dialogBinding.switchAge.isChecked = usersViewModel.currentApplyAgeFilter()
            dialogBinding.switchOnline.isChecked = usersViewModel.currentApplyOnlineFilter()
        }

        dialogBinding.spinnerMinAge.isEnabled = dialogBinding.switchAge.isChecked
        dialogBinding.spinerMaxAge.isEnabled = dialogBinding.switchAge.isChecked

        dialogBinding.switchAge.setOnCheckedChangeListener { _, checked ->
            dialogBinding.spinnerMinAge.isEnabled = checked
            dialogBinding.spinerMaxAge.isEnabled = checked
        }

        AlertDialog.Builder(ctx)
            .setView(dialogBinding.root)
            .setPositiveButton(DIALOG_FILTER_ON) { _, _ ->
                val applyAgeFilter = dialogBinding.switchAge.isChecked
                val applyOnlineFilter = dialogBinding.switchOnline.isChecked
                val minAge = ages[dialogBinding.spinnerMinAge.selectedItemPosition]
                val maxAge = ages[dialogBinding.spinerMaxAge.selectedItemPosition]

                usersViewModel.applyFilters(
                    applyAgeFilter = applyAgeFilter,
                    applyOnlineFilter = applyOnlineFilter,
                    minAge = minAge,
                    maxAge = maxAge
                )
            }
            .setNegativeButton(DIALOG_CANCEL, null)
            .setNeutralButton(DIALOG_FILTER_OFF) { _, _ ->
                usersViewModel.clearFilters()
            }
            .create()
            .show()
    }

    private fun setupOptionMenu() {
        val menuHost = requireActivity() as MenuHost

        menuHost.addMenuProvider(
            object : MenuProvider {

                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // No-op: el menú lo infla BaseToolbarActivity
                }

                override fun onPrepareMenu(menu: Menu) {
                    // Mostramos SOLO lo que Users necesita.
                    // El resto ya queda oculto por BaseToolbarActivity.
                    menu.findItem(R.id.action_settings)?.isVisible = true
                    menu.findItem(R.id.action_unblock_users)?.isVisible = true
                    menu.findItem(R.id.action_unhide_chats)?.isVisible = true
                    menu.findItem(R.id.action_favorites)?.isVisible = true
                    menu.findItem(R.id.action_search)?.isVisible = true

                    // Si tenés ítems que NO querés en Users, asegurate de dejarlos false acá.
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    // No manejamos clicks desde el fragment:
                    // se delega a BaseToolbarActivity/MainActivity
                    return false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    // SearchHandler: llamado por MainActivity cuando el SearchView cambia
    override fun onSearchQueryChanged(query: String?) {
        adapterUsers?.filterByName(query)
    }

    private fun scrollToBottom() {
        val count = adapterUsers?.itemCount ?: 0
        if (count > 0) binding.rv.scrollToPosition(count - 1)
    }

    private fun isUserAtBottom(): Boolean {
        val count = adapterUsers?.itemCount ?: 0
        if (count == 0) return true
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        return lastVisible >= count - 2
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapterUsers = null
        _binding = null
    }
}
