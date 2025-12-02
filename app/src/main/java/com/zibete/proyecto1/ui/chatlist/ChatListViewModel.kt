package com.zibete.proyecto1.ui.chatlist

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.ui.constants.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer,
) : ViewModel() {

    // ---------- Firebase ref ----------

    private val chatRef
        get() = firebaseRefsContainer.refDatos
            .child(userRepository.myUid)
            .child(Constants.CHATWITH)

    private var chatListListener: ValueEventListener? = null

    // ---------- UI state ----------

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    // ---------- One-shot events ----------

    private val _events = MutableSharedFlow<ChatListUiEvent>()
    val events: SharedFlow<ChatListUiEvent> = _events.asSharedFlow()

    init {
        observeChatList()
    }

    // ---------- Firebase observer ----------

    private fun observeChatList() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    _uiState.value = ChatListUiState(
                        isLoading = false,
                        chats = emptyList(),
                        filteredChats = emptyList(),
                        showOnboarding = true
                    )
                    return
                }

                val chats = mutableListOf<ChatWith>()
                var visibleCount = 0L

                for (child in snapshot.children) {
                    val chat = child.getValue(ChatWith::class.java) ?: continue
                    chats.add(chat)

                    val state = child.child("estado").getValue(String::class.java)
                    val photo = child.child("wUserPhoto").getValue(String::class.java)

                    if (photo != Constants.EMPTY &&
                        (state == Constants.CHATWITH || state == "silent")
                    ) {
                        visibleCount++
                    }
                }

                updateDatesAndSort(chats)

                val currentQuery = _uiState.value.searchQuery
                val filtered = filterChats(chats, currentQuery)

                _uiState.value = ChatListUiState(
                    isLoading = false,
                    chats = chats,
                    filteredChats = filtered,
                    showOnboarding = visibleCount == 0L
                )
            }


            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }

        chatListListener = listener
        chatRef.addValueEventListener(listener)
    }

    fun onSearchQueryChanged(query: String) {
        val normalized = query.trim()
        val currentChats = _uiState.value.chats
        val filtered = filterChats(currentChats, normalized)

        _uiState.value = _uiState.value.copy(
            searchQuery = normalized,
            filteredChats = filtered
        )
    }


    @SuppressLint("SimpleDateFormat")
    private fun updateDatesAndSort(list: MutableList<ChatWith>) {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        list.forEach { chat ->
            try {
                chat.date = format.parse(chat.dateTime)
            } catch (_: ParseException) {
            }
        }
        list.sort()
    }

    override fun onCleared() {
        super.onCleared()
        chatListListener?.let { chatRef.removeEventListener(it) }
        chatListListener = null
    }

    // ---------- Acciones de menú ----------

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
                        userRepository.deleteChat(
                            chatWithId,
                            chatType,
                            deleteMessages = true
                        )
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

    private fun filterChats(chats: List<ChatWith>, query: String): List<ChatWith> {
        if (query.isBlank()) return chats
        val lower = query.trim().lowercase()

        return chats.filter { chat ->
            chat.userName.lowercase().contains(lower) ||
                    chat.userId.lowercase().contains(lower)
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
