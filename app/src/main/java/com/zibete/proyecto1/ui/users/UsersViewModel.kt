package com.zibete.proyecto1.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
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
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class UsersViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val myUid = userRepository.myUid

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<UsersUiEvent>()
    val events: SharedFlow<UsersUiEvent> = _events.asSharedFlow()

    private val _filterActive = MutableStateFlow(userPreferencesRepository.filterSwitch)
    val filterActive: StateFlow<Boolean> = _filterActive

//    private val _events = MutableSharedFlow<UsersUiEvent>()
//    val events: SharedFlow<UsersUiEvent> = _events.asSharedFlow()

    fun loadUsers(isRefresh: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // Ojo: usá tu ref real, dejo nombre genérico
        firebaseRefsContainer.refCuentas
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        _uiState.value = UsersUiState(
                            isLoading = false,
                            users = emptyList(),
                            errorMessage = "No existen usuarios"
                        )
                        return
                    }

                    val tempList = mutableListOf<Users>()

                    val applyAgeFilter = userPreferencesRepository.applyAgeFilter
                    val applyOnlineFilter = userPreferencesRepository.applyOnlineFilter
                    val minAge = userPreferencesRepository.minAgePref
                    val maxAge = userPreferencesRepository.maxAgePref

                    for (child in snapshot.children) {
                        val key = child.key ?: continue
                        if (key == myUid) continue

                        val user = child.getValue(Users::class.java) ?: continue

                        user.age = Utils.calcAge(user.birthDay)

                        user.distanceMeters = locationRepository.getDistanceMeters(
                            userRepository.latitude,
                            userRepository.longitude,
                            user.latitude,
                            user.longitude
                        )

                        var isValid = true

                        if (applyOnlineFilter && !user.state) {
                            isValid = false
                        }

                        if (isValid && applyAgeFilter) {
                            if (user.age !in minAge..maxAge) {
                                isValid = false
                            }
                        }

                        if (isValid) {
                            tempList.add(user)
                        }
                    }

                    tempList.sort()

                    _uiState.value = UsersUiState(
                        isLoading = false,
                        users = tempList,
                        errorMessage = null
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    _uiState.value = UsersUiState(
                        isLoading = false,
                        users = emptyList(),
                        errorMessage = error.message
                    )
                }
            })
    }

    fun applyFilters(
        applyAgeFilter: Boolean,
        applyOnlineFilter: Boolean,
        minAge: Int,
        maxAge: Int
    ) {
        userPreferencesRepository.applyAgeFilter = applyAgeFilter
        userPreferencesRepository.applyOnlineFilter = applyOnlineFilter

        if (applyAgeFilter) {
            userPreferencesRepository.minAgePref = minAge
            userPreferencesRepository.maxAgePref = maxAge
        } else {
            userPreferencesRepository.minAgePref = 0
            userPreferencesRepository.maxAgePref = 0
        }

        loadUsers(isRefresh = false)
    }

    fun clearFilters() {
        userPreferencesRepository.applyAgeFilter = false
        userPreferencesRepository.applyOnlineFilter = false
        userPreferencesRepository.minAgePref = 0
        userPreferencesRepository.maxAgePref = 0

        loadUsers(isRefresh = false)
    }

    fun onFilterClicked() {
        viewModelScope.launch {
            _events.emit(UsersUiEvent.ShowFilterDialog)
        }
    }



}