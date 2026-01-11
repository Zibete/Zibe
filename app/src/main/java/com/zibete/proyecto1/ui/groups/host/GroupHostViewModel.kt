package com.zibete.proyecto1.ui.groups.host

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.model.GroupChatChildEvent
import com.zibete.proyecto1.model.ChatGroupItem
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.core.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.core.constants.ERR_ZIBE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupHostViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val userPreferencesProvider: UserPreferencesProvider
) : ViewModel() {

    val myUid get() = userRepository.myUid

    private val _uiState = MutableStateFlow(GroupHostUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<GroupHostEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val groupContext: StateFlow<GroupContext?> =
        userPreferencesProvider.groupContextFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        bootstrap()
    }

    private fun bootstrap() {

        viewModelScope.launch {
            groupContext
                .collect { ctx ->
                    _uiState.update {
                        it.copy(groupContext = ctx)
                    }
                }
        }

        val groupContext = uiState.value.groupContext ?: return

        viewModelScope.launch {

            val groupName = groupContext.groupName

            val groupMeta = groupRepository.getGroup(groupName)

            if (groupMeta == null) {
                onError("Grupo '$groupName' ya no existe")
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            groupRepository.observeGroupUsers(groupName)
                .collect { users ->
                    _uiState.update { it.copy(users = users, isLoading = false) }
                }

            // 4) Group chat events (filtrados para no mostrar historial previo)
            groupRepository.observeGroupChatEvents(
                groupName = groupName
            ).collect { event ->
                when (event) {
                    is GroupChatChildEvent.Added -> onMessageAdded(event.item)
                    is GroupChatChildEvent.Changed -> onMessageChanged(event.item)
                    is GroupChatChildEvent.Removed -> onMessageRemoved(event.id)
                }
            }
        }
    }

    private fun onMessageAdded(item: ChatGroupItem) {
        _uiState.update { state ->
            state.copy(messages = (state.messages + item).takeLast(state.maxChatSize))
        }
        markReadIfChatVisible()
    }

    private fun onMessageChanged(item: ChatGroupItem) {
        _uiState.update { st ->
            st.copy(messages = st.messages.map { if (it.id == item.id) item else it })
        }
    }

    private fun onMessageRemoved(item: ChatGroupItem) {
        _uiState.update { st ->
            st.copy(messages = st.messages.filterNot { it.id == item.id })
        }
    }

    fun onTabSelected(tab: GroupHostTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        markReadIfChatVisible()
    }

    fun tryHandleBack(): Boolean {
        val state = _uiState.value
        return if (state.selectedTab != GroupHostTab.GROUP_CHAT) {
            onTabSelected(GroupHostTab.GROUP_CHAT)
            true
        } else {
            false
        }
    }

    private fun markReadIfChatVisible() {
        val state = _uiState.value

        if (state.selectedTab != GroupHostTab.GROUP_CHAT) return

        val groupContext = uiState.value.groupContext ?: return

        val groupName = groupContext.groupName

        viewModelScope.launch {
            groupRepository.markGroupAsRead(groupName)
        }
    }

    fun onUserClicked(user: UserGroup) {
        if (user.userId.isBlank() || user.userId == userRepository.myUid) return

        viewModelScope.launch {
            _events.send(
                GroupHostEvent.OpenPrivateChat(
                    otherUid = user.userId,
                    nodeType = NODE_GROUP_DM
                )
            )
        }
    }

    fun sendTextMessage(content: String) {
        val groupContext = uiState.value.groupContext ?: return

        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            try {
                groupRepository.sendGroupMessage(
                    groupName = groupContext.groupName,
                    userName = groupContext.userName,
                    userType = groupContext.userType,
                    chatType = MSG_TEXT,
                    content = content.trim()
                )
            } catch (e: Exception) {
                onError(e.message)
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    suspend fun onError(message: String?){
        _events.send(GroupHostEvent.ShowSnack(
            message = message ?: ERR_ZIBE,
            type = ZibeSnackType.ERROR)
        )
    }

    fun sendPhotoMessage(photoUri: Uri) {
        val groupContext = uiState.value.groupContext ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            try {
                groupRepository.sendGroupPhotoMessage(
                    groupName = groupContext.groupName,
                    photoUri = photoUri,
                    senderName = groupContext.userName,
                    userType = groupContext.userType
                )
            } catch (e: Exception) {
                onError(e.message)
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun onNotImplementedYet() {
        viewModelScope.launch {
            _events.send(GroupHostEvent.ShowSnack(
                message = "No implemented",
                type = ZibeSnackType.WARNING)
            )
        }
    }
}
