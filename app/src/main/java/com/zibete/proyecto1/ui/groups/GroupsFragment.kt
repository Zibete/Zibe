package com.zibete.proyecto1.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterGroups
import com.zibete.proyecto1.databinding.DialogGoGroupBinding
import com.zibete.proyecto1.databinding.DialogGoNewGroupBinding
import com.zibete.proyecto1.databinding.FragmentGroupsBinding
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.core.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.main.MainUiEvent
import com.zibete.proyecto1.ui.main.MainViewModel
import com.zibete.proyecto1.ui.search.SearchHandler
import com.zibete.proyecto1.core.utils.SimpleWatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupsFragment : BaseChatSessionFragment(), SearchHandler {

    private var joinGroupDialog: AlertDialog? = null

    private val groupsViewModel: GroupsViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterGroups: AdapterGroups
    private lateinit var layoutManager: GridLayoutManager
    private var scrollListener: RecyclerView.OnScrollListener? = null
    private val scrollTopThreshold = 3

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        setupScrollTopFab()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectUiState()
        collectEvents()

        groupsViewModel.loadGroups()
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupsViewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun collectEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupsViewModel.events.collect { event ->
                    when (event) {
                        is GroupsUiEvent.NickInUse -> {
                            mainViewModel.emit(
                                MainUiEvent.ShowSnack(
                                    uiText = UiText.StringRes(
                                        R.string.group_nick_in_use,
                                        listOf(event.nick)
                                    ),
                                    snackType = ZibeSnackType.INFO
                                )
                            )
                        }

                        is GroupsUiEvent.GroupNameInUse -> {
                            mainViewModel.emit(
                                MainUiEvent.ShowSnack(
                                    uiText = UiText.StringRes(
                                        R.string.group_name_in_use,
                                        listOf(event.name)
                                    ),
                                    snackType = ZibeSnackType.WARNING
                                )
                            )
                        }

                        is GroupsUiEvent.ShowSnack -> {
                            mainViewModel.emit(
                                MainUiEvent.ShowSnack(
                                    uiText = event.uiText,
                                    snackType = event.snackType
                                )
                            )
                        }

                        is GroupsUiEvent.NavigateToGroupHost -> {
                            joinGroupDialog?.dismiss()
                            joinGroupDialog = null
                            mainViewModel.toGroupHost()
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() = with(binding) {
        layoutManager = GridLayoutManager(requireContext(), 2)
        rvGroups.apply {
            layoutManager = this@GroupsFragment.layoutManager
            setHasFixedSize(true)
            adapter = AdapterGroups { groupSelected ->
                showJoinGroupDialog(groupSelected)
            }.also { adapterGroups = it }
        }
    }

    private fun setupSwipeRefresh() = with(binding) {
        groupSwipeRefresh.setRecyclerView(rvGroups)
        groupSwipeRefresh.setOnRefreshListener {
            groupsViewModel.refreshGroups()
        }
    }

    private fun setupFab() = with(binding) {
        fabNewGroup.setOnClickListener { showCreateGroupDialog() }
    }

    private fun showJoinGroupDialog(group: Groups) {
        val dialogBinding = DialogGoGroupBinding.inflate(layoutInflater)

        val displayName = groupsViewModel.myDisplayName()
        val photoUrl = groupsViewModel.myPhotoUrl()

        dialogBinding.nameUser.text = displayName
        dialogBinding.tvChat.text = group.name
        Glide.with(requireContext()).load(photoUrl).into(dialogBinding.userImage)

        dialogBinding.btnStartAnonymousChat.isEnabled = false

        dialogBinding.edtNick.addTextChangedListener(SimpleWatcher { text ->
            dialogBinding.btnStartAnonymousChat.isEnabled = text.isNotEmpty()
        })

        joinGroupDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnStartChat.setOnClickListener {
            groupsViewModel.onJoinGroupRequested(
                groupName = group.name,
                nick = displayName,
                type = PUBLIC_USER,
                message = requireContext().getString(R.string.msg_user_joined)
            )
        }

        dialogBinding.btnStartAnonymousChat.setOnClickListener {
            val nick = dialogBinding.edtNick.text.toString()
            groupsViewModel.onJoinGroupRequested(
                groupName = group.name,
                nick = nick,
                type = ANONYMOUS_USER,
                message = requireContext().getString(R.string.msg_user_joined)
            )
        }

        dialogBinding.btnClose.setOnClickListener { joinGroupDialog?.dismiss() }
        joinGroupDialog?.show()
    }

    private fun showCreateGroupDialog() {
        val dialogBinding = DialogGoNewGroupBinding.inflate(layoutInflater)

        val displayName = groupsViewModel.myDisplayName()
        val photoUrl = groupsViewModel.myPhotoUrl()

        dialogBinding.nameUser.text = displayName
        Glide.with(requireContext()).load(photoUrl).into(dialogBinding.userImage)
        dialogBinding.btnCreateNewChat.isEnabled = false

        val validate = { _: String ->
            dialogBinding.btnCreateNewChat.isEnabled =
                dialogBinding.edtNameNewGroup.text?.isNotEmpty() == true &&
                        dialogBinding.edtDataNewGroup.text?.isNotEmpty() == true
        }

        dialogBinding.edtNameNewGroup.addTextChangedListener(SimpleWatcher(validate))
        dialogBinding.edtDataNewGroup.addTextChangedListener(SimpleWatcher(validate))

        joinGroupDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnCreateNewChat.setOnClickListener {
            groupsViewModel.onCreateNewGroupClicked(
                groupName = dialogBinding.edtNameNewGroup.text.toString(),
                groupData = dialogBinding.edtDataNewGroup.text.toString(),
                message = requireContext().getString(R.string.msg_user_joined)
            )
        }

        dialogBinding.btnClose.setOnClickListener { joinGroupDialog?.dismiss() }
        joinGroupDialog?.show()
    }

    private fun render(state: GroupsUiState) {
        val b = _binding ?: return

        b.progressIndicator.isVisible = state.isLoading
        if (!state.isLoading) b.groupSwipeRefresh.isRefreshing = false

        adapterGroups.submitOriginal(state.filteredGroups)
        updateScrollTopFab()
    }

    private fun setupScrollTopFab() {
        binding.fabScrollTop.setOnClickListener {
            binding.rvGroups.smoothScrollToPosition(0)
        }

        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollTopFab()
            }
        }
        scrollListener?.let { binding.rvGroups.addOnScrollListener(it) }
        updateScrollTopFab()
    }

    private fun updateScrollTopFab() {
        val b = _binding ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        b.fabScrollTop.isVisible = firstVisible > scrollTopThreshold
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.let { b ->
            scrollListener?.let { b.rvGroups.removeOnScrollListener(it) }
        }
        scrollListener = null
        joinGroupDialog?.dismiss()
        joinGroupDialog = null
        _binding = null
    }

    override fun onSearchQueryChanged(query: String?) {
        groupsViewModel.onSearchQueryChanged(query.orEmpty())
    }
}
