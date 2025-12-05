package com.zibete.proyecto1.ui.chat

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
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatRefs
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATS
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_ANONYMOUS_GROUP_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_MESSAGES
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository
) : ViewModel() {

    private val myUid = userRepository.myUid
    private val userId: String = savedStateHandle["userId"]?: ""
    private val nodeType: String = savedStateHandle["nodeType"]?: NODE_CURRENT_CHAT // 0 = unknown 1 = normal // DEF = ChatWith
    private val groupName = userPreferencesRepository.groupName


    private val _chatEvents = MutableSharedFlow<ChatUiEvent>()
    val chatEvents: SharedFlow<ChatUiEvent> = _chatEvents.asSharedFlow()

    private val _events = MutableSharedFlow<ChatSessionUiEvent>()
    val events: SharedFlow<ChatSessionUiEvent> = _events.asSharedFlow()


    // Header (nombre, estado, foto, bloqueo, notifs, etc.)
    private val _headerState = MutableStateFlow<ChatHeaderState>(ChatHeaderState.Loading)
    val headerState: StateFlow<ChatHeaderState> = _headerState.asStateFlow()

    // Estado de conexión del otro usuario
    val userStatus: StateFlow<UserStatus> = userRepository
        .observeUserStatus(userId, "chatWith")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatus.Offline)

    // Referencias del chat (para mensajes, storage, etc.)
    private val _chatRefs = MutableStateFlow<ChatRefs?>(null)
    val chatRefs: StateFlow<ChatRefs?> = _chatRefs.asStateFlow()

    // Eventos de UI (snackbar, diálogos, navegación, etc.)

    init {
        viewModelScope.launch {
            loadOtherProfile()
            setupChat()
        }
    }

    private val _otherProfile = MutableStateFlow<Users?>(null)
    val otherProfile: StateFlow<Users?> = _otherProfile

    suspend fun loadOtherProfile() {
        _otherProfile.value = userRepository.getUserProfile(userId)
    }


    // =========================================================================
    //  SETUP INICIAL
    // =========================================================================

    private suspend fun setupChat() = withContext(Dispatchers.IO) {
        _headerState.value = ChatHeaderState.Loading

        if (nodeType == NODE_CURRENT_CHAT) {
            // Chat 1 a 1
            loadOneToOneChat()

            // Una vez que tenemos header + refs, traemos estado de notifs/bloqueo
            val chatType = _chatRefs.value?.refChatWith
            if (chatType != null) {
                val state = userRepository.getChatStateWith(userId, chatType)
                val notificationsEnabled = (state != CHAT_STATE_SILENT)
                val isBlocked = (state == CHAT_STATE_BLOQ)

                _headerState.update { current ->
                    (current as? ChatHeaderState.Loaded)?.copy(
                        notificationsEnabled = notificationsEnabled,
                        isBlocked = isBlocked
                    ) ?: current
                }
            }

        } else { //(nodeType == NODE_CHATWITHUNKNOWN) {
            // Chat desconocido (grupo / anonymous)
            loadUnknownChat()
            // Por ahora no manejamos notifs/bloqueo para unknown
        }
    }

    private fun loadOneToOneChat() {

        val name = otherProfile.value?.name
        val profilePhoto = otherProfile.value?.profilePhoto
        val token = otherProfile.value?.token

        _headerState.value = ChatHeaderState.Loaded(
            name = name,
            status = context.getString(R.string.offline),
            photoUrl = profilePhoto,
            isConnected = true
        )

        _chatRefs.value = ChatRefs(
            startedByMe = firebaseRefsContainer.refChatsRoot
                .child(NODE_CHATS)
                .child("$myUid <---> $userId")
                .child(NODE_MESSAGES),
            startedByHim = firebaseRefsContainer.refChatsRoot
                .child(NODE_CHATS)
                .child("$userId <---> $myUid")
                .child(NODE_MESSAGES),
            refYourReceiverData = firebaseRefsContainer.storage.reference.child("$NODE_CURRENT_CHAT/$userId/"),
            refMyReceiverData = firebaseRefsContainer.storage.reference.child("$NODE_CURRENT_CHAT/$myUid/"),
            refActual = firebaseRefsContainer.refDatos
                .child(myUid)
                .child(NODE_CHATLIST)
                .child("Actual"),
            token = token,
            refChat = NODE_CHATS,
            refChatWith = NODE_CURRENT_CHAT // Antes estaba como "CHATWITH" (Mayus)
        )
    }

    private suspend fun loadUnknownChat() { // No sé quién es, no tendrá ft nunca

        // EL TYPE SIEMPRE SERÁ 0
        // SIEMPRE VENGO ACA DESDE UN GRUPO o profile
        // NUNCA TENDRÁ FOTO

        val userGroup = userRepository.getUserGroup(userId, groupName)

        _headerState.value = ChatHeaderState.Loaded(
            name = userGroup?.userName,
            status = context.getString(R.string.offline),
            photoUrl = DEFAULT_PROFILE_PHOTO_URL,
            isConnected = true
        )

        _chatRefs.value = ChatRefs(
            startedByMe = firebaseRefsContainer.refChatsRoot
                .child(NODE_ANONYMOUS_GROUP_CHAT)
                .child("$myUid <---> $userId")
                .child(NODE_MESSAGES),
            startedByHim = firebaseRefsContainer.refChatsRoot
                .child(NODE_ANONYMOUS_GROUP_CHAT)
                .child("$userId <---> $myUid")
                .child(NODE_MESSAGES),
            refYourReceiverData = firebaseRefsContainer.storage.reference.child("$NODE_ANONYMOUS_GROUP_CHAT/$userId/"),
            refMyReceiverData = firebaseRefsContainer.storage.reference.child("$NODE_ANONYMOUS_GROUP_CHAT/$myUid/"),
            refActual = firebaseRefsContainer.refDatos
                .child(myUid)
                .child(NODE_CHATLIST)
                .child("Actual"),
            token = null,
            refChat = NODE_ANONYMOUS_GROUP_CHAT,
            refChatWith = NODE_ANONYMOUS_GROUP_CHAT
        )
    }

    // =========================================================================
    //  MEDIA: Imágenes (uCrop) y Audio (placeholders)
    // =========================================================================

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
        data: Intent?,
        activity: AppCompatActivity
    ) {
        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val resultUri = UCrop.getOutput(data)
            if (resultUri != null) {
                viewModelScope.launch {
                    _chatEvents.emit(
                        ChatUiEvent.ShowSnackbar(
                            "Imagen lista para enviar.",
                            ZibeSnackType.SUCCESS
                        )
                    )
                }
                // TODO: subir a Storage y preparar sendMessage con PHOTO.
            } else {
                viewModelScope.launch {
                    _chatEvents.emit(
                        ChatUiEvent.ShowSnackbar(
                            "Error al obtener la imagen recortada.",
                            ZibeSnackType.ERROR
                        )
                    )
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            val error = UCrop.getError(data)
            Log.e("UCrop", "Error al recortar: $error")
            viewModelScope.launch {
                _chatEvents.emit(
                    ChatUiEvent.ShowSnackbar(
                        "Fallo al recortar la imagen.",
                        ZibeSnackType.ERROR
                    )
                )
            }
        }
    }

    fun startRecordAudio() {
        viewModelScope.launch {
            _chatEvents.emit(
                ChatUiEvent.ShowSnackbar(
                    "Grabando audio... (Lógica en VM)",
                    ZibeSnackType.INFO
                )
            )
        }
    }

    fun stopRecordAudio() {
        viewModelScope.launch {
            _chatEvents.emit(ChatUiEvent.AudioUploadSuccess("00:05"))
            sendMessage("") // TODO: pasar URL real del audio
        }
    }

    fun cancelRecordAudio() {
        viewModelScope.launch {
            _chatEvents.emit(
                ChatUiEvent.ShowSnackbar(
                    "Grabación cancelada.",
                    ZibeSnackType.WARNING
                )
            )
        }
    }

    // =========================================================================
    //  MENSAJES
    // =========================================================================

    fun sendMessage(text: String) = viewModelScope.launch {
        // TODO: implementar lógica real de envío
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

    // Online / Offline
    fun setUserOnline() = viewModelScope.launch { userRepository.setUserOnline() }
    fun setUserOffline() = viewModelScope.launch { userRepository.setUserLastSeen() }
}
