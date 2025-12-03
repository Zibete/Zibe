package com.zibete.proyecto1.ui.chatlist

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
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
            .child(Constants.CHAT_STATE_CHATWITH)

    private var chatListListener: ValueEventListener? = null

    // ---------- UI state ----------

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    // ---------- One-shot events ----------

    private val _events = MutableSharedFlow<ChatSessionUiEvent>()
    val events: SharedFlow<ChatSessionUiEvent> = _events.asSharedFlow()

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
                        (state == Constants.CHAT_STATE_CHATWITH || state == "silent")
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

    fun onBlockClicked(idUser: String, userName: String, nodeType : String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmBlock(
                    name = userName,
                    onConfirm = {
                        userRepository.blockUser(idUser, userName, nodeType)
                        _events.emit(ChatSessionUiEvent.ShowBlockSuccess(userName))
                    }
                )
            )
        }
    }

    fun onDeleteClicked(idUser: String, nodeType: String, nameUser: String) {
        viewModelScope.launch {
            val count = userRepository.getMessageCount(idUser, nodeType)

            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = nameUser,
                    countMessages = count,
                    onConfirm = { deleteMessages ->
                        viewModelScope.launch {
                            userRepository.deleteChat(idUser, nameUser, nodeType, deleteMessages)
                            _events.emit(ChatSessionUiEvent.ShowDeleteChatSuccess(nameUser))
                        }
                    }
                )
            )
        }
    }




    private fun unblockUser(chatWithId: String, chatType: String, userName: String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmUnblock(userName) {
                    viewModelScope.launch {
                        userRepository.unblockUser(chatWithId, chatType)
                        _events.emit(ChatSessionUiEvent.ShowUnblockSuccess(userName))
                    }
                }
            )
        }
    }

    private fun hideChat(chatWithId: String, chatType: String, userName: String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmHideChat(userName) {
                    viewModelScope.launch {
                        userRepository.hideChat(chatWithId, chatType)
                        _events.emit(ChatSessionUiEvent.ShowChatHidden)
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
