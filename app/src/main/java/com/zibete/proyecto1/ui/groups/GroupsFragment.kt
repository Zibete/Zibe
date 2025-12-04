package com.zibete.proyecto1.ui.groups

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.zibete.proyecto1.PageAdapterGroup
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterGroups
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.databinding.DialogGoGroupBinding
import com.zibete.proyecto1.databinding.DialogGoNewGroupBinding
import com.zibete.proyecto1.databinding.FragmentGroupsBinding
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.utils.FirebaseRefs
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupsFragment : Fragment(), SearchView.OnQueryTextListener {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var userRepository: UserRepository

    private val user = userRepository.user

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
        collectUiState()
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
            onGroupClicked(groupSelected)
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

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupsViewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect

                    b.progressbar.isVisible = state.isLoading

                    if (state.error != null) {
                        Toast.makeText(requireContext(), state.error, Toast.LENGTH_SHORT).show()
                    }

                    adapter.updateDataGroups(ArrayList(state.groups))

                    if (state.groups.isNotEmpty()) {
                        b.rvGroups.scrollToPosition(0)
                    }
                }
            }
        }
    }

    // 1. Click en grupo existente
    private fun onGroupClicked(group: Groups) {
        showJoinGroupDialog(group)
    }

    // 2. Diálogo para unirse a grupo público
    private fun showJoinGroupDialog(group: Groups) {
        val dialogBinding = DialogGoGroupBinding.inflate(layoutInflater)

        dialogBinding.nameUser.text = user.displayName
        dialogBinding.tvChat.text = group.name
        Glide.with(requireContext()).load(user.photoUrl).into(dialogBinding.userImage)
        dialogBinding.btnStartChat2.isEnabled = false

        dialogBinding.edtNick.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialogBinding.btnStartChat2.isEnabled =
                    dialogBinding.edtNick.text?.isNotEmpty() == true
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(
            ContextThemeWrapper(
                requireContext(),
                R.style.AlertDialogApp
            )
        )
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnStartChat1.setOnClickListener {
            joinGroupAndNavigate(dialog, group.name, user.displayName ?: "", 1)
        }

        dialogBinding.btnStartChat2.setOnClickListener {
            FirebaseRefs.refGroupUsers.child(group.name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            val name = child.child("user_name").getValue(String::class.java)
                            if (name == dialogBinding.edtNick.text.toString()) {
                                Toast.makeText(
                                    requireContext(),
                                    "${dialogBinding.edtNick.text} está en uso",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
                            }
                        }
                        joinGroupAndNavigate(
                            dialog,
                            group.name,
                            dialogBinding.edtNick.text.toString(),
                            0
                        )
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        dialogBinding.imgCancelDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // 3. Diálogo para crear nuevo grupo
    fun showCreateGroupDialog() {
        val dialogBinding = DialogGoNewGroupBinding.inflate(layoutInflater)

        dialogBinding.nameUser.text = user.displayName
        Glide.with(requireContext()).load(user.photoUrl).into(dialogBinding.userImage)
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

        val dialog = AlertDialog.Builder(
            ContextThemeWrapper(
                requireContext(),
                R.style.AlertDialogApp
            )
        )
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnCreateNewChat.setOnClickListener {
            val name = dialogBinding.edtNameNewGroup.text.toString()
            val data = dialogBinding.edtDataNewGroup.text.toString()

            FirebaseRefs.refGroupData.child(name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(
                                requireContext(),
                                "El nombre ya está en uso",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }

                        @SuppressLint("SimpleDateFormat")
                        val date = SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date())
                        val group =
                            Groups(
                                name,
                                data,
                                user.uid,
                                Constants.PUBLIC_GROUP,
                                0,
                                date
                            )

                        FirebaseRefs.refGroupData.child(name).setValue(group)

                        joinGroupAndNavigate(
                            dialog,
                            name,
                            user.displayName ?: "",
                            Constants.PUBLIC_GROUP
                        )
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        dialogBinding.imgCancelDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // 4. Lógica central de navegación y guardado
    private fun joinGroupAndNavigate(
        dialog: AlertDialog?,
        groupName: String,
        userName: String,
        type: Int
    ) {

        @SuppressLint("SimpleDateFormat")
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS").format(Date())

        // Repo
        userPreferencesRepository.userNameGroup = userName
        userPreferencesRepository.groupName = groupName
        userPreferencesRepository.inGroup = true
        userPreferencesRepository.userType = type
        userPreferencesRepository.userDate = date

        // Firebase chat
        val chatMsg =
            ChatsGroup("se unió a la sala", date, userName, user.uid, 0, type)
        FirebaseRefs.refGroupChat.child(groupName).push().setValue(chatMsg)

        val query: Query = FirebaseRefs.refDatos.child(user.uid)
            .child(Constants.CHAT_STATE_UNKNOWN)
            .orderByChild("noVisto")
            .startAt(1.0)

        // Listeners opcionales (comentados)
        // MainActivity.listenerGroupBadge?.let { listener ->
        //     FirebaseRefs.refGroupChat.child(groupName).addValueEventListener(listener)
        // }
        // MainActivity.listenerMsgUnreadBadge?.let { listener ->
        //     query.addValueEventListener(listener)
        // }

        (activity as? MainActivity)?.invalidateOptionsMenu()

        val newFragment = PageAdapterGroup()
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, newFragment)
            .commit()

        val userGroup = UserGroup(user.uid, userName, type)
        FirebaseRefs.refGroupUsers.child(groupName).child(user.uid).setValue(userGroup)

        dialog?.dismiss()
    }

    // ---------- SearchView ----------

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        groupsViewModel.filter(newText.orEmpty())
        return true
    }

    // ---------- Menú ----------

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val actionSearch = menu.findItem(R.id.action_search)
        val actionSettings = menu.findItem(R.id.action_settings)
        val actionUnlock = menu.findItem(R.id.action_unblock)
        val actionFavorites = menu.findItem(R.id.action_favorites)
        val actionExit = menu.findItem(R.id.action_exit_group)

        actionExit.isVisible = false
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
