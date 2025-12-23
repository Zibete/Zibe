package com.zibete.proyecto1.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.constants.Constants.NODE_FAVORITE_LIST
import com.zibete.proyecto1.ui.constants.ERR_ZIBE
import com.zibete.proyecto1.utils.TimeUtils.ageCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer
) : ViewModel() {

    private val myUid: String get() = userRepository.myUid

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState

    private val _events = MutableSharedFlow<FavoritesUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FavoritesUiEvent> = _events

    fun loadFavorites() = fetchFavorites(showLoading = true)

    fun refreshFavorites() = fetchFavorites(showLoading = false)

    private fun fetchFavorites(showLoading: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = showLoading) }

            if (showLoading) {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                val favListSnap = firebaseRefsContainer.refData
                    .child(myUid)
                    .child(NODE_FAVORITE_LIST)
                    .get()
                    .await()

                if (!favListSnap.exists() || favListSnap.childrenCount == 0L) {
                    _uiState.update { it.copy(isLoading = false, favorites = emptyList()) }
                    showEmptyFavoritesMessage()
                    return@launch
                }

                val favIds = favListSnap.children
                    .mapNotNull { it.getValue(String::class.java) }
                    .filter { it.isNotBlank() }
                    .distinct()

                val result = favIds.mapNotNull { favUserId ->
                    val userRef = firebaseRefsContainer.refAccounts.child(favUserId)

                    val userSnap = userRef.get().await()
                    if (!userSnap.exists()) {
                        // Limpieza: usuario ya no existe -> borrar de favoritos
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

                _uiState.update { it.copy(isLoading = false, favorites = result) }

                if (result.isEmpty()) showEmptyFavoritesMessage()

            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, favorites = emptyList()) }
                _events.emit(FavoritesUiEvent.ShowMessage(
                    t.message ?: ERR_ZIBE
                )
                )
            }
        }
    }

    fun showEmptyFavoritesMessage() {
        viewModelScope.launch {
            _events.emit(FavoritesUiEvent.ShowMessage("Aún no hay favoritos"))
        }
    }
}
