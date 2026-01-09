package com.zibete.proyecto1.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Users
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
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val locationRepository: LocationRepository,
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
            val applyAgeFilter = userPreferencesProvider.applyAgeFilterFlow.first()
            val applyOnlineFilter = userPreferencesProvider.applyOnlineFilterFlow.first()
            val minAge = userPreferencesProvider.minAgeFlow.first()
            val maxAge = userPreferencesProvider.maxAgeFlow.first()

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

                    user.age = ageCalculator(user.birthDate)

                    user.distanceMeters = locationRepository.getDistanceMeters(
                        locationRepository.latitude,
                        locationRepository.longitude,
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
            userPreferencesActions.setApplyAgeFilter(applyAgeFilter)
            userPreferencesActions.setApplyOnlineFilter(applyOnlineFilter)

            if (applyAgeFilter) {
                userPreferencesActions.setMinAge(minAge)
                userPreferencesActions.setMaxAge(maxAge)
                userPreferencesActions.setFilterSwitch(true)
            } else {
                userPreferencesActions.setMinAge(0)
                userPreferencesActions.setMaxAge(0)
                userPreferencesActions.setFilterSwitch(applyOnlineFilter) // si online queda activo, sigue habiendo filtro
            }

            loadUsers()
        }
    }

    fun clearFilters() {
        viewModelScope.launch {
            userPreferencesActions.setApplyAgeFilter(false)
            userPreferencesActions.setApplyOnlineFilter(false)
            userPreferencesActions.setMinAge(0)
            userPreferencesActions.setMaxAge(0)
            userPreferencesActions.setFilterSwitch(false)

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
        return userPreferencesProvider.applyAgeFilterFlow.first()
    }

    suspend fun currentApplyOnlineFilter(): Boolean {
        return userPreferencesProvider.applyOnlineFilterFlow.first()
    }

    fun formatDistance(meters: Double): String =
        locationRepository.formatDistance(meters)

}
