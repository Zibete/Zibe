package com.zibete.proyecto1.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserPreferencesDSRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val userPreferencesDSRepository: UserPreferencesDSRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    val locationRepository: LocationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val myUid: String get() = userRepository.myUid

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UsersUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UsersUiEvent> = _events.asSharedFlow()

    fun loadUsers() {
        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            // Leer filtros una sola vez (snapshot) para esta carga
            val applyAgeFilter = userPreferencesDSRepository.applyAgeFilterFlow.first()
            val applyOnlineFilter = userPreferencesDSRepository.applyOnlineFilterFlow.first()
            val minAge = userPreferencesDSRepository.minAgeFlow.first()
            val maxAge = userPreferencesDSRepository.maxAgeFlow.first()

            try {
                val snapshot = firebaseRefsContainer.refAccounts.get().await()

                if (!snapshot.exists()) {
                    _uiState.value = UsersUiState(isLoading = false, users = emptyList())
                    return@launch
                }

                val tempList = mutableListOf<Users>()

                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    if (key == myUid) continue

                    val user = child.getValue(Users::class.java) ?: continue

                    user.age = Utils.calcAge(user.birthDate)

                    user.distanceMeters = locationRepository.getDistanceMeters(
                        userRepository.latitude,
                        userRepository.longitude,
                        user.latitude,
                        user.longitude
                    )

                    var isValid = true

                    if (applyOnlineFilter && !user.isOnline) isValid = false

                    if (isValid && applyAgeFilter) {
                        if (user.age !in minAge..maxAge) isValid = false
                    }

                    if (isValid) tempList.add(user)
                }

                tempList.sort()

                _uiState.value = UsersUiState(
                    isLoading = false,
                    users = tempList
                )
            } catch (_: Throwable) {
                _uiState.value = UsersUiState(
                    isLoading = false,
                    users = emptyList()
                )
            }
        }
    }

    fun applyFilters(
        applyAgeFilter: Boolean,
        applyOnlineFilter: Boolean,
        minAge: Int,
        maxAge: Int
    ) {
        viewModelScope.launch {
            userPreferencesDSRepository.setApplyAgeFilter(applyAgeFilter)
            userPreferencesDSRepository.setApplyOnlineFilter(applyOnlineFilter)

            if (applyAgeFilter) {
                userPreferencesDSRepository.setMinAge(minAge)
                userPreferencesDSRepository.setMaxAge(maxAge)
                userPreferencesDSRepository.setFilterSwitch(true)
            } else {
                userPreferencesDSRepository.setMinAge(0)
                userPreferencesDSRepository.setMaxAge(0)
                userPreferencesDSRepository.setFilterSwitch(applyOnlineFilter) // si online queda activo, sigue habiendo filtro
            }

            loadUsers()
        }
    }

    fun clearFilters() {
        viewModelScope.launch {
            userPreferencesDSRepository.setApplyAgeFilter(false)
            userPreferencesDSRepository.setApplyOnlineFilter(false)
            userPreferencesDSRepository.setMinAge(0)
            userPreferencesDSRepository.setMaxAge(0)
            userPreferencesDSRepository.setFilterSwitch(false)

            loadUsers()
        }
    }

    fun onFilterClicked() {
        viewModelScope.launch {
            _events.emit(UsersUiEvent.ShowFilterDialog)
        }
    }

    // Helpers para UI actual (si tu fragment lo pide)
    suspend fun currentApplyAgeFilter(): Boolean {
        return userPreferencesDSRepository.applyAgeFilterFlow.first()
    }

    suspend fun currentApplyOnlineFilter(): Boolean {
        return userPreferencesDSRepository.applyOnlineFilterFlow.first()
    }

}
