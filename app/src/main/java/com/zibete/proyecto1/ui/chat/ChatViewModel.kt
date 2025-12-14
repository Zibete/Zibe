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
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.yalantis.ucrop.UCrop
import com.zibete.proyecto1.R
import com.zibete.proyecto1.data.ChatChildEvent
import com.zibete.proyecto1.data.ChatRefs
import com.zibete.proyecto1.data.ChatRepository
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.ui.constants.Constants.AUTH
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.Constants.MAXCHATSIZE
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.ui.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
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
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    ) : ViewModel() {

    val myUid = userRepository.myUid
    private val user = userRepository.user

    val userId: String = savedStateHandle["userId"] ?: ""
    val nodeType: String = savedStateHandle["nodeType"] ?: NODE_CURRENT_CHAT// 0 = unknown 1 = normal // DEF = ChatWith

    private val groupName = userPreferencesRepository.groupName
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
        .observeUserStatus(userId, nodeType)
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
        if (nodeType != NODE_CURRENT_CHAT) {
            groupRepository.observeGroupUserAvailability(groupName, userId).collect { isAvailable ->
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
            chatRepository.observeChatMessages().collect { event ->
                _chatState.update { state ->
                    when (event) {
                        is ChatChildEvent.Added -> state.copy(
                            messages = (state.messages + event.message).takeLast(MAXCHATSIZE)
                        )

                        is ChatChildEvent.Changed -> state.copy(
                            messages = state.messages.map { if (sameMsg(it, event.message)) event.message else it }
                        )

                        is ChatChildEvent.Removed -> state.copy(
                            messages = state.messages.filterNot { sameMsg(it, event.message) },
                            selectedMessages = state.selectedMessages.filterNot { sameMsg(it, event.message) }.toSet()
                        )
                    }
                }
            }
        }
    }

    private fun sameMsg(a: ChatMessage, b: ChatMessage): Boolean {
        return a.date == b.date && a.sender == b.sender  // ideal: usar key/id del nodo (mejor que date)
    }


    private fun markMessagesAsSeenOnOpen() {
        viewModelScope.launch { chatRepository.markChatAsSeen() }
    }

    private suspend fun setupChat() {
        _headerState.value = ChatHeaderState.Loading

        _chatRefs.value = chatRepository.buildChatRefs(userId, nodeType)

        if (nodeType == NODE_CURRENT_CHAT) {
            loadChatPublicProfiles()
            applyChatStateForOneToOne()
        } else {
            loadChatFromGroup()
        }
    }

    // Aplica notificaciones / bloqueo solo para chats 1 a 1
    private suspend fun applyChatStateForOneToOne() {

        val state = userRepository.getChatStateWith(userId, nodeType)
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
        val userToken: String = ""
    )

    private lateinit var otherProfileNN: Users


    lateinit var myIdentity: ChatIdentity

    lateinit var otherIdentity: ChatIdentity

    private suspend fun loadChatPublicProfiles() {
        val profile = userRepository.getUserProfile(userId) ?: return
        _otherProfile.value = profile

        myIdentity = ChatIdentity(
            userName = user.displayName.orEmpty(),
            userPhotoUrl = user.photoUrl?.toString().orEmpty()
        )

        otherIdentity = ChatIdentity(
            userName = profile.name,
            userPhotoUrl = profile.profilePhoto,
            userToken = profile.token
        )

        _headerState.value = ChatHeaderState.Loaded(
            name = currentOtherName(),
            status = context.getString(R.string.loading),
            photoUrl = otherIdentity.userPhotoUrl
        )
    }


    private suspend fun loadChatFromGroup() {
        val profile = userRepository.getUserProfile(userId) ?: return
        _otherProfile.value = profile

        val myUserGroup = groupRepository.getUserGroup(myUid, groupName)
        val otherUserGroup = groupRepository.getUserGroup(userId, groupName)

        myIdentity = if (myUserGroup?.type == ANONYMOUS_USER) {
            ChatIdentity(
                userName = myUserGroup.userName,
                userType = ANONYMOUS_USER,
                userPhotoUrl = DEFAULT_PROFILE_PHOTO_URL
            )
        } else {
            ChatIdentity(
                userName = user.displayName.orEmpty(),
                userPhotoUrl = user.photoUrl?.toString().orEmpty()
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
                userPhotoUrl = profile.profilePhoto,
                userToken = profile.token
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

            chatRepository.uploadChatData(
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
        timerText: String)
    {

        if (content.isEmpty()) return

        val myChatWith = chatRepository.getChatWith(myUid, userId, nodeType)
        val myState = myChatWith?.state?: nodeType
        val otherChatWith = chatRepository.getChatWith(userId, myUid, nodeType)
        val otherState = otherChatWith?.state?: nodeType
        val otherCountMsgReceivedUnread = otherChatWith?.msgReceivedUnread?: 0

        if (otherState == CHAT_STATE_BLOQ) {
            _events.emit(
                ChatSessionUiEvent.ShowBlockedByOther(
                    userName = currentOtherName()
                )
            )
            return
        }

        val otherActiveChat = chatRepository.getActiveChat()
        val seen = if (otherActiveChat != myUid + nodeType) MSG_DELIVERED else MSG_SEEN
        val now = now()
        val date = if (timerText == "") now else "$now $timerText"

        val chatMessage = ChatMessage(
            content,
            date,
            myUid,
            msgType,
            seen
        )

        chatRepository.pushMessageToChat(chatMessage)

        //CHATWITH

        val (myMsg, otherMsg) = when (msgType) {
            MSG_PHOTO -> {
                context.getString(R.string.photo_send) to context.getString(R.string.photo_received)
            }
            MSG_AUDIO -> context.getString(R.string.audio_send) to context.getString(R.string.audio_received)
            else -> {
                content to content
            }
        }

        val myNewChatWith = ChatWith(
            myMsg,
            date,
            null,
            myUid,
            userId,
            currentOtherName(),
            otherIdentity.userPhotoUrl,
            myState,
            0,
            0
        )

        chatRepository.saveChatWith(myUid,nodeType, userId, myNewChatWith)

        if (otherActiveChat != myUid + nodeType) {

            val countOtherMsgUnread = otherCountMsgReceivedUnread + 1

            val otherNewChatWith = ChatWith(
                otherMsg,
                date,
                null,
                myUid,
                myUid,
                myIdentity.userName,
                myIdentity.userPhotoUrl,
                otherState,
                countOtherMsgUnread,
                1
            )

            chatRepository.saveChatWith(userId, nodeType, myUid, otherNewChatWith)

            if (otherState != CHAT_STATE_SILENT) {
                val body = JSONObject().apply {
                    put("novistos", countOtherMsgUnread)
                    put("user", myIdentity.userName)
                    put("msg", otherMsg)
                    put("id_user", chatMessage.sender)
                    put("type", nodeType)
                }
                val json = JSONObject().apply {
                    put("to", otherProfile.value?.token)
                    put("priority", "high")
                    put("data", body)
                }
                val req = object : JsonObjectRequest(
                    Method.POST, "https://fcm.googleapis.com/fcm/send", json, null, null
                ) {
                    override fun getHeaders(): MutableMap<String, String> = hashMapOf(
                        "content-type" to "application/json",
                        "authorization" to AUTH
                    )
                }
                Volley.newRequestQueue(context).add(req)
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

    suspend fun onError(message: String) {
        _events.emit(ChatSessionUiEvent.ShowErrorDialog(
            message = message
        ))
    }

    fun onPhotoPickerHandled() {
        _chatState.update { it.copy(showPhotoPicker = false) }
    }

    fun onSendPhotoClicked() {
        viewModelScope.launch {
            val otherChatWith = chatRepository.getChatWith(userId, myUid, nodeType)
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

    fun setUserOnline() {
        viewModelScope.launch { userRepository.setUserOnline() }
    }

    fun setUserLastSeen(){
        viewModelScope.launch { userRepository.setUserLastSeen() }
    }

    fun setActiveChat(activeChat: String){
        chatRepository.setActiveChat(activeChat)
    }

    // --- Acciones de menú ----------

    fun onToggleNotificationsClicked() {

        viewModelScope.launch {
            val chatWith = chatRepository.getChatWith(myUid,userId, nodeType)
            val currentState = chatWith?.state
            val userName = currentOtherName()

            val newState = if (currentState == CHAT_STATE_SILENT) {
                nodeType // Siempre va a ser !incógnito acá x ahora
            } else {
                CHAT_STATE_SILENT
            }

            userRepository.updateStateChatWith(userId, userName, nodeType, newState)
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
                        userRepository.updateStateChatWith(userId, userName, nodeType, CHAT_STATE_BLOQ)
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
                        userRepository.updateStateChatWith(userId, userName, nodeType, nodeType)
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

    fun onMessageSelectionChanged(message: ChatMessage, isSelected: Boolean) {
        _chatState.update { current ->
            val currentSet = current.selectedMessages.toMutableSet()
            if (isSelected) {
                currentSet.add(message)
            } else {
                currentSet.remove(message)
            }

            current.copy(
                selectedMessages = currentSet
            )
        }
    }

    fun clearSelection() {
        _chatState.update { it.copy(selectedMessages = emptySet()) }
    }


    fun onDeleteSelectedMessages() {
        val selected = _chatState.value.selectedMessages.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            try {
                val result = chatRepository.deleteMessages(selected)

                _chatState.update {
                    it.copy(
                        selectedMessages = emptySet()
                    )
                }

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
            val count = chatRepository.getMessageCount()
            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = currentOtherName(),
                    countMessages = count,
                    onConfirm = { deleteMessages ->
                        viewModelScope.launch {
                            try {
                                val result = chatRepository.deleteMessages(null, deleteMessages)
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
        return if (nodeType == NODE_CURRENT_CHAT) {
            val fromProfile = _otherProfile.value?.name
            if (!fromProfile.isNullOrBlank()) fromProfile else otherIdentity.userName
        } else {
            otherIdentity.userName
        }
    }

}