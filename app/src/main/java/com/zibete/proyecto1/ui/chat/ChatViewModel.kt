package com.zibete.proyecto1.ui.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// ESTADO INTERNO (Propiedades que antes eran 'lateinit' en la Activity)
// Ahora son privadas y gestionadas por el VM.
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

    private val myUid = userSessionManager.uid
    private val targetUserId: String? = savedStateHandle["userId"]

    // Estado del header (nombre, foto, estado, bloqueo, etc.)
    private val _headerState = MutableStateFlow<ChatHeaderState>(ChatHeaderState.Loading)
    val headerState: StateFlow<ChatHeaderState> = _headerState.asStateFlow()

    // Estado de conexión del otro usuario
    val userStatus: StateFlow<UserStatus> = userRepository
        .observeUserStatus(targetUserId ?: "", "chatWith")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatus.Offline)

    // Referencias del chat (para el adapter y envío)
    private val _chatRefs = MutableStateFlow<ChatRefs?>(null)
    val chatRefs: StateFlow<ChatRefs?> = _chatRefs.asStateFlow()

    // Eventos UI (snackbar, navegación, etc.)
    private val _events = MutableSharedFlow<ChatUiEvent>()
    val events: SharedFlow<ChatUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            setupChat()
        }
    }

    private suspend fun setupChat() = withContext(Dispatchers.IO) {
        _headerState.value = ChatHeaderState.Loading

        if (targetUserId != null) {
            loadOneToOneChat(targetUserId)
        } else {
            loadUnknownChat()
        }
    }

    private suspend fun loadOneToOneChat(userId: String) {
        // Cargar datos del usuario
        val snapshot = firebaseRefsContainer.refCuentas.child(userId).get().await()
        val name = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario"
        val photo = snapshot.child("foto").getValue(String::class.java).orEmpty()

        _headerState.value = ChatHeaderState.Loaded(
            name = name,
            status = context.getString(R.string.offline),
            photoUrl = photo.ifEmpty { Constants.DEFAULT_PROFILE_PHOTO_URL },
            isConnected = true
        )

        val token = firebaseRefsContainer.refCuentas.child(userId).child("token")
            .get().await().getValue(String::class.java)

        _chatRefs.value = ChatRefs(
            startedByMe = firebaseRefsContainer.refChatsRoot.child("CHAT")
                .child("$myUid <---> $userId").child("Mensajes"),
            startedByHim = firebaseRefsContainer.refChatsRoot.child("CHAT")
                .child("$userId <---> $myUid").child("Mensajes"),
            refYourReceiverData = Constants.storageReference.child("CHATWITH/$userId/"),
            refMyReceiverData = Constants.storageReference.child("CHATWITH/$myUid/"),
            refActual = firebaseRefsContainer.refDatos.child(myUid).child("ChatList").child("Actual"),
            token = token,
            refChat = "CHAT",
            refChatWith = "CHATWITH"
        )
    }

    private suspend fun loadUnknownChat() {
        val groupName = userPreferencesRepository.groupName
        val unknownId = userPreferencesRepository.unknownUserId

        var photoUrl = Constants.DEFAULT_PROFILE_PHOTO_URL
        val type = firebaseRefsContainer.refGroupUsers.child(groupName).child(unknownId)
            .child("type").get().await().getValue(Int::class.java) ?: 0

        if (type != 0) {
            photoUrl = firebaseRefsContainer.refCuentas.child(unknownId)
                .child("foto").get().await().getValue(String::class.java).orEmpty()
        }

        _headerState.value = ChatHeaderState.Loaded(
            name = context.getString(R.string.anonimous),
            status = context.getString(R.string.offline),
            photoUrl = photoUrl,
            isConnected = true
        )

        _chatRefs.value = ChatRefs(
            startedByMe = firebaseRefsContainer.refChatsRoot.child("UNKNOWN")
                .child("$myUid <---> $unknownId").child("Mensajes"),
            startedByHim = firebaseRefsContainer.refChatsRoot.child("UNKNOWN")
                .child("$unknownId <---> $myUid").child("Mensajes"),
            refYourReceiverData = Constants.storageReference.child("CHATWITHUNKNOWN/$unknownId/"),
            refMyReceiverData = Constants.storageReference.child("CHATWITHUNKNOWN/$myUid/"),
            refActual = firebaseRefsContainer.refDatos.child(myUid).child("ChatList")
                .child("Actual"),
            token = null,
            refChat = "UNKNOWN",
            refChatWith = "CHATWITHUNKNOWN"
        )
    }

    // =========================================================================
    //  CICLO DE VIDA Y SETUP INICIAL
    // =========================================================================

    fun loadChatDetails(extras: Bundle?) {
        // Mapear los extras de la Intent a las variables de estado internas
//        val idUser = extras?.getString("id_user")
//        val unknownName = extras?.getString("unknownName")
//        val idUserUnknown = extras?.getString("idUserUnknown")
//
//        if (idUser != null) {
//            // Chat 1 a 1
//            idUserFinal = idUser
//            refChat = CHAT_TYPE_INDIVIDUAL
//            refChatWith = "CHATWITH"
//        } else if (idUserUnknown != null) {
//            // Chat Desconocido (Grupo)
//            idUserFinal = idUserUnknown
//            nameUserFinal = unknownName
//            refChat = CHAT_TYPE_UNKNOWN
//            refChatWith = CHAT_TYPE_UNKNOWN
//        }

        // 🛑 Lógica movida de setupChatHeaderAndRefs
        fetchChatHeaderData()
    }

    private fun setupListeners() {
        // 🛑 Aquí se lanzarían los listeners de Firebase (ChildEventListener, ValueEventListener)
        // en el viewModelScope.

        // Ejemplo: Lanzar el listener de mensajes
        // val chatRef = firebaseRefs.getChatRef(refChat, user.uid, idUserFinal!!)
        // chatRef.addChildEventListener(object : ChildEventListener { ... })
    }

    // Lógica compleja que estaba en setupChatHeaderAndRefs
    private fun fetchChatHeaderData() = viewModelScope.launch {
        // En una app profesional, esta lógica compleja iría a un Repository o UseCase
        // para buscar el nombre/foto del receptor y actualizar _headerState.

        // Aquí se realizarían las llamadas a Firebase
        // EJEMPLO:
        // val receiverData = firebaseRefs.refCuentas.child(idUserFinal!!).get()
        // _headerState.value = _headerState.value.copy(nameUser = receiverData.name, ...)
    }

    // =========================================================================
    //  GESTIÓN DE MEDIA (Crop, Audio)
    // =========================================================================

    // Reemplaza CropHelper.launchCrop
    fun startUCropFlow(sourceUri: Uri, activity: AppCompatActivity, uCropLauncher: ActivityResultLauncher<Intent>) {
        val destinationUri = Uri.fromFile(
            File(context.cacheDir, "${System.currentTimeMillis()}_cropped.jpg")
        )

        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .getIntent(activity) // Necesita la activity para construir el Intent

        uCropLauncher.launch(uCropIntent)
    }

    // Reemplaza la lógica de callback del CropHelper
    fun handleCroppedImageResult(resultCode: Int, data: Intent?, activity: AppCompatActivity) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val resultUri = UCrop.getOutput(data)
            if (resultUri != null) {
                // 🛑 Lógica de subida a Storage aquí (reemplazando refMyReceiverData!!.child(name).putFile)
                // Usamos un evento para notificar a la UI
                viewModelScope.launch {
                    _events.emit(ChatUiEvent.ShowSnackbar("Imagen lista para enviar."))
                }

                // Aquí el VM almacenaría la URI de la imagen lista para enviar
                // stringMsg = resultUri.toString()
                // msgType = Constants.PHOTO

            } else {
                viewModelScope.launch {
                    _events.emit(ChatUiEvent.ShowSnackbar("Error al obtener la imagen recortada."))
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            val error = UCrop.getError(data)
            Log.e("UCrop", "Error al recortar: $error")
            viewModelScope.launch {
                _events.emit(ChatUiEvent.ShowSnackbar("Fallo al recortar la imagen."))
            }
        }
    }

    // Lógica delegada de Audio (anteriormente startRecordAudio)
    fun startRecordAudio() {
        // 🛑 Lógica de MediaRecorder y creación de archivos MediaStore
        // (Toda la lógica compleja de PFD, MediaRecorder.start(), y gestión de Uri)
        // Al terminar, actualiza el estado _micUiState.value = recording
        viewModelScope.launch {
            _events.emit(ChatUiEvent.ShowSnackbar("Grabando audio... (Lógica en VM)"))
        }
    }

    fun stopRecordAudio() {
        // 🛑 Lógica de MediaRecorder.stop(), upload a Firebase Storage, y llama a sendMessage()
        viewModelScope.launch {
            _events.emit(ChatUiEvent.AudioUploadSuccess("00:05")) // Simulación de éxito
            sendMessage("") // Llama a send message con el URL del audio
        }
    }

    fun cancelRecordAudio() {
        // 🛑 Lógica de limpieza y borrado de archivo temporal
        viewModelScope.launch {
            _events.emit(ChatUiEvent.ShowSnackbar("Grabación cancelada."))
        }
    }


    // =========================================================================
    //  MENSAJES Y ESTADO
    // =========================================================================

    fun sendMessage(text: String) = viewModelScope.launch {
//        if (text.isEmpty() || headerState.value.isBlocked) {
//            _events.emit(ChatUiEvent.ShowSnackbar("Error: Chat bloqueado o mensaje vacío."))
//            return@launch
//        }
        // 🛑 Toda la lógica compleja de enviar a Firebase, actualizar ChatList,
        // generar la notificación FCM, y actualizar noVisto se ejecuta aquí.
    }

    fun deleteSelectedMsgs() = viewModelScope.launch {
        // 🛑 Lógica de DeleteMsgs (llamando a las referencias inyectadas: firebaseRefs)
    }

    fun onMessageTyping(isTyping: Boolean) {
        // 🛑 Lógica para actualizar el estado "escribiendo" en Firebase
    }

    fun toggleNotificationState(silent: Boolean) {
        // 🛑 Lógica de UserRepository.silent(...)
        viewModelScope.launch {
            _events.emit(ChatUiEvent.NotifyUiUpdate("MENU"))
        }
    }

    fun toggleNotifications() {
        viewModelScope.launch {
            val currentName = (headerState.value as? ChatHeaderState.Loaded)?.name
                ?: return@launch

            userRepository.toggleNotifications(
                chatWithId = targetUserId ?: return@launch,
                chatType = _chatRefs.value?.refChatWith ?: return@launch,
                nameUser = currentName
            )
        }
    }

    // Opcional: helper para saber el estado actual (si querés mensaje preciso)
    private fun isCurrentlySilent(): Boolean {
        val current = headerState.value as? ChatHeaderState.Loaded ?: return false
        return current.status == "silent" // o podés guardar el estado en una variable
    }

    fun setBlockUser(view: View) {
        // 🛑 Lógica de UserRepository.setBlockUser(...)
        viewModelScope.launch {
            _events.emit(ChatUiEvent.ShowSnackbar("Usuario bloqueado (Lógica en Manager)"))
        }
    }

    fun setUnBlockUser(view: View) {
        // 🛑 Lógica de UserRepository.setUnBlockUser(...)
        viewModelScope.launch {
            _events.emit(ChatUiEvent.ShowSnackbar("Usuario desbloqueado (Lógica en Manager)"))
        }
    }

    fun triggerChatDelete(activity: AppCompatActivity, view: View) {
        // 🛑 Lógica de ChatUtils.deleteChat(...)
        viewModelScope.launch {
            _events.emit(ChatUiEvent.ShowSnackbar("Chat borrado (Lógica en Util)"))
        }
    }

    fun triggerProfileView(context: Context) {
        // 🛑 Lógica de navegación a SlideProfileActivity
    }

    // =========================================================================

    // Online / Offline
    fun setUserOnline() = viewModelScope.launch { userRepository.setUserOnline() }
    fun setUserOffline() = viewModelScope.launch { userRepository.setUserLastSeen() }
}