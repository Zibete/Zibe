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
import com.zibete.proyecto1.data.ChatRefs
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.model.ChatChildEvent
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.ui.constants.Constants.MAX_CHAT_SIZE
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.ui.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.ui.constants.Constants.NODE_DM
import com.zibete.proyecto1.ui.constants.Constants.PATH_AUDIOS
import com.zibete.proyecto1.ui.constants.Constants.PATH_PHOTOS
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.ui.constants.ERR_ZIBE
import com.zibete.proyecto1.utils.Utils.now
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
import java.io.File
import javax.inject.Inject
import kotlin.collections.filterNot
import kotlin.collections.map

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val myUid get() = userRepository.myUid

    val otherUid: String = savedStateHandle[EXTRA_CHAT_ID] ?: ""
    val nodeType: String = savedStateHandle[EXTRA_CHAT_NODE] ?: NODE_DM

    val groupName: String
        get() = runBlocking { userPreferencesRepository.groupNameFlow.first() }

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
    val userStatus: StateFlow<UserStatus> = userRepository
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

    init {
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

    private fun sameMsg(a: ChatMessage, b: ChatMessage): Boolean {
        return a.date == b.date && a.senderUid == b.senderUid  // ideal: usar key/id del nodo (mejor que date)
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

        val state = userRepository.getChatStateWith(otherUid, nodeType)
        val notificationsEnabled = (state != CHAT_STATE_SILENT)
        val isBlocked = (state == CHAT_STATE_BLOQ)

        _headerState.update { current ->
            (current as? ChatHeaderState.Loaded)?.copy(
                notificationsEnabled = notificationsEnabled,
                isBlocked = isBlocked
            ) ?: current
        }
    }

    data class ChatIdentity(
        val userName: String,
        val userType: Int = PUBLIC_USER,
        val userPhotoUrl: String,
        val fcmToken: String = ""
    )

    lateinit var myIdentity: ChatIdentity
    lateinit var otherIdentity: ChatIdentity

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
        val profile = userRepository.getAccount(otherUid) ?: return

        _otherProfile.value = profile

        val otherFcmToken = sessionRepository.getFcmToken(profile.id) ?: return

        val myUserGroup = groupRepository.getUserGroup(myUid, groupName)

        val otherUserGroup = groupRepository.getUserGroup(otherUid, groupName)

        myIdentity = if (myUserGroup?.type == ANONYMOUS_USER) {
            ChatIdentity(
                userName = myUserGroup.userName,
                userType = ANONYMOUS_USER,
                userPhotoUrl = DEFAULT_PROFILE_PHOTO_URL
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
                userPhotoUrl = DEFAULT_PROFILE_PHOTO_URL
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
        val destinationUri = Uri.fromFile(
            File(context.cacheDir, "${System.currentTimeMillis()}_cropped.jpg")
        )

        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .getIntent(activity)

        uCropLauncher.launch(uCropIntent)
    }

    fun handleCroppedImageResult(
        resultCode: Int,
        fileName: String?,
        data: Intent?
    ) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val resultUri = UCrop.getOutput(data)

            if (resultUri == null || fileName == null) {
                viewModelScope.launch {
                    _events.emit(
                        ChatSessionUiEvent.ShowErrorDialog(
                            message = "Error al obtener la imagen recortada"
                        )
                    )
                }
                return
            }

            viewModelScope.launch {
                val url = uploadMedia(
                    fileName = fileName,
                    uri = resultUri,
                    path = PATH_PHOTOS
                )

                if (url == null) {
                    _events.emit(
                        ChatSessionUiEvent.ShowErrorDialog(
                            message = "No se pudo subir la imagen"
                        )
                    )
                } else {
                    onPhotoReady(url, true)
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            val error = UCrop.getError(data)
            Log.e("UCrop", "Error al recortar: $error")
            viewModelScope.launch {
                _events.emit(
                    ChatSessionUiEvent.ShowErrorDialog(
                        message = "Fallo al recortar la imagen."
                    )
                )
            }
        }
    }

    suspend fun onSendMessage(text: String) {
        val state = _chatState.value

        when {
            state.photoReady && state.pendingFileUrl != null -> {
                onSendPhoto(state.pendingFileUrl)
            }

            state.textReady -> {
                onSendText(text)
            }
        }
    }


    suspend fun onSendText(text: String) {
        sendMessage(
            msgType = MSG_TEXT,
            content = text,
            timerText = ""
        )
    }

    suspend fun onSendPhoto(url: String) {
        sendMessage(
            msgType = MSG_PHOTO,
            content = url,
            timerText = ""
        )
    }

    suspend fun onSendAudio(url: String, duration: String) {
        sendMessage(
            msgType = MSG_AUDIO,
            content = url,
            timerText = duration
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

    // =========================================================================
    //  MENSAJES
    // =========================================================================

    suspend fun uploadMedia(fileName: String, uri: Uri, path: String): String? {
        return try {
            val refs = chatRefs.first { it != null }!!

            val refData = when (path) {
                PATH_AUDIOS -> refs.refAudios
                PATH_PHOTOS -> refs.refPhotos
                else -> throw IllegalArgumentException(ERR_ZIBE)
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
        timerText: String
    ) {
        if (content.isEmpty()) return

        val myChatWith = chatRepository.getConversation(myUid, otherUid, nodeType)
        val myState = myChatWith?.state ?: nodeType

        val otherChatWith = chatRepository.getConversation(otherUid, myUid, nodeType)
        val otherState = otherChatWith?.state ?: nodeType
        val otherCountMsgReceivedUnread = otherChatWith?.unreadCount ?: 0

        if (otherState == CHAT_STATE_BLOQ) {
            _events.emit(
                ChatSessionUiEvent.ShowBlockedByOther(
                    userName = currentOtherName()
                )
            )
            return
        }

        val now = now()

        val date = if (timerText.isBlank()) now else "$now $timerText"

        val chatMessage = ChatMessage(
            content = content,
            date = date,
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
            lastDate = date,
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
            lastDate = date,
            userId = myUid,
            otherId = myUid,
            otherName = myIdentity.userName,
            otherPhotoUrl = myIdentity.userPhotoUrl,
            state = otherState,
            unreadCount = otherCountMsgReceivedUnread + 1
        )
        chatRepository.saveConversation(otherUid, nodeType,myUid,otherNewConversation)

        _chatState.update {
            it.copy(
                photoReady = false,
                textReady = false,
                pendingFileUrl = null
            )
        }
    }

    suspend fun onError(message: String) {
        _events.emit(
            ChatSessionUiEvent.ShowErrorDialog(
                message = message
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

            if (otherState == CHAT_STATE_BLOQ) {
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

            userRepository.updateStateChatWith(otherUid, userName, nodeType, newState)
            val enabled = newState != CHAT_STATE_SILENT // UI: enabled = TRUE si NO está en silent

            // Actualizar header
            _headerState.update { current ->
                (current as? ChatHeaderState.Loaded)?.copy(
                    notificationsEnabled = enabled
                ) ?: current
            }

            // Emitir evento para mostrar snack
            _events.emit(ChatSessionUiEvent.ShowToggleNotificationSuccess(userName, enabled))
        }
    }

    fun onBlockClicked() {
        viewModelScope.launch {
            val userName = currentOtherName()
            _events.emit(
                ChatSessionUiEvent.ConfirmBlock(
                    name = userName,
                    onConfirm = {
                        userRepository.updateStateChatWith(
                            otherUid,
                            userName,
                            nodeType,
                            CHAT_STATE_BLOQ
                        )
                        _events.emit(ChatSessionUiEvent.ShowBlockSuccess(userName))
                        _headerState.update { current ->
                            (current as? ChatHeaderState.Loaded)?.copy(
                                isBlocked = true
                            ) ?: current
                        }
                    }
                )
            )
        }
    }

    fun onUnblockClicked() {
        viewModelScope.launch {
            val userName = currentOtherName()
            _events.emit(
                ChatSessionUiEvent.ConfirmUnblock(
                    name = userName,
                    onConfirm = {
                        userRepository.updateStateChatWith(otherUid, userName, nodeType, nodeType)
                        _events.emit(ChatSessionUiEvent.ShowUnblockSuccess(userName))
                        _headerState.update { current ->
                            (current as? ChatHeaderState.Loaded)?.copy(
                                isBlocked = false
                            ) ?: current
                        }
                    }
                )
            )
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
            try {
                val result = chatRepository.deleteMessages(
                    chatRefs = requireChatRefs(),
                    selectedIds = selectedIds,
                    deleteMessages = true
                )

                _chatState.update { it.copy(selectedIds = emptySet()) }

                _events.emit(ChatSessionUiEvent.ShowDeleteMessagesSuccess(result.deletedCount))

                if (result.chatRemoved) {
                    _events.emit(ChatSessionUiEvent.CloseChat)
                }
            } catch (e: Exception) {
                _events.emit(
                    ChatSessionUiEvent.ShowErrorDialog(
                        e.message ?: "Error al eliminar mensajes"
                    )
                )
            }
        }
    }

    fun onDeleteChatClicked() {
        viewModelScope.launch {
            val count = chatRepository.getMessageCount(requireChatRefs())
            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = currentOtherName(),
                    countMessages = count,
                    onConfirm = { deleteMessages ->
                        viewModelScope.launch {
                            try {
                                val result = chatRepository.deleteMessages(
                                    requireChatRefs(),
                                    null,
                                    deleteMessages
                                )
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

}