package com.zibete.proyecto1.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.USER_NOT_FOUND_EXCEPTION
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.getOrDefault
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onFinally
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.core.utils.onSuccessNotNull
import com.zibete.proyecto1.data.ChatRefs
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.profile.BlockState
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.cancellation.CancellationException
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

    private val _events = MutableSharedFlow<ChatSessionUiEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    val snackEvents = snackBarManager.events

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _photoList = MutableStateFlow<List<String>>(emptyList())
    val photoList: StateFlow<List<String>> = _photoList.asStateFlow()

    val userStatus: StateFlow<UserStatus> =
        profileRepositoryProvider.observeUserStatus(otherUid, NODE_DM)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserStatus.Offline)

    private var loadJob: Job? = null
    private var metaJob: Job? = null

    fun refreshProfile() = loadProfile(isRefresh = true)

    fun refreshProfileMeta() {
        if (otherUid.isBlank()) return
        if (_uiState.value.content !is ProfileContent.Ready) return
        metaJob?.cancel()
        metaJob = viewModelScope.launch { enrichProfileMeta() }
    }

    fun loadProfile(isRefresh: Boolean = false) {
        loadJob?.cancel()
        metaJob?.cancel()
        loadJob = viewModelScope.launch {
            val currentJob = coroutineContext[Job]
            val hadReadyContent = _uiState.value.content is ProfileContent.Ready
            val shouldShowLoading = !isRefresh || !hadReadyContent

            if (isRefresh) _uiState.update { it.copy(isRefreshing = true) }

            if (otherUid.isBlank()) {
                if (shouldShowLoading) setNotProfileState(content = ProfileContent.NotFound)
                else _events.emit(ChatSessionUiEvent.ShowErrorDialog(profileError))
                if (isRefresh && loadJob == currentJob) _uiState.update { it.copy(isRefreshing = false) }
                return@launch
            } else if (_uiState.value.profile?.id != otherUid && shouldShowLoading)
                setNotProfileState(content = ProfileContent.Loading)

            profileRepositoryProvider.getOtherAccount(otherUid)
                .onFailure { e ->
                    if (isRefresh && hadReadyContent) {
                        _events.emit(ChatSessionUiEvent.ShowErrorDialog(profileError))
                        return@onFailure
                    }
                    setNotProfileState(
                        content = if (e.message == USER_NOT_FOUND_EXCEPTION)
                            ProfileContent.NotFound
                        else
                            ProfileContent.Error(profileError)
                    )
                    _events.emit(ChatSessionUiEvent.ShowErrorDialog(profileError))
                }
                .onSuccess { profile ->
                    if (profile == null) {
                        if (isRefresh && hadReadyContent)
                            _events.emit(ChatSessionUiEvent.ShowErrorDialog(profileError))
                        else
                            setNotProfileState(content = ProfileContent.NotFound)

                        return@onSuccess
                    }

                    _uiState.update {
                        it.copy(
                            content = ProfileContent.Ready(profile),
                            profile = profile
                        )
                    }

                    if (isRefresh) {
                        enrichProfileMeta()
                        return@onSuccess
                    }
                    metaJob = viewModelScope.launch { enrichProfileMeta() }
                }
                .onFinally {
                    if (isRefresh && loadJob == currentJob)
                        _uiState.update { it.copy(isRefreshing = false) }
                }
        }
    }

    private suspend fun enrichProfileMeta() = supervisorScope {
        val groupNameValue = groupName.value

        val chatStateDeferred = async(Dispatchers.IO) {
            profileRepositoryProvider.getMyChatState(otherUid)
                .onFailure { onFailure(it) }
                .getOrDefault("")
        }
        val favoriteDeferred = async(Dispatchers.IO) {
            profileRepositoryProvider.isFavorite(otherUid)
                .onFailure { onFailure(it) }
                .getOrDefault(false)
        }
        val blockStateDeferred = async(Dispatchers.IO) {
            profileRepositoryProvider.getBlockStateWith(otherUid, NODE_DM)
                .onFailure { onFailure(it) }
                .getOrDefault(BlockState(isBlockedByMe = false, hasBlockedMe = false))
        }
        val distanceDeferred = async(Dispatchers.IO) {
            locationRepository.getDistanceToUser(otherUid)
                .onFailure { onFailure(it) }
                .getOrDefault("")
        }
        val groupMatchDeferred = async(Dispatchers.IO) {
            groupRepositoryProvider.isGroupMatch(otherUid, groupNameValue)
                .onFailure { onFailure(it) }
                .getOrDefault(false)
        }
        val photosDeferred = async(Dispatchers.IO) {
            profileRepositoryProvider.getDmPhotoList(otherUid, NODE_DM)
                .onFailure { onFailure(it) }
                .getOrDefault(emptyList())
        }
        val hasConversationDeferred = async(Dispatchers.IO) {
            chatRepository.hasConversation(otherUid, NODE_DM)
                .onFailure { onFailure(it) }
                .getOrDefault(false)
        }

        val chatState = chatStateDeferred.await()
        val blockState = blockStateDeferred.await()
        val distanceLabel = distanceDeferred.await()
        val isGroupMatch = groupMatchDeferred.await()
        val isFavorite = favoriteDeferred.await()
        val hasConversation = hasConversationDeferred.await()
        val photoList = photosDeferred.await()

        _uiState.update {
            it.copy(
                distanceLabel = distanceLabel,
                isGroupMatch = isGroupMatch,
                isFavorite = isFavorite,
                isBlockedByMe = blockState.isBlockedByMe,
                hasBlockedMe = blockState.hasBlockedMe,
                isNotificationsSilenced = chatState == CHAT_STATE_SILENT,
                isHide = chatState == CHAT_STATE_HIDE,
                hasConversation = hasConversation
            )
        }
        _photoList.value = photoList
    }

    fun setNotProfileState(content: ProfileContent) {
        _uiState.update {
            it.copy(
                content = content,
                isRefreshing = false,
                isActionLoading = false,
                profile = null,
                distanceLabel = "",
                isGroupMatch = false,
                isFavorite = false,
                isBlockedByMe = false,
                isNotificationsSilenced = false,
                hasBlockedMe = false,
                isHide = false,
                hasConversation = false
            )
        }
    }

    fun onToggleFavorite() {
        if (isActionLoading()) return
        val profile = _uiState.value.profile ?: return

        viewModelScope.launch {
            setActionLoading(true)
            profileRepositoryActions.toggleFavoriteUser(otherUid)
                .onSuccessNotNull { newFavoriteState ->
                    _uiState.update { it.copy(isFavorite = newFavoriteState) }
                    _events.emit(
                        ChatSessionUiEvent.ShowToggleFavoriteSuccess(
                            profile.name,
                            newFavoriteState
                        )
                    )
                }
                .onFailure { onFailure(it) }
                .onFinally { setActionLoading(false) }
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
            }.onSuccess { isNotificationsSilenced ->
                _uiState.update { it.copy(isNotificationsSilenced = isNotificationsSilenced) }
                _events.emit(
                    ChatSessionUiEvent.ShowToggleNotificationSuccess(
                        otherName,
                        isNotificationsSilenced
                    )
                )
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
                ChatSessionUiEvent.ConfirmToggleBlockAction(
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

    fun onConfirmHide() {
        if (isActionLoading()) return
        val profile = _uiState.value.profile ?: return
        val userName = profile.name
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmHideChat(
                    name = userName,
                    onConfirm = {
                        setActionLoading(true)
                        hideConversation(otherUid, userName, NODE_DM)
                        setActionLoading(false)
                    }
                )
            )
        }
    }

    fun onDeleteChoiceMode() {
        if (isActionLoading()) return
        val profile = _uiState.value.profile ?: return
        val userName = profile.name
        viewModelScope.launch {
            val chatRefs = chatRepository.buildChatRefs(otherUid, NODE_DM)
            val count = chatRepository.getMessageCount(chatRefs)
            if (_uiState.value.isHide) onConfirmDelete(chatRefs, userName)
            else
                _events.emit(
                    ChatSessionUiEvent.DeleteClickedChoiceMode(
                        name = userName,
                        countMessages = count,
                        onConfirm = { shouldDeleteMessages ->
                            if (shouldDeleteMessages) onConfirmDelete(chatRefs, userName)
                            else onConfirmHide()
                        }
                    )
                )
        }
    }

    private fun onConfirmDelete(chatRefs: ChatRefs, userName: String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = userName,
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

    private suspend fun hideConversation(userId: String, userName: String, nodeType: String) {
        profileRepositoryActions.updateChatState(
            userId,
            userName,
            nodeType,
            CHAT_STATE_HIDE
        ).onSuccess {
            _uiState.update { it.copy(isHide = true) }
            _events.emit(ChatSessionUiEvent.ShowChatHiddenSuccess(userName))
        }.onFailure { onFailure(it) }
    }

    private suspend fun deleteMessages(chatRefs: ChatRefs) {
        chatRepository.deleteMessages(
            chatRefs = chatRefs,
            selectedIds = null
        ).onSuccess { deleteResult ->
            val deleteResult = deleteResult ?: return@onSuccess
            _events.emit(ChatSessionUiEvent.ShowDeleteMessagesSuccess(deleteResult.deletedCount))
        }.onFailure { onFailure(it) }
    }

    // ---------- UI ----------

    private fun setActionLoading(isActionLoading: Boolean) =
        _uiState.update { it.copy(isActionLoading = isActionLoading) }

    private fun isActionLoading() = _uiState.value.isActionLoading

    private suspend fun onFailure(e: Throwable) {
        if (e is CancellationException) return
        _events.emit(
            ChatSessionUiEvent.ShowErrorDialog(
                UiText.StringRes(
                    R.string.err_zibe_prefix,
                    listOf(e.message ?: "")
                )
            )
        )
    }
}
