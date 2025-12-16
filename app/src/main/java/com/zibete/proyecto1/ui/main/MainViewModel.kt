package com.zibete.proyecto1.ui.main

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Enum para controlar la pantalla actual desde el VM
enum class CurrentScreen {
    CHAT, USERS, GROUPS, EDIT_PROFILE, FAVORITES, OTHER
}

// Eventos de una sola vez (para navegación o diálogos)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userSessionManager: UserSessionManager,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val sessionRepository: SessionRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val myUid = userRepository.myUid

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<MainNavEvent>()
    val navEvents: SharedFlow<MainNavEvent> = _navEvents.asSharedFlow()

    private var installIdListener: ValueEventListener? = null

    init {
        viewModelScope.launch {
            // 1) Sesión activa get & set
            val installId = sessionRepository.getLocalInstallId()
            val fcmToken = sessionRepository.getLocalFcmToken()

            sessionRepository.setActiveSession(
                uid = myUid,
                installId = installId,
                fcmToken = fcmToken
            )

            // 2) Listener de sesión
            checkSessionConflict(myUid, installId)

            // 3) Listeners --> Badges
            userRepository.observeUnreadChats().collect { count ->
                _uiState.update { it.copy(chatBadgeCount = count) }
            }

            // 4) Listeners --> Badges grupos
            if (!userPreferencesRepository.inGroup) {
                groupRepository.observeUnreadGroup().collect { count ->
                    _uiState.update { it.copy(groupBadgeCount = count) }
                }
            }
        }
    }

    // --- LOGICA DE SESIÓN (Install ID) ---
    private fun checkSessionConflict(uid: String, installId: String) {
        installIdListener?.let { sessionRepository.removeSessionListener(uid, it) }

        installIdListener = sessionRepository.observeSessionConflict(
            uid = uid,
            myInstallId = installId
        ) {
            viewModelScope.launch {
                userPreferencesRepository.
                _navEvents.emit(MainNavEvent.ToSplashSessionConflict)
            }
        }
    }

    // --- FUNCIONES DE UI ---

    fun setScreen(screen: CurrentScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun showToolbar(show: Boolean) {
        _uiState.update { it.copy(toolbarVisible = show) }
    }

    fun showLayoutSettings(show: Boolean) {
        _uiState.update { it.copy(layoutSettingsVisible = show) }
    }

    fun showBottomNav(show: Boolean) {
        _uiState.update { it.copy(bottomNavVisible = show) }
    }

    fun setToolbarTitle(title: String) {
        _uiState.update { it.copy(toolbarTitle = title) }
    }

    // --- ACCIONES DE USUARIO (LOGOUT / EXIT GROUP) ---
    fun onLocationChanged(location: Location) {
        viewModelScope.launch {
            locationRepository.updateLocation(location)
        }
    }

//    fun onLogoutConfirmed() {
//        viewModelScope.launch {
//            val intent = userSessionManager.logOutCleanup()
//            _navEvents.emit(MainNavEvent.ToSplashAfterLogout(intent))
//        }
//    }

    fun isOnboardingProfileDone() {
        if (!userPreferencesRepository.firstLoginDone) onEditProfileSelected()
    }

    fun onExitGroupConfirmed() {
        viewModelScope.launch {
            userSessionManager.performExitGroupDataCleanup()
            setScreen(CurrentScreen.GROUPS)
            _navEvents.emit(MainNavEvent.ToGroupsAfterExit)
        }
    }

    fun onBottomItemSelected(itemId: Int) {
        when (itemId) {

            R.id.navBottomUsers -> {

                if (_uiState.value.currentScreen == CurrentScreen.USERS) return

                setScreen(CurrentScreen.USERS)
                showToolbar(true)
                showLayoutSettings(true)
                showBottomNav(true)

                viewModelScope.launch {
                    _navEvents.emit(MainNavEvent.ToUsers)
                }
            }

            R.id.navBottomChat -> {
                onChatTabSelected()
            }

            R.id.navBottomFavorites -> {
                if (_uiState.value.currentScreen == CurrentScreen.FAVORITES) return

                setScreen(CurrentScreen.FAVORITES)
                showToolbar(true)
                showLayoutSettings(false)
                showBottomNav(true)

                viewModelScope.launch {
                    _navEvents.emit(MainNavEvent.ToFavorites)
                }
            }

            R.id.navBottomGrupos -> {
                onGroupsTabSelected()
            }
        }
    }

    fun onChatTabSelected() {
        if (_uiState.value.currentScreen == CurrentScreen.CHAT) return

        setScreen(CurrentScreen.CHAT)
        showToolbar(true)
        showLayoutSettings(false)
        showBottomNav(true)

        viewModelScope.launch {
            _navEvents.emit(MainNavEvent.ToChat)
        }
    }

    fun onGroupsTabSelected() {
        if (_uiState.value.currentScreen == CurrentScreen.GROUPS) return

        setScreen(CurrentScreen.GROUPS)
        showBottomNav(true)
        showLayoutSettings(false)

        viewModelScope.launch {
            if (!userPreferencesRepository.inGroup) {
                _navEvents.emit(MainNavEvent.ToGroupsSelect)
            } else {
                showToolbar(true)
                onGroupJoinConfirmed()
            }
        }
    }

    fun onGroupJoinConfirmed() {
        viewModelScope.launch {
            _navEvents.emit(
                MainNavEvent.ToGroupsDetail(
                    groupName = userPreferencesRepository.groupName,
                    userName = userPreferencesRepository.userNameGroup
                )
            )
        }
    }


    fun onEditProfileSelected() {
        if (_uiState.value.currentScreen == CurrentScreen.EDIT_PROFILE) return

        setScreen(CurrentScreen.EDIT_PROFILE)
        showToolbar(true)
        showBottomNav(false)
        showLayoutSettings(false)

        viewModelScope.launch {
            _navEvents.emit(MainNavEvent.ToEditProfile)
        }
    }

    fun onBackPressed() {
        when (_uiState.value.currentScreen) {

            CurrentScreen.EDIT_PROFILE -> {
                viewModelScope.launch {













                    _navEvents.emit(MainNavEvent.BackFromEditProfile)
                }
            }

            CurrentScreen.CHAT,
            CurrentScreen.USERS,
            CurrentScreen.FAVORITES,
            CurrentScreen.GROUPS-> {
                viewModelScope.launch {
                    _navEvents.emit(MainNavEvent.BackExitAppOrCloseSearch)
                }
            }

            else -> {
                viewModelScope.launch {
                    _navEvents.emit(MainNavEvent.BackToChat)
                }
            }
        }
    }

    fun onSetUserOnline(){
        viewModelScope.launch { userRepository.setUserOnline() }
    }

    fun onSetUserLastSeen(){
        viewModelScope.launch { userRepository.setUserLastSeen() }
    }

    override fun onCleared() {
        super.onCleared()
        installIdListener?.let {
            sessionRepository.removeSessionListener(myUid, it)
        }
    }

    fun onToolbarItemSelected(itemId: Int) {
        when (itemId) {

            R.id.action_settings -> {
                viewModelScope.launch {
                    _navEvents.emit(MainNavEvent.ToSettings)
                }
            }

            R.id.action_favorites -> {
                onBottomItemSelected(R.id.navBottomFavorites)
            }

            R.id.action_exit_group -> {
                viewModelScope.launch {
                    _navEvents.emit(MainNavEvent.ConfirmExitGroup(userPreferencesRepository.groupName))
                }
            }
        }
    }
}
