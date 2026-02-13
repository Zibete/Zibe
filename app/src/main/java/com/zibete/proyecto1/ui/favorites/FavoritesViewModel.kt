package com.zibete.proyecto1.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.NODE_FAVORITE_LIST
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Users
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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer
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
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true) }
            }
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            }

            runCatching { fetchFavoriteUsers() }
                .onSuccess { result ->
                    allFavorites = result
                    updateVisibleFavorites(isLoading = false, isRefreshing = false)
                }
                .onFailure { t ->
                    onError(
                        t.message.toUiText(
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
        allFavorites = emptyList()
        updateVisibleFavorites(isLoading = false, isRefreshing = false)
    }

    private suspend fun fetchFavoriteUsers(): List<FavoriteUserUi> {
        val myUid = userRepository.myUid

        val favListSnap = firebaseRefsContainer.refData
            .child(myUid)
            .child(NODE_FAVORITE_LIST)
            .get()
            .await()

        if (!favListSnap.exists() || favListSnap.childrenCount == 0L) {
            return emptyList()
        }

        val favIds = favListSnap.children
            .mapNotNull { it.getValue(String::class.java) }
            .filter { it.isNotBlank() }
            .distinct()

        return favIds.mapNotNull { favUserId ->
            val userRef = firebaseRefsContainer.refAccounts.child(favUserId)

            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                favListSnap.ref.child(favUserId).removeValue().await()
                null
            } else {
                val u = userSnap.getValue(Users::class.java) ?: return@mapNotNull null

                FavoriteUserUi(
                    id = favUserId,
                    name = u.name,
                    age = ageCalculator(u.birthDate),
                    profilePhoto = u.photoUrl,
                    isOnline = u.isOnline
                )
            }
        }.sortedBy { it.name.lowercase() }
    }

    private fun updateVisibleFavorites(
        isLoading: Boolean? = null,
        isRefreshing: Boolean? = null
    ) {
        val query = searchQuery.trim()
        val filtered = if (query.isBlank()) {
            allFavorites
        } else {
            allFavorites.filter { it.name.contains(query, ignoreCase = true) }
        }

        _uiState.update { state ->
            state.copy(
                isLoading = isLoading ?: state.isLoading,
                isRefreshing = isRefreshing ?: state.isRefreshing,
                favorites = allFavorites,
                filteredFavorites = filtered,
                showOnboarding = allFavorites.isEmpty(),
                searchQuery = searchQuery
            )
        }
    }
}
