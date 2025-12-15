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
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.utils.Utils.now
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val loginManager: LoginManager,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository
) {

    val firebaseUser: FirebaseUser
        get() = checkNotNull(firebaseAuth.currentUser) {
        "User must be logged in to access this property"
    }

    val myUid: String
        get() = firebaseUser.uid

    var latitude: Double = 0.0
    var longitude: Double = 0.0

    fun performExitGroupDataCleanup() {

        // Elimino mi lista de chats en el grupo
        firebaseRefsContainer.refData.child(myUid).child(NODE_GROUP_CHAT).removeValue()

        // Eliminar todos mis chats del grupo
        firebaseRefsContainer.refChatMessageGroupsRoot
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val key = snapshot.key ?: continue

                        if (key.contains(myUid)) {
                            snapshot.ref.removeValue()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })


        // 2. Notificación de Abandono al grupo
        val chatmsg = ChatsGroup(
            "abandonó la sala",
            now(),
            userPreferencesRepository.userNameGroup,
            myUid,
            0,
            userPreferencesRepository.userType
        )
        firebaseRefsContainer.refGroupChat
            .child(userPreferencesRepository.groupName)
            .push()
            .setValue(chatmsg)

        // 3. Eliminar usuario del nodo Users
        firebaseRefsContainer.refGroupUsers
            .child(userPreferencesRepository.groupName)
            .child(myUid)
            .removeValue()

        // 4. Reset estado local (Repo)
        userPreferencesRepository.resetGroupState() // Función que limpia el estado de grupo en el Repo
    }

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

    fun deleteFirebaseUser() {
        firebaseUser.delete()
    }

}