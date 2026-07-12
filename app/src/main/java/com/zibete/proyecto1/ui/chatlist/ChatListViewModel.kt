package com.zibete.proyecto1.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOCKED
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.core.utils.runCatchingPreservingCancellation
import com.zibete.proyecto1.data.ChatRepositoryContract
import com.zibete.proyecto1.data.ChatThread
import com.zibete.proyecto1.data.ConversationOverviewRepository
import com.zibete.proyecto1.data.LocalRepositoryProvider
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val localRepositoryProvider: LocalRepositoryProvider,
    private val conversationOverviewRepository: ConversationOverviewRepository,
    private val profileRepositoryActions: ProfileRepositoryActions,
    private val profileRepositoryProvider: ProfileRepositoryProvider,
    private val chatRepository: ChatRepositoryContract
) : ViewModel() {

//    private val chatRef
//        get() = userRepository.conversationsRootRef(nodeType = NODE_DM)

    private var observeJob: Job? = null
    private var allChats: List<Conversation> = emptyList()

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatSessionUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ChatSessionUiEvent> = _events.asSharedFlow()

    fun startObserving() {
        if (observeJob != null) return
        setIsLoading(true)
        observeJob = viewModelScope.launch {
            chatRepository.observeConversations(NODE_DM).collect { all ->
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
        }
    }

    fun stopObserving() {
        observeJob?.cancel()
        observeJob = null
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

    private fun computeVisibleChats(all: List<Conversation>): List<Conversation> =
        all.filter { chat -> chat.isVisible() }


    private fun filterChats(chats: List<Conversation>, query: String): List<Conversation> {
        if (query.isBlank()) return chats
        val lower = query.trim().lowercase()

        return chats.filter { chat ->
            val name = chat.otherName.lowercase()
            val id = chat.otherId.lowercase()
            name.contains(lower) || id.contains(lower)
        }
    }

    // ---------- Acciones de menú ----------

    fun onMarkAsReadChatListClicked(userId: String, nodeType: String) {
        viewModelScope.launch {
            conversationOverviewRepository.toggleUnreadBadge(userId, nodeType)
        }
    }

    fun onToggleNotificationsClicked(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            val chatWith = chatRepository.getConversation(
                firstUid = localRepositoryProvider.myUid,
                secondUid = userId,
                nodeType = nodeType
            )
            val currentState = chatWith?.state

            val newState = if (currentState == CHAT_STATE_SILENT) nodeType else CHAT_STATE_SILENT
            conversationOverviewRepository.updateChatState(userId, userName, nodeType, newState)
                .onSuccess {
                    _events.emit(
                        ChatSessionUiEvent.ShowToggleNotificationSuccess(
                            name = userName,
                            isNotificationsSilenced = newState == CHAT_STATE_SILENT
                        )
                    )
                }
                .onFailure { onFailure(it) }
        }
    }

    fun onConfirmToggleBlockAction(otherUid: String, otherName: String) {
        viewModelScope.launch {
            profileRepositoryProvider.getMyChatState(otherUid)
                .onSuccess { state ->
                    _events.emit(
                        ChatSessionUiEvent.ConfirmToggleBlockAction(
                            name = otherName,
                            isBlockedByMe = state == CHAT_STATE_BLOCKED,
                            onConfirm = { toggleBlock(otherUid, otherName) }
                        )
                    )
                }
                .onFailure { onFailure(it) }
        }
    }

    fun onConfirmHide(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmHideChat(
                    name = userName,
                    onConfirm = {
                        setIsLoading(true)
                        hideConversation(userId, userName, nodeType)
                        setIsLoading(false)
                    }
                )
            )
        }
    }

    fun onDeleteChoiceMode(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            val chatRefs = chatRepository.chatThread(userId, nodeType)
            val count = chatRepository.getMessageCount(chatRefs)
            _events.emit(
                ChatSessionUiEvent.DeleteClickedChoiceMode(
                    name = userName,
                    countMessages = count,
                    onConfirm = { shouldDeleteMessages ->
                        if (shouldDeleteMessages) onConfirmDelete(chatRefs, userName)
                        else onConfirmHide(userId, userName, nodeType)
                    }
                )
            )
        }
    }

    private suspend fun toggleBlock(otherUid: String, otherName: String) {
        runCatchingPreservingCancellation {
            profileRepositoryActions.toggleBlock(otherUid, otherName)
        }.onSuccess { isBlockedByMe ->
            _events.emit(ChatSessionUiEvent.ShowToggleBlockSuccess(otherName, isBlockedByMe))
        }.onFailure { onFailure(it) }
    }

    private fun onConfirmDelete(chatRefs: ChatThread, userName: String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = userName,
                    onConfirm = {
                        viewModelScope.launch {
                            setIsLoading(true)
                            deleteMessages(chatRefs)
                            setIsLoading(false)
                        }
                    }
                )
            )
        }
    }

    private suspend fun hideConversation(userId: String, userName: String, nodeType: String) {
        conversationOverviewRepository.updateChatState(
            userId,
            userName,
            nodeType,
            CHAT_STATE_HIDE
        ).onSuccess {
            _events.emit(ChatSessionUiEvent.ShowChatHiddenSuccess(userName))
        }.onFailure { onFailure(it) }
    }

    private suspend fun deleteMessages(chatRefs: ChatThread) {
        chatRepository.deleteMessages(
            thread = chatRefs,
            selectedIds = null
        ).onSuccess { deleteResult ->
            val deleteResult = deleteResult ?: return@onSuccess
            _events.emit(ChatSessionUiEvent.ShowDeleteMessagesSuccess(deleteResult.deletedCount))
        }.onFailure { onFailure(it) }
    }

    // ---------- UI ----------

    private fun setIsLoading(isLoading: Boolean) =
        _uiState.update { it.copy(isLoading = isLoading) }

    private suspend fun onFailure(e: Throwable) {
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
