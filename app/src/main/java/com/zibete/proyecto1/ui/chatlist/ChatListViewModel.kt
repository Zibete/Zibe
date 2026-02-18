package com.zibete.proyecto1.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOCKED
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.getOrDefault
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val profileRepositoryActions: ProfileRepositoryActions,
    private val profileRepositoryProvider: ProfileRepositoryProvider,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val chatRef
        get() = userRepository.conversationsRootRef(nodeType = NODE_DM)

    private var chatListListener: ValueEventListener? = null
    private var allChats: List<Conversation> = emptyList()

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatSessionUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ChatSessionUiEvent> = _events.asSharedFlow()

    fun startObserving() {
        if (chatListListener != null) return
        _uiState.update { it.copy(isLoading = true) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    allChats = emptyList()
                    _uiState.value = ChatListUiState(
                        isLoading = false,
                        chats = emptyList(),
                        filteredChats = emptyList(),
                        showOnboarding = true,
                        searchQuery = _uiState.value.searchQuery
                    )
                    return
                }

                val all = mapSnapshot(snapshot)
                val visible = computeVisibleChats(all)
                allChats = visible

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

    fun stopObserving() {
        chatListListener?.let { chatRef.removeEventListener(it) }
        chatListListener = null
    }

    fun onSearchQueryChanged(query: String) {
        val normalized = query.trim()
        val filtered = filterChats(allChats, normalized)

        _uiState.update {
            it.copy(
                searchQuery = normalized,
                filteredChats = filtered
            )
        }
    }

    private fun mapSnapshot(snapshot: DataSnapshot): List<Conversation> =
        snapshot.children
            .mapNotNull { it.getValue(Conversation::class.java) }
            .sorted()

    private fun computeVisibleChats(all: List<Conversation>): List<Conversation> =
        all.filter { chat -> chat.isVisible() }

    // ---------- Acciones de menú ----------

    fun onMarkAsReadChatListClicked(userId: String, nodeType: String) {
        viewModelScope.launch {
            userRepository.toggleUnreadBadge(userId, nodeType)
        }
    }

    fun onToggleNotificationsClicked(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            val chatWith = chatRepository.getConversation(secondUid = userId, nodeType = nodeType)
            val currentState = chatWith?.state

            val newState = if (currentState == CHAT_STATE_SILENT) nodeType else CHAT_STATE_SILENT
            userRepository.updateChatState(userId, userName, nodeType, newState)

            val isNotificationsSilenced = newState == CHAT_STATE_SILENT
            _events.tryEmit(
                ChatSessionUiEvent.ShowToggleNotificationSuccess(
                    name = userName,
                    isNotificationsSilenced = isNotificationsSilenced
                )
            )
        }
    }

    suspend fun toggleBlock(otherUid: String, otherName: String) {
        runCatching {
            profileRepositoryActions.toggleBlock(otherUid, otherName)
        }.onSuccess { isBlockedByMe ->
            _events.emit(ChatSessionUiEvent.ShowToggleBlockSuccess(otherName, isBlockedByMe))
        }.onFailure { e ->
            _events.emit(
                ChatSessionUiEvent.ShowErrorDialog(
                    e.message.toUiText(
                        R.string.err_zibe_prefix,
                        R.string.err_zibe
                    )
                )
            )
        }
    }

    fun onConfirmToggleBlockAction(otherUid: String, otherName: String) {
        viewModelScope.launch {
            val isBlockedByMe =
                profileRepositoryProvider.getMyChatState(otherUid).getOrThrow() == CHAT_STATE_BLOCKED
            _events.emit(
                ChatSessionUiEvent.ConfirmToggleBlockAction(
                    name = otherName,
                    isBlockedByMe = isBlockedByMe,
                    onConfirm = { toggleBlock(otherUid, otherName) }
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
                        userRepository.updateChatState(
                            userId,
                            userName,
                            nodeType,
                            CHAT_STATE_HIDE
                        )
                        _events.emit(ChatSessionUiEvent.ShowChatHiddenSuccess(userName))
                    }
                )
            )
        }
    }

    fun onDeleteClicked(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            val chatRefs = chatRepository.buildChatRefs(userId, nodeType)
            val count = chatRepository.getMessageCount(chatRefs)
            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = userName,
                    countMessages = count,
                    onConfirm = { deleteMessages ->
                        viewModelScope.launch {
                            try {
                                val result = chatRepository.deleteMessages(
                                    chatRefs = chatRefs,
                                    selectedIds = null,
                                    deleteMessages = deleteMessages
                                )
                                _events.emit(ChatSessionUiEvent.ShowDeleteMessagesSuccess(result.deletedCount))
                            } catch (_: Exception) {
                                _events.emit(
                                    ChatSessionUiEvent.ShowErrorDialog(
                                        UiText.StringRes(R.string.chat_error_delete_messages)
                                    )
                                )
                            }
                        }
                    }
                )
            )
        }
    }

    private fun filterChats(chats: List<Conversation>, query: String): List<Conversation> {
        if (query.isBlank()) return chats
        val lower = query.trim().lowercase()

        return chats.filter { chat ->
            val name = chat.otherName.lowercase()
            val id = chat.otherId.lowercase()
            name.contains(lower) || id.contains(lower)
        }
    }
}
