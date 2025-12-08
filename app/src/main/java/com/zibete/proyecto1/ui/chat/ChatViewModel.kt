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
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.chat.ChatActivity.Companion.yourPhoto
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.constants.Constants.AUTH
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.ui.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT
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
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    ) : ViewModel() {

    private val myUid = userRepository.myUid
    private val user = userRepository.user


    private val userId: String = savedStateHandle["userId"] ?: ""
    private val nodeType: String = savedStateHandle["nodeType"] ?: NODE_CURRENT_CHAT// 0 = unknown 1 = normal // DEF = ChatWith
    private val userName: String = savedStateHandle["userName"] ?: ""
    private val groupName = userPreferencesRepository.groupName
    // ------------------------------------------------------------------------------------------------------------------------
//    private val _chatEvents = MutableSharedFlow<ChatUiEvent>()
//    val chatEvents: SharedFlow<ChatUiEvent> = _chatEvents.asSharedFlow()
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

            // Opcional: si querés que el header.status se actualice con userStatus
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
            chatRepository
                .observeGroupUserAvailability(groupName, userId)
                .collect { isAvailable ->
                    if (!isAvailable) {
                        _events.emit(
                            ChatSessionUiEvent.OtherUserNoLongerAvailable(
                                userName = userName,
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

    private fun startChatListeners() {
        viewModelScope.launch {
            // Esperamos a tener refs listos
            val refs = chatRefs.first { it != null }!!

            chatRepository
                .observeChatMessages(refs.refChatId)
                .collect { event ->
                    when (event) {
                        is ChatChildEvent.Added   -> addChat(event.snapshot)
                        is ChatChildEvent.Changed -> updateChat(event.snapshot)
                        is ChatChildEvent.Removed -> deleteChat(event.snapshot)
                    }
                }
        }
    }

    private fun markMessagesAsSeenOnOpen() {
        viewModelScope.launch {
            val chatRefs = chatRefs.first { it != null }!!

            chatRepository.markChatAsSeen(
                userId = userId,
                nodeType = nodeType,
                refChatId = chatRefs.refChatId
            )
        }
    }


    private suspend fun setupChat() {
        _headerState.value = ChatHeaderState.Loading

        _otherProfile.value = userRepository.getUserProfile(userId)

        _chatRefs.value = chatRepository.buildChatRefs(userId, nodeType)

        if (nodeType == NODE_CURRENT_CHAT) {
            // Chat 1 a 1
            loadChatPublicProfiles()
            applyChatStateForOneToOne()
        } else {
            // Chat desde grupo / anonymous
            loadChatFromGroup()
            // Por ahora no manejamos notifs/bloqueo para chatList_group
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

    data class MyChatIdentity(
        val name: String?,
        val type: Int,
        val photoUrl: String?
    )

    private lateinit var myIdentity: MyChatIdentity

    private fun loadChatPublicProfiles() {

        _headerState.value = ChatHeaderState.Loaded(
            name = otherProfile.value?.name,
            status = context.getString(R.string.cargando),
            photoUrl = otherProfile.value?.profilePhoto
        )

        myIdentity = MyChatIdentity(
            name = user.displayName,
            type = 1,
            photoUrl = user.photoUrl.toString()
        )
    }

    private suspend fun loadChatFromGroup() {

        val otherUserGroup = groupRepository.getUserGroup(userId, groupName)

        _headerState.value = ChatHeaderState.Loaded(
            name = otherUserGroup?.userName,
            status = context.getString(R.string.cargando),
            photoUrl = DEFAULT_PROFILE_PHOTO_URL
        )

        val myUserGroup = groupRepository.getUserGroup(myUid, groupName)
        val myType = myUserGroup?.type


        myIdentity = if (myType == 0) {
            MyChatIdentity(
                name = myUserGroup.userName,
                type = 0,
                photoUrl = DEFAULT_PROFILE_PHOTO_URL
            )
        } else {
            MyChatIdentity(
                name = user.displayName,
                type = 1,
                photoUrl = user.photoUrl.toString()
            )
        }
    }


    private fun mapStatusToText(status: UserStatus): String =
        when (status) {
            is UserStatus.Online -> context.getString(R.string.online)
            is UserStatus.TypingOrRecording -> status.text   // ya viene formateado
            is UserStatus.LastSeen -> status.text            // ya viene formateado
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
                    path = "images"
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
            val refOtherReceiverData = refs.refOtherReceiverData

            chatRepository.uploadChatData(
                uri,
                fileName,
                path,
                refOtherReceiverData)
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
                    userName = userName
                )
            )
            return
        }

        val otherActiveChat = chatRepository.getActiveChat(userId, nodeType)
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

        val refs = chatRefs.first { it != null }!!
        chatRepository.pushMessageToChat(refs.refChatId, chatMessage)

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
            userName,
            yourPhoto,
            myState,
            0,
            0
        )

        chatRepository.saveChatWith(myUid,nodeType, userId, myNewChatWith)

        if (otherActiveChat != myUid + nodeType) {

            val countOtherMsgUnread = otherCountMsgReceivedUnread + 1

            val me = myIdentity

            val otherNewChatWith = ChatWith(
                otherMsg,
                date,
                null,
                myUid,
                myUid,
                me.name,
                me.photoUrl,
                otherState,
                countOtherMsgUnread,
                1
            )

            chatRepository.saveChatWith(userId,nodeType, myUid, otherNewChatWith)

            if (otherState != CHAT_STATE_SILENT) {
                val body = JSONObject().apply {
                    put("novistos", countOtherMsgUnread)
                    put("user", me.name)
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
                pendingFileUrl = null,
                textReady = false,
                pendingTextMessage = null
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
            val otherName = otherProfile.value?.name ?: ""

            if (otherState == CHAT_STATE_BLOQ) {
                _events.emit(
                    ChatSessionUiEvent.ShowBlockedByOther(
                        userName = otherName
                    )
                )
            } else {
                _chatState.update { it.copy(showPhotoPicker = true) }
            }
        }
    }


    fun deleteSelectedMsgs() = viewModelScope.launch {
        // TODO: implementar delete de mensajes
    }

    fun onMessageTyping(isTyping: Boolean) {
        // TODO: actualizar estado "escribiendo" en Firebase
    }

    // ---------- Acciones de menú ----------

    fun onToggleNotificationsClicked(userId: String, userName: String, nodeType : String) {

        viewModelScope.launch {
            val chatWith = userRepository.getChatWith(userId, nodeType)

            val currentState = chatWith?.state

            val newState = if (currentState == CHAT_STATE_SILENT) {
                nodeType // Siempre va a ser ChatWith acá x ahora
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

    fun onBlockClicked(userId: String, userName: String, nodeType : String) {
        viewModelScope.launch {
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

    fun onUnblockClicked(userId: String, userName: String, nodeType : String) {
        viewModelScope.launch {
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

    fun onDeleteClicked(userId: String, userName: String, nodeType: String) {
        viewModelScope.launch {
            val count = userRepository.getMessageCount(userId, nodeType)
            _events.emit(
                ChatSessionUiEvent.ConfirmDeleteChat(
                    name = userName,
                    countMessages = count,
                    onConfirm = { deleteMessages ->
                        viewModelScope.launch {
                            userRepository.deleteChat(userId, userName, nodeType, deleteMessages)
                            _events.emit(ChatSessionUiEvent.ShowDeleteChatSuccess(userName))
                        }
                    }
                )
            )
        }
    }



    fun triggerProfileView(context: Context) {
        // TODO: navegación a pantalla de perfil
    }


}
