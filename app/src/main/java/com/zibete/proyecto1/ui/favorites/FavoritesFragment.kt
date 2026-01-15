package com.zibete.proyecto1.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterFavoriteUsers
import com.zibete.proyecto1.databinding.FragmentFavoritesBinding
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.ui.profile.ProfileActivity
import com.zibete.proyecto1.ui.search.SearchHandler
import com.zibete.proyecto1.core.utils.UserMessageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : BaseChatSessionFragment(), SearchHandler {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val favoritesViewModel: FavoritesViewModel by viewModels()

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

        setupOptionMenu()

        setupRecyclerView()
        setupSwipeRefresh()
        collectUiState()
        collectEvents()

        favoritesViewModel.loadFavorites()
    }

    private fun setupOptionMenu() {
        val menuHost = requireActivity() as MenuHost

        menuHost.addMenuProvider(
            object : MenuProvider {

                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // No-op: el menú lo infla BaseEdgeToEdgeActivity
                }

                override fun onPrepareMenu(menu: Menu) {
                    menu.findItem(R.id.action_settings)?.isVisible = true
                    menu.findItem(R.id.action_search)?.isVisible = true
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
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
                    b.progressbar.isVisible = state.isLoading
                    b.favoriteSwipeRefresh.isRefreshing = false
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
                            UserMessageUtils.showSnack(
                                root = binding.root,
                                message = event.uiText.asString(requireContext())
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
