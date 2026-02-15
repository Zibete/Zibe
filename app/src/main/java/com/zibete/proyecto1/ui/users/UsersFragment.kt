package com.zibete.proyecto1.ui.users

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zibete.proyecto1.BuildConfig
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterUsers
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_START_INDEX
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_IDS
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.databinding.FilterLayoutBinding
import com.zibete.proyecto1.databinding.FragmentUsersBinding
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.main.MainUiEvent
import com.zibete.proyecto1.ui.main.MainViewModel
import com.zibete.proyecto1.ui.profile.ProfileActivity
import com.zibete.proyecto1.ui.search.SearchHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UsersFragment : BaseChatSessionFragment(), SearchHandler, UsersToolbarHandler {

    @Inject
    lateinit var profileRepositoryProvider: ProfileRepositoryProvider

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    private val usersViewModel: UsersViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var layoutManager: LinearLayoutManager
    private var adapterUsers: AdapterUsers? = null
    private var scrollListener: RecyclerView.OnScrollListener? = null
    private val scrollTopThreshold = 3
    private val prefetchExtra = 4

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
    }

    private fun setupRecyclerView() {
        layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = false
            stackFromEnd = false
        }

        binding.rv.apply {
            this.layoutManager = this@UsersFragment.layoutManager
            setHasFixedSize(true)
        }

        adapterUsers = AdapterUsers(
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            profileRepositoryProvider = profileRepositoryProvider,
            onChatClicked = { userId -> usersViewModel.onUserChatClick(userId) },
            onProfileClicked = { userId -> usersViewModel.onUserProfileClick(userId) },
            formatDistance = { meters -> usersViewModel.formatDistance(meters) }
        )
        binding.rv.adapter = adapterUsers
        setupScrollTopFab()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setRecyclerView(binding.rv)
            setOnRefreshListener {
                usersViewModel.loadUsers()
            }
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usersViewModel.uiState.collect { state ->
                    _binding?.let { b ->
                        b.progressIndicator.isVisible = state.isLoading
                        b.swipeRefresh.isRefreshing = state.isLoading

                        adapterUsers?.submitUsers(state.users)
                        b.rv.post { prefetchVisibleHasBlockedMe() }

                        updateScrollTopFab()
                    }
                }
            }
        }
    }

    private fun collectEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usersViewModel.events.collect { event ->
                    when (event) {
                        is UsersUiEvent.ShowFilterDialog -> showFilterDialog(event)
                        is UsersUiEvent.NavigateToChat -> openChat(event.userId)
                        is UsersUiEvent.NavigateToProfile -> openProfile(
                            event.userIds,
                            event.startIndex
                        )

                        is UsersUiEvent.ShowSnack -> {
                            mainViewModel.emit(
                                MainUiEvent.ShowSnack(
                                    uiText = event.uiText,
                                    snackType = event.snackType
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showFilterDialog(event: UsersUiEvent.ShowFilterDialog) {
        val ctx = requireContext()
        val dialogBinding = FilterLayoutBinding.inflate(layoutInflater)
        val slider = dialogBinding.rangeAge

        val minAllowed = 18
        val maxAllowed = 99

        // 1. Configuración inicial y valores de referencia
        slider.valueFrom = minAllowed.toFloat()
        slider.valueTo = maxAllowed.toFloat()
        slider.stepSize = 1f

        val initialMin = if (event.minAge in minAllowed..maxAllowed) event.minAge else minAllowed
        val initialMax = if (event.maxAge in minAllowed..maxAllowed) event.maxAge else maxAllowed
        slider.values = listOf(initialMin.toFloat(), initialMax.toFloat())

        // 2. Funciones auxiliares
        fun currentValues(): Pair<Int, Int> {
            val v = slider.values
            return (v.getOrNull(0)?.toInt() ?: minAllowed) to (v.getOrNull(1)?.toInt()
                ?: maxAllowed)
        }

        fun updateAgeLabels(minValue: Int, maxValue: Int) {
            dialogBinding.tvMinAge.text = ctx.getString(R.string.label_age, minValue)
            dialogBinding.tvMaxAge.text = ctx.getString(R.string.label_age, maxValue)
        }

        // 3. Lógica de habilitación de botones (La clave de tu pedido)
        fun checkChanges() {
            val (currentMin, currentMax) = currentValues()
            val currentAgeEnabled = dialogBinding.switchAge.isChecked
            val currentOnlineEnabled = dialogBinding.switchOnline.isChecked

            // ¿Cambió algo respecto al evento inicial?
            val hasChanged = currentAgeEnabled != event.applyAgeFilter ||
                    currentOnlineEnabled != event.applyOnlineFilter ||
                    (currentAgeEnabled && (currentMin != event.minAge || currentMax != event.maxAge))

            // ¿Hay algún filtro activo actualmente para permitir "Quitar Filtros"?
            val anyFilterActive = event.applyAgeFilter || event.applyOnlineFilter

            dialogBinding.btnApplyFilter.isEnabled = hasChanged
            dialogBinding.btnClearFilter.isEnabled = anyFilterActive
        }

        // 4. Sincronización inicial de UI
        val (startMin, startMax) = currentValues()
        updateAgeLabels(startMin, startMax)

        dialogBinding.switchOnline.isChecked = event.applyOnlineFilter
        dialogBinding.switchAge.isChecked = event.applyAgeFilter

        val isAgeActive = event.applyAgeFilter
        slider.isEnabled = isAgeActive
        dialogBinding.tvMinAge.isEnabled = isAgeActive
        dialogBinding.tvMaxAge.isEnabled = isAgeActive

        // Ejecutamos por primera vez para setear el estado inicial de los botones
        checkChanges()

        // 5. Listeners con llamada a checkChanges()
        slider.addOnChangeListener { _, _, _ ->
            val (min, max) = currentValues()
            updateAgeLabels(min, max)
            checkChanges()
        }

        dialogBinding.switchAge.setOnCheckedChangeListener { _, checked ->
            slider.isEnabled = checked
            dialogBinding.tvMinAge.isEnabled = checked
            dialogBinding.tvMaxAge.isEnabled = checked
            checkChanges()
        }

        dialogBinding.switchOnline.setOnCheckedChangeListener { _, _ ->
            checkChanges()
        }

        // 6. Mostrar Diálogo
        val dialog = MaterialAlertDialogBuilder(ctx)
            .setView(dialogBinding.root)
            .show()

        // 7. Click Listeners finales
        dialogBinding.btnApplyFilter.setOnClickListener {
            val (finalMin, finalMax) = currentValues()
            usersViewModel.applyFilters(
                applyAgeFilter = dialogBinding.switchAge.isChecked,
                applyOnlineFilter = dialogBinding.switchOnline.isChecked,
                minAge = finalMin,
                maxAge = finalMax
            )
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnClearFilter.setOnClickListener {
            usersViewModel.clearFilters()
            dialog.dismiss()
        }
    }

    private fun openChat(userId: String) {
        startActivity(
            Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra(EXTRA_CHAT_ID, userId)
                putExtra(EXTRA_CHAT_NODE, NODE_DM)
            }
        )
    }

    private fun openProfile(userIds: ArrayList<String>, startIndex: Int) {
        startActivity(
            Intent(requireContext(), ProfileActivity::class.java)
                .putStringArrayListExtra(EXTRA_USER_IDS, userIds)
                .putExtra(EXTRA_START_INDEX, startIndex)
        )
    }

    override fun onSearchQueryChanged(query: String?) =
        usersViewModel.onSearchQueryChanged(query)

    override fun onRefreshUsers() = usersViewModel.loadUsers()

    override fun onFilterUsers() = usersViewModel.onFilterClicked()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.let { b ->
            scrollListener?.let { b.rv.removeOnScrollListener(it) }
        }
        scrollListener = null
        adapterUsers = null
        _binding = null
    }

    private fun setupScrollTopFab() {
        binding.fabScrollTop.setOnClickListener {
            binding.rv.smoothScrollToPosition(0)
        }

        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollTopFab()
                prefetchVisibleHasBlockedMe()
            }
        }
        scrollListener?.let { binding.rv.addOnScrollListener(it) }
        updateScrollTopFab()
    }

    private fun updateScrollTopFab() {
        _binding?.let { b ->
            val firstVisible = layoutManager.findFirstVisibleItemPosition()
            b.fabScrollTop.isVisible = firstVisible > scrollTopThreshold
        }
    }

    private fun prefetchVisibleHasBlockedMe() {
        val adapter = adapterUsers ?: return
        if (adapter.itemCount == 0) return
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) return
        val start = (first - prefetchExtra).coerceAtLeast(0)
        val end = (last + prefetchExtra).coerceAtMost(adapter.itemCount - 1)
        if (start > end) return
        val ids = (start..end).mapNotNull { adapter.currentList.getOrNull(it)?.id }
        usersViewModel.prefetchHasBlockedMe(ids)
    }
}
