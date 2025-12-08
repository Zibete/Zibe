package com.zibete.proyecto1.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.ERR_ZIBE
import com.zibete.proyecto1.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val locationRepository: LocationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<ChatSessionUiEvent>()
    val events = _events

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _photosFromChat = MutableStateFlow<List<String>>(emptyList())
    val photosFromChat: StateFlow<List<String>> = _photosFromChat


    private val targetUserId: String? = savedStateHandle["userId"]


    val userStatus: StateFlow<UserStatus> = userRepository
        .observeUserStatus(targetUserId ?: "", "chatWith")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatus.Offline)

    private val myUid = userRepository.myUid

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val profile = userRepository.getUserProfile(userId)

            if (profile == null) {
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    error = ERR_ZIBE
                )
                return@launch
            }

            val age = Utils.calcAge(profile.birthDay)

            val distanceMeters = locationRepository.getDistanceMeters(
                userRepository.latitude,
                userRepository.longitude,
                profile.latitude,
                profile.longitude
            )
            val distanceFormat = locationRepository.formatDistance(distanceMeters)

            val userState = userRepository.getChatStateWith(userId, NODE_CURRENT_CHAT)

            _uiState.value = ProfileUiState(
                isLoading = false,
                name = profile.name,
                age = age,
                description = profile.description,
                distance = distanceFormat,
                photoUrl = profile.profilePhoto,
                chatState = userState
            )

            val photos = userRepository.getChatPhotosWithUser(userId, NODE_CURRENT_CHAT)
            _photosFromChat.value = photos

            loadFavoriteAndBlockState(userId)
            loadGroupMatch(userId)
        }
    }

    private suspend fun loadFavoriteAndBlockState(userId: String) {
        val isFav = userRepository.isUserFavorite(userId)
        val blockState = userRepository.getBlockStateWith(userId)

        _uiState.value = _uiState.value.copy(
            isFavorite = isFav,
            iBlockedUser = blockState.iBlockedUser,
            userBlockedMe = blockState.userBlockedMe
        )
    }

    fun onToggleFavorite(userId: String) {
        viewModelScope.launch {
            val current = _uiState.value.isFavorite
            if (current) {
                userRepository.removeFavoriteUser(userId)
            } else {
                userRepository.addFavoriteUser(userId)
            }
            _uiState.value = _uiState.value.copy(isFavorite = !current)
        }
    }

    fun loadGroupMatch(userId: String) {
        viewModelScope.launch {

            val groupName = userPreferencesRepository.groupName

            val userGroup = groupRepository.getUserGroup(userId, groupName)

            if (userGroup == null) {
                _uiState.value = _uiState.value.copy(isGroupMatch = false)
                return@launch
            }

            if (userGroup.type == 0) {
                _uiState.value = _uiState.value.copy(isGroupMatch = false)
                return@launch
            }

            // Si existe y type = 1 → match válido
            _uiState.value = _uiState.value.copy(isGroupMatch = true)
        }
    }

    // ---------- Acciones de menú ----------

    fun onToggleNotificationsClicked(userId: String, userName: String, nodeType : String) {

        viewModelScope.launch {
            val chatWith = chatRepository.getChatWith(myUid,userId, nodeType)

            val currentState = chatWith?.state

            val newState = if (currentState == CHAT_STATE_SILENT) {
                nodeType // Siempre va a ser ChatWith acá x ahora
            } else {
                CHAT_STATE_SILENT
            }

            userRepository.updateStateChatWith(userId, userName, nodeType, newState)

            val enabled = newState != CHAT_STATE_SILENT // UI: enabled = TRUE si NO está en silent

            // Actualizar header
            _uiState.update { it.copy(chatState = newState) }

            // Emitir evento para mostrar snack
            _events.emit(ChatSessionUiEvent.ShowToggleNotificationSuccess(userName, enabled))
        }
    }

    fun onBlockClicked(userId: String, userName: String, nodeType : String) {
        viewModelScope.launch {
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

    fun onUnblockClicked(userId: String, userName: String, nodeType : String) {
        viewModelScope.launch {
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

    fun onDeleteClicked(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            val count = userRepository.getMessageCount(userId, nodeType)
            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = userName,
                    countMessages = count,
                    onConfirm = { deleteMessages ->
                        viewModelScope.launch {
                            userRepository.deleteChat(userId, nodeType, deleteMessages)
                            _events.emit(ChatSessionUiEvent.ShowDeleteChatSuccess(userName))
                        }
                    }
                )
            )
        }
    }

}
