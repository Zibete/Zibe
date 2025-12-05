package com.zibete.proyecto1.ui.groups

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.zibete.proyecto1.PageAdapterGroup
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterGroups
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.databinding.DialogGoGroupBinding
import com.zibete.proyecto1.databinding.DialogGoNewGroupBinding
import com.zibete.proyecto1.databinding.FragmentGroupsBinding
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.utils.UserMessageUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import javax.inject.Inject
import kotlinx.coroutines.launch
private var joinGroupDialog: AlertDialog? = null

@AndroidEntryPoint
class GroupsFragment : BaseChatSessionFragment(), SearchView.OnQueryTextListener {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var firebaseRefsContainer: FirebaseRefsContainer

    private val user = userRepository.user
    private val myUid = userRepository.user.uid

    private val groupsViewModel: GroupsViewModel by viewModels()

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AdapterGroups
    private lateinit var layoutManager: GridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)

        setupRecycler()
        setupSwipeRefresh()
        setupFab()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    groupsViewModel.uiState.collect { state ->
                        val b = _binding ?: return@collect

                        b.progressbar.isVisible = state.isLoading

                        if (state.error != null) {
                            UserMessageUtils.showSnack(
                                root = view,
                                message = state.error,
                                duration = Snackbar.LENGTH_SHORT,
                                iconRes = R.drawable.ic_warning_24
                            )
                        }

                        adapter.updateDataGroups(ArrayList(state.groups))

                        if (state.groups.isNotEmpty()) {
                            b.rvGroups.scrollToPosition(0)
                        }
                    }
                }
            }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupsViewModel.events.collect { event ->
                    when (event) {
                        is GroupsUiEvent.NickInUse -> {
                            UserMessageUtils.showSnack(
                                root = view,
                                message = "${event.nick} está en uso",
                                duration = Snackbar.LENGTH_SHORT,
                                iconRes = R.drawable.ic_warning_24
                            )
                        }

                        is GroupsUiEvent.GroupNameInUse -> {
                            UserMessageUtils.showSnack(
                                root = view,
                                message = "El nombre ${event.name} ya está en uso",
                                duration = Snackbar.LENGTH_SHORT,
                                iconRes = R.drawable.ic_warning_24
                            )
                        }

                        is GroupsUiEvent.JoinGroup -> {
                            joinGroupDialog?.dismiss()
                            joinGroupDialog = null

                            groupsViewModel.joinGroupAndNavigate(
                                event.groupName,
                                event.nick,
                                event.type
                            )
                        }

                        is GroupsUiEvent.NavigateToGroupPager -> {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.nav_host_fragment, PageAdapterGroup())
                                .commit()
                        }
                    }
                }
            }
        }

        groupsViewModel.loadGroups()
    }

    // ---------- UI / Recycler ----------

    private fun setupRecycler() = with(binding) {
        layoutManager = GridLayoutManager(requireContext(), 2).apply {
            reverseLayout = false
        }

        adapter = AdapterGroups(
            mutableListOf(),
            mutableListOf(),
            requireContext()
        ) { groupSelected ->
            showJoinGroupDialog(groupSelected)
        }

        rvGroups.layoutManager = layoutManager
        rvGroups.adapter = adapter
    }

    private fun setupSwipeRefresh() = with(binding) {
        groupSwipeRefresh.setRecyclerView(rvGroups)
        groupSwipeRefresh.setOnRefreshListener {
            groupsViewModel.refreshGroups()
            groupSwipeRefresh.isRefreshing = false
        }
    }

    private fun setupFab() = with(binding) {
        fabNewGroup.setOnClickListener {
            showCreateGroupDialog()
        }
    }

    // 2. Diálogo para unirse a grupo público
    private fun showJoinGroupDialog(group: Groups) {
        val dialogBinding = DialogGoGroupBinding.inflate(layoutInflater)

        dialogBinding.nameUser.text = user.displayName
        dialogBinding.tvChat.text = group.name
        Glide.with(requireContext()).load(user.photoUrl).into(dialogBinding.userImage)
        dialogBinding.btnStartAnonymousChat.isEnabled = false

        dialogBinding.edtNick.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialogBinding.btnStartAnonymousChat.isEnabled =
                    dialogBinding.edtNick.text?.isNotEmpty() == true
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        joinGroupDialog = dialog

        dialogBinding.btnStartChat.setOnClickListener {
            groupsViewModel.onJoinGroupRequested(
                groupName = group.name,
                nick = user.displayName ?: "",
                type = PUBLIC_USER
            )
        }

        dialogBinding.btnStartAnonymousChat.setOnClickListener {
            val nick = dialogBinding.edtNick.text.toString()
            groupsViewModel.onJoinGroupRequested(
                group.name,
                nick,
                ANONYMOUS_USER)
        }

        dialogBinding.imgCancelDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // 3. Diálogo para crear nuevo grupo
    fun showCreateGroupDialog() {
        val dialogBinding = DialogGoNewGroupBinding.inflate(layoutInflater)

        dialogBinding.nameUser.text = user.displayName
        Glide.with(
            requireContext())
            .load(user.photoUrl)
            .into(dialogBinding.userImage)
        dialogBinding.btnCreateNewChat.isEnabled = false

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialogBinding.btnCreateNewChat.isEnabled =
                    dialogBinding.edtNameNewGroup.text?.isNotEmpty() == true &&
                            dialogBinding.edtDataNewGroup.text?.isNotEmpty() == true
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        dialogBinding.edtNameNewGroup.addTextChangedListener(watcher)
        dialogBinding.edtDataNewGroup.addTextChangedListener(watcher)


        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        joinGroupDialog = dialog

        dialogBinding.btnCreateNewChat.setOnClickListener {
            val groupName = dialogBinding.edtNameNewGroup.text.toString()
            val groupData = dialogBinding.edtDataNewGroup.text.toString()
            groupsViewModel.onCreateNewGroupClicked(groupName, groupData)
        }

        dialogBinding.imgCancelDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ---------- SearchView ----------

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        groupsViewModel.filter(newText.orEmpty())
        return true
    }

    // ---------- Menú ----------
    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val actionSearch = menu.findItem(R.id.action_search)
        val actionSettings = menu.findItem(R.id.action_settings)
        val actionUnlock = menu.findItem(R.id.action_unblock)
        val actionFavorites = menu.findItem(R.id.action_favorites)

        actionSearch.isVisible = true
        actionSettings.isVisible = true
        actionUnlock.isVisible = true
        actionFavorites.isVisible = true

        val searchView = actionSearch.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
    }

    // ---------- Ciclo de vida ----------

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
