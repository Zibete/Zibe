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
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val loginManager: LoginManager,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository
) {

    // Propiedad calculada: Acceso seguro al usuario (mantiene la lógica "crash-si-no-hay")
    val user: FirebaseUser
        get() = checkNotNull(firebaseAuth.currentUser) {
        "User must be logged in to access this property"
    }

    val uid: String
        get() = user.uid

    /**
     * Lógica de abandono de grupo (Solo manipulación de datos y Firebase).
     */
    fun performExitGroupDataCleanup() {
        val currentGroup = userPreferencesRepository.groupName
        val myUid = user.uid
        val myUserNameGroup = userPreferencesRepository.userNameGroup

        if (currentGroup.isEmpty()) return

        // 🛑 CRÍTICO: Eliminación de Listeners Estáticos
        // Los listeners estáticos (listenerGroupBadge, etc.) NO PUEDEN ser accedidos
        // aquí. La Activity/ViewModel que los creó debe eliminarlos al observar el cambio
        // de estado (repo.inGroup = false) o al recibir un evento.

        // 1. Eliminar chats unknown vinculados (Lógica de negocio)
        firebaseRefsContainer.refDatos.child(myUid).child(CHATWITHUNKNOWN)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val key = snapshot.key ?: continue
                        firebaseRefsContainer.refChatUnknown.child("$myUid <---> $key").removeValue()
                        firebaseRefsContainer.refChatUnknown.child("$key <---> $myUid").removeValue()
                        firebaseRefsContainer.refDatos.child(key).child(CHATWITHUNKNOWN).child(myUid).removeValue()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        firebaseRefsContainer.refDatos.child(myUid).child(CHATWITHUNKNOWN).removeValue()

        // 2. Notificación de Abandono al grupo
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS", Locale.getDefault())
        val chatmsg = ChatsGroup(
            "abandonó la sala",
            dateFormat.format(Calendar.getInstance().time),
            myUserNameGroup,
            myUid,
            0,
            userPreferencesRepository.userType
        )
        firebaseRefsContainer.refGroupChat.child(currentGroup).push().setValue(chatmsg)

        // 3. Eliminar usuario del nodo Users
        firebaseRefsContainer.refGroupUsers.child(currentGroup).child(myUid).removeValue()

        // 4. Reset estado local (Repo)
        userPreferencesRepository.resetGroupState() // Función que limpia el estado de grupo en el Repo
    }

    /**
     * Ejecuta toda la limpieza de sesión y devuelve el Intent de navegación.
     * @return Intent configurado para navegar a SplashActivity.
     */
    suspend fun logOutCleanup(): Intent {

        // 1. Limpieza de estado
        userRepository.setUserLastSeen()

        // 2. Si está en grupo, realizar limpieza de grupo
        if (userPreferencesRepository.inGroup) {
            performExitGroupDataCleanup()
        }

        // 3. Limpieza de preferencias
        userPreferencesRepository.clearAllData()

        // 4. Cierre de sesión en Firebase y Facebook
        firebaseAuth.signOut()
        loginManager.logOut()

        // 5. Devolver Intent para navegación
        return Intent(applicationContext, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}