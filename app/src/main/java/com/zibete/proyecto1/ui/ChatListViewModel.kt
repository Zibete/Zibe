package com.zibete.proyecto1.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    fun onChatItemMenuAction(
        action: ChatMenuAction,
        chatWithId: String,
        chatType: String,
        userName: String
    ) {
        viewModelScope.launch {
            when (action) {
                ChatMenuAction.MarkAsReadChat -> userRepository.markAsReadChat(chatWithId, chatType)
                ChatMenuAction.SilentUser -> userRepository.silentUser(chatWithId)
                ChatMenuAction.BlockUser -> userRepository.blockUser(chatWithId)
                ChatMenuAction.UnhideChat -> userRepository.unHideChat(chatWithId)
                ChatMenuAction.DeleteChat -> userRepository.deleteChat(chatWithId)
                // etc.
            }
        }
    }
}

sealed class ChatMenuAction {
    object MarkAsReadChat : ChatMenuAction()
    object SilentUser : ChatMenuAction()
    object BlockUser : ChatMenuAction()
    object UnhideChat : ChatMenuAction()
    object DeleteChat : ChatMenuAction()
}