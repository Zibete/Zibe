package com.zibete.proyecto1.ui.chatlist

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer
) : ViewModel() {

    private val myUid: String get() = userRepository.myUid

    private val chatRef
        get() = firebaseRefsContainer.refData
            .child(myUid)
            .child(NODE_CURRENT_CHAT)

    private var chatListListener: ValueEventListener? = null
    private var observing = false

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatSessionUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ChatSessionUiEvent> = _events.asSharedFlow()

    fun loadChatList() {
        if (observing) return
        observing = true
        observeChatList()
    }

    private fun observeChatList() {
        _uiState.update { it.copy(isLoading = true) }

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

                val all = snapshot.children.mapNotNull { it.getValue(ChatWith::class.java) }.toMutableList()

                updateDatesAndSort(all)

                // Visible = (foto != EMPTY) y (estado == NODE_CURRENT_CHAT o estado == SILENT)
                val visible = all.filter { chat ->
                    val photo = chat.userPhoto
                    val state = chat.state
                    photo != Constants.EMPTY && (state == NODE_CURRENT_CHAT || state == CHAT_STATE_SILENT)
                }

                val q = _uiState.value.searchQuery
                val filtered = filterChats(visible, q)

                _uiState.value = ChatListUiState(
                    isLoading = false,
                    chats = visible,
                    filteredChats = filtered,
                    showOnboarding = visible.isEmpty(),
                    searchQuery = q
                )
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        chatListListener = listener
        chatRef.addValueEventListener(listener)
    }

    fun onSearchQueryChanged(query: String) {
        val normalized = query.trim()
        val currentChats = _uiState.value.chats
        val filtered = filterChats(currentChats, normalized)

        _uiState.update {
            it.copy(
                searchQuery = normalized,
                filteredChats = filtered
            )
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateDatesAndSort(list: MutableList<ChatWith>) {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

        list.forEach { chat ->
            val dt = chat.dateTime
            if (dt.isNullOrBlank()) return@forEach
            try {
                chat.date = format.parse(dt)
            } catch (_: ParseException) {
            }
        }

        // Respeta tu Comparable si existe (como venías usando)
        list.sort()
    }

    override fun onCleared() {
        super.onCleared()
        chatListListener?.let { chatRef.removeEventListener(it) }
        chatListListener = null
        observing = false
    }

    // ---------- Acciones de menú ----------

    fun onMarkAsReadChatListClicked(userId: String, nodeType: String) {
        viewModelScope.launch {
            userRepository.toggleUnreadBadge(userId, nodeType)
        }
    }

    fun onToggleNotificationsClicked(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            val chatWith = chatRepository.getChatWith(myUid, userId, nodeType)
            val currentState = chatWith?.state

            val newState = if (currentState == CHAT_STATE_SILENT) nodeType else CHAT_STATE_SILENT
            userRepository.updateStateChatWith(userId, userName, nodeType, newState)

            val enabled = newState != CHAT_STATE_SILENT
            _events.tryEmit(ChatSessionUiEvent.ShowToggleNotificationSuccess(userName, enabled))
        }
    }

    fun onBlockClicked(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmBlock(
                    name = userName,
                    onConfirm = {
                        userRepository.updateStateChatWith(userId, userName, nodeType, CHAT_STATE_BLOQ)
                        _events.emit(ChatSessionUiEvent.ShowBlockSuccess(userName))
                    }
                )
            )
        }
    }

    fun onHideClicked(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmHideChat(
                    name = userName,
                    onConfirm = {
                        userRepository.updateStateChatWith(userId, userName, nodeType, CHAT_STATE_HIDE)
                        _events.emit(ChatSessionUiEvent.ShowChatHiddenSuccess(userName))
                    }
                )
            )
        }
    }

    fun onDeleteClicked(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            val count = chatRepository.getMessageCountFor(userId, nodeType)

            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = userName,
                    countMessages = count,
                    onConfirm = { deleteMessages ->
                        viewModelScope.launch {
                            try {
                                val result = chatRepository.deleteChatFor(userId, nodeType, deleteMessages)
                                _events.emit(ChatSessionUiEvent.ShowDeleteMessagesSuccess(result.deletedCount))
                            } catch (e: Exception) {
                                _events.emit(
                                    ChatSessionUiEvent.ShowErrorDialog(
                                        e.message ?: "Error al eliminar mensajes"
                                    )
                                )
                            }
                        }
                    }
                )
            )
        }
    }

    private fun filterChats(chats: List<ChatWith>, query: String): List<ChatWith> {
        if (query.isBlank()) return chats
        val lower = query.trim().lowercase()

        return chats.filter { chat ->
            val name = chat.userName.orEmpty().lowercase()
            val id = chat.userId.orEmpty().lowercase()
            name.contains(lower) || id.contains(lower)
        }
    }
}
