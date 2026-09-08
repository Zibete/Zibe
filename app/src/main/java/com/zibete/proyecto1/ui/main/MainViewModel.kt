package com.zibete.proyecto1.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_DEFAULT_DM
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.di.SettingsConfig
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.core.utils.runCatchingPreservingCancellation
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.ConversationOverviewRepository
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.LocationRepositoryActions
import com.zibete.proyecto1.data.PresenceRepositoryActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.domain.roomsv2.ObserveRoomsV2UnreadSummaryUseCase
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.domain.session.ExitGroupUseCase
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.main.chrome.CurrentScreen
import com.zibete.proyecto1.ui.main.chrome.MainDestinationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val exitGroupUseCase: ExitGroupUseCase,
    private val localRepositoryProvider: LocalRepositoryProvider,
    private val conversationOverviewRepository: ConversationOverviewRepository,
    private val observeRoomsV2UnreadSummary: ObserveRoomsV2UnreadSummaryUseCase,
    private val locationRepository: LocationRepositoryActions,
    private val presenceRepository: PresenceRepositoryActions,
    private val logoutUseCase: LogoutUseCase,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val profileRepositoryProvider: ProfileRepositoryProvider,
    private val profileRepositoryActions: ProfileRepositoryActions,
    private val appNavigator: AppNavigator,
    private val config: SettingsConfig,
) : ViewModel() {

    val groupContext: StateFlow<GroupContext?> =
        userPreferencesProvider.groupContextFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val groupName: StateFlow<String> =
        userPreferencesProvider.groupContextFlow
            .map { it?.groupName.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val _destinationUiState = MutableStateFlow(MainDestinationUiState())
    val destinationUiState = _destinationUiState.asStateFlow()

    private val _uiEvents = Channel<MainUiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<MainUiEvent> = _uiEvents.receiveAsFlow()

    fun startPresence() {
        viewModelScope.launch {
            presenceRepository.startPresence()
        }
    }

    init {
        refreshAccountProfile()

        // 1) Badge chats
        viewModelScope.launch {
            conversationOverviewRepository.observeUnreadChatList()
                .collect { count ->
                    _uiState.update { it.copy(chatListBadgeCount = count) }
                }
        }

        // 2) Badge RoomsV2
        observeGroupBadges()
    }

    private fun observeGroupBadges() {
        viewModelScope.launch {
            observeRoomsV2UnreadSummary().collect { summary ->
                _uiState.update {
                    it.copy(
                        groupBadgeCount = summary.totalUnread,
                        unreadGroupChatCount = summary.publicUnread,
                        unreadPrivateMessagesCount = summary.privateUnread,
                    )
                }
            }
        }
    }

    // --- ACCIONES DE USUARIO ---
    fun onLocationChanged(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            locationRepository.updateLocation(latitude, longitude)
        }
    }

    fun onLogoutRequested() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGlobalLoading = true) }
            logoutUseCase.execute()
                .onSuccess {
                    delay(config.navigationDelay)
                    appNavigator.finishFlowNavigateToSplash()
                }.onFailure { e ->
                    _uiState.update { it.copy(isGlobalLoading = false) }
                    showErrorSnack(e)
                }
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
                    emit(MainUiEvent.ToGroupsAfterExit)
                }
                .onFailure { e ->
                    showErrorSnack(e)
                }
        }
    }

    fun onUsersTabSelected() {
        if (_destinationUiState.value.currentScreen == CurrentScreen.USERS) return
        viewModelScope.launch { toUsers() }
    }

    fun onChatTabSelected() {
        if (_destinationUiState.value.currentScreen == CurrentScreen.CHAT) return
        viewModelScope.launch { toChat() }
    }

    fun onFavoritesTabSelected() {
        if (_destinationUiState.value.currentScreen == CurrentScreen.FAVORITES) return
        viewModelScope.launch { toFavorites() }
    }

    fun onGroupsTabSelected() {
        viewModelScope.launch { toGroupsSelect() }
    }

    fun onEditProfileSelected() {
        if (_destinationUiState.value.currentScreen == CurrentScreen.EDIT_PROFILE) return
        closeAccountSheet()
        viewModelScope.launch { toEditProfile() }
    }

    fun openAccountSheet() {
        _uiState.update { it.copy(isAccountSheetOpen = true) }
    }

    fun refreshAccountProfile() {
        _uiState.update {
            it.copy(
                accountName = localRepositoryProvider.myUserName,
                accountEmail = localRepositoryProvider.myEmail,
                accountPhotoUrl = localRepositoryProvider.myProfilePhotoUrl
            )
        }
    }

    fun closeAccountSheet() {
        _uiState.update { it.copy(isAccountSheetOpen = false) }
    }

    fun onAccountSettingsSelected() {
        closeAccountSheet()
        toSettings()
    }

    fun onAccountLogoutSelected() {
        closeAccountSheet()
        emit(MainUiEvent.ConfirmLogout)
    }

    fun toEditProfile() = emit(MainUiEvent.ToEditProfile)

    fun toGroupHost() = emit(MainUiEvent.ToGroupHost)
    fun toGroupsSelect() = emit(MainUiEvent.ToGroupsSelect)
    fun toFavorites() = emit(MainUiEvent.ToFavorites)
    fun toChat() = emit(MainUiEvent.ToChat)
    fun toUsers() = emit(MainUiEvent.ToUsers)
    fun toSettings() = emit(MainUiEvent.NavigateToSettings)
    fun confirmExitGroup() = emit(MainUiEvent.ConfirmExitGroup)

    fun emit(event: MainUiEvent) {
        viewModelScope.launch { _uiEvents.send(event) }
    }

    fun showSnack(
        delay: Boolean = false,
        uiText: UiText,
        snackType: ZibeSnackType
    ) {
        viewModelScope.launch {
            if (delay) delay(1000L)
            emit(MainUiEvent.ShowSnack(uiText, snackType))
        }
    }

    fun showErrorSnack(e: Throwable) {
        showSnack(
            uiText = e.message.toUiText(
                R.string.err_zibe_prefix,
                R.string.err_zibe
            ),
            snackType = ZibeSnackType.ERROR
        )
    }

    fun onBackPressed() {
        when (_destinationUiState.value.currentScreen) {

            CurrentScreen.CHAT,
            CurrentScreen.USERS,
            CurrentScreen.FAVORITES,
            CurrentScreen.GROUPS -> {
                viewModelScope.launch {
                    emit(MainUiEvent.BackExitAppOrCloseSearch)
                }
            }

            else -> {
                viewModelScope.launch {
                    emit(MainUiEvent.BackToChat)
                }
            }
        }
    }

    fun onToolbarItemSelected(itemId: Int) {
        when (itemId) {
            R.id.action_settings -> toSettings()
            R.id.action_favorites -> onBottomItemSelected(R.id.navBottomFavorites)
            R.id.action_exit_group -> confirmExitGroup()
            R.id.action_unblock_users -> onUnblockUsersSelected()
            R.id.action_unhide_chats -> onUnhideChatsSelected()
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

    private fun onUnblockUsersSelected() {
        viewModelScope.launch {
            val blockedUsers = profileRepositoryProvider.getBlockedUsers()
            if (blockedUsers.isEmpty()) {
                showSnack(
                    uiText = UiText.StringRes(R.string.msg_no_blocked_users),
                    snackType = ZibeSnackType.WARNING
                )
            } else
                emit(MainUiEvent.ShowUnblockUsersDialog(blockedUsers))
        }
    }

    private fun onUnhideChatsSelected() {
        viewModelScope.launch {
            val hiddenChats = conversationOverviewRepository.getHiddenChats()
            if (hiddenChats.isEmpty()) {
                showSnack(
                    uiText = UiText.StringRes(R.string.msg_no_hidden_chats),
                    snackType = ZibeSnackType.WARNING
                )
            } else {
                emit(MainUiEvent.ShowUnhideChatsDialog(hiddenChats))
            }
        }
    }

    fun onUnhideChatConfirmed(userId: String, userName: String) {
        viewModelScope.launch {
            conversationOverviewRepository.updateChatState(
                userId,
                userName,
                NODE_DM,
                CHAT_STATE_DEFAULT_DM
            )
                .onSuccess {
                    showSnack(
                        uiText = UiText.StringRes(R.string.chat_unhide_success, listOf(userName)),
                        snackType = ZibeSnackType.SUCCESS
                    )
                }.onFailure { e ->
                    showErrorSnack(e)
                }
        }
    }

    fun onConfirmUnblockAction(otherUid: String, otherName: String) {
        viewModelScope.launch {
            emit(
                MainUiEvent.HandleChatSessionEvent(
                    ChatSessionUiEvent.ConfirmToggleBlockAction(
                        name = otherName,
                        isBlockedByMe = true,
                        onConfirm = { toggleBlock(otherUid, otherName) }
                    )
                )
            )
        }
    }

    suspend fun toggleBlock(userId: String, userName: String) {
        runCatchingPreservingCancellation {
            profileRepositoryActions.toggleBlock(userId, userName)
        }.onSuccess { isBlockedByMe ->
            emit(
                MainUiEvent.HandleChatSessionEvent(
                    ChatSessionUiEvent.ShowToggleBlockSuccess(userName, isBlockedByMe)
                )
            )
        }.onFailure { e ->
            showErrorSnack(e)
        }
    }

    fun setDestinationUiState(state: MainDestinationUiState) {
        _destinationUiState.value = state
    }


}
