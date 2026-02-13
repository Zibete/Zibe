package com.zibete.proyecto1.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.zibete.proyecto1.adapters.AdapterFavoriteUsers
import com.zibete.proyecto1.databinding.FragmentFavoritesBinding
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.ui.main.MainUiEvent
import com.zibete.proyecto1.ui.main.MainViewModel
import com.zibete.proyecto1.ui.profile.ProfileActivity
import com.zibete.proyecto1.ui.search.SearchHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : BaseChatSessionFragment(), SearchHandler {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val favoritesViewModel: FavoritesViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var adapterFavoriteUsers: AdapterFavoriteUsers

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        collectUiState()
        collectEvents()

        favoritesViewModel.loadFavorites()
    }

    private fun setupRecyclerView() = with(binding) {
        rvFavorites.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            setHasFixedSize(true)
            adapter = AdapterFavoriteUsers { favoriteUser ->
                startActivity(
                    Intent(requireContext(), ProfileActivity::class.java)
                        .putExtra(EXTRA_USER_ID, favoriteUser.id)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                )
            }.also { adapterFavoriteUsers = it }
        }
    }

    private fun setupSwipeRefresh() = with(binding) {
        favoriteSwipeRefresh.setRecyclerView(rvFavorites)
        favoriteSwipeRefresh.setOnRefreshListener {
            favoritesViewModel.refreshFavorites()
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                favoritesViewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect
                    b.progressIndicator.isVisible = state.isLoading
                    b.rvFavorites.isVisible = !state.isLoading
                    if (!state.isLoading) b.favoriteSwipeRefresh.isRefreshing = false
                    adapterFavoriteUsers.submitOriginal(state.favorites)
                }
            }
        }
    }

    private fun collectEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                favoritesViewModel.events.collect { event ->
                    when (event) {
                        is FavoritesUiEvent.ShowSnack -> {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSearchQueryChanged(query: String?) {
        adapterFavoriteUsers.filterByName(query)
    }
}
