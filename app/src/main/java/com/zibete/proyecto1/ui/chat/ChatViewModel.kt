package com.zibete.proyecto1.ui.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yalantis.ucrop.UCrop
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOCKED
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.MAX_CHAT_SIZE
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.core.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.PATH_AUDIOS
import com.zibete.proyecto1.core.constants.Constants.PATH_PHOTOS
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.TimeUtils.now
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.data.ChatRefs
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.model.ChatChildEvent
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.media.buildZibeUcropIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val groupRepository: GroupRepository,
    private val groupRepositoryProvider: GroupRepositoryProvider,
    private val userRepository: UserRepository,
    private val userRepositoryProvider: UserRepositoryProvider,
    private val profileRepositoryProvider: ProfileRepositoryProvider,
    private val profileRepositoryActions: ProfileRepositoryActions,
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
    private val sessionRepositoryProvider: SessionRepositoryProvider,
    private val userPreferencesProvider: UserPreferencesProvider
) : ViewModel() {

    val myUid get() = userRepository.myUid

    val otherUid: String = savedStateHandle[EXTRA_CHAT_ID] ?: ""
    val nodeType: String = savedStateHandle[EXTRA_CHAT_NODE] ?: NODE_DM

//    val partnerId: String get() = otherUid

    data class ChatIdentity(
        val userName: String,
        val userType: Int = PUBLIC_USER,
        val userPhotoUrl: String,
        val fcmToken: String = ""
    )

    lateinit var myIdentity: ChatIdentity
    lateinit var otherIdentity: ChatIdentity

    val groupName: String
        get() = runBlocking { userPreferencesProvider.groupNameFlow.first() }

    // ------------------------------------------------------------------------------------------------------------------------
    private val _events = MutableSharedFlow<ChatSessionUiEvent>()
    val events: SharedFlow<ChatSessionUiEvent> = _events.asSharedFlow()

    // ------------------------------------------------------------------------------------------------------------------------
    private val _headerState = MutableStateFlow<ChatHeaderState>(ChatHeaderState.Loading)
    val headerState: StateFlow<ChatHeaderState> = _headerState.asStateFlow()

    // ------------------------------------------------------------------------------------------------------------------------
    private val _otherProfile = MutableStateFlow<Users?>(null)
    val otherProfile: StateFlow<Users?> = _otherProfile

    // ------------------------------------------------------------------------------------------------------------------------
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    // ------------------------------------------------------------------------------------------------------------------------
    // Estado de conexión del otro usuario
    val userStatus: StateFlow<UserStatus> = profileRepositoryProvider
        .observeUserStatus(otherUid, nodeType)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatus.Offline)

    // ------------------------------------------------------------------------------------------------------------------------
    // Referencias del chat (para mensajes, storage, etc.)
    private val _chatRefs = MutableStateFlow<ChatRefs?>(null)
    val chatRefs: StateFlow<ChatRefs?> = _chatRefs.asStateFlow()

    // ------------------------------------------------------------------------------------------------------------------------
    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState
    // ------------------------------------------------------------------------------------------------------------------------

    fun init() {
        viewModelScope.launch {
            _headerState.value = ChatHeaderState.Loading

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

    private suspend fun startGroupUserAvailability() {
        if (nodeType != NODE_DM) {
            groupRepository.observeIsUserInGroup(groupName, otherUid).collect { isAvailable ->
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

    fun startChatListeners() {
        viewModelScope.launch {
            val refs = requireChatRefs()
            chatRepository.observeChatMessages(refs).collect { event ->
                _chatState.update { state ->
                    when (event) {
                        is ChatChildEvent.Added -> state.copy(
                            messages = (state.messages + event.item).takeLast(MAX_CHAT_SIZE)
                        )

                        is ChatChildEvent.Changed -> state.copy(
                            messages = state.messages.map { if (it.id == event.item.id) event.item else it }
                        )

                        is ChatChildEvent.Removed -> state.copy(
                            messages = state.messages.filterNot { it.id == event.item.id },
                            selectedIds = state.selectedIds - event.item.id
                        )
                    }
                }
            }
        }
    }

    private fun sameMsg(a: ChatMessageItem, b: ChatMessageItem): Boolean {
        return a.id == b.id
    }

    private fun markMessagesAsSeenOnOpen() {
        viewModelScope.launch { chatRepository.markChatAsSeen(requireChatRefs()) }
    }

    private suspend fun setupChat() {
        _headerState.value = ChatHeaderState.Loading

        _chatRefs.value = chatRepository.buildChatRefs(otherUid, nodeType)

        if (nodeType == NODE_DM) {
            loadChatPublicProfiles()
            applyChatStateForOneToOne()
        } else {
            loadChatFromGroup()
        }
    }

    // Aplica notificaciones / bloqueo solo para chats 1 a 1
    private suspend fun applyChatStateForOneToOne() {

        val state = profileRepositoryProvider.getMyChatState(otherUid).getOrThrow()
        val notificationsEnabled = (state != CHAT_STATE_SILENT)
        val isBlocked = (state == CHAT_STATE_BLOCKED)

        _headerState.update { current ->
            (current as? ChatHeaderState.Loaded)?.copy(
                notificationsEnabled = notificationsEnabled,
                isBlocked = isBlocked
            ) ?: current
        }
    }

    private suspend fun loadChatPublicProfiles() {
        val profile = userRepository.getAccount(otherUid) ?: return

        _otherProfile.value = profile

        val otherFcmToken = sessionRepository.getFcmToken(profile.id) ?: return

        myIdentity = ChatIdentity(
            userName = userRepository.myUserName,
            userPhotoUrl = userRepository.myProfilePhotoUrl
        )

        otherIdentity = ChatIdentity(
            userName = profile.name,
            userPhotoUrl = profile.photoUrl,
            fcmToken = otherFcmToken
        )

        _headerState.value = ChatHeaderState.Loaded(
            name = currentOtherName(),
            status = context.getString(R.string.loading),
            photoUrl = otherIdentity.userPhotoUrl
        )
    }

    private suspend fun loadChatFromGroup() {
        val profile = userRepositoryProvider.getAccount(otherUid) ?: return

        _otherProfile.value = profile

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
                userName = userRepository.myUserName,
                userPhotoUrl = userRepository.myProfilePhotoUrl
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
            status = context.getString(R.string.loading),
            photoUrl = otherIdentity.userPhotoUrl
        )
    }


    private fun mapStatusToText(status: UserStatus): String =
        when (status) {
            is UserStatus.Online -> context.getString(R.string.online)
            is UserStatus.TypingOrRecording -> status.text
            is UserStatus.LastSeen -> status.text
            is UserStatus.Offline -> context.getString(R.string.offline)
        }


    fun startUCropFlow(
        sourceUri: Uri,
        activity: AppCompatActivity,
        uCropLauncher: ActivityResultLauncher<Intent>
    ) {
        uCropLauncher.launch(buildZibeUcropIntent(activity, sourceUri))
    }

    fun handleCroppedImageResult(
        resultCode: Int,
        fileName: String?,
        data: Intent?
    ) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val resultUri = UCrop.getOutput(data)

            if (resultUri == null || fileName == null) {
                clearPendingPhotoState()
                viewModelScope.launch {
                    _events.emit(
                        ChatSessionUiEvent.ShowErrorDialog(
                            uiText = UiText.StringRes(R.string.chat_error_get_cropped_image)
                        )
                    )
                }
                return
            }

            viewModelScope.launch {
                onPhotoSelected(resultUri)
                val url = uploadMedia(
                    fileName = fileName,
                    uri = resultUri,
                    path = PATH_PHOTOS
                )

                if (url == null) {
                    clearPendingPhotoState()
                    _events.emit(
                        ChatSessionUiEvent.ShowErrorDialog(
                            uiText = UiText.StringRes(R.string.chat_error_upload_image)
                        )
                    )
                } else {
                    onPhotoReady(url, true)
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            clearPendingPhotoState()
            val error = UCrop.getError(data)
            Log.e("UCrop", "Error al recortar: $error")
            viewModelScope.launch {
                _events.emit(
                    ChatSessionUiEvent.ShowErrorDialog(
                        uiText = UiText.StringRes(R.string.chat_error_crop_image)
                    )
                )
            }
        } else {
            clearPendingPhotoState()
        }
    }

    fun onSendMessage() {
        val state = _chatState.value
        viewModelScope.launch {
            when {
                state.photoReady && state.pendingFileUrl != null -> {
                    onSendPhoto(state.pendingFileUrl)
                }

                state.textReady -> {
                    // This assumes there's a mechanism to get the current text, 
                    // or it uses a stored text value. 
                    // For now, let's assume we need to handle this properly.
                }
            }
        }
    }

    fun onSendMessage(text: String) {
        viewModelScope.launch {
            onSendText(text)
        }
    }

    fun onTextChanged(text: String) {
        _chatState.update { it.copy(textReady = text.isNotBlank()) }
    }

    fun onCameraClicked() {
        // Implementation
    }

    fun onAttachClicked() {
        onSendPhotoClicked()
    }

    fun onPhotoSelected(uri: Uri) {
        _chatState.update {
            it.copy(
                pendingPhotoUri = uri,
                photoReady = true,
                pendingFileUrl = null
            )
        }
    }

    fun onRemovePendingPhoto() {
        clearPendingPhotoState()
    }

    fun onMicPressed() {
        _chatState.update { it.copy(isRecording = true) }
    }

    fun onMicMoved(x: Float, y: Float, width: Float) {
        // Implementation
    }

    fun onMicReleased() {
        _chatState.update { it.copy(isRecording = false) }
    }

    fun onPhotoPickerShown() {
        _chatState.update { it.copy(showPhotoPicker = false) }
    }


    suspend fun onSendText(text: String) {
        sendMessage(
            msgType = MSG_TEXT,
            content = text
        )
    }

    suspend fun onSendPhoto(url: String) {
        sendMessage(
            msgType = MSG_PHOTO,
            content = url
        )
    }

    suspend fun onSendAudio(url: String, audioDurationMs: Long) {
        sendMessage(
            msgType = MSG_AUDIO,
            content = url,
            audioDurationMs = audioDurationMs
        )
    }

    fun onTextReady(state: Boolean) {
        _chatState.update {
            it.copy(
                textReady = state
            )
        }
    }

    fun onPhotoReady(url: String, state: Boolean) {
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

    suspend fun uploadMedia(fileName: String, uri: Uri, path: String): String? {
        return try {
            val refs = chatRefs.first { it != null }!!

            val refData = when (path) {
                PATH_AUDIOS -> refs.refAudios
                PATH_PHOTOS -> refs.refPhotos
                else -> throw IllegalArgumentException(context.getString(R.string.err_zibe))
            }

            chatRepository.uploadMedia(
                uri,
                fileName,
                refData
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun sendMessage(
        msgType: Int,
        content: String,
        audioDurationMs: Long = 0L
    ) {
        if (content.isEmpty()) return

        val myConversation = chatRepository.getConversation(myUid, otherUid, nodeType)
        val myState = myConversation?.state ?: nodeType

        val otherConversation = chatRepository.getConversation(otherUid, myUid, nodeType)
        val otherState = otherConversation?.state ?: nodeType
        val otherCountMsgReceivedUnread = otherConversation?.unreadCount ?: 0

        if (otherState == CHAT_STATE_BLOCKED) {
            _events.emit(
                ChatSessionUiEvent.ShowBlockedByOther(
                    userName = currentOtherName()
                )
            )
            return
        }

        val lastMessageAt = now()

        val chatMessage = ChatMessage(
            content = content,
            createdAt = lastMessageAt,
            audioDurationMs = audioDurationMs,
            senderUid = myUid,
            type = msgType,
            seen = MSG_DELIVERED
        )

        val chatRefs = requireChatRefs()

        // 1) Mensaje (esto dispara el trigger en Cloud Functions)
        chatRepository.pushMessageToChat(chatRefs, chatMessage)

        // 2) Texto resumen para Conversation
        val (myMsg, otherMsg) = when (msgType) {
            MSG_PHOTO -> context.getString(R.string.photo_send) to context.getString(R.string.photo_received)
            MSG_AUDIO -> context.getString(R.string.audio_send) to context.getString(R.string.audio_received)
            else -> content to content
        }

        // 3) Conversations
        val myNewChatWith = Conversation(
            lastContent = myMsg,
            lastMessageAt = lastMessageAt,
            userId = myUid,
            otherId = otherUid,
            otherName = currentOtherName(),
            otherPhotoUrl = otherIdentity.userPhotoUrl,
            state = myState,
            unreadCount = 0,
            seen = MSG_DELIVERED
        )
        chatRepository.saveConversation(myUid, nodeType, otherUid, myNewChatWith)

        val otherNewConversation = Conversation(
            lastContent = otherMsg,
            lastMessageAt = lastMessageAt,
            userId = myUid,
            otherId = myUid,
            otherName = myIdentity.userName,
            otherPhotoUrl = myIdentity.userPhotoUrl,
            state = otherState,
            unreadCount = otherCountMsgReceivedUnread + 1
        )
        chatRepository.saveConversation(otherUid, nodeType, myUid, otherNewConversation)

        _chatState.update {
            it.copy(
                photoReady = false,
                textReady = false,
                pendingFileUrl = null
            )
        }
    }

    suspend fun onError(message: UiText) {
        _events.emit(
            ChatSessionUiEvent.ShowErrorDialog(
                uiText = message
            )
        )
    }

    fun onPhotoPickerHandled() {
        _chatState.update { it.copy(showPhotoPicker = false) }
    }

    fun onSendPhotoClicked() {
        viewModelScope.launch {
            val otherChatWith = chatRepository.getConversation(otherUid, myUid, nodeType)
            val otherState = otherChatWith?.state ?: nodeType
            val otherName = otherProfile.value?.name

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
        viewModelScope.launch { userRepository.setUserActivityStatus(status) }
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

            userRepository.updateChatState(otherUid, userName, nodeType, newState)
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
            runCatching {
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

    private fun requireChatRefs(): ChatRefs =
        _chatRefs.value ?: error("ChatRefs not initialized. Call setChatContext() first.")

    fun onDeleteSelectedMessages() {
        val selectedIds = _chatState.value.selectedIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            chatRepository.deleteMessages(
                chatRefs = requireChatRefs(),
                selectedIds = selectedIds
            ).onSuccess { deleteResult ->
                val deleteResult = deleteResult ?: return@onSuccess
                _chatState.update { it.copy(selectedIds = emptySet()) }
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
            val chatRefs = chatRepository.buildChatRefs(otherUid, NODE_DM)
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

    private fun onConfirmDelete(chatRefs: ChatRefs, userName: String) {
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
        userRepository.updateChatState(
            userId,
            userName,
            nodeType,
            CHAT_STATE_HIDE
        ).onSuccess {
            _events.emit(ChatSessionUiEvent.ShowChatHiddenSuccess(userName))
        }.onFailure { onFailure(it) }
    }

    private suspend fun deleteMessages(chatRefs: ChatRefs) {
        chatRepository.deleteMessages(
            chatRefs = chatRefs,
            selectedIds = null
        ).onSuccess { deleteResult ->
            val deleteResult = deleteResult ?: return@onSuccess
            _events.emit(ChatSessionUiEvent.ShowDeleteMessagesSuccess(deleteResult.deletedCount))
        }.onFailure { onFailure(it) }
    }

    suspend fun onFailure(e: Throwable) {
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
            val fromProfile = _otherProfile.value?.name
            if (!fromProfile.isNullOrBlank()) fromProfile else otherIdentity.userName
        } else {
            otherIdentity.userName
        }
    }

    fun onThreadScreenStarted() {
        viewModelScope.launch {
            userRepository.setActiveThread(
                otherUid = otherUid,
                nodeType = nodeType
            )
        }
    }

    fun onThreadScreenStopped() {
        viewModelScope.launch {
            userRepository.clearActiveThread()
        }
    }

    private fun setActionLoading(isActionLoading: Boolean) =
        _chatState.update { it.copy(isActionLoading = isActionLoading) }

    private fun isActionLoading() = _chatState.value.isActionLoading
}
