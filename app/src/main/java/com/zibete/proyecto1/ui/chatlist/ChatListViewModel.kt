package com.zibete.proyecto1.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<ChatListUiEvent>()
    val events: SharedFlow<ChatListUiEvent> = _events.asSharedFlow()

    fun onChatItemMenuAction(
        action: ChatMenuAction,
        chatWithId: String,
        chatType: String,
        userName: String
    ) {
        viewModelScope.launch {
            when (action) {
                ChatMenuAction.MarkAsReadChat ->
                    userRepository.markAsReadChat(chatWithId, chatType)

                ChatMenuAction.SilentUser ->
                    userRepository.updateNotificationState(chatWithId, chatType, userName)

                ChatMenuAction.BlockUser ->
                    blockUser(chatWithId, chatType, userName)

                ChatMenuAction.HideChat ->
                    hideChat(chatWithId, chatType, userName)

                ChatMenuAction.DeleteChat ->
                    deleteChat(chatWithId, chatType, userName)
            }
        }
    }

    private fun deleteChat(chatWithId: String, chatType: String, userName: String) {
        viewModelScope.launch {
            _events.emit(
                ChatListUiEvent.ConfirmDeleteChat(userName) {
                    viewModelScope.launch {
                        // Desde la lista, borro SIEMPRE los mensajes → deleteMessages = true
                        userRepository.deleteChat(chatWithId, chatType, deleteMessages = true)
                        _events.emit(ChatListUiEvent.ShowDeleteChatSuccess(userName))
                    }
                }
            )
        }
    }

    private fun blockUser(chatWithId: String, chatType: String, userName: String) {
        viewModelScope.launch {
            _events.emit(
                ChatListUiEvent.ConfirmBlock(userName) {
                    viewModelScope.launch {
                        userRepository.blockUser(chatWithId, chatType, userName)
                        _events.emit(ChatListUiEvent.ShowBlockSuccess(userName))
                    }
                }
            )
        }
    }

    private fun unblockUser(chatWithId: String, chatType: String, userName: String) {
        viewModelScope.launch {
            _events.emit(
                ChatListUiEvent.ConfirmUnblock(userName) {
                    viewModelScope.launch {
                        // 🔧 acá antes llamabas blockUser por error
                        userRepository.unblockUser(chatWithId, chatType)
                        _events.emit(ChatListUiEvent.ShowUnblockSuccess(userName))
                    }
                }
            )
        }
    }

    private fun hideChat(chatWithId: String, chatType: String, userName: String) {
        viewModelScope.launch {
            _events.emit(
                ChatListUiEvent.ConfirmHideChat(userName) {
                    viewModelScope.launch {
                        userRepository.hideChat(chatWithId, chatType)
                        _events.emit(ChatListUiEvent.ShowChatHidden)
                    }
                }
            )
        }
    }
}

sealed class ChatMenuAction {
    object MarkAsReadChat : ChatMenuAction()
    object SilentUser : ChatMenuAction()
    object BlockUser : ChatMenuAction()
    object HideChat : ChatMenuAction()
    object DeleteChat : ChatMenuAction()
}