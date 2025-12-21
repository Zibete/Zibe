package com.zibete.proyecto1.data

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.facebook.login.LoginManager
import com.google.firebase.auth.*
import com.zibete.proyecto1.ui.constants.Constants.MSG_INFO
import com.zibete.proyecto1.ui.constants.MSG_USER_LEAVED
import com.zibete.proyecto1.ui.splash.SplashActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val userPreferencesDSRepository: UserPreferencesDSRepository,
    private val firebaseAuth: FirebaseAuth,
    private val loginManager: LoginManager,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) {

    // ---------------------------------------------------------------------------------------------
    // AUTH USER
    // ---------------------------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------------------------
    // AUTH API
    // ---------------------------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------------------------
    // PROFILE (AUTH USER)
    // ---------------------------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------------------------
    // GROUP EXIT CLEANUP
    // ---------------------------------------------------------------------------------------------

    suspend fun performExitGroupDataCleanup() {

        val groupContext = userPreferencesDSRepository.groupContextFlow.first() ?: return
        val groupName = groupContext.groupName

        // 1) Eliminar mi lista de chats del grupo
        groupRepository.removeMyGroupChatList()

        // 2) Eliminar mis chats privados dentro del grupo
        groupRepository.removeMyPrivateGroupChats()

        // 3) Enviar mensaje de abandono
        groupRepository.sendGroupMessage(
            groupName = groupName,
            userName = groupContext.userName,
            userType = groupContext.userType,
            chatType = MSG_INFO,
            content = MSG_USER_LEAVED
        )

        // 4) Eliminar usuario del grupo
        groupRepository.removeUserFromGroup(
            groupName = groupName
        )

        // 5) Reset estado local (DataStore)
        userPreferencesDSRepository.resetGroupState()
    }


    // ---------------------------------------------------------------------------------------------
    // LOGOUT CLEANUP
    // ---------------------------------------------------------------------------------------------

    suspend fun logOutCleanup(): Intent {

        // 1) Presencia
        userRepository.setUserLastSeen()

        // 2) Limpieza de grupo si corresponde
        val inGroup = userPreferencesDSRepository.inGroupFlow.first()
        if (inGroup) {
            performExitGroupDataCleanup()
        }

        // 3) Limpiar prefs (DataStore)
        userPreferencesDSRepository.clearAllData()

        // 4) Sign out
        firebaseAuth.signOut()
        loginManager.logOut()

        // 5) Navegación
        return Intent(applicationContext, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    // ---------------------------------------------------------------------------------------------
    // PROVIDER TYPE
    // ---------------------------------------------------------------------------------------------

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
