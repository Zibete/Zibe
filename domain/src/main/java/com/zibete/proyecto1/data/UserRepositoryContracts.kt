package com.zibete.proyecto1.data

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.model.Users

interface LocalRepositoryProvider {
    val myUserName: String
    val myProfilePhotoUrl: String
    val myEmail: String
}

interface UserRepositoryProvider {
    suspend fun getMyAccount(): ZibeResult<Users>
    suspend fun accountExists(uid: String): Boolean
    suspend fun hasBirthDate(uid: String): Boolean
    suspend fun getDefaultProfilePhotoUrl(): ZibeResult<String>
    suspend fun getProfilePhotoUrl(): String?
    suspend fun getAccount(uid: String): Users?
}

interface UserRepositoryActions {
    suspend fun createUserNode(
        firebaseUser: FirebaseUser,
        name: String,
        birthDate: String,
        description: String
    ): ZibeResult<Unit>

    suspend fun setUserLastSeen()
    suspend fun setUserActivityStatus(status: String)
    suspend fun deleteMyAccountData(): ZibeResult<Unit>
    suspend fun deleteProfilePhoto(): ZibeResult<Unit>
    suspend fun putProfilePhotoInStorage(localUri: Uri): ZibeResult<Unit>
    suspend fun updateUserFields(fields: Map<String, Any?>)
    suspend fun updateLocalProfile(name: String?, photoUrl: String?, email: String?)
    suspend fun sendFeedback(
        feedback: String,
        screen: String,
        model: String,
        appVersion: String
    ): ZibeResult<Unit>
}
