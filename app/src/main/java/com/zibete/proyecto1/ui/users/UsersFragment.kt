package com.zibete.proyecto1.ui.users

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterUsers
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_START_INDEX
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_IDS
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.databinding.FilterLayoutBinding
import com.zibete.proyecto1.databinding.FragmentUsersBinding
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.profile.ProfileActivity
import com.zibete.proyecto1.ui.search.SearchHandler
import com.zibete.proyecto1.ui.users.UsersToolbarHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsersFragment : BaseChatSessionFragment(), SearchHandler, UsersToolbarHandler {

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
            formatDistance = { meters -> usersViewModel.formatDistance(meters) }
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

        val filterOn = UiText.StringRes(R.string.action_filter_on).asString(ctx)
        val filterOff = UiText.StringRes(R.string.action_filter_off).asString(ctx)
        val actionCancel = UiText.StringRes(R.string.action_cancel).asString(ctx)

        AlertDialog.Builder(ctx)
            .setView(dialogBinding.root)
            .setPositiveButton(filterOn) { _, _ ->
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
            .setNegativeButton(actionCancel, null)
            .setNeutralButton(filterOff) { _, _ ->
                usersViewModel.clearFilters()
            }
            .create()
            .show()
    }

    // SearchHandler: llamado por MainActivity cuando el SearchView cambia
    override fun onSearchQueryChanged(query: String?) {
        adapterUsers?.filterByName(query)
    }

    override fun onRefreshUsers() {
        usersViewModel.loadUsers()
    }

    override fun onFilterUsers() {
        usersViewModel.onFilterClicked()
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
