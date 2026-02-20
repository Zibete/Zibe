package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario

class FakeUserRepositoryProvider(
    private val scenarioProvider: () -> TestScenario
) : UserRepositoryProvider, LocalRepositoryProvider {

    private val shouldFail: Boolean get() = scenarioProvider().shouldFail
    private val runtimeException: Throwable get() = scenarioProvider().runtimeException

    // LocalRepositoryProvider implementation
    override val myUserName: String = "Test User"
    override val myProfilePhotoUrl: String = ""
    override val myEmail: String = "test@example.com"

    override suspend fun accountExists(uid: String): Boolean =
        if (shouldFail) throw runtimeException else scenarioProvider().accountExists

    override suspend fun hasBirthDate(uid: String): Boolean =
        if (shouldFail) throw runtimeException else scenarioProvider().hasBirthDate

    override suspend fun getProfilePhotoUrl(): String? =
        if (shouldFail) throw runtimeException else TestData.PHOTO_URL

    override suspend fun getDefaultProfilePhotoUrl(): ZibeResult<String> =
        if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else {
            ZibeResult.Success(TestData.PHOTO_URL)
        }

    override suspend fun getAccount(uid: String): Users? {
        if (shouldFail) throw runtimeException
        return if (scenarioProvider().accountExists) TestData.USER.copy(id = uid) else null
    }

    suspend fun getChatStateWith(
        otherUid: String,
        nodeType: String
    ): String {
        if (shouldFail) throw runtimeException
        return TestData.CHAT_STATE
    }

    override suspend fun getMyAccount(): ZibeResult<Users> {
        return if (shouldFail) {
            ZibeResult.Failure(runtimeException)
        } else if (scenarioProvider().accountExists) {
            ZibeResult.Success(TestData.USER)
        } else {
            ZibeResult.Failure(RuntimeException("USER_NOT_FOUND"))
        }
    }
}
