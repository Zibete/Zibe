package com.zibete.proyecto1.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
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
import com.zibete.proyecto1.ui.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.ui.constants.ERR_ZIBE
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.search.SearchHandler
import com.zibete.proyecto1.utils.UserMessageUtils
import com.zibete.proyecto1.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupsFragment : BaseChatSessionFragment(), SearchHandler {

    private var joinGroupDialog: AlertDialog? = null

    private val groupsViewModel: GroupsViewModel by viewModels()

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterGroups: AdapterGroups

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupOptionMenu()

        collectUiState()
        collectEvents()

        groupsViewModel.loadGroups()
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupsViewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect

                    b.progressbar.isVisible = state.isLoading
                    b.rvGroups.isVisible = !state.isLoading

                    // Spinner del swipe se apaga cuando termina la carga
                    if (!state.isLoading) {
                        b.groupSwipeRefresh.isRefreshing = false
                    }

                    adapterGroups.submitOriginal(state.groups)
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
                            UserMessageUtils.showSnack(
                                root = binding.root,
                                message = "${event.nick} está en uso",
                                type = ZibeSnackType.INFO
                            )
                        }

                        is GroupsUiEvent.GroupNameInUse -> {
                            UserMessageUtils.showSnack(
                                root = binding.root,
                                message = "El nombre ${event.name} ya está en uso",
                                type = ZibeSnackType.WARNING
                            )
                        }

                        is GroupsUiEvent.ShowMessage -> {
                            UserMessageUtils.showSnack(
                                root = binding.root,
                                message = event.message ?: ERR_ZIBE,
                                type = ZibeSnackType.INFO
                            )
                        }

                        is GroupsUiEvent.NavigateToGroupHost -> {
                            joinGroupDialog?.dismiss()
                            joinGroupDialog = null
                            (activity as? MainActivity)?.mainViewModel?.toGroupHost()
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() = with(binding) {
        rvGroups.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
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

        dialogBinding.edtNick.addTextChangedListener(Utils.SimpleWatcher { text ->
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
                type = PUBLIC_USER
            )
        }

        dialogBinding.btnStartAnonymousChat.setOnClickListener {
            val nick = dialogBinding.edtNick.text.toString()
            groupsViewModel.onJoinGroupRequested(
                groupName = group.name,
                nick = nick,
                type = ANONYMOUS_USER
            )
        }

        dialogBinding.imgCancelDialog.setOnClickListener { joinGroupDialog?.dismiss() }
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

        dialogBinding.edtNameNewGroup.addTextChangedListener(Utils.SimpleWatcher(validate))
        dialogBinding.edtDataNewGroup.addTextChangedListener(Utils.SimpleWatcher(validate))

        joinGroupDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnCreateNewChat.setOnClickListener {
            groupsViewModel.onCreateNewGroupClicked(
                groupName = dialogBinding.edtNameNewGroup.text.toString(),
                groupData = dialogBinding.edtDataNewGroup.text.toString()
            )
        }

        dialogBinding.imgCancelDialog.setOnClickListener { joinGroupDialog?.dismiss() }
        joinGroupDialog?.show()
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
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        joinGroupDialog?.dismiss()
        joinGroupDialog = null
    }

    override fun onSearchQueryChanged(query: String?) {
        adapterGroups.filterByName(query)
    }
}