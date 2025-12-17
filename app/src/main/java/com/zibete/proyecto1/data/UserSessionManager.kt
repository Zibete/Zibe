package com.zibete.proyecto1.data

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.facebook.login.LoginManager
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.utils.Utils.now
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
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

    // --- Acceso seguro/nullable ---
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val firebaseUser: FirebaseUser
        get() = checkNotNull(firebaseAuth.currentUser) {
            "User must be logged in to access this property"
        }

    val myUid: String
        get() = firebaseUser.uid

    fun hasSession(): Boolean = currentUser != null

    var latitude: Double = 0.0
    var longitude: Double = 0.0

    // ================= AUTH API (centralizada) =================

    suspend fun signInWithEmail(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signInWithCredential(credential: AuthCredential) {
        firebaseAuth.signInWithCredential(credential).await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    suspend fun deleteFirebaseUser() {
        firebaseUser.delete().await()
    }

    // ================= PROFILE (AUTH USER) =================

    suspend fun updateAuthProfile(
        userName: String,
        photoUrl: String?
    ) {
        val req = UserProfileChangeRequest.Builder()
            .setDisplayName(userName)
            .apply { photoUrl?.let { photoUri = it.toUri() } }
            .build()

        firebaseUser.updateProfile(req).await()
    }

    // ================= GROUP EXIT CLEANUP =================

    fun performExitGroupDataCleanup() {

        // Elimino mi lista de chats en el grupo
        firebaseRefsContainer.refData.child(myUid).child(NODE_GROUP_CHAT).removeValue()

        // Eliminar todos mis chats del grupo
        firebaseRefsContainer.refChatMessageGroupsRoot
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val key = snapshot.key ?: continue
                        if (key.contains(myUid)) snapshot.ref.removeValue()
                    }
                }
                override fun onCancelled(error: DatabaseError) = Unit
            })

        // Notificación de abandono
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

        // Eliminar usuario del nodo Users
        firebaseRefsContainer.refGroupUsers
            .child(userPreferencesRepository.groupName)
            .child(myUid)
            .removeValue()

        // Reset estado local
        userPreferencesRepository.resetGroupState()
    }

    // ================= LOGOUT CLEANUP =================

    suspend fun logOutCleanup(): Intent {

        // 1) Presencia
        userRepository.setUserLastSeen()

        // 2) Si está en grupo, limpieza de grupo
        if (userPreferencesRepository.inGroup) {
            performExitGroupDataCleanup()
        }

        // 3) Prefs
        userPreferencesRepository.clearAllData()

        // 4) Sign out
        firebaseAuth.signOut()
        loginManager.logOut()

        // 5) Navegación
        return Intent(applicationContext, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    // ================= PROVIDER TYPE =================

    enum class AuthProvider { PASSWORD, GOOGLE, FACEBOOK, OTHER }

    fun authProvider(): AuthProvider {
        val providers = firebaseUser.providerData.map { it.providerId }.toSet()
        return when {
            "password" in providers -> AuthProvider.PASSWORD
            "google.com" in providers -> AuthProvider.GOOGLE
            "facebook.com" in providers -> AuthProvider.FACEBOOK
            else -> AuthProvider.OTHER
        }
    }

    fun authProviderLabel(): String? = when (authProvider()) {
        AuthProvider.GOOGLE -> "Google"
        AuthProvider.FACEBOOK -> "Facebook"
        else -> null
    }
}
