package com.zibete.proyecto1.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
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

    private data class UsersFilters(
        val applyAgeFilter: Boolean = false,
        val applyOnlineFilter: Boolean = false,
        val minAge: Int = 0,
        val maxAge: Int = 0
    )

    private val myUid: String get() = userRepository.myUid

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UsersUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UsersUiEvent> = _events.asSharedFlow()

    private var allUsers: List<UsersRowUiModel> = emptyList()
    private var currentFilters = UsersFilters()
    private var searchQuery: String = ""

    fun loadUsers() {
        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            currentFilters = readFiltersFromPrefs()

            try {
                val snapshot = firebaseRefsContainer.refAccounts.get().await()

                if (!snapshot.exists()) {
                    allUsers = emptyList()
                    updateVisibleUsers(isLoading = false)
                    return@launch
                }

                val tempList = mutableListOf<UsersRowUiModel>()

                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    if (key == myUid) continue

                    val user = child.getValue(Users::class.java) ?: continue

                    val age = ageCalculator(user.birthDate)
                    val distanceMeters = locationRepository.getDistanceMeters(
                        locationRepository.latitude,
                        locationRepository.longitude,
                        user.latitude,
                        user.longitude
                    )

                    tempList.add(
                        UsersRowUiModel(
                            id = key,
                            name = user.name,
                            age = age,
                            isOnline = user.isOnline,
                            distanceMeters = distanceMeters,
                            photoUrl = user.photoUrl,
                            description = user.description
                        )
                    )
                }

                tempList.sortBy { it.distanceMeters }

                allUsers = tempList
                updateVisibleUsers(isLoading = false)
            } catch (e: Throwable) {
                _events.emit(
                    UsersUiEvent.ShowSnack(
                        uiText = e.message.toUiText(
                            R.string.err_zibe_prefix,
                            R.string.err_zibe
                        ),
                        snackType = ZibeSnackType.ERROR
                    )
                )
                _uiState.update { it.copy(isLoading = false) }
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

            currentFilters = UsersFilters(
                applyAgeFilter = applyAgeFilter,
                applyOnlineFilter = applyOnlineFilter,
                minAge = minAge,
                maxAge = maxAge
            )
            updateVisibleUsers()
        }
    }

    fun clearFilters() {
        viewModelScope.launch {
            userPreferencesActions.setApplyAgeFilter(false)
            userPreferencesActions.setApplyOnlineFilter(false)
            userPreferencesActions.setMinAge(0)
            userPreferencesActions.setMaxAge(0)
            userPreferencesActions.setFilterSwitch(false)

            currentFilters = UsersFilters()
            updateVisibleUsers()
        }
    }

    fun onFilterClicked() {
        viewModelScope.launch {
            val filters = readFiltersFromPrefs()

            _events.emit(
                UsersUiEvent.ShowFilterDialog(
                    applyAgeFilter = filters.applyAgeFilter,
                    applyOnlineFilter = filters.applyOnlineFilter,
                    minAge = filters.minAge,
                    maxAge = filters.maxAge
                )
            )
        }
    }

    fun onUserChatClick(userId: String) {
        viewModelScope.launch {
            _events.emit(UsersUiEvent.NavigateToChat(userId))
        }
    }

    fun onUserProfileClick(userId: String) {
        viewModelScope.launch {
            val list = uiState.value.users
            val position = list.indexOfFirst { it.id == userId }.coerceAtLeast(0)
            val ids = ArrayList(list.map { it.id })

            _events.emit(
                UsersUiEvent.NavigateToProfile(
                    userIds = ids,
                    startIndex = position
                )
            )
        }
    }

    fun onSearchQueryChanged(query: String?) {
        viewModelScope.launch {
            searchQuery = query.orEmpty()
            updateVisibleUsers()
        }
    }

    private suspend fun readFiltersFromPrefs(): UsersFilters = UsersFilters(
        applyAgeFilter = userPreferencesProvider.applyAgeFilterFlow.first(),
        applyOnlineFilter = userPreferencesProvider.applyOnlineFilterFlow.first(),
        minAge = userPreferencesProvider.minAgeFlow.first(),
        maxAge = userPreferencesProvider.maxAgeFlow.first()
    )

    private fun updateVisibleUsers(isLoading: Boolean? = null) {
        val query = searchQuery.trim().lowercase()
        val filters = currentFilters
        val filtered = allUsers.filter { user ->
            if (filters.applyOnlineFilter && !user.isOnline) return@filter false
            if (filters.applyAgeFilter && user.age !in filters.minAge..filters.maxAge) {
                return@filter false
            }
            if (query.isBlank()) return@filter true
            user.name.lowercase().contains(query)
        }

        _uiState.update { state ->
            state.copy(
                isLoading = isLoading ?: state.isLoading,
                users = filtered
            )
        }
    }

    fun formatDistance(meters: Double): String = locationRepository.formatDistance(meters)

}
