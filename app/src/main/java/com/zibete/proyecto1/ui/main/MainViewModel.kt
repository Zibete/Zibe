package com.zibete.proyecto1.ui.main

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.PresenceRepository
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.domain.session.DefaultLogoutUseCase
import com.zibete.proyecto1.domain.session.ExitGroupUseCase
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
    private val locationRepository: LocationRepository,
    private val presenceRepository: PresenceRepository,
    private val logoutUseCase: DefaultLogoutUseCase,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val snackBarManager: SnackBarManager
) : ViewModel() {

    val groupContext: StateFlow<GroupContext?> =
        userPreferencesProvider.groupContextFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val groupName: StateFlow<String> =
        userPreferencesProvider.groupContextFlow
            .map { it?.groupName.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val hasActiveFilter: StateFlow<Boolean> =
        userPreferencesProvider.filterSwitchFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val _toolbarState = MutableStateFlow(ToolbarState())
    val toolbarState = _toolbarState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<MainUiEvent>()
    val uiEvents: SharedFlow<MainUiEvent> = _uiEvents.asSharedFlow()

    fun startPresence() {
        viewModelScope.launch {
            presenceRepository.startPresence()
        }
    }

    init {

        // 1) Badge chats
        viewModelScope.launch {
            userRepository.observeUnreadChatList()
                .collect { count ->
                    _uiState.update { it.copy(chatListBadgeCount = count) }
                }
        }

        // 2) Badge grupos
        observeGroupBadges()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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

//    fun showLayoutSettings(show: Boolean) {
//        _toolbarState.update { it.copy(showUsersFragmentSettings = show) }
//    }
//
//    fun showBottomNav(show: Boolean) {
//        _toolbarState.update { it.copy(showBottomNav = show) }
//    }
//
//    fun showSkipButton(show: Boolean) {
//        _toolbarState.update { it.copy(showSkipButton = show) }
//    }

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
                    _uiEvents.emit(MainUiEvent.ToGroupsAfterExit)
                }
                .onFailure { e ->
                    val uiText = e.message.toUiText(
                        R.string.err_zibe_prefix,
                        R.string.err_zibe
                    )
                    showSnack(
                        uiText = uiText,
                        snackType = ZibeSnackType.ERROR
                    )
                }
        }
    }

    fun showSnack(
        uiText: UiText,
        snackType: ZibeSnackType
    ) {
        snackBarManager.show(
            uiText = uiText,
            type = snackType
        )
    }

    fun showPendingSnack(
        uiText: UiText,
        snackType: ZibeSnackType
    ) {
        viewModelScope.launch {
            delay(1000L)
            showSnack(uiText, snackType)
        }
    }

    fun onBottomItemSelected(itemId: Int) {
        when (itemId) {
            R.id.navBottomUsers -> onUsersTabSelected()
            R.id.navBottomChat -> onChatTabSelected()
            R.id.navBottomFavorites -> onFavoritesTabSelected()
            R.id.navBottomGroups -> onGroupsTabSelected()
        }
    }

    fun onUsersTabSelected() {
        if (_toolbarState.value.currentScreen == CurrentScreen.USERS) return
        viewModelScope.launch { toUsers() }
    }

    fun onChatTabSelected() {
        if (_toolbarState.value.currentScreen == CurrentScreen.CHAT) return
        viewModelScope.launch { toChat() }
    }

    fun onFavoritesTabSelected() {
        if (_toolbarState.value.currentScreen == CurrentScreen.FAVORITES) return
        viewModelScope.launch { toFavorites() }
    }

    fun onGroupsTabSelected() {
        viewModelScope.launch {
            val ctx = groupContext.value
            val inGroup = ctx?.inGroup ?: false
            if (!inGroup) toGroupsSelect() else toGroupHost()
        }
    }

    fun onEditProfileSelected() {
        if (_toolbarState.value.currentScreen == CurrentScreen.EDIT_PROFILE) return
        viewModelScope.launch { toEditProfile() }
    }

    suspend fun toEditProfile() {
        _uiEvents.emit(MainUiEvent.ToEditProfile)
    }

    suspend fun toGroupHost() {
        _uiEvents.emit(MainUiEvent.ToGroupHost)
    }

    suspend fun toGroupsSelect() {
        _uiEvents.emit(MainUiEvent.ToGroupsSelect)
    }

    suspend fun toFavorites() {
        _uiEvents.emit(MainUiEvent.ToFavorites)
    }

    suspend fun toChat() {
        _uiEvents.emit(MainUiEvent.ToChat)
    }

    suspend fun toUsers() {
        _uiEvents.emit(MainUiEvent.ToUsers)
    }

    fun onBackPressed() {
        when (_toolbarState.value.currentScreen) {

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

    fun onToolbarItemSelected(itemId: Int) {
        when (itemId) {

            R.id.action_settings -> {
                viewModelScope.launch {
                    _uiEvents.emit(MainUiEvent.NavigateToSettings)
                }
            }

            R.id.action_favorites -> onBottomItemSelected(R.id.navBottomFavorites)

            R.id.action_exit_group -> {
                viewModelScope.launch {
                    _uiEvents.emit(MainUiEvent.ConfirmExitGroup)
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
        showSkipButton: Boolean
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

    fun myDisplayName(): String = userRepository.myUserName
    fun myEmail(): String = userRepository.myEmail
    fun myPhotoUrl(): String = userRepository.myUserName

}
