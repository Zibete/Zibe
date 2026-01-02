package com.zibete.proyecto1.data

import android.content.Context
import androidx.core.net.toUri
import com.facebook.login.LoginManager
import com.google.firebase.auth.*
import com.zibete.proyecto1.core.ZibeResult
import com.zibete.proyecto1.core.constants.Constants.MSG_INFO
import com.zibete.proyecto1.core.constants.MSG_USER_LEAVED
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface UserSessionProvider {
    val currentUser: FirebaseUser?
}

interface UserSessionActions {
    suspend fun logOutCleanup()
    suspend fun signInWithEmail(email: String, password: String): ZibeResult<AuthResult>
    suspend fun signInWithCredential(credential: AuthCredential)
    suspend fun sendPasswordResetEmail(email: String): ZibeResult<Unit>
    suspend fun deleteFirebaseUser()
    suspend fun updateAuthProfile(userName: String, photoUrl: String?): ZibeResult<Unit>
    suspend fun createUser(email: String, password: String): ZibeResult<AuthResult>
}

@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions,
    private val firebaseAuth: FirebaseAuth,
    private val loginManager: LoginManager,
    private val groupRepository: GroupRepository
) : UserSessionProvider, UserSessionActions {

    // ---------------------------------------------------------------------------------------------
    // AUTH USER
    // ---------------------------------------------------------------------------------------------

    override val currentUser: FirebaseUser?
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

//    val latitude: Double get() = latitude
//    val longitude: Double get() = longitude

    fun updateMyLocation(lat: Double, lon: Double) {
        latitude = lat
        longitude = lon
    }

    // ---------------------------------------------------------------------------------------------
    // AUTH API
    // ---------------------------------------------------------------------------------------------

    // En SignIn: Aquí SI devuelves algo útil (el resultado de Auth)
    override suspend fun signInWithEmail(email: String, password: String): ZibeResult<AuthResult> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            ZibeResult.Success(result)
        } catch (e: Exception) {
            ZibeResult.Failure(e)
        }
    }

    // En UpdateProfile: Solo éxito/fallo
    override suspend fun updateAuthProfile(userName: String, photoUrl: String?): ZibeResult<Unit> {
        return try {
            val req = UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .apply { photoUrl?.let { photoUri = it.toUri() } }
                .build()
            firebaseUser.updateProfile(req).await()
            ZibeResult.Success(Unit)
        } catch (e: Exception) {
            ZibeResult.Failure(e)
        }
    }

    // 1. Cambiamos Unit por AuthResult
    override suspend fun createUser(
        email: String,
        password: String
    ): ZibeResult<AuthResult> {
        return try {
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()
            ZibeResult.Success(authResult)
        } catch (e: Exception) {
            ZibeResult.Failure(e)
        }
    }

    //    override suspend fun signInWithEmail(email: String, password: String) {
//        firebaseAuth.signInWithEmailAndPassword(email, password).await()
//    }
//
    override suspend fun signInWithCredential(credential: AuthCredential) {
        firebaseAuth.signInWithCredential(credential).await()
    }

    override suspend fun sendPasswordResetEmail(email: String): ZibeResult<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            ZibeResult.Success(Unit)
        } catch (e: Exception) {
            ZibeResult.Failure(e)
        }
    }

    override suspend fun deleteFirebaseUser() {
        firebaseUser.delete().await()
    }

    // ---------------------------------------------------------------------------------------------
    // PROFILE (AUTH USER)
    // ---------------------------------------------------------------------------------------------

//    suspend fun updateAuthProfile(
//        userName: String,
//        photoUrl: String?
//    ) {
//        val req = UserProfileChangeRequest.Builder()
//            .setDisplayName(userName)
//            .apply { photoUrl?.let { photoUri = it.toUri() } }
//            .build()
//
//        firebaseUser.updateProfile(req).await()
//    }

    // ---------------------------------------------------------------------------------------------
    // GROUP EXIT CLEANUP
    // ---------------------------------------------------------------------------------------------

    suspend fun performExitGroupDataCleanup() {

        val groupContext = userPreferencesProvider.groupContextFlow.first() ?: return
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
        userPreferencesActions.resetGroupState()
    }


    // ---------------------------------------------------------------------------------------------
    // LOGOUT CLEANUP
    // ---------------------------------------------------------------------------------------------

    override suspend fun logOutCleanup() {
        // 2) Limpieza de grupo si corresponde
        val inGroup = userPreferencesProvider.inGroupFlow.first()
        if (inGroup) { performExitGroupDataCleanup() }

        // 3) Limpiar prefs (DataStore)
        userPreferencesActions.clearAllData()

        // 4) Sign out
        firebaseAuth.signOut()
        loginManager.logOut()
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
