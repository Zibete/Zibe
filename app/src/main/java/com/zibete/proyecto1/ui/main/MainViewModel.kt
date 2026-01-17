package com.zibete.proyecto1.ui.main

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.PresenceRepository
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.domain.session.DefaultLogoutUseCase
import com.zibete.proyecto1.domain.session.ExitGroupUseCase
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val exitGroupUseCase: ExitGroupUseCase,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val sessionRepository: SessionRepository,
    private val locationRepository: LocationRepository,
    private val presenceRepository: PresenceRepository,
    private val logoutUseCase: DefaultLogoutUseCase,
    private val userPreferencesProvider: UserPreferencesProvider,
) : ViewModel() {

    private val myUid: String get() = userRepository.myUid

    val groupContext: StateFlow<GroupContext?> =
        userPreferencesProvider.groupContextFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val _toolbarState = MutableStateFlow(ToolbarState())
    val toolbarState = _toolbarState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<MainUiEvent>()
    val uiEvents: SharedFlow<MainUiEvent> = _uiEvents.asSharedFlow()

    private var installIdListener: ValueEventListener? = null

    fun startPresence() {
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
            userPreferencesProvider.groupContextFlow
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
            userPreferencesProvider.groupContextFlow
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
            userPreferencesProvider.groupContextFlow
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
                _uiEvents.emit(MainUiEvent.NavigateToSplash(sessionConflict = true))
            }
        }
    }

    // --- FUNCIONES DE UI ---

    fun emit(event: MainUiEvent) {
        viewModelScope.launch { _uiEvents.emit(event) }
    }

    fun setScreen(screen: CurrentScreen) {
        _toolbarState.update { it.copy(currentScreen = screen) }
    }

    fun showToolbar(show: Boolean) {
        _toolbarState.update { it.copy(showToolbar = show) }
    }

    fun showLayoutSettings(show: Boolean) {
        _toolbarState.update { it.copy(showUsersFragmentSettings = show) }
    }

    fun showBottomNav(show: Boolean) {
        _toolbarState.update { it.copy(showBottomNav = show) }
    }

    fun showSkipButton(show: Boolean) {
        _toolbarState.update { it.copy(showSkipButton = show) }
    }

    // --- ACCIONES DE USUARIO (LOGOUT / EXIT GROUP) ---
    fun onLocationChanged(location: Location) {
        viewModelScope.launch {
            locationRepository.updateLocation(location)
        }
    }

    val showSkipButton = _toolbarState.value.showSkipButton

    fun onLogoutConfirmed() {
        viewModelScope.launch {
            logoutUseCase.execute()
            _uiEvents.emit(MainUiEvent.NavigateToSplash())
        }
    }

    fun checkFirstLoginDone() {
        viewModelScope.launch {
            val done = userPreferencesProvider.isFirstLoginDone()
            if (!done) onEditProfileSelected()
        }
    }

    fun onExitGroupConfirmed(message: String) {
        viewModelScope.launch {
            exitGroupUseCase.performExitGroupDataCleanup(message)
                .onSuccess {
                    setScreen(CurrentScreen.GROUPS)
                    _uiEvents.emit(MainUiEvent.ToGroupsAfterExit)
                }
                .onFailure { e ->
                    UiText.StringRes(
                        R.string.err_zibe_prefix,
                        args = listOf(e.message ?: "")
                    )
                }
        }
    }

    suspend fun onError(message: UiText) {
        _uiEvents.emit(
            MainUiEvent.ShowSnack(
                uiText = message,
                type = ZibeSnackType.ERROR
            )
        )
    }

    fun onBottomItemSelected(itemId: Int) {
        when (itemId) {

            R.id.navBottomUsers -> {

                if (_toolbarState.value.currentScreen == CurrentScreen.USERS) return

                setScreen(CurrentScreen.USERS)
                showToolbar(true)
                showLayoutSettings(true)
                showBottomNav(true)

                viewModelScope.launch {
                    _uiEvents.emit(MainUiEvent.ToUsers)
                }
            }

            R.id.navBottomChat -> {
                onChatTabSelected()
            }

            R.id.navBottomFavorites -> {
                if (_toolbarState.value.currentScreen == CurrentScreen.FAVORITES) return

                setScreen(CurrentScreen.FAVORITES)
                showToolbar(true)
                showLayoutSettings(false)
                showBottomNav(true)

                viewModelScope.launch {
                    _uiEvents.emit(MainUiEvent.ToFavorites)
                }
            }

            R.id.navBottomGrupos -> {
                onGroupsTabSelected()
            }
        }
    }

    fun onChatTabSelected() {
        if (_toolbarState.value.currentScreen == CurrentScreen.CHAT) return

        setScreen(CurrentScreen.CHAT)
        showToolbar(true)
        showLayoutSettings(false)
        showBottomNav(true)

        viewModelScope.launch {
            _uiEvents.emit(MainUiEvent.ToChat)
        }
    }

    fun onGroupsTabSelected() {
        if (_toolbarState.value.currentScreen == CurrentScreen.GROUPS) return

        setScreen(CurrentScreen.GROUPS)
        showBottomNav(true)
        showLayoutSettings(false)

        viewModelScope.launch {
            val ctx = groupContext.value
            val inGroup = ctx?.inGroup ?: false
            if (!inGroup) toGroupsSelect() else {
                showToolbar(true); toGroupHost()
            }
        }

    }

    fun toGroupHost() {
        viewModelScope.launch {
            _uiEvents.emit(MainUiEvent.ToGroupHost)
        }
    }

    fun toGroupsSelect() {
        viewModelScope.launch {
            _uiEvents.emit(MainUiEvent.ToGroupsSelect)
        }
    }

    fun onEditProfileSelected() {
        if (_toolbarState.value.currentScreen == CurrentScreen.EDIT_PROFILE) return

        setScreen(CurrentScreen.EDIT_PROFILE)
        showToolbar(true)
        showBottomNav(false)
        showLayoutSettings(false)

        viewModelScope.launch {
            _uiEvents.emit(MainUiEvent.ToEditProfile)
        }
    }

    fun onBackPressed() {
        when (_toolbarState.value.currentScreen) {

            CurrentScreen.EDIT_PROFILE -> {
                viewModelScope.launch {
                    _uiEvents.emit(MainUiEvent.BackFromEditProfile)
                }
            }

            CurrentScreen.CHAT,
            CurrentScreen.USERS,
            CurrentScreen.FAVORITES,
            CurrentScreen.GROUPS -> {
                viewModelScope.launch {
                    _uiEvents.emit(MainUiEvent.BackExitAppOrCloseSearch)
                }
            }

            else -> {
                viewModelScope.launch {
                    _uiEvents.emit(MainUiEvent.BackToChat)
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
                    _uiEvents.emit(MainUiEvent.ToSettings)
                }
            }

            R.id.action_favorites -> {
                onBottomItemSelected(R.id.navBottomFavorites)
            }

            R.id.action_exit_group -> {
                viewModelScope.launch {
                    _uiEvents.emit(MainUiEvent.ConfirmExitGroup)
                }
            }

            R.id.action_skip -> {
                viewModelScope.launch {
                    _uiEvents.emit(MainUiEvent.BackFromEditProfile)
                }
            }
        }
    }

    fun setToolbarState(
        showToolbar: Boolean,
        showBack: Boolean,
        showUsersFragmentSettings: Boolean,
        showBottomNav: Boolean,
        currentScreen: CurrentScreen,
        showSkipButton: Boolean = false
    ) {
        _toolbarState.update {
            it.copy(
                showToolbar = showToolbar,
                showBack = showBack,
                showUsersFragmentSettings = showUsersFragmentSettings,
                showBottomNav = showBottomNav,
                currentScreen = currentScreen,
                showSkipButton = showSkipButton
            )
        }
    }

    val groupName: StateFlow<String> =
        userPreferencesProvider.groupContextFlow
            .map { it?.groupName.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val hasActiveFilter: StateFlow<Boolean> =
        userPreferencesProvider.filterSwitchFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)


    fun myDisplayName(): String = userRepository.myUserName
    fun myEmail(): String = userRepository.myEmail
    fun myPhotoUrl(): String = userRepository.myUserName

}
