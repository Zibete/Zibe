package com.zibete.proyecto1.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.USER_NOT_FOUND_EXCEPTION
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.data.ChatRefs
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val groupRepositoryProvider: GroupRepositoryProvider,
    private val locationRepository: LocationRepository,
    private val profileRepositoryProvider: ProfileRepositoryProvider,
    private val profileRepositoryActions: ProfileRepositoryActions,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val snackBarManager: SnackBarManager
) : ViewModel() {

    val otherUid: String = savedStateHandle[EXTRA_USER_ID] ?: ""
    val groupName: StateFlow<String> = userPreferencesProvider.groupNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val profileError = UiText.StringRes(R.string.msg_profile_load_error)

    private val _events = MutableSharedFlow<ChatSessionUiEvent>(extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()

    val snackEvents = snackBarManager.events

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _photoList = MutableStateFlow<List<String>>(emptyList())
    val photoList: StateFlow<List<String>> = _photoList.asStateFlow()

    val userStatus: StateFlow<UserStatus> =
        profileRepositoryProvider.observeUserStatus(otherUid, NODE_DM)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserStatus.Offline)

    fun loadProfile() {
        viewModelScope.launch {
            if (otherUid.isBlank()) {
                setNotProfileState(content = ProfileContent.NotFound)
                return@launch
            } else setNotProfileState(content = ProfileContent.Loading)

            profileRepositoryProvider.getOtherAccount(otherUid)
                .onFailure { e ->
                    setNotProfileState(
                        content = if (e.message == USER_NOT_FOUND_EXCEPTION) ProfileContent.NotFound
                        else ProfileContent.Error(profileError)
                    )
                    _events.emit(ChatSessionUiEvent.ShowErrorDialog(profileError))
                    return@launch
                }
                .onSuccess { profile ->
                    if (profile == null) {
                        setNotProfileState(content = ProfileContent.NotFound)
                        return@launch
                    }

                    runCatching {
                        val chatRefs = chatRepository.buildChatRefs(otherUid, NODE_DM)
                        val count = chatRepository.getMessageCount(chatRefs)
                        val chatState = profileRepositoryProvider.getMyChatState(otherUid)
                        val isFavorite = profileRepositoryProvider.isFavorite(otherUid)
                        val blockState = profileRepositoryProvider.getBlockStateWith(otherUid)
                        val distanceLabel = locationRepository.getDistanceToUser(otherUid)
                        val isGroupMatch =
                            groupRepositoryProvider.isGroupMatch(otherUid, groupName.value)

                        _uiState.update {
                            it.copy(
                                content = ProfileContent.Ready(profile),
                                profile = profile,
                                distanceLabel = distanceLabel,
                                isGroupMatch = isGroupMatch,
                                isFavorite = isFavorite,
                                isBlockedByMe = blockState.isBlockedByMe,
                                hasBlockedMe = blockState.hasBlockedMe,
                                isNotificationsSilenced = chatState == CHAT_STATE_SILENT,
                                canDeleteChat = count > 0
                            )
                        }

                        _photoList.value =
                            profileRepositoryProvider.getDmPhotoList(otherUid)

                    }.onFailure {
                        setNotProfileState(content = ProfileContent.Error(profileError))
                        return@launch
                    }
                }
        }
    }

    fun setNotProfileState(content: ProfileContent) {
        _uiState.update {
            it.copy(
                content = content,
                isActionLoading = false,
                profile = null,
                distanceLabel = "",
                isGroupMatch = false,
                isFavorite = false,
                isBlockedByMe = false,
                isNotificationsSilenced = false,
                hasBlockedMe = false,
                canDeleteChat = false
            )
        }
    }

    fun onToggleFavorite() {
        if (isActionLoading()) return
        val profile = _uiState.value.profile ?: return

        viewModelScope.launch {
            setActionLoading(true)
            runCatching {
                profileRepositoryActions.toggleFavoriteUser(otherUid)
            }.onSuccess { newFavoriteState ->
                _uiState.update { it.copy(isFavorite = newFavoriteState) }
                _events.emit(ChatSessionUiEvent.ShowToggleFavoriteSuccess(profile.name, newFavoriteState))
            }
            setActionLoading(false)
        }
    }

    fun onToggleNotifications() {
        if (isActionLoading()) return
        val profile = _uiState.value.profile ?: return
        val otherName = profile.name

        viewModelScope.launch {
            setActionLoading(true)
            runCatching {
                profileRepositoryActions.toggleNotificationsUser(otherUid, otherName)
            }.onSuccess { silenced ->
                _uiState.update { it.copy(isNotificationsSilenced = silenced) }
                _events.emit(ChatSessionUiEvent.ShowToggleNotificationSuccess(otherName, silenced))
            }
            setActionLoading(false)
        }
    }

    fun onConfirmBlockAction() {
        if (isActionLoading()) return
        val state = _uiState.value
        val profile = _uiState.value.profile ?: return
        val otherName = profile.name

        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmBlockAction(
                    name = otherName,
                    isBlockedByMe = state.isBlockedByMe,
                    onConfirm = {
                        viewModelScope.launch {
                            setActionLoading(true)
                            toggleBlock(otherName)
                            setActionLoading(false)
                        }
                    }
                )
            )
        }
    }

    suspend fun toggleBlock(otherName: String) {
        runCatching {
            profileRepositoryActions.toggleBlock(otherUid, otherName)
        }.onSuccess { isBlockedByMe ->
            _uiState.update { it.copy(isBlockedByMe = isBlockedByMe) }
            _events.emit(ChatSessionUiEvent.ShowToggleBlockSuccess(otherName, isBlockedByMe))
        }
    }

    fun onConfirmDeleteAction() {
        if (isActionLoading()) return
        val profile = _uiState.value.profile ?: return
        viewModelScope.launch {

            val chatRefs = chatRepository.buildChatRefs(otherUid, NODE_DM)
            val count = chatRepository.getMessageCount(chatRefs)

            if (count == 0) {
                _events.emit(ChatSessionUiEvent.ShowErrorDialog(UiText.StringRes(R.string.empty_chat)))
                return@launch
            }

            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = profile.name,
                    countMessages = count,
                    onConfirm = {
                        viewModelScope.launch {
                            setActionLoading(true)
                            deleteMessages(chatRefs)
                            setActionLoading(false)
                        }
                    }
                )
            )
        }
    }

    suspend fun deleteMessages(chatRefs: ChatRefs) {
        runCatching {
            chatRepository.deleteMessages(chatRefs, null, true)
        }.onSuccess { deleteResult ->
            _events.emit(ChatSessionUiEvent.ShowDeleteMessagesSuccess(deleteResult.deletedCount))
        }.onFailure { e ->
            _events.emit(
                ChatSessionUiEvent.ShowErrorDialog(
                    UiText.StringRes(R.string.err_zibe_prefix, listOf(e.message ?: ""))
                )
            )
        }
    }

    fun setActionLoading(isActionLoading: Boolean) {
        _uiState.update { it.copy(isActionLoading = isActionLoading) }
    }

    fun isActionLoading() = _uiState.value.isActionLoading
}
