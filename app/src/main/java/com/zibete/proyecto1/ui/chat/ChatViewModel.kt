package com.zibete.proyecto1.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatRefs
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.Constants
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val CHAT_TYPE_INDIVIDUAL = "CHAT"
private const val CHAT_TYPE_UNKNOWN = "CHATWITHUNKNOWN"

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userSessionManager: UserSessionManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val myUid = userSessionManager.myUid
    private val targetUserId: String? = savedStateHandle["userId"]

    // Header (nombre, estado, foto, bloqueo, notifs, etc.)
    private val _headerState = MutableStateFlow<ChatHeaderState>(ChatHeaderState.Loading)
    val headerState: StateFlow<ChatHeaderState> = _headerState.asStateFlow()

    // Estado de conexión del otro usuario
    val userStatus: StateFlow<UserStatus> = userRepository
        .observeUserStatus(targetUserId ?: "", "chatWith")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatus.Offline)

    // Referencias del chat (para mensajes, storage, etc.)
    private val _chatRefs = MutableStateFlow<ChatRefs?>(null)
    val chatRefs: StateFlow<ChatRefs?> = _chatRefs.asStateFlow()

    // Eventos de UI (snackbar, diálogos, navegación, etc.)
    private val _events = MutableSharedFlow<ChatUiEvent>()
    val events: SharedFlow<ChatUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            setupChat()
        }
    }

    // =========================================================================
    //  SETUP INICIAL
    // =========================================================================

    private suspend fun setupChat() = withContext(Dispatchers.IO) {
        _headerState.value = ChatHeaderState.Loading

        if (targetUserId != null) {
            // Chat 1 a 1
            loadOneToOneChat(targetUserId)

            // Una vez que tenemos header + refs, traemos estado de notifs/bloqueo
            val chatType = _chatRefs.value?.refChatWith
            if (chatType != null) {
                val state = userRepository.getNotificationState(targetUserId, chatType)
                val notificationsEnabled = (state != "silent")
                val isBlocked = (state == "bloq")

                _headerState.update { current ->
                    (current as? ChatHeaderState.Loaded)?.copy(
                        notificationsEnabled = notificationsEnabled,
                        isBlocked = isBlocked
                    ) ?: current
                }
            }
        } else {
            // Chat desconocido (grupo / anonymous)
            loadUnknownChat()
            // Por ahora no manejamos notifs/bloqueo para unknown
        }
    }

    private suspend fun loadOneToOneChat(userId: String) {
        val snapshot = firebaseRefsContainer.refCuentas.child(userId).get().await()
        val name = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario"
        val photo = snapshot.child("foto").getValue(String::class.java).orEmpty()
        val finalPhoto = photo.ifEmpty {
            Constants.DEFAULT_PROFILE_PHOTO_URL
        }

        _headerState.value = ChatHeaderState.Loaded(
            name = name,
            status = context.getString(R.string.offline),
            photoUrl = finalPhoto,
            isConnected = true
        )

        val token = firebaseRefsContainer.refCuentas
            .child(userId)
            .child("token")
            .get()
            .await()
            .getValue(String::class.java)

        _chatRefs.value = ChatRefs(
            startedByMe = firebaseRefsContainer.refChatsRoot
                .child(CHAT_TYPE_INDIVIDUAL)
                .child("$myUid <---> $userId")
                .child("Mensajes"),
            startedByHim = firebaseRefsContainer.refChatsRoot
                .child(CHAT_TYPE_INDIVIDUAL)
                .child("$userId <---> $myUid")
                .child("Mensajes"),
            refYourReceiverData = Constants.storageReference.child("CHATWITH/$userId/"),
            refMyReceiverData = Constants.storageReference.child("CHATWITH/$myUid/"),
            refActual = firebaseRefsContainer.refDatos
                .child(myUid)
                .child("ChatList")
                .child("Actual"),
            token = token,
            refChat = CHAT_TYPE_INDIVIDUAL,
            refChatWith = "CHATWITH"
        )
    }

    private suspend fun loadUnknownChat() {
        val groupName = userPreferencesRepository.groupName
        val unknownId = userPreferencesRepository.unknownUserId

        var photoUrl = Constants.DEFAULT_PROFILE_PHOTO_URL
        val type = firebaseRefsContainer.refGroupUsers
            .child(groupName)
            .child(unknownId)
            .child("type")
            .get()
            .await()
            .getValue(Int::class.java) ?: 0

        if (type != 0) {
            photoUrl = firebaseRefsContainer.refCuentas
                .child(unknownId)
                .child("foto")
                .get()
                .await()
                .getValue(String::class.java)
                .orEmpty()
        }

        _headerState.value = ChatHeaderState.Loaded(
            name = context.getString(R.string.anonimous),
            status = context.getString(R.string.offline),
            photoUrl = photoUrl.ifEmpty { Constants.DEFAULT_PROFILE_PHOTO_URL },
            isConnected = true
        )

        _chatRefs.value = ChatRefs(
            startedByMe = firebaseRefsContainer.refChatsRoot
                .child("UNKNOWN")
                .child("$myUid <---> $unknownId")
                .child("Mensajes"),
            startedByHim = firebaseRefsContainer.refChatsRoot
                .child("UNKNOWN")
                .child("$unknownId <---> $myUid")
                .child("Mensajes"),
            refYourReceiverData = Constants.storageReference.child("$CHAT_TYPE_UNKNOWN/$unknownId/"),
            refMyReceiverData = Constants.storageReference.child("$CHAT_TYPE_UNKNOWN/$myUid/"),
            refActual = firebaseRefsContainer.refDatos
                .child(myUid)
                .child("ChatList")
                .child("Actual"),
            token = null,
            refChat = "UNKNOWN",
            refChatWith = CHAT_TYPE_UNKNOWN
        )
    }

    // Si en algún momento necesitás extras de la Intent:
    fun loadChatDetails(extras: Bundle?) {
        // Hoy la lógica principal está en setupChat(); esto queda como hook futuro.
        fetchChatHeaderData()
    }

    private fun fetchChatHeaderData() = viewModelScope.launch {
        // En una app real, esto iría a un UseCase o Repository.
        // Lo dejamos como placeholder.
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
                    _events.emit(
                        ChatUiEvent.ShowSnackbar(
                            "Imagen lista para enviar.",
                            ZibeSnackType.SUCCESS
                        )
                    )
                }
                // TODO: subir a Storage y preparar sendMessage con PHOTO.
            } else {
                viewModelScope.launch {
                    _events.emit(
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
                _events.emit(
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
            _events.emit(
                ChatUiEvent.ShowSnackbar(
                    "Grabando audio... (Lógica en VM)",
                    ZibeSnackType.INFO
                )
            )
        }
    }

    fun stopRecordAudio() {
        viewModelScope.launch {
            _events.emit(ChatUiEvent.AudioUploadSuccess("00:05"))
            sendMessage("") // TODO: pasar URL real del audio
        }
    }

    fun cancelRecordAudio() {
        viewModelScope.launch {
            _events.emit(
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

    // =========================================================================
    //  NOTIFICACIONES / BLOQUEO / ELIMINAR CHAT
    // =========================================================================

    fun toggleNotifications() {
        viewModelScope.launch {
            val ctx = getChatContextOrNull() ?: return@launch

            val currentState = userRepository.getNotificationState(ctx.id, ctx.type)
            val newState = if (currentState == "silent") ctx.type else "silent"

            userRepository.updateNotificationState(ctx.id, ctx.type, ctx.name)

            val enabled = (newState != "silent")

            // Actualizar UI
            _headerState.update { current ->
                (current as? ChatHeaderState.Loaded)?.copy(
                    notificationsEnabled = enabled
                ) ?: current
            }

            // Evento de UI
            _events.emit(
                ChatUiEvent.ShowToggleNotificationSuccess(
                    name = ctx.name,
                    enabled = enabled
                )
            )
        }
    }


    fun blockUser() {
        viewModelScope.launch {
            val ctx = getChatContextOrNull() ?: return@launch
            _events.emit(ChatUiEvent.ConfirmBlock(ctx.name))
        }
    }

    fun onBlockConfirmed() {
        viewModelScope.launch {
            val ctx = getChatContextOrNull() ?: return@launch

            // TODO: asegurarse de que exista userRepository.blockUser(id, type)
            userRepository.blockUser(ctx.id, ctx.type, ctx.name)

            _headerState.update { current ->
                (current as? ChatHeaderState.Loaded)?.copy(
                    isBlocked = true
                ) ?: current
            }

            _events.emit(ChatUiEvent.ShowBlockSuccess(ctx.name))
        }
    }

    fun unblockUser() {
        viewModelScope.launch {
            val ctx = getChatContextOrNull() ?: return@launch
            _events.emit(ChatUiEvent.ConfirmUnblock(ctx.name))
        }
    }

    fun onUnblockConfirmed() {
        viewModelScope.launch {
            val ctx = getChatContextOrNull() ?: return@launch

            userRepository.unblockUser(ctx.id, ctx.type)

            _headerState.update { current ->
                (current as? ChatHeaderState.Loaded)?.copy(
                    isBlocked = false
                ) ?: current
            }

            _events.emit(ChatUiEvent.ShowUnblockSuccess(ctx.name))
        }
    }

    fun deleteChat() {
        viewModelScope.launch {
            val ctx = getChatContextOrNull() ?: return@launch
            _events.emit(ChatUiEvent.ConfirmDeleteChat(ctx.name))
        }
    }

    fun onDeleteChatConfirmed(deleteMessages: Boolean) {
        viewModelScope.launch {
            val ctx = getChatContextOrNull() ?: return@launch
            userRepository.deleteChat(ctx.id, ctx.type, deleteMessages)
            _events.emit(ChatUiEvent.ShowChatDeleted)
        }
    }

    private data class ChatContext(
        val name: String,
        val id: String,
        val type: String
    )

    private fun getChatContextOrNull(): ChatContext? {
        val header = headerState.value as? ChatHeaderState.Loaded ?: return null
        val id = targetUserId ?: return null
        val type = _chatRefs.value?.refChatWith ?: return null

        return ChatContext(
            name = header.name,
            id = id,
            type = type
        )
    }

    fun triggerProfileView(context: Context) {
        // TODO: navegación a pantalla de perfil
    }

    // Online / Offline
    fun setUserOnline() = viewModelScope.launch { userRepository.setUserOnline() }
    fun setUserOffline() = viewModelScope.launch { userRepository.setUserLastSeen() }
}
