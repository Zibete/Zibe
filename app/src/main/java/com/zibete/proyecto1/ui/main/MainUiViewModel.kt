package com.zibete.proyecto1.ui.main

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.CHATWITHUNKNOWN
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

// Enum para controlar la pantalla actual desde el VM
enum class CurrentScreen {
    CHAT, USERS, GROUPS, EDIT_PROFILE, FAVORITES, OTHER
}

// Eventos de una sola vez (para navegación o diálogos)
sealed class MainUiEvent {
    object Logout : MainUiEvent()
    object SessionConflict : MainUiEvent() // Mostrar diálogo de conflicto
    data class ShowSnack(val message: String) : MainUiEvent()
    data class NavigateTo(val screenId: Int) : MainUiEvent()
}
@HiltViewModel // 1. Marcamos el VM
class MainUiViewModel @Inject constructor( // 2. Inyectamos el constructor
    private val userPreferencesRepository: UserPreferencesRepository, // Hilt nos da esto gratis
    private val firebaseAuth: FirebaseAuth,
    private val firebaseRefsContainer: FirebaseRefsContainer //
) : ViewModel() {

    private val user: FirebaseUser
        get() = firebaseAuth.currentUser!!

    private var groupMsgCountListener: ValueEventListener? = null

    // Visibilidad de componentes
    private val _toolbarVisible = MutableStateFlow(true)
    val toolbarVisible = _toolbarVisible.asStateFlow()

    private val _layoutSettingsVisible = MutableStateFlow(false)
    val layoutSettingsVisible = _layoutSettingsVisible.asStateFlow()

    private val _bottomNavVisible = MutableStateFlow(true)
    val bottomNavVisible = _bottomNavVisible.asStateFlow()

    private val _toolbarTitle = MutableStateFlow("")
    val toolbarTitle = _toolbarTitle.asStateFlow()

    // Pantalla actual
    private val _currentScreen = MutableStateFlow(CurrentScreen.OTHER)
    val currentScreen = _currentScreen.asStateFlow()

    // Badges (Contadores)
    private val _chatBadgeCount = MutableStateFlow(0)
    val chatBadgeCount = _chatBadgeCount.asStateFlow()

    private val _groupBadgeCount = MutableStateFlow(0)
    val groupBadgeCount = _groupBadgeCount.asStateFlow()

    // --- LOGICA DE SESION ---
    private var myInstallId: String? = null
    private var installIdListener: ValueEventListener? = null

    init {
        // Inicializar listeners
        setupInstallIdAndFcm()
        listenToChatBadges()
        listenToGroupBadges()
    }

    // --- FUNCIONES DE UI ---

    fun setScreen(screen: CurrentScreen) {
        _currentScreen.value = screen
    }

    fun showToolbar(show: Boolean) { _toolbarVisible.value = show }
    fun showLayoutSettings(show: Boolean) { _layoutSettingsVisible.value = show }
    fun showBottomNav(show: Boolean) { _bottomNavVisible.value = show }
    fun setToolbarTitle(title: String) { _toolbarTitle.value = title }
    fun setToolbarTitle(resId: Int) {
        // Helper simple, idealmente usar String resources wrapper
        // Por ahora pasamos el ID y la Activity lo resuelve o pasamos el string.
        // Dejaremos que la Activity observe esto o lo maneje localmente si es simple.
    }

    // --- LOGICA DE BADGES ---

    private fun listenToChatBadges() {
        val uid = user?.uid ?: return
        firebaseRefsContainer.refDatos.child(uid).child(Constants.CHATWITH)
            .orderByChild("noVisto").startAt(1.0)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var count = 0
                    for (child in snapshot.children) {
                        count += child.child("noVisto").getValue(Int::class.java) ?: 0
                    }
                    _chatBadgeCount.value = count
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun listenToGroupBadges() {
        val uid = user?.uid ?: return

        // El listener solo debe estar activo si no estamos DENTRO de un chat de grupo.
        if (userPreferencesRepository.inGroup) return

        // Apunta al nodo de grupos (asumiendo que quieres el total de mensajes/grupos)
        // Usamos el contenedor inyectado: firebaseRefs
        val query = firebaseRefsContainer.refGroupChat.parent

        groupMsgCountListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                calculateGroupBadgeCount(uid, snapshot)
            }

            override fun onCancelled(error: DatabaseError) { /* Manejo de errores */ }
        }

        query?.addValueEventListener(groupMsgCountListener!!)
    }

    // Función que ejecuta la lógica de cálculo anidada y actualiza el StateFlow
    private fun calculateGroupBadgeCount(uid: String, snapshot: DataSnapshot) {
        if (!snapshot.exists()) {
            _groupBadgeCount.value = 0
            return
        }

        // 1. Obtener el total de mensajes de TODOS los grupos
        val totalMsgCount = snapshot.children.sumOf { it.childrenCount }

        // 2. Obtener los mensajes leídos por el usuario (Asíncrono 1)
        firebaseRefsContainer.refDatos.child(uid).child("ChatList").child("msgReadGroup") // 👈 firebaseRefs
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot1: DataSnapshot) {
                    val leidos = dataSnapshot1.getValue(Int::class.java) ?: 0

                    // 3. Obtener los mensajes no leídos en chats desconocidos (Asíncrono 2)
                    val queryUnreadUnknown = firebaseRefsContainer.refDatos.child(uid) // 👈 firebaseRefs
                        .child(CHATWITHUNKNOWN)
                        .orderByChild("noVisto")
                        .startAt(1.0)

                    queryUnreadUnknown.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(ds: DataSnapshot) {
                            var countMsgUnread = 0
                            if (ds.exists()) {
                                for (child in ds.children) {
                                    countMsgUnread += child.child("noVisto").getValue(Int::class.java) ?: 0
                                }
                            }

                            // 4. Cálculo final: (Total Grupos - Leídos) + No Leídos Desconocidos
                            val totalBadge = (totalMsgCount.toInt() - leidos) + countMsgUnread

                            // 5. Actualizar StateFlow
                            _groupBadgeCount.value = if (totalBadge > 0) totalBadge else 0
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onCleared() {
        super.onCleared()
        // Asegúrate de que los listeners de badges se eliminen
        groupMsgCountListener?.let {
            // Usamos firebaseRefs aquí también
            firebaseRefsContainer.refGroupChat.parent?.removeEventListener(it)
        }
        // ... cualquier otro listener que uses debe limpiarse aquí
    }

    // --- LOGICA DE SESIÓN (Install ID) ---

    private fun setupInstallIdAndFcm() {
        val uid = user?.uid ?: return
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                myInstallId = task.result
                checkSessionConflict(uid)
            }
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                firebaseRefsContainer.refCuentas.child(uid).child("fcmToken").setValue(task.result)
            }
        }
    }

    private fun checkSessionConflict(uid: String) {
        val ref = firebaseRefsContainer.refCuentas.child(uid).child("installId")

        installIdListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remoteId = snapshot.getValue(String::class.java)
                if (myInstallId != null && remoteId != null && remoteId != myInstallId) {
                    // CONFLICTO DETECTADO -> Avisar a la UI (Activity)
                    // _uiEvents.emit(MainUiEvent.SessionConflict) // Necesitaremos un SharedFlow para eventos
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(installIdListener!!)
    }

    // --- ACCIONES DE USUARIO (LOGOUT / EXIT GROUP) ---

    fun logout() {
        val uid = user?.uid ?: return

        // 1. Salir del grupo si corresponde
        if (userPreferencesRepository.inGroup) {
            performExitGroupLogic()
        }

        // 2. Limpiar listeners
        installIdListener?.let {
            firebaseRefsContainer.refCuentas.child(uid).child("installId").removeEventListener(it)
        }

        // 3. Limpiar Repo y Auth
        userPreferencesRepository.clearAllData()
        FirebaseAuth.getInstance().signOut()
        // LoginManager.getInstance().logOut() // Facebook, requiere dependencia en gradle

        // 4. Reset VM state
        _chatBadgeCount.value = 0
    }

//    fun exitGroup() {
//        if (!userPreferencesRepository.inGroup) return
//        performExitGroupLogic()
//    }

//    private fun performExitGroupLogic() {
//        val uid = user?.uid ?: return
//        val groupName = userPreferencesRepository.groupName
//
//        // ... (Toda tu lógica de borrar nodos de firebaseRefs va aquí) ...
//
//        // Ejemplo simplificado de lo que tenías:
//        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS", Locale.getDefault())
//        val chatMsg = ChatsGroup(
//            "abandonó la sala",
//            dateFormat.format(Calendar.getInstance().time),
//            userPreferencesRepository.userName,
//            uid,
//            0,
//            userPreferencesRepository.userType
//        )
//        firebaseRefsContainer.refGroupChat.child(groupName).push().setValue(chatMsg)
//        firebaseRefsContainer.refGroupUsers.child(groupName).child(uid).removeValue()
//
//        // Actualizar Repo
//        userPreferencesRepository.clearAllData() // O solo las keys de grupo
//
//        // Actualizar estado UI
//        _toolbarVisible.value = true // Reset toolbar
//    }

}