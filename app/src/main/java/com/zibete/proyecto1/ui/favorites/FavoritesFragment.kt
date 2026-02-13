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
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var layoutManager: GridLayoutManager
    private var scrollListener: RecyclerView.OnScrollListener? = null
    private val scrollTopThreshold = 3

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
        setupScrollTopFab()
        collectUiState()
        collectEvents()

        favoritesViewModel.loadFavorites()
    }

    private fun setupRecyclerView() = with(binding) {
        layoutManager = GridLayoutManager(requireContext(), 3)
        rvFavorites.apply {
            layoutManager = this@FavoritesFragment.layoutManager
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
                    render(state)
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
        _binding?.let { b ->
            scrollListener?.let { b.rvFavorites.removeOnScrollListener(it) }
            b.lottieFavorites.cancelAnimation()
        }
        scrollListener = null
        _binding = null
    }

    override fun onSearchQueryChanged(query: String?) {
        favoritesViewModel.onSearchQueryChanged(query.orEmpty())
    }

    private fun render(state: FavoritesUiState) {
        val b = _binding ?: return

        b.progressIndicator.isVisible = state.isLoading
        b.favoriteSwipeRefresh.isRefreshing = state.isRefreshing

        if (state.showOnboarding) showOnBoarding(b)
        else showFavoritesList(b)

        adapterFavoriteUsers.submitOriginal(state.filteredFavorites)
        updateScrollTopFab()
    }

    private fun showOnBoarding(b: FragmentFavoritesBinding) {
        b.rvFavorites.isVisible = false
        b.linearOnboardingFavorites.isVisible = true
        b.lottieFavorites.playAnimation()
    }

    private fun showFavoritesList(b: FragmentFavoritesBinding) {
        b.rvFavorites.isVisible = true
        b.linearOnboardingFavorites.isVisible = false
        b.lottieFavorites.cancelAnimation()
    }

    private fun setupScrollTopFab() {
        binding.fabScrollTop.setOnClickListener {
            binding.rvFavorites.smoothScrollToPosition(0)
        }

        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollTopFab()
            }
        }
        scrollListener?.let { binding.rvFavorites.addOnScrollListener(it) }
        updateScrollTopFab()
    }

    private fun updateScrollTopFab() {
        val b = _binding ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        b.fabScrollTop.isVisible = firstVisible > scrollTopThreshold
    }
}
