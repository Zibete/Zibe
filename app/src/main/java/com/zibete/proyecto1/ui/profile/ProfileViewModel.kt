package com.zibete.proyecto1.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_CHATWITH
import com.zibete.proyecto1.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
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
                    error = "No se encontró el usuario"
                )
                return@launch
            }

            val age = Utils.calcAge(profile.birthDay)

            val distanceMeters = locationRepository.getDistanceMeters(
                userRepository.latitude,
                userRepository.longitude,
                profile.lat,
                profile.long
            )
            val distanceFormat = locationRepository.formatDistance(distanceMeters)

            val userState = userRepository.getChatStateWith(userId, CHAT_STATE_CHATWITH)

            _uiState.value = ProfileUiState(
                isLoading = false,
                name = profile.name,
                age = age,
                description = profile.description,
                distance = distanceFormat,
                photoUrl = profile.photoUrl,
                chatState = userState
            )

            val photos = userRepository.getChatPhotosWithUser(userId)
            _photosFromChat.value = photos

            loadFavoriteAndBlockState(userId)
            loadGroupAlias(userId)
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

    fun loadGroupAlias(userId: String) {
        viewModelScope.launch {
            val groupName = userPreferencesRepository.groupName

            val userGroup = userRepository.getUserGroup(groupName, userId)
            val name = userGroup?.userName

            _uiState.value = _uiState.value.copy(
                unknownName = name,
                hasGroupAlias = name != null
            )
        }
    }

    fun onBlockClicked(idUser: String) {
        viewModelScope.launch {
            val nameUser = _uiState.value.name
            _events.emit(
                ChatSessionUiEvent.ConfirmBlock(
                    name = nameUser,
                    onConfirm = {
                        viewModelScope.launch {
                            userRepository.blockUser(
                                idUser = idUser,
                                nodoType = CHAT_STATE_CHATWITH,
                                userName = nameUser
                            )
                            _uiState.value = _uiState.value.copy(chatState = CHAT_STATE_BLOQ)
                            _events.emit(ChatSessionUiEvent.ShowBlockSuccess(nameUser))
                        }
                    }
                )
            )
        }
    }

    fun onUnblockClicked(idUser: String, nameUser: String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmUnblock(
                    name = nameUser,
                    onConfirm = {
                        viewModelScope.launch {
                            userRepository.unblockUser(
                                userId = idUser,
                                chatType = CHAT_STATE_CHATWITH
                            )
                            _uiState.value = _uiState.value.copy(chatState = CHAT_STATE_CHATWITH)
                            _events.emit(ChatSessionUiEvent.ShowUnblockSuccess(nameUser))
                        }
                    }
                )
            )
        }
    }

    fun onDeleteChatClicked(chatWithId: String, chatType: String, nameUser: String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(nameUser) {
                    viewModelScope.launch {
                        userRepository.deleteChat(
                            chatWithId,
                            chatType,
                            deleteMessages = true
                        )
                        _events.emit(ChatSessionUiEvent.ShowDeleteChatSuccess(nameUser))
                    }
                }
            )
        }
    }


}
