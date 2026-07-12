package com.zibete.proyecto1.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.core.utils.runCatchingPreservingCancellation
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.UserDirectoryProvider
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val localRepositoryProvider: LocalRepositoryProvider,
    private val userDirectoryProvider: UserDirectoryProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FavoritesUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FavoritesUiEvent> = _events.asSharedFlow()

    private var allFavorites: List<FavoriteUserUi> = emptyList()
    private var searchQuery: String = ""

    fun loadFavorites() = fetchFavorites(showLoading = true, isRefresh = false)

    fun refreshFavorites() = fetchFavorites(showLoading = false, isRefresh = true)

    private fun fetchFavorites(showLoading: Boolean, isRefresh: Boolean) {
        viewModelScope.launch {
            val shouldShowLoading = showLoading && allFavorites.isEmpty()

            if (shouldShowLoading) _uiState.update { it.copy(isLoading = true) }
            if (isRefresh) _uiState.update { it.copy(isRefreshing = true) }

            runCatchingPreservingCancellation { fetchFavoriteUsers() }
                .onSuccess { result ->
                    allFavorites = result
                    updateVisibleFavorites(isLoading = false, isRefreshing = false)
                }
                .onFailure { e ->
                    onError(
                        e.message.toUiText(
                            R.string.err_zibe_prefix,
                            R.string.err_zibe
                        )
                    )
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            searchQuery = query
            updateVisibleFavorites()
        }
    }

    fun onError(uiText: UiText) {
        _events.tryEmit(
            FavoritesUiEvent.ShowSnack(
                uiText = uiText,
                snackType = ZibeSnackType.ERROR
            )
        )
        updateVisibleFavorites(isLoading = false, isRefreshing = false)
    }

    private suspend fun fetchFavoriteUsers(): List<FavoriteUserUi> {
        val myUid = localRepositoryProvider.myUid

        val favIds = userDirectoryProvider.getFavoriteUserIds(myUid)
        if (favIds.isEmpty()) return emptyList()

        val foundIds = mutableSetOf<String>()
        val favorites = mutableListOf<FavoriteUserUi>()

        userDirectoryProvider.getAllAccounts().forEach { user ->
            val uid = user.id
            if (uid.isBlank() || uid !in favIds) return@forEach
            foundIds += uid
            favorites += FavoriteUserUi(
                id = uid,
                name = user.name,
                age = ageCalculator(user.birthDate),
                profilePhoto = user.photoUrl,
                isOnline = user.online
            )
        }

        val missingIds = favIds.filterNot { it in foundIds }
        if (missingIds.isNotEmpty()) {
            userDirectoryProvider.removeFavoriteUserIds(myUid, missingIds)
        }

        return favorites.sortedBy { it.name.lowercase() }
    }

    private fun updateVisibleFavorites(
        isLoading: Boolean? = null,
        isRefreshing: Boolean? = null
    ) {
        val query = searchQuery.trim()

        val filtered = if (query.isBlank()) allFavorites
        else allFavorites.filter { it.name.contains(query, ignoreCase = true) }

        _uiState.update { state ->
            state.copy(
                isLoading = isLoading ?: state.isLoading,
                isRefreshing = isRefreshing ?: state.isRefreshing,
                favorites = allFavorites,
                filteredFavorites = filtered.toList(),
                showOnboarding = allFavorites.isEmpty(),
                searchQuery = searchQuery
            )
        }
    }
}
