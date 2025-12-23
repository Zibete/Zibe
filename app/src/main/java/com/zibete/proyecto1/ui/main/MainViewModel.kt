package com.zibete.proyecto1.ui.main

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.PresenceRepository
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Enum para controlar la pantalla actual desde el VM
enum class CurrentScreen {
    CHAT, USERS, GROUPS, EDIT_PROFILE, FAVORITES, OTHER
}



@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userSessionManager: UserSessionManager,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val sessionRepository: SessionRepository,
    private val locationRepository: LocationRepository,
    private val presenceRepository: PresenceRepository
) : ViewModel() {

    private val myUid: String get() = userRepository.myUid

    val groupContext: StateFlow<GroupContext?> =
        userPreferencesRepository.groupContextFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<MainNavEvent>()
    val navEvents: SharedFlow<MainNavEvent> = _navEvents.asSharedFlow()

    private var installIdListener: ValueEventListener? = null

//    val groupBadgeCount: StateFlow<Int> =
//        uiState.map { it.groupBadgeCount }
//            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
//
//    val groupTabUnreadCount: StateFlow<Int> =
//        uiState.map { it.groupTabUnreadCount }
//            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)


    fun startPresence(){
        viewModelScope.launch {
            presenceRepository.startPresence()
        }
    }

    init {

        // 1) Setup sesión (una vez)
        viewModelScope.launch {
            val installId = sessionRepository.getLocalInstallId()
            val fcmToken = sessionRepository.getLocalFcmToken()

            sessionRepository.setActiveSession(
                uid = myUid,
                installId = installId,
                fcmToken = fcmToken
            )

            checkSessionConflict(myUid, installId)
        }

        // 2) Badge chats (siempre)
        viewModelScope.launch {
            userRepository.observeUnreadChatList()
                .collect { count ->
                    _uiState.update { it.copy(chatListBadgeCount = count) }
                }
        }

        // 3)  Badge grupos (siempre)
        observeGroupBadges()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeGroupBadges() {

        // Badge bottom nav = (unread chat grupo) + (unread privados dentro de grupo)
        viewModelScope.launch {
            userPreferencesRepository.groupContextFlow
                .flatMapLatest { ctx ->
                    if (ctx == null) flowOf(0)
                    else groupRepository.unreadGroupBadgeCount(ctx.groupName)
                }
                .distinctUntilChanged()
                .collect { count ->
                    _uiState.update { it.copy(groupBadgeCount = count) }
                }
        }

        // Badge tab chat de grupo
        viewModelScope.launch {
            userPreferencesRepository.groupContextFlow
                .flatMapLatest { ctx ->
                    if (ctx == null) flowOf(0)
                    else groupRepository.observeUnreadGroupChat(ctx.groupName)
                }
                .distinctUntilChanged()
                .collect { count ->
                    _uiState.update { it.copy(unreadGroupChatCount = count) }
                }
        }

        // Badge privados dentro del grupo
        viewModelScope.launch {
            userPreferencesRepository.groupContextFlow
                .flatMapLatest { ctx ->
                    if (ctx == null) flowOf(0)
                    else groupRepository.observeUnreadPrivateMessages()
                    // ⚠️ Si querés que sea “solo del grupo actual”, tu repo tiene que filtrar por groupName.
                }
                .distinctUntilChanged()
                .collect { count ->
                    _uiState.update { it.copy(unreadPrivateMessagesCount = count) }
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
                _navEvents.emit(MainNavEvent.ToSplashSessionConflict)
            }
        }
    }

    // --- FUNCIONES DE UI ---

    fun emit(event: MainNavEvent) {
        viewModelScope.launch { _navEvents.emit(event) }
    }

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

    fun onLogoutConfirmed() {
        viewModelScope.launch {
            userRepository.setUserLastSeen()
            val intent = userSessionManager.logOutCleanup()
            _navEvents.emit(MainNavEvent.ToSplashAfterLogout(intent))
        }
    }

    fun checkFirstLogin() {
        viewModelScope.launch {
            val done = userPreferencesRepository.getFirstLoginDone()
            if (!done) onEditProfileSelected()
        }
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
            groupContext.collect { groupContext ->
                val inGroup = groupContext?.inGroup ?: false
                if (!inGroup) {
                    toGroupsSelect()
                } else {
                    showToolbar(true)
                    toGroupHost()
                }
            }
        }
    }

    fun toGroupHost(){
        viewModelScope.launch {
            _navEvents.emit(MainNavEvent.ToGroupHost)
        }
    }

    fun toGroupsSelect(){
        viewModelScope.launch {
            _navEvents.emit(MainNavEvent.ToGroupsSelect)
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
                    _navEvents.emit(MainNavEvent.BackFromEditProfile())
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
                    _navEvents.emit(MainNavEvent.ConfirmExitGroup)
                }
            }
        }
    }

    val groupName: StateFlow<String> =
        userPreferencesRepository.groupContextFlow
            .map { it?.groupName.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val hasActiveFilter: StateFlow<Boolean> =
        userPreferencesRepository.filterSwitchFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)


    fun myDisplayName(): String = userRepository.myUserName
    fun myEmail(): String = userRepository.myEmail
    fun myPhotoUrl(): String = userRepository.myUserName

}
