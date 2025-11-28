package com.zibete.proyecto1.data

import android.content.Context
import android.content.Intent
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.ui.constants.Constants.CHATWITHUNKNOWN
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.utils.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManager @Inject constructor(
    // Dependencias inyectadas por Hilt
    @ApplicationContext private val applicationContext: Context,
    private val repo: UserPreferencesRepository,
    private val auth: FirebaseAuth,
    private val loginManager: LoginManager,
    private val firebaseRefs: FirebaseRefsContainer
) {

    // Propiedad calculada: Acceso seguro al usuario (mantiene la lógica "crash-si-no-hay")
    private val user: FirebaseUser
        get() = auth.currentUser!!

    /**
     * Lógica de abandono de grupo (Solo manipulación de datos y Firebase).
     * Toda la lógica de UI, Fragmentos y Toolbars DEBE estar fuera de aquí.
     */
    fun performExitGroupDataCleanup() {
        val currentGroup = repo.groupName
        val currentUid = user.uid
        val currentUserName = repo.userName

        if (currentGroup.isEmpty()) return

        // 🛑 CRÍTICO: Eliminación de Listeners Estáticos
        // Los listeners estáticos (listenerGroupBadge, etc.) NO PUEDEN ser accedidos
        // aquí. La Activity/ViewModel que los creó debe eliminarlos al observar el cambio
        // de estado (repo.inGroup = false) o al recibir un evento.

        // 1. Eliminar chats unknown vinculados (Lógica de negocio)
        firebaseRefs.refDatos.child(currentUid).child(CHATWITHUNKNOWN)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val key = snapshot.key ?: continue
                        firebaseRefs.refChatUnknown.child("$currentUid <---> $key").removeValue()
                        firebaseRefs.refChatUnknown.child("$key <---> $currentUid").removeValue()
                        firebaseRefs.refDatos.child(key).child(CHATWITHUNKNOWN).child(currentUid).removeValue()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        firebaseRefs.refDatos.child(currentUid).child(CHATWITHUNKNOWN).removeValue()

        // 2. Notificación de Abandono al grupo
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS", Locale.getDefault())
        val chatmsg = ChatsGroup(
            "abandonó la sala",
            dateFormat.format(Calendar.getInstance().time),
            currentUserName,
            currentUid,
            0,
            repo.userType
        )
        firebaseRefs.refGroupChat.child(currentGroup).push().setValue(chatmsg)

        // 3. Eliminar usuario del nodo Users
        firebaseRefs.refGroupUsers.child(currentGroup).child(currentUid).removeValue()

        // 4. Reset estado local (Repo)
        repo.resetGroupState() // Función que limpia el estado de grupo en el Repo
    }

    /**
     * Ejecuta toda la limpieza de sesión y devuelve el Intent de navegación.
     * @param deleteUser Si es distinto de null, implica que el usuario está siendo borrado.
     * @return Intent configurado para navegar a SplashActivity.
     */
    fun logOutCleanup(deleteUser: String?): Intent {
        val currentUid = user.uid

        // 1. Limpieza de estado y listeners globales (Datos)
        if (deleteUser == null) {
            UserRepository.setUserOffline(applicationContext, currentUid)
        }

        if (repo.inGroup) {
            // Llama a la lógica de limpieza de grupo (solo datos)
            performExitGroupDataCleanup()
        }

        // 🛑 CRÍTICO: Eliminación de Listeners Estáticos
        // El listenerToken de sesión DEBE ser removido por la Activity/ViewModel que lo esté gestionando.

        // 2. Limpieza de datos
        repo.clearAllData()
        // EditProfileFragment.deleteProfilePreferences(this) <--- Tarea UI/Contextual (se queda en Activity)

        // 3. Auth
        auth.signOut()
        loginManager.logOut() // Facebook Logout

        // 4. Devolver el Intent (La Activity lo lanzará)
        return Intent(applicationContext, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}