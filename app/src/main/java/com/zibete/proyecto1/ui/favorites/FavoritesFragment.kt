package com.zibete.proyecto1.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.zibete.proyecto1.ui.profile.ProfileActivity
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterFavoriteUsers
import com.zibete.proyecto1.databinding.FragmentFavoritesBinding
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_USER_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels()

    private lateinit var adapter: AdapterFavoriteUsers
    private lateinit var layoutManager: GridLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        setupRecycler()
        setupSwipeRefresh()
        collectUiState()

        viewModel.loadFavorites()

        return binding.root
    }

    private fun setupRecycler() = with(binding) {
        layoutManager = GridLayoutManager(requireContext(), 3)
        rvFavorites.layoutManager = layoutManager

        adapter = AdapterFavoriteUsers(
            favorites = mutableListOf(),
            context = requireContext()
        ) { favoriteUser ->
            // Navegar al perfil
            startActivity(
                Intent(requireContext(), ProfileActivity::class.java)
                    .putExtra(EXTRA_USER_ID, favoriteUser.id)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }

        rvFavorites.adapter = adapter
    }

    private fun setupSwipeRefresh() = with(binding) {
        favoriteSwipeRefresh.setRecyclerView(rvFavorites)
        favoriteSwipeRefresh.setOnRefreshListener {
            viewModel.refreshFavorites()
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect

                    b.progressbar.isVisible = state.isLoading
                    b.favoriteSwipeRefresh.isRefreshing = false

                    if (state.error != null) {
                        Toast.makeText(requireContext(), state.error, Toast.LENGTH_SHORT).show()
                    }

                    adapter.updateDataUsers(state.favorites)

                    if (state.isEmpty) {
                        Toast.makeText(requireContext(), "No hay favoritos", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.action_search)?.isVisible = false
        menu.findItem(R.id.action_unblock)?.isVisible = false
        menu.findItem(R.id.action_favorites)?.isVisible = false
        menu.findItem(R.id.action_exit_group)?.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
