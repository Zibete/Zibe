package com.zibete.proyecto1.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserPreferencesDSRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.ui.constants.Constants.NODE_DM
import com.zibete.proyecto1.ui.constants.ERR_ZIBE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val locationRepository: LocationRepository,
    private val userPreferencesDSRepository: UserPreferencesDSRepository
) : ViewModel() {

    private val myUid: String get() = userRepository.myUid
    private val userId: String = savedStateHandle[EXTRA_USER_ID] ?: ""

    private val _events = MutableSharedFlow<ChatSessionUiEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _photosFromChat = MutableStateFlow<List<String>>(emptyList())
    val photosFromChat: StateFlow<List<String>> = _photosFromChat.asStateFlow()

    val userStatus: StateFlow<UserStatus> = userRepository
        .observeUserStatus(userId, NODE_DM)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserStatus.Offline)

    fun loadProfile(force: Boolean = false) {
        if (!force && _uiState.value.profile != null) return

        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            val profile = userRepository.getAccount(userId)
            if (profile == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val userState = userRepository.getChatStateWith(userId, _uiState.value.chatState)

            _uiState.value = ProfileUiState(
                isLoading = false,
                profile = profile,
                chatState = userState
            )

            val photos = userRepository.getChatPhotosWithUser(userId, NODE_DM)
            _photosFromChat.value = photos

            loadFavoriteAndBlockState()
            loadGroupMatch()
        }
    }

    fun getDistanceToUser(profile: Users): String {
        val distanceMeters = locationRepository.getDistanceMeters(
            userRepository.latitude,
            userRepository.longitude,
            profile.latitude,
            profile.longitude
        )
        return locationRepository.formatDistance(distanceMeters)
    }

    private suspend fun loadFavoriteAndBlockState() {
        val isFav = userRepository.isUserFavorite(userId)
        val blockState = userRepository.getBlockStateWith(userId)

        _uiState.value = _uiState.value.copy(
            isFavorite = isFav,
            iBlockedUser = blockState.iBlockedUser,
            userBlockedMe = blockState.userBlockedMe
        )
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            val current = _uiState.value.isFavorite
            userRepository.toggleFavoriteUser(userId, current)
            _uiState.value = _uiState.value.copy(isFavorite = !current)
        }
    }

    // En MainViewModel
    val groupName: String
        get() = runBlocking { userPreferencesDSRepository.groupNameFlow.first() }

    fun loadGroupMatch() {
        viewModelScope.launch {

            if (groupName.isBlank()) {
                _uiState.value = _uiState.value.copy(isGroupMatch = false)
                return@launch
            }

            val userGroup = groupRepository.getUserGroup(userId, groupName)

            // no existe -> false
            if (userGroup == null) {
                _uiState.value = _uiState.value.copy(isGroupMatch = false)
                return@launch
            }

            // existe pero type=0 -> false
            if (userGroup.type == 0) {
                _uiState.value = _uiState.value.copy(isGroupMatch = false)
                return@launch
            }

            // existe y type=1 -> true
            _uiState.value = _uiState.value.copy(isGroupMatch = true)
        }
    }

    // ---------- Acciones de menú ----------

    fun onToggleNotificationsClicked(nodeType: String) {
        val userName = _uiState.value.profile?.name ?: return

        viewModelScope.launch {
            val chatWith = chatRepository.getConversation(myUid, userId, nodeType)

            val currentState = chatWith?.state
            val newState = if (currentState == CHAT_STATE_SILENT) nodeType else CHAT_STATE_SILENT

            userRepository.updateStateChatWith(userId, userName, nodeType, newState)

            val enabled = newState != CHAT_STATE_SILENT

            _uiState.update { it.copy(chatState = newState) }
            _events.emit(ChatSessionUiEvent.ShowToggleNotificationSuccess(userName, enabled))
        }
    }

    fun onBlockClicked(nodeType: String) {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            val profile = _uiState.value.profile ?: return@launch

            val userName = profile.name

            _events.emit(
                ChatSessionUiEvent.ConfirmBlock(
                    name = userName,
                    onConfirm = {
                        userRepository.updateStateChatWith(userId, userName, nodeType, CHAT_STATE_BLOQ)
                        _events.emit(ChatSessionUiEvent.ShowBlockSuccess(userName))
                        _uiState.update { it.copy(iBlockedUser = true, chatState = CHAT_STATE_BLOQ) }
                    }
                )
            )
        }
    }

    fun onUnblockClicked(nodeType: String) {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            val profile = _uiState.value.profile ?: return@launch

            val userName = profile.name

            _events.emit(
                ChatSessionUiEvent.ConfirmUnblock(
                    name = userName,
                    onConfirm = {
                        userRepository.updateStateChatWith(userId, userName, nodeType, nodeType)
                        _events.emit(ChatSessionUiEvent.ShowUnblockSuccess(userName))
                        _uiState.update { it.copy(iBlockedUser = false, chatState = nodeType) }
                    }
                )
            )
        }
    }

    fun onDeleteClicked(nodeType: String) {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            val profile = _uiState.value.profile ?: return@launch

            val chatRefs = chatRepository.buildChatRefs(userId, nodeType)
            val count = chatRepository.getMessageCount(chatRefs)

            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = profile.name,
                    countMessages = count,
                    onConfirm = { deleteMessages ->
                        viewModelScope.launch {
                            try {
                                val result = chatRepository.deleteMessages(chatRefs, null, deleteMessages)
                                _events.emit(ChatSessionUiEvent.ShowDeleteMessagesSuccess(result.deletedCount))
                            } catch (e: Exception) {
                                _events.emit(ChatSessionUiEvent.ShowErrorDialog(e.message ?: ERR_ZIBE))
                            }
                        }
                    }
                )
            )
        }
    }
}
