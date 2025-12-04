package com.zibete.proyecto1.ui.users

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlideProfileActivity
import com.zibete.proyecto1.adapters.AdapterUsers
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.databinding.FilterLayoutBinding
import com.zibete.proyecto1.databinding.FragmentUsersBinding
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.constants.DIALOG_FILTER_OFF
import com.zibete.proyecto1.ui.constants.DIALOG_FILTER_ON
import com.zibete.proyecto1.utils.ProfileUiBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UsersFragment : Fragment(), SearchView.OnQueryTextListener {

    @Inject lateinit var userPreferencesRepository : UserPreferencesRepository
    @Inject lateinit var userRepository : UserRepository
    @Inject lateinit var profileUiBinder : ProfileUiBinder
    @Inject lateinit var locationRepository : LocationRepository

    private lateinit var _binding: FragmentUsersBinding
    private val binding get() = _binding

    private val usersViewModel: UsersViewModel by viewModels()
    private var adapterUsers: AdapterUsers? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentUsersBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        setupRecyclerView()
        setupSwipeRefresh()

        // Carga inicial
        usersViewModel.loadUsers(isRefresh = false)

        // Observamos el estado
        viewLifecycleOwner.lifecycleScope.launch {
            usersViewModel.uiState.collect { state ->
                // loading
                binding.progressbar.visibility =
                    if (state.isLoading) View.VISIBLE else View.GONE

                // datos
                adapterUsers?.updateDataUsers(state.users)

                // scroll al final
                scrollToBottom()
            }
        }

        // Eventos one-shot (como abrir el diálogo de filtros)
        viewLifecycleOwner.lifecycleScope.launch {
            usersViewModel.events.collect { event ->
                when (event) {
                    UsersUiEvent.ShowFilterDialog -> showFilterDialog()
                    else -> {}
                }
            }
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        val rv = binding.rv
        val mLayoutManager = LinearLayoutManager(context).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        rv.apply {
            layoutManager = mLayoutManager
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        }

        adapterUsers = AdapterUsers(
            context = requireContext(),
            locationRepository = locationRepository,
            onChatClicked = { userId ->
                val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                    putExtra("id_user", userId)
                }
                startActivity(intent)
            },
            onProfileClicked = { selectedUser ->
                val extra = ArrayList(usersViewModel.uiState.value.users)
                extra.reverse()

                val position = extra.indexOf(selectedUser)

                val intent = Intent(requireContext(), SlideProfileActivity::class.java).apply {
                    putExtra("userList", extra)
                    putExtra("position", position)
                    putExtra("rotation", 0)
                }
                startActivity(intent)
            }
        )

        rv.adapter = adapterUsers
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setRecyclerView(binding.rv)
            setOnRefreshListener {
                usersViewModel.loadUsers(isRefresh = true)
                isRefreshing = false
            }
        }
    }

    private fun showFilterDialog() {
        val ctx = requireContext()
        val dialogBinding = FilterLayoutBinding.inflate(layoutInflater)

        val ages = (18..99).toList()
        val adapter = ArrayAdapter(ctx, R.layout.tv_spinner_selected, ages).apply {
            setDropDownViewResource(R.layout.tv_spinner_lista)
        }

        dialogBinding.spinnerMinAge.adapter = adapter
        dialogBinding.spinerMaxAge.adapter = adapter

        // cargar valores actuales
        dialogBinding.switchAge.isChecked = userPreferencesRepository.applyAgeFilter
        dialogBinding.switchOnline.isChecked = userPreferencesRepository.applyOnlineFilter

        dialogBinding.spinnerMinAge.isEnabled = dialogBinding.switchAge.isChecked
        dialogBinding.spinerMaxAge.isEnabled = dialogBinding.switchAge.isChecked

        val dialog = AlertDialog.Builder(ctx)
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

        dialog.show()
    }

    private fun scrollToBottom() {
        val count = adapterUsers?.itemCount ?: 0
        if (count > 0) {
            binding.rv.scrollToPosition(count - 1)
        }
    }

    // --- Search Logic ---

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_exit_group)?.isVisible = false
        menu.findItem(R.id.action_unblock)?.isVisible = true

        val searchItem = menu.findItem(R.id.action_search)
        searchItem?.isVisible = true

        val searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        adapterUsers?.filter?.filter(newText)
        return false
    }

    override fun onResume() {
        super.onResume()
        // mainUiViewModel.showToolbar()
        // mainUiViewModel.hideLayoutSettings()
    }
}
