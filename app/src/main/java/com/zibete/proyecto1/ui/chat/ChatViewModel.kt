package com.zibete.proyecto1.ui.chat

import android.app.Application
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.yalantis.ucrop.UCrop
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Chats
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    private val repo: UserPreferencesRepository,
    private val auth: FirebaseAuth,
    private val firebaseRefs: FirebaseRefsContainer,
    private val sessionManager: UserSessionManager
) : ViewModel() {

    // --- 1. ESTADO PÚBLICO (Observado por la Activity) ---
    private val _chats = MutableStateFlow<List<Chats>>(emptyList())
    val chats: StateFlow<List<Chats>> = _chats

    private val _headerState = MutableStateFlow(ChatHeaderState())
    val headerState: StateFlow<ChatHeaderState> = _headerState

    // UI Events (Mensajes, Dialogos, etc.)
    private val _events = MutableSharedFlow<ChatUiEvent>()
    val events: SharedFlow<ChatUiEvent> = _events


    // --- 2. ESTADO INTERNO (Datos del Chat) ---
    private var idUserFinal: String? = null
    private var nameUserFinal: String? = null
    private var refChatWith: String = "" // CHATWITH o CHATWITHUNKNOWN
    private var refChat: String = "" // CHAT o UNKNOWN
    private var receiverToken: String? = null

    private val user get() = auth.currentUser!!

    init {
        // ViewModel inicia la escucha de datos
        setupListeners()
    }

    // =========================================================================
    //  CICLO DE VIDA Y SETUP INICIAL
    // =========================================================================

    fun loadChatDetails(extras: Bundle?) {
        // Mapear los extras de la Intent a las variables de estado internas
        val idUser = extras?.getString("id_user")
        val unknownName = extras?.getString("unknownName")
        val idUserUnknown = extras?.getString("idUserUnknown")

        if (idUser != null) {
            // Chat 1 a 1
            idUserFinal = idUser
            refChat = CHAT_TYPE_INDIVIDUAL
            refChatWith = "CHATWITH"
        } else if (idUserUnknown != null) {
            // Chat Desconocido (Grupo)
            idUserFinal = idUserUnknown
            nameUserFinal = unknownName
            refChat = CHAT_TYPE_UNKNOWN
            refChatWith = CHAT_TYPE_UNKNOWN
        }

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
        if (text.isEmpty() || headerState.value.isBlocked) {
            _events.emit(ChatUiEvent.ShowSnackbar("Error: Chat bloqueado o mensaje vacío."))
            return@launch
        }
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
    //  CICLO DE VIDA MVVM
    // =========================================================================

    fun onPause() {
        // 🛑 Actualizar estado Firebase: setUserOffline / Limpiar refActual
    }

    fun onResume() {
        // 🛑 Actualizar estado Firebase: setUserOnline / Setear refActual
    }

    override fun onCleared() {
        super.onCleared()
        // 🛑 Limpieza de listeners de Firebase (removeEventListener)
        // Importante para prevenir Memory Leaks
    }

}