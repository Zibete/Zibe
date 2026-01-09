package com.zibete.proyecto1.fakes

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario

data class CreateUserNodeCall(
    val user: FirebaseUser,
    val birthDate: String,
    val description: String
)

data class ActiveSessionCall(
    val uid: String,
    val installId: String,
    val fcmToken: String?
)

class FakeUserRepositoryProvider(
    private val scenarioProvider: () -> TestScenario
) : UserRepositoryProvider {

    override val myUid: String
        get() = scenarioProvider().currentUserUid ?: TestData.UID

    private fun failIfNeeded() {
        val s = scenarioProvider()
        if (s.shouldFail) throw s.runtimeException
    }

    override suspend fun accountExists(uid: String): Boolean {
        failIfNeeded()
        return scenarioProvider().accountExists
    }

    override suspend fun hasBirthDate(uid: String): Boolean {
        failIfNeeded()
        return scenarioProvider().hasBirthDate
    }

    override suspend fun getProfilePhotoUrl(): String? {
        failIfNeeded()
        return TestData.PHOTO_URL
    }

    override suspend fun getAccount(uid: String): Users? {
        failIfNeeded()
        return if (scenarioProvider().accountExists) {
            TestData.USER.copy(id = uid)
        } else {
            null
        }
    }

    override suspend fun getChatStateWith(otherUid: String, nodeType: String): String {
        failIfNeeded()
        return TestData.CHAT_STATE
    }

    override suspend fun getChatPhotosWithUser(otherUid: String, nodeType: String): List<String> {
        failIfNeeded()
        return emptyList()
    }
}

class FakeUserRepositoryActions(
    private val scenarioProvider: () -> TestScenario,
) : UserRepositoryActions {
    var lastCreateUserNodeCall: CreateUserNodeCall? = null
    private fun failIfNeeded() {
        val s = scenarioProvider()
        if (s.shouldFail) throw s.runtimeException
    }

    override suspend fun createUserNode(firebaseUser: FirebaseUser, birthDate: String, description: String) {
        val s = scenarioProvider()
        if (s.shouldFail) throw s.runtimeException
        else lastCreateUserNodeCall = CreateUserNodeCall(firebaseUser, birthDate, description)
    }

    override suspend fun setUserLastSeen() {
        failIfNeeded()
    }

    override suspend fun setUserActivityStatus(status: String) {
        failIfNeeded()
    }

    override suspend fun deleteMyAccountData(): ZibeResult<Unit> =
        if (scenarioProvider().shouldFail) {
            ZibeResult.Failure(scenarioProvider().runtimeException)
        } else {
            ZibeResult.Success(Unit)
        }

    override suspend fun deleteProfilePhoto() {
        failIfNeeded()
    }

    override suspend fun putProfilePhotoInStorage(localUri: Uri) {
        failIfNeeded()
    }

    override suspend fun updateUserFields(fields: Map<String, Any?>) {
        failIfNeeded()
    }
}
