package com.zibete.proyecto1.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOCKED
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.core.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.PATH_PHOTOS
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.TimeUtils.now
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.core.utils.runCatchingPreservingCancellation
import com.zibete.proyecto1.data.ChatRepositoryContract
import com.zibete.proyecto1.data.ChatThread
import com.zibete.proyecto1.data.ConversationOverviewRepository
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.domain.chat.SendChatMessageCommand
import com.zibete.proyecto1.domain.chat.SendChatMessageOutcome
import com.zibete.proyecto1.domain.chat.SendChatMessageUseCase
import com.zibete.proyecto1.model.ChatChildEvent
import com.zibete.proyecto1.model.ChatMessageItem
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.model.isDeletedFor
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val textProvider: ChatTextProvider,
    private val groupRepositoryProvider: GroupRepositoryProvider,
    private val localRepositoryProvider: LocalRepositoryProvider,
    private val userRepositoryActions: UserRepositoryActions,
    private val conversationOverviewRepository: ConversationOverviewRepository,
    private val userRepositoryProvider: UserRepositoryProvider,
    private val profileRepositoryProvider: ProfileRepositoryProvider,
    private val profileRepositoryActions: ProfileRepositoryActions,
    private val chatRepository: ChatRepositoryContract,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val sessionRepositoryProvider: SessionRepositoryProvider,
    private val userPreferencesProvider: UserPreferencesProvider
) : ViewModel() {

    private val dmSeenRequests = Channel<Unit>(capacity = Channel.CONFLATED)

    init {
        viewModelScope.launch {
            for (ignored in dmSeenRequests) {
                chatRepository.markChatAsSeen(requireChatThread())
                    .onFailure { onFailure(it) }
            }
        }
    }

    val myUid get() = localRepositoryProvider.myUid

    val otherUid: String = savedStateHandle[EXTRA_CHAT_ID] ?: ""
    val nodeType: String = savedStateHandle[EXTRA_CHAT_NODE] ?: NODE_DM

    data class ChatIdentity(
        val userName: String,
        val userType: Int = PUBLIC_USER,
        val userPhotoUrl: String,
        val fcmToken: String = ""
    )

    private var myIdentity = ChatIdentity(userName = "", userPhotoUrl = "")
    private var otherIdentity = ChatIdentity(userName = "", userPhotoUrl = "")
    val myPhotoUrl: String get() = myIdentity.userPhotoUrl
    private var groupName: String = ""

    // ------------------------------------------------------------------------------------------------------------------------
    private val _events = MutableSharedFlow<ChatSessionUiEvent>()
    val events: SharedFlow<ChatSessionUiEvent> = _events.asSharedFlow()

    // ------------------------------------------------------------------------------------------------------------------------
    private val _headerState = MutableStateFlow<ChatHeaderState>(ChatHeaderState.Loading)

    // ------------------------------------------------------------------------------------------------------------------------
    private var otherProfile: Users? = null

    // ------------------------------------------------------------------------------------------------------------------------
    // Estado de conexión del otro usuario
    val userStatus: StateFlow<UserStatus> = profileRepositoryProvider
        .observeUserStatus(otherUid, nodeType)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatus.Offline)

    // ------------------------------------------------------------------------------------------------------------------------
    // Referencias del chat (para mensajes, storage, etc.)
    private val _chatRefs = MutableStateFlow<ChatThread?>(null)

    // ------------------------------------------------------------------------------------------------------------------------
    private val _chatState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatUiState> = combine(_headerState, _chatState) { header, chat ->
        ChatUiState(header = header, chat = chat)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ChatUiState()
    )
    // ------------------------------------------------------------------------------------------------------------------------

    fun init() {
        viewModelScope.launch {
            _headerState.value = ChatHeaderState.Loading

            groupName = userPreferencesProvider.groupNameFlow.first()

            setupChat()

            startGroupUserAvailability()

            startChatListeners()

            markMessagesAsSeenOnOpen()

            launch {
                userStatus.collect { status ->
                    _headerState.update { current ->
                        val loaded = current as? ChatHeaderState.Loaded ?: return@update current
                        loaded.copy(status = mapStatusToText(status))
                    }
                }
            }
        }
    }

    private fun startGroupUserAvailability() {
        if (nodeType != NODE_DM) {
            viewModelScope.launch {
                groupRepositoryProvider.observeIsUserInGroup(groupName, otherUid)
                    .collect { isAvailable ->
                        if (!isAvailable) {
                            _events.emit(
                                ChatSessionUiEvent.OtherUserNoLongerAvailable(
                                    userName = currentOtherName(),
                                    onConfirm = {
                                        viewModelScope.launch {
                                            _events.emit(ChatSessionUiEvent.CloseChat)
                                        }
                                    }
                                )
                            )
                        }
                    }
            }
        }
    }

    private fun startChatListeners() {
        viewModelScope.launch {
            val refs = requireChatThread()
            chatRepository.observeChatMessages(refs).collect { event ->
                _chatState.update { state -> state.reduce(event, myUid) }

                if (nodeType == NODE_DM) {
                    requestDmSeenSyncIfNeeded(event)
                } else {
                    markIncomingMessageAsSeenIfNeeded(refs, event)
                }
            }
        }
    }

    private fun markMessagesAsSeenOnOpen() {
        if (nodeType == NODE_DM) {
            dmSeenRequests.trySend(Unit)
            return
        }

        viewModelScope.launch {
            chatRepository.markChatAsSeen(requireChatThread())
                .onFailure { onFailure(it) }
        }
    }

    private fun requestDmSeenSyncIfNeeded(event: ChatChildEvent) {
        val item = when (event) {
            is ChatChildEvent.Added -> event.item
            is ChatChildEvent.Changed -> event.item
            is ChatChildEvent.Removed -> return
        }
        val message = item.message
        if (message.senderUid == myUid) return
        if (message.seen >= MSG_SEEN) return
        if (message.isDeletedFor(myUid)) return

        dmSeenRequests.trySend(Unit)
    }

    private suspend fun markIncomingMessageAsSeenIfNeeded(
        refs: ChatThread,
        event: ChatChildEvent
    ) {
        val item = when (event) {
            is ChatChildEvent.Added -> event.item
            is ChatChildEvent.Changed -> event.item
            is ChatChildEvent.Removed -> return
        }

        if (item.message.senderUid == myUid) return
        if (item.message.seen >= MSG_SEEN) return
        if (item.message.isDeletedFor(myUid)) return

        chatRepository.markMessageAsSeenIfNeeded(refs, item.id, item.message)
            .onFailure { onFailure(it) }
    }

    private suspend fun setupChat() {
        _headerState.value = ChatHeaderState.Loading

        _chatRefs.value = chatRepository.chatThread(otherUid, nodeType)

        if (nodeType == NODE_DM) {
            loadChatPublicProfiles()
            applyChatStateForOneToOne()
        } else {
            loadChatFromGroup()
        }
    }

    // Aplica notificaciones / bloqueo solo para chats 1 a 1
    private suspend fun applyChatStateForOneToOne() {

        profileRepositoryProvider.getMyChatState(otherUid)
            .onSuccess { state ->
                _headerState.update { current ->
                    (current as? ChatHeaderState.Loaded)?.copy(
                        notificationsEnabled = state != CHAT_STATE_SILENT,
                        isBlocked = state == CHAT_STATE_BLOCKED
                    ) ?: current
                }
            }
            .onFailure { onFailure(it) }
    }

    private suspend fun loadChatPublicProfiles() {
        val profile = userRepositoryProvider.getAccount(otherUid) ?: return

        otherProfile = profile

        val otherFcmToken = sessionRepositoryProvider.getFcmToken(profile.id) ?: return

        myIdentity = ChatIdentity(
            userName = localRepositoryProvider.myUserName,
            userPhotoUrl = localRepositoryProvider.myProfilePhotoUrl
        )

        otherIdentity = ChatIdentity(
            userName = profile.name,
            userPhotoUrl = profile.photoUrl,
            fcmToken = otherFcmToken
        )

        _headerState.value = ChatHeaderState.Loaded(
            name = currentOtherName(),
            status = textProvider.loading,
            photoUrl = otherIdentity.userPhotoUrl
        )
    }

    private suspend fun loadChatFromGroup() {
        val profile = userRepositoryProvider.getAccount(otherUid) ?: return

        otherProfile = profile

        val otherFcmToken = sessionRepositoryProvider.getFcmToken(profile.id) ?: return

        val defaultPhotoUrl = when (val result = userRepositoryProvider.getDefaultProfilePhotoUrl()) {
            is ZibeResult.Success -> result.data.orEmpty()
            is ZibeResult.Failure -> {
                onFailure(result.exception)
                ""
            }
        }

        val myUserGroup = groupRepositoryProvider.findUserGroup(myUid, groupName)

        val otherUserGroup = groupRepositoryProvider.findUserGroup(otherUid, groupName)

        myIdentity = if (myUserGroup?.type == ANONYMOUS_USER) {
            ChatIdentity(
                userName = myUserGroup.userName,
                userType = ANONYMOUS_USER,
                userPhotoUrl = defaultPhotoUrl
            )
        } else {
            ChatIdentity(
                userName = localRepositoryProvider.myUserName,
                userPhotoUrl = localRepositoryProvider.myProfilePhotoUrl
            )
        }

        otherIdentity = if (otherUserGroup?.type == ANONYMOUS_USER) {
            ChatIdentity(
                userName = otherUserGroup.userName,
                userType = ANONYMOUS_USER,
                userPhotoUrl = defaultPhotoUrl
            )
        } else {
            ChatIdentity(
                userName = profile.name,
                userPhotoUrl = profile.photoUrl,
                fcmToken = otherFcmToken
            )
        }

        _headerState.value = ChatHeaderState.Loaded(
            name = currentOtherName(),
            status = textProvider.loading,
            photoUrl = otherIdentity.userPhotoUrl
        )
    }


    private fun mapStatusToText(status: UserStatus): String =
        when (status) {
            is UserStatus.Online -> textProvider.online
            is UserStatus.TypingOrRecording -> status.text
            is UserStatus.LastSeen -> status.text
            is UserStatus.Offline -> textProvider.offline
        }


    fun onSendMessage(text: String) {
        sendMessage(MSG_TEXT, text)
    }

    fun onTextChanged(text: String) {
        _chatState.update { it.copy(textReady = text.isNotBlank()) }
    }

    fun onPhotoSelected(uri: String) {
        _chatState.update {
            it.copy(
                pendingPhotoUri = uri,
                photoReady = true,
                pendingFileUrl = null
            )
        }
    }

    fun onCroppedPhotoReady(fileName: String, uri: String) {
        onPhotoSelected(uri)
        uploadMedia(fileName, uri, PATH_PHOTOS) { url ->
            if (url == null) {
                clearPendingPhotoState()
                onError(UiText.StringRes(R.string.chat_error_upload_image))
            } else {
                onPhotoReady(url, true)
            }
        }
    }

    fun onRemovePendingPhoto() {
        clearPendingPhotoState()
    }

    fun onMicPressed() {
        _chatState.update { it.copy(isRecording = true) }
    }

    fun onMicReleased() {
        _chatState.update { it.copy(isRecording = false) }
    }

    fun onPhotoPickerShown() {
        _chatState.update { it.copy(showPhotoPicker = false) }
    }


    fun onSendPhoto(url: String) = sendMessage(MSG_PHOTO, url)

    fun onSendAudio(url: String, audioDurationMs: Long) =
        sendMessage(MSG_AUDIO, url, audioDurationMs)

    private fun onPhotoReady(url: String, state: Boolean) {
        _chatState.update {
            it.copy(
                photoReady = state,
                pendingFileUrl = url
            )
        }
    }

    private fun clearPendingPhotoState() {
        _chatState.update {
            it.copy(
                pendingPhotoUri = null,
                photoReady = false,
                pendingFileUrl = null
            )
        }
    }

    // =========================================================================
    //  MENSAJES
    // =========================================================================

    fun uploadMedia(
        fileName: String,
        uri: String,
        path: String,
        onComplete: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val thread = _chatRefs.first { it != null }!!
            chatRepository.uploadMedia(uri, fileName, thread, path)
                .onSuccess { onComplete(it) }
                .onFailure {
                    onComplete(null)
                    onFailure(it)
                }
        }
    }

    private fun sendMessage(
        msgType: Int,
        content: String,
        audioDurationMs: Long = 0L
    ) {
        if (content.isEmpty()) return
        viewModelScope.launch { sendMessageInternal(msgType, content, audioDurationMs) }
    }

    private suspend fun sendMessageInternal(
        msgType: Int,
        content: String,
        audioDurationMs: Long
    ) {

        val lastMessageAt = now()

        val (myMsg, otherMsg) = when (msgType) {
            MSG_PHOTO -> textProvider.sentPhoto to textProvider.receivedPhoto
            MSG_AUDIO -> textProvider.sentAudio to textProvider.receivedAudio
            else -> content to content
        }

        when (
            val result = sendChatMessageUseCase.execute(
                SendChatMessageCommand(
                    senderUid = myUid,
                    receiverUid = otherUid,
                    nodeType = nodeType,
                    messageType = msgType,
                    content = content,
                    audioDurationMs = audioDurationMs,
                    createdAt = lastMessageAt,
                    senderConversationContent = myMsg,
                    receiverConversationContent = otherMsg,
                    receiverName = currentOtherName(),
                    receiverPhotoUrl = otherIdentity.userPhotoUrl,
                    senderName = myIdentity.userName,
                    senderPhotoUrl = myIdentity.userPhotoUrl
                )
            )
        ) {
            is ZibeResult.Failure -> {
                onFailure(result.exception)
                return
            }
            is ZibeResult.Success -> when (result.data) {
                SendChatMessageOutcome.BlockedByRecipient -> {
                    _events.emit(ChatSessionUiEvent.ShowBlockedByOther(currentOtherName()))
                    return
                }
                SendChatMessageOutcome.Sent -> Unit
                null -> {
                    onFailure(IllegalStateException("Missing send message outcome"))
                    return
                }
            }
        }

        _chatState.update {
            it.copy(
                photoReady = false,
                textReady = false,
                pendingFileUrl = null
            )
        }
    }

    fun onError(message: UiText) {
        _events.tryEmit(
            ChatSessionUiEvent.ShowErrorDialog(
                uiText = message
            )
        )
    }

    fun onSendPhotoClicked() {
        viewModelScope.launch {
            val otherChatWith = chatRepository.getConversation(otherUid, myUid, nodeType)
            val otherState = otherChatWith?.state ?: nodeType
            val otherName = otherProfile?.name

            if (otherState == CHAT_STATE_BLOCKED) {
                _events.emit(
                    ChatSessionUiEvent.ShowBlockedByOther(
                        userName = otherName.orEmpty()
                    )
                )
            } else {
                _chatState.update { it.copy(showPhotoPicker = true) }
            }
        }
    }

    fun setUserActivityStatus(status: String) {
        viewModelScope.launch { userRepositoryActions.setUserActivityStatus(status) }
    }

    // --- Acciones de menú ----------

    fun onToggleNotificationsClicked() {

        viewModelScope.launch {
            val chatWith = chatRepository.getConversation(myUid, otherUid, nodeType)
            val currentState = chatWith?.state
            val userName = currentOtherName()

            val newState = if (currentState == CHAT_STATE_SILENT) {
                nodeType // Siempre va a ser !incógnito acá x ahora
            } else {
                CHAT_STATE_SILENT
            }

            conversationOverviewRepository.updateChatState(otherUid, userName, nodeType, newState)
            val isNotificationsSilenced = newState == CHAT_STATE_SILENT
            val enabled = newState != CHAT_STATE_SILENT // UI: enabled = TRUE si NO está en silent

            // Actualizar header
            _headerState.update { current ->
                (current as? ChatHeaderState.Loaded)?.copy(
                    notificationsEnabled = enabled
                ) ?: current
            }

            // Emitir evento para mostrar snack
            _events.emit(
                ChatSessionUiEvent.ShowToggleNotificationSuccess(
                    name = userName,
                    isNotificationsSilenced = isNotificationsSilenced
                )
            )
        }
    }

    fun onToggleBlockClicked() {
        viewModelScope.launch {
            runCatchingPreservingCancellation {
                profileRepositoryActions.toggleBlock(otherUid, otherIdentity.userName)
            }.onSuccess { isBlockedByMe ->
                _headerState.update { current ->
                    val loaded = current as? ChatHeaderState.Loaded ?: return@update current
                    loaded.copy(isBlocked = isBlockedByMe)
                }
                _events.emit(
                    ChatSessionUiEvent.ShowToggleBlockSuccess(
                        otherIdentity.userName,
                        isBlockedByMe
                    )
                )
            }
        }
    }

    fun onMessageSelectionChanged(item: ChatMessageItem, isSelected: Boolean) {
        _chatState.update { s ->
            val set = s.selectedIds.toMutableSet()
            if (isSelected) set.add(item.id) else set.remove(item.id)
            s.copy(selectedIds = set)
        }
    }

    fun clearSelection() {
        _chatState.update { it.copy(selectedIds = emptySet()) }
    }

    private fun requireChatThread(): ChatThread =
        _chatRefs.value ?: error("Chat thread is not initialized")

    fun onDeleteSelectedMessages() {
        val selectedIds = _chatState.value.selectedIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            chatRepository.deleteMessages(
                thread = requireChatThread(),
                selectedIds = selectedIds
            ).onSuccess { deleteResult ->
                val deleteResult = deleteResult ?: return@onSuccess
                val selectedSet = selectedIds.toSet()
                _chatState.update { state ->
                    state.copy(
                        messages = state.messages.filterNot { it.id in selectedSet },
                        selectedIds = emptySet()
                    )
                }
                _events.emit(ChatSessionUiEvent.ShowDeleteMessagesSuccess(deleteResult.deletedCount))
                if (deleteResult.chatRemoved)
                    _events.emit(ChatSessionUiEvent.CloseChat)
            }.onFailure { onFailure(it) }
        }
    }

    fun onConfirmHide() {
        if (isActionLoading()) return
        val userName = currentOtherName()
        viewModelScope.launch {
            if (_chatState.value.messages.isEmpty())
                _events.emit(
                    ChatSessionUiEvent.ShowErrorDialog(
                        UiText.StringRes(R.string.msg_no_messages_to_hide)
                    )
                )
            else
                _events.emit(
                    ChatSessionUiEvent.ConfirmHideChat(
                        name = userName,
                        onConfirm = {
                            setActionLoading(true)
                            hideConversation(otherUid, userName, NODE_DM)
                            setActionLoading(false)
                        }
                    )
                )
        }
    }

    fun onDeleteChoiceMode() {
        if (isActionLoading()) return
        val userName = currentOtherName()
        viewModelScope.launch {
            val chatRefs = chatRepository.chatThread(otherUid, NODE_DM)
            val count = chatRepository.getMessageCount(chatRefs)
            if (_chatState.value.messages.isEmpty())
                _events.emit(
                    ChatSessionUiEvent.ShowErrorDialog(
                        UiText.StringRes(R.string.msg_no_messages_to_delete)
                    )
                )
            else
                _events.emit(
                    ChatSessionUiEvent.DeleteClickedChoiceMode(
                        name = userName,
                        countMessages = count,
                        onConfirm = { shouldDeleteMessages ->
                            if (shouldDeleteMessages) onConfirmDelete(chatRefs, userName)
                            else onConfirmHide()
                        }
                    )
                )
        }
    }

    private fun onConfirmDelete(chatRefs: ChatThread, userName: String) {
        viewModelScope.launch {
            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = userName,
                    onConfirm = {
                        viewModelScope.launch {
                            setActionLoading(true)
                            deleteMessages(chatRefs)
                            setActionLoading(false)
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
        chatRepository.deleteConversationForMe(chatRefs)
            .onSuccess { deleteResult ->
                val deleteResult = deleteResult ?: return@onSuccess
                _chatState.update {
                    it.copy(
                        messages = emptyList(),
                        selectedIds = emptySet()
                    )
                }
                _events.emit(ChatSessionUiEvent.ShowDeleteMessagesSuccess(deleteResult.deletedCount))
            }.onFailure { onFailure(it) }
    }

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

    private fun currentOtherName(): String {
        return if (nodeType == NODE_DM) {
            val fromProfile = otherProfile?.name
            if (!fromProfile.isNullOrBlank()) fromProfile else otherIdentity.userName
        } else {
            otherIdentity.userName
        }
    }

    private var activeThreadJob: Job? = null

    fun onThreadScreenStarted() {
        activeThreadJob?.cancel()
        activeThreadJob = viewModelScope.launch {
            while (true) {
                conversationOverviewRepository.setActiveThread(otherUid, nodeType)
                    .onFailure { onFailure(it) }
                delay(ACTIVE_THREAD_HEARTBEAT_MS)
            }
        }
    }

    fun onThreadScreenStopped() {
        activeThreadJob?.cancel()
        activeThreadJob = null
        viewModelScope.launch {
            conversationOverviewRepository.clearActiveThread()
                .onFailure { onFailure(it) }
        }
    }

    private fun setActionLoading(isActionLoading: Boolean) =
        _chatState.update { it.copy(isActionLoading = isActionLoading) }

    private fun isActionLoading() = _chatState.value.isActionLoading

    private companion object {
        const val ACTIVE_THREAD_HEARTBEAT_MS = 60_000L
    }
}
